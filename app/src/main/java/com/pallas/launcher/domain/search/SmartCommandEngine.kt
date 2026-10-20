package com.pallas.launcher.domain.search

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import com.pallas.launcher.domain.model.CommandConfig
import com.pallas.launcher.domain.model.LauncherSettings
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

    fun evaluateCommands(
        rawQuery: String,
        contacts: List<SearchContact>,
        webSearch: WebSearchConfig = WebSearchConfig(),
        configs: Map<String, CommandConfig> = CommandConfig.ALL_ENABLED,
        defaultWeatherCity: String = LauncherSettings.DEFAULT_WEATHER_CITY
    ): CommandEvaluation {
        val trimmed = rawQuery.trim()
        if (trimmed.isEmpty()) return CommandEvaluation(emptyList())
        val lower = trimmed.lowercase(Locale.ROOT)
        val results = mutableListOf<LauncherSearchResult>()
        var asyncReq: AsyncCommandRequest? = null

        fun cfg(id: String) = configs[id] ?: CommandConfig.DEFAULTS.getValue(id)
        val fallbackCity = defaultWeatherCity.trim().ifEmpty { LauncherSettings.DEFAULT_WEATHER_CITY }

        // 1. Math & Fat-Tailed Probability
        val mathCfg = cfg("math")
        if (mathCfg.enabled) {
            val mathArg = matchPrefixArg(trimmed, mathCfg.prefix)
            if (mathArg != null) {
                if (mathArg.isEmpty()) {
                    results.add(
                        LauncherSearchResult.Command(
                            id = "cmd_math_hint",
                            title = "Math & Probability Engine",
                            subtitle = "e.g. '${mathCfg.prefix} ParetoDistribution[1, 2]' or 'Mean[{1, 2, 3, 4}]'",
                            type = SmartCommandType.MATH_STATS,
                            hasInfoManual = false,
                            execute = {}
                        )
                    )
                } else {
                    val out = MathStatsEngine.evaluate(trimmed, mathCfg.prefix)
                    if (out != null) {
                        results.add(
                            LauncherSearchResult.RichCard(
                                id = "cmd_math_${out.title}",
                                type = SmartCommandType.MATH_STATS,
                                title = out.title,
                                primaryValue = out.primaryValue,
                                badgeText = out.badge,
                                isBadgePositive = true,
                                subtitle = out.subtitle,
                                hasInfoManual = false,
                                execute = { ctx ->
                                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    cm?.setPrimaryClip(ClipData.newPlainText("Pallas Math", out.copyText))
                                    Toast.makeText(ctx, "Copied ${out.copyText}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        )
                    } else {
                        // User is actively typing an expression after the math prefix
                        results.add(
                            LauncherSearchResult.RichCard(
                                id = "cmd_math_typing",
                                type = SmartCommandType.MATH_STATS,
                                title = "Math & Probability Engine",
                                subtitle = "Evaluating expression…",
                                primaryValue = mathArg,
                                isLoading = false
                            )
                        )
                    }
                }
            } else {
                MathStatsEngine.evaluate(trimmed, "")?.let { out ->
                    results.add(
                        LauncherSearchResult.RichCard(
                            id = "cmd_math_${out.title}",
                            type = SmartCommandType.MATH_STATS,
                            title = out.title,
                            primaryValue = out.primaryValue,
                            badgeText = out.badge,
                            isBadgePositive = true,
                            subtitle = out.subtitle,
                            hasInfoManual = false,
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

        // 2. Weather
        val wxCfg = cfg("weather")
        if (wxCfg.enabled) {
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
        if (stockCfg.enabled) {
            val stockArg = matchPrefixArg(trimmed, stockCfg.prefix)
            if (stockArg != null) {
                if (stockArg.isEmpty()) {
                    results.add(
                        LauncherSearchResult.Command(
                            id = "cmd_stock_hint",
                            title = "Markets & Commodities",
                            subtitle = "e.g. '${stockCfg.prefix} nifty', '${stockCfg.prefix} reliance', '${stockCfg.prefix} nvda', '${stockCfg.prefix} gold'",
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
        if (solarCfg.enabled) {
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
        if (waCfg.enabled) {
            val waArg = matchPrefixArg(trimmed, waCfg.prefix)
            if (waArg != null) {
                if (waArg.isEmpty()) {
                    results.add(
                        LauncherSearchResult.Command(
                            id = "cmd_wa_hint",
                            title = "Wolfram|Alpha Short Answer",
                            subtitle = "Type '${waCfg.prefix} <query>'",
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
                            subtitle = "Clock • e.g. '${alCfg.prefix} 530', '${alCfg.prefix} 1805'",
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
                                subtitle = "Clock • Tap or press Enter to set alarm",
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
                            subtitle = "Clock • e.g. '${timerCfg.prefix} 5m', '${timerCfg.prefix} 1:05'",
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
                                subtitle = "Clock • Tap or press Enter to start timer",
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
        if (webCfg.enabled) {
            val webArg = matchPrefixArg(trimmed, webCfg.prefix) ?: matchPrefixArg(trimmed, "query")
            if (webArg != null) {
                if (webArg.isEmpty()) {
                    results.add(
                        LauncherSearchResult.Command(
                            id = "cmd_web_hint",
                            title = "Search the web",
                            subtitle = "Type '${webCfg.prefix} <terms>' • opens in ${webSearch.browserLabel ?: "your default browser"}",
                            type = SmartCommandType.WEB_SEARCH,
                            execute = { SystemActionsManager.openWebSearch(it, "", webSearch.engine, webSearch.browserPackage) }
                        )
                    )
                } else {
                    results.add(
                        LauncherSearchResult.Command(
                            id = "cmd_web_$webArg",
                            title = "Search ${webSearch.engine.label} for \"$webArg\"",
                            subtitle = "Opens in ${webSearch.browserLabel ?: "your default browser"}",
                            type = SmartCommandType.WEB_SEARCH,
                            execute = { SystemActionsManager.openWebSearch(it, webArg, webSearch.engine, webSearch.browserPackage) }
                        )
                    )
                }
            }
        }

        // 9. Call
        val callCfg = cfg("call")
        if (callCfg.enabled) {
            val afterCall = matchPrefixArg(trimmed, callCfg.prefix)
            if (afterCall != null) {
                val matches = if (afterCall.isEmpty()) contacts else {
                    contacts.filter { it.nickname.equals(afterCall, ignoreCase = true) }
                        .ifEmpty { contacts.filter { it.nickname.startsWith(afterCall, ignoreCase = true) } }
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
                contacts.filter { it.nickname.equals(trimmed, ignoreCase = true) }.forEach { contact ->
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

        if (lower == "settings" || lower == "pallas") {
            results.add(
                LauncherSearchResult.Command(
                    id = SETTINGS_COMMAND_ID,
                    title = "Pallas settings",
                    subtitle = "Open launcher settings",
                    type = SmartCommandType.SETTINGS,
                    execute = {}
                )
            )
        }

        return CommandEvaluation(results, asyncReq)
    }
}
