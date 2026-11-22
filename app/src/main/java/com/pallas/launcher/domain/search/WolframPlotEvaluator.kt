package com.pallas.launcher.domain.search

import java.util.Locale
import kotlin.math.*

internal object WolframPlotEvaluator {

    val PLOT_FUNCTION_NAMES: Set<String> = setOf(
        "plot", "logplot", "loglinearplot", "loglogplot",
        "parametricplot", "polarplot",
        "listplot", "listlineplot", "listlogplot", "listloglinearplot", "listloglogplot",
        "histogram", "barchart", "piechart", "boxwhiskerchart"
    )

    private val CONST_IDENTS = setOf(
        "pi", "π", "e", "infinity", "inf", "∞", "true", "false", "null", "%", "out"
    )

    fun tryEvalPlotOrChart(
        fn: String,
        args: List<MathNode>,
        ctx: ExecutionContext
    ): MathValue.ChartVal? {
        if (fn !in PLOT_FUNCTION_NAMES) return null
        val chart: MathChartData = when (fn) {
            "plot" -> evalFunctionPlot("Plot", args, ctx, AxisScale.LINEAR, AxisScale.LINEAR)
            "logplot" -> evalFunctionPlot("LogPlot", args, ctx, AxisScale.LINEAR, AxisScale.LOG10)
            "loglinearplot" -> evalFunctionPlot("LogLinearPlot", args, ctx, AxisScale.LOG10, AxisScale.LINEAR)
            "loglogplot" -> evalFunctionPlot("LogLogPlot", args, ctx, AxisScale.LOG10, AxisScale.LOG10)
            "parametricplot" -> evalParametricPlot(args, ctx)
            "polarplot" -> evalPolarPlot(args, ctx)
            "listplot" -> evalListPlot("ListPlot", args, ctx, AxisScale.LINEAR, AxisScale.LINEAR, defaultJoined = false)
            "listlineplot" -> evalListPlot("ListLinePlot", args, ctx, AxisScale.LINEAR, AxisScale.LINEAR, defaultJoined = true)
            "listlogplot" -> evalListPlot("ListLogPlot", args, ctx, AxisScale.LINEAR, AxisScale.LOG10, defaultJoined = false)
            "listloglinearplot" -> evalListPlot("ListLogLinearPlot", args, ctx, AxisScale.LOG10, AxisScale.LINEAR, defaultJoined = false)
            "listloglogplot" -> evalListPlot("ListLogLogPlot", args, ctx, AxisScale.LOG10, AxisScale.LOG10, defaultJoined = false)
            "histogram" -> evalHistogram(args, ctx)
            "barchart" -> evalBarChart(args, ctx)
            "piechart" -> evalPieChart(args, ctx)
            "boxwhiskerchart" -> evalBoxWhiskerChart(args, ctx)
            else -> return null
        }
        return MathValue.ChartVal(chart)
    }

    // =========================================================================
    // 1. Function Plots: Plot, LogPlot, LogLinearPlot, LogLogPlot
    // =========================================================================

    private data class DomainSpec(val varName: String, val min: Double, val max: Double)

    private fun parseDomainSpec(
        specNode: MathNode?,
        fallbackVar: String,
        defaultMin: Double,
        defaultMax: Double,
        ctx: ExecutionContext
    ): DomainSpec {
        if (specNode is MathNode.ListNode) {
            val items = specNode.items
            if (items.size >= 3 && items[0] is MathNode.IdentNode) {
                val v = (items[0] as MathNode.IdentNode).name.lowercase(Locale.ROOT)
                val a = items[1].eval(ctx).asDouble()
                val b = items[2].eval(ctx).asDouble()
                if (a.isFinite() && b.isFinite() && a != b) {
                    return DomainSpec(v, minOf(a, b), maxOf(a, b))
                }
            } else if (items.size == 2) {
                val first = items[0]
                if (first is MathNode.IdentNode && first.name.lowercase(Locale.ROOT) !in CONST_IDENTS && ctx.env[first.name.lowercase(Locale.ROOT)] == null) {
                    val v = first.name.lowercase(Locale.ROOT)
                    val b = items[1].eval(ctx).asDouble()
                    if (b.isFinite() && b != 0.0) {
                        return DomainSpec(v, minOf(0.0, b), maxOf(0.0, b))
                    }
                } else {
                    val a = items[0].eval(ctx).asDouble()
                    val b = items[1].eval(ctx).asDouble()
                    if (a.isFinite() && b.isFinite() && a != b) {
                        return DomainSpec(fallbackVar, minOf(a, b), maxOf(a, b))
                    }
                }
            }
        }
        return DomainSpec(fallbackVar, defaultMin, defaultMax)
    }

