package com.pallas.launcher.domain.search

import com.pallas.launcher.data.repository.AppUsageTracker
import com.pallas.launcher.domain.model.AppItem
import com.pallas.launcher.domain.model.SearchContact

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

    fun searchAll(
        rawQuery: String,
        contacts: List<SearchContact> = emptyList()
    ): List<LauncherSearchResult> {
        val trimmed = rawQuery.trim()
        if (trimmed.isEmpty()) return emptyList()

        val commands = SmartCommandEngine.evaluateCommands(trimmed, contacts)
        val appResults = searchIndex.search(trimmed).map { LauncherSearchResult.App(it) }

        return commands + appResults
    }
}