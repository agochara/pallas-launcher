package com.pallas.launcher.domain.search

import com.pallas.launcher.domain.model.SearchContact
import com.pallas.launcher.platform.SystemActionsManager
import java.util.Locale

data class ParsedAlarm(val hour: Int, val minute: Int)

data class ParsedTimer(
    val totalSeconds: Int,
    val formattedDuration: String
)

object SmartCommandEngine {

    fun parseAlarm(query: String): ParsedAlarm? {
        val trimmed = query.trim().lowercase(Locale.ROOT)
        val verb = when {
            trimmed == "al" || trimmed == "alarm" -> return null
            trimmed.startsWith("alarm ") -> "alarm"
            trimmed.startsWith("al ") -> "al"
            // Also handle without space if followed immediately by digit, e.g. "al5", "al530"
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
                    // e.g. "530" -> 5:30
                    hour = clean.substring(0, 1).toInt()
                    minute = clean.substring(1, 3).toInt()
                }
                4 -> {
                    // e.g. "1805" -> 18:05, "0530" -> 5:30
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
            else if (hour != 12 && hour > 12) {
                if (hour !in 13..23) return null
            }
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

    fun parseTimerDuration(rawArg: String): ParsedTimer? {
        val trimmed = rawArg.trim().lowercase(Locale.ROOT)
        if (trimmed.isEmpty()) return null

        // Format 1: Colon notation "1:05", "1:30", "1:05:30"
        if (trimmed.contains(":")) {
            val parts = trimmed.split(":")
            if (parts.size == 2) {
                val hours = parts[0].toIntOrNull() ?: return null
                val minutes = parts[1].toIntOrNull() ?: return null
                if (hours < 0 || minutes < 0 || minutes >= 60) return null
                val totalSeconds = hours * 3600 + minutes * 60
                if (totalSeconds <= 0) return null
                return ParsedTimer(totalSeconds, formatDuration(hours, minutes, 0))
            } else if (parts.size == 3) {
                val hours = parts[0].toIntOrNull() ?: return null
                val minutes = parts[1].toIntOrNull() ?: return null
                val seconds = parts[2].toIntOrNull() ?: return null
                if (hours < 0 || minutes < 0 || minutes >= 60 || seconds < 0 || seconds >= 60) return null
                val totalSeconds = hours * 3600 + minutes * 60 + seconds
                if (totalSeconds <= 0) return null
                return ParsedTimer(totalSeconds, formatDuration(hours, minutes, seconds))
            }
        }

        // Format 2: Plain number -> default to minutes, e.g. "5" -> 5 minutes
        trimmed.toIntOrNull()?.let { num ->
            if (num > 0) {
                val totalSeconds = num * 60
                val hours = num / 60
                val minutes = num % 60
                return ParsedTimer(totalSeconds, formatDuration(hours, minutes, 0))
            }
        }

        // Format 3: Unit strings, e.g. "5m", "1h 5m", "1hr5m", "30s", "1hr 30min"
        var hours = 0
        var minutes = 0
        var seconds = 0
        var matched = false

        val unitRegex = """(\d+)\s*(h(?:ours?|rs?)?|m(?:in(?:utes?)?)?|s(?:ec(?:onds?)?)?)""".toRegex()
        val matches = unitRegex.findAll(trimmed).toList()
        if (matches.isNotEmpty()) {
            for (m in matches) {
                val value = m.groupValues[1].toIntOrNull() ?: return null
                val unit = m.groupValues[2]
                when {
                    unit.startsWith("h") -> hours += value
                    unit.startsWith("m") -> minutes += value
                    unit.startsWith("s") -> seconds += value
                }
                matched = true
            }
        }

        if (!matched) return null

        val totalSeconds = hours * 3600 + minutes * 60 + seconds
        if (totalSeconds <= 0) return null
        val actualHours = totalSeconds / 3600
        val actualMinutes = (totalSeconds % 3600) / 60
        val actualSeconds = totalSeconds % 60
        return ParsedTimer(totalSeconds, formatDuration(actualHours, actualMinutes, actualSeconds))
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

    fun evaluateCommands(
        rawQuery: String,
        contacts: List<SearchContact>
    ): List<LauncherSearchResult> {
        val trimmed = rawQuery.trim()
        if (trimmed.isEmpty()) return emptyList()
        val lower = trimmed.lowercase(Locale.ROOT)
        val results = mutableListOf<LauncherSearchResult>()

        // 1. Check for Alarm command
        if (lower == "al" || lower == "alarm") {
            results.add(
                LauncherSearchResult.Command(
                    id = "cmd_alarm_hint",
                    title = "Set Alarm",
                    subtitle = "Clock • e.g. 'al 5', 'al 530', 'alarm 1805'",
                    type = SmartCommandType.ALARM,
                    execute = { SystemActionsManager.openClock(it) }
                )
            )
        } else {
            val parsedAlarm = parseAlarm(trimmed)
            if (parsedAlarm != null) {
                val formatted = formatAlarmTime(parsedAlarm)
                results.add(
                    LauncherSearchResult.Command(
                        id = "cmd_alarm_${parsedAlarm.hour}_${parsedAlarm.minute}",
                        title = "Set alarm for $formatted",
                        subtitle = "Clock • Tap or press Enter to set alarm",
                        type = SmartCommandType.ALARM,
                        execute = { SystemActionsManager.setAlarm(it, parsedAlarm.hour, parsedAlarm.minute) }
                    )
                )
            }
        }

        // 2. Check for Timer command
        if (lower == "timer") {
            results.add(
                LauncherSearchResult.Command(
                    id = "cmd_timer_hint",
                    title = "Set Timer",
                    subtitle = "Clock • e.g. 'timer 5m', 'timer 1h 5m', 'timer 1:05'",
                    type = SmartCommandType.TIMER,
                    execute = { SystemActionsManager.openClock(it) }
                )
            )
        } else {
            val parsedTimer = parseTimer(trimmed)
            if (parsedTimer != null) {
                results.add(
                    LauncherSearchResult.Command(
                        id = "cmd_timer_${parsedTimer.totalSeconds}",
                        title = "Set timer for ${parsedTimer.formattedDuration}",
                        subtitle = "Clock • Tap or press Enter to start timer",
                        type = SmartCommandType.TIMER,
                        execute = { SystemActionsManager.setTimer(it, parsedTimer.totalSeconds) }
                    )
                )
            }
        }

        // 3. Check for Call command (ONLY matching contacts added in Search Settings)
        if (lower.startsWith("call")) {
            val afterCall = trimmed.substring(4).trim()
            if (afterCall.isEmpty()) {
                // List configured contacts
                for (contact in contacts) {
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
                // Find matching contact by nickname
                val matches = contacts.filter {
                    it.nickname.equals(afterCall, ignoreCase = true)
                }.ifEmpty {
                    contacts.filter {
                        it.nickname.startsWith(afterCall, ignoreCase = true)
                    }
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
            }
        } else {
            // Also allow typing nickname directly if it matches exactly
            val directMatch = contacts.filter { it.nickname.equals(trimmed, ignoreCase = true) }
            for (contact in directMatch) {
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

        return results
    }
}
