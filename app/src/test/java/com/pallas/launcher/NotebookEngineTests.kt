package com.pallas.launcher

import com.pallas.launcher.data.repository.NotebookRepository
import com.pallas.launcher.domain.notebook.NotebookCell
import com.pallas.launcher.domain.notebook.NotebookKernel
import com.pallas.launcher.domain.search.MathStatsEngine
import com.pallas.launcher.domain.search.MathValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class NotebookEngineTests {

    @Test
    fun kernelMaintainsSharedStatePlotsAndTeXFormAcrossCells() {
        val kernel = NotebookKernel()

        val out1 = kernel.evaluateCell("x = {1, 2, 3, 4, 5}")
        assertFalse(out1.isError)
        assertTrue(kernel.definedSymbols().contains("x"))

        val out2 = kernel.evaluateCell("Mean[x]")
        assertFalse(out2.isError)
        assertEquals("3", out2.copyText)

        val out3 = kernel.evaluateCell("y = x^2; Total[y]")
        assertFalse(out3.isError)
        assertEquals("55", out3.copyText)
        assertTrue(kernel.definedSymbols().containsAll(listOf("x", "y")))

        val plotOut = kernel.evaluateCell("ListLinePlot[y]")
        assertFalse(plotOut.isError)
        assertNotNull("Expected chart from prior cell variable y", plotOut.chart)

        val texFormOut = kernel.evaluateCell("TeXForm[{{1, 2}, {3, 4}}]")
        assertFalse(texFormOut.isError)
        assertNotNull(texFormOut.latexSource)
        assertTrue(texFormOut.latexSource!!.contains("\\begin{pmatrix}"))

        val texCmdOut = kernel.evaluateCell("""tex \sum_{k=1}^{n} k""")
        assertFalse(texCmdOut.isError)
        assertEquals("""\sum_{k=1}^{n} k""", texCmdOut.latexSource)

        kernel.reset()
        assertTrue(kernel.definedSymbols().isEmpty())
    }

    @Test
    fun notebookKernelIsIsolatedFromGlobalSearchLastEvaluatedValue() {
        val prev = MathStatsEngine.lastEvaluatedValue
        try {
            MathStatsEngine.lastEvaluatedValue = MathValue.NumberVal(999.0)
            val kernel = NotebookKernel()
            val out = kernel.evaluateCell("%")
            assertEquals("0", out.copyText)
        } finally {
            MathStatsEngine.lastEvaluatedValue = prev
        }
    }

    @Test
    fun notebookRepositoryPersistsCellsChartsAndLatexRoundTrip() {
        val dir = Files.createTempDirectory("pallas_notebook_test").toFile()
        try {
            val kernel = NotebookKernel()
            val c1 = NotebookCell(source = "xs = Range[5]", executionOrder = 1, output = kernel.evaluateCell("xs = Range[5]"))
            val c2 = NotebookCell(source = "ListLinePlot[xs^2]", executionOrder = 2, output = kernel.evaluateCell("ListLinePlot[xs^2]"))
            val c3 = NotebookCell(source = "TeXForm[{{1, 2}, {3, 4}}]", executionOrder = 3, output = kernel.evaluateCell("TeXForm[{{1, 2}, {3, 4}}]"))

            val repo = NotebookRepository(dir)
            val created = repo.createNotebook(initialCells = listOf(c1, c2, c3))
            repo.renameNotebook(created.id, "Quadratic Plot")

            val reloaded = NotebookRepository(dir)
            assertEquals(1, reloaded.notebooks.value.size)
            val loaded = reloaded.notebooks.value.first()
            assertEquals("Quadratic Plot", loaded.displayTitle)
            assertEquals(3, loaded.cells.size)
            assertNotNull("Chart output should survive JSON round-trip", loaded.cells[1].output?.chart)
            assertTrue(loaded.cells[2].output?.latexSource?.contains("\\begin{pmatrix}") == true)

            reloaded.deleteNotebook(loaded.id)
            assertNull(reloaded.notebooks.value.firstOrNull())

            reloaded.restoreNotebook(loaded)
            assertEquals(1, reloaded.notebooks.value.size)
        } finally {
            dir.deleteRecursively()
        }
    }
}
