package com.pallas.launcher

import com.pallas.launcher.data.repository.LatexDocumentRepository
import com.pallas.launcher.domain.latex.InlineSegment
import com.pallas.launcher.domain.latex.LatexDocBlock
import com.pallas.launcher.domain.latex.LatexDocumentProcessor
import com.pallas.launcher.domain.latex.LatexTextAlign
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class LatexStudioTests {

    @Test
    fun defaultPreambleUsesAmsWithoutTikzOrBoilerplateMetadata() {
        val parsed = LatexDocumentProcessor.parse("""\frac{1}{2} + \sqrt{x}""")
        assertTrue(parsed.packages.contains("\\usepackage{amsmath}"))
        assertTrue(parsed.packages.contains("\\usepackage{amssymb}"))
        assertTrue(parsed.packages.none { it.contains("tikz", ignoreCase = true) })
        assertEquals("", parsed.title)
        assertEquals("", parsed.author)
        assertEquals("", parsed.date)
        assertFalse(parsed.hasHeaderMetadata)
        assertTrue(parsed.isPureMath)

        val compilable = LatexDocumentProcessor.buildCompilableTex("""\frac{1}{2} + \sqrt{x}""")
        assertTrue(compilable.contains("\\usepackage{amsmath}"))
        assertTrue(compilable.contains("\\usepackage{amssymb}"))
        assertFalse(compilable.contains("tikz", ignoreCase = true))
        assertTrue(compilable.contains("\\title{}"))
        assertTrue(compilable.contains("\\author{}"))
        assertTrue(compilable.contains("\\date{}"))
        assertFalse(compilable.contains("\\maketitle"))
        assertTrue(compilable.contains("\\["))
        assertTrue(compilable.contains("\\frac{1}{2} + \\sqrt{x}"))
    }

    @Test
    fun partialMetadataAndStructuredDocumentParseCorrectly() {
        val source = """
            \title{My Notes}
            \usepackage{amsmath,geometry}
            \section{Introduction}
            Consider the inline sum ${'$'}\sum_{k=1}^n k${'$'} and bold \textbf{result}.

            ${'$'}${'$'}E = mc^2${'$'}${'$'}

            \begin{itemize}
            \item First point
            \item Second point
            \end{itemize}
        """.trimIndent()

        val parsed = LatexDocumentProcessor.parse(source)
        assertEquals("My Notes", parsed.title)
        assertEquals("", parsed.author)
        assertEquals("", parsed.date)
        assertTrue(parsed.hasHeaderMetadata)
        assertTrue(parsed.packages.contains("\\usepackage{amsmath,geometry}"))

        assertTrue(parsed.blocks.any { it is LatexDocBlock.Section && it.title == "Introduction" })
        assertTrue(parsed.blocks.any { it is LatexDocBlock.DisplayMath && it.latex.contains("E = mc^2") })
        val listBlock = parsed.blocks.firstOrNull { it is LatexDocBlock.BulletList } as? LatexDocBlock.BulletList
        assertEquals(2, listBlock?.items?.size)
        val para = parsed.blocks.firstOrNull { it is LatexDocBlock.Paragraph } as? LatexDocBlock.Paragraph
        assertTrue(para?.segments?.any { it is InlineSegment.InlineMath } == true)

        val compiled = LatexDocumentProcessor.buildCompilableTex(source)
        assertTrue(compiled.contains("\\maketitle"))
        assertEquals(1, Regex("""\\maketitle""").findAll(compiled).count())
    }

    @Test
    fun repositoryPersistsRenamesUpdatesDeletesAndRestores() {
        val dir = Files.createTempDirectory("pallas_latex_test").toFile()
        try {
            val repo = LatexDocumentRepository(dir)
            val created = repo.createDocument("""\frac{a}{b}""")
            assertEquals(1, repo.documents.value.size)

            repo.updateContent(created.id, """\section{Wave}\n$$ u_{tt} = c^2 u_{xx} $$""")
            repo.renameDocument(created.id, "Wave PDE")

            // Reload from disk via a fresh repository instance
            val reloaded = LatexDocumentRepository(dir)
            assertEquals(1, reloaded.documents.value.size)
            assertEquals("Wave PDE", reloaded.documents.value.first().displayTitle)
            assertTrue(reloaded.documents.value.first().content.contains("Wave"))

            val snapshot = reloaded.documents.value.first()
            reloaded.deleteDocument(created.id)
            assertTrue(reloaded.documents.value.isEmpty())

            reloaded.restoreDocument(snapshot)
            assertEquals(1, reloaded.documents.value.size)
            assertEquals("Wave PDE", reloaded.documents.value.first().displayTitle)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun preamblePageConfigHonorsPaperSizeFontSizeGeometryParskipAndPagestyle() {
        val doc = """
            \documentclass[12pt,letterpaper,landscape]{article}
            \usepackage[margin=1in,left=1.5in]{geometry}
            \usepackage{parskip}
            \pagestyle{empty}
            \begin{document}
            Hello world.
            \end{document}
        """.trimIndent()
        val parsed = LatexDocumentProcessor.parse(doc)
        val cfg = parsed.pageConfig
        assertEquals(792, cfg.pageWidthPt)
        assertEquals(612, cfg.pageHeightPt)
        assertEquals(12f, cfg.baseFontSizePt, 0.01f)
        assertEquals(108f, cfg.marginLeftPt, 0.01f)
        assertEquals(72f, cfg.marginRightPt, 0.01f)
        assertEquals(72f, cfg.marginTopPt, 0.01f)
        assertEquals(72f, cfg.marginBottomPt, 0.01f)
        assertTrue(cfg.useParskip)
        assertFalse(cfg.showPageNumbers)
    }

    @Test
    fun overridesAffectOnlyOverriddenFieldsAndTodayExpandsOnlyWhenSpecified() {
        val titleOnly = LatexDocumentProcessor.parse(
            """
            \title{new}
            \frac{1}{2} + \sqrt{x}
            """.trimIndent()
        )
        assertEquals("new", titleOnly.title)
        assertEquals("", titleOnly.author)
        assertEquals("", titleOnly.date)
        assertEquals("", titleOnly.displayDate)
        assertTrue(titleOnly.isPureMath)
        val pureBlock = titleOnly.blocks.single() as LatexDocBlock.DisplayMath
        assertTrue(pureBlock.latex.contains("\\frac{1}{2} + \\sqrt{x}"))

        val compilable = LatexDocumentProcessor.buildCompilableTex(
            """
            \title{new}
            \frac{1}{2} + \sqrt{x}
            """.trimIndent()
        )
        assertTrue(compilable.contains("\\title{new}"))
        assertTrue(compilable.contains("\\author{}"))
        assertTrue(compilable.contains("\\date{}"))
        assertTrue(compilable.contains("\\maketitle"))
        assertTrue(compilable.contains("\\["))
        assertTrue(compilable.contains("\\frac{1}{2} + \\sqrt{x}"))

        val todayDoc = LatexDocumentProcessor.parse(
            """
            \title{On Zeta}
            \date{\today}
            \begin{document}\maketitle Body.\end{document}
            """.trimIndent()
        )
        assertEquals("\\today", todayDoc.date)
        assertTrue(todayDoc.displayDate.isNotBlank())
        assertFalse(todayDoc.displayDate.contains("\\today"))
        val compiledToday = LatexDocumentProcessor.buildCompilableTex(
            """
            \title{On Zeta}
            \date{\today}
            \begin{document}\maketitle Body.\end{document}
            """.trimIndent()
        )
        assertTrue(compiledToday.contains("\\date{\\today}"))
    }

    @Test
    fun standaloneBareMathAfterHeaderOrSectionRendersInMathMode() {
        val docWithSection = LatexDocumentProcessor.parse(
            """
            \title{Notes}
            \section{Formula}
            \frac{a}{b}
            """.trimIndent()
        )
        val dm = docWithSection.blocks.filterIsInstance<LatexDocBlock.DisplayMath>().single()
        assertEquals("\\frac{a}{b}", dm.latex)

        val singleEnv = LatexDocumentProcessor.parse(
            """
            \title{Eq}
            \begin{equation}
            \label{e1}
            E = mc^2
            \end{equation}
            """.trimIndent()
        )
        assertTrue(singleEnv.isPureMath)
        val eqBlock = singleEnv.blocks.single() as LatexDocBlock.DisplayMath
        assertEquals("E = mc^2", eqBlock.latex)
        assertTrue(eqBlock.numbered)
        assertFalse(eqBlock.latex.contains("\\begin{equation}"))
        assertFalse(eqBlock.latex.contains("\\label"))
    }

    @Test
    fun nestedFormattingSizesSectionsEnvironmentsTabularAndMacrosParseCleanly() {
        val source = """
            \newcommand{\R}{\mathbb{R}}
            \newcommand{\norm}[1]{\left\| #1 \right\|}
            \begin{document}
            \section*{Preface}
            \noindent Let \textbf{\textit{a} and ${'$'}x \in \R${'$'}} with {\Large big} and {\Huge huge} text and ${'$'}\norm{v}${'$'}.

            \begin{center}
            Centered line
            \end{center}

            \begin{quote}
            Quoted thought.
            \end{quote}

            \begin{abstract}
            Short summary.
            \end{abstract}

            \begin{theorem}[Pythagoras]
            ${'$'}a^2 + b^2 = c^2${'$'}.
            \end{theorem}

            \begin{proof}
            Trivial.
            \end{proof}

            \begin{equation}
            \label{eq:py}
            E = mc^2
            \end{equation}

            \begin{tabular}{|l|r|}
            \hline
            Item & Value \\
            \hline
            Pi & ${'$'}3.14${'$'} \\
            \hline
            \end{tabular}

            \begin{verbatim}
            val x = 42
            \end{verbatim}

            \hrule
            \vspace{12pt}
            \newpage
            Next page.
            \end{document}
        """.trimIndent()

        val parsed = LatexDocumentProcessor.parse(source)
        val preface = parsed.blocks.filterIsInstance<LatexDocBlock.Section>().first()
        assertTrue(preface.starred)
        assertEquals("Preface", preface.title)

        val firstPara = parsed.blocks.filterIsInstance<LatexDocBlock.Paragraph>().first()
        assertTrue(firstPara.noIndent)
        assertTrue(
            firstPara.segments.filterIsInstance<InlineSegment.TextRun>().any { it.bold && it.italic && it.text.contains("a") }
        )
        assertTrue(
            firstPara.segments.filterIsInstance<InlineSegment.TextRun>().any { it.sizeScale > 2.0f && it.text.contains("huge") }
        )
        assertTrue(
            firstPara.segments.filterIsInstance<InlineSegment.InlineMath>().any { it.latex.contains("\\mathbb{R}") }
        )
        assertTrue(
            firstPara.segments.filterIsInstance<InlineSegment.InlineMath>().any { it.latex.contains("\\left\\| v \\right\\|") }
        )

        val centered = parsed.blocks.filterIsInstance<LatexDocBlock.Paragraph>().first { it.align == LatexTextAlign.CENTER }
        assertNotNull(centered)

        assertTrue(parsed.blocks.any { it is LatexDocBlock.Quote })
        assertTrue(parsed.blocks.any { it is LatexDocBlock.Abstract })
        val thm = parsed.blocks.filterIsInstance<LatexDocBlock.TheoremBox>().first { !it.isProof }
        assertEquals("Theorem", thm.kind)
        assertEquals("Pythagoras", thm.title)
        assertTrue(thm.italicBody)
        assertTrue(parsed.blocks.filterIsInstance<LatexDocBlock.TheoremBox>().any { it.isProof })

        val dm = parsed.blocks.filterIsInstance<LatexDocBlock.DisplayMath>().first()
        assertTrue(dm.numbered)
        assertFalse(dm.latex.contains("\\label"))
        assertTrue(dm.latex.contains("E = mc^2"))

        val tab = parsed.blocks.filterIsInstance<LatexDocBlock.Tabular>().first()
        assertEquals(listOf(LatexTextAlign.LEFT, LatexTextAlign.RIGHT), tab.colAligns)
        assertEquals(2, tab.rows.size)
        assertTrue(tab.hlinesBeforeRow.containsAll(setOf(0, 1, 2)))

        assertTrue(parsed.blocks.any { it is LatexDocBlock.Verbatim && it.text.contains("val x = 42") })
        assertTrue(parsed.blocks.any { it === LatexDocBlock.HorizontalRule })
        assertTrue(parsed.blocks.any { it is LatexDocBlock.VerticalSpace && it.pt == 12f })
        assertTrue(parsed.blocks.any { it === LatexDocBlock.PageBreak })
    }
}
