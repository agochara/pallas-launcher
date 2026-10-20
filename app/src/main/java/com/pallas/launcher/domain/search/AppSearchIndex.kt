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
        val rawLabel: String,
        val normalizedLabel: String,
        val normalizedWords: List<String>,
        val wordStarts: List<Int>,
        val initials: String,
        val isCustomLabel: Boolean,
        val normToRaw: IntArray
    )

    data class AliasEntry(
        val normalizedAlias: String,
        val targetApp: AppItem
    )

    @Volatile
    private var indexedApps: List<IndexedApp> = emptyList()

    @Volatile
    private var aliasEntries: List<AliasEntry> = emptyList()

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
        // Track exact 1:many/many:1 mapping between normalized chars and raw label positions
        val normBuilder = StringBuilder(label.length)
        val normToRawList = ArrayList<Int>(label.length)
        for (rawIdx in label.indices) {
            val normPart = SearchRanking.normalize(label[rawIdx].toString())
            for (c in normPart) {
                normBuilder.append(c)
                normToRawList.add(rawIdx)
            }
        }
        val normLabel = normBuilder.toString()
        val normToRaw = normToRawList.toIntArray()

        val rawWords = SearchRanking.extractWords(label)
        val words = rawWords.map { SearchRanking.normalize(it) }.filter { it.isNotEmpty() }

        val starts = ArrayList<Int>(words.size)
        val keptWords = ArrayList<String>(words.size)
        val initials = StringBuilder()
        var cursor = 0
        for (w in words) {
            val idx = normLabel.indexOf(w, cursor)
            if (idx == -1) continue
            starts += idx
            keptWords += w
            initials.append(w[0])
            cursor = idx + w.length
        }

        return IndexedApp(
            app = app,
            rawLabel = label,
            normalizedLabel = normLabel,
            normalizedWords = keptWords,
            wordStarts = starts,
            initials = initials.toString(),
            isCustomLabel = isCustom,
            normToRaw = normToRaw
        )
    }

    private fun mapRangesToRaw(ranges: List<IntRange>, normToRaw: IntArray): List<IntRange> {
        if (ranges.isEmpty() || normToRaw.isEmpty()) return emptyList()
        val rawIndices = mutableListOf<Int>()
        for (r in ranges) {
            val start = r.first.coerceIn(0, normToRaw.lastIndex)
            val end = r.last.coerceIn(0, normToRaw.lastIndex)
            for (i in start..end) {
                rawIndices.add(normToRaw[i])
            }
        }
        return consolidate(rawIndices.distinct().sorted())
    }

    fun search(rawQuery: String, limit: Int = 8): List<AppSearchResult> {
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
                val frecency = appUsageTracker?.getUsageBonus(entry.targetApp.uniqueKey) ?: 0.0
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

            val frecency = appUsageTracker?.getUsageBonus(item.app.uniqueKey) ?: 0.0
            val lengthPenalty = label.length * 0.5
            val score = tier.baseScore + sub + frecency - lengthPenalty
            val canHighlight = !item.isCustomLabel
            val mappedRanges = if (canHighlight) mapRangesToRaw(ranges, item.normToRaw) else emptyList()
            offer(item.app, score, mappedRanges)
        }

        return candidates.values
            .sortedWith(
                compareByDescending<AppSearchResult> { it.score }
                    .thenBy { it.app.label.lowercase(Locale.getDefault()) }
            )
            .take(limit)
    }

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