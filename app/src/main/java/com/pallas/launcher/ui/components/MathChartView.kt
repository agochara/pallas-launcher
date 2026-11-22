package com.pallas.launcher.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pallas.launcher.domain.search.AxisScale
import com.pallas.launcher.domain.search.MathChartData
import com.pallas.launcher.domain.search.MathStatsEngine
import com.pallas.launcher.platform.TexExportHelper
import com.pallas.launcher.ui.theme.LocalLauncherColors
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

private val CHART_PALETTE_DARK = listOf(
    Color(0xFFE6B450), // Warm Wolfram gold
    Color(0xFF5FB3A1), // Sage teal
    Color(0xFF6CA4E8), // Sky blue
    Color(0xFFE2786F), // Coral red
    Color(0xFFB388EB), // Soft violet
    Color(0xFF8CC084)  // Mint green
)

private val CHART_PALETTE_LIGHT = listOf(
    Color(0xFFC47D16), // Deep amber gold
    Color(0xFF287D6B), // Deep teal
    Color(0xFF2F69B3), // Royal blue
    Color(0xFFC2463D), // Brick coral
    Color(0xFF794BC4), // Deep purple
    Color(0xFF47893F)  // Forest green
)

enum class ChartExportTheme(val label: String) {
    DARK("Dark"),
    LIGHT("Light")
}

@Composable
fun MathChartCanvas(
    chart: MathChartData,
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    scrubFraction: Float? = null
) {
    val palette = if (isDark) CHART_PALETTE_DARK else CHART_PALETTE_LIGHT
    val axisColor = if (isDark) Color(0xFFD0D4DC).copy(alpha = 0.55f) else Color(0xFF2A2E37).copy(alpha = 0.65f)
    val gridColor = if (isDark) Color(0xFFD0D4DC).copy(alpha = 0.13f) else Color(0xFF2A2E37).copy(alpha = 0.13f)
    val labelArgb = if (isDark) 0xFFB8BEC9.toInt() else 0xFF3C414E.toInt()
    val strongLabelArgb = if (isDark) 0xFFF0F2F7.toInt() else 0xFF14171F.toInt()

    Canvas(modifier = modifier) {
        when (chart) {
            is MathChartData.Cartesian2D -> drawCartesian2D(
                chart = chart,
                palette = palette,
                axisColor = axisColor,
                gridColor = gridColor,
                labelArgb = labelArgb,
                strongLabelArgb = strongLabelArgb,
                scrubFraction = scrubFraction
            )
            is MathChartData.HistogramChart -> drawHistogram(
                chart = chart,
                palette = palette,
                axisColor = axisColor,
                gridColor = gridColor,
                labelArgb = labelArgb,
                strongLabelArgb = strongLabelArgb,
                scrubFraction = scrubFraction
            )
            is MathChartData.BarChartData -> drawBarChart(
                chart = chart,
                palette = palette,
                axisColor = axisColor,
                gridColor = gridColor,
                labelArgb = labelArgb,
                strongLabelArgb = strongLabelArgb
            )
            is MathChartData.PieChartData -> drawPieChart(
                chart = chart,
                palette = palette,
                labelArgb = labelArgb,
                strongLabelArgb = strongLabelArgb,
                isDark = isDark
            )
            is MathChartData.BoxWhiskerData -> drawBoxWhisker(
                chart = chart,
                palette = palette,
                axisColor = axisColor,
                gridColor = gridColor,
                labelArgb = labelArgb,
                strongLabelArgb = strongLabelArgb
            )
        }
    }
}

private fun formatAxisTick(value: Double, scale: AxisScale): String {
    val actual = if (scale == AxisScale.LOG10) 10.0.pow(value) else value
    val a = abs(actual)
    return when {
        a == 0.0 -> "0"
        a >= 1e5 || (a > 0 && a < 1e-3) -> String.format(Locale.US, "%.1e", actual)
        abs(actual - actual.roundToInt()) < 1e-6 -> actual.roundToInt().toString()
        a >= 100 -> String.format(Locale.US, "%.1f", actual).trimEnd('0').trimEnd('.')
        a >= 1 -> String.format(Locale.US, "%.2f", actual).trimEnd('0').trimEnd('.')
        else -> String.format(Locale.US, "%.3f", actual).trimEnd('0').trimEnd('.')
    }
}

