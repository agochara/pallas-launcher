package com.pallas.launcher.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.pallas.launcher.domain.model.AppsHorizontalAlignment
import com.pallas.launcher.domain.model.AppsVerticalPosition
import com.pallas.launcher.domain.model.ClockHorizontalAlignment
import com.pallas.launcher.domain.model.ClockVerticalPosition
import com.pallas.launcher.domain.model.EdgeScrubPosition
import com.pallas.launcher.domain.model.EdgeScrubReleaseAction
import com.pallas.launcher.domain.model.GestureAction
import com.pallas.launcher.domain.model.LauncherSettings
import com.pallas.launcher.domain.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<LauncherSettings> = _settings.asStateFlow()

    private fun loadSettings(): LauncherSettings {
        val themeModeStr = prefs.getString(KEY_THEME_MODE, ThemeMode.DARK.name)
        val themeMode = try {
            ThemeMode.valueOf(themeModeStr ?: ThemeMode.DARK.name)
        } catch (_: Exception) {
            ThemeMode.DARK
        }

        val showScrim = prefs.getBoolean(KEY_WALLPAPER_SCRIM, false)
        val hideStatusBar = prefs.getBoolean(KEY_HIDE_STATUS_BAR, false)

        val clockVert = enumOrDefault(prefs.getString(KEY_CLOCK_VERT, null), ClockVerticalPosition.TOP)

        val clockHorizStr = prefs.getString(KEY_CLOCK_HORIZ, ClockHorizontalAlignment.RIGHT.name)
        val clockHoriz = try {
            ClockHorizontalAlignment.valueOf(clockHorizStr ?: ClockHorizontalAlignment.RIGHT.name)
        } catch (_: Exception) {
            ClockHorizontalAlignment.RIGHT
        }

        val appsVert = enumOrDefault(prefs.getString(KEY_APPS_VERT, null), AppsVerticalPosition.BOTTOM)

        val appsHorizStr = prefs.getString(KEY_APPS_HORIZ, AppsHorizontalAlignment.RIGHT.name)
        val appsHoriz = try {
            AppsHorizontalAlignment.valueOf(appsHorizStr ?: AppsHorizontalAlignment.RIGHT.name)
        } catch (_: Exception) {
            AppsHorizontalAlignment.RIGHT
        }

        val is24Hour = prefs.getBoolean(KEY_24_HOUR, false)
        val showDate = prefs.getBoolean(KEY_SHOW_DATE, true)
        val showBattery = prefs.getBoolean(KEY_SHOW_BATTERY, true)

        val swipeLeftStr = prefs.getString(KEY_SWIPE_LEFT, "camera") ?: "camera"
        val swipeRightStr = prefs.getString(KEY_SWIPE_RIGHT, "dialer") ?: "dialer"
        val swipeUpStr = prefs.getString(KEY_SWIPE_UP, "search") ?: "search"
        val swipeDownStr = prefs.getString(KEY_SWIPE_DOWN, "notifications") ?: "notifications"

        val aliasesJson = prefs.getString(KEY_ALIASES, "{}") ?: "{}"
        val aliases = mutableMapOf<String, String>()
        try {
            val json = JSONObject(aliasesJson)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                aliases[key] = json.getString(key)
            }
        } catch (_: Exception) {}

        val fontScale = prefs.getFloat(KEY_FONT_SCALE, 1.0f)
        val hiddenApps = prefs.getStringSet(KEY_HIDDEN_APPS, emptySet()) ?: emptySet()
        val edgeScrubPos = enumOrDefault(prefs.getString(KEY_EDGE_SCRUB_POS, null), EdgeScrubPosition.RIGHT)
        val edgeScrubRelease = enumOrDefault(prefs.getString(KEY_EDGE_SCRUB_RELEASE, null), EdgeScrubReleaseAction.STAY_OPEN)

        return LauncherSettings(
            themeMode = themeMode,
            showWallpaperScrim = showScrim,
            hideStatusBar = hideStatusBar,
            clockVerticalPosition = clockVert,
            clockHorizontalAlignment = clockHoriz,
            appsVerticalPosition = appsVert,
            appsHorizontalAlignment = appsHoriz,
            is24HourClock = is24Hour,
            showDate = showDate,
            showBattery = showBattery,
            shortSwipeLeftAction = GestureAction.fromString(swipeLeftStr),
            shortSwipeRightAction = GestureAction.fromString(swipeRightStr),
            swipeUpAction = GestureAction.fromString(swipeUpStr),
            swipeDownAction = GestureAction.fromString(swipeDownStr),
            customAliases = aliases,
            appFontScale = fontScale,
            hiddenApps = hiddenApps,
            edgeScrubPosition = edgeScrubPos,
            edgeScrubReleaseAction = edgeScrubRelease
        )
    }

    fun updateThemeMode(theme: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, theme.name).apply()
        _settings.value = _settings.value.copy(themeMode = theme)
    }

    fun updateWallpaperScrim(scrim: Boolean) {
        prefs.edit().putBoolean(KEY_WALLPAPER_SCRIM, scrim).apply()
        _settings.value = _settings.value.copy(showWallpaperScrim = scrim)
    }

    fun updateHideStatusBar(hide: Boolean) {
        prefs.edit().putBoolean(KEY_HIDE_STATUS_BAR, hide).apply()
        _settings.value = _settings.value.copy(hideStatusBar = hide)
    }

    fun updateClockPosition(vertical: ClockVerticalPosition, horizontal: ClockHorizontalAlignment) {
        prefs.edit()
            .putString(KEY_CLOCK_VERT, vertical.name)
            .putString(KEY_CLOCK_HORIZ, horizontal.name)
            .apply()
        _settings.value = _settings.value.copy(
            clockVerticalPosition = vertical,
            clockHorizontalAlignment = horizontal
        )
    }

    fun updateAppsPosition(vertical: AppsVerticalPosition, horizontal: AppsHorizontalAlignment) {
        prefs.edit()
            .putString(KEY_APPS_VERT, vertical.name)
            .putString(KEY_APPS_HORIZ, horizontal.name)
            .apply()
        _settings.value = _settings.value.copy(
            appsVerticalPosition = vertical,
            appsHorizontalAlignment = horizontal
        )
    }

    fun update24HourClock(is24Hour: Boolean) {
        prefs.edit().putBoolean(KEY_24_HOUR, is24Hour).apply()
        _settings.value = _settings.value.copy(is24HourClock = is24Hour)
    }

    fun updateShowDate(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_DATE, show).apply()
        _settings.value = _settings.value.copy(showDate = show)
    }

    fun updateShowBattery(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_BATTERY, show).apply()
        _settings.value = _settings.value.copy(showBattery = show)
    }

    fun updateGestureActions(
        swipeLeft: GestureAction? = null,
        swipeRight: GestureAction? = null,
        swipeUp: GestureAction? = null,
        swipeDown: GestureAction? = null
    ) {
        val editor = prefs.edit()
        var current = _settings.value

        swipeLeft?.let {
            editor.putString(KEY_SWIPE_LEFT, GestureAction.toString(it))
            current = current.copy(shortSwipeLeftAction = it)
        }
        swipeRight?.let {
            editor.putString(KEY_SWIPE_RIGHT, GestureAction.toString(it))
            current = current.copy(shortSwipeRightAction = it)
        }
        swipeUp?.let {
            editor.putString(KEY_SWIPE_UP, GestureAction.toString(it))
            current = current.copy(swipeUpAction = it)
        }
        swipeDown?.let {
            editor.putString(KEY_SWIPE_DOWN, GestureAction.toString(it))
            current = current.copy(swipeDownAction = it)
        }

        editor.apply()
        _settings.value = current
    }

    fun setAlias(alias: String, target: String) {
        val current = _settings.value.customAliases.toMutableMap()
        if (target.isBlank()) {
            current.remove(alias.lowercase().trim())
        } else {
            current[alias.lowercase().trim()] = target.trim()
        }
        val json = JSONObject(current as Map<*, *>)
        prefs.edit().putString(KEY_ALIASES, json.toString()).apply()
        _settings.value = _settings.value.copy(customAliases = current)
    }

    fun removeAlias(alias: String) {
        val current = _settings.value.customAliases.toMutableMap()
        current.remove(alias.lowercase().trim())
        val json = JSONObject(current as Map<*, *>)
        prefs.edit().putString(KEY_ALIASES, json.toString()).apply()
        _settings.value = _settings.value.copy(customAliases = current)
    }

    fun hideApp(packageName: String) {
        val current = _settings.value.hiddenApps.toMutableSet()
        current.add(packageName)
        prefs.edit().putStringSet(KEY_HIDDEN_APPS, current).apply()
        _settings.value = _settings.value.copy(hiddenApps = current)
    }

    fun unhideApp(packageName: String) {
        val current = _settings.value.hiddenApps.toMutableSet()
        current.remove(packageName)
        prefs.edit().putStringSet(KEY_HIDDEN_APPS, current).apply()
        _settings.value = _settings.value.copy(hiddenApps = current)
    }

    fun updateFontScale(scale: Float) {
        prefs.edit().putFloat(KEY_FONT_SCALE, scale).apply()
        _settings.value = _settings.value.copy(appFontScale = scale)
    }

    fun updateEdgeScrubPosition(position: EdgeScrubPosition) {
        prefs.edit().putString(KEY_EDGE_SCRUB_POS, position.name).apply()
        _settings.value = _settings.value.copy(edgeScrubPosition = position)
    }

    fun updateEdgeScrubReleaseAction(action: EdgeScrubReleaseAction) {
        prefs.edit().putString(KEY_EDGE_SCRUB_RELEASE, action.name).apply()
        _settings.value = _settings.value.copy(edgeScrubReleaseAction = action)
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(value: String?, default: T): T {
        if (value == null) return default
        return try {
            java.lang.Enum.valueOf(T::class.java, value)
        } catch (_: Exception) {
            default
        }
    }

    companion object {
        private const val PREFS_NAME = "pallas_launcher_prefs"
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_WALLPAPER_SCRIM = "key_wallpaper_scrim"
        private const val KEY_HIDE_STATUS_BAR = "key_hide_status_bar"
        private const val KEY_CLOCK_VERT = "key_clock_vert"
        private const val KEY_CLOCK_HORIZ = "key_clock_horiz"
        private const val KEY_APPS_VERT = "key_apps_vert"
        private const val KEY_APPS_HORIZ = "key_apps_horiz"
        private const val KEY_24_HOUR = "key_24_hour"
        private const val KEY_SHOW_DATE = "key_show_date"
        private const val KEY_SHOW_BATTERY = "key_show_battery"
        private const val KEY_SWIPE_LEFT = "key_swipe_left"
        private const val KEY_SWIPE_RIGHT = "key_swipe_right"
        private const val KEY_SWIPE_UP = "key_swipe_up"
        private const val KEY_SWIPE_DOWN = "key_swipe_down"
        private const val KEY_ALIASES = "key_aliases"
        private const val KEY_FONT_SCALE = "key_font_scale"
        private const val KEY_HIDDEN_APPS = "key_hidden_apps"
        private const val KEY_EDGE_SCRUB_POS = "key_edge_scrub_pos"
        private const val KEY_EDGE_SCRUB_RELEASE = "key_edge_scrub_release"
    }
}
