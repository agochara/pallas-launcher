package com.pallas.launcher

import com.pallas.launcher.domain.search.AsyncCommandRequest
import com.pallas.launcher.domain.search.AxisScale
import com.pallas.launcher.domain.search.LauncherSearchResult
import com.pallas.launcher.domain.search.MathChartData
import com.pallas.launcher.domain.search.MathStatsEngine
import com.pallas.launcher.domain.search.SmartCommandEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PlotAndChartTests {

    @Test
    fun `Plot evaluates single and multi-series functions and splits poles`() {
        val single = MathStatsEngine.evaluate("= Plot[Sin[x], {x, -Pi, Pi}]", "=")
        assertNotNull(single)
        val c1 = single!!.chart as? MathChartData.Cartesian2D
        assertNotNull(c1)
        assertEquals(1, c1!!.series.size)
        assertEquals(AxisScale.LINEAR, c1.xScale)
        assertEquals(AxisScale.LINEAR, c1.yScale)
        assertTrue(c1.series[0].segments.flatten().size > 50)

        val multi = MathStatsEngine.evaluate("= Plot[{Sin[x], Cos[x]}, {x, 0, 2 Pi}]", "=")
        assertNotNull(multi)
        val c2 = multi!!.chart as? MathChartData.Cartesian2D
        assertNotNull(c2)
        assertEquals(2, c2!!.series.size)

        // Pole splitting on 1/x across [-2, 2]
        val pole = MathStatsEngine.evaluate("= Plot[1/x, {x, -2, 2}]", "=")
        assertNotNull(pole)
        val c3 = pole!!.chart as? MathChartData.Cartesian2D
        assertNotNull(c3)
        assertTrue("1/x across 0 should split into at least 2 segments", c3!!.series[0].segments.size >= 2)
    }

    @Test
    fun `Plot directly plots probability distributions`() {
        val distPlot = MathStatsEngine.evaluate("= Plot[NormalDistribution[0, 1]]", "=")
        assertNotNull(distPlot)
        val chart = distPlot!!.chart as? MathChartData.Cartesian2D
        assertNotNull(chart)
        assertEquals(1, chart!!.series.size)
        assertTrue(chart.yMax > 0.35 && chart.yMax < 0.45)
    }

    @Test
    fun `LogPlot LogLinearPlot and LogLogPlot use log10 axis scales`() {
        val logPlot = MathStatsEngine.evaluate("= LogPlot[Exp[x], {x, 0, 5}]", "=")
        assertNotNull(logPlot)
        val c1 = logPlot!!.chart as MathChartData.Cartesian2D
        assertEquals(AxisScale.LINEAR, c1.xScale)
        assertEquals(AxisScale.LOG10, c1.yScale)

        val logLin = MathStatsEngine.evaluate("= LogLinearPlot[Log10[x], {x, 1, 1000}]", "=")
        assertNotNull(logLin)
        val c2 = logLin!!.chart as MathChartData.Cartesian2D
        assertEquals(AxisScale.LOG10, c2.xScale)
        assertEquals(AxisScale.LINEAR, c2.yScale)
        assertEquals(0.0, c2.xMin, 1e-6)
        assertEquals(3.0, c2.xMax, 1e-6)

        val logLog = MathStatsEngine.evaluate("= LogLogPlot[x^2, {x, 1, 100}]", "=")
        assertNotNull(logLog)
        val c3 = logLog!!.chart as MathChartData.Cartesian2D
        assertEquals(AxisScale.LOG10, c3.xScale)
        assertEquals(AxisScale.LOG10, c3.yScale)
    }

    @Test
    fun `ParametricPlot and PolarPlot produce equal-aspect 2D curves`() {
        val param = MathStatsEngine.evaluate("= ParametricPlot[{Cos[t], Sin[t]}, {t, 0, 2 Pi}]", "=")
        assertNotNull(param)
        val c1 = param!!.chart as MathChartData.Cartesian2D
        assertTrue(c1.isAspectEqual)
        assertEquals(1, c1.series.size)

        val polar = MathStatsEngine.evaluate("= PolarPlot[1 + Cos[t], {t, 0, 2 Pi}]", "=")
        assertNotNull(polar)
        val c2 = polar!!.chart as MathChartData.Cartesian2D
        assertTrue(c2.isAspectEqual)
        assertEquals(1, c2.series.size)
    }

    @Test
    fun `ListPlot ListLinePlot and log list variants support 1D 2D and multi-series`() {
        val lp = MathStatsEngine.evaluate("= ListPlot[{2, 4, 6, 8}]", "=")
        assertNotNull(lp)
        val c1 = lp!!.chart as MathChartData.Cartesian2D
        assertEquals(1, c1.series.size)
        assertTrue(c1.series[0].showPoints && !c1.series[0].showLine)
        assertEquals(4, c1.series[0].segments.flatten().size)

        val llp = MathStatsEngine.evaluate("= ListLinePlot[{{1, 10}, {2, 20}, {3, 15}}]", "=")
        assertNotNull(llp)
        val c2 = llp!!.chart as MathChartData.Cartesian2D
        assertTrue(c2.series[0].showLine)
        assertEquals(3, c2.series[0].segments.flatten().size)

        val multi = MathStatsEngine.evaluate("= ListLinePlot[{{1, 2, 3}, {3, 2, 1}}]", "=")
        assertNotNull(multi)
        val c3 = multi!!.chart as MathChartData.Cartesian2D
        assertEquals(2, c3.series.size)

        val listLog = MathStatsEngine.evaluate("= ListLogPlot[{1, 10, 100, 1000}]", "=")
        assertNotNull(listLog)
        val c4 = listLog!!.chart as MathChartData.Cartesian2D
        assertEquals(AxisScale.LOG10, c4.yScale)

        val listLogLinear = MathStatsEngine.evaluate("= ListLogLinearPlot[{{1, 2}, {10, 4}, {100, 6}}]", "=")
        assertNotNull(listLogLinear)
        val c5 = listLogLinear!!.chart as MathChartData.Cartesian2D
        assertEquals(AxisScale.LOG10, c5.xScale)
        assertEquals(AxisScale.LINEAR, c5.yScale)

        val listLogLog = MathStatsEngine.evaluate("= ListLogLogPlot[{{1, 1}, {10, 100}, {100, 10000}}]", "=")
        assertNotNull(listLogLog)
        val c6 = listLogLog!!.chart as MathChartData.Cartesian2D
        assertEquals(AxisScale.LOG10, c6.xScale)
        assertEquals(AxisScale.LOG10, c6.yScale)
    }

    @Test
    fun `Histogram bins numeric lists and distributions with Count Probability or PDF modes`() {
        val h1 = MathStatsEngine.evaluate("= Histogram[Range[100], 10]", "=")
        assertNotNull(h1)
        val c1 = h1!!.chart as MathChartData.HistogramChart
        assertEquals(10, c1.bins.size)
        assertEquals(100, c1.sampleSize)

        val hProb = MathStatsEngine.evaluate("= Histogram[Range[50], 5, \"Probability\"]", "=")
        assertNotNull(hProb)
        val c2 = hProb!!.chart as MathChartData.HistogramChart
        val probSum = c2.bins.sumOf { it.height }
        assertTrue(abs(probSum - 1.0) < 1e-6)

        val hDist = MathStatsEngine.evaluate("= Histogram[NormalDistribution[0, 1]]", "=")
        assertNotNull(hDist)
        val c3 = hDist!!.chart as MathChartData.HistogramChart
        assertTrue(c3.bins.isNotEmpty())
    }

    @Test
    fun `BarChart PieChart and BoxWhiskerChart evaluate labeled and numeric datasets`() {
        val bar = MathStatsEngine.evaluate("= BarChart[{Q1 -> 20, Q2 -> 35, Q3 -> 50}]", "=")
        assertNotNull(bar)
        val bc = bar!!.chart as MathChartData.BarChartData
        assertEquals(3, bc.groups.size)
        assertEquals("Q1", bc.groups[0].label)
        assertEquals(20.0, bc.groups[0].values[0], 1e-6)

        val pie = MathStatsEngine.evaluate("= PieChart[{A -> 50, B -> 30, C -> 20}]", "=")
        assertNotNull(pie)
        val pc = pie!!.chart as MathChartData.PieChartData
        assertEquals(3, pc.slices.size)
        assertEquals(0.5, pc.slices[0].fraction, 1e-6)
        assertEquals(1.0, pc.slices.sumOf { it.fraction }, 1e-6)

        val box = MathStatsEngine.evaluate("= BoxWhiskerChart[{1, 2, 3, 4, 5, 6, 7, 8, 9, 25}]", "=")
        assertNotNull(box)
        val bwc = box!!.chart as MathChartData.BoxWhiskerData
        assertEquals(1, bwc.boxes.size)
        assertTrue(bwc.boxes[0].outliers.contains(25.0))
    }

    @Test
    fun `SmartCommandEngine gates plot evaluation on balanced brackets and routes via AsyncCommandRequest MathPlot`() {
        val unclosed = SmartCommandEngine.evaluateCommands("= Plot[Sin[x], {x, 0, Pi}", emptyList())
        assertNull("Unclosed plot brackets must not schedule async evaluation", unclosed.asyncRequest)
        val card1 = unclosed.immediateResults.firstOrNull() as? LauncherSearchResult.RichCard
        assertNotNull(card1)
        assertTrue(card1!!.subtitle.contains("Complete brackets"))

        val closed = SmartCommandEngine.evaluateCommands("= Plot[Sin[x], {x, 0, Pi}]", emptyList())
        assertTrue("Closed plot expression must route through debounced AsyncCommandRequest.MathPlot", closed.asyncRequest is AsyncCommandRequest.MathPlot)
        val card2 = closed.immediateResults.firstOrNull() as? LauncherSearchResult.RichCard
        assertNotNull(card2)
        assertTrue(card2!!.isLoading)
    }
}