    private fun findFreeVariables(node: MathNode, ctx: ExecutionContext): Set<String> {
        val vars = linkedSetOf<String>()
        fun walk(n: MathNode, bound: Set<String> = emptySet()) {
            when (n) {
                is MathNode.IdentNode -> {
                    val lower = n.name.lowercase(Locale.ROOT)
                    if (lower !in bound && lower !in CONST_IDENTS && ctx.env[lower] == null) {
                        vars.add(lower)
                    }
                }
                is MathNode.UnaryOpNode -> walk(n.child, bound)
                is MathNode.BinaryOpNode -> { walk(n.left, bound); walk(n.right, bound) }
                is MathNode.ComparisonNode -> { walk(n.left, bound); walk(n.right, bound) }
                is MathNode.ChainedComparisonNode -> n.operands.forEach { walk(it, bound) }
                is MathNode.LogicalOpNode -> { walk(n.left, bound); walk(n.right, bound) }
                is MathNode.CallNode -> n.args.forEach { walk(it, bound) }
                is MathNode.ApplyCallNode -> { walk(n.callee, bound); n.args.forEach { walk(it, bound) } }
                is MathNode.ListNode -> n.items.forEach { walk(it, bound) }
                is MathNode.SpanNode -> {
                    n.start?.let { walk(it, bound) }
                    n.end?.let { walk(it, bound) }
                    n.step?.let { walk(it, bound) }
                }
                is MathNode.PrefixNode -> { walk(n.func, bound); walk(n.arg, bound) }
                is MathNode.PostfixNode -> { walk(n.arg, bound); walk(n.func, bound) }
                is MathNode.MapNode -> { walk(n.func, bound); walk(n.listNode, bound) }
                is MathNode.ApplyNode -> { walk(n.func, bound); walk(n.exprNode, bound) }
                is MathNode.PartNode -> { walk(n.expr, bound); n.indices.forEach { walk(it, bound) } }
                is MathNode.ReplaceAllNode -> walk(n.expr, bound)
                is MathNode.CompoundNode -> n.expressions.forEach { walk(it, bound) }
                is MathNode.AssignNode -> walk(n.expr, bound)
                is MathNode.AssignDelayedNode -> walk(n.expr, bound)
                is MathNode.AssignFunctionNode -> walk(n.body, bound + n.paramNames.map { it.lowercase(Locale.ROOT) })
                is MathNode.FunctionNode -> walk(n.body, bound + n.paramNames.map { it.lowercase(Locale.ROOT) })
                is MathNode.ComposeNode -> { walk(n.left, bound); walk(n.right, bound) }
                is MathNode.ComposeBodyNode -> { walk(n.first, bound); walk(n.second, bound) }
                is MathNode.StringJoinNode -> { walk(n.left, bound); walk(n.right, bound) }
                else -> {}
            }
        }
        walk(node)
        return vars
    }

    private fun inferDefaultVariable(node: MathNode, ctx: ExecutionContext): String {
        val free = findFreeVariables(node, ctx)
        for (preferred in listOf("x", "t", "u", "theta", "θ", "z", "n")) {
            if (preferred in free) return preferred
        }
        return free.firstOrNull() ?: "x"
    }

    private fun evalScalarAt(
        fnNode: MathNode,
        varName: String,
        xVal: Double,
        usesVarDirectly: Boolean,
        ctx: ExecutionContext
    ): Double {
        ctx.tick()
        val saved = ctx.env[varName]
        return try {
            ctx.env[varName] = MathValue.NumberVal(xVal)
            val v = if (usesVarDirectly) {
                fnNode.eval(ctx)
            } else {
                when (fnNode) {
                    is MathNode.FunctionNode -> invokeFunction(fnNode, listOf(MathValue.NumberVal(xVal)), ctx)
                    is MathNode.IdentNode -> {
                        val evaluated = fnNode.eval(ctx)
                        when (evaluated) {
                            is MathValue.NumberVal -> evaluated
                            is MathValue.DistVal -> MathValue.NumberVal(evaluated.dist.pdf(xVal))
                            else -> invokeFunction(fnNode, listOf(MathValue.NumberVal(xVal)), ctx)
                        }
                    }
                    else -> {
                        val evaluated = fnNode.eval(ctx)
                        when (evaluated) {
                            is MathValue.FunctionVal -> invokePureFunction(evaluated, listOf(MathValue.NumberVal(xVal)), ctx)
                            is MathValue.DistVal -> MathValue.NumberVal(evaluated.dist.pdf(xVal))
                            else -> evaluated
                        }
                    }
                }
            }
            when (v) {
                is MathValue.DistVal -> v.dist.pdf(xVal)
                else -> v.asDouble()
            }
        } catch (_: ComputationLimitException) {
            throw ComputationLimitException("Plot evaluation timed out")
        } catch (_: Throwable) {
            Double.NaN
        } finally {
            if (saved != null) ctx.env[varName] = saved else ctx.env.remove(varName)
        }
    }

    private fun distAutoDomain(dist: MathDistribution, xScale: AxisScale): Pair<Double, Double> {
        val qLow = runCatching { dist.quantile(0.005) }.getOrDefault(Double.NaN)
        val qHigh = runCatching { dist.quantile(0.995) }.getOrDefault(Double.NaN)
        val mean = dist.mean()
        val sd = dist.standardDeviation()
        var low = if (qLow.isFinite()) qLow else if (mean.isFinite() && sd.isFinite() && sd > 0) mean - 4.0 * sd else -5.0
        var high = if (qHigh.isFinite()) qHigh else if (mean.isFinite() && sd.isFinite() && sd > 0) mean + 4.0 * sd else 5.0
        if (high - low > 100.0 && qLow.isFinite()) {
            val q90 = runCatching { dist.quantile(0.92) }.getOrDefault(low + 15.0)
            if (q90.isFinite() && q90 > low) high = q90
        }
        if (low >= high) {
            low -= 1.0
            high += 1.0
        }
        if (xScale == AxisScale.LOG10 && low <= 0.0) {
            low = if (high > 0.0) minOf(1e-2, high / 500.0) else 0.1
            if (high <= low) high = low * 100.0
        }
        return low to high
    }

