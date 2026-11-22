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
import com.pallas.launcher.domain.model.WolframRequestTrigger
import com.pallas.launcher.domain.model.WolframSubmitMode
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
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

    private val VALID_STOCK_RANGES = setOf("1d", "5d", "1mo", "3mo", "6mo", "1y", "2y", "5y", "max")

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) optString(key) else null

    private fun JSONArray.optStringOrNull(index: Int): String? =
        if (index in 0 until length() && !isNull(index)) optString(index) else null

    data class Parsed(
        val settings: LauncherSettings,
        val screensJson: String?,   // raw array, parsed by ScreenRepository (it owns the user-handle mapping)
        val screenCount: Int,
        val appCount: Int,
        val customAppLabels: Map<String, String> = emptyMap(),
        val hasCustomAppLabels: Boolean = false
    )

    sealed interface ParseResult {
        data class Ok(val parsed: Parsed) : ParseResult
        data class Error(val message: String) : ParseResult
    }

    fun export(
        settings: LauncherSettings,
        screens: List<HomeScreen>,
        screenRepository: ScreenRepository,
        customAppLabels: Map<String, String> = emptyMap()
    ): String {
        val root = JSONObject()
        root.put("app", APP_ID)
        root.put("schemaVersion", SCHEMA_VERSION)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("settings", settingsToJson(settings))
        root.put("screens", screenRepository.serializeScreens(screens))
        val labelsObj = JSONObject()
        customAppLabels.toSortedMap().forEach { (k, v) ->
            val key = k.trim()
            val value = v.trim()
            if (key.isNotEmpty() && value.isNotEmpty()) {
                labelsObj.put(key, value)
            }
        }
        root.put("customAppLabels", labelsObj)
        return root.toString(2)
    }

    fun parse(text: String): ParseResult {
        val cleanText = text.trimStart('\uFEFF').trim()
        val root = try {
            JSONObject(cleanText)
        } catch (e: Exception) {
            return ParseResult.Error("This file is not valid JSON.")
        }
        if (root.optStringOrNull("app") != APP_ID) return ParseResult.Error("This is not a Pallas Launcher backup.")
        val version = root.optInt("schemaVersion", -1)
        if (version < 1) return ParseResult.Error("Backup has no valid schema version.")
        if (version > SCHEMA_VERSION) {
            return ParseResult.Error("This backup was made by a newer version of Pallas (schema $version). Update the app and try again.")
        }

        val settingsObj = root.optJSONObject("settings") ?: return ParseResult.Error("Backup contains no settings.")
        val settings = settingsFromJson(settingsObj)

        val screensPresent = root.has("screens") && !root.isNull("screens")
        val screensArr = if (screensPresent) {
            root.optJSONArray("screens") ?: return ParseResult.Error("Invalid screens section in backup.")
        } else {
            null
        }
        var screenCount = 0
        var appCount = 0
        if (screensArr != null) {
            for (i in 0 until screensArr.length()) {
                val screenObj = screensArr.optJSONObject(i) ?: continue
                screenCount++
                val appsArr = screenObj.optJSONArray("apps") ?: continue
                for (j in 0 until appsArr.length()) {
                    val appObj = appsArr.optJSONObject(j) ?: continue
                    if (!appObj.optStringOrNull("packageName").isNullOrBlank()) {
                        appCount++
                    }
                }
            }
        }

        val hasCustomAppLabels = root.has("customAppLabels") && !root.isNull("customAppLabels")
        val customAppLabels = if (hasCustomAppLabels) {
            val labelsObj = root.optJSONObject("customAppLabels")
                ?: return ParseResult.Error("Invalid customAppLabels section in backup.")
            buildMap {
                labelsObj.keys().forEach { k ->
                    val cleanKey = k.trim()
                    val cleanVal = labelsObj.optStringOrNull(k)?.trim()
                    if (cleanKey.isNotEmpty() && !cleanVal.isNullOrEmpty()) {
                        put(cleanKey, cleanVal)
                    }
                }
            }
        } else {
            emptyMap()
        }

        return ParseResult.Ok(
            Parsed(
                settings = settings,
                screensJson = screensArr?.takeIf { screenCount > 0 }?.toString(),
                screenCount = screenCount,
                appCount = appCount,
                customAppLabels = customAppLabels,
                hasCustomAppLabels = hasCustomAppLabels
            )
        )
    }

    // ------------------------------------------------------------------------------------------ settings <-> json

    fun settingsToJson(s: LauncherSettings): JSONObject = JSONObject().apply {
        put("themeMode", s.themeMode.name)
        put("showWallpaperScrim", s.showWallpaperScrim)
        put("wallpaperScrimStrength", s.wallpaperScrimStrength.toDouble())
        put("hideStatusBar", s.hideStatusBar)
        put("textShadowMode", s.textShadowMode.name)

        put("clockVerticalPosition", s.clockVerticalPosition.name)
        put("clockHorizontalAlignment", s.clockHorizontalAlignment.name)
        put("is24HourClock", s.is24HourClock)
        put("showClock", s.showClock)
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
        put("screensAppSpacingDp", s.screensAppSpacingDp)
        put("nameLengthSortOrder", s.nameLengthSortOrder.name)

        put("appFontScale", s.appFontScale.toDouble())
        put("screensFontScale", s.screensFontScale.toDouble())
        put("appListFontScale", s.appListFontScale.toDouble())

        put("swipeLeftAction", GestureAction.toString(s.shortSwipeLeftAction))
        put("swipeRightAction", GestureAction.toString(s.shortSwipeRightAction))
        put("swipeUpAction", GestureAction.toString(s.swipeUpAction))
        put("swipeDownAction", GestureAction.toString(s.swipeDownAction))
        put("swipeDownLeftAction", GestureAction.toString(s.swipeDownLeftAction))
        put("swipeDownRightAction", GestureAction.toString(s.swipeDownRightAction))
        put("wolframSubmitMode", s.wolframSubmitMode.name)
        put("wolframRequestTrigger", s.wolframRequestTrigger.name)
        put("wolframIdleDelaySeconds", s.wolframIdleDelaySeconds)
        put("doubleTapAction", GestureAction.toString(s.doubleTapAction))
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
        put("stockDefaultRange", s.stockDefaultRange)
        put("stockDefaultInterval", s.stockDefaultInterval)
        put("stockShowSparkline", s.stockShowSparkline)
    }

    fun settingsFromJson(o: JSONObject, base: LauncherSettings = LauncherSettings()): LauncherSettings {
        fun <T : Enum<T>> enumOf(key: String, default: T, values: Array<T>): T {
            val raw = o.optStringOrNull(key) ?: return default
            return values.firstOrNull { it.name == raw } ?: default
        }

        fun dp(key: String, default: Int) = o.optInt(key, default).coerceIn(0, 200)
        fun boundedFloat(key: String, default: Float, min: Float, max: Float): Float {
            val raw = o.optDouble(key, default.toDouble())
            val v = if (raw.isNaN() || raw.isInfinite()) default else raw.toFloat()
            return v.coerceIn(min, max)
        }
        fun scale(key: String, default: Float) = boundedFloat(key, default, 0.5f, 2.0f)
        fun strings(key: String, default: Set<String>): Set<String> {
            if (!o.has(key)) return default
            if (o.isNull(key)) return emptySet()
            val arr = o.optJSONArray(key) ?: return default
            return (0 until arr.length()).mapNotNull { arr.optStringOrNull(it)?.takeIf { v -> v.isNotBlank() } }.toSet()
        }

        val aliases = if (!o.has("customAliases")) {
            base.customAliases
        } else if (o.isNull("customAliases")) {
            emptyMap()
        } else {
            val obj = o.optJSONObject("customAliases")
            if (obj == null) base.customAliases
            else buildMap {
                obj.keys().forEach { k ->
                    val key = k.lowercase(Locale.ROOT).trim()
                    val value = obj.optStringOrNull(k)?.trim()
                    if (key.isNotEmpty() && !value.isNullOrEmpty()) {
                        put(key, value)
                    }
                }
            }
        }
        val contacts = if (!o.has("searchContacts")) {
            base.searchContacts
        } else if (o.isNull("searchContacts")) {
            emptyList()
        } else {
            val arr = o.optJSONArray("searchContacts")
            if (arr == null) base.searchContacts
            else buildList {
                for (i in 0 until arr.length()) {
                    val c = arr.optJSONObject(i) ?: continue
                    val nick = c.optStringOrNull("nickname")?.trim().orEmpty()
                    val number = c.optStringOrNull("phoneNumber")?.trim().orEmpty()
                    if (nick.isEmpty() || number.isEmpty()) continue
                    val id = c.optStringOrNull("id")?.trim().orEmpty().ifBlank { UUID.randomUUID().toString() }
                    val name = c.optStringOrNull("name")?.trim().orEmpty()
                    add(SearchContact(id, nick, name, number))
                }
            }
        }
        val commandConfigs = run {
            val obj = o.optJSONObject("commandConfigs") ?: return@run base.commandConfigs
            val merged = CommandConfig.DEFAULTS.toMutableMap()
            val parsed = linkedMapOf<String, Pair<Boolean, String>>()
            val usedPrefixes = mutableSetOf<String>()
            val wsRegex = "\\s".toRegex()

            // Pass 1: sanitize in deterministic DEFAULTS order and reserve defaults kept active.
            CommandConfig.DEFAULTS.forEach { (id, def) ->
                val c = obj.optJSONObject(id)
                val enabled = c?.optBoolean("enabled", def.enabled) ?: def.enabled
                var prefix = c?.optStringOrNull("prefix")?.trim() ?: def.prefix
                if (prefix.isBlank() || wsRegex.containsMatchIn(prefix)) {
                    prefix = def.prefix
                }
                parsed[id] = enabled to prefix
                if (enabled && prefix.equals(def.prefix, ignoreCase = true)) {
                    usedPrefixes.add(def.prefix.lowercase(Locale.ROOT))
                }
            }

            // Pass 2: validate custom overrides in deterministic order and write final configs.
            CommandConfig.DEFAULTS.forEach { (id, def) ->
                val (enabled, rawPrefix) = parsed.getValue(id)
                var finalEnabled = enabled
                var prefix = rawPrefix
                if (finalEnabled) {
                    val key = prefix.lowercase(Locale.ROOT)
                    if (!prefix.equals(def.prefix, ignoreCase = true) && key in usedPrefixes) {
                        val defKey = def.prefix.lowercase(Locale.ROOT)
                        if (defKey !in usedPrefixes) {
                            prefix = def.prefix
                            usedPrefixes.add(defKey)
                        } else {
                            prefix = def.prefix
                            finalEnabled = false
                        }
                    } else {
                        usedPrefixes.add(key)
                    }
                }
                merged[id] = CommandConfig(
                    id = id,
                    label = def.label,
                    enabled = finalEnabled,
                    prefix = prefix
                )
            }
            merged
        }

        val parsedSwipeDown = o.optStringOrNull("swipeDownAction")?.let(GestureAction::fromStringOrNull)
        val stockRangeRaw = o.optStringOrNull("stockDefaultRange")?.trim()?.lowercase(Locale.ROOT)
        val stockRange = if (stockRangeRaw != null && stockRangeRaw in VALID_STOCK_RANGES) stockRangeRaw else base.stockDefaultRange
        val stockIntervalRaw = o.optStringOrNull("stockDefaultInterval")?.trim()
        val stockInterval = if (!stockIntervalRaw.isNullOrEmpty()) stockIntervalRaw else base.stockDefaultInterval

        return base.copy(
            themeMode = enumOf("themeMode", base.themeMode, ThemeMode.entries.toTypedArray()),
            showWallpaperScrim = o.optBoolean("showWallpaperScrim", base.showWallpaperScrim),
            wallpaperScrimStrength = boundedFloat("wallpaperScrimStrength", base.wallpaperScrimStrength, 0f, 1f),
            hideStatusBar = o.optBoolean("hideStatusBar", base.hideStatusBar),
            textShadowMode = enumOf("textShadowMode", base.textShadowMode, TextShadowMode.entries.toTypedArray()),

            clockVerticalPosition = enumOf("clockVerticalPosition", base.clockVerticalPosition, ClockVerticalPosition.entries.toTypedArray()),
            clockHorizontalAlignment = enumOf("clockHorizontalAlignment", base.clockHorizontalAlignment, ClockHorizontalAlignment.entries.toTypedArray()),
            is24HourClock = o.optBoolean("is24HourClock", base.is24HourClock),
            showClock = o.optBoolean("showClock", base.showClock),
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
            screensAppSpacingDp = dp("screensAppSpacingDp", base.screensAppSpacingDp),
            nameLengthSortOrder = enumOf("nameLengthSortOrder", base.nameLengthSortOrder, NameLengthSortOrder.entries.toTypedArray()),

            appFontScale = scale("appFontScale", base.appFontScale),
            screensFontScale = scale("screensFontScale", base.screensFontScale),
            appListFontScale = scale("appListFontScale", base.appListFontScale),

            shortSwipeLeftAction = o.optStringOrNull("swipeLeftAction")?.let(GestureAction::fromStringOrNull) ?: base.shortSwipeLeftAction,
            shortSwipeRightAction = o.optStringOrNull("swipeRightAction")?.let(GestureAction::fromStringOrNull) ?: base.shortSwipeRightAction,
            swipeUpAction = o.optStringOrNull("swipeUpAction")?.let(GestureAction::fromStringOrNull) ?: base.swipeUpAction,
            swipeDownAction = parsedSwipeDown ?: base.swipeDownAction,
            swipeDownLeftAction = o.optStringOrNull("swipeDownLeftAction")?.let(GestureAction::fromStringOrNull)
                ?: parsedSwipeDown ?: base.swipeDownLeftAction,
            swipeDownRightAction = o.optStringOrNull("swipeDownRightAction")?.let(GestureAction::fromStringOrNull)
                ?: parsedSwipeDown ?: base.swipeDownRightAction,
            wolframSubmitMode = enumOf("wolframSubmitMode", base.wolframSubmitMode, WolframSubmitMode.entries.toTypedArray()),
            wolframRequestTrigger = enumOf("wolframRequestTrigger", base.wolframRequestTrigger, WolframRequestTrigger.entries.toTypedArray()),
            wolframIdleDelaySeconds = o.optInt("wolframIdleDelaySeconds", base.wolframIdleDelaySeconds).coerceIn(1, 30),
            doubleTapAction = o.optStringOrNull("doubleTapAction")?.let(GestureAction::fromStringOrNull) ?: base.doubleTapAction,
            edgeScrubPosition = enumOf("edgeScrubPosition", base.edgeScrubPosition, EdgeScrubPosition.entries.toTypedArray()),
            edgeScrubReleaseAction = enumOf("edgeScrubReleaseAction", base.edgeScrubReleaseAction, EdgeScrubReleaseAction.entries.toTypedArray()),

            hiddenApps = strings("hiddenApps", base.hiddenApps),
            hiddenFromAppList = strings("hiddenFromAppList", base.hiddenFromAppList),
            customAliases = aliases,
            searchContacts = contacts,
            webSearchEngine = enumOf("webSearchEngine", base.webSearchEngine, WebSearchEngine.entries.toTypedArray()),
            preferredBrowserPackage = if (o.has("preferredBrowserPackage")) {
                o.optStringOrNull("preferredBrowserPackage")?.trim()?.takeIf { it.isNotEmpty() }
            } else {
                base.preferredBrowserPackage
            },
            commandConfigs = commandConfigs,
            defaultWeatherCity = o.optStringOrNull("defaultWeatherCity")?.trim() ?: base.defaultWeatherCity,
            autoLaunchSingleMatch = o.optBoolean("autoLaunchSingleMatch", base.autoLaunchSingleMatch),
            stockDefaultRange = stockRange,
            stockDefaultInterval = stockInterval,
            stockShowSparkline = o.optBoolean("stockShowSparkline", base.stockShowSparkline)
        )
    }
}