private fun DrawScope.drawCartesian2D(
    chart: MathChartData.Cartesian2D,
    palette: List<Color>,
    axisColor: Color,
    gridColor: Color,
    labelArgb: Int,
    strongLabelArgb: Int,
    scrubFraction: Float?
) {
    val hasLegend = chart.series.size > 1
    val leftPad = 44.dp.toPx()
    val rightPad = 14.dp.toPx()
    val topPad = if (hasLegend) 24.dp.toPx() else 12.dp.toPx()
    val bottomPad = 24.dp.toPx()

    var plotLeft = leftPad
    var plotTop = topPad
    var plotW = (size.width - leftPad - rightPad).coerceAtLeast(10f)
    var plotH = (size.height - topPad - bottomPad).coerceAtLeast(10f)

    val xSpan = (chart.xMax - chart.xMin).takeIf { it > 1e-12 } ?: 1.0
    val ySpan = (chart.yMax - chart.yMin).takeIf { it > 1e-12 } ?: 1.0

    if (chart.isAspectEqual) {
        val scale = min(plotW / xSpan.toFloat(), plotH / ySpan.toFloat())
        val actualW = (xSpan.toFloat() * scale).coerceAtLeast(10f)
        val actualH = (ySpan.toFloat() * scale).coerceAtLeast(10f)
        plotLeft += (plotW - actualW) * 0.5f
        plotTop += (plotH - actualH) * 0.5f
        plotW = actualW
        plotH = actualH
    }

    val plotRight = plotLeft + plotW
    val plotBottom = plotTop + plotH

    fun mapX(x: Double): Float =
        (plotLeft + ((x - chart.xMin) / xSpan).toFloat() * plotW).coerceIn(plotLeft, plotRight)

    fun mapY(y: Double): Float =
        (plotBottom - ((y - chart.yMin) / ySpan).toFloat() * plotH).coerceIn(plotTop, plotBottom)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = labelArgb
        textSize = 9.5.sp.toPx()
        typeface = Typeface.MONOSPACE
    }

    // Horizontal gridlines & Y ticks (4 intervals)
    val ySteps = 4
    textPaint.textAlign = Paint.Align.RIGHT
    for (i in 0..ySteps) {
        val frac = i.toDouble() / ySteps
        val yVal = chart.yMin + frac * ySpan
        val py = (plotBottom - frac.toFloat() * plotH)
        drawLine(
            color = gridColor,
            start = Offset(plotLeft, py),
            end = Offset(plotRight, py),
            strokeWidth = 1.dp.toPx()
        )
        val tickStr = formatAxisTick(yVal, chart.yScale)
        drawContext.canvas.nativeCanvas.drawText(
            tickStr,
            plotLeft - 5.dp.toPx(),
            py + 3.dp.toPx(),
            textPaint
        )
    }

    // Vertical gridlines & X ticks (4 intervals)
    val xSteps = 4
    for (i in 0..xSteps) {
        val frac = i.toDouble() / xSteps
        val xVal = chart.xMin + frac * xSpan
        val px = (plotLeft + frac.toFloat() * plotW)
        drawLine(
            color = gridColor,
            start = Offset(px, plotTop),
            end = Offset(px, plotBottom),
            strokeWidth = 1.dp.toPx()
        )
        textPaint.textAlign = when (i) {
            0 -> Paint.Align.LEFT
            xSteps -> Paint.Align.RIGHT
            else -> Paint.Align.CENTER
        }
        val tickStr = formatAxisTick(xVal, chart.xScale)
        drawContext.canvas.nativeCanvas.drawText(
            tickStr,
            px,
            plotBottom + 14.dp.toPx(),
            textPaint
        )
    }

    // Zero axes if inside domain
    if (chart.xScale == AxisScale.LINEAR && chart.xMin <= 0.0 && chart.xMax >= 0.0) {
        val x0 = mapX(0.0)
        drawLine(
            color = axisColor,
            start = Offset(x0, plotTop),
            end = Offset(x0, plotBottom),
            strokeWidth = 1.2.dp.toPx()
        )
    }
    if (chart.yScale == AxisScale.LINEAR && chart.yMin <= 0.0 && chart.yMax >= 0.0) {
        val y0 = mapY(0.0)
        drawLine(
            color = axisColor,
            start = Offset(plotLeft, y0),
            end = Offset(plotRight, y0),
            strokeWidth = 1.2.dp.toPx()
        )
    }

    // Outer frame border
    drawRect(
        color = axisColor.copy(alpha = 0.35f),
        topLeft = Offset(plotLeft, plotTop),
        size = Size(plotW, plotH),
        style = Stroke(width = 1.dp.toPx())
    )

    // Series curves and points
    chart.series.forEachIndexed { idx, s ->
        val color = palette[idx % palette.size]
        if (s.showLine) {
            for (seg in s.segments) {
                if (seg.size >= 2) {
                    val path = Path()
                    seg.forEachIndexed { ptIdx, pt ->
                        val px = mapX(pt.x)
                        val py = mapY(pt.y)
                        if (ptIdx == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(
                            width = 2.2.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
        if (s.showPoints) {
            val r = if (s.showLine) 2.6.dp.toPx() else 3.2.dp.toPx()
            for (seg in s.segments) {
                for (pt in seg) {
                    val px = mapX(pt.x)
                    val py = mapY(pt.y)
                    drawCircle(
                        color = color,
                        radius = r,
                        center = Offset(px, py)
                    )
                }
            }
        }
    }

    // Legend row at top if multiple series
    if (hasLegend) {
        val legendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = strongLabelArgb
            textSize = 10.sp.toPx()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        var curX = plotLeft
        val legY = 12.dp.toPx()
        chart.series.take(4).forEachIndexed { idx, s ->
            val color = palette[idx % palette.size]
            drawCircle(
                color = color,
                radius = 3.5.dp.toPx(),
                center = Offset(curX + 4.dp.toPx(), legY - 3.dp.toPx())
            )
            curX += 11.dp.toPx()
            val lbl = if (s.label.length > 16) s.label.take(15) + "…" else s.label
            drawContext.canvas.nativeCanvas.drawText(lbl, curX, legY, legendPaint)
            curX += legendPaint.measureText(lbl) + 12.dp.toPx()
        }
    }

    // Interactive scrub crosshair & value readout
    if (scrubFraction != null) {
        val f = scrubFraction.coerceIn(0f, 1f)
        val scrubXVal = chart.xMin + f * xSpan
        val scrubPx = plotLeft + f * plotW
        drawLine(
            color = axisColor,
            start = Offset(scrubPx, plotTop),
            end = Offset(scrubPx, plotBottom),
            strokeWidth = 1.2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
        )

        val readouts = mutableListOf<String>()
        val xActual = if (chart.xScale == AxisScale.LOG10) 10.0.pow(scrubXVal) else scrubXVal
        readouts.add("x=${MathStatsEngine.fmt(xActual)}")

        chart.series.forEachIndexed { idx, s ->
            val allPts = s.segments.flatten()
            val nearest = allPts.minByOrNull { abs(it.x - scrubXVal) }
            if (nearest != null) {
                val color = palette[idx % palette.size]
                val py = mapY(nearest.y)
                val px = mapX(nearest.x)
                drawCircle(color = color, radius = 4.5.dp.toPx(), center = Offset(px, py))
                val yActual = if (chart.yScale == AxisScale.LOG10) 10.0.pow(nearest.y) else nearest.y
                readouts.add("${s.label.take(10)}=${MathStatsEngine.fmt(yActual)}")
            }
        }

        val bannerText = readouts.joinToString("  •  ")
        val bannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = strongLabelArgb
            textSize = 10.sp.toPx()
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.RIGHT
        }
        drawContext.canvas.nativeCanvas.drawText(
            bannerText,
            plotRight,
            plotTop + 12.dp.toPx(),
            bannerPaint
        )
    }
}

private fun DrawScope.drawHistogram(
    chart: MathChartData.HistogramChart,
    palette: List<Color>,
    axisColor: Color,
    gridColor: Color,
    labelArgb: Int,
    strongLabelArgb: Int,
    scrubFraction: Float?
) {
    val leftPad = 42.dp.toPx()
    val rightPad = 14.dp.toPx()
    val topPad = 14.dp.toPx()
    val bottomPad = 24.dp.toPx()

    val plotLeft = leftPad
    val plotTop = topPad
    val plotW = (size.width - leftPad - rightPad).coerceAtLeast(10f)
    val plotH = (size.height - topPad - bottomPad).coerceAtLeast(10f)
    val plotRight = plotLeft + plotW
    val plotBottom = plotTop + plotH

    val xSpan = (chart.xMax - chart.xMin).takeIf { it > 1e-12 } ?: 1.0
    val yMax = (chart.yMax * 1.08).takeIf { it > 1e-12 } ?: 1.0

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = labelArgb
        textSize = 9.5.sp.toPx()
        typeface = Typeface.MONOSPACE
    }

    // Y gridlines
    val ySteps = 4
    textPaint.textAlign = Paint.Align.RIGHT
    for (i in 0..ySteps) {
        val frac = i.toDouble() / ySteps
        val yVal = frac * yMax
        val py = plotBottom - frac.toFloat() * plotH
        drawLine(
            color = gridColor,
            start = Offset(plotLeft, py),
            end = Offset(plotRight, py),
            strokeWidth = 1.dp.toPx()
        )
        drawContext.canvas.nativeCanvas.drawText(
            formatAxisTick(yVal, AxisScale.LINEAR),
            plotLeft - 5.dp.toPx(),
            py + 3.dp.toPx(),
            textPaint
        )
    }

    // X ticks
    val xSteps = 4
    for (i in 0..xSteps) {
        val frac = i.toDouble() / xSteps
        val xVal = chart.xMin + frac * xSpan
        val px = plotLeft + frac.toFloat() * plotW
        textPaint.textAlign = when (i) {
            0 -> Paint.Align.LEFT
            xSteps -> Paint.Align.RIGHT
            else -> Paint.Align.CENTER
        }
        drawContext.canvas.nativeCanvas.drawText(
            formatAxisTick(xVal, AxisScale.LINEAR),
            px,
            plotBottom + 14.dp.toPx(),
            textPaint
        )
    }

    val barColor = palette[1]
    val highlightColor = palette[0]
    val activeIdx = if (scrubFraction != null && chart.bins.isNotEmpty()) {
        (scrubFraction.coerceIn(0f, 0.9999f) * chart.bins.size).toInt()
    } else -1

    chart.bins.forEachIndexed { idx, bin ->
        val x0 = plotLeft + (((bin.xLow - chart.xMin) / xSpan).toFloat() * plotW)
        val x1 = plotLeft + (((bin.xHigh - chart.xMin) / xSpan).toFloat() * plotW)
        val bh = ((bin.height / yMax).toFloat() * plotH).coerceIn(0f, plotH)
        val bw = (x1 - x0 - 1.5.dp.toPx()).coerceAtLeast(1.5f)
        val isHovered = idx == activeIdx
        val fill = if (isHovered) highlightColor.copy(alpha = 0.85f) else barColor.copy(alpha = 0.68f)
        val stroke = if (isHovered) highlightColor else barColor

        drawRect(
            color = fill,
            topLeft = Offset(x0 + 0.75.dp.toPx(), plotBottom - bh),
            size = Size(bw, bh)
        )
        drawRect(
            color = stroke,
            topLeft = Offset(x0 + 0.75.dp.toPx(), plotBottom - bh),
            size = Size(bw, bh),
            style = Stroke(width = 1.dp.toPx())
        )
    }

    // Mean vertical dashed line
    if (chart.mean.isFinite() && chart.mean in chart.xMin..chart.xMax) {
        val mx = plotLeft + (((chart.mean - chart.xMin) / xSpan).toFloat() * plotW)
        drawLine(
            color = highlightColor,
            start = Offset(mx, plotTop),
            end = Offset(mx, plotBottom),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
        )
    }

    drawLine(
        color = axisColor,
        start = Offset(plotLeft, plotBottom),
        end = Offset(plotRight, plotBottom),
        strokeWidth = 1.2.dp.toPx()
    )

    if (activeIdx in chart.bins.indices) {
        val b = chart.bins[activeIdx]
        val bannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = strongLabelArgb
            textSize = 10.sp.toPx()
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.RIGHT
        }
        val txt = "[${MathStatsEngine.fmt(b.xLow)}, ${MathStatsEngine.fmt(b.xHigh)}): ${chart.modeLabel}=${MathStatsEngine.fmt(b.height)} (n=${b.count})"
        drawContext.canvas.nativeCanvas.drawText(txt, plotRight, plotTop + 11.dp.toPx(), bannerPaint)
    }
}

private fun DrawScope.drawBarChart(
    chart: MathChartData.BarChartData,
    palette: List<Color>,
    axisColor: Color,
    gridColor: Color,
    labelArgb: Int,
    strongLabelArgb: Int
) {
    val leftPad = 42.dp.toPx()
    val rightPad = 14.dp.toPx()
    val topPad = 16.dp.toPx()
    val bottomPad = 24.dp.toPx()

    val plotLeft = leftPad
    val plotTop = topPad
    val plotW = (size.width - leftPad - rightPad).coerceAtLeast(10f)
    val plotH = (size.height - topPad - bottomPad).coerceAtLeast(10f)
    val plotRight = plotLeft + plotW
    val plotBottom = plotTop + plotH

    val yMin = chart.yMin
    val yMax = chart.yMax
    val ySpan = (yMax - yMin).takeIf { it > 1e-12 } ?: 1.0

    fun mapY(v: Double): Float =
        (plotBottom - ((v - yMin) / ySpan).toFloat() * plotH).coerceIn(plotTop, plotBottom)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = labelArgb
        textSize = 9.5.sp.toPx()
        typeface = Typeface.MONOSPACE
    }

    val ySteps = 4
    textPaint.textAlign = Paint.Align.RIGHT
    for (i in 0..ySteps) {
        val frac = i.toDouble() / ySteps
        val yVal = yMin + frac * ySpan
        val py = plotBottom - frac.toFloat() * plotH
        drawLine(
            color = gridColor,
            start = Offset(plotLeft, py),
            end = Offset(plotRight, py),
            strokeWidth = 1.dp.toPx()
        )
        drawContext.canvas.nativeCanvas.drawText(
            formatAxisTick(yVal, AxisScale.LINEAR),
            plotLeft - 5.dp.toPx(),
            py + 3.dp.toPx(),
            textPaint
        )
    }

    val yZero = mapY(0.0)
    val nGroups = chart.groups.size.coerceAtLeast(1)
    val groupStep = plotW / nGroups
    val showValueLabels = nGroups <= 10

    val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = strongLabelArgb
        textSize = 9.sp.toPx()
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
    }

    chart.groups.forEachIndexed { gIdx, group ->
        val groupLeft = plotLeft + gIdx * groupStep
        val m = group.values.size.coerceAtLeast(1)
        val innerPad = groupStep * 0.16f
        val availW = (groupStep - innerPad * 2).coerceAtLeast(2f)
        val barW = (availW / m).coerceAtMost(38.dp.toPx())
        val totalBarsW = barW * m
        val startX = groupLeft + (groupStep - totalBarsW) * 0.5f

        group.values.forEachIndexed { vIdx, v ->
            val color = if (m == 1) palette[gIdx % palette.size] else palette[vIdx % palette.size]
            val bx = startX + vIdx * barW
            val py = mapY(v)
            val topY = minOf(py, yZero)
            val barH = abs(py - yZero).coerceAtLeast(1.5.dp.toPx())

            drawRoundRect(
                color = color.copy(alpha = 0.78f),
                topLeft = Offset(bx + 1.dp.toPx(), topY),
                size = Size((barW - 2.dp.toPx()).coerceAtLeast(1.5f), barH),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )
            drawRoundRect(
                color = color,
                topLeft = Offset(bx + 1.dp.toPx(), topY),
                size = Size((barW - 2.dp.toPx()).coerceAtLeast(1.5f), barH),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )

            if (showValueLabels && m == 1) {
                val labelY = if (v >= 0) (topY - 4.dp.toPx()).coerceAtLeast(10.dp.toPx()) else (topY + barH + 10.dp.toPx())
                drawContext.canvas.nativeCanvas.drawText(
                    MathStatsEngine.fmt(v),
                    bx + barW * 0.5f,
                    labelY,
                    valPaint
                )
            }
        }

        // Category label at bottom
        if (nGroups <= 12 || gIdx % ((nGroups / 8).coerceAtLeast(1)) == 0) {
            textPaint.textAlign = Paint.Align.CENTER
            val catLbl = if (group.label.length > 8) group.label.take(7) + "…" else group.label
            drawContext.canvas.nativeCanvas.drawText(
                catLbl,
                groupLeft + groupStep * 0.5f,
                plotBottom + 14.dp.toPx(),
                textPaint
            )
        }
    }

    drawLine(
        color = axisColor,
        start = Offset(plotLeft, yZero),
        end = Offset(plotRight, yZero),
        strokeWidth = 1.2.dp.toPx()
    )
}