    private fun evalFunctionPlot(
        plotName: String,
        args: List<MathNode>,
        ctx: ExecutionContext,
        xScale: AxisScale,
        yScale: AxisScale
    ): MathChartData.Cartesian2D {
        if (args.isEmpty()) error("$plotName requires at least 1 argument")
        val firstArg = args[0]
        val fnNodes: List<MathNode> = if (firstArg is MathNode.ListNode) {
            firstArg.items.ifEmpty { listOf(firstArg) }
        } else {
            listOf(firstArg)
        }

        val inferredVar = inferDefaultVariable(firstArg, ctx)
        val maybeDist: MathDistribution? = if (args.size == 1 && fnNodes.size == 1 && findFreeVariables(fnNodes[0], ctx).isEmpty()) {
            runCatching { (fnNodes[0].eval(ctx) as? MathValue.DistVal)?.dist }.getOrNull()
        } else null

        val (defMin, defMax) = if (maybeDist != null) {
            distAutoDomain(maybeDist, xScale)
        } else if (xScale == AxisScale.LOG10) {
            0.1 to 10.0
        } else {
            -5.0 to 5.0
        }

        val domain = parseDomainSpec(args.getOrNull(1), inferredVar, defMin, defMax, ctx)
        var xMinReal = domain.min
        var xMaxReal = domain.max
        if (xScale == AxisScale.LOG10) {
            if (xMaxReal <= 0.0) error("$plotName requires positive x domain")
            if (xMinReal <= 0.0) xMinReal = minOf(1e-3, xMaxReal / 1000.0)
            if (xMinReal >= xMaxReal) xMinReal = xMaxReal / 10.0
        }

        val sampleCount = 200
        val xPlotMin = if (xScale == AxisScale.LOG10) log10(xMinReal) else xMinReal
        val xPlotMax = if (xScale == AxisScale.LOG10) log10(xMaxReal) else xMaxReal

        val rawSeries = fnNodes.map { fnNode ->
            val label = nodeToLabel(fnNode)
            val free = findFreeVariables(fnNode, ctx)
            val usesVarDirectly = domain.varName in free
            val pts = ArrayList<ChartPoint?>(sampleCount)
            for (i in 0 until sampleCount) {
                val frac = i.toDouble() / (sampleCount - 1)
                val xPlot = xPlotMin + frac * (xPlotMax - xPlotMin)
                val xReal = if (xScale == AxisScale.LOG10) 10.0.pow(xPlot) else xPlot
                val yReal = evalScalarAt(fnNode, domain.varName, xReal, usesVarDirectly, ctx)
                val yPlot = when {
                    !yReal.isFinite() -> Double.NaN
                    yScale == AxisScale.LOG10 -> if (yReal > 0.0) log10(yReal) else Double.NaN
                    else -> yReal
                }
                if (xPlot.isFinite() && yPlot.isFinite()) {
                    pts.add(ChartPoint(xPlot, yPlot))
                } else {
                    pts.add(null)
                }
            }
            label to pts
        }

        return buildCartesianFromSampledSeries(
            title = "$plotName[${fnNodes.joinToString(", ") { nodeToLabel(it) }}]",
            badge = plotName,
            varLabel = domain.varName,
            xRealMin = xMinReal,
            xRealMax = xMaxReal,
            xPlotMin = xPlotMin,
            xPlotMax = xPlotMax,
            xScale = xScale,
            yScale = yScale,
            rawSeries = rawSeries
        )
    }

    private fun buildCartesianFromSampledSeries(
        title: String,
        badge: String,
        varLabel: String,
        xRealMin: Double,
        xRealMax: Double,
        xPlotMin: Double,
        xPlotMax: Double,
        xScale: AxisScale,
        yScale: AxisScale,
        rawSeries: List<Pair<String, List<ChartPoint?>>>
    ): MathChartData.Cartesian2D {
        val allY = rawSeries.flatMap { (_, pts) -> pts.mapNotNull { it?.y } }.sorted()
        if (allY.isEmpty()) error("No real values in plot domain")

        val rawMinY = allY.first()
        val rawMaxY = allY.last()
        val p05 = percentileSorted(allY, 0.05)
        val p95 = percentileSorted(allY, 0.95)
        val coreSpan = (p95 - p05).coerceAtLeast(1e-9)
        val totalSpan = rawMaxY - rawMinY

        val hasPoles = allY.size >= 20 && totalSpan > 8.0 * coreSpan && coreSpan > 1e-6
        var yMin = if (hasPoles) percentileSorted(allY, 0.03) - 0.35 * coreSpan else rawMinY
        var yMax = if (hasPoles) percentileSorted(allY, 0.97) + 0.35 * coreSpan else rawMaxY
        if (abs(yMax - yMin) < 1e-9) {
            yMin -= 1.0
            yMax += 1.0
        }
        val ySpan = yMax - yMin
        val clipMargin = if (hasPoles) 0.5 * ySpan else Double.POSITIVE_INFINITY

        val builtSeries = rawSeries.map { (label, pts) ->
            val segments = mutableListOf<List<ChartPoint>>()
            var current = mutableListOf<ChartPoint>()
            for (pt in pts) {
                if (pt == null || pt.y < yMin - clipMargin || pt.y > yMax + clipMargin) {
                    if (current.isNotEmpty()) {
                        segments.add(current)
                        current = mutableListOf()
                    }
                    continue
                }
                if (current.isNotEmpty()) {
                    val prev = current.last()
                    val jump = abs(pt.y - prev.y)
                    if (hasPoles && jump > 0.72 * ySpan && (pt.y - yMin) * (prev.y - yMax) <= 0.0) {
                        segments.add(current)
                        current = mutableListOf()
                    }
                }
                current.add(pt)
            }
            if (current.isNotEmpty()) segments.add(current)
            CurveSeries(
                label = label,
                segments = segments,
                showLine = true,
                showPoints = false
            )
        }

        val yDispMin = if (yScale == AxisScale.LOG10) 10.0.pow(yMin) else yMin
        val yDispMax = if (yScale == AxisScale.LOG10) 10.0.pow(yMax) else yMax
        val summary = "$varLabel ∈ [${MathStatsEngine.fmt(xRealMin)}, ${MathStatsEngine.fmt(xRealMax)}] • y ∈ [${MathStatsEngine.fmt(yDispMin)}, ${MathStatsEngine.fmt(yDispMax)}]"

        return MathChartData.Cartesian2D(
            title = title,
            summary = summary,
            badge = badge,
            series = builtSeries,
            xMin = xPlotMin,
            xMax = xPlotMax,
            yMin = yMin,
            yMax = yMax,
            xScale = xScale,
            yScale = yScale,
            isAspectEqual = false
        )
    }

