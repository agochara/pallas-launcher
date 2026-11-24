package com.pallas.launcher.domain.search

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToInt

data class EmiResult(
    val title: String,
    val primaryValue: String,
    val badge: String,
    val subtitle: String,
    val bodyText: String,
    val copyText: String
)

/**
 * Pure-Kotlin Loan EMI calculator & amortization milestone summary:
 * Usage: `emi <principal> <annual_rate%> <tenure>`
 * Examples:
 * - `emi 500000 8.5 5y`
 * - `emi 50L 8.5% 20y`
 * - `emi $250k 6.2 30yr`
 * - `emi 100000 10 24m`
 */
object EmiCalculatorEngine {

    private val CURRENCY_SYMBOL_REGEX = Regex("""^[₹$€£¥]\s*""")
    private val NUMBER_UNIT_REGEX = Regex("""^([0-9]+(?:,[0-9]{2,3})*(?:\.[0-9]+)?|\d+(?:\.\d+)?)\s*([a-zA-Z%]*)$""")

    fun evaluate(rawArg: String): EmiResult? {
        val clean = rawArg.trim()
        if (clean.isEmpty()) return null

        val parts = tokenizeArguments(clean)
        if (parts.size != 3) return null

        val (currencyPrefix, principal) = parsePrincipal(parts[0]) ?: return null
        val annualRate = parseRate(parts[1]) ?: return null
        val months = parseTenureMonths(parts[2]) ?: return null
        if (principal <= 0.0 || annualRate < 0.0 || annualRate > 100.0 || months !in 1..600) return null

        val monthlyRate = annualRate / 12.0 / 100.0
        val emi = if (monthlyRate == 0.0) {
            principal / months.toDouble()
        } else {
            val factor = (1.0 + monthlyRate).pow(months.toDouble())
            principal * monthlyRate * factor / (factor - 1.0)
        }
        val totalPayment = emi * months
        val totalInterest = (totalPayment - principal).coerceAtLeast(0.0)

        val interestPct = if (totalPayment > 0.0) (totalInterest / totalPayment * 100.0).roundToInt() else 0
        val principalPct = (100 - interestPct).coerceIn(0, 100)

        val milestones = buildAmortizationMilestones(
            principal = principal,
            monthlyRate = monthlyRate,
            emi = emi,
            totalMonths = months,
            currencyPrefix = currencyPrefix
        )

        val sym = currencyPrefix.ifEmpty { "" }
        val fmtEmi = "$sym${fmtMoney(emi)}/mo"
        val fmtInterest = "$sym${fmtMoney(totalInterest)}"
        val fmtTotal = "$sym${fmtMoney(totalPayment)}"
        val fmtPrincipal = "$sym${fmtCompactMoney(principal)}"

        val tenureLabel = if (months % 12 == 0) "${months / 12}y (${months}m)" else "${months}m"
        val rateLabel = String.format(Locale.US, "%.2f", annualRate).trimEnd('0').trimEnd('.') + "%"

        return EmiResult(
            title = "EMI • $fmtPrincipal @ $rateLabel for $tenureLabel",
            primaryValue = fmtEmi,
            badge = "$principalPct% Prin / $interestPct% Int",
            subtitle = "Interest: $fmtInterest • Total Payable: $fmtTotal",
            bodyText = milestones,
            copyText = fmtMoneyPlain(emi)
        )
    }

    private fun tokenizeArguments(clean: String): List<String> {
        val rawTokens = clean.split(Regex("""\s+""")).filter { it.isNotEmpty() }
        if (rawTokens.size == 3) return rawTokens

        // Combine adjacent number + alpha unit tokens (e.g. "50 lakh 8.5 % 20 years")
        val combined = mutableListOf<String>()
        var i = 0
        while (i < rawTokens.size) {
            val cur = rawTokens[i]
            val next = rawTokens.getOrNull(i + 1)
            val isPureUnit = next != null && next.matches(Regex("""^(%|k|l|lac|lakh|lakhs|cr|crore|crores|m|mn|million|b|bn|y|yr|yrs|year|years|mo|mos|month|months)$""", RegexOption.IGNORE_CASE))
            if (isPureUnit && !cur.any { it.isLetter() && it !in listOf('₹') }) {
                combined.add(cur + next)
                i += 2
            } else {
                combined.add(cur)
                i++
            }
        }
        return combined
    }

