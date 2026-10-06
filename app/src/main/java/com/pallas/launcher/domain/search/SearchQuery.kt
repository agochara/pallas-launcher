package com.pallas.launcher.domain.search

data class SearchQuery(
    val raw: String
) {
    val trimmed: String = raw.trim()
    val normalized: String = trimmed.lowercase()
    val tokens: List<String> = normalized.split("\\s+".toRegex()).filter { it.isNotBlank() }

    val commandVerb: String? = tokens.firstOrNull()
    val commandArg: String = if (tokens.size > 1) trimmed.substringAfter(tokens[0]).trim() else ""

    fun isBlank(): Boolean = trimmed.isBlank()
}