    // =========================================================================
    // 2. ParametricPlot & PolarPlot
    // =========================================================================

    private fun evalParametricPlot(
        args: List<MathNode>,
        ctx: ExecutionContext
    ): MathChartData.Cartesian2D {
        if (args.isEmpty()) error("ParametricPlot requires arguments")
        val first = args[0] as? MathNode.ListNode ?: error("ParametricPlot expects {fx, fy}")
        val pairs: List<Pair<MathNode, MathNode>> = if (first.items.size == 2 && first.items.none { it is MathNode.ListNode }) {
            listOf(first.items[0] to first.items[1])
        } else {
            first.items.mapNotNull { sub ->
                val list = sub as? MathNode.ListNode
                if (list != null && list.items.size >= 2) list.items[0] to list.items[1] else null
            }
        }
        if (pairs.isEmpty()) error("ParametricPlot expects {fx, fy} pair(s)")

        val inferredVar = inferDefaultVariable(first, ctx)
        val domain = parseDomainSpec(args.getOrNull(1), inferredVar, 0.0, 2.0 * PI, ctx)
        val sampleCount = 240

        var xMin = Double.POSITIVE_INFINITY
        var xMax = Double.NEGATIVE_INFINITY
        var yMin = Double.POSITIVE_INFINITY
        var yMax = Double.NEGATIVE_INFINITY

        val series = pairs.map { (fxNode, fyNode) ->
            val label = "{${nodeToLabel(fxNode)}, ${nodeToLabel(fyNode)}}"
            val usesX = domain.varName in findFreeVariables(fxNode, ctx)
            val usesY = domain.varName in findFreeVariables(fyNode, ctx)
            val segments = mutableListOf<List<ChartPoint>>()
            var current = mutableListOf<ChartPoint>()
            for (i in 0 until sampleCount) {
                val u = domain.min + (i.toDouble() / (sampleCount - 1)) * (domain.max - domain.min)
                val x = evalScalarAt(fxNode, domain.varName, u, usesX, ctx)
                val y = evalScalarAt(fyNode, domain.varName, u, usesY, ctx)
                if (x.isFinite() && y.isFinite()) {
                    if (x < xMin) xMin = x
                    if (x > xMax) xMax = x
                    if (y < yMin) yMin = y
                    if (y > yMax) yMax = y
                    current.add(ChartPoint(x, y))
                } else if (current.isNotEmpty()) {
                    segments.add(current)
                    current = mutableListOf()
                }
            }
            if (current.isNotEmpty()) segments.add(current)
            CurveSeries(label = label, segments = segments, showLine = true, showPoints = false)
        }

        if (!xMin.isFinite() || !yMin.isFinite()) error("No real points in ParametricPlot")
        if (abs(xMax - xMin) < 1e-9) { xMin -= 1.0; xMax += 1.0 }
        if (abs(yMax - yMin) < 1e-9) { yMin -= 1.0; yMax += 1.0 }

        return MathChartData.Cartesian2D(
            title = "ParametricPlot[${series.joinToString(", ") { it.label }}]",
            summary = "${domain.varName} ∈ [${MathStatsEngine.fmt(domain.min)}, ${MathStatsEngine.fmt(domain.max)}] • x ∈ [${MathStatsEngine.fmt(xMin)}, ${MathStatsEngine.fmt(xMax)}] • y ∈ [${MathStatsEngine.fmt(yMin)}, ${MathStatsEngine.fmt(yMax)}]",
            badge = "Parametric",
            series = series,
            xMin = xMin,
            xMax = xMax,
            yMin = yMin,
            yMax = yMax,
            isAspectEqual = true
        )
    }

    private fun evalPolarPlot(
        args: List<MathNode>,
        ctx: ExecutionContext
    ): MathChartData.Cartesian2D {
        if (args.isEmpty()) error("PolarPlot requires arguments")
        val first = args[0]
        val rNodes = if (first is MathNode.ListNode) first.items.ifEmpty { listOf(first) } else listOf(first)
        val inferredVar = inferDefaultVariable(first, ctx)
        val domain = parseDomainSpec(args.getOrNull(1), inferredVar, 0.0, 2.0 * PI, ctx)
        val sampleCount = 240

        var xMin = Double.POSITIVE_INFINITY
        var xMax = Double.NEGATIVE_INFINITY
        var yMin = Double.POSITIVE_INFINITY
        var yMax = Double.NEGATIVE_INFINITY

        val series = rNodes.map { rNode ->
            val label = "r = ${nodeToLabel(rNode)}"
            val usesVar = domain.varName in findFreeVariables(rNode, ctx)
            val segments = mutableListOf<List<ChartPoint>>()
            var current = mutableListOf<ChartPoint>()
            for (i in 0 until sampleCount) {
                val theta = domain.min + (i.toDouble() / (sampleCount - 1)) * (domain.max - domain.min)
                val r = evalScalarAt(rNode, domain.varName, theta, usesVar, ctx)
                val x = r * cos(theta)
                val y = r * sin(theta)
                if (x.isFinite() && y.isFinite()) {
                    if (x < xMin) xMin = x
                    if (x > xMax) xMax = x
                    if (y < yMin) yMin = y
                    if (y > yMax) yMax = y
                    current.add(ChartPoint(x, y))
                } else if (current.isNotEmpty()) {
                    segments.add(current)
                    current = mutableListOf()
                }
            }
            if (current.isNotEmpty()) segments.add(current)
            CurveSeries(label = label, segments = segments, showLine = true, showPoints = false)
        }

        if (!xMin.isFinite() || !yMin.isFinite()) error("No real points in PolarPlot")
        if (abs(xMax - xMin) < 1e-9) { xMin -= 1.0; xMax += 1.0 }
        if (abs(yMax - yMin) < 1e-9) { yMin -= 1.0; yMax += 1.0 }

        return MathChartData.Cartesian2D(
            title = "PolarPlot[${rNodes.joinToString(", ") { nodeToLabel(it) }}]",
            summary = "${domain.varName} ∈ [${MathStatsEngine.fmt(domain.min)}, ${MathStatsEngine.fmt(domain.max)}]",
            badge = "PolarPlot",
            series = series,
            xMin = xMin,
            xMax = xMax,
            yMin = yMin,
            yMax = yMax,
            isAspectEqual = true
        )
    }