private fun DrawScope.drawPieChart(
    chart: MathChartData.PieChartData,
    palette: List<Color>,
    labelArgb: Int,
    strongLabelArgb: Int,
    isDark: Boolean
) {
    val pad = 12.dp.toPx()
    val availH = (size.height - pad * 2).coerceAtLeast(20f)
    val diameter = min(availH, size.width * 0.44f).coerceAtLeast(24f)
    val radius = diameter * 0.5f
    val cx = pad + radius + 8.dp.toPx()
    val cy = size.height * 0.5f
    val topLeft = Offset(cx - radius, cy - radius)
    val arcSize = Size(diameter, diameter)

    val separatorColor = if (isDark) Color(0xFF14161B) else Color.White
    var startAngle = -90f

    chart.slices.forEachIndexed { idx, slice ->
        val sweep = (slice.fraction.toFloat() * 360f).coerceAtLeast(0.5f)
        val color = palette[idx % palette.size]
        drawArc(
            color = color.copy(alpha = 0.85f),
            startAngle = startAngle,
            sweepAngle = sweep,
            useCenter = true,
            topLeft = topLeft,
            size = arcSize
        )
        drawArc(
            color = separatorColor,
            startAngle = startAngle,
            sweepAngle = sweep,
            useCenter = true,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = 1.8.dp.toPx())
        )
        startAngle += sweep
    }

    // Center donut cutout for clean modern look
    drawCircle(
        color = separatorColor,
        radius = radius * 0.42f,
        center = Offset(cx, cy)
    )

    // Legend on the right side
    val legendX = cx + radius + 20.dp.toPx()
    val maxLegendItems = 7
    val visibleSlices = chart.slices.take(maxLegendItems)
    val rowH = 18.dp.toPx()
    val totalLegendH = visibleSlices.size * rowH
    var curY = (cy - totalLegendH * 0.5f + 10.dp.toPx()).coerceAtLeast(18.dp.toPx())

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = strongLabelArgb
        textSize = 10.5.sp.toPx()
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = labelArgb
        textSize = 9.5.sp.toPx()
        typeface = Typeface.MONOSPACE
    }

    visibleSlices.forEachIndexed { idx, slice ->
        val color = palette[idx % palette.size]
        drawCircle(
            color = color,
            radius = 4.dp.toPx(),
            center = Offset(legendX, curY - 3.5.dp.toPx())
        )
        val lbl = if (slice.label.length > 12) slice.label.take(11) + "…" else slice.label
        val pct = String.format(Locale.US, "%.1f%%", slice.fraction * 100.0)
        drawContext.canvas.nativeCanvas.drawText(
            lbl,
            legendX + 10.dp.toPx(),
            curY,
            titlePaint
        )
        val lblW = titlePaint.measureText(lbl)
        drawContext.canvas.nativeCanvas.drawText(
            "  ${MathStatsEngine.fmt(slice.value)} ($pct)",
            legendX + 10.dp.toPx() + lblW,
            curY,
            subPaint
        )
        curY += rowH
    }
}

