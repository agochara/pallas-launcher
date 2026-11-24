package com.pallas.launcher.domain.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

enum class FocusDndMode {
    PRIORITY,
    ALARMS_ONLY,
    TOTAL_SILENCE
}

data class FocusProfile(
    val id: String,
    val name: String,
    val allowedScreenIds: Set<String> = emptySet(),
    val allowedAppKeys: Set<String> = emptySet(),
    val syncDnd: Boolean = false,
    val dndMode: FocusDndMode = FocusDndMode.PRIORITY,
    val muteRinger: Boolean = false,
    val disabledCommands: Set<String> = emptySet(),
    val onEnterMacroId: String? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("allowedScreenIds", JSONArray(allowedScreenIds.sorted()))
        put("allowedAppKeys", JSONArray(allowedAppKeys.sorted()))
        put("syncDnd", syncDnd)
        put("dndMode", dndMode.name)
        put("muteRinger", muteRinger)
        put("disabledCommands", JSONArray(disabledCommands.sorted()))
        if (!onEnterMacroId.isNullOrBlank()) put("onEnterMacroId", onEnterMacroId)
    }

    companion object {
        private val DURATION_TOKEN_REGEX = Regex(
            """^\d+\s*(h|hr|hrs|hour|hours|m|min|mins|minute|minutes|s|sec|secs|second|seconds)$""",
            RegexOption.IGNORE_CASE
        )

        val RESERVED_NAMES = setOf(
            "off", "on", "dnd", "give", "up", "all", "none", "default", "status",
            "stop", "end", "cancel", "min", "mins", "minute", "minutes",
            "hr", "hrs", "hour", "hours", "h", "m", "s", "sec", "secs", "focus"
        )

        /**
         * Returns an error message if [raw] is not a valid Focus Profile name, or `null` if valid.
         */
        fun validateName(
            raw: String,
            existingNames: Collection<String> = emptyList(),
            ignoreExistingName: String? = null
        ): String? {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return "Profile name cannot be empty"
            if (trimmed.any { it.isWhitespace() }) return "Profile name cannot contain spaces"
            if (trimmed.contains('+') || trimmed.contains('-')) return "Profile name cannot contain '+' or '-'"
            if (trimmed.all { it.isDigit() }) return "Profile name cannot be only numbers"
            if (DURATION_TOKEN_REGEX.matches(trimmed)) return "Profile name cannot look like a duration"
            val lower = trimmed.lowercase(Locale.ROOT)
            if (lower in RESERVED_NAMES) return "'$trimmed' is a reserved command keyword"
            val conflict = existingNames.any { existing ->
                existing.trim().equals(trimmed, ignoreCase = true) &&
                    (ignoreExistingName == null || !existing.trim().equals(ignoreExistingName.trim(), ignoreCase = true))
            }
            if (conflict) return "A profile named '$trimmed' already exists"
            return null
        }

        fun fromJson(obj: JSONObject): FocusProfile? {
            val id = if (obj.isNull("id")) "" else obj.optString("id", "").trim()
            val name = if (obj.isNull("name")) "" else obj.optString("name", "").trim()
            if (id.isEmpty() || name.isEmpty()) return null

            fun stringSet(key: String): Set<String> {
                val arr = obj.optJSONArray(key) ?: return emptySet()
                return buildSet {
                    for (i in 0 until arr.length()) {
                        val s = if (!arr.isNull(i)) arr.optString(i).trim() else ""
                        if (s.isNotEmpty()) add(s)
                    }
                }
            }

            val dndModeRaw = if (obj.isNull("dndMode")) FocusDndMode.PRIORITY.name else obj.optString("dndMode", FocusDndMode.PRIORITY.name)
            val dndMode = FocusDndMode.entries.firstOrNull { it.name == dndModeRaw } ?: FocusDndMode.PRIORITY
            val onEnter = if (obj.isNull("onEnterMacroId")) null else obj.optString("onEnterMacroId", "").trim().takeIf { it.isNotEmpty() }

            return FocusProfile(
                id = id,
                name = name,
                allowedScreenIds = stringSet("allowedScreenIds"),
                allowedAppKeys = stringSet("allowedAppKeys"),
                syncDnd = obj.optBoolean("syncDnd", false),
                dndMode = dndMode,
                muteRinger = obj.optBoolean("muteRinger", false),
                disabledCommands = stringSet("disabledCommands"),
                onEnterMacroId = onEnter
            )
        }
    }
}