    // =========================================================================
    // 3. Data Plots: ListPlot, ListLinePlot, ListLogPlot, ListLogLinearPlot, ListLogLogPlot
    // =========================================================================

    private fun extractDataSeriesFromNode(node: MathNode, ctx: ExecutionContext): List<Pair<String, List<ChartPoint>>> {
        if (node is MathNode.ListNode) {
            val childNodes = node.items
            if (childNodes.isEmpty()) return emptyList()

            // Check if all children areListNode or evaluate to ListVal
            val allSublists = childNodes.all { it is MathNode.ListNode }
            if (allSublists) {
                val subLists = childNodes.map { it as MathNode.ListNode }
                // Case A: list of {x, y} pairs: {{x1, y1}, {x2, y2}, ...}
                val isListOfPairs = subLists.all { it.items.size == 2 && it.items.none { inner -> inner is MathNode.ListNode } }
                if (isListOfPairs) {
                    val pts = subLists.mapNotNull { pair ->
                        val x = pair.items[0].eval(ctx).asDouble()
                        val y = pair.items[1].eval(ctx).asDouble()
                        if (x.isFinite() && y.isFinite()) ChartPoint(x, y) else null
                    }
                    return listOf("Data" to pts)
                }

                // Case B: multiple series (each sublist is either a list of pairs or a list of y-values)
                return subLists.mapIndexedNotNull { idx, sub ->
                    val nested = extractDataSeriesFromNode(sub, ctx)
                    val pts = nested.firstOrNull()?.second.orEmpty()
                    if (pts.isEmpty()) null else "Series ${idx + 1}" to pts
                }
            }
        }

        // Case C: Evaluated MathValue (supports {y1, y2, ...}, {{x, y}, ...}, and multi-series MixedListVal)
        val evaluated = node.eval(ctx)
        if (evaluated is MathValue.MixedListVal) {
            return extractSeriesFromMixedList(evaluated, nodeToLabel(node))
        }
        val list = evaluated.asList()
        val pts = list.mapIndexedNotNull { idx, y ->
            if (y.isFinite()) ChartPoint((idx + 1).toDouble(), y) else null
        }
        return if (pts.isEmpty()) emptyList() else listOf(nodeToLabel(node) to pts)
    }

    private fun asXYChartPoint(v: MathValue): ChartPoint? = when (v) {
        is MathValue.ListVal -> {
            if (v.elements.size == 2 && v.elements[0].isFinite() && v.elements[1].isFinite()) {
                ChartPoint(v.elements[0], v.elements[1])
            } else {
                null
            }
        }
        is MathValue.MixedListVal -> {
            if (v.items.size == 2) {
                val x = v.items[0].asDouble()
                val y = v.items[1].asDouble()
                if (x.isFinite() && y.isFinite()) ChartPoint(x, y) else null
            } else {
                null
            }
        }
        else -> null
    }

    private fun extractSeriesFromMixedList(
        ml: MathValue.MixedListVal,
        label: String
    ): List<Pair<String, List<ChartPoint>>> {
        if (ml.items.isEmpty()) return emptyList()
        val pairPts = ml.items.map(::asXYChartPoint)
        if (pairPts.all { it != null }) {
            return listOf("Data" to pairPts.filterNotNull())
        }
        if (ml.items.all { it is MathValue.ListVal || it is MathValue.MixedListVal }) {
            return ml.items.mapIndexedNotNull { idx, item ->
                val pts = when (item) {
                    is MathValue.ListVal -> item.elements.mapIndexedNotNull { i, y ->
                        if (y.isFinite()) ChartPoint((i + 1).toDouble(), y) else null
                    }
                    is MathValue.MixedListVal -> extractSeriesFromMixedList(item, "Series ${idx + 1}")
                        .firstOrNull()?.second.orEmpty()
                    else -> emptyList()
                }
                if (pts.isEmpty()) null else "Series ${idx + 1}" to pts
            }
        }
        val scalarPts = ml.items.mapIndexedNotNull { idx, item ->
            val y = item.asDouble()
            if (y.isFinite()) ChartPoint((idx + 1).toDouble(), y) else null
        }
        return if (scalarPts.isEmpty()) emptyList() else listOf(label to scalarPts)
    }