private fun DrawScope.drawBoxWhisker(
    chart: MathChartData.BoxWhiskerData,
    palette: List<Color>,
    axisColor: Color,
    gridColor: Color,
    labelArgb: Int,
    strongLabelArgb: Int
) {
    val leftPad = 48.dp.toPx()
    val rightPad = 16.dp.toPx()
    val topPad = 14.dp.toPx()
    val bottomPad = 24.dp.toPx()

    val plotLeft = leftPad
    val plotTop = topPad
    val plotW = (size.width - leftPad - rightPad).coerceAtLeast(10f)
    val plotH = (size.height - topPad - bottomPad).coerceAtLeast(10f)
    val plotRight = plotLeft + plotW
    val plotBottom = plotTop + plotH

    val span = (chart.globalMax - chart.globalMin).takeIf { it > 1e-12 } ?: 1.0
    fun mapX(v: Double): Float =
        (plotLeft + ((v - chart.globalMin) / span).toFloat() * plotW).coerceIn(plotLeft, plotRight)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = labelArgb
        textSize = 9.5.sp.toPx()
        typeface = Typeface.MONOSPACE
    }

    // X gridlines & ticks along bottom (horizontal box & whisker layout)
    val xSteps = 4
    for (i in 0..xSteps) {
        val frac = i.toDouble() / xSteps
        val xVal = chart.globalMin + frac * span
        val px = plotLeft + frac.toFloat() * plotW
        drawLine(
            color = gridColor,
            start = Offset(px, plotTop),
            end = Offset(px, plotBottom),
            strokeWidth = 1.dp.toPx()
        )
        textPaint.textAlign = when (i) {
            0 -> Paint.Align.LEFT
            xSteps -> Paint.Align.RIGHT
            else -> Paint.Align.CENTER
        }
        drawContext.canvas.nativeCanvas.drawText(
            formatAxisTick(xVal, AxisScale.LINEAR),
            px,
            plotBottom + 14.dp.toPx(),
            textPaint
        )
    }

    val n = chart.boxes.size.coerceAtLeast(1)
    val rowStep = plotH / n
    val boxH = (rowStep * 0.48f).coerceIn(10.dp.toPx(), 32.dp.toPx())

    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = strongLabelArgb
        textSize = 10.sp.toPx()
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.RIGHT
    }

    chart.boxes.forEachIndexed { idx, item ->
        val color = palette[idx % palette.size]
        val cy = plotTop + (idx + 0.5f) * rowStep
        val xMinW = mapX(item.min)
        val xQ1 = mapX(item.q1)
        val xMed = mapX(item.median)
        val xQ3 = mapX(item.q3)
        val xMaxW = mapX(item.max)

        // Row label
        val lbl = if (item.label.length > 7) item.label.take(6) + "…" else item.label
        drawContext.canvas.nativeCanvas.drawText(lbl, plotLeft - 6.dp.toPx(), cy + 3.dp.toPx(), labelPaint)

        // Whisker horizontal lines
        drawLine(
            color = color,
            start = Offset(xMinW, cy),
            end = Offset(xQ1, cy),
            strokeWidth = 1.6.dp.toPx()
        )
        drawLine(
            color = color,
            start = Offset(xQ3, cy),
            end = Offset(xMaxW, cy),
            strokeWidth = 1.6.dp.toPx()
        )

        // Whisker end caps
        val capHalf = boxH * 0.32f
        drawLine(
            color = color,
            start = Offset(xMinW, cy - capHalf),
            end = Offset(xMinW, cy + capHalf),
            strokeWidth = 1.6.dp.toPx()
        )
        drawLine(
            color = color,
            start = Offset(xMaxW, cy - capHalf),
            end = Offset(xMaxW, cy + capHalf),
            strokeWidth = 1.6.dp.toPx()
        )

        // IQR box [Q1, Q3]
        val boxW = (xQ3 - xQ1).coerceAtLeast(2.dp.toPx())
        drawRoundRect(
            color = color.copy(alpha = 0.32f),
            topLeft = Offset(xQ1, cy - boxH * 0.5f),
            size = Size(boxW, boxH),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(xQ1, cy - boxH * 0.5f),
            size = Size(boxW, boxH),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            style = Stroke(width = 1.6.dp.toPx())
        )

        // Median vertical line inside box
        drawLine(
            color = color,
            start = Offset(xMed, cy - boxH * 0.5f),
            end = Offset(xMed, cy + boxH * 0.5f),
            strokeWidth = 2.4.dp.toPx()
        )

        // Outliers
        for (out in item.outliers) {
            val ox = mapX(out)
            drawCircle(
                color = color,
                radius = 2.5.dp.toPx(),
                center = Offset(ox, cy),
                style = Stroke(width = 1.3.dp.toPx())
            )
        }
    }

    drawLine(
        color = axisColor,
        start = Offset(plotLeft, plotBottom),
        end = Offset(plotRight, plotBottom),
        strokeWidth = 1.2.dp.toPx()
    )
}

