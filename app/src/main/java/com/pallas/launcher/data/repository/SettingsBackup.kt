package com.pallas.launcher.data.repository

import com.pallas.launcher.domain.model.AppsHorizontalAlignment
import com.pallas.launcher.domain.model.AppsVerticalPosition
import com.pallas.launcher.domain.model.ClockHorizontalAlignment
import com.pallas.launcher.domain.model.ClockVerticalPosition
import com.pallas.launcher.domain.model.CommandConfig
import com.pallas.launcher.domain.model.EdgeScrubPosition
import com.pallas.launcher.domain.model.EdgeScrubReleaseAction
import com.pallas.launcher.domain.model.GestureAction
import com.pallas.launcher.domain.model.HomeScreen
import com.pallas.launcher.domain.model.LauncherSettings
import com.pallas.launcher.domain.model.NameLengthSortOrder
import com.pallas.launcher.domain.model.SearchContact
import com.pallas.launcher.domain.model.TextShadowMode
import com.pallas.launcher.domain.model.ThemeMode
import com.pallas.launcher.domain.model.WebSearchEngine
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Launcher backup as a single, human-readable JSON document:
 *
 * ```
 * { "app": "pallas-launcher", "schemaVersion": 1, "exportedAt": 1760000000000,
 *   "settings": { ... every LauncherSettings field ... },
 *   "screens":  [ { id, name, contextTag, apps: [ ... ] } ] }
 * ```
 *
 * Import is deliberately forgiving: missing keys fall back to defaults, unknown enum values fall back to defaults and
 * out-of-range numbers are clamped, so a hand-edited or older/newer file can never leave the launcher in a broken state.
 * App usage history (launch counts) is intentionally NOT part of a backup - it is device-local.
 */
object SettingsBackup {
    const val APP_ID = "pallas-launcher"
    const val SCHEMA_VERSION = 1

    data class Parsed(
        val settings: LauncherSettings,
        val screensJson: String?,   // raw array, parsed by ScreenRepository (it owns the user-handle mapping)
        val screenCount: Int,
        val appCount: Int
    )

    sealed interface ParseResult {
        data class Ok(val parsed: Parsed) : ParseResult
        data class Error(val message: String) : ParseResult
    }

    fun export(settings: LauncherSettings, screens: List<HomeScreen>, screenRepository: ScreenRepository): String {
        val root = JSONObject()
        root.put("app", APP_ID)
        root.put("schemaVersion", SCHEMA_VERSION)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("settings", settingsToJson(settings))
        root.put("screens", screenRepository.serializeScreens(screens))
        return root.toString(2)
    }

    fun parse(text: String): ParseResult {
        val root = try {
            JSONObject(text.trim())
        } catch (e: Exception) {
            return ParseResult.Error("This file is not valid JSON.")
        }
        if (root.optString("app") != APP_ID) return ParseResult.Error("This is not a Pallas Launcher backup.")
        val version = root.optInt("schemaVersion", -1)
        if (version < 1) return ParseResult.Error("Backup has no valid schema version.")
        if (version > SCHEMA_VERSION) {
            return ParseResult.Error("This backup was made by a newer version of Pallas (schema $version). Update the app and try again.")
        }

        val settingsObj = root.optJSONObject("settings") ?: return ParseResult.Error("Backup contains no settings.")
        val settings = settingsFromJson(settingsObj)

        val screensArr = root.optJSONArray("screens")
        var appCount = 0
        if (screensArr != null) {
            for (i in 0 until screensArr.length()) {
                appCount += screensArr.optJSONObject(i)?.optJSONArray("apps")?.length() ?: 0
            }
        }
        return ParseResult.Ok(
            Parsed(
                settings = settings,
                screensJson = screensArr?.takeIf { it.length() > 0 }?.toString(),
                screenCount = screensArr?.length() ?: 0,
                appCount = appCount
            )
        )
    }

    // ------------------------------------------------------------------------------------------ settings <-> json