    private fun evalListPlot(
        plotName: String,
        args: List<MathNode>,
        ctx: ExecutionContext,
        xScale: AxisScale,
        yScale: AxisScale,
        defaultJoined: Boolean
    ): MathChartData.Cartesian2D {
        if (args.isEmpty()) error("$plotName requires data")
        val rawSeries = extractDataSeriesFromNode(args[0], ctx)
        if (rawSeries.isEmpty()) error("Empty dataset in $plotName")

        var joined = defaultJoined
        for (optNode in args.drop(1)) {
            val optVal = runCatching { optNode.eval(ctx) }.getOrNull()
            if (optVal is MathValue.RuleVal && optVal.lhs.equals("joined", ignoreCase = true)) {
                joined = optVal.rhs.asBoolean()
            } else if (optVal is MathValue.BooleanVal) {
                joined = optVal.value
            }
        }

        var xMin = Double.POSITIVE_INFINITY
        var xMax = Double.NEGATIVE_INFINITY
        var yMin = Double.POSITIVE_INFINITY
        var yMax = Double.NEGATIVE_INFINITY
        var totalPoints = 0

        val transformedSeries = rawSeries.mapNotNull { (label, pts) ->
            val segments = mutableListOf<List<ChartPoint>>()
            var current = mutableListOf<ChartPoint>()
            for (pt in pts) {
                val xPlot = if (xScale == AxisScale.LOG10) {
                    if (pt.x > 0.0) log10(pt.x) else Double.NaN
                } else pt.x
                val yPlot = if (yScale == AxisScale.LOG10) {
                    if (pt.y > 0.0) log10(pt.y) else Double.NaN
                } else pt.y

                if (xPlot.isFinite() && yPlot.isFinite()) {
                    if (xPlot < xMin) xMin = xPlot
                    if (xPlot > xMax) xMax = xPlot
                    if (yPlot < yMin) yMin = yPlot
                    if (yPlot > yMax) yMax = yPlot
                    current.add(ChartPoint(xPlot, yPlot))
                    totalPoints++
                } else if (current.isNotEmpty()) {
                    segments.add(current)
                    current = mutableListOf()
                }
            }
            if (current.isNotEmpty()) segments.add(current)
            if (segments.isEmpty()) null
            else CurveSeries(
                label = label,
                segments = segments,
                showLine = joined,
                showPoints = !joined || pts.size <= 32
            )
        }

        if (transformedSeries.isEmpty() || !xMin.isFinite() || !yMin.isFinite()) {
            error("No valid points for $plotName")
        }
        if (abs(xMax - xMin) < 1e-9) { xMin -= 1.0; xMax += 1.0 }
        if (abs(yMax - yMin) < 1e-9) { yMin -= 1.0; yMax += 1.0 }

        val yDispMin = if (yScale == AxisScale.LOG10) 10.0.pow(yMin) else yMin
        val yDispMax = if (yScale == AxisScale.LOG10) 10.0.pow(yMax) else yMax

        return MathChartData.Cartesian2D(
            title = "$plotName ($totalPoints pts)",
            summary = "y ∈ [${MathStatsEngine.fmt(yDispMin)}, ${MathStatsEngine.fmt(yDispMax)}]",
            badge = plotName,
            series = transformedSeries,
            xMin = xMin,
            xMax = xMax,
            yMin = yMin,
            yMax = yMax,
            xScale = xScale,
            yScale = yScale
        )
    }

    // =========================================================================
    // 4. Histogram
    // =========================================================================

    private fun evalHistogram(
        args: List<MathNode>,
        ctx: ExecutionContext
    ): MathChartData.HistogramChart {
        if (args.isEmpty()) error("Histogram requires data")
        val firstVal = args[0].eval(ctx)
        val rawData: List<Double> = when (firstVal) {
            is MathValue.DistVal -> (0 until 600).map { firstVal.dist.randomVariate(MathStatsEngine.rng) }
            else -> firstVal.asList()
        }.filter { it.isFinite() }

        if (rawData.isEmpty()) error("Histogram requires non-empty numeric data")
        val sorted = rawData.sorted()
        val n = sorted.size
        var minVal = sorted.first()
        var maxVal = sorted.last()
        if (abs(maxVal - minVal) < 1e-9) {
            minVal -= 0.5
            maxVal += 0.5
        }

        // Bin specification: optional 2nd arg (number of bins, or {xmin, xmax, dx})
        var binMin = minVal
        var binMax = maxVal
        var binCount: Int

        val secondVal = args.getOrNull(1)?.eval(ctx)
        if (secondVal is MathValue.ListVal && secondVal.elements.size >= 3) {
            val a = secondVal.elements[0]
            val b = secondVal.elements[1]
            val dx = secondVal.elements[2]
            if (a.isFinite() && b.isFinite() && dx.isFinite() && b > a && dx > 0.0) {
                binMin = a
                binMax = b
                binCount = ceil((b - a) / dx).toInt().coerceIn(1, 60)
            } else {
                binCount = autoHistogramBins(sorted)
            }
        } else if (secondVal is MathValue.NumberVal && secondVal.value.isFinite() && secondVal.value >= 1.0) {
            binCount = secondVal.value.toInt().coerceIn(2, 60)
        } else {
            binCount = autoHistogramBins(sorted)
        }

        // Height specification: optional 3rd arg ("Count", "Probability", "PDF")
        val thirdNode = args.getOrNull(2)
        val hSpecRaw = when (thirdNode) {
            is MathNode.IdentNode -> thirdNode.name
            null -> "Count"
            else -> thirdNode.eval(ctx).toDisplayString()
        }.trim().removeSurrounding("\"").lowercase(Locale.ROOT)

        val modeLabel = when (hSpecRaw) {
            "pdf", "density" -> "PDF"
            "probability", "prob", "fraction" -> "Probability"
            else -> "Count"
        }

        val span = (binMax - binMin).coerceAtLeast(1e-9)
        val binWidth = span / binCount
        val counts = IntArray(binCount)
        for (v in sorted) {
            if (v < binMin || v > binMax) continue
            val idx = ((v - binMin) / binWidth).toInt().coerceIn(0, binCount - 1)
            counts[idx]++
        }

        val bins = (0 until binCount).map { i ->
            val low = binMin + i * binWidth
            val high = low + binWidth
            val c = counts[i]
            val h = when (modeLabel) {
                "PDF" -> c.toDouble() / (n * binWidth)
                "Probability" -> c.toDouble() / n
                else -> c.toDouble()
            }
            MathChartData.HistogramBin(xLow = low, xHigh = high, height = h, count = c)
        }

        val yMax = (bins.maxOfOrNull { it.height } ?: 1.0).coerceAtLeast(1e-9)
        val mean = sorted.average()
        val sd = if (n >= 2) sqrt(sorted.sumOf { (it - mean).pow(2) } / (n - 1)) else 0.0

        return MathChartData.HistogramChart(
            title = "Histogram (n = $n, $binCount bins)",
            summary = "Mean = ${MathStatsEngine.fmt(mean)} • SD = ${MathStatsEngine.fmt(sd)} • Mode: $modeLabel",
            bins = bins,
            xMin = binMin,
            xMax = binMax,
            yMax = yMax,
            modeLabel = modeLabel,
            sampleSize = n,
            mean = mean,
            sd = sd
        )
    }

