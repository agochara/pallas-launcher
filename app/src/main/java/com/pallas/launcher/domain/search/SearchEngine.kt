package com.pallas.launcher.domain.search

import com.pallas.launcher.data.repository.AppUsageTracker
import com.pallas.launcher.domain.model.AppItem
import com.pallas.launcher.domain.model.CommandConfig
import com.pallas.launcher.domain.model.SearchContact
import java.util.Locale

class SearchEngine(
    private val appUsageTracker: AppUsageTracker? = null
) {
    val searchIndex = AppSearchIndex(appUsageTracker)

    fun updateIndex(
        apps: List<AppItem>,
        aliases: Map<String, String>,
        customLabels: Map<String, String> = emptyMap()
    ) {
        searchIndex.buildIndex(apps, aliases, customLabels)
    }

    fun search(rawQuery: String): List<AppSearchResult> = searchIndex.search(rawQuery)

    /**
     * Ordering contract (index 0 = best = the row next to the search pill, Enter picks the first actionable row):
     *  - A query that is JUST a short command prefix ("w", "al", "q", "st", "wa", "sun", "=") is far more likely an
     *    app search than a command. Apps come first, command hint rows trail, and NO network request is fired.
     *    If no app matches at all, the hints/cards are shown exactly as before.
     *  - Full-word commands ("weather", "sunrise", "alarm", "timer", "call", ...) and anything with an argument
     *    ("w tokyo", "al 530") keep command rows first.
     *  - The "Pallas settings" row always trails the app results, so typing "settings" still finds the system app.
     */
    fun searchAll(
        rawQuery: String,
        contacts: List<SearchContact> = emptyList(),
        webSearch: WebSearchConfig = WebSearchConfig(),
        configs: Map<String, CommandConfig> = CommandConfig.ALL_ENABLED,
        defaultWeatherCity: String = ""
    ): Pair<List<LauncherSearchResult>, AsyncCommandRequest?> {
        val trimmed = rawQuery.trim()
        if (trimmed.isEmpty()) return emptyList<LauncherSearchResult>() to null

        val eval = SmartCommandEngine.evaluateCommands(trimmed, contacts, webSearch, configs, defaultWeatherCity)
        val appResults = searchIndex.search(trimmed).map { LauncherSearchResult.App(it) }

        val trailing = eval.immediateResults.filter { it.id == SETTINGS_COMMAND_ID }
        val commandRows = eval.immediateResults.filter { it.id != SETTINGS_COMMAND_ID }

        return (commandRows + appResults + trailing) to eval.asyncRequest
    }

    private companion object {
        /** Typing these in full is an unambiguous command, even though they equal a configured prefix. */
        val FULL_WORD_COMMANDS = setOf("weather", "sunrise", "sunset", "alarm", "timer", "call", "query", "settings", "pallas")
    }
}