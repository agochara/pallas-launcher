package com.pallas.launcher.domain.notebook

import com.pallas.launcher.domain.search.AxisScale
import com.pallas.launcher.domain.search.ChartPoint
import com.pallas.launcher.domain.search.CurveSeries
import com.pallas.launcher.domain.search.MathChartData
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class NotebookCellOutput(
    val primaryValue: String = "",
    val badge: String? = null,
    val subtitle: String = "",
    val consoleOutput: String? = null,
    val latexSource: String? = null,
    val chart: MathChartData? = null,
    val copyText: String = "",
    val isError: Boolean = false
)

data class NotebookCell(
    val id: String = UUID.randomUUID().toString(),
    val source: String = "",
    val executionOrder: Int? = null,
    val output: NotebookCellOutput? = null
)

data class ComputationalNotebook(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val customTitle: String? = null,
    val cells: List<NotebookCell> = listOf(NotebookCell()),
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
) {
    val displayTitle: String
        get() = customTitle?.takeIf { it.isNotBlank() } ?: title
}

object NotebookJsonCodec {

    fun deriveAutoTitle(cells: List<NotebookCell>, createdAtMs: Long): String {
        val firstLine = cells.asSequence()
            .flatMap { it.source.lineSequence() }
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() && !it.startsWith("(*") }
        if (!firstLine.isNullOrBlank()) {
            return if (firstLine.length > 40) firstLine.take(39) + "…" else firstLine
        }
        val stamp = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(createdAtMs))
        return "Notebook • $stamp"
    }

    fun toJson(nb: ComputationalNotebook): JSONObject = JSONObject().apply {
        put("id", nb.id)
        put("title", nb.title)
        if (nb.customTitle != null) put("customTitle", nb.customTitle) else put("customTitle", JSONObject.NULL)
        put("createdAtMs", nb.createdAtMs)
        put("updatedAtMs", nb.updatedAtMs)
        put("cells", JSONArray().apply {
            nb.cells.forEach { cell ->
                put(cellToJson(cell))
            }
        })
    }

    fun fromJson(obj: JSONObject): ComputationalNotebook {
        val id = obj.optString("id").ifBlank { UUID.randomUUID().toString() }
        val created = obj.optLong("createdAtMs", System.currentTimeMillis())
        val updated = obj.optLong("updatedAtMs", created)
        val customTitle = if (obj.has("customTitle") && !obj.isNull("customTitle")) {
            obj.optString("customTitle").takeIf { it.isNotBlank() }
        } else null
        val cellsArr = obj.optJSONArray("cells")
        val cells = if (cellsArr != null && cellsArr.length() > 0) {
            (0 until cellsArr.length()).mapNotNull { idx ->
                cellsArr.optJSONObject(idx)?.let(::cellFromJson)
            }.ifEmpty { listOf(NotebookCell()) }
        } else {
            listOf(NotebookCell())
        }
        val title = obj.optString("title").ifBlank { deriveAutoTitle(cells, created) }
        return ComputationalNotebook(
            id = id,
            title = title,
            customTitle = customTitle,
            cells = cells,
            createdAtMs = created,
            updatedAtMs = updated
        )
    }

    private fun cellToJson(c: NotebookCell): JSONObject = JSONObject().apply {
        put("id", c.id)
        put("source", c.source)
        if (c.executionOrder != null) put("executionOrder", c.executionOrder) else put("executionOrder", JSONObject.NULL)
        if (c.output != null) put("output", outputToJson(c.output)) else put("output", JSONObject.NULL)
    }

    private fun cellFromJson(o: JSONObject): NotebookCell {
        val id = o.optString("id").ifBlank { UUID.randomUUID().toString() }
        val source = o.optString("source", "")
        val order = if (o.has("executionOrder") && !o.isNull("executionOrder")) o.optInt("executionOrder") else null
        val out = o.optJSONObject("output")?.let(::outputFromJson)
        return NotebookCell(id = id, source = source, executionOrder = order, output = out)
    }

    private fun outputToJson(out: NotebookCellOutput): JSONObject = JSONObject().apply {
        put("primaryValue", out.primaryValue)
        if (out.badge != null) put("badge", out.badge) else put("badge", JSONObject.NULL)
        put("subtitle", out.subtitle)
        if (out.consoleOutput != null) put("consoleOutput", out.consoleOutput) else put("consoleOutput", JSONObject.NULL)
        if (out.latexSource != null) put("latexSource", out.latexSource) else put("latexSource", JSONObject.NULL)
        put("copyText", out.copyText)
        put("isError", out.isError)
        if (out.chart != null) put("chart", chartToJson(out.chart)) else put("chart", JSONObject.NULL)
    }

    private fun outputFromJson(o: JSONObject): NotebookCellOutput = NotebookCellOutput(
        primaryValue = o.optString("primaryValue", ""),
        badge = if (o.has("badge") && !o.isNull("badge")) o.optString("badge") else null,
        subtitle = o.optString("subtitle", ""),
        consoleOutput = if (o.has("consoleOutput") && !o.isNull("consoleOutput")) o.optString("consoleOutput") else null,
        latexSource = if (o.has("latexSource") && !o.isNull("latexSource")) o.optString("latexSource") else null,
        chart = o.optJSONObject("chart")?.let(::chartFromJson),
        copyText = o.optString("copyText", ""),
        isError = o.optBoolean("isError", false)
    )

    private fun chartToJson(chart: MathChartData): JSONObject = JSONObject().apply {
        put("title", chart.title)
        put("summary", chart.summary)
        put("badge", chart.badge)
        when (chart) {
            is MathChartData.Cartesian2D -> {
                put("kind", "cartesian2d")
                put("xMin", chart.xMin)
                put("xMax", chart.xMax)
                put("yMin", chart.yMin)
                put("yMax", chart.yMax)
                put("xScale", chart.xScale.name)
                put("yScale", chart.yScale.name)
                put("isAspectEqual", chart.isAspectEqual)
                put("series", JSONArray().apply {
                    chart.series.forEach { s ->
                        put(JSONObject().apply {
                            put("label", s.label)
                            put("showLine", s.showLine)
                            put("showPoints", s.showPoints)
                            put("segments", JSONArray().apply {
                                s.segments.forEach { seg ->
                                    put(JSONArray().apply {
                                        seg.forEach { pt ->
                                            put(pt.x)
                                            put(pt.y)
                                        }
                                    })
                                }
                            })
                        })
                    }
                })
            }
            is MathChartData.HistogramChart -> {
                put("kind", "histogram")
                put("xMin", chart.xMin)
                put("xMax", chart.xMax)
                put("yMax", chart.yMax)
                put("modeLabel", chart.modeLabel)
                put("sampleSize", chart.sampleSize)
                put("mean", chart.mean)
                put("sd", chart.sd)
                put("bins", JSONArray().apply {
                    chart.bins.forEach { b ->
                        put(JSONObject().apply {
                            put("xLow", b.xLow)
                            put("xHigh", b.xHigh)
                            put("height", b.height)
                            put("count", b.count)
                        })
                    }
                })
            }
            is MathChartData.BarChartData -> {
                put("kind", "bar")
                put("yMin", chart.yMin)
                put("yMax", chart.yMax)
                put("seriesLabels", JSONArray(chart.seriesLabels))
                put("groups", JSONArray().apply {
                    chart.groups.forEach { g ->
                        put(JSONObject().apply {
                            put("label", g.label)
                            put("values", JSONArray(g.values))
                        })
                    }
                })
            }
            is MathChartData.PieChartData -> {
                put("kind", "pie")
                put("total", chart.total)
                put("slices", JSONArray().apply {
                    chart.slices.forEach { s ->
                        put(JSONObject().apply {
                            put("label", s.label)
                            put("value", s.value)
                            put("fraction", s.fraction)
                        })
                    }
                })
            }
            is MathChartData.BoxWhiskerData -> {
                put("kind", "boxwhisker")
                put("globalMin", chart.globalMin)
                put("globalMax", chart.globalMax)
                put("boxes", JSONArray().apply {
                    chart.boxes.forEach { b ->
                        put(JSONObject().apply {
                            put("label", b.label)
                            put("min", b.min)
                            put("q1", b.q1)
                            put("median", b.median)
                            put("q3", b.q3)
                            put("max", b.max)
                            put("outliers", JSONArray(b.outliers))
                        })
                    }
                })
            }
        }
    }

    private fun chartFromJson(o: JSONObject): MathChartData? = runCatching {
        val title = o.optString("title", "")
        val summary = o.optString("summary", "")
        val badge = o.optString("badge", "Plot")
        when (o.optString("kind")) {
            "cartesian2d" -> {
                val seriesArr = o.getJSONArray("series")
                val series = (0 until seriesArr.length()).map { si ->
                    val so = seriesArr.getJSONObject(si)
                    val segsArr = so.getJSONArray("segments")
                    val segments = (0 until segsArr.length()).map { gi ->
                        val flat = segsArr.getJSONArray(gi)
                        val pts = ArrayList<ChartPoint>(flat.length() / 2)
                        var k = 0
                        while (k + 1 < flat.length()) {
                            pts.add(ChartPoint(flat.getDouble(k), flat.getDouble(k + 1)))
                            k += 2
                        }
                        pts
                    }
                    CurveSeries(
                        label = so.optString("label", ""),
                        segments = segments,
                        showLine = so.optBoolean("showLine", true),
                        showPoints = so.optBoolean("showPoints", false)
                    )
                }
                MathChartData.Cartesian2D(
                    title = title,
                    summary = summary,
                    badge = badge,
                    series = series,
                    xMin = o.getDouble("xMin"),
                    xMax = o.getDouble("xMax"),
                    yMin = o.getDouble("yMin"),
                    yMax = o.getDouble("yMax"),
                    xScale = AxisScale.entries.firstOrNull { it.name == o.optString("xScale") } ?: AxisScale.LINEAR,
                    yScale = AxisScale.entries.firstOrNull { it.name == o.optString("yScale") } ?: AxisScale.LINEAR,
                    isAspectEqual = o.optBoolean("isAspectEqual", false)
                )
            }
            "histogram" -> {
                val binsArr = o.getJSONArray("bins")
                val bins = (0 until binsArr.length()).map { bi ->
                    val bo = binsArr.getJSONObject(bi)
                    MathChartData.HistogramBin(
                        xLow = bo.getDouble("xLow"),
                        xHigh = bo.getDouble("xHigh"),
                        height = bo.getDouble("height"),
                        count = bo.getInt("count")
                    )
                }
                MathChartData.HistogramChart(
                    title = title,
                    summary = summary,
                    badge = badge,
                    bins = bins,
                    xMin = o.getDouble("xMin"),
                    xMax = o.getDouble("xMax"),
                    yMax = o.getDouble("yMax"),
                    modeLabel = o.optString("modeLabel", "Count"),
                    sampleSize = o.optInt("sampleSize", 0),
                    mean = o.optDouble("mean", Double.NaN),
                    sd = o.optDouble("sd", Double.NaN)
                )
            }
            "bar" -> {
                val labelsArr = o.optJSONArray("seriesLabels")
                val seriesLabels = if (labelsArr != null) {
                    (0 until labelsArr.length()).map { labelsArr.getString(it) }
                } else emptyList()
                val groupsArr = o.getJSONArray("groups")
                val groups = (0 until groupsArr.length()).map { gi ->
                    val go = groupsArr.getJSONObject(gi)
                    val valsArr = go.getJSONArray("values")
                    val vals = (0 until valsArr.length()).map { valsArr.getDouble(it) }
                    MathChartData.BarGroup(label = go.getString("label"), values = vals)
                }
                MathChartData.BarChartData(
                    title = title,
                    summary = summary,
                    badge = badge,
                    groups = groups,
                    seriesLabels = seriesLabels,
                    yMin = o.getDouble("yMin"),
                    yMax = o.getDouble("yMax")
                )
            }
            "pie" -> {
                val slicesArr = o.getJSONArray("slices")
                val slices = (0 until slicesArr.length()).map { si ->
                    val so = slicesArr.getJSONObject(si)
                    MathChartData.PieSlice(
                        label = so.getString("label"),
                        value = so.getDouble("value"),
                        fraction = so.getDouble("fraction")
                    )
                }
                MathChartData.PieChartData(
                    title = title,
                    summary = summary,
                    badge = badge,
                    slices = slices,
                    total = o.getDouble("total")
                )
            }
            "boxwhisker" -> {
                val boxesArr = o.getJSONArray("boxes")
                val boxes = (0 until boxesArr.length()).map { bi ->
                    val bo = boxesArr.getJSONObject(bi)
                    val outArr = bo.optJSONArray("outliers")
                    val outliers = if (outArr != null) {
                        (0 until outArr.length()).map { outArr.getDouble(it) }
                    } else emptyList()
                    MathChartData.BoxWhiskerSummary(
                        label = bo.getString("label"),
                        min = bo.getDouble("min"),
                        q1 = bo.getDouble("q1"),
                        median = bo.getDouble("median"),
                        q3 = bo.getDouble("q3"),
                        max = bo.getDouble("max"),
                        outliers = outliers
                    )
                }
                MathChartData.BoxWhiskerData(
                    title = title,
                    summary = summary,
                    badge = badge,
                    boxes = boxes,
                    globalMin = o.getDouble("globalMin"),
                    globalMax = o.getDouble("globalMax")
                )
            }
            else -> null
        }
    }.getOrNull()
}