    private fun autoHistogramBins(sorted: List<Double>): Int {
        val n = sorted.size
        if (n <= 4) return 3
        val sturges = ceil(log2(n.toDouble()) + 1.0).toInt()
        val q25 = percentileSorted(sorted, 0.25)
        val q75 = percentileSorted(sorted, 0.75)
        val iqr = q75 - q25
        val span = sorted.last() - sorted.first()
        val fd = if (iqr > 1e-9 && span > 0.0) {
            val h = 2.0 * iqr * n.toDouble().pow(-1.0 / 3.0)
            if (h > 0.0) ceil(span / h).toInt() else sturges
        } else sturges
        return maxOf(sturges, minOf(fd, 24)).coerceIn(5, 28)
    }

    // =========================================================================
    // 5. BarChart
    // =========================================================================

    private fun evalBarChart(
        args: List<MathNode>,
        ctx: ExecutionContext
    ): MathChartData.BarChartData {
        if (args.isEmpty()) error("BarChart requires data")
        val first = args[0]
        val groups = mutableListOf<MathChartData.BarGroup>()

        if (first is MathNode.ListNode) {
            val items = first.items
            if (items.isNotEmpty() && items.all { it is MathNode.ListNode }) {
                items.forEachIndexed { idx, sub ->
                    val vals = (sub as MathNode.ListNode).eval(ctx).asList().filter { it.isFinite() }
                    if (vals.isNotEmpty()) {
                        groups.add(MathChartData.BarGroup(label = "${idx + 1}", values = vals))
                    }
                }
            } else {
                items.forEachIndexed { idx, itemNode ->
                    when (val v = itemNode.eval(ctx)) {
                        is MathValue.RuleVal -> {
                            val nums = v.rhs.asList().filter { it.isFinite() }
                            if (nums.isNotEmpty()) {
                                groups.add(MathChartData.BarGroup(label = v.lhs, values = nums))
                            }
                        }
                        is MathValue.ListVal -> {
                            val nums = v.elements.filter { it.isFinite() }
                            if (nums.isNotEmpty()) {
                                groups.add(MathChartData.BarGroup(label = "${idx + 1}", values = nums))
                            }
                        }
                        else -> {
                            val d = v.asDouble()
                            if (d.isFinite()) {
                                groups.add(MathChartData.BarGroup(label = "${idx + 1}", values = listOf(d)))
                            }
                        }
                    }
                }
            }
        } else {
            val list = first.eval(ctx).asList().filter { it.isFinite() }
            list.forEachIndexed { idx, d ->
                groups.add(MathChartData.BarGroup(label = "${idx + 1}", values = listOf(d)))
            }
        }

        if (groups.isEmpty()) error("No valid bars in BarChart")
        val allValues = groups.flatMap { it.values }
        val minVal = minOf(0.0, allValues.minOrNull() ?: 0.0)
        val maxVal = maxOf(0.0, allValues.maxOrNull() ?: 1.0)
        val total = allValues.sum()
        val mean = allValues.average()

        return MathChartData.BarChartData(
            title = "BarChart (${groups.size} items)",
            summary = "Total = ${MathStatsEngine.fmt(total)} • Mean = ${MathStatsEngine.fmt(mean)} • Range [${MathStatsEngine.fmt(allValues.minOrNull() ?: 0.0)}, ${MathStatsEngine.fmt(allValues.maxOrNull() ?: 0.0)}]",
            groups = groups,
            yMin = minVal,
            yMax = if (maxVal == minVal) minVal + 1.0 else maxVal
        )
    }

    // =========================================================================
    // 6. PieChart
    // =========================================================================

    private fun evalPieChart(
        args: List<MathNode>,
        ctx: ExecutionContext
    ): MathChartData.PieChartData {
        if (args.isEmpty()) error("PieChart requires data")
        val first = args[0]
        val rawSlices = mutableListOf<Pair<String, Double>>()

        if (first is MathNode.ListNode) {
            first.items.forEachIndexed { idx, itemNode ->
                when (val v = itemNode.eval(ctx)) {
                    is MathValue.RuleVal -> {
                        val d = v.rhs.asDouble()
                        if (d.isFinite() && d > 0.0) rawSlices.add(v.lhs to d)
                    }
                    else -> {
                        val d = v.asDouble()
                        if (d.isFinite() && d > 0.0) rawSlices.add("#${idx + 1}" to d)
                    }
                }
            }
        } else {
            val list = first.eval(ctx).asList()
            list.forEachIndexed { idx, d ->
                if (d.isFinite() && d > 0.0) rawSlices.add("#${idx + 1}" to d)
            }
        }

        if (rawSlices.isEmpty()) error("PieChart requires positive numeric values")
        val total = rawSlices.sumOf { it.second }
        val slices = rawSlices.map { (label, value) ->
            MathChartData.PieSlice(label = label, value = value, fraction = value / total)
        }
        val topSlice = slices.maxByOrNull { it.value }
        val topDesc = if (topSlice != null) "Largest: ${topSlice.label} (${String.format(Locale.US, "%.1f%%", topSlice.fraction * 100.0)})" else ""

        return MathChartData.PieChartData(
            title = "PieChart (${slices.size} sectors)",
            summary = "Total = ${MathStatsEngine.fmt(total)} • $topDesc",
            slices = slices,
            total = total
        )
    }

