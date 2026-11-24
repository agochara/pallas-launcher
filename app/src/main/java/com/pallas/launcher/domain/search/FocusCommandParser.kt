package com.pallas.launcher.domain.search

import com.pallas.launcher.domain.model.FocusProfile
import java.util.Locale

sealed interface FocusCommand {
    data object TurnOff : FocusCommand
    data class TurnOn(
        val profile: FocusProfile?,
        val durationSeconds: Int?,
        val dndOverride: Boolean?
    ) : FocusCommand
    data object NoProfilesConfigured : FocusCommand
}

object FocusCommandParser {

    fun parse(
        rawQuery: String,
        profiles: List<FocusProfile>,
        defaultProfileId: String?
    ): FocusCommand? {
        val trimmed = rawQuery.trim()
        if (trimmed.isEmpty()) return null
        val lower = trimmed.lowercase(Locale.ROOT)

        if (lower == "give up" || lower == "focus off") {
            return FocusCommand.TurnOff
        }

        if (lower != "focus" && !lower.startsWith("focus ")) {
            return null
        }

        if (profiles.isEmpty()) {
            return FocusCommand.NoProfilesConfigured
        }

        val rawArgs = trimmed.drop(5).trim()
        val defaultProfile = defaultProfileId?.let { id -> profiles.firstOrNull { it.id == id } } ?: profiles.first()
        if (rawArgs.isEmpty()) {
            return FocusCommand.TurnOn(profile = defaultProfile, durationSeconds = null, dndOverride = null)
        }

        // Tokenize while keeping '+' attached/separated cleanly
        val rawTokens = rawArgs
            .replace("+", " + ")
            .trim()
            .split("\\s+".toRegex())
            .filter { it.isNotEmpty() }

        var dndOverride: Boolean? = null
        val remaining = mutableListOf<String>()
        var idx = 0
        while (idx < rawTokens.size) {
            val tok = rawTokens[idx]
            val tokLower = tok.lowercase(Locale.ROOT)
            when {
                tokLower == "+" -> Unit
                tokLower == "dnd" || tokLower == "+dnd" -> dndOverride = true
                tokLower == "-dnd" -> dndOverride = false
                tokLower == "no" && idx + 1 < rawTokens.size && rawTokens[idx + 1].equals("dnd", ignoreCase = true) -> {
                    dndOverride = false
                    idx++
                }
                else -> remaining += tok
            }
            idx++
        }

        if (remaining.isEmpty()) {
            return FocusCommand.TurnOn(profile = defaultProfile, durationSeconds = null, dndOverride = dndOverride)
        }

        // Try matching a profile name from any non-numeric token (exact match first, then unique prefix match)
        var matchedProfile: FocusProfile? = null
        var matchedTokenIndex = -1

        for (i in remaining.indices) {
            val t = remaining[i]
            if (t.first().isDigit()) continue
            val exact = profiles.firstOrNull { it.name.equals(t, ignoreCase = true) }
            if (exact != null) {
                matchedProfile = exact
                matchedTokenIndex = i
                break
            }
        }

        if (matchedProfile == null) {
            for (i in remaining.indices) {
                val t = remaining[i]
                if (t.first().isDigit()) continue
                val prefixMatches = profiles.filter { it.name.startsWith(t, ignoreCase = true) }
                if (prefixMatches.size == 1) {
                    matchedProfile = prefixMatches.first()
                    matchedTokenIndex = i
                    break
                }
            }
        }

        val durationTokens = if (matchedTokenIndex >= 0) {
            remaining.filterIndexed { i, _ -> i != matchedTokenIndex }
        } else {
            remaining
        }

        val durationSeconds = if (durationTokens.isEmpty()) {
            null
        } else {
            val joined = durationTokens.joinToString(" ")
            val parsed = SmartCommandEngine.parseTimerDuration(joined) ?: return null
            parsed.totalSeconds
        }

        return FocusCommand.TurnOn(
            profile = matchedProfile ?: defaultProfile,
            durationSeconds = durationSeconds,
            dndOverride = dndOverride
        )
    }
}
