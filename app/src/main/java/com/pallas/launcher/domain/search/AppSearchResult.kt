package com.pallas.launcher.domain.search

import com.pallas.launcher.domain.model.AppItem

data class AppSearchResult(
    val app: AppItem,
    val score: Double,
    val matchedRanges: List<IntRange> = emptyList()
) {
    val id: String
        get() = app.uniqueKey
}