    // =========================================================================
    // 7. BoxWhiskerChart
    // =========================================================================

    private fun evalBoxWhiskerChart(
        args: List<MathNode>,
        ctx: ExecutionContext
    ): MathChartData.BoxWhiskerData {
        if (args.isEmpty()) error("BoxWhiskerChart requires data")
        val first = args[0]
        val datasets = mutableListOf<Pair<String, List<Double>>>()

        if (first is MathNode.ListNode && first.items.isNotEmpty() &&
            first.items.all { it is MathNode.ListNode || it is MathNode.RuleNode }
        ) {
            first.items.forEachIndexed { idx, itemNode ->
                when (val v = itemNode.eval(ctx)) {
                    is MathValue.RuleVal -> {
                        val nums = v.rhs.asList().filter { it.isFinite() }
                        if (nums.isNotEmpty()) datasets.add(v.lhs to nums)
                    }
                    else -> {
                        val nums = v.asList().filter { it.isFinite() }
                        if (nums.isNotEmpty()) datasets.add("Set ${idx + 1}" to nums)
                    }
                }
            }
        } else {
            val v = first.eval(ctx)
            val nums = when (v) {
                is MathValue.DistVal -> (0 until 500).map { v.dist.randomVariate(MathStatsEngine.rng) }
                else -> v.asList()
            }.filter { it.isFinite() }
            if (nums.isNotEmpty()) {
                datasets.add("Data" to nums)
            }
        }

        if (datasets.isEmpty()) error("BoxWhiskerChart requires non-empty numeric data")

        var globalMin = Double.POSITIVE_INFINITY
        var globalMax = Double.NEGATIVE_INFINITY

        val boxes = datasets.map { (label, data) ->
            val sorted = data.sorted()
            val minAll = sorted.first()
            val maxAll = sorted.last()
            if (minAll < globalMin) globalMin = minAll
            if (maxAll > globalMax) globalMax = maxAll

            val q1 = percentileSorted(sorted, 0.25)
            val median = percentileSorted(sorted, 0.50)
            val q3 = percentileSorted(sorted, 0.75)
            val iqr = q3 - q1
            val lowFence = q1 - 1.5 * iqr
            val highFence = q3 + 1.5 * iqr
            val whiskerMin = sorted.firstOrNull { it >= lowFence } ?: minAll
            val whiskerMax = sorted.lastOrNull { it <= highFence } ?: maxAll
            val outliers = sorted.filter { it < lowFence || it > highFence }.take(40)

            MathChartData.BoxWhiskerSummary(
                label = label,
                min = whiskerMin,
                q1 = q1,
                median = median,
                q3 = q3,
                max = whiskerMax,
                outliers = outliers
            )
        }

        if (abs(globalMax - globalMin) < 1e-9) {
            globalMin -= 1.0
            globalMax += 1.0
        }
        val firstBox = boxes.first()
        return MathChartData.BoxWhiskerData(
            title = "BoxWhiskerChart (${boxes.size} ${if (boxes.size == 1) "series" else "series"})",
            summary = "Median = ${MathStatsEngine.fmt(firstBox.median)} • Q1 = ${MathStatsEngine.fmt(firstBox.q1)} • Q3 = ${MathStatsEngine.fmt(firstBox.q3)} • IQR = ${MathStatsEngine.fmt(firstBox.q3 - firstBox.q1)}",
            boxes = boxes,
            globalMin = globalMin,
            globalMax = globalMax
        )
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private fun percentileSorted(sorted: List<Double>, q: Double): Double {
        if (sorted.isEmpty()) return Double.NaN
        val clamped = q.coerceIn(0.0, 1.0)
        val pos = clamped * (sorted.size - 1)
        val low = floor(pos).toInt().coerceIn(0, sorted.lastIndex)
        val high = ceil(pos).toInt().coerceIn(0, sorted.lastIndex)
        val w = pos - low
        return sorted[low] * (1.0 - w) + sorted[high] * w
    }

    internal fun nodeToLabel(node: MathNode): String = when (node) {
        is MathNode.NumberNode -> MathStatsEngine.fmt(node.value)
        is MathNode.BooleanNode -> if (node.value) "True" else "False"
        is MathNode.IdentNode -> node.name
        is MathNode.SlotNode -> if (node.index == 1) "#" else "#${node.index}"
        is MathNode.OutNode -> "%"
        is MathNode.UnaryOpNode -> "${node.op}${nodeToLabel(node.child)}"
        is MathNode.BinaryOpNode -> "${nodeToLabel(node.left)}${node.op}${nodeToLabel(node.right)}"
        is MathNode.CallNode -> "${node.funcName}[${node.args.joinToString(", ") { nodeToLabel(it) }}]"
        is MathNode.ListNode -> "{${node.items.joinToString(", ") { nodeToLabel(it) }}}"
        is MathNode.FunctionNode -> "${nodeToLabel(node.body)} &"
        is MathNode.PrefixNode -> "${nodeToLabel(node.func)} @ ${nodeToLabel(node.arg)}"
        is MathNode.PostfixNode -> "${nodeToLabel(node.arg)} // ${nodeToLabel(node.func)}"
        is MathNode.RuleNode -> "${nodeToLabel(node.pattern)} -> ${nodeToLabel(node.replacement)}"
        else -> "expr"
    }
}
