package com.pallas.launcher.domain.search

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import com.pallas.launcher.data.repository.QuickCaptureRepository
import com.pallas.launcher.domain.model.CommandConfig
import com.pallas.launcher.domain.model.FocusProfile
import com.pallas.launcher.domain.model.LauncherSettings
import com.pallas.launcher.domain.model.MacroRoutine
import com.pallas.launcher.domain.model.SearchContact
import com.pallas.launcher.domain.model.WebSearchEngine
import com.pallas.launcher.platform.SystemActionsManager
import java.util.Locale

data class ParsedAlarm(val hour: Int, val minute: Int)
data class ParsedTimer(val totalSeconds: Int, val formattedDuration: String)

data class WebSearchConfig(
    val engine: WebSearchEngine = WebSearchEngine.GOOGLE,
    val browserPackage: String? = null,
    val browserLabel: String? = null
)

sealed interface AsyncCommandRequest {
    data class Finance(val query: String) : AsyncCommandRequest
    data class Weather(val city: String, val isSolarOnly: Boolean) : AsyncCommandRequest
    data class Wolfram(val query: String) : AsyncCommandRequest
    data class Wiki(val query: String) : AsyncCommandRequest
    data class Define(val word: String) : AsyncCommandRequest
    data class MathPlot(val rawInput: String, val prefix: String) : AsyncCommandRequest
    data class LocalFiles(val query: String) : AsyncCommandRequest
    data object CalendarAgenda : AsyncCommandRequest
}

data class CommandEvaluation(
    val immediateResults: List<LauncherSearchResult>,
    val asyncRequest: AsyncCommandRequest? = null
)

object SmartCommandEngine {

    fun parseAlarm(query: String): ParsedAlarm? {
        val trimmed = query.trim().lowercase(Locale.ROOT)
        val verb = when {
            trimmed == "al" || trimmed == "alarm" -> return null
            trimmed.startsWith("alarm ") -> "alarm"
            trimmed.startsWith("al ") -> "al"
            trimmed.startsWith("alarm") && trimmed.length > 5 && trimmed[5].isDigit() -> "alarm"
            trimmed.startsWith("al") && trimmed.length > 2 && trimmed[2].isDigit() -> "al"
            else -> return null
        }

        val rawArg = trimmed.removePrefix(verb).trim()
        if (rawArg.isEmpty()) return null

        return parseAlarmTime(rawArg)
    }

    fun parseAlarmTime(rawArg: String): ParsedAlarm? {
        var clean = rawArg.trim().lowercase(Locale.ROOT).replace("\\s+".toRegex(), "")
        if (clean.isEmpty()) return null

        var isPm: Boolean? = null
        if (clean.endsWith("am")) {
            isPm = false
            clean = clean.removeSuffix("am")
        } else if (clean.endsWith("pm")) {
            isPm = true
            clean = clean.removeSuffix("pm")
        }

        var hour = -1
        var minute = -1

        if (clean.contains(":")) {
            val parts = clean.split(":")
            if (parts.size == 2) {
                hour = parts[0].toIntOrNull() ?: return null
                minute = parts[1].toIntOrNull() ?: return null
            } else return null
        } else if (clean.all { it.isDigit() }) {
            when (clean.length) {
                1, 2 -> {
                    hour = clean.toInt()
                    minute = 0
                }
                3 -> {
                    hour = clean.substring(0, 1).toInt()
                    minute = clean.substring(1, 3).toInt()
                }
                4 -> {
                    hour = clean.substring(0, 2).toInt()
                    minute = clean.substring(2, 4).toInt()
                }
                else -> return null
            }
        } else {
            return null
        }

        if (minute !in 0..59) return null

        if (isPm == true) {
            if (hour in 1..11) hour += 12
            else if (hour != 12) return null
        } else if (isPm == false) {
            if (hour == 12) hour = 0
            else if (hour !in 0..11) return null
        }

        if (hour !in 0..23) return null
        return ParsedAlarm(hour, minute)
    }

    fun parseTimer(query: String): ParsedTimer? {
        val trimmed = query.trim().lowercase(Locale.ROOT)
        if (trimmed == "timer") return null
        val verb = when {
            trimmed.startsWith("timer ") -> "timer"
            trimmed.startsWith("timer") && trimmed.length > 5 && trimmed[5].isDigit() -> "timer"
            else -> return null
        }

        val rawArg = trimmed.removePrefix(verb).trim()
        if (rawArg.isEmpty()) return null

        return parseTimerDuration(rawArg)
    }

    const val MAX_TIMER_SECONDS = 24L * 3600L

    private val PLAIN_NUMBER = Regex("""\d{1,5}""")
    private val COLON_FORM = Regex("""(\d{1,3}):(\d{1,2})(?::(\d{1,2}))?""")
    private val UNIT_TOKEN = Regex("""(\d{1,6})\s*(hours?|hrs?|h|minutes?|mins?|m|seconds?|secs?|s)""")

    fun parseTimerDuration(rawArg: String): ParsedTimer? {
        val text = rawArg.trim().lowercase(Locale.ROOT)
        if (text.isEmpty()) return null

        var hours = 0L
        var minutes = 0L
        var seconds = 0L

        val colon = COLON_FORM.matchEntire(text)
        if (colon != null) {
            val a = colon.groupValues[1].toLong()
            val b = colon.groupValues[2].toLong()
            val c = colon.groupValues[3]
            if (b >= 60) return null
            hours = a
            minutes = b
            if (c.isNotEmpty()) {
                seconds = c.toLong()
                if (seconds >= 60) return null
            }
        } else if (PLAIN_NUMBER.matches(text)) {
            minutes = text.toLong()
        } else {
            var pos = 0
            var seenH = false
            var seenM = false
            var seenS = false
            while (pos < text.length) {
                while (pos < text.length && text[pos].isWhitespace()) pos++
                if (pos >= text.length) break
                val m = UNIT_TOKEN.matchAt(text, pos) ?: return null
                val value = m.groupValues[1].toLong()
                when (m.groupValues[2][0]) {
                    'h' -> { if (seenH) return null; seenH = true; hours = value }
                    'm' -> { if (seenM) return null; seenM = true; minutes = value }
                    's' -> { if (seenS) return null; seenS = true; seconds = value }
                    else -> return null
                }
                pos = m.range.last + 1
            }
            if (!seenH && !seenM && !seenS) return null
        }

        val total = hours * 3600L + minutes * 60L + seconds
        if (total <= 0L || total > MAX_TIMER_SECONDS) return null
        val t = total.toInt()
        return ParsedTimer(t, formatDuration(t / 3600, (t % 3600) / 60, t % 60))
    }

    fun parseWebQuery(query: String): String? {
        val trimmed = query.trim()
        val lower = trimmed.lowercase(Locale.ROOT)
        val verbLength = when {
            lower.startsWith("query ") -> 5
            lower.startsWith("q ") -> 1
            else -> return null
        }
        return trimmed.substring(verbLength).trim().takeIf { it.isNotEmpty() }
    }

