package com.pallas.launcher.data.repository

import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import com.pallas.launcher.domain.model.FocusDndMode
import com.pallas.launcher.domain.model.FocusProfile
import com.pallas.launcher.domain.model.LauncherSettings
import com.pallas.launcher.platform.SystemActionsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

data class FocusState(
    val isActive: Boolean = false,
    val activeProfileId: String? = null,
    val endTimeMillis: Long = 0L,
    val isScheduled: Boolean = false
)

class FocusModeManager(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val lock = Any()

    private val _state = MutableStateFlow(loadState())
    val state: StateFlow<FocusState> = _state.asStateFlow()

    private fun loadState(): FocusState {
        val activeId = prefs.getString(KEY_ACTIVE_PROFILE_ID, null)?.takeIf { it.isNotBlank() }
        val endTime = prefs.getLong(KEY_END_TIME_MILLIS, 0L)
        val isScheduled = prefs.getBoolean(KEY_IS_SCHEDULED, false)
        val isActive = activeId != null && (endTime == -1L || endTime > System.currentTimeMillis())
        return if (isActive) {
            FocusState(
                isActive = true,
                activeProfileId = activeId,
                endTimeMillis = endTime,
                isScheduled = isScheduled
            )
        } else {
            FocusState()
        }
    }

    fun activate(
        profile: FocusProfile,
        durationMillis: Long?,
        forceDndOverride: Boolean? = null,
        isScheduled: Boolean = false,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        synchronized(lock) {
            val endTime = if (durationMillis == null || durationMillis <= 0L) -1L else nowMillis + durationMillis
            val shouldSyncDnd = forceDndOverride ?: profile.syncDnd

            val prevDndEnabled = prefs.getBoolean(KEY_FOCUS_ENABLED_DND, false)
            val prevRingerMuted = prefs.getBoolean(KEY_FOCUS_ENABLED_RINGER_MUTE, false)

            // Capture pre-Focus ringer mode BEFORE toggling DND, because Android's DND transition
            // (especially ALARMS_ONLY / TOTAL_SILENCE) immediately mutates AudioManager.ringerMode.
            val audio = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            var prevRingerMode = prefs.getInt(KEY_PREV_RINGER_MODE, AudioManager.RINGER_MODE_NORMAL)
            if (profile.muteRinger && !prevRingerMuted && audio != null) {
                try {
                    prevRingerMode = audio.ringerMode
                } catch (_: SecurityException) {}
            }

            var enabledDnd = prevDndEnabled
            var prevFilter = prefs.getInt(KEY_PREV_FILTER, NotificationManager.INTERRUPTION_FILTER_ALL)
            var appliedFilter = prefs.getInt(KEY_APPLIED_FILTER, NotificationManager.INTERRUPTION_FILTER_ALL)

            if (shouldSyncDnd && SystemActionsManager.isDndAccessGranted(appContext)) {
                val currentFilter = SystemActionsManager.getCurrentInterruptionFilter(appContext)
                if (!prevDndEnabled) {
                    prevFilter = currentFilter
                }
                val targetFilter = when (profile.dndMode) {
                    FocusDndMode.PRIORITY -> NotificationManager.INTERRUPTION_FILTER_PRIORITY
                    FocusDndMode.ALARMS_ONLY -> NotificationManager.INTERRUPTION_FILTER_ALARMS
                    FocusDndMode.TOTAL_SILENCE -> NotificationManager.INTERRUPTION_FILTER_NONE
                }
                if (SystemActionsManager.setDnd(appContext, enabled = true, filter = targetFilter, showToast = false)) {
                    enabledDnd = true
                    appliedFilter = targetFilter
                }
            } else if (!shouldSyncDnd && prevDndEnabled) {
                restoreDndIfOwned()
                enabledDnd = false
            }

            var mutedRinger = prevRingerMuted
            var appliedRingerMode = prefs.getInt(KEY_APPLIED_RINGER_MODE, AudioManager.RINGER_MODE_NORMAL)

            if (profile.muteRinger) {
                if (audio != null) {
                    try {
                        if (audio.ringerMode != AudioManager.RINGER_MODE_SILENT) {
                            audio.ringerMode = AudioManager.RINGER_MODE_SILENT
                        }
                        mutedRinger = true
                        appliedRingerMode = AudioManager.RINGER_MODE_SILENT
                    } catch (_: SecurityException) {
                        // Ignore when policy access is not granted
                    }
                }
            } else if (prevRingerMuted) {
                restoreRingerIfOwned()
                mutedRinger = false
            }

            val existingDismissedUntil = prefs.getLong(KEY_SCHEDULE_DISMISSED_UNTIL, 0L)
            val updatedDismissedUntil = if (isScheduled || nowMillis >= existingDismissedUntil) 0L else existingDismissedUntil

            prefs.edit()
                .putString(KEY_ACTIVE_PROFILE_ID, profile.id)
                .putLong(KEY_END_TIME_MILLIS, endTime)
                .putBoolean(KEY_IS_SCHEDULED, isScheduled)
                .putBoolean(KEY_FOCUS_ENABLED_DND, enabledDnd)
                .putInt(KEY_PREV_FILTER, prevFilter)
                .putInt(KEY_APPLIED_FILTER, appliedFilter)
                .putBoolean(KEY_FOCUS_ENABLED_RINGER_MUTE, mutedRinger)
                .putInt(KEY_PREV_RINGER_MODE, prevRingerMode)
                .putInt(KEY_APPLIED_RINGER_MODE, appliedRingerMode)
                .putLong(KEY_SCHEDULE_DISMISSED_UNTIL, updatedDismissedUntil)
                .apply()

            _state.value = FocusState(
                isActive = true,
                activeProfileId = profile.id,
                endTimeMillis = endTime,
                isScheduled = isScheduled
            )
        }
    }

    fun deactivate(
        settings: LauncherSettings,
        isManualDismiss: Boolean = true,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        synchronized(lock) {
            restoreDndIfOwned()
            restoreRingerIfOwned()

            val dismissedUntil = if (isManualDismiss && settings.focusScheduleEnabled &&
                isInScheduleWindow(nowMillis, settings.focusScheduleStartMinute, settings.focusScheduleEndMinute)
            ) {
                scheduleWindowEndMillis(nowMillis, settings.focusScheduleStartMinute, settings.focusScheduleEndMinute)
            } else {
                0L
            }

            prefs.edit()
                .remove(KEY_ACTIVE_PROFILE_ID)
                .putLong(KEY_END_TIME_MILLIS, 0L)
                .putBoolean(KEY_IS_SCHEDULED, false)
                .putBoolean(KEY_FOCUS_ENABLED_DND, false)
                .putBoolean(KEY_FOCUS_ENABLED_RINGER_MUTE, false)
                .putLong(KEY_SCHEDULE_DISMISSED_UNTIL, dismissedUntil)
                .apply()

            _state.value = FocusState()
        }
    }

    fun toggleProfile(profile: FocusProfile, settings: LauncherSettings, nowMillis: Long = System.currentTimeMillis()) {
        val current = _state.value
        if (current.isActive && current.activeProfileId == profile.id) {
            deactivate(settings, isManualDismiss = true, nowMillis = nowMillis)
        } else {
            activate(profile, durationMillis = null, isScheduled = false, nowMillis = nowMillis)
        }
    }

    fun syncState(settings: LauncherSettings, nowMillis: Long = System.currentTimeMillis()): FocusProfile? {
        synchronized(lock) {
            val activeId = prefs.getString(KEY_ACTIVE_PROFILE_ID, null)?.takeIf { it.isNotBlank() }
            val endTime = prefs.getLong(KEY_END_TIME_MILLIS, 0L)
            val isScheduled = prefs.getBoolean(KEY_IS_SCHEDULED, false)
            val profileExists = activeId != null && settings.focusProfiles.any { it.id == activeId }

            if (activeId != null && !profileExists) {
                deactivate(settings, isManualDismiss = false, nowMillis = nowMillis)
                return null
            }

            if (activeId != null && !isScheduled && endTime > 0L && nowMillis >= endTime) {
                deactivate(settings, isManualDismiss = false, nowMillis = nowMillis)
            }

            val currentAfterExpiry = _state.value
            if (settings.focusScheduleEnabled) {
                val inWindow = isInScheduleWindow(
                    nowMillis,
                    settings.focusScheduleStartMinute,
                    settings.focusScheduleEndMinute
                )
                val dismissedUntil = prefs.getLong(KEY_SCHEDULE_DISMISSED_UNTIL, 0L)
                if (inWindow && !currentAfterExpiry.isActive && nowMillis >= dismissedUntil) {
                    val targetProfile = settings.focusProfiles.firstOrNull { it.id == settings.focusScheduleProfileId }
                        ?: settings.focusProfiles.firstOrNull { it.id == settings.defaultFocusProfileId }
                        ?: settings.focusProfiles.firstOrNull()
                    if (targetProfile != null) {
                        activate(
                            profile = targetProfile,
                            durationMillis = null,
                            isScheduled = true,
                            nowMillis = nowMillis
                        )
                        return targetProfile
                    }
                } else if (!inWindow && currentAfterExpiry.isActive && currentAfterExpiry.isScheduled) {
                    deactivate(settings, isManualDismiss = false, nowMillis = nowMillis)
                }
            } else if (currentAfterExpiry.isActive && currentAfterExpiry.isScheduled) {
                deactivate(settings, isManualDismiss = false, nowMillis = nowMillis)
            }
            return null
        }
    }

    private fun restoreDndIfOwned() {
        val enabledByFocus = prefs.getBoolean(KEY_FOCUS_ENABLED_DND, false)
        if (!enabledByFocus) return
        if (!SystemActionsManager.isDndAccessGranted(appContext)) return
        val appliedFilter = prefs.getInt(KEY_APPLIED_FILTER, NotificationManager.INTERRUPTION_FILTER_ALL)
        val currentFilter = SystemActionsManager.getCurrentInterruptionFilter(appContext)
        // Only restore if the user didn't manually change DND after Focus enabled it
        if (currentFilter == appliedFilter) {
            val prevFilter = prefs.getInt(KEY_PREV_FILTER, NotificationManager.INTERRUPTION_FILTER_ALL)
            val restoreEnabled = prevFilter != NotificationManager.INTERRUPTION_FILTER_ALL
            SystemActionsManager.setDnd(
                appContext,
                enabled = restoreEnabled,
                filter = prevFilter,
                showToast = false
            )
        }
    }

    private fun restoreRingerIfOwned() {
        val mutedByFocus = prefs.getBoolean(KEY_FOCUS_ENABLED_RINGER_MUTE, false)
        if (!mutedByFocus) return
        val audio = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val appliedMode = prefs.getInt(KEY_APPLIED_RINGER_MODE, AudioManager.RINGER_MODE_NORMAL)
        if (audio.ringerMode == appliedMode) {
            val prevMode = prefs.getInt(KEY_PREV_RINGER_MODE, AudioManager.RINGER_MODE_NORMAL)
            try {
                audio.ringerMode = prevMode
            } catch (_: SecurityException) {
                // Ignore
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "pallas_focus_state"
        private const val KEY_ACTIVE_PROFILE_ID = "active_profile_id"
        private const val KEY_END_TIME_MILLIS = "focus_end_time_millis"
        private const val KEY_IS_SCHEDULED = "focus_is_scheduled"
        private const val KEY_FOCUS_ENABLED_DND = "focus_enabled_dnd"
        private const val KEY_PREV_FILTER = "prev_interruption_filter"
        private const val KEY_APPLIED_FILTER = "applied_interruption_filter"
        private const val KEY_FOCUS_ENABLED_RINGER_MUTE = "focus_enabled_ringer_mute"
        private const val KEY_PREV_RINGER_MODE = "prev_ringer_mode"
        private const val KEY_APPLIED_RINGER_MODE = "applied_ringer_mode"
        private const val KEY_SCHEDULE_DISMISSED_UNTIL = "schedule_dismissed_until_millis"

        @Volatile
        private var instance: FocusModeManager? = null

        fun getInstance(context: Context): FocusModeManager =
            instance ?: synchronized(this) {
                instance ?: FocusModeManager(context.applicationContext).also { instance = it }
            }

        fun isMinuteInWindow(minuteOfDay: Int, startMinute: Int, endMinute: Int): Boolean {
            if (startMinute == endMinute) return false
            return if (startMinute < endMinute) {
                minuteOfDay in startMinute until endMinute
            } else {
                minuteOfDay >= startMinute || minuteOfDay < endMinute
            }
        }

        fun isInScheduleWindow(nowMillis: Long, startMinute: Int, endMinute: Int): Boolean {
            val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
            val minuteOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            return isMinuteInWindow(minuteOfDay, startMinute, endMinute)
        }

        fun scheduleWindowEndMillis(nowMillis: Long, startMinute: Int, endMinute: Int): Long {
            val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
            val currentMinute = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val endCal = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                set(Calendar.HOUR_OF_DAY, endMinute / 60)
                set(Calendar.MINUTE, endMinute % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (startMinute > endMinute && currentMinute >= startMinute) {
                endCal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return endCal.timeInMillis
        }
    }
}