@Composable
fun ChartPreviewDialog(
    chart: MathChartData,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()

    val isLauncherDark = LocalLauncherColors.current.isDark
    var exportTheme by remember(isLauncherDark) {
        mutableStateOf(if (isLauncherDark) ChartExportTheme.DARK else ChartExportTheme.LIGHT)
    }
    var isExporting by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableStateOf<Float?>(null) }

    val isDarkStage = exportTheme == ChartExportTheme.DARK
    val stageBg = if (isDarkStage) Color(0xFF14161B) else Color.White

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .widthIn(max = 440.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = chart.badge,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = chart.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Theme selector (Dark / Light)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ChartExportTheme.entries.forEach { option ->
                        val selected = exportTheme == option
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) colorScheme.primaryContainer else colorScheme.surfaceContainerLow,
                            border = BorderStroke(
                                1.dp,
                                if (selected) colorScheme.primary.copy(alpha = 0.6f)
                                else colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { exportTheme = option }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Interactive Chart Stage (captured via graphicsLayer for PNG export)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .drawWithContent {
                            graphicsLayer.record {
                                this@drawWithContent.drawContent()
                            }
                            drawLayer(graphicsLayer)
                        }
                        .background(stageBg)
                        .pointerInput(chart) {
                            if (chart is MathChartData.Cartesian2D || chart is MathChartData.HistogramChart) {
                                detectTapGestures(
                                    onPress = { offset ->
                                        val w = size.width.toFloat().coerceAtLeast(1f)
                                        scrubFraction = (offset.x / w).coerceIn(0f, 1f)
                                        tryAwaitRelease()
                                        scrubFraction = null
                                    }
                                )
                            }
                        }
                        .pointerInput(chart) {
                            if (chart is MathChartData.Cartesian2D || chart is MathChartData.HistogramChart) {
                                detectHorizontalDragGestures(
                                    onDragStart = { offset ->
                                        val w = size.width.toFloat().coerceAtLeast(1f)
                                        scrubFraction = (offset.x / w).coerceIn(0f, 1f)
                                    },
                                    onHorizontalDrag = { change, _ ->
                                        val w = size.width.toFloat().coerceAtLeast(1f)
                                        scrubFraction = (change.position.x / w).coerceIn(0f, 1f)
                                    },
                                    onDragEnd = { scrubFraction = null },
                                    onDragCancel = { scrubFraction = null }
                                )
                            }
                        }
                ) {
                    MathChartCanvas(
                        chart = chart,
                        isDark = isDarkStage,
                        scrubFraction = scrubFraction,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.55f)),
                        modifier = Modifier.matchParentSize()
                    ) {}
                }

                // Expression bar with copy button
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colorScheme.surfaceContainerLowest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = chart.title,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { TexExportHelper.copyPlainText(context, "Math Expression", chart.title, "Copied expression") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy expression",
                                tint = colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Action buttons: Share / Save PNG / Copy PNG
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        enabled = !isExporting,
                        onClick = {
                            scope.launch {
                                isExporting = true
                                try {
                                    val bmp = graphicsLayer.toImageBitmap().asAndroidBitmap()
                                    TexExportHelper.sharePng(context, bmp).onFailure {
                                        Toast.makeText(context, "Could not share chart", Toast.LENGTH_SHORT).show()
                                    }
                                } finally {
                                    isExporting = false
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primary,
                            contentColor = colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(20.dp))
                    }

                    OutlinedButton(
                        enabled = !isExporting,
                        onClick = {
                            scope.launch {
                                isExporting = true
                                try {
                                    val bmp = graphicsLayer.toImageBitmap().asAndroidBitmap()
                                    TexExportHelper.savePngToGallery(
                                        context,
                                        bmp,
                                        filePrefix = "pallas_chart",
                                        folderName = "Pallas Charts"
                                    )
                                        .onSuccess {
                                            Toast.makeText(context, "Saved to Pictures/Pallas Charts", Toast.LENGTH_SHORT).show()
                                        }
                                        .onFailure {
                                            Toast.makeText(context, "Could not save PNG", Toast.LENGTH_SHORT).show()
                                        }
                                } finally {
                                    isExporting = false
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Save PNG", modifier = Modifier.size(20.dp))
                    }

                    OutlinedButton(
                        enabled = !isExporting,
                        onClick = {
                            scope.launch {
                                isExporting = true
                                try {
                                    val bmp = graphicsLayer.toImageBitmap().asAndroidBitmap()
                                    TexExportHelper.copyPngToClipboard(context, bmp)
                                        .onSuccess {
                                            Toast.makeText(context, "Chart copied to clipboard", Toast.LENGTH_SHORT).show()
                                        }
                                        .onFailure {
                                            Toast.makeText(context, "Could not copy chart", Toast.LENGTH_SHORT).show()
                                        }
                                } finally {
                                    isExporting = false
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy PNG", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
