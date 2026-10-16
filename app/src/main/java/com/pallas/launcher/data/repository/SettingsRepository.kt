package com.pallas.launcher.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.pallas.launcher.domain.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Settings persistence, reduced to ONE serialization path.
 *
 *  - The whole [LauncherSettings] lives in prefs as a single JSON blob, written by [SettingsBackup.settingsToJson] -
 *    the exact same code the export file uses. Adding a setting therefore means: add the field to LauncherSettings and
 *    to SettingsBackup.settingsToJson / settingsFromJson. Nothing else. (The round-trip test fails if you forget.)
 *  - Secrets (the Wolfram AppID) live in a SEPARATE prefs file that is excluded from every backup and never exported.
 *  - Every `updateX` below is a one-liner over [update]. They are kept only so MainViewModel / the settings pages
 *    compile unchanged; Phase 2 deletes them in favour of `update { copy(...) }`.
 *  - First launch after this change migrates the old per-key preferences (with the OLD defaults, so nobody's
 *    behaviour changes), writes the blob, then removes the legacy keys.
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val secrets: SharedPreferences = context.getSharedPreferences(SECRETS_NAME, Context.MODE_PRIVATE)
    private val lock = Any()

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<LauncherSettings> = _settings.asStateFlow()

    // ------------------------------------------------------------------------------------------------ core API

    /** Atomically transforms the settings, persists them and publishes the result. */
    fun update(transform: LauncherSettings.() -> LauncherSettings) {
        synchronized(lock) {
            val old = _settings.value
            val new = old.transform()
            if (new == old) return
            persist(old, new)
            _settings.value = new
        }
    }

    /** Import: replaces everything except the secret, which is never part of a backup. */
    fun replaceAll(new: LauncherSettings) = update { new.copy(wolframAppId = wolframAppId) }

    private fun persist(old: LauncherSettings, new: LauncherSettings) {
        prefs.edit().putString(KEY_SETTINGS_JSON, SettingsBackup.settingsToJson(new).toString()).apply()
        if (new.wolframAppId != old.wolframAppId) {
            secrets.edit().putString(KEY_WOLFRAM_APP_ID, new.wolframAppId).apply()
        }
    }

    private fun load(): LauncherSettings {
        val blob = prefs.getString(KEY_SETTINGS_JSON, null)
        val base = if (blob != null) {
            try {
                SettingsBackup.settingsFromJson(JSONObject(blob))
            } catch (_: Exception) {
                LauncherSettings()
            }
        } else {
            migrateLegacy()
        }
        return base.copy(wolframAppId = secrets.getString(KEY_WOLFRAM_APP_ID, "") ?: "")
    }

    // ------------------------------------------------------------------------------------ legacy migration

    /**
     * Reads the pre-blob per-key preferences using the defaults the OLD loader used (they differ from the model
     * defaults in places - e.g. swipe-up was "search"), so an existing install behaves exactly as before.
     * Network commands stay ENABLED for existing installs; only fresh installs get them off (opt-in).
     */
    private fun migrateLegacy(): LauncherSettings {
        val hasLegacy = prefs.all.isNotEmpty()

        fun <T : Enum<T>> enumOf(key: String, default: T, values: Array<T>): T =
            values.firstOrNull { it.name == prefs.getString(key, null) } ?: default

        fun gesture(key: String, default: String) = GestureAction.fromString(prefs.getString(key, default) ?: default)

        val aliases = mutableMapOf<String, String>()
        try {
            val json = JSONObject(prefs.getString(KEY_ALIASES, "{}") ?: "{}")
            json.keys().forEach { k -> aliases[k] = json.getString(k) }
        } catch (_: Exception) {
        }

        val contacts = mutableListOf<SearchContact>()
        try {
            val arr = JSONArray(prefs.getString(KEY_SEARCH_CONTACTS, "[]") ?: "[]")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                contacts.add(
                    SearchContact(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        nickname = o.getString("nickname"),
                        name = o.optString("name", ""),
                        phoneNumber = o.getString("phoneNumber")
                    )
                )
            }
        } catch (_: Exception) {
        }

        val fontScale = prefs.getFloat(KEY_FONT_SCALE, 1.0f)
        val clockPad = prefs.getInt(KEY_CLOCK_PADDING, 24)
        val screensPad = prefs.getInt(KEY_SCREENS_PADDING, 32)

        // Old behaviour: every command enabled unless the user had changed it.
        val commandConfigs = CommandConfig.ALL_ENABLED.toMutableMap()
        try {
            prefs.getString(KEY_COMMAND_CONFIGS, null)?.let { raw ->
                val obj = JSONObject(raw)
                obj.keys().forEach { id ->
                    val c = obj.optJSONObject(id) ?: return@forEach
                    val def = CommandConfig.ALL_ENABLED[id]
                    commandConfigs[id] = CommandConfig(
                        id = id,
                        label = def?.label ?: id,
                        enabled = c.optBoolean("enabled", def?.enabled ?: true),
                        prefix = c.optString("prefix", def?.prefix ?: id)
                    )
                }
            }
        } catch (_: Exception) {
        }

        val migrated = LauncherSettings(
            themeMode = enumOf(KEY_THEME_MODE, ThemeMode.DARK, ThemeMode.entries.toTypedArray()),
            showWallpaperScrim = prefs.getBoolean(KEY_WALLPAPER_SCRIM, false),
            hideStatusBar = prefs.getBoolean(KEY_HIDE_STATUS_BAR, false),
            clockVerticalPosition = enumOf(KEY_CLOCK_VERT, ClockVerticalPosition.TOP, ClockVerticalPosition.entries.toTypedArray()),
            clockHorizontalAlignment = enumOf(KEY_CLOCK_HORIZ, ClockHorizontalAlignment.RIGHT, ClockHorizontalAlignment.entries.toTypedArray()),
            appsVerticalPosition = enumOf(KEY_APPS_VERT, AppsVerticalPosition.BOTTOM, AppsVerticalPosition.entries.toTypedArray()),
            appsHorizontalAlignment = enumOf(KEY_APPS_HORIZ, AppsHorizontalAlignment.RIGHT, AppsHorizontalAlignment.entries.toTypedArray()),
            is24HourClock = prefs.getBoolean(KEY_24_HOUR, false),
            showDate = prefs.getBoolean(KEY_SHOW_DATE, true),
            showBattery = prefs.getBoolean(KEY_SHOW_BATTERY, true),
            shortSwipeLeftAction = gesture(KEY_SWIPE_LEFT, "camera"),
            shortSwipeRightAction = gesture(KEY_SWIPE_RIGHT, "dialer"),
            swipeUpAction = gesture(KEY_SWIPE_UP, "search"),
            swipeDownAction = gesture(KEY_SWIPE_DOWN, "notifications"),
            customAliases = aliases,
            searchContacts = contacts,
            appFontScale = fontScale,
            screensFontScale = prefs.getFloat(KEY_SCREENS_FONT_SCALE, fontScale),
            appListFontScale = prefs.getFloat(KEY_APP_LIST_FONT_SCALE, fontScale),
            clockPaddingDp = clockPad,
            clockPaddingLeftDp = prefs.getInt(KEY_CLOCK_PADDING_LEFT, clockPad),
            clockPaddingRightDp = prefs.getInt(KEY_CLOCK_PADDING_RIGHT, clockPad),
            clockPaddingTopDp = prefs.getInt(KEY_CLOCK_PADDING_TOP, clockPad),
            clockPaddingBottomDp = prefs.getInt(KEY_CLOCK_PADDING_BOTTOM, (clockPad * 0.5f).toInt().coerceAtLeast(8)),
            screensPaddingDp = screensPad,
            screensPaddingLeftDp = prefs.getInt(KEY_SCREENS_PADDING_LEFT, screensPad),
            screensPaddingRightDp = prefs.getInt(KEY_SCREENS_PADDING_RIGHT, screensPad),
            screensPaddingTopDp = prefs.getInt(KEY_SCREENS_PADDING_TOP, 12),
            screensPaddingBottomDp = prefs.getInt(KEY_SCREENS_PADDING_BOTTOM, 24),
            nameLengthSortOrder = enumOf(KEY_NAME_LENGTH_SORT_ORDER, NameLengthSortOrder.OFF, NameLengthSortOrder.entries.toTypedArray()),
            hiddenApps = prefs.getStringSet(KEY_HIDDEN_APPS, emptySet())?.toSet() ?: emptySet(),
            hiddenFromAppList = prefs.getStringSet(KEY_HIDDEN_FROM_APP_LIST, emptySet())?.toSet() ?: emptySet(),
            edgeScrubPosition = enumOf(KEY_EDGE_SCRUB_POS, EdgeScrubPosition.RIGHT, EdgeScrubPosition.entries.toTypedArray()),
            edgeScrubReleaseAction = enumOf(KEY_EDGE_SCRUB_RELEASE, EdgeScrubReleaseAction.STAY_OPEN, EdgeScrubReleaseAction.entries.toTypedArray()),
            textShadowMode = enumOf(KEY_TEXT_SHADOW, TextShadowMode.AUTO, TextShadowMode.entries.toTypedArray()),
            preferredBrowserPackage = prefs.getString(KEY_BROWSER_PACKAGE, null)?.takeIf { it.isNotBlank() },
            webSearchEngine = enumOf(KEY_WEB_ENGINE, WebSearchEngine.GOOGLE, WebSearchEngine.entries.toTypedArray()),
            commandConfigs = commandConfigs,
            defaultWeatherCity = prefs.getString(KEY_DEFAULT_WEATHER_CITY, "") ?: "",
            wolframAppId = "" // moved to the secrets file below
        )

        if (hasLegacy) {
            // Move the secret, then replace every legacy key by the blob in one atomic commit.
            prefs.getString(KEY_WOLFRAM_APP_ID, null)?.takeIf { it.isNotBlank() }?.let {
                secrets.edit().putString(KEY_WOLFRAM_APP_ID, it).commit()
            }
            prefs.edit()
                .clear()
                .putString(KEY_SETTINGS_JSON, SettingsBackup.settingsToJson(migrated).toString())
                .commit()
        }
        return migrated
    }

    fun updateCommandConfig(id: String, enabled: Boolean, prefix: String) = update {
        val existing = commandConfigs[id] ?: CommandConfig.DEFAULTS[id] ?: CommandConfig(id, id, enabled, prefix)
        copy(commandConfigs = commandConfigs + (id to existing.copy(enabled = enabled, prefix = prefix.trim())))
    }

    fun setAlias(alias: String, target: String) = update {
        val key = alias.lowercase().trim()
        copy(customAliases = if (target.isBlank()) customAliases - key else customAliases + (key to target.trim()))
    }

    fun removeAlias(alias: String) = update { copy(customAliases = customAliases - alias.lowercase().trim()) }

    fun hideApp(uniqueKey: String) = update { copy(hiddenApps = hiddenApps + uniqueKey) }
    fun unhideApp(uniqueKey: String) = update {
        copy(hiddenApps = hiddenApps.filterNot { k -> k == uniqueKey || k == uniqueKey.substringBefore('/') }.toSet())
    }
    fun hideFromAppList(uniqueKey: String) = update { copy(hiddenFromAppList = hiddenFromAppList + uniqueKey) }
    fun unhideFromAppList(uniqueKey: String) = update {
        copy(hiddenFromAppList = hiddenFromAppList.filterNot { k -> k == uniqueKey || k == uniqueKey.substringBefore('/') }.toSet())
    }

    /** Migrates legacy package-keyed hidden entries to uniqueKey-keyed entries */
    fun migrateHiddenApps(installedApps: List<AppItem>) {
        if (installedApps.isEmpty()) return
        update {
            val appByPackage = installedApps.groupBy { it.packageName }
            val newHiddenApps = hiddenApps.flatMap { entry ->
                if (!entry.contains('#') && !entry.contains('/')) {
                    appByPackage[entry]?.map { it.uniqueKey } ?: listOf(entry)
                } else {
                    listOf(entry)
                }
            }.toSet()
            val newHiddenFromAppList = hiddenFromAppList.flatMap { entry ->
                if (!entry.contains('#') && !entry.contains('/')) {
                    appByPackage[entry]?.map { it.uniqueKey } ?: listOf(entry)
                } else {
                    listOf(entry)
                }
            }.toSet()
            copy(hiddenApps = newHiddenApps, hiddenFromAppList = newHiddenFromAppList)
        }
    }

    fun addSearchContact(nickname: String, name: String, phoneNumber: String) = update {
        val list = searchContacts.toMutableList()
        val nick = nickname.trim()
        val idx = list.indexOfFirst { it.nickname.equals(nick, ignoreCase = true) }
        val contact = SearchContact(
            id = if (idx != -1) list[idx].id else UUID.randomUUID().toString(),
            nickname = nick,
            name = name.trim(),
            phoneNumber = phoneNumber.trim()
        )
        if (idx != -1) list[idx] = contact else list.add(contact)
        copy(searchContacts = list)
    }

    fun removeSearchContact(id: String) = update { copy(searchContacts = searchContacts.filterNot { c -> c.id == id }) }

    companion object {
        private const val PREFS_NAME = "pallas_launcher_prefs"
        private const val SECRETS_NAME = "pallas_secrets"
        private const val KEY_SETTINGS_JSON = "settings_json_v1"
        private const val KEY_WOLFRAM_APP_ID = "key_wolfram_app_id"

        @Volatile
        private var instance: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository =
            instance ?: synchronized(this) {
                instance ?: SettingsRepository(context.applicationContext).also { instance = it }
            }

        // ---- legacy per-key preferences (read once by migrateLegacy, then deleted) ----
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
        private const val KEY_SEARCH_CONTACTS = "key_search_contacts"
        private const val KEY_FONT_SCALE = "key_font_scale"
        private const val KEY_SCREENS_FONT_SCALE = "key_screens_font_scale"
        private const val KEY_APP_LIST_FONT_SCALE = "key_app_list_font_scale"
        private const val KEY_CLOCK_PADDING = "key_clock_padding"
        private const val KEY_CLOCK_PADDING_LEFT = "key_clock_padding_left"
        private const val KEY_CLOCK_PADDING_RIGHT = "key_clock_padding_right"
        private const val KEY_CLOCK_PADDING_TOP = "key_clock_padding_top"
        private const val KEY_CLOCK_PADDING_BOTTOM = "key_clock_padding_bottom"
        private const val KEY_SCREENS_PADDING = "key_screens_padding"
        private const val KEY_SCREENS_PADDING_LEFT = "key_screens_padding_left"
        private const val KEY_SCREENS_PADDING_RIGHT = "key_screens_padding_right"
        private const val KEY_SCREENS_PADDING_TOP = "key_screens_padding_top"
        private const val KEY_SCREENS_PADDING_BOTTOM = "key_screens_padding_bottom"
        private const val KEY_NAME_LENGTH_SORT_ORDER = "key_name_length_sort_order"
        private const val KEY_HIDDEN_APPS = "key_hidden_apps"
        private const val KEY_HIDDEN_FROM_APP_LIST = "key_hidden_from_app_list"
        private const val KEY_EDGE_SCRUB_POS = "key_edge_scrub_pos"
        private const val KEY_EDGE_SCRUB_RELEASE = "key_edge_scrub_release"
        private const val KEY_TEXT_SHADOW = "key_text_shadow"
        private const val KEY_BROWSER_PACKAGE = "key_browser_package"
        private const val KEY_WEB_ENGINE = "key_web_engine"
        private const val KEY_COMMAND_CONFIGS = "key_command_configs"
        private const val KEY_DEFAULT_WEATHER_CITY = "key_default_weather_city"
    }
}