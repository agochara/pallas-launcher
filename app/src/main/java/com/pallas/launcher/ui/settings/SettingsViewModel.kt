package com.pallas.launcher.ui.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pallas.launcher.data.repository.AppRepository
import com.pallas.launcher.data.repository.FocusModeManager
import com.pallas.launcher.data.repository.FocusState
import com.pallas.launcher.data.repository.QuickCaptureRepository
import com.pallas.launcher.data.repository.ScreenRepository
import com.pallas.launcher.data.repository.SettingsBackup
import com.pallas.launcher.data.repository.SettingsRepository
import com.pallas.launcher.domain.model.AppItem
import com.pallas.launcher.domain.model.CommandConfig
import com.pallas.launcher.domain.model.FocusProfile
import com.pallas.launcher.domain.model.HomeScreen
import com.pallas.launcher.domain.model.LauncherSettings
import com.pallas.launcher.domain.model.MacroRoutine
import com.pallas.launcher.domain.model.ScreenApp
import com.pallas.launcher.platform.MacroExecutor
import com.pallas.launcher.platform.SystemActionsManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

/**
 * Dedicated ViewModel for launcher settings, screens management, backups, and secondary configuration.
 * Decouples settings management from [com.pallas.launcher.ui.MainViewModel].
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    val settingsRepository = SettingsRepository.getInstance(context)
    val screenRepository = ScreenRepository.getInstance(context)
    val appRepository = AppRepository.getInstance(context)
    val focusModeManager = FocusModeManager.getInstance(context)
    val quickCaptureRepository = QuickCaptureRepository.getInstance(context)
    private val uiPrefs = context.getSharedPreferences("pallas_ui_state", Context.MODE_PRIVATE)

    val settings: StateFlow<LauncherSettings> = settingsRepository.settings
    val focusState: StateFlow<FocusState> = focusModeManager.state
    val screens: StateFlow<List<HomeScreen>> = screenRepository.screens
    val installedApps: StateFlow<List<AppItem>> = appRepository.installedApps
    val allInstalledApps: StateFlow<List<AppItem>> = appRepository.rawInstalledApps

    private val _selectedScreenId = MutableStateFlow(uiPrefs.getString("current_screen_id", null))

    val currentScreenIndex: StateFlow<Int> = combine(
        screenRepository.screens,
        _selectedScreenId
    ) { screenList, id ->
        if (screenList.isEmpty()) 0
        else {
            val idx = if (id != null) screenList.indexOfFirst { it.id == id } else 0
            idx.coerceIn(0, screenList.lastIndex)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    fun setScreenIndex(index: Int) {
        val id = screens.value.getOrNull(index)?.id ?: return
        _selectedScreenId.value = id
        uiPrefs.edit().putString("current_screen_id", id).apply()
    }

    fun updateSettings(transform: LauncherSettings.() -> LauncherSettings) =
        settingsRepository.update(transform)

    fun addScreen(name: String, contextTag: String?) = screenRepository.addScreen(name, contextTag?.trim()?.ifBlank { null })
    fun renameScreen(id: String, name: String, contextTag: String?) = screenRepository.renameScreen(id, name, contextTag?.trim()?.ifBlank { null })
    fun removeScreen(id: String) = screenRepository.removeScreen(id)
    fun moveScreen(fromIndex: Int, toIndex: Int) = screenRepository.moveScreen(fromIndex, toIndex)
    fun reorderApps(screenId: String, apps: List<ScreenApp>) = screenRepository.reorderApps(screenId, apps)
    fun removeAppFromScreen(screenId: String, uniqueKey: String) = screenRepository.removeAppFromScreen(screenId, uniqueKey)
    fun updateAppLabel(screenId: String, uniqueKey: String, newLabel: String) =
        screenRepository.updateAppLabel(screenId, uniqueKey, newLabel, com.pallas.launcher.domain.model.NameLengthSortOrder.OFF)
    fun addAppsToScreen(screenId: String, apps: List<AppItem>) =
        screenRepository.addAppsToScreen(screenId, apps, com.pallas.launcher.domain.model.NameLengthSortOrder.OFF)
    fun sortedForDisplay(apps: List<ScreenApp>): List<ScreenApp> =
        screenRepository.sortAppsByNameLength(apps, settings.value.nameLengthSortOrder)

    fun unhideApp(uniqueKey: String) = settingsRepository.unhideApp(uniqueKey)
    fun unhideFromAppList(uniqueKey: String) = settingsRepository.unhideFromAppList(uniqueKey)

    fun setAlias(alias: String, target: String) = settingsRepository.setAlias(alias, target)
    fun removeAlias(alias: String) = settingsRepository.removeAlias(alias)
    fun addSearchContact(nickname: String, name: String, phoneNumber: String) =
        settingsRepository.addSearchContact(nickname, name, phoneNumber)
    fun removeSearchContact(id: String) = settingsRepository.removeSearchContact(id)

    fun activateFocus(profile: FocusProfile, durationMinutes: Int? = null) {
        if ((profile.syncDnd || profile.muteRinger) && !SystemActionsManager.isDndAccessGranted(context)) {
            SystemActionsManager.openDndAccessSettings(context)
        }
        val durationMs = durationMinutes?.takeIf { it > 0 }?.let { it * 60_000L }
        focusModeManager.activate(profile, durationMillis = durationMs, isScheduled = false)
        profile.onEnterMacroId?.let { macroId ->
            settings.value.macros.firstOrNull { it.id == macroId }?.let { runMacro(it) }
        }
    }

    fun deactivateFocus() {
        focusModeManager.deactivate(settings.value, isManualDismiss = true)
    }

    fun saveFocusProfile(profile: FocusProfile, makeDefault: Boolean = false) {
        updateSettings {
            val idx = focusProfiles.indexOfFirst { it.id == profile.id }
            val updatedList = if (idx >= 0) {
                focusProfiles.toMutableList().apply { this[idx] = profile }
            } else {
                focusProfiles + profile
            }
            val newDefault = when {
                makeDefault -> profile.id
                defaultFocusProfileId == null -> profile.id
                else -> defaultFocusProfileId
            }
            copy(focusProfiles = updatedList, defaultFocusProfileId = newDefault)
        }
        focusModeManager.syncState(settings.value)
    }

    fun deleteFocusProfile(profileId: String) {
        updateSettings {
            val remaining = focusProfiles.filterNot { it.id == profileId }
            val newDefault = if (defaultFocusProfileId == profileId) remaining.firstOrNull()?.id else defaultFocusProfileId
            val newSchedProfile = if (focusScheduleProfileId == profileId) remaining.firstOrNull()?.id else focusScheduleProfileId
            copy(
                focusProfiles = remaining,
                defaultFocusProfileId = newDefault,
                focusScheduleProfileId = newSchedProfile
            )
        }
        focusModeManager.syncState(settings.value)
    }

    fun setDefaultFocusProfile(profileId: String) {
        updateSettings { copy(defaultFocusProfileId = profileId) }
    }

    fun saveMacro(macro: MacroRoutine) {
        updateSettings {
            val idx = macros.indexOfFirst { it.id == macro.id }
            val updated = if (idx >= 0) {
                macros.toMutableList().apply { this[idx] = macro }
            } else {
                macros + macro
            }
            copy(macros = updated)
        }
    }

    fun deleteMacro(macroId: String) {
        updateSettings { copy(macros = macros.filterNot { it.id == macroId }) }
    }

    fun runMacro(
        macro: MacroRoutine,
        onNavigate: (SettingsSubpage) -> Unit = {}
    ) {
        viewModelScope.launch {
            MacroExecutor.execute(
                context = context,
                macro = macro,
                settingsRepository = settingsRepository,
                appRepository = appRepository,
                quickCaptureRepository = quickCaptureRepository,
                allowedKeys = null,
                onOverlay = { cmd ->
                    when (cmd) {
                        MacroExecutor.OverlayCommand.Notebook -> onNavigate(SettingsSubpage.NOTEBOOK)
                        MacroExecutor.OverlayCommand.LatexStudio -> onNavigate(SettingsSubpage.LATEX_STUDIO)
                        MacroExecutor.OverlayCommand.Atlas -> onNavigate(SettingsSubpage.WORLD_ATLAS)
                        MacroExecutor.OverlayCommand.QuickCapture -> onNavigate(SettingsSubpage.QUICK_CAPTURE)
                        is MacroExecutor.OverlayCommand.SwitchScreen -> {
                            val idx = screens.value.indexOfFirst { it.id == cmd.screenId }
                            if (idx >= 0) setScreenIndex(idx)
                        }
                    }
                }
            )
        }
    }

    fun updateCommandConfig(id: String, enabled: Boolean, prefix: String): String? {
        val cleanPrefix = prefix.trim()
        if (cleanPrefix.isEmpty()) return "Prefix cannot be blank"
        if (cleanPrefix.contains("\\s".toRegex())) return "Prefix cannot contain spaces"
        val currentConfigs = settings.value.commandConfigs
        val conflict = currentConfigs.values.firstOrNull {
            it.id != id && it.enabled && it.prefix.equals(cleanPrefix, ignoreCase = true)
        }
        if (conflict != null && enabled) {
            return "Prefix '$cleanPrefix' is already used by ${conflict.label}"
        }
        updateSettings {
            val innerConflict = commandConfigs.values.firstOrNull {
                it.id != id && it.enabled && it.prefix.equals(cleanPrefix, ignoreCase = true)
            }
            if (innerConflict != null && enabled) return@updateSettings this
            val existing = commandConfigs[id] ?: CommandConfig.DEFAULTS[id]
                ?: CommandConfig(id, id, enabled, cleanPrefix)
            copy(commandConfigs = commandConfigs + (id to existing.copy(enabled = enabled, prefix = cleanPrefix)))
        }
        return null
    }

    sealed interface ImportOutcome {
        data class Success(val screens: Int, val apps: Int) : ImportOutcome
        data class Failure(val message: String) : ImportOutcome
    }

    suspend fun buildBackupJson(): String = withContext(Dispatchers.Default) {
        SettingsBackup.export(
            settings = settings.value,
            screens = screens.value,
            screenRepository = screenRepository,
            customAppLabels = screenRepository.customAppLabels.value
        )
    }

    suspend fun previewBackup(text: String): SettingsBackup.ParseResult = withContext(Dispatchers.Default) {
        SettingsBackup.parse(text)
    }

    suspend fun applyBackup(text: String): ImportOutcome = withContext(Dispatchers.Default) {
        when (val result = SettingsBackup.parse(text)) {
            is SettingsBackup.ParseResult.Error -> ImportOutcome.Failure(result.message)
            is SettingsBackup.ParseResult.Ok -> {
                val parsed = result.parsed
                settingsRepository.replaceAll(parsed.settings)
                val restoredScreens = parsed.screensJson?.let {
                    val list = screenRepository.parseScreens(it)
                    screenRepository.replaceAll(list)
                    list
                }
                if (parsed.hasCustomAppLabels) {
                    screenRepository.replaceCustomAppLabels(parsed.customAppLabels)
                } else if (restoredScreens != null) {
                    val derivedLabels = buildMap {
                        restoredScreens.forEach { screen ->
                            screen.apps.forEach { app ->
                                app.customLabel?.trim()?.takeIf { it.isNotEmpty() }?.let { put(app.uniqueKey, it) }
                            }
                        }
                    }
                    screenRepository.replaceCustomAppLabels(derivedLabels)
                }
                restoredScreens?.firstOrNull()?.id?.let { firstId ->
                    _selectedScreenId.value = firstId
                    uiPrefs.edit().putString("current_screen_id", firstId).apply()
                }
                ImportOutcome.Success(parsed.screenCount, parsed.appCount)
            }
        }
    }
}