    private fun formatDuration(hours: Int, minutes: Int, seconds: Int): String {
        val parts = mutableListOf<String>()
        if (hours > 0) parts.add("$hours hr${if (hours > 1) "s" else ""}")
        if (minutes > 0) parts.add("$minutes min${if (minutes > 1) "s" else ""}")
        if (seconds > 0) parts.add("$seconds sec")
        return if (parts.isEmpty()) "0 sec" else parts.joinToString(" ")
    }

    fun formatAlarmTime(alarm: ParsedAlarm): String {
        val amPm = if (alarm.hour < 12) "AM" else "PM"
        val hour12 = if (alarm.hour % 12 == 0) 12 else alarm.hour % 12
        val minStr = String.format(Locale.getDefault(), "%02d", alarm.minute)
        return "$hour12:$minStr $amPm"
    }

    private fun matchPrefixArg(input: String, prefix: String, allowDigitGlue: Boolean = false): String? {
        val p = prefix.trim().lowercase(Locale.ROOT)
        if (p.isEmpty()) return null
        val lower = input.lowercase(Locale.ROOT)
        if (lower == p) return ""
        if (lower.startsWith("$p ")) return input.substring(p.length + 1).trim()
        val isSymbolPrefix = p.all { !it.isLetterOrDigit() }
        if (lower.startsWith(p) && input.length > p.length) {
            val nextChar = input[p.length]
            if (isSymbolPrefix || (allowDigitGlue && nextChar.isDigit())) {
                return input.substring(p.length).trim()
            }
        }
        return null
    }

    private fun hasUnescapedToken(text: String, token: String): Boolean {
        var idx = text.indexOf(token)
        while (idx >= 0) {
            var backslashes = 0
            var pos = idx - 1
            while (pos >= 0 && text[pos] == '\\') {
                backslashes++
                pos--
            }
            if (backslashes % 2 == 0) return true
            idx = text.indexOf(token, idx + token.length)
        }
        return false
    }

    fun normalizeUserLatex(raw: String): String {
        var s = raw.trim()
        if (s.isEmpty()) return ""

        val beginDoc = "\\begin{document}"
        val endDoc = "\\end{document}"
        val beginIdx = s.indexOf(beginDoc)
        if (beginIdx >= 0) {
            val endIdx = s.lastIndexOf(endDoc)
            if (endIdx > beginIdx) {
                s = s.substring(beginIdx + beginDoc.length, endIdx).trim()
            }
        }

        var changed = true
        while (changed) {
            changed = false
            s = s.trim()
            when {
                s.startsWith("$$") && s.endsWith("$$") && s.length >= 4 -> {
                    val inner = s.substring(2, s.length - 2)
                    if (!hasUnescapedToken(inner, "$$")) {
                        s = inner.trim()
                        changed = true
                    }
                }
                s.startsWith("\\[") && s.endsWith("\\]") && s.length >= 4 -> {
                    val inner = s.substring(2, s.length - 2)
                    if (!hasUnescapedToken(inner, "\\]")) {
                        s = inner.trim()
                        changed = true
                    }
                }
                s.startsWith("\\(") && s.endsWith("\\)") && s.length >= 4 -> {
                    val inner = s.substring(2, s.length - 2)
                    if (!hasUnescapedToken(inner, "\\)")) {
                        s = inner.trim()
                        changed = true
                    }
                }
                s.startsWith("$") && !s.startsWith("$$") && s.endsWith("$") && !s.endsWith("$$") && s.length >= 2 -> {
                    val inner = s.substring(1, s.length - 1)
                    if (!hasUnescapedToken(inner, "$")) {
                        s = inner.trim()
                        changed = true
                    }
                }
            }
        }
        return s.trim()
    }

