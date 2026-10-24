package com.pallas.launcher.domain.search

data class ManualEntry(
    val signature: String,
    val category: String,
    val description: String,
    val example: String
) {
    val family: String get() = category
}

data class MathEvalOutcome(
    val title: String,
    val primaryValue: String,
    val badge: String? = null,
    val subtitle: String,
    val copyText: String
)

sealed interface MathValue {
    data class NumberVal(val value: Double) : MathValue
    data class BooleanVal(val value: Boolean) : MathValue
    data class ListVal(val elements: List<Double>) : MathValue
    data class DistVal(val dist: MathDistribution) : MathValue
    data class SymbolVal(val name: String) : MathValue
    data class ExprVal(val text: String) : MathValue
    object NullVal : MathValue
    data class FunctionVal(val body: MathNode, val paramNames: List<String> = emptyList()) : MathValue
    data class RuleVal(val lhs: String, val rhs: MathValue) : MathValue

    fun asDouble(): Double = when (this) {
        is NumberVal -> value
        is BooleanVal -> if (value) 1.0 else 0.0
        is ListVal -> if (elements.size == 1) elements[0] else Double.NaN
        is DistVal, is SymbolVal, is ExprVal, is NullVal, is FunctionVal, is RuleVal -> Double.NaN
    }

    fun asBoolean(): Boolean = when (this) {
        is BooleanVal -> value
        is NumberVal -> value != 0.0 && !value.isNaN()
        is ListVal -> elements.isNotEmpty()
        is SymbolVal -> name.equals("true", ignoreCase = true)
        is NullVal -> false
        else -> true
    }

    fun asList(): List<Double> = when (this) {
        is ListVal -> elements
        is NumberVal -> listOf(value)
        is BooleanVal -> listOf(if (value) 1.0 else 0.0)
        is DistVal, is SymbolVal, is ExprVal, is NullVal, is FunctionVal, is RuleVal -> emptyList()
    }

    fun toDisplayString(): String = when (this) {
        is NumberVal -> MathStatsEngine.fmt(value)
        is BooleanVal -> if (value) "True" else "False"
        is ListVal -> if (elements.isEmpty()) "{}" else elements.joinToString(", ", prefix = "{", postfix = "}") { MathStatsEngine.fmt(it) }
        is DistVal -> "${dist.name}[${dist.paramsString}]"
        is SymbolVal -> name
        is ExprVal -> text
        is NullVal -> "Null"
        is FunctionVal -> "Function[...]"
        is RuleVal -> "$lhs -> ${rhs.toDisplayString()}"
    }
}
