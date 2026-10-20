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

    fun asDouble(): Double = when (this) {
        is NumberVal -> value
        is BooleanVal -> if (value) 1.0 else 0.0
        is ListVal -> if (elements.size == 1) elements[0] else Double.NaN
        is DistVal, is SymbolVal, is ExprVal -> Double.NaN
    }

    fun asList(): List<Double> = when (this) {
        is ListVal -> elements
        is NumberVal -> listOf(value)
        is BooleanVal -> listOf(if (value) 1.0 else 0.0)
        is DistVal, is SymbolVal, is ExprVal -> emptyList()
    }
}