    fun evaluateCommands(
        rawQuery: String,
        contacts: List<SearchContact>,
        webSearch: WebSearchConfig = WebSearchConfig(),
        configs: Map<String, CommandConfig> = CommandConfig.ALL_ENABLED,
        defaultWeatherCity: String = LauncherSettings.DEFAULT_WEATHER_CITY,
        deviceContacts: List<SearchContact> = emptyList(),
        is24HourClock: Boolean = false,
        dateArithmeticEnabled: Boolean = true,
        colorBaseInspectorEnabled: Boolean = true,
        quickCaptureCount: Int = 0,
        quickCapturePreview: String = "",
        focusProfiles: List<FocusProfile> = emptyList(),
        defaultFocusProfileId: String? = null,
        isFocusActive: Boolean = false,
        activeFocusProfile: FocusProfile? = null,
        focusAllowedAppKeys: Set<String>? = null,
        macros: List<MacroRoutine> = emptyList(),
        isDndEnabled: Boolean = false,
        hasCalendarPermission: Boolean = true,
        hasFileSearchPermission: Boolean = true
    ): CommandEvaluation {
        val trimmed = rawQuery.trim()
        if (trimmed.isEmpty()) return CommandEvaluation(emptyList())
        val lower = trimmed.lowercase(Locale.ROOT)
        val results = mutableListOf<LauncherSearchResult>()
        var asyncReq: AsyncCommandRequest? = null

        fun cfg(id: String): CommandConfig {
            val base = configs[id] ?: CommandConfig.DEFAULTS.getValue(id)
            val disabledByFocus = activeFocusProfile != null &&
                id in activeFocusProfile.disabledCommands &&
                id != "settings"
            return if (disabledByFocus) base.copy(enabled = false) else base
        }
        val fallbackCity = defaultWeatherCity.trim().ifEmpty { LauncherSettings.DEFAULT_WEATHER_CITY }

        // -0.5 Focus Mode, Macros, Instant Hardware Toggles, Calendar Agenda & Local Files
        FocusCommandParser.parse(trimmed, focusProfiles, defaultFocusProfileId)?.let { focusCmd ->
            when (focusCmd) {
                is FocusCommand.TurnOff -> {
                    results.add(
                        LauncherSearchResult.Command(
                            id = FOCUS_OFF_COMMAND_ID,
                            title = "End Focus Mode",
                            subtitle = "Restore apps & Do Not Disturb",
                            type = SmartCommandType.FOCUS,
                            execute = {}
                        )
                    )
                }
                is FocusCommand.NoProfilesConfigured -> {
                    results.add(
                        LauncherSearchResult.Command(
                            id = FOCUS_SETUP_COMMAND_ID,
                            title = "Set up Focus Mode",
                            subtitle = "Create a profile in Settings → Focus Mode",
                            type = SmartCommandType.FOCUS,
                            execute = {}
                        )
                    )
                }
                is FocusCommand.TurnOn -> {
                    val profile = focusCmd.profile
                    if (profile != null) {
                        val secs = focusCmd.durationSeconds
                        val durLabel = if (secs != null && secs > 0) {
                            formatDuration(secs / 3600, (secs % 3600) / 60, secs % 60)
                        } else {
                            "until ended"
                        }
                        val willDnd = focusCmd.dndOverride ?: profile.syncDnd
                        val dndSuffix = when {
                            focusCmd.dndOverride == true -> " + DND"
                            focusCmd.dndOverride == false -> " (no DND)"
                            willDnd -> " • Syncs DND"
                            else -> ""
                        }
                        val dndToken = focusCmd.dndOverride?.toString() ?: "auto"
                        results.add(
                            LauncherSearchResult.Command(
                                id = "${FOCUS_ON_COMMAND_ID}:${profile.id}:${secs ?: 0}:$dndToken",
                                title = "Focus: ${profile.name} ($durLabel)",
                                subtitle = "Hide distracting apps$dndSuffix",
                                type = SmartCommandType.FOCUS,
                                execute = {}
                            )
                        )
                    }
                }
            }
        }

        if (macros.isNotEmpty() && !trimmed.startsWith("@") && !trimmed.startsWith("#") && !trimmed.startsWith("=")) {
            val matchedMacros = macros.filter { m ->
                val trig = m.triggerWord.trim()
                trig.equals(lower, ignoreCase = true) ||
                    (lower.length >= 2 && trig.startsWith(lower, ignoreCase = true))
            }.sortedByDescending { it.triggerWord.equals(lower, ignoreCase = true) }
            for (macro in matchedMacros) {
                results.add(
                    LauncherSearchResult.Command(
                        id = "$MACRO_COMMAND_PREFIX${macro.id}",
                        title = macro.name,
                        subtitle = "Macro '${macro.triggerWord}' • ${macro.steps.size} step${if (macro.steps.size == 1) "" else "s"}",
                        type = SmartCommandType.MACRO,
                        execute = {}
                    )
                )
            }
        }

        val flCfg = cfg("flashlight")
        if (flCfg.enabled) {
            val flArg = matchPrefixArg(trimmed, flCfg.prefix)
                ?: matchPrefixArg(trimmed, "flashlight")
                ?: matchPrefixArg(trimmed, "torch")
            if (flArg != null) {
                when (flArg.lowercase(Locale.ROOT)) {
                    "" -> results.add(
                        LauncherSearchResult.Command(
                            id = "cmd_hw_flashlight_toggle",
                            title = "Toggle Flashlight",
                            subtitle = "Hardware utility",
                            type = SmartCommandType.HARDWARE,
                            execute = { SystemActionsManager.toggleTorch(it) }
                        )
                    )
                    "on" -> results.add(
                        LauncherSearchResult.Command(
                            id = "cmd_hw_flashlight_on",
                            title = "Turn Flashlight ON",
                            subtitle = "Hardware utility",
                            type = SmartCommandType.HARDWARE,
                            execute = { SystemActionsManager.setTorch(it, true) }
                        )
                    )
                    "off" -> results.add(
                        LauncherSearchResult.Command(
                            id = "cmd_hw_flashlight_off",
                            title = "Turn Flashlight OFF",
                            subtitle = "Hardware utility",
                            type = SmartCommandType.HARDWARE,
                            execute = { SystemActionsManager.setTorch(it, false) }
                        )
                    )
                }
            }
        }

        val dndCfg = cfg("dnd")
        if (dndCfg.enabled) {
            val dndArg = matchPrefixArg(trimmed, dndCfg.prefix)
            if (dndArg != null) {
                when (dndArg.lowercase(Locale.ROOT)) {
                    "" -> results.add(
                        LauncherSearchResult.Command(
                            id = "cmd_hw_dnd_toggle",
                            title = if (isDndEnabled) "Turn Do Not Disturb OFF" else "Turn Do Not Disturb ON",
                            subtitle = "System interruption filter",
                            type = SmartCommandType.HARDWARE,
                            execute = { SystemActionsManager.toggleDnd(it) }
                        )
                    )
                    "on" -> results.add(
                        LauncherSearchResult.Command(
                            id = "cmd_hw_dnd_on",
                            title = "Turn Do Not Disturb ON",
                            subtitle = "System interruption filter",
                            type = SmartCommandType.HARDWARE,
                            execute = { SystemActionsManager.setDnd(it, true) }
                        )
                    )
                    "off" -> results.add(
                        LauncherSearchResult.Command(
                            id = "cmd_hw_dnd_off",
                            title = "Turn Do Not Disturb OFF",
                            subtitle = "System interruption filter",
                            type = SmartCommandType.HARDWARE,
                            execute = { SystemActionsManager.setDnd(it, false) }
                        )
                    )
                }
            }
        }

        val calCfg = cfg("calendar")
        if (calCfg.enabled) {
            val isShowCal = lower == "show cal" || lower == "agenda" ||
                matchPrefixArg(trimmed, calCfg.prefix)?.isEmpty() == true
            if (isShowCal) {
                if (!hasCalendarPermission) {
                    results.add(
                        LauncherSearchResult.Command(
                            id = CALENDAR_GRANT_PERMISSION_ID,
                            title = "Show Calendar Agenda",
                            subtitle = "Tap to grant Calendar permission",
                            type = SmartCommandType.CALENDAR,
                            execute = {}
                        )
                    )
                } else {
                    results.add(
                        LauncherSearchResult.RichCard(
                            id = "cal_loading",
                            type = SmartCommandType.CALENDAR,
                            title = "Calendar Agenda",
                            subtitle = "loading this week's events…",
                            isLoading = true
                        )
                    )
                    if (asyncReq == null) {
                        asyncReq = AsyncCommandRequest.CalendarAgenda
                    }
                }
            }
        }

        var matchedFilesPrefix = false
        val filesCfg = cfg("files")
        if (filesCfg.enabled) {
            val fileArg: String? = if (filesCfg.prefix == "#") {
                if (trimmed.startsWith("#") && !ColorAndBaseInspector.isHexColor(trimmed)) {
                    trimmed.drop(1).trim()
                } else null
            } else {
                matchPrefixArg(trimmed, filesCfg.prefix)
            }
            if (fileArg != null) {
                matchedFilesPrefix = true
                if (!hasFileSearchPermission) {
                    results.add(
                        LauncherSearchResult.Command(
                            id = FILES_GRANT_PERMISSION_ID,
                            title = "Grant File Access",
                            subtitle = "Allow Pallas to search documents, downloads & media",
                            type = SmartCommandType.FILE,
                            execute = {}
                        )
                    )
                } else if (fileArg.isEmpty()) {
                    results.add(
                        LauncherSearchResult.Command(
                            id = "cmd_files_hint",
                            title = "Local File Search",
                            subtitle = "Type ${filesCfg.prefix} followed by a filename",
                            type = SmartCommandType.FILE,
                            execute = {}
                        )
                    )
                } else {
                    results.add(
                        LauncherSearchResult.RichCard(
                            id = "files_loading_$fileArg",
                            type = SmartCommandType.FILE,
                            title = "Files matching \"$fileArg\"",
                            subtitle = "searching MediaStore…",
                            isLoading = true
                        )
                    )
                    if (asyncReq == null) {
                        asyncReq = AsyncCommandRequest.LocalFiles(fileArg)
                    }
                }
            }
        }

        var matchedAppSearch = false
        val appSearchCfg = cfg("app_search")
        if (appSearchCfg.enabled) {
            val appArg = if (appSearchCfg.prefix == "!") {
                if (trimmed.startsWith("!")) trimmed.drop(1).trim() else null
            } else {
                matchPrefixArg(trimmed, appSearchCfg.prefix)
            }
            if (appArg != null) {
                matchedAppSearch = true
                if (appArg.isEmpty()) {
                    results.add(
                        LauncherSearchResult.Command(
                            id = "cmd_app_search_hint",
                            title = "Direct App Search",
                            subtitle = "Try ${appSearchCfg.prefix}yt, ${appSearchCfg.prefix}az, ${appSearchCfg.prefix}sp, ${appSearchCfg.prefix}gm, ${appSearchCfg.prefix}play",
                            type = SmartCommandType.APP_SEARCH,
                            execute = {}
                        )
                    )
                } else {
                    val parsed = DirectAppSearchEngine.parse(appArg)
                    if (parsed != null) {
                        val isAllowedInFocus = focusAllowedAppKeys == null || parsed.target.packages.any { pkg ->
                            pkg in focusAllowedAppKeys || focusAllowedAppKeys.any { k ->
                                k == pkg || k.startsWith("$pkg/") || k.startsWith("$pkg#")
                            }
                        }
                        if (isAllowedInFocus) {
                            val title = if (parsed.query.isEmpty()) {
                                "Search in ${parsed.target.displayName}"
                            } else {
                                "Search ${parsed.target.displayName} for \"${parsed.query}\""
                            }
                            results.add(
                                LauncherSearchResult.Command(
                                    id = "cmd_app_search_${parsed.target.aliases.first()}_${parsed.query}",
                                    title = title,
                                    subtitle = "Open in ${parsed.target.displayName} app",
                                    type = SmartCommandType.APP_SEARCH,
                                    execute = { ctx -> DirectAppSearchEngine.launch(ctx, parsed.target, parsed.query) }
                                )
                            )
                        }
                    } else {
                        results.add(
                            LauncherSearchResult.Command(
                                id = "cmd_app_search_hint",
                                title = "Direct App Search",
                                subtitle = "Apps: yt, az, sp, gm, play, rd, gh, x",
                                type = SmartCommandType.APP_SEARCH,
                                execute = {}
                            )
                        )
                    }
                }
            }
        }

        // 0. Smart Unit & Currency Converter (evaluated before Math so `100 km -> mi` or `= 100 usd to inr` doesn't hit Wolfram RuleVal)
        var matchedConvert = false
        val convCfg = cfg("convert")
        if (convCfg.enabled) {
            val convArg = matchPrefixArg(trimmed, convCfg.prefix)
                ?: matchPrefixArg(trimmed, "convert")
                ?: if (convCfg.prefix.equals("convert", ignoreCase = true)) matchPrefixArg(trimmed, "conv") else null
            if (convArg != null) {
                if (convArg.isEmpty()) {
                    results.add(
                        LauncherSearchResult.Command(
                            id = "cmd_convert_hint",
                            title = "Unit & Currency Converter",
                            subtitle = "",
                            type = SmartCommandType.CONVERT,
                            execute = {}
                        )
                    )
                } else {
                    val out = UnitConverterEngine.evaluate(convArg, hasExplicitPrefix = true)
                    if (out != null) {
                        results.add(
                            LauncherSearchResult.RichCard(
                                id = "cmd_convert_${out.title}",
                                type = SmartCommandType.CONVERT,
                                title = out.title,
                                primaryValue = out.primaryValue,
                                badgeText = out.badge,
                                isBadgePositive = true,
                                subtitle = out.subtitle,
                                copyText = out.copyText,
                                execute = { ctx ->
                                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    cm?.setPrimaryClip(ClipData.newPlainText("Pallas Convert", out.copyText))
                                    Toast.makeText(ctx, "Copied ${out.copyText}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        )
                    }
                    matchedConvert = true
                }
            } else {
                UnitConverterEngine.evaluate(trimmed, hasExplicitPrefix = false)?.let { out ->
                    results.add(
                        LauncherSearchResult.RichCard(
                            id = "cmd_convert_${out.title}",
                            type = SmartCommandType.CONVERT,
                            title = out.title,
                            primaryValue = out.primaryValue,
                            badgeText = out.badge,
                            isBadgePositive = true,
                            subtitle = out.subtitle,
                            copyText = out.copyText,
                            execute = { ctx ->
                                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                cm?.setPrimaryClip(ClipData.newPlainText("Pallas Convert", out.copyText))
                                Toast.makeText(ctx, "Copied ${out.copyText}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    )
                    matchedConvert = true
                }
            }
        }

        // 0.25 Offline Utilities (Prefix-based): World Time, Quick Definition, Quick Capture, Offline QR, Loan EMI
        var matchedUtilityPrefix = false

        val tzCfg = cfg("tz")
        if (tzCfg.enabled) {
            val tzArg = matchPrefixArg(trimmed, tzCfg.prefix)
            if (tzArg != null) {
                matchedUtilityPrefix = true
                if (tzArg.isEmpty()) {
                    results.add(
                        LauncherSearchResult.Command(
                            id = "cmd_tz_hint",
                            title = "World Time",
                            subtitle = "",
                            type = SmartCommandType.WORLD_TIME,
                            execute = {}
                        )
                    )
                } else {
                    WorldTimeEngine.evaluate(tzArg, is24Hour = is24HourClock)?.let { out ->
                        results.add(
                            LauncherSearchResult.RichCard(
                                id = "cmd_tz_${out.title}",
                                type = SmartCommandType.WORLD_TIME,
                                title = out.title,
                                primaryValue = out.primaryValue,
                                badgeText = out.badge,
                                isBadgePositive = true,
                                subtitle = out.subtitle,
                                copyText = out.copyText,
                                execute = { ctx ->
                                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    cm?.setPrimaryClip(ClipData.newPlainText("Pallas World Time", out.copyText))
                                    Toast.makeText(ctx, "Copied ${out.copyText}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        )
                    }
                }
            }
        }

        val defCfg = cfg("def")
        if (defCfg.enabled) {
            val defArg = matchPrefixArg(trimmed, defCfg.prefix)
            if (defArg != null) {
                matchedUtilityPrefix = true
                if (defArg.isEmpty()) {
                    results.add(
                        LauncherSearchResult.Command(
                            id = "cmd_def_hint",
                            title = "Quick Definition",
                            subtitle = "",
                            type = SmartCommandType.DEFINE,
                            execute = {}
                        )
                    )
                } else {
                    results.add(
                        LauncherSearchResult.RichCard(
                            id = "def_loading_$defArg",
                            type = SmartCommandType.DEFINE,
                            title = defArg,
                            subtitle = "looking up definition…",
                            isLoading = true
                        )
                    )
                    if (asyncReq == null) {
                        asyncReq = AsyncCommandRequest.Define(defArg)
                    }
                }
            }
        }

        val noteCfg = cfg("note")
        if (noteCfg.enabled) {
            val noteArg = matchPrefixArg(trimmed, noteCfg.prefix)
            if (noteArg != null && !noteArg.trimStart().startsWith("=")) {
                matchedUtilityPrefix = true
                if (noteArg.isEmpty()) {
                    val inboxSubtitle = when {
                        quickCaptureCount <= 0 -> "0 notes"
                        quickCapturePreview.isNotBlank() -> "$quickCaptureCount note${if (quickCaptureCount == 1) "" else "s"} · \"$quickCapturePreview\""
                        else -> "$quickCaptureCount notes"
                    }
                    results.add(
                        LauncherSearchResult.Command(
                            id = QUICK_CAPTURE_INBOX_ID,
                            title = "Quick Capture Inbox",
                            subtitle = inboxSubtitle,
                            type = SmartCommandType.NOTE,
                            execute = {}
                        )
                    )
                } else {
                    results.add(
                        LauncherSearchResult.RichCard(
                            id = "cmd_note_append",
                            type = SmartCommandType.NOTE,
                            title = "Quick Capture",
                            primaryValue = noteArg,
                            badgeText = "Save .md",
                            isBadgePositive = true,
                            subtitle = "",
                            copyText = noteArg,
                            execute = { ctx ->
                                QuickCaptureRepository.getInstance(ctx).appendNote(noteArg)
                                Toast.makeText(ctx, "Saved to Quick Capture", Toast.LENGTH_SHORT).show()
                            }
                        )
                    )
                }
            }
        }

        val qrCfg = cfg("qr")
        if (qrCfg.enabled) {
            val qrArg = matchPrefixArg(trimmed, qrCfg.prefix)
            if (qrArg != null) {
                matchedUtilityPrefix = true
                if (qrArg.isEmpty()) {
                    results.add(
                        LauncherSearchResult.Command(
                            id = "cmd_qr_hint",
                            title = "Offline QR Code",
                            subtitle = "",
                            type = SmartCommandType.QR,
                            execute = {}
                        )
                    )
                } else {
                    val matrix = QrCodeGenerator.encode(qrArg)
                    if (matrix != null) {
                        results.add(
                            LauncherSearchResult.RichCard(
                                id = "cmd_qr_code",
                                type = SmartCommandType.QR,
                                title = "Offline QR Code",
                                subtitle = qrArg,
                                badgeText = "${matrix.size}×${matrix.size}",
                                isBadgePositive = true,
                                copyText = qrArg,
                                rawInput = qrArg,
                                qrMatrix = matrix,
                                execute = {}
                            )
                        )
                    } else {
                        results.add(
                            LauncherSearchResult.RichCard(
                                id = "cmd_qr_too_long",
                                type = SmartCommandType.QR,
                                title = "Offline QR Code",
                                primaryValue = "Payload too long (max 458 bytes)",
                                isBadgePositive = false,
                                subtitle = "${qrArg.toByteArray(Charsets.UTF_8).size} bytes entered"
                            )
                        )
                    }
                }
            }
        }

        val emiCfg = cfg("emi")
        if (emiCfg.enabled) {
            val emiArg = matchPrefixArg(trimmed, emiCfg.prefix)
            if (emiArg != null) {
                matchedUtilityPrefix = true
                if (emiArg.isEmpty()) {
                    results.add(
                        LauncherSearchResult.Command(
                            id = "cmd_emi_hint",
                            title = "Loan EMI Calculator",
                            subtitle = "",
                            type = SmartCommandType.EMI,
                            execute = {}
                        )
                    )
                } else {
                    val emi = EmiCalculatorEngine.evaluate(emiArg)
                    if (emi != null) {
                        results.add(
                            LauncherSearchResult.RichCard(
                                id = "cmd_emi_${emi.title}",
                                type = SmartCommandType.EMI,
                                title = emi.title,
                                primaryValue = emi.primaryValue,
                                badgeText = emi.badge,
                                isBadgePositive = true,
                                subtitle = emi.subtitle,
                                bodyText = emi.bodyText,
                                copyText = emi.copyText,
                                execute = { ctx ->
                                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    cm?.setPrimaryClip(ClipData.newPlainText("Pallas EMI", emi.copyText))
                                    Toast.makeText(ctx, "Copied EMI ${emi.copyText}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        )
                    } else {
                        results.add(
                            LauncherSearchResult.RichCard(
                                id = "cmd_emi_typing",
                                type = SmartCommandType.EMI,
                                title = "Loan EMI Calculator",
                                subtitle = "<principal> <rate%> <tenure>"
                            )
                        )
                    }
                }
            }
        }

        // 0.5 Prefixless Tools: Color & Base Inspector + Date Arithmetic (evaluated before Math so `#D4AF37`, `0xFF`, `today + 90d` don't hit Wolfram)
        var matchedColorBase = false
        if (colorBaseInspectorEnabled && !matchedConvert && !matchedUtilityPrefix && !matchedFilesPrefix) {
            ColorAndBaseInspector.evaluate(trimmed)?.let { out ->
                matchedColorBase = true
                results.add(
                    LauncherSearchResult.RichCard(
                        id = "cmd_colorbase_${out.title}",
                        type = SmartCommandType.COLOR_BASE,
                        title = out.title,
                        primaryValue = out.primaryValue,
                        badgeText = out.badge,
                        isBadgePositive = true,
                        subtitle = out.subtitle,
                        copyText = out.copyText,
                        colorSwatchArgb = out.colorSwatchArgb,
                        execute = { ctx ->
                            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            cm?.setPrimaryClip(ClipData.newPlainText("Pallas Inspector", out.copyText))
                            Toast.makeText(ctx, "Copied ${out.copyText}", Toast.LENGTH_SHORT).show()
                        }
                    )
                )
            }
        }

        var matchedDateMath = false
        if (dateArithmeticEnabled && !matchedConvert && !matchedUtilityPrefix && !matchedColorBase && !matchedFilesPrefix) {
            DateArithmeticEngine.evaluate(trimmed)?.let { out ->
                matchedDateMath = true
                results.add(
                    LauncherSearchResult.RichCard(
                        id = "cmd_date_${out.title}",
                        type = SmartCommandType.DATE_ARITHMETIC,
                        title = out.title,
                        primaryValue = out.primaryValue,
                        badgeText = out.badge,
                        isBadgePositive = true,
                        subtitle = out.subtitle,
                        copyText = out.copyText,
                        execute = { ctx ->
                            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            cm?.setPrimaryClip(ClipData.newPlainText("Pallas Date", out.copyText))
                            Toast.makeText(ctx, "Copied ${out.copyText}", Toast.LENGTH_SHORT).show()
                        }
                    )
                )
            }
        }

        // Skip unprefixed math if explicit tex, wiki, !, or pallas command matched (do NOT block `q = 10` or `n = 5` math assignments)
        val isPallasQuery = lower == "pallas" || lower.startsWith("pallas ")
        val hasExplicitOtherPrefix =
            matchedAppSearch ||
            isPallasQuery ||
            (cfg("tex").enabled && matchPrefixArg(trimmed, cfg("tex").prefix) != null) ||
            (cfg("wiki").enabled && matchPrefixArg(trimmed, cfg("wiki").prefix) != null)

        // 1. Math & Fat-Tailed Probability
        var matchedMath = false
        val mathCfg = cfg("math")
        if (mathCfg.enabled && !matchedConvert) {
            val mathArg = matchPrefixArg(trimmed, mathCfg.prefix)
            if (mathArg != null) {
                matchedMath = true
                if (mathArg.isEmpty()) {
                    results.add(
                        LauncherSearchResult.Command(
                            id = "cmd_math_hint",
                            title = "Math & Probability Engine",
                            subtitle = "",
                            type = SmartCommandType.MATH_STATS,
                            hasInfoManual = false,
                            execute = {}
                        )
                    )
                } else {
                    if (MathStatsEngine.containsPlotOrChart(trimmed, mathCfg.prefix)) {
                        if (!MathStatsEngine.areDelimitersBalanced(mathArg) || !mathArg.contains('[')) {
                            results.add(
                                LauncherSearchResult.RichCard(
                                    id = "cmd_math_typing",
                                    type = SmartCommandType.MATH_STATS,
                                    title = "Plot & Visualization Engine",
                                    subtitle = "Complete brackets…",
                                    primaryValue = "",
                                    isLoading = false
                                )
                            )
                        } else {
                            results.add(
                                LauncherSearchResult.RichCard(
                                    id = "cmd_math_plot_loading",
                                    type = SmartCommandType.MATH_STATS,
                                    title = mathArg,
                                    subtitle = "rendering plot…",
                                    isLoading = true
                                )
                            )
                            asyncReq = AsyncCommandRequest.MathPlot(trimmed, mathCfg.prefix)
                        }
                    } else {
                        val out = MathStatsEngine.evaluate(trimmed, mathCfg.prefix)
                        if (out != null) {
                            results.add(
                                LauncherSearchResult.RichCard(
                                    id = "cmd_math_${out.title}",
                                    type = SmartCommandType.MATH_STATS,
                                    title = out.title,
                                    primaryValue = out.primaryValue,
                                    consoleOutput = out.consoleOutput,
                                    chart = out.chart,
                                    badgeText = out.badge,
                                    isBadgePositive = true,
                                    subtitle = out.subtitle,
                                    hasInfoManual = false,
                                    rawInput = mathArg,
                                    execute = { ctx ->
                                        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        cm?.setPrimaryClip(ClipData.newPlainText("Pallas Math", out.copyText))
                                        Toast.makeText(ctx, "Copied ${out.copyText}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            )
                        }
                    }
                }
            } else if (!matchedUtilityPrefix && !matchedColorBase && !matchedDateMath && !matchedFilesPrefix && !hasExplicitOtherPrefix) {
                if (MathStatsEngine.containsPlotOrChart(trimmed, "")) {
                    if (MathStatsEngine.areDelimitersBalanced(trimmed) && (trimmed.contains('[') || trimmed.contains("//") || trimmed.contains('@'))) {
                        results.add(
                            LauncherSearchResult.RichCard(
                                id = "cmd_math_plot_loading",
                                type = SmartCommandType.MATH_STATS,
                                title = trimmed,
                                subtitle = "rendering plot…",
                                isLoading = true
                            )
                        )
                        asyncReq = AsyncCommandRequest.MathPlot(trimmed, "")
                        matchedMath = true
                    }
                } else {
                    MathStatsEngine.evaluate(trimmed, "")?.let { out ->
                        results.add(
                            LauncherSearchResult.RichCard(
                                id = "cmd_math_${out.title}",
                                type = SmartCommandType.MATH_STATS,
                                title = out.title,
                                primaryValue = out.primaryValue,
                                consoleOutput = out.consoleOutput,
                                chart = out.chart,
                                badgeText = out.badge,
                                isBadgePositive = true,
                                subtitle = out.subtitle,
                                hasInfoManual = false,
                                rawInput = trimmed,
                                execute = { ctx ->
                                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    cm?.setPrimaryClip(ClipData.newPlainText("Pallas Math", out.copyText))
                                    Toast.makeText(ctx, "Copied ${out.copyText}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        )
                        matchedMath = true
                    }
                }
            }
        }

        // 1.5. LaTeX Renderer
        val texCfg = cfg("tex")
        if (texCfg.enabled) {
            val texArg = matchPrefixArg(trimmed, texCfg.prefix)
            if (texArg != null) {
                if (texArg.isEmpty()) {
                    results.add(
                        LauncherSearchResult.Command(
                            id = "cmd_tex_hint",
                            title = "LaTeX Formula Renderer",
                            subtitle = "",
                            type = SmartCommandType.LATEX,
                            execute = {}
                        )
                    )
                } else {
                    val normalized = normalizeUserLatex(texArg)
                    if (normalized.isNotEmpty()) {
                        results.add(
                            LauncherSearchResult.RichCard(
                                id = "cmd_tex_preview",
                                type = SmartCommandType.LATEX,
                                title = "LaTeX",
                                primaryValue = "",
                                latexSource = normalized,
                                badgeText = "LaTeX",
                                isBadgePositive = true,
                                subtitle = "",
                                rawInput = normalized
                            )
                        )
                    }
                }
            }
        }

        // 2. Weather
        val wxCfg = cfg("weather")
        if (wxCfg.enabled && !matchedMath) {
            val wxArg = matchPrefixArg(trimmed, wxCfg.prefix) ?: matchPrefixArg(trimmed, "weather")
            if (wxArg != null) {
                val targetCity = wxArg.ifEmpty { fallbackCity }
                results.add(
                    LauncherSearchResult.RichCard(
                        id = "wx_loading_$targetCity",
                        type = SmartCommandType.WEATHER,
                        title = targetCity,
                        subtitle = "fetching current conditions…",
                        isLoading = true
                    )
                )
                if (asyncReq == null) {
                    asyncReq = AsyncCommandRequest.Weather(targetCity, isSolarOnly = false)
                }
            }
        }

        // 3. Finance & Markets
        val stockCfg = cfg("stock")
        if (stockCfg.enabled && !matchedMath) {
            val stockArg = matchPrefixArg(trimmed, stockCfg.prefix)
            if (stockArg != null) {
                if (stockArg.isEmpty()) {
                    results.add(
                        LauncherSearchResult.Command(
                            id = "cmd_stock_hint",
                            title = "Markets & Commodities",
                            subtitle = "",
                            type = SmartCommandType.FINANCE,
                            execute = {}
                        )
                    )
                } else {
                    results.add(
                        LauncherSearchResult.RichCard(
                            id = "fin_loading_$stockArg",
                            type = SmartCommandType.FINANCE,
                            title = stockArg.uppercase(Locale.ROOT),
                            subtitle = "fetching market quote…",
                            isLoading = true
                        )
                    )
                    if (asyncReq == null) {
                        asyncReq = AsyncCommandRequest.Finance(stockArg)
                    }
                }
            }
        }

        // 4. Sunrise / Sunset
        val solarCfg = cfg("solar")
        if (solarCfg.enabled && !matchedMath) {
            val solarArg = matchPrefixArg(trimmed, solarCfg.prefix)
                ?: matchPrefixArg(trimmed, "sunrise")
                ?: matchPrefixArg(trimmed, "sunset")
            if (solarArg != null) {
                val targetCity = solarArg.ifEmpty { fallbackCity }
                results.add(
                    LauncherSearchResult.RichCard(
                        id = "solar_loading_$targetCity",
                        type = SmartCommandType.SOLAR,
                        title = targetCity,
                        subtitle = "calculating solar times…",
                        isLoading = true
                    )
                )
                if (asyncReq == null) {
                    asyncReq = AsyncCommandRequest.Weather(targetCity, isSolarOnly = true)
                }
            }
        }

        // 5. Wolfram Alpha
        val waCfg = cfg("wolfram")
        if (waCfg.enabled && !matchedMath) {
            val waArg = matchPrefixArg(trimmed, waCfg.prefix)
            if (waArg != null) {
                if (waArg.isEmpty()) {
                    results.add(
                        LauncherSearchResult.Command(
                            id = "cmd_wa_hint",
                            title = "Wolfram|Alpha Short Answer",
                            subtitle = "",
                            type = SmartCommandType.WOLFRAM,
                            execute = {}
                        )
                    )
                } else {
                    results.add(
                        LauncherSearchResult.RichCard(
                            id = "wa_loading_$waArg",
                            type = SmartCommandType.WOLFRAM,
                            title = waArg,
                            subtitle = "querying Wolfram|Alpha…",
                            isLoading = true
                        )
                    )
                    if (asyncReq == null) {
                        asyncReq = AsyncCommandRequest.Wolfram(waArg)
                    }
                }
            }
        }

        // 5.5. Wikipedia
        val wikiCfg = cfg("wiki")
        if (wikiCfg.enabled && !matchedMath) {
            val wikiArg = matchPrefixArg(trimmed, wikiCfg.prefix)
            if (wikiArg != null) {
                if (wikiArg.isEmpty()) {
                    results.add(
                        LauncherSearchResult.Command(
                            id = "cmd_wiki_hint",
                            title = "Wikipedia Summary",
                            subtitle = "",
                            type = SmartCommandType.WIKI,
                            execute = {}
                        )
                    )
                } else {
                    results.add(
                        LauncherSearchResult.RichCard(
                            id = "wiki_loading_$wikiArg",
                            type = SmartCommandType.WIKI,
                            title = wikiArg,
                            subtitle = "fetching Wikipedia summary…",
                            isLoading = true
                        )
                    )
                    if (asyncReq == null) {
                        asyncReq = AsyncCommandRequest.Wiki(wikiArg)
                    }
                }
            }
        }

        // 6. Alarm
        val alCfg = cfg("alarm")
        if (alCfg.enabled) {
            val alArg = matchPrefixArg(trimmed, alCfg.prefix, allowDigitGlue = true)
                ?: matchPrefixArg(trimmed, "alarm", allowDigitGlue = true)
            if (alArg != null) {
                if (alArg.isEmpty()) {
                    results.add(
                        LauncherSearchResult.Command(
                            id = "cmd_alarm_hint",
                            title = "Set Alarm",
                            subtitle = "Clock",
                            type = SmartCommandType.ALARM,
                            execute = { SystemActionsManager.openClock(it) }
                        )
                    )
                } else {
                    parseAlarmTime(alArg)?.let { parsed ->
                        val formatted = formatAlarmTime(parsed)
                        results.add(
                            LauncherSearchResult.Command(
                                id = "cmd_alarm_${parsed.hour}_${parsed.minute}",
                                title = "Set alarm for $formatted",
                                subtitle = "Clock",
                                type = SmartCommandType.ALARM,
                                execute = { SystemActionsManager.setAlarm(it, parsed.hour, parsed.minute) }
                            )
                        )
                    }
                }
            }
        }

        // 7. Timer
        val timerCfg = cfg("timer")
        if (timerCfg.enabled) {
            val timerArg = matchPrefixArg(trimmed, timerCfg.prefix, allowDigitGlue = true)
            if (timerArg != null) {
                if (timerArg.isEmpty()) {
                    results.add(
                        LauncherSearchResult.Command(
                            id = "cmd_timer_hint",
                            title = "Set Timer",
                            subtitle = "Clock",
                            type = SmartCommandType.TIMER,
                            execute = { SystemActionsManager.openClock(it) }
                        )
                    )
                } else {
                    parseTimerDuration(timerArg)?.let { parsed ->
                        results.add(
                            LauncherSearchResult.Command(
                                id = "cmd_timer_${parsed.totalSeconds}",
                                title = "Set timer for ${parsed.formattedDuration}",
                                subtitle = "Clock",
                                type = SmartCommandType.TIMER,
                                execute = { SystemActionsManager.setTimer(it, parsed.totalSeconds) }
                            )
                        )
                    }
                }
            }
        }

        // 8. Web Search
        val webCfg = cfg("web")
        if (webCfg.enabled && !matchedMath) {
            val webArg = matchPrefixArg(trimmed, webCfg.prefix) ?: matchPrefixArg(trimmed, "query")
            if (webArg != null) {
                if (webArg.isEmpty()) {
                    results.add(
                        LauncherSearchResult.Command(
                            id = "cmd_web_hint",
                            title = "Search the web",
                            subtitle = webSearch.browserLabel ?: "Default browser",
                            type = SmartCommandType.WEB_SEARCH,
                            execute = { SystemActionsManager.openWebSearch(it, "", webSearch.engine, webSearch.browserPackage) }
                        )
                    )
                } else {
                    results.add(
                        LauncherSearchResult.Command(
                            id = "cmd_web_$webArg",
                            title = "Search ${webSearch.engine.label} for \"$webArg\"",
                            subtitle = webSearch.browserLabel ?: "Default browser",
                            type = SmartCommandType.WEB_SEARCH,
                            execute = { SystemActionsManager.openWebSearch(it, webArg, webSearch.engine, webSearch.browserPackage) }
                        )
                    )
                }
            }
        }

        // @nickname / @name searches saved contacts and the full device contacts list.
        val callCfg = cfg("call")
        val savedContacts = contacts.filter { !it.id.startsWith("device:") && it.nickname.isNotBlank() }
        if (callCfg.enabled && trimmed.startsWith("@")) {
            val deviceByDigits = deviceContacts
                .mapNotNull { c -> c.phoneNumber.filter(Char::isDigit).takeIf { it.isNotEmpty() }?.let { it to c } }
                .toMap()
            val enrichedSaved = savedContacts.map { s ->
                if (s.name.isBlank()) {
                    val d = s.phoneNumber.filter(Char::isDigit)
                    val match = if (d.isNotEmpty()) deviceByDigits[d] else null
                    if (match != null) s.copy(name = match.name) else s
                } else s
            }
            val savedDigits = enrichedSaved.map { it.phoneNumber.filter(Char::isDigit) }.filter { it.isNotEmpty() }.toSet()
            val atContacts = enrichedSaved + deviceContacts.filter { c ->
                val d = c.phoneNumber.filter(Char::isDigit)
                d.isEmpty() || d !in savedDigits
            }
            val query = trimmed.drop(1).trim()
            atContacts.mapNotNull { contact ->
                val nicknameMatch = if (query.isBlank()) MatchResult(1.0) else SearchRanking.evaluateMatch(query, contact.nickname)
                val nameMatch = if (query.isBlank() || contact.name.isBlank()) MatchResult(0.0) else SearchRanking.evaluateMatch(query, contact.name)
                val phoneMatch = if (query.isBlank()) MatchResult(0.0) else SearchRanking.evaluateMatch(query, contact.phoneNumber)
                val boostedNickname = if (nicknameMatch.score > 0.0) nicknameMatch.score + 120.0 else 0.0
                val best = maxOf(boostedNickname, nameMatch.score, phoneMatch.score)
                if (query.isNotBlank() && best <= 0.0) return@mapNotNull null
                best to LauncherSearchResult.ContactCall(
                        id = "contact_call_${contact.id}",
                        nickname = contact.nickname,
                        contactName = contact.name,
                        phoneNumber = contact.phoneNumber,
                        execute = { SystemActionsManager.callPhoneNumber(it, contact.phoneNumber) }
                    )
            }.sortedWith(compareByDescending<Pair<Double, LauncherSearchResult.ContactCall>> { it.first }
                .thenBy { it.second.nickname.lowercase(Locale.ROOT) })
                .take(100)
                .forEach { results.add(it.second) }
        }

        // 9. Call (ONLY favorite/pinned contacts with explicitly added nicknames)
        if (callCfg.enabled && !trimmed.startsWith("@")) {
            val afterCall = matchPrefixArg(trimmed, callCfg.prefix)
            if (afterCall != null) {
                val matches = if (afterCall.isEmpty()) savedContacts else {
                    savedContacts.filter { it.nickname.equals(afterCall, ignoreCase = true) }
                        .ifEmpty { savedContacts.filter { it.nickname.startsWith(afterCall, ignoreCase = true) } }
                }
                for (contact in matches) {
                    results.add(
                        LauncherSearchResult.ContactCall(
                            id = "contact_call_${contact.id}",
                            nickname = contact.nickname,
                            contactName = contact.name,
                            phoneNumber = contact.phoneNumber,
                            execute = { SystemActionsManager.callPhoneNumber(it, contact.phoneNumber) }
                        )
                    )
                }
            } else {
                savedContacts.filter { it.nickname.equals(trimmed, ignoreCase = true) }.forEach { contact ->
                    results.add(
                        LauncherSearchResult.ContactCall(
                            id = "contact_call_${contact.id}",
                            nickname = contact.nickname,
                            contactName = contact.name,
                            phoneNumber = contact.phoneNumber,
                            execute = { SystemActionsManager.callPhoneNumber(it, contact.phoneNumber) }
                        )
                    )
                }
            }
        }

        if (lower == "settings") {
            results.add(
                LauncherSearchResult.Command(
                    id = SETTINGS_COMMAND_ID,
                    title = "Pallas settings",
                    subtitle = "Open launcher settings",
                    type = SmartCommandType.SETTINGS,
                    execute = {}
                )
            )
        } else {
            PallasCommandParser.resolve(trimmed).forEach { nav ->
                val navId = if (nav.target == PallasTarget.SettingsMain) {
                    SETTINGS_COMMAND_ID
                } else {
                    "$PALLAS_NAV_PREFIX${nav.target.encode()}"
                }
                results.add(
                    LauncherSearchResult.Command(
                        id = navId,
                        title = nav.title,
                        subtitle = nav.subtitle,
                        type = SmartCommandType.SETTINGS,
                        execute = {}
                    )
                )
            }
        }

        return CommandEvaluation(results, asyncReq)
    }

    /**
     * Returns true when [rawQuery] is still a plausible prefix of any built-in command, alias,
     * macro trigger, or contact nickname, so single-match app auto-launch should wait instead of
     * preempting what the user is typing.
     */
    fun hasPotentialCommandOrAliasMatch(
        rawQuery: String,
        configs: Map<String, CommandConfig> = CommandConfig.ALL_ENABLED,
        activeFocusProfile: FocusProfile? = null,
        macros: List<MacroRoutine> = emptyList(),
        customAliases: Map<String, String> = emptyMap(),
        contacts: List<SearchContact> = emptyList(),
        matchedAppUniqueKey: String? = null,
        matchedAppPackage: String? = null
    ): Boolean {
        val q = rawQuery.trim().lowercase(Locale.ROOT)
        if (q.isEmpty()) return false

        fun cfg(id: String): CommandConfig {
            val base = configs[id] ?: CommandConfig.DEFAULTS.getValue(id)
            val disabledByFocus = activeFocusProfile != null &&
                id in activeFocusProfile.disabledCommands &&
                id != "settings"
            return if (disabledByFocus) base.copy(enabled = false) else base
        }

        val alwaysOnKeywords = listOf("focus", "give up", "pallas", "settings")
        if (alwaysOnKeywords.any { it.startsWith(q) }) return true

        val extraAliasesByCommandId = mapOf(
            "alarm" to listOf("alarm"),
            "timer" to listOf("timer"),
            "web" to listOf("query"),
            "convert" to listOf("convert", "conv"),
            "weather" to listOf("weather"),
            "solar" to listOf("sunrise", "sunset"),
            "calendar" to listOf("show cal", "show calendar", "calendar", "agenda"),
            "flashlight" to listOf("flashlight")
        )

        for (id in CommandConfig.DEFAULTS.keys) {
            val c = cfg(id)
            if (!c.enabled) continue
            val prefix = c.prefix.trim().lowercase(Locale.ROOT)
            if (prefix.isNotEmpty() && prefix.startsWith(q)) return true
            val extras = extraAliasesByCommandId[id].orEmpty()
            if (extras.any { it.startsWith(q) }) return true
        }

        for (macro in macros) {
            val trigger = macro.triggerWord.trim().lowercase(Locale.ROOT)
            if (trigger.isNotEmpty() && trigger.startsWith(q)) return true
        }

        if (cfg("call").enabled) {
            for (contact in contacts) {
                if (contact.id.startsWith("device:")) continue
                val nick = contact.nickname.trim().lowercase(Locale.ROOT)
                if (nick.isNotEmpty() && nick.startsWith(q)) return true
            }
        }

        for ((rawAlias, rawTarget) in customAliases) {
            val k = rawAlias.trim().lowercase(Locale.ROOT)
            if (k.isEmpty() || !k.startsWith(q)) continue
            if (k.length > q.length) return true
            val target = rawTarget.trim()
            val matchesThisApp = (!matchedAppUniqueKey.isNullOrBlank() && target.equals(matchedAppUniqueKey, ignoreCase = true)) ||
                (!matchedAppPackage.isNullOrBlank() && target.equals(matchedAppPackage, ignoreCase = true))
            if (!matchesThisApp) return true
        }

        return false
    }
}