    private fun parsePrincipal(token: String): Pair<String, Double>? {
        val symMatch = CURRENCY_SYMBOL_REGEX.find(token)
        val currency = symMatch?.value?.trim().orEmpty()
        val rest = if (symMatch != null) token.substring(symMatch.value.length) else token
        val m = NUMBER_UNIT_REGEX.matchEntire(rest) ?: return null
        val num = m.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        val mult = when (m.groupValues[2].lowercase(Locale.ROOT)) {
            "" -> 1.0
            "k" -> 1_000.0
            "l", "lac", "lakh", "lakhs" -> 100_000.0
            "m", "mn", "million" -> 1_000_000.0
            "cr", "crore", "crores" -> 10_000_000.0
            "b", "bn" -> 1_000_000_000.0
            else -> return null
        }
        return currency to (num * mult)
    }

    private fun parseRate(token: String): Double? {
        val clean = token.trim().removeSuffix("%")
        return clean.toDoubleOrNull()
    }

    private fun parseTenureMonths(token: String): Int? {
        val m = NUMBER_UNIT_REGEX.matchEntire(token.trim()) ?: return null
        val num = m.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        if (num <= 0.0) return null
        val unit = m.groupValues[2].lowercase(Locale.ROOT)
        return when (unit) {
            "y", "yr", "yrs", "year", "years" -> (num * 12.0).roundToInt()
            "m", "mo", "mos", "month", "months" -> num.roundToInt()
            "" -> if (num <= 40.0) (num * 12.0).roundToInt() else num.roundToInt()
            else -> null
        }
    }

    private fun buildAmortizationMilestones(
        principal: Double,
        monthlyRate: Double,
        emi: Double,
        totalMonths: Int,
        currencyPrefix: String
    ): String {
        val sym = currencyPrefix.ifEmpty { "" }
        val checkpointMonths = linkedSetOf<Int>()
        if (totalMonths >= 12) checkpointMonths.add(12)
        if (totalMonths >= 24) checkpointMonths.add((totalMonths / 2))
        checkpointMonths.add(totalMonths)

        var balance = principal
        var cumPrincipal = 0.0
        var cumInterest = 0.0
        val lines = mutableListOf<String>()

        for (month in 1..totalMonths) {
            val interest = balance * monthlyRate
            val paidPrincipal = if (month == totalMonths) balance else (emi - interest).coerceIn(0.0, balance)
            balance = (balance - paidPrincipal).coerceAtLeast(0.0)
            cumPrincipal += paidPrincipal
            cumInterest += interest

            if (month in checkpointMonths) {
                val label = when {
                    month == totalMonths -> "Final (M$month)"
                    month % 12 == 0 -> "Year ${month / 12}"
                    else -> "Month $month"
                }
                lines.add(
                    "$label: Prin $sym${fmtCompactMoney(cumPrincipal)} · Int $sym${fmtCompactMoney(cumInterest)} · Bal $sym${fmtCompactMoney(balance)}"
                )
            }
        }
        return lines.joinToString("\n")
    }

    private fun fmtMoney(v: Double): String {
        val nf = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return nf.format(v)
    }

    private fun fmtMoneyPlain(v: Double): String = String.format(Locale.US, "%.2f", v)

    private fun fmtCompactMoney(v: Double): String {
        val nf = NumberFormat.getIntegerInstance(Locale.US)
        return nf.format(v.roundToInt().toLong())
    }
}
