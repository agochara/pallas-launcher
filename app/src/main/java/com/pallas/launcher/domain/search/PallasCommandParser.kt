package com.pallas.launcher.domain.search

import com.pallas.launcher.domain.math.MathCheatsheets
import java.util.Locale

sealed interface PallasTarget {
    data object SettingsMain : PallasTarget
    data object FocusSettings : PallasTarget
    data object MacrosSettings : PallasTarget
    data object WorldAtlas : PallasTarget
    data object Notebook : PallasTarget
    data object LatexStudio : PallasTarget
    data class GuideTab(val tabIndex: Int) : PallasTarget
    data class Cheatsheet(
        val topicId: String,
        val topicTitle: String,
        val formulaTitle: String? = null
    ) : PallasTarget

    fun encode(): String = when (this) {
        SettingsMain -> "settings"
        FocusSettings -> "focus"
        MacrosSettings -> "macros"
        WorldAtlas -> "atlas"
        Notebook -> "notebook"
        LatexStudio -> "latex"
        is GuideTab -> "guide_tab:$tabIndex"
        is Cheatsheet -> if (formulaTitle.isNullOrBlank()) {
            "cheatsheet:$topicId"
        } else {
            "cheatsheet:$topicId:$formulaTitle"
        }
    }

    companion object {
        fun decode(encoded: String): PallasTarget? = when {
            encoded == "settings" -> SettingsMain
            encoded == "focus" -> FocusSettings
            encoded == "macros" -> MacrosSettings
            encoded == "atlas" -> WorldAtlas
            encoded == "notebook" -> Notebook
            encoded == "latex" -> LatexStudio
            encoded.startsWith("guide_tab:") -> {
                val idx = encoded.removePrefix("guide_tab:").toIntOrNull()?.coerceIn(0, 2) ?: 0
                GuideTab(idx)
            }
            encoded.startsWith("cheatsheet:") -> {
                val rest = encoded.removePrefix("cheatsheet:")
                val sep = rest.indexOf(':')
                if (sep == -1) {
                    val topic = MathCheatsheets.topicById(rest)
                    Cheatsheet(rest, topic?.shortTitle ?: rest, null)
                } else {
                    val topicId = rest.substring(0, sep)
                    val formulaTitle = rest.substring(sep + 1)
                    val topic = MathCheatsheets.topicById(topicId)
                    Cheatsheet(topicId, topic?.shortTitle ?: topicId, formulaTitle)
                }
            }
            else -> null
        }
    }
}

data class PallasNavMatch(
    val target: PallasTarget,
    val title: String,
    val subtitle: String
)

object PallasCommandParser {

    fun resolve(rawQuery: String): List<PallasNavMatch> {
        val trimmed = rawQuery.trim()
        val lower = trimmed.lowercase(Locale.ROOT)
        if (lower == "pallas") {
            return listOf(
                PallasNavMatch(
                    target = PallasTarget.SettingsMain,
                    title = "Pallas settings",
                    subtitle = "Open launcher settings (or try 'pallas engine', 'pallas stats', 'pallas jensen')"
                )
            )
        }
        if (!lower.startsWith("pallas ")) return emptyList()

        val arg = trimmed.drop(7).trim()
        if (arg.isEmpty()) return emptyList()
        val argLower = arg.lowercase(Locale.ROOT)

        when (argLower) {
            "settings", "setting", "prefs", "config" -> return listOf(
                PallasNavMatch(PallasTarget.SettingsMain, "Pallas settings", "Open launcher settings")
            )
            "engine", "math", "manual", "guide" -> return listOf(
                PallasNavMatch(PallasTarget.GuideTab(0), "Pallas Guide · Engine", "Open math & engine reference")
            )
            "cheatsheet", "cheatsheets", "sheet", "sheets", "ref", "reference" -> return listOf(
                PallasNavMatch(PallasTarget.GuideTab(1), "Pallas Guide · Cheatsheets", "Browse all offline reference sheets")
            )
            "commands", "command", "cmds", "cmd" -> return listOf(
                PallasNavMatch(PallasTarget.GuideTab(2), "Pallas Guide · Commands", "Browse all launcher commands & prefixes")
            )
            "atlas", "map" -> return listOf(
                PallasNavMatch(PallasTarget.WorldAtlas, "World Atlas", "Open offline physical & political map")
            )
            "notebook", "nb" -> return listOf(
                PallasNavMatch(PallasTarget.Notebook, "Computational Notebook", "Open multi-cell math workspace")
            )
            "latex", "studio" -> return listOf(
                PallasNavMatch(PallasTarget.LatexStudio, "LaTeX Document Studio", "Open LaTeX editor & exporter")
            )
            "focus" -> return listOf(
                PallasNavMatch(PallasTarget.FocusSettings, "Focus Mode Settings", "Configure profiles, DND & schedule")
            )
            "macros", "macro", "routines" -> return listOf(
                PallasNavMatch(PallasTarget.MacrosSettings, "Macros & Routines", "Configure custom action chains")
            )
        }

        val matches = mutableListOf<PallasNavMatch>()
        val scoredTopics = MathCheatsheets.TOPICS.mapNotNull { topic ->
            val aliases = MathCheatsheets.aliasesFor(topic)
            val titleLower = topic.title.lowercase(Locale.ROOT)
            val shortLower = topic.shortTitle.lowercase(Locale.ROOT)
            val idNorm = topic.id.replace('_', ' ')
            val score = when {
                titleLower == argLower || shortLower == argLower || idNorm == argLower -> 100
                aliases.any { it.equals(argLower, ignoreCase = true) } -> 95
                titleLower.startsWith(argLower) || shortLower.startsWith(argLower) -> 80
                aliases.any { it.startsWith(argLower, ignoreCase = true) } -> 75
                titleLower.contains(argLower) || shortLower.contains(argLower) || aliases.any { it.contains(argLower, ignoreCase = true) } -> 60
                else -> 0
            }
            if (score > 0) score to topic else null
        }.sortedByDescending { it.first }

        scoredTopics.take(2).forEach { (_, topic) ->
            matches += PallasNavMatch(
                target = PallasTarget.Cheatsheet(topic.id, topic.shortTitle),
                title = "Cheatsheet: ${topic.title}",
                subtitle = "${topic.formulaCount} formulas · Jump to sheet"
            )
        }

        // Also search individual formulas & footnotes for direct equation jumps (e.g. `pallas jensen`, `pallas buckling`)
        val formulaHits = MathCheatsheets.searchFormulas(arg).take(3)
        formulaHits.forEach { hit ->
            matches += PallasNavMatch(
                target = PallasTarget.Cheatsheet(hit.topic.id, hit.topic.shortTitle, hit.formula.title),
                title = hit.formula.title,
                subtitle = "${hit.topic.shortTitle} · ${hit.subsection.shortTitle}"
            )
        }

        if (matches.isEmpty()) {
            matches += PallasNavMatch(
                target = PallasTarget.GuideTab(1),
                title = "Open Pallas Guide",
                subtitle = "No direct match for \"$arg\""
            )
        }
        return matches.distinctBy { it.target.encode() }
    }
}