    fun settingsToJson(s: LauncherSettings): JSONObject = JSONObject().apply {
        put("themeMode", s.themeMode.name)
        put("showWallpaperScrim", s.showWallpaperScrim)
        put("hideStatusBar", s.hideStatusBar)
        put("textShadowMode", s.textShadowMode.name)

        put("clockVerticalPosition", s.clockVerticalPosition.name)
        put("clockHorizontalAlignment", s.clockHorizontalAlignment.name)
        put("is24HourClock", s.is24HourClock)
        put("showDate", s.showDate)
        put("showBattery", s.showBattery)
        put("clockPaddingDp", s.clockPaddingDp)
        put("clockPaddingLeftDp", s.clockPaddingLeftDp)
        put("clockPaddingRightDp", s.clockPaddingRightDp)
        put("clockPaddingTopDp", s.clockPaddingTopDp)
        put("clockPaddingBottomDp", s.clockPaddingBottomDp)

        put("appsVerticalPosition", s.appsVerticalPosition.name)
        put("appsHorizontalAlignment", s.appsHorizontalAlignment.name)
        put("screensPaddingDp", s.screensPaddingDp)
        put("screensPaddingLeftDp", s.screensPaddingLeftDp)
        put("screensPaddingRightDp", s.screensPaddingRightDp)
        put("screensPaddingTopDp", s.screensPaddingTopDp)
        put("screensPaddingBottomDp", s.screensPaddingBottomDp)
        put("nameLengthSortOrder", s.nameLengthSortOrder.name)

        put("appFontScale", s.appFontScale.toDouble())
        put("screensFontScale", s.screensFontScale.toDouble())
        put("appListFontScale", s.appListFontScale.toDouble())

        put("swipeLeftAction", GestureAction.toString(s.shortSwipeLeftAction))
        put("swipeRightAction", GestureAction.toString(s.shortSwipeRightAction))
        put("swipeUpAction", GestureAction.toString(s.swipeUpAction))
        put("swipeDownAction", GestureAction.toString(s.swipeDownAction))
        put("edgeScrubPosition", s.edgeScrubPosition.name)
        put("edgeScrubReleaseAction", s.edgeScrubReleaseAction.name)

        put("hiddenApps", JSONArray(s.hiddenApps.sorted()))
        put("hiddenFromAppList", JSONArray(s.hiddenFromAppList.sorted()))

        put("customAliases", JSONObject().also { o -> s.customAliases.toSortedMap().forEach { (k, v) -> o.put(k, v) } })
        put("searchContacts", JSONArray().also { arr ->
            s.searchContacts.forEach { c ->
                arr.put(JSONObject().put("id", c.id).put("nickname", c.nickname).put("name", c.name).put("phoneNumber", c.phoneNumber))
            }
        })
        put("webSearchEngine", s.webSearchEngine.name)
        if (s.preferredBrowserPackage != null) put("preferredBrowserPackage", s.preferredBrowserPackage)
        put("commandConfigs", JSONObject().also { o ->
            s.commandConfigs.forEach { (id, c) ->
                o.put(id, JSONObject().put("enabled", c.enabled).put("prefix", c.prefix))
            }
        })
        put("defaultWeatherCity", s.defaultWeatherCity)
        put("autoLaunchSingleMatch", s.autoLaunchSingleMatch)
    }

    fun settingsFromJson(o: JSONObject, base: LauncherSettings = LauncherSettings()): LauncherSettings {
        fun <T : Enum<T>> enumOf(key: String, default: T, values: Array<T>): T =
            values.firstOrNull { it.name == o.optString(key) } ?: default

        fun dp(key: String, default: Int) = o.optInt(key, default).coerceIn(0, 200)
        fun scale(key: String, default: Float) = o.optDouble(key, default.toDouble()).toFloat().coerceIn(0.5f, 2.0f)
        fun strings(key: String): Set<String> {
            val arr = o.optJSONArray(key) ?: return emptySet()
            return (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { v -> v.isNotBlank() } }.toSet()
        }

        val aliases = buildMap {
            o.optJSONObject("customAliases")?.let { obj ->
                obj.keys().forEach { k -> obj.optString(k).takeIf { it.isNotBlank() }?.let { put(k, it) } }
            }
        }
        val contacts = buildList {
            val arr = o.optJSONArray("searchContacts") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val c = arr.optJSONObject(i) ?: continue
                val nick = c.optString("nickname").trim()
                val number = c.optString("phoneNumber").trim()
                if (nick.isEmpty() || number.isEmpty()) continue
                add(SearchContact(c.optString("id").ifBlank { UUID.randomUUID().toString() }, nick, c.optString("name"), number))
            }
        }
        val commandConfigs = run {
            val obj = o.optJSONObject("commandConfigs") ?: return@run base.commandConfigs
            val merged = CommandConfig.DEFAULTS.toMutableMap()
            obj.keys().forEach { id ->
                val c = obj.optJSONObject(id) ?: return@forEach
                val def = CommandConfig.DEFAULTS[id]
                merged[id] = CommandConfig(
                    id = id,
                    label = def?.label ?: id,
                    enabled = c.optBoolean("enabled", def?.enabled ?: true),
                    prefix = c.optString("prefix", def?.prefix ?: id)
                )
            }
            merged
        }

