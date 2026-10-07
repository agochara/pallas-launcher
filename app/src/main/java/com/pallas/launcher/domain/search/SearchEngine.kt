package com.pallas.launcher.domain.search

import com.pallas.launcher.data.repository.AppUsageTracker
import com.pallas.launcher.domain.model.AppItem

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
}