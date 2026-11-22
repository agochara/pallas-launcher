package com.pallas.launcher

import com.hrm.latex.parser.LatexParser
import com.pallas.launcher.domain.math.MathCheatsheets
import com.pallas.launcher.domain.model.AppItem
import com.pallas.launcher.domain.model.CommandConfig
import com.pallas.launcher.domain.search.LauncherSearchResult
import com.pallas.launcher.domain.search.MathStatsEngine
import com.pallas.launcher.domain.search.SearchEngine
import com.pallas.launcher.domain.search.SmartCommandEngine
import com.pallas.launcher.domain.search.SmartCommandType
import com.pallas.launcher.domain.search.isEnterEligible
import com.pallas.launcher.domain.search.isInertOnTap
import com.pallas.launcher.ui.components.preprocessLatexForRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TexAndCheatsheetTests {

    @Test
    fun normalizeUserLatexStripsDocumentAndOuterMathDelimiters() {
        assertEquals(
            "\\frac{a}{b}",
            SmartCommandEngine.normalizeUserLatex("$$ \\frac{a}{b} $$")
        )
        assertEquals(
            "\\alpha + \\beta",
            SmartCommandEngine.normalizeUserLatex("$\\alpha + \\beta$")
        )
        assertEquals(
            "\\int_0^1 x\\,dx",
            SmartCommandEngine.normalizeUserLatex("\\[ \\int_0^1 x\\,dx \\]")
        )
        assertEquals(
            "e^{i\\pi} + 1 = 0",
            SmartCommandEngine.normalizeUserLatex("\\( e^{i\\pi} + 1 = 0 \\)")
        )
        assertEquals(
            "\\sum_{n=1}^{\\infty} \\frac{1}{n^2} = \\frac{\\pi^2}{6}",
            SmartCommandEngine.normalizeUserLatex(
                "\\documentclass{article}\n\\usepackage{amsmath}\n\\begin{document}\n$$\\sum_{n=1}^{\\infty} \\frac{1}{n^2} = \\frac{\\pi^2}{6}$$\n\\end{document}"
            )
        )
        // Multiple inline expressions must not have outer delimiters stripped blindly
        assertEquals(
            "\$a\$ + \$b\$",
            SmartCommandEngine.normalizeUserLatex("\$a\$ + \$b\$")
        )
        // Plain LaTeX passes through cleanly
        assertEquals(
            "\\int_0^\\infty e^{-x^2}\\,dx",
            SmartCommandEngine.normalizeUserLatex("  \\int_0^\\infty e^{-x^2}\\,dx  ")
        )
    }

    @Test
    fun texCommandProducesHintAndPreviewCards() {
        val hintEval = SmartCommandEngine.evaluateCommands("tex", emptyList())
        val hint = hintEval.immediateResults.single() as LauncherSearchResult.Command
        assertEquals("cmd_tex_hint", hint.id)
        assertEquals(SmartCommandType.LATEX, hint.type)
        assertTrue(hint.isInertOnTap)
        assertFalse(hint.isEnterEligible)

        val previewEval = SmartCommandEngine.evaluateCommands("tex \\frac{1}{2}", emptyList())
        val card = previewEval.immediateResults.single() as LauncherSearchResult.RichCard
        assertEquals(SmartCommandType.LATEX, card.type)
        assertEquals("\\frac{1}{2}", card.latexSource)
        assertEquals("LaTeX", card.badgeText)
        assertTrue(card.isEnterEligible)
        assertFalse(card.isInertOnTap)
    }

    @Test
    fun texCommandRespectsCustomPrefixAndDisabledState() {
        val custom = CommandConfig.DEFAULTS.toMutableMap().apply {
            this["tex"] = getValue("tex").copy(prefix = "lx")
        }

        val oldPrefix = SmartCommandEngine.evaluateCommands("tex \\frac{1}{2}", emptyList(), configs = custom)
        assertTrue(oldPrefix.immediateResults.none { it is LauncherSearchResult.RichCard && it.type == SmartCommandType.LATEX })

        val newPrefix = SmartCommandEngine.evaluateCommands("lx \\sqrt{x}", emptyList(), configs = custom)
        val card = newPrefix.immediateResults.single() as LauncherSearchResult.RichCard
        assertEquals("\\sqrt{x}", card.latexSource)

        val disabled = CommandConfig.DEFAULTS.toMutableMap().apply {
            this["tex"] = getValue("tex").copy(enabled = false)
        }
        val disabledEval = SmartCommandEngine.evaluateCommands("tex \\frac{1}{2}", emptyList(), configs = disabled)
        assertTrue(disabledEval.immediateResults.isEmpty())
    }

    @Test
    fun searchEngineSuppressesAppsOnlyWhenTexHasArgument() {
        val engine = SearchEngine()
        engine.updateIndex(
            listOf(AppItem("com.example.tex", "Main", "Tex Editor", null, 0L)),
            emptyMap()
        )

        val bareTex = engine.searchAll("tex").first
        assertTrue(bareTex.any { it is LauncherSearchResult.App && it.app.label == "Tex Editor" })
        assertTrue(bareTex.any { it is LauncherSearchResult.Command && it.type == SmartCommandType.LATEX })

        val withArg = engine.searchAll("tex \\int x\\,dx").first
        assertTrue(withArg.none { it is LauncherSearchResult.App })
        assertTrue(withArg.any { it is LauncherSearchResult.RichCard && it.type == SmartCommandType.LATEX })
    }

    @Test
    fun manualEntriesDynamicallyReflectPrefixesAndEnabledState() {
        val custom = CommandConfig.DEFAULTS.toMutableMap().apply {
            this["math"] = getValue("math").copy(prefix = "calc")
            this["tex"] = getValue("tex").copy(prefix = "lx", enabled = false)
            this["weather"] = getValue("weather").copy(prefix = "wx", enabled = false)
            this["stock"] = getValue("stock").copy(prefix = "mkt")
        }

        val entries = MathStatsEngine.manualEntriesFor(custom)

        val mathFunc = entries.first { it.signature.startsWith("Mean[list]") }
        assertTrue(mathFunc.example.startsWith("calc "))

        val cmdEntries = entries.filter { it.category == "Launcher Commands" }
        val texCmd = cmdEntries.firstOrNull { it.signature.startsWith("lx ") }
        assertNotNull(texCmd)
        assertTrue(texCmd!!.example.startsWith("lx "))
        assertFalse(texCmd.isEnabled)

        val wxCmd = cmdEntries.firstOrNull { it.signature.startsWith("wx ") }
        assertNotNull(wxCmd)
        assertFalse(wxCmd!!.isEnabled)

        val stockCmd = cmdEntries.firstOrNull { it.signature.startsWith("mkt ") }
        assertNotNull(stockCmd)
        assertTrue(stockCmd!!.example.startsWith("mkt "))
        assertTrue(stockCmd.isEnabled)
    }

    @Test
    fun cheatsheetsCoverCalculusAndHaveBalancedLatexWithoutBasicArithmetic() {
        val topics = MathCheatsheets.TOPICS
        assertEquals(
            listOf("calculus", "linear_algebra", "probability_statistics", "latex"),
            topics.map { it.id }
        )

        val leftCmdRegex = Regex("""\\left(?![A-Za-z])""")
        val rightCmdRegex = Regex("""\\right(?![A-Za-z])""")
        val envCmdRegex = Regex("""\\(begin|end)\{([^}]+)\}""")

        topics.forEach { topic ->
            assertTrue("shortTitle blank for ${topic.id}", topic.shortTitle.isNotBlank())
            assertTrue("symbol blank for ${topic.id}", topic.symbol.isNotBlank())
            assertTrue("subsections empty for ${topic.id}", topic.subsections.isNotEmpty())
            assertEquals(
                "Duplicate subsection titles in ${topic.id}",
                topic.subsections.size,
                topic.subsections.map { it.title }.distinct().size
            )
            topic.subsections.forEach { sub ->
                assertEquals(
                    "Duplicate formula titles in ${topic.id}/${sub.title}",
                    sub.formulas.size,
                    sub.formulas.map { it.title }.distinct().size
                )
            }
        }

        val calculus = MathCheatsheets.topicById("calculus")!!
        assertEquals(6, calculus.subsections.size)

        val probTopic = MathCheatsheets.topicById("probability_statistics")!!
        assertFalse(probTopic.title.contains("Fat Tail", ignoreCase = true))

        val latexTopic = MathCheatsheets.topicById("latex")
        assertNotNull(latexTopic)
        assertEquals("LaTeX", latexTopic!!.symbol)
        assertTrue(latexTopic.showLatexSource)
        assertEquals(8, latexTopic.subsections.size)
        assertTrue(
            latexTopic.subsections
                .flatMap { it.formulas }
                .any { it.latex.contains("\\begin{align}") }
        )

        val allFormulas = topics.flatMap { t -> t.subsections.flatMap { it.formulas } }
        assertTrue(allFormulas.size >= 45)

        allFormulas.forEach { formula ->
            assertTrue("Formula title blank", formula.title.isNotBlank())
            assertTrue("Formula latex blank", formula.latex.isNotBlank())
            assertFalse(
                "Basic arithmetic identity found in ${formula.title}",
                formula.latex.contains("(a+b)^2") || formula.latex.contains("a^2+2ab+b^2")
            )

            // Verify balanced unescaped curly braces { and }
            var depth = 0
            var i = 0
            while (i < formula.latex.length) {
                val ch = formula.latex[i]
                if (ch == '\\') {
                    i += 2
                    continue
                }
                if (ch == '{') depth++
                else if (ch == '}') depth--
                assertTrue("Unmatched closing brace in ${formula.title}: ${formula.latex}", depth >= 0)
                i++
            }
            assertEquals("Unclosed brace in ${formula.title}: ${formula.latex}", 0, depth)

            // Verify balanced \left / \right delimiter commands
            val leftCount = leftCmdRegex.findAll(formula.latex).count()
            val rightCount = rightCmdRegex.findAll(formula.latex).count()
            assertEquals(
                "Unbalanced \\left / \\right in ${formula.title}: ${formula.latex}",
                leftCount,
                rightCount
            )

            // Verify balanced \begin{env} / \end{env} pairs
            val envStack = ArrayDeque<String>()
            envCmdRegex.findAll(formula.latex).forEach { match ->
                val kind = match.groupValues[1]
                val envName = match.groupValues[2]
                if (kind == "begin") {
                    envStack.addLast(envName)
                } else {
                    assertTrue("Unexpected \\end{$envName} in ${formula.title}", envStack.isNotEmpty())
                    assertEquals(
                        "Mismatched environment in ${formula.title}",
                        envStack.removeLast(),
                        envName
                    )
                }
            }
            assertTrue("Unclosed environments in ${formula.title}: $envStack", envStack.isEmpty())
        }
    }

    @Test
    fun allCheatsheetFormulasParseWithoutErrorsInLatexParser() {
        val parser = LatexParser()
        MathCheatsheets.TOPICS.forEach { topic ->
            topic.subsections.forEach { sub ->
                sub.formulas.forEach { formula ->
                    val preprocessed = preprocessLatexForRenderer(formula.latex)
                    val result = parser.parseWithDiagnostics(preprocessed)
                    val problems = result.errors + result.warnings
                    assertTrue(
                        "Diagnostics in ${topic.id}/${sub.title}/${formula.title}: $problems",
                        problems.isEmpty()
                    )
                }
            }
        }
    }

    @Test
    fun impliedbyPreprocessesCleanlyWithoutCorruptingOtherMacros() {
        assertEquals("A \\;\\Longleftarrow\\; B", preprocessLatexForRenderer("A \\impliedby B"))
        assertEquals("\\impliedbyfoo", preprocessLatexForRenderer("\\impliedbyfoo"))
        assertEquals("a \\\\impliedby b", preprocessLatexForRenderer("a \\\\impliedby b"))
        assertEquals("\\impliedby", preprocessLatexForRenderer("\\impliedby").let { "\\impliedby" })

        val parser = LatexParser()
        val parsed = parser.parseWithDiagnostics(preprocessLatexForRenderer("P \\impliedby Q"))
        assertTrue((parsed.errors + parsed.warnings).isEmpty())
    }

    @Test
    fun engineManualCategoriesMatchOfficialDocumentationSet() {
        val officialCategories = setOf(
            "Arithmetic & Number Theory",
            "Calculus & Solvers",
            "Lists & Linear Algebra",
            "Statistics",
            "Distributions",
            "Probability & Random",
            "Operators & Control Flow",
            "Plotting & Charts"
        )
        val entries = MathStatsEngine.manualEntriesFor(CommandConfig.DEFAULTS)
            .filterNot { it.category == "Launcher Commands" }

        val actualCategories = entries.map { it.category }.toSet()
        assertEquals(officialCategories, actualCategories)
        officialCategories.forEach { cat ->
            assertTrue("Expected entries in category $cat", entries.any { it.category == cat })
        }
    }
}
