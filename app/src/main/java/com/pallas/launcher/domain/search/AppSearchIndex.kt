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
        val normalizedWords: List<String>,
        /** start offset of each word inside [normalizedLabel] (same length as [normalizedWords]) */
        val wordStarts: List<Int>,
        val initials: String,
        /** true when this entry was built from a user-renamed label (ranges don't map onto app.label) */
        val isCustomLabel: Boolean
    )

    data class AliasEntry(
        val normalizedAlias: String,
        val targetApp: AppItem
    )

    @Volatile
    private var indexedApps: List<IndexedApp> = emptyList()

    @Volatile
    private var aliasEntries: List<AliasEntry> = emptyList()

    /**
     * @param customLabels uniqueKey -> user-chosen label (Rename Display Label). Both the original and the
     *                     custom label are searchable.
     */
    fun buildIndex(
        apps: List<AppItem>,
        aliases: Map<String, String>,
        customLabels: Map<String, String> = emptyMap()
    ) {
        val entries = ArrayList<IndexedApp>(apps.size + customLabels.size)
        for (app in apps) {
            entries += makeEntry(app, app.label, isCustom = false)
            val custom = customLabels[app.uniqueKey]?.trim().orEmpty()
            if (custom.isNotEmpty() && !custom.equals(app.label, ignoreCase = true)) {
                entries += makeEntry(app, custom, isCustom = true)
            }
        }

        val newAliases = mutableListOf<AliasEntry>()
        for ((alias, target) in aliases) {
            val normAlias = SearchRanking.normalize(alias.trim())
            if (normAlias.isEmpty()) continue

            val normTarget = SearchRanking.normalize(target.trim())
            // Aliases created from the long-press menu store a package name / uniqueKey,
            // aliases typed in Settings store an app NAME - support both.
            val targetApp = apps.firstOrNull { it.uniqueKey == target }
                ?: apps.firstOrNull { it.packageName.equals(target, ignoreCase = true) }
                ?: apps.firstOrNull { it.activityName.equals(target, ignoreCase = true) }
                ?: apps.firstOrNull { SearchRanking.normalize(it.label) == normTarget }
            if (targetApp != null) newAliases += AliasEntry(normAlias, targetApp)
        }

        indexedApps = entries
        aliasEntries = newAliases
    }

    private fun makeEntry(app: AppItem, label: String, isCustom: Boolean): IndexedApp {
        val normLabel = SearchRanking.normalize(label)
        val words = SearchRanking.extractWords(label).map { SearchRanking.normalize(it) }.filter { it.isNotEmpty() }

        val starts = ArrayList<Int>(words.size)
        val keptWords = ArrayList<String>(words.size)
        val initials = StringBuilder()
        var cursor = 0
        for (w in words) {
            // search from the running cursor so repeated words ("Google Google") don't all map to the first one
            val idx = normLabel.indexOf(w, cursor)
            if (idx == -1) continue
            starts += idx
            keptWords += w
            initials.append(w[0])
            cursor = idx + w.length
        }

        return IndexedApp(
            app = app,
            normalizedLabel = normLabel,
            normalizedWords = keptWords,
            wordStarts = starts,
            initials = initials.toString(),
            isCustomLabel = isCustom
        )
    }

    fun search(rawQuery: String): List<AppSearchResult> {
        val trimmed = rawQuery.trim()
        if (trimmed.isEmpty()) return emptyList()
        val q = SearchRanking.normalize(trimmed)
        if (q.isEmpty()) return emptyList()
        val qLen = q.length

        val candidates = HashMap<String, AppSearchResult>()

        fun offer(app: AppItem, score: Double, ranges: List<IntRange>) {
            val existing = candidates[app.uniqueKey]
            if (existing == null || score > existing.score) {
                candidates[app.uniqueKey] = AppSearchResult(app = app, score = score, matchedRanges = ranges)
            }
        }

        // 1. aliases (exact)
        for (entry in aliasEntries) {
            if (q == entry.normalizedAlias) {
                val frecency = appUsageTracker?.getUsageBonus(entry.targetApp.packageName) ?: 0.0
                offer(entry.targetApp, MatchTier.ALIAS.baseScore + frecency, emptyList())
            }
        }

        // 2. label matching
        for (item in indexedApps) {
            val label = item.normalizedLabel
            var tier = MatchTier.NONE
            var sub = 0.0
            var ranges: List<IntRange> = emptyList()

            if (q == label) {
                tier = MatchTier.EXACT
                ranges = listOf(0 until qLen)
            } else if (label.startsWith(q)) {
                tier = MatchTier.PREFIX; sub = 60.0
                ranges = listOf(0 until qLen)
            } else {
                val wordIdx = item.normalizedWords.indexOfFirst { it.startsWith(q) }
                if (wordIdx != -1) {
                    tier = MatchTier.PREFIX; sub = 30.0 - wordIdx * 5.0
                    val s = item.wordStarts[wordIdx]
                    ranges = listOf(s until (s + qLen))
                } else if (qLen >= 2 && item.initials.startsWith(q)) {
                    // "wa" -> WhatsApp-style initials ("Whats App"), "gm" -> "Google Maps"
                    tier = MatchTier.PREFIX; sub = 0.0
                    ranges = (0 until qLen).map { item.wordStarts[it].let { p -> p..p } }
                } else {
                    val idx = label.indexOf(q)
                    if (idx != -1) {
                        tier = MatchTier.SUBSTRING
                        sub = -idx.toDouble()
                        ranges = listOf(idx until (idx + qLen))
                    } else {
                        val fuzzy = fuzzyRanges(q, label, item.wordStarts)
                        if (fuzzy != null) {
                            tier = MatchTier.FUZZY
                            ranges = fuzzy
                        }
                    }
                }
            }

            if (tier == MatchTier.NONE) continue

            val frecency = appUsageTracker?.getUsageBonus(item.app.packageName) ?: 0.0
            // shorter labels first inside the same tier ("Camera" before "Camera Pro Studio")
            val lengthPenalty = label.length * 0.5
            val score = tier.baseScore + sub + frecency - lengthPenalty
            offer(item.app, score, if (item.isCustomLabel) emptyList() else ranges)
        }

        return candidates.values
            .sortedWith(
                compareByDescending<AppSearchResult> { it.score }
                    .thenBy { it.app.label.lowercase(Locale.getDefault()) }
            )
            .take(8)
    }

    /**
     * Subsequence match with guard rails so it doesn't return noise:
     *  - at least 3 chars
     *  - first matched char must start a word
     *  - matched characters must be reasonably compact (span <= 2x query length)
     */
    private fun fuzzyRanges(q: String, target: String, wordStarts: List<Int>): List<IntRange>? {
        if (q.length < 3 || target.length < q.length) return null

        var qi = 0
        val matched = ArrayList<Int>(q.length)
        for (ti in target.indices) {
            if (qi < q.length && q[qi] == target[ti]) {
                matched += ti
                qi++
            }
        }
        if (qi != q.length) return null
        if (matched.first() !in wordStarts) return null
        if (matched.last() - matched.first() + 1 > q.length * 2) return null
        return consolidate(matched)
    }

    private fun consolidate(indices: List<Int>): List<IntRange> {
        if (indices.isEmpty()) return emptyList()
        val ranges = mutableListOf<IntRange>()
        var start = indices[0]
        var prev = start
        for (i in 1 until indices.size) {
            val cur = indices[i]
            if (cur == prev + 1) prev = cur else {
                ranges += start..prev
                start = cur; prev = cur
            }
        }
        ranges += start..prev
        return ranges
    }
}