        return base.copy(
            themeMode = enumOf("themeMode", base.themeMode, ThemeMode.entries.toTypedArray()),
            showWallpaperScrim = o.optBoolean("showWallpaperScrim", base.showWallpaperScrim),
            hideStatusBar = o.optBoolean("hideStatusBar", base.hideStatusBar),
            textShadowMode = enumOf("textShadowMode", base.textShadowMode, TextShadowMode.entries.toTypedArray()),

            clockVerticalPosition = enumOf("clockVerticalPosition", base.clockVerticalPosition, ClockVerticalPosition.entries.toTypedArray()),
            clockHorizontalAlignment = enumOf("clockHorizontalAlignment", base.clockHorizontalAlignment, ClockHorizontalAlignment.entries.toTypedArray()),
            is24HourClock = o.optBoolean("is24HourClock", base.is24HourClock),
            showDate = o.optBoolean("showDate", base.showDate),
            showBattery = o.optBoolean("showBattery", base.showBattery),
            clockPaddingDp = dp("clockPaddingDp", base.clockPaddingDp),
            clockPaddingLeftDp = dp("clockPaddingLeftDp", base.clockPaddingLeftDp),
            clockPaddingRightDp = dp("clockPaddingRightDp", base.clockPaddingRightDp),
            clockPaddingTopDp = dp("clockPaddingTopDp", base.clockPaddingTopDp),
            clockPaddingBottomDp = dp("clockPaddingBottomDp", base.clockPaddingBottomDp),

            appsVerticalPosition = enumOf("appsVerticalPosition", base.appsVerticalPosition, AppsVerticalPosition.entries.toTypedArray()),
            appsHorizontalAlignment = enumOf("appsHorizontalAlignment", base.appsHorizontalAlignment, AppsHorizontalAlignment.entries.toTypedArray()),
            screensPaddingDp = dp("screensPaddingDp", base.screensPaddingDp),
            screensPaddingLeftDp = dp("screensPaddingLeftDp", base.screensPaddingLeftDp),
            screensPaddingRightDp = dp("screensPaddingRightDp", base.screensPaddingRightDp),
            screensPaddingTopDp = dp("screensPaddingTopDp", base.screensPaddingTopDp),
            screensPaddingBottomDp = dp("screensPaddingBottomDp", base.screensPaddingBottomDp),
            nameLengthSortOrder = enumOf("nameLengthSortOrder", base.nameLengthSortOrder, NameLengthSortOrder.entries.toTypedArray()),

            appFontScale = scale("appFontScale", base.appFontScale),
            screensFontScale = scale("screensFontScale", base.screensFontScale),
            appListFontScale = scale("appListFontScale", base.appListFontScale),

            shortSwipeLeftAction = o.optString("swipeLeftAction").takeIf { it.isNotBlank() }?.let(GestureAction::fromString) ?: base.shortSwipeLeftAction,
            shortSwipeRightAction = o.optString("swipeRightAction").takeIf { it.isNotBlank() }?.let(GestureAction::fromString) ?: base.shortSwipeRightAction,
            swipeUpAction = o.optString("swipeUpAction").takeIf { it.isNotBlank() }?.let(GestureAction::fromString) ?: base.swipeUpAction,
            swipeDownAction = o.optString("swipeDownAction").takeIf { it.isNotBlank() }?.let(GestureAction::fromString) ?: base.swipeDownAction,
            edgeScrubPosition = enumOf("edgeScrubPosition", base.edgeScrubPosition, EdgeScrubPosition.entries.toTypedArray()),
            edgeScrubReleaseAction = enumOf("edgeScrubReleaseAction", base.edgeScrubReleaseAction, EdgeScrubReleaseAction.entries.toTypedArray()),

            hiddenApps = strings("hiddenApps"),
            hiddenFromAppList = strings("hiddenFromAppList"),
            customAliases = aliases,
            searchContacts = contacts,
            webSearchEngine = enumOf("webSearchEngine", base.webSearchEngine, WebSearchEngine.entries.toTypedArray()),
            preferredBrowserPackage = o.optString("preferredBrowserPackage").takeIf { it.isNotBlank() },
            commandConfigs = commandConfigs,
            defaultWeatherCity = o.optString("defaultWeatherCity", base.defaultWeatherCity),
            autoLaunchSingleMatch = o.optBoolean("autoLaunchSingleMatch", base.autoLaunchSingleMatch)
        )
    }
}
