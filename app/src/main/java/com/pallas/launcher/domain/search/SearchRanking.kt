package com.pallas.launcher.domain.search

import java.text.Normalizer
import java.util.Locale

data class MatchResult(
    val score: Double,
    val matchedRanges: List<IntRange> = emptyList()
)

object SearchRanking {

    private val DIACRITICS_REGEX = """\p{InCombiningDiacriticalMarks}+""".toRegex()
    private val CAMEL_CASE_1 = "(?<=[a-z])(?=[A-Z])".toRegex()
    private val CAMEL_CASE_2 = "(?<=[A-Z])(?=[A-Z][a-z])".toRegex()
    private val WORD_SPLIT = """[\s_\-.,:/]+""".toRegex()

    fun normalize(input: String): String {
        for (i in 0 until input.length) {
            if (input[i].code >= 128) {
                val nfd = Normalizer.normalize(input, Normalizer.Form.NFD)
                return nfd.replace(DIACRITICS_REGEX, "")
                    .lowercase(Locale.ROOT)
            }
        }
        return input.lowercase(Locale.ROOT)
    }

    fun extractWords(input: String): List<String> {
        val normalized = input.trim()
        if (normalized.isEmpty()) return emptyList()

        val withSpaces = normalized.replace(CAMEL_CASE_1, " ")
            .replace(CAMEL_CASE_2, " ")

        return withSpaces.split(WORD_SPLIT).filter { it.isNotBlank() }
    }

    fun evaluateMatch(query: String, target: String, allowFuzzy: Boolean = true): MatchResult {
        val qRaw = query.trim()
        val tRaw = target.trim()

        if (qRaw.isEmpty() || tRaw.isEmpty()) return MatchResult(0.0)

        val q = normalize(qRaw)
        val t = normalize(tRaw)

        // 1. Exact Match
        if (q == t) {
            return MatchResult(2000.0, listOf(0 until tRaw.length))
        }

        // 2. Exact Prefix Match
        if (t.startsWith(q)) {
            val ratio = q.length.toDouble() / t.length.toDouble()
            val score = 1500.0 + (ratio * 100.0)
            return MatchResult(score, listOf(0 until q.length))
        }

        // 3. Acronym / Initials Match (e.g. "wa" for "WhatsApp", "yt" for "YouTube", "calc" for "Calculator")
        val words = extractWords(tRaw)
        val acronymBuilder = StringBuilder()
        val wordStartIndices = mutableListOf<Int>()

        var searchCursor = 0
        for (word in words) {
            val normWord = normalize(word)
            val startIdx = t.indexOf(normWord, searchCursor)
            if (startIdx != -1) {
                acronymBuilder.append(word.first().lowercaseChar())
                wordStartIndices.add(startIdx)
                searchCursor = startIdx + normWord.length
            }
        }
        val acronym = acronymBuilder.toString()

        if (acronym.isNotEmpty() && acronym.startsWith(q)) {
            val matchedRanges = (0 until q.length).map { idx ->
                val charPos = wordStartIndices[idx]
                charPos..charPos
            }
            val score = 1300.0 + (q.length.toDouble() / acronym.length.toDouble() * 50.0)
            return MatchResult(score, matchedRanges)
        }

        // 4. Word-level Prefix Match
        // e.g. "calc" matches "Quick Calculator", "play" matches "Google Play"
        var wordSearchCursor = 0
        for (word in words) {
            val wordNorm = normalize(word)
            val startIdx = t.indexOf(wordNorm, wordSearchCursor)
            val effectiveStart = if (startIdx != -1) {
                wordSearchCursor = startIdx + wordNorm.length
                startIdx
            } else {
                t.indexOf(wordNorm)
            }
            if (wordNorm.startsWith(q) && effectiveStart != -1) {
                val ranges = listOf(effectiveStart until (effectiveStart + q.length))
                val positionPenalty = effectiveStart * 2.0
                val score = 1100.0 - positionPenalty + (q.length.toDouble() / wordNorm.length.toDouble() * 50.0)
                return MatchResult(maxOf(score, 600.0), ranges)
            }
        }

        // 5. Substring Match
        val substrIndex = t.indexOf(q)
        if (substrIndex != -1) {
            val ranges = listOf(substrIndex until (substrIndex + q.length))
            val score = 800.0 - (substrIndex * 4.0)
            return MatchResult(maxOf(score, 500.0), ranges)
        }

        val isFuzzyAllowed = allowFuzzy && (q.length >= 3 || (q.length == 2 && t.isNotEmpty() && t[0] == q[0]))
        if (!isFuzzyAllowed) return MatchResult(0.0)

        // 6. Fuzzy Subsequence Match
        var qIdx = 0
        var tIdx = 0
        var consecutiveMatches = 0
        var scoreBonus = 0.0
        val matchedIndices = mutableListOf<Int>()

        while (qIdx < q.length && tIdx < t.length) {
            if (q[qIdx] == t[tIdx]) {
                matchedIndices.add(tIdx)

                // Word start bonus
                val isWordStart = tIdx == 0 || !t[tIdx - 1].isLetterOrDigit() ||
                        (tRaw[tIdx].isUpperCase() && (tIdx > 0 && !tRaw[tIdx - 1].isUpperCase()))

                if (isWordStart) {
                    scoreBonus += 30.0
                }

                // Consecutive match bonus
                scoreBonus += 15.0 + (consecutiveMatches * 10.0)
                consecutiveMatches++
                qIdx++
            } else {
                consecutiveMatches = 0
            }
            tIdx++
        }

        if (qIdx == q.length) {
            // All characters found in order
            val gapPenalty = (t.length - q.length) * 2.0
            val startPenalty = (matchedIndices.firstOrNull() ?: 0) * 3.0
            val totalScore = maxOf(50.0, 400.0 + scoreBonus - gapPenalty - startPenalty)

            // Consolidate continuous indices into ranges
            val ranges = consolidateRanges(matchedIndices)
            return MatchResult(totalScore, ranges)
        }

        return MatchResult(0.0)
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

    fun calculateMatchScore(query: String, target: String): Double {
        return evaluateMatch(query, target).score
    }
}
