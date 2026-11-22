package com.pallas.launcher.domain.search

data class ManualEntry(
    val signature: String,
    val category: String,
    val description: String,
    val example: String,
    val isEnabled: Boolean = true
) {
    val family: String get() = category
}

data class MathEvalOutcome(
    val title: String,
    val primaryValue: String,
    val badge: String? = null,
    val subtitle: String,
    val copyText: String,
    val chart: MathChartData? = null,
    val consoleOutput: String? = null
)

sealed interface MathValue {
    data class NumberVal(val value: Double) : MathValue
    data class BooleanVal(val value: Boolean) : MathValue
    data class StringVal(val text: String) : MathValue
    data class ListVal(val elements: List<Double>) : MathValue
    data class MixedListVal(val items: List<MathValue>) : MathValue
    data class DistVal(val dist: MathDistribution) : MathValue
    data class SymbolVal(val name: String) : MathValue
    data class ExprVal(val text: String) : MathValue
    object NullVal : MathValue
    data class FunctionVal(val body: MathNode, val paramNames: List<String> = emptyList()) : MathValue
    data class DelayedVal(val expr: MathNode) : MathValue
    data class RuleVal(val lhs: String, val rhs: MathValue) : MathValue
    data class ChartVal(val chart: MathChartData) : MathValue

    fun asDouble(): Double = when (this) {
        is NumberVal -> value
        is BooleanVal -> if (value) 1.0 else 0.0
        is StringVal -> text.toDoubleOrNull() ?: Double.NaN
        is ListVal -> if (elements.size == 1) elements[0] else Double.NaN
        is MixedListVal -> if (items.size == 1) items[0].asDouble() else Double.NaN
        is DistVal, is SymbolVal, is ExprVal, is NullVal, is FunctionVal, is DelayedVal, is RuleVal, is ChartVal -> Double.NaN
    }

    fun asBoolean(): Boolean = when (this) {
        is BooleanVal -> value
        is NumberVal -> value != 0.0 && !value.isNaN()
        is StringVal -> text.isNotEmpty()
        is ListVal -> elements.isNotEmpty()
        is MixedListVal -> items.isNotEmpty()
        is SymbolVal -> name.equals("true", ignoreCase = true)
        is NullVal, is DelayedVal -> false
        else -> true
    }

    fun asList(): List<Double> = when (this) {
        is ListVal -> elements
        is MixedListVal -> items.flatMap { it.asList() }
        is NumberVal -> listOf(value)
        is BooleanVal -> listOf(if (value) 1.0 else 0.0)
        is StringVal, is DistVal, is SymbolVal, is ExprVal, is NullVal, is FunctionVal, is DelayedVal, is RuleVal, is ChartVal -> emptyList()
    }

    fun toDisplayString(): String = when (this) {
        is NumberVal -> MathStatsEngine.fmt(value)
        is BooleanVal -> if (value) "True" else "False"
        is StringVal -> text
        is ListVal -> if (elements.isEmpty()) "{}" else elements.joinToString(", ", prefix = "{", postfix = "}") { MathStatsEngine.fmt(it) }
        is MixedListVal -> items.joinToString(", ", prefix = "{", postfix = "}") { it.toDisplayString() }
        is DistVal -> "${dist.name}[${dist.paramsString}]"
        is SymbolVal -> name
        is ExprVal -> text
        is NullVal -> "Null"
        is FunctionVal -> "Function[...]"
        is DelayedVal -> "Delayed[...]"
        is RuleVal -> "$lhs -> ${rhs.toDisplayString()}"
        is ChartVal -> chart.title
    }
}

fun MathValue.toNumericMatrix(): List<List<Double>>? {
    if (this !is MathValue.MixedListVal || items.isEmpty()) return null
    val rows = mutableListOf<List<Double>>()
    var cols = -1
    for (item in items) {
        if (item !is MathValue.ListVal || item.elements.isEmpty()) return null
        if (cols < 0) cols = item.elements.size
        else if (item.elements.size != cols) return null
        rows.add(item.elements)
    }
    return rows
}
