package com.pallas.launcher.domain.search

import com.pallas.launcher.data.repository.AppUsageTracker
import com.pallas.launcher.domain.model.AppItem
import java.util.Locale

enum class MatchTier(val baseScore: Double) {
    ALIAS(10000.0),
    EXACT(8000.0),
    PREFIX(6000.0),
    SUBSTRING(4000.0),
    FUZZY(2000.0),
    NONE(0.0)
}

class AppSearchIndex(
    private val appUsageTracker: AppUsageTracker? = null
) {
    data class IndexedApp(
        val app: AppItem,
        val normalizedLabel: String,
        val words: List<String>,
        val normalizedWords: List<String>,
        val initials: String,
        val wordStartIndices: List<Int>
    )

    data class AliasEntry(
        val normalizedAlias: String,
        val targetApp: AppItem
    )

    @Volatile
    private var indexedApps: List<IndexedApp> = emptyList()

    @Volatile
    private var aliasEntries: List<AliasEntry> = emptyList()

    fun buildIndex(apps: List<AppItem>, aliases: Map<String, String>) {
        val newIndexed = apps.map { app ->
            val normLabel = SearchRanking.normalize(app.label)
            val words = SearchRanking.extractWords(app.label)
            val normWords = words.map { SearchRanking.normalize(it) }

            val initialsBuilder = StringBuilder()
            val startIndices = mutableListOf<Int>()
            for (word in words) {
                val normW = SearchRanking.normalize(word)
                val idx = normLabel.indexOf(normW)
                if (idx != -1) {
                    initialsBuilder.append(normW.firstOrNull() ?: "")
                    startIndices.add(idx)
                }
            }

            IndexedApp(
                app = app,
                normalizedLabel = normLabel,
                words = words,
                normalizedWords = normWords,
                initials = initialsBuilder.toString(),
                wordStartIndices = startIndices
            )
        }

        val newAliases = mutableListOf<AliasEntry>()
        for ((alias, target) in aliases) {
            val normAlias = SearchRanking.normalize(alias.trim())
            if (normAlias.isEmpty()) continue

            // Resolve target app by exact uniqueKey, packageName, or activityName
            val targetApp = apps.find {
                it.uniqueKey == target ||
                it.packageName.equals(target, ignoreCase = true) ||
                it.activityName.equals(target, ignoreCase = true)
            }
            if (targetApp != null) {
                newAliases.add(AliasEntry(normAlias, targetApp))
            }
        }

        indexedApps = newIndexed
        aliasEntries = newAliases
    }

    fun search(rawQuery: String): List<AppSearchResult> {
        val trimmed = rawQuery.trim()
        if (trimmed.isEmpty()) return emptyList()

        val q = SearchRanking.normalize(trimmed)
        if (q.isEmpty()) return emptyList()

        val candidates = mutableMapOf<String, AppSearchResult>()

        // 1. Check User Aliases: Tier 0 (base 10000.0)
        for (aliasEntry in aliasEntries) {
            if (q == aliasEntry.normalizedAlias) {
                val app = aliasEntry.targetApp
                val frecency = appUsageTracker?.getUsageBonus(app.packageName) ?: 0.0
                val score = MatchTier.ALIAS.baseScore + frecency
                candidates[app.uniqueKey] = AppSearchResult(
                    app = app,
                    score = score,
                    matchedRanges = listOf(0 until app.label.length)
                )
            }
        }

        // 2. Search indexed apps
        val qLen = q.length

        for (item in indexedApps) {
            val label = item.normalizedLabel
            val app = item.app

            var matchTier = MatchTier.NONE
            var matchedRanges = emptyList<IntRange>()

            // Tier 1: Exact match
            if (q == label) {
                matchTier = MatchTier.EXACT
                matchedRanges = listOf(0 until app.label.length)
            }
            // Tier 2: Prefix tier (label prefix, word prefix, initials)
            else if (label.startsWith(q)) {
                matchTier = MatchTier.PREFIX
                matchedRanges = listOf(0 until qLen)
            } else if (item.initials.isNotEmpty() && item.initials.startsWith(q)) {
                matchTier = MatchTier.PREFIX
                matchedRanges = (0 until qLen).map { idx ->
                    val pos = item.wordStartIndices.getOrElse(idx) { 0 }
                    pos..pos
                }
            } else {
                var wordPrefixIndex = -1
                for (w in item.normalizedWords) {
                    if (w.startsWith(q)) {
                        wordPrefixIndex = label.indexOf(w)
                        break
                    }
                }
                if (wordPrefixIndex != -1) {
                    matchTier = MatchTier.PREFIX
                    matchedRanges = listOf(wordPrefixIndex until (wordPrefixIndex + qLen))
                } else if (label.contains(q)) {
                    // Tier 3: Substring
                    val idx = label.indexOf(q)
                    matchTier = MatchTier.SUBSTRING
                    matchedRanges = listOf(idx until (idx + qLen))
                } else {
                    // Tier 4: Fuzzy (3+ chars, or 2 chars when first char matches label's first char)
                    val fuzzyRanges = checkFuzzyMatch(q, label)
                    if (fuzzyRanges != null) {
                        matchTier = MatchTier.FUZZY
                        matchedRanges = fuzzyRanges
                    }
                }
            }

            if (matchTier != MatchTier.NONE) {
                val frecency = appUsageTracker?.getUsageBonus(app.packageName) ?: 0.0
                val score = matchTier.baseScore + frecency

                val existing = candidates[app.uniqueKey]
                if (existing == null || score > existing.score) {
                    candidates[app.uniqueKey] = AppSearchResult(
                        app = app,
                        score = score,
                        matchedRanges = matchedRanges
                    )
                }
            }
        }

        return candidates.values
            .sortedWith(
                compareByDescending<AppSearchResult> { it.score }
                    .thenBy { it.app.label.lowercase(Locale.getDefault()) }
            )
            .take(8)
    }

    private fun checkFuzzyMatch(q: String, target: String): List<IntRange>? {
        val qLen = q.length
        val tLen = target.length
        if (tLen < qLen) return null

        val isFuzzyAllowed = qLen >= 3 || (qLen == 2 && target.isNotEmpty() && target[0] == q[0])
        if (!isFuzzyAllowed) return null

        var qIdx = 0
        var tIdx = 0
        val matched = mutableListOf<Int>()

        while (qIdx < qLen && tIdx < tLen) {
            if (q[qIdx] == target[tIdx]) {
                matched.add(tIdx)
                qIdx++
            }
            tIdx++
        }

        return if (qIdx == qLen) consolidateRanges(matched) else null
    }

    private fun consolidateRanges(indices: List<Int>): List<IntRange> {
        if (indices.isEmpty()) return emptyList()
        val sorted = indices.sorted()
        val ranges = mutableListOf<IntRange>()
        var start = sorted[0]
        var prev = start
        for (i in 1 until sorted.size) {
            val curr = sorted[i]
            if (curr == prev + 1) {
                prev = curr
            } else {
                ranges.add(start..prev)
                start = curr
                prev = curr
            }
        }
        ranges.add(start..prev)
        return ranges
    }
}
