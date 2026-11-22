package com.pallas.launcher.domain.search

enum class AxisScale {
    LINEAR,
    LOG10
}

data class ChartPoint(
    val x: Double,
    val y: Double
)

data class CurveSeries(
    val label: String,
    /**
     * Disjoint continuous polyline segments in plot-space coordinates
     * (where LOG10 axes are stored as log10(v)). Discontinuities, NaNs,
     * non-positive values on log axes, and vertical asymptotes split segments.
     */
    val segments: List<List<ChartPoint>>,
    val showLine: Boolean = true,
    val showPoints: Boolean = false
)

sealed interface MathChartData {
    val title: String
    val summary: String
    val badge: String

    data class Cartesian2D(
        override val title: String,
        override val summary: String,
        override val badge: String = "Plot",
        val series: List<CurveSeries>,
        val xMin: Double,
        val xMax: Double,
        val yMin: Double,
        val yMax: Double,
        val xScale: AxisScale = AxisScale.LINEAR,
        val yScale: AxisScale = AxisScale.LINEAR,
        val isAspectEqual: Boolean = false
    ) : MathChartData

    data class HistogramChart(
        override val title: String,
        override val summary: String,
        override val badge: String = "Histogram",
        val bins: List<HistogramBin>,
        val xMin: Double,
        val xMax: Double,
        val yMax: Double,
        val modeLabel: String,
        val sampleSize: Int,
        val mean: Double,
        val sd: Double
    ) : MathChartData

    data class HistogramBin(
        val xLow: Double,
        val xHigh: Double,
        val height: Double,
        val count: Int
    )

    data class BarChartData(
        override val title: String,
        override val summary: String,
        override val badge: String = "BarChart",
        val groups: List<BarGroup>,
        val seriesLabels: List<String> = emptyList(),
        val yMin: Double,
        val yMax: Double
    ) : MathChartData

    data class BarGroup(
        val label: String,
        val values: List<Double>
    )

    data class PieChartData(
        override val title: String,
        override val summary: String,
        override val badge: String = "PieChart",
        val slices: List<PieSlice>,
        val total: Double
    ) : MathChartData

    data class PieSlice(
        val label: String,
        val value: Double,
        val fraction: Double
    )

    data class BoxWhiskerData(
        override val title: String,
        override val summary: String,
        override val badge: String = "BoxWhisker",
        val boxes: List<BoxWhiskerSummary>,
        val globalMin: Double,
        val globalMax: Double
    ) : MathChartData

    data class BoxWhiskerSummary(
        val label: String,
        val min: Double,
        val q1: Double,
        val median: Double,
        val q3: Double,
        val max: Double,
        val outliers: List<Double>
    )
}
