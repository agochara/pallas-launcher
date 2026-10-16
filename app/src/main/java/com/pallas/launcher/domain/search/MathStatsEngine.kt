package com.pallas.launcher.domain.search

import java.util.Locale
import kotlin.math.*

data class MathEvalOutcome(
    val title: String,
    val primaryValue: String,
    val badge: String? = null,
    val subtitle: String,
    val copyText: String
)

object MathStatsEngine {

    data class ManualEntry(
        val signature: String,
        val family: String,
        val description: String,
        val example: String
    )

    val MANUAL_ENTRIES = listOf(
        ManualEntry("pareto(x, α, [xm=1])", "Fat-Tail (Power Law)", "Survival P(X>x) = (xm/x)^α. Infinite variance for α ≤ 2, infinite mean for α ≤ 1.", "= pareto(5, 1.5, 1)"),
        ManualEntry("levy(x, c, [μ=0])", "Extreme Fat-Tail (α=1/2)", "Lévy stable distribution. Heavy power-law tail P(X>x) ~ erfc(√(c/2(x-μ))). Infinite mean & variance.", "= levy(4, 1, 0)"),
        ManualEntry("cauchy(x, [x0=0], [γ=1])", "Fat-Tail (α=1)", "Student-t with df=1. Undefined mean and variance.", "= cauchy(3, 0, 1)"),
        ManualEntry("t(x, df)", "Power-Law Tail", "Student's t. Fat-tailed for small df; converges to Gaussian as df → ∞.", "= t(2.5, 3)"),
        ManualEntry("lognorm(x, [μ=0], [σ=1])", "Subexponential", "Borders Gaussian and power laws; spikes violently at high σ.", "= lognorm(10, 0, 1.2)"),
        ManualEntry("frechet(x, α, [s=1], [m=0])", "Extreme Value (Type II)", "Fat-tailed maximum domain of attraction. P(X≤x) = exp(-((x-m)/s)^(-α)).", "= frechet(4, 2, 1)"),
        ManualEntry("weibull(x, k, [λ=1])", "Subexponential (k < 1)", "Fatter than exponential when shape k < 1; thin-tailed when k > 1.", "= weibull(3, 0.7, 1)"),
        ManualEntry("norm(x, [μ=0], [σ=1])", "Thin-Tail (Mediocristan)", "Gaussian distribution. Rapidly decaying exponential quadratic tail.", "= norm(1.96)"),
        ManualEntry("exp(x, [λ=1]) / pois(k, λ) / binom(k, n, p)", "Classical", "Exponential, Poisson, and Binomial exact probabilities.", "= pois(3, 2.5)"),
        ManualEntry("choose(n, k) / fact(n) / ln(x)", "Combinatorics & Calc", "Standard arithmetic (+ - * / ^) and functions.", "= choose(52, 5)")
    )

    fun evaluate(rawInput: String, prefix: String): MathEvalOutcome? {
        val trimmed = rawInput.trim()
        val hasPrefix = prefix.isNotEmpty() && (trimmed.startsWith("$prefix ", ignoreCase = true) ||
                (trimmed.startsWith(prefix, ignoreCase = true) && trimmed.length > prefix.length))

        val expr = if (hasPrefix) trimmed.substring(prefix.length).trim() else trimmed
        if (expr.isEmpty()) return null

        if (!hasPrefix && !looksLikeMathOrDist(expr)) return null

        return tryParseDistribution(expr) ?: tryEvaluateArithmetic(expr)
    }

    private val DIST_NAMES = setOf(
        "pareto", "levy", "cauchy", "t", "student", "lognorm", "lnorm",
        "frechet", "weibull", "norm", "normal", "exp", "pois", "poisson", "binom"
    )

    private fun looksLikeMathOrDist(s: String): Boolean {
        val lower = s.lowercase(Locale.ROOT)
        if (DIST_NAMES.any { lower.startsWith("$it(") }) return true
        val hasOp = s.any { it in "+-*/^%" }
        val hasDigit = s.any { it.isDigit() }
        val startsFunc = listOf("sin(", "cos(", "tan(", "ln(", "log(", "sqrt(", "choose(", "fact(", "abs(")
            .any { lower.startsWith(it) }
        return (hasDigit && hasOp && s.none { it.isLetter() && it !in "eEpiPI" }) || startsFunc
    }

    /** Splits comma-separated arguments only at parenthesis depth 0. */
    private fun splitTopLevelArgs(argsStr: String): List<String>? {
        val result = mutableListOf<String>()
        var depth = 0
        var start = 0
        for (i in argsStr.indices) {
            when (argsStr[i]) {
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth < 0) return null
                }
                ',' -> if (depth == 0) {
                    result.add(argsStr.substring(start, i).trim())
                    start = i + 1
                }
            }
        }
        if (depth != 0) return null
        result.add(argsStr.substring(start).trim())
        return result.filter { it.isNotEmpty() }
    }

    private fun tryParseDistribution(expr: String): MathEvalOutcome? {
        val openIdx = expr.indexOf('(')
        if (openIdx <= 0 || !expr.endsWith(')')) return null
        val fn = expr.substring(0, openIdx).trim().lowercase(Locale.ROOT)
        if (fn !in DIST_NAMES) return null

        val argsStr = expr.substring(openIdx + 1, expr.length - 1)
        val rawArgs = splitTopLevelArgs(argsStr) ?: return null
        if (rawArgs.isEmpty()) return null

        val args = rawArgs.map {
            val v = Parser(it).parse()
            if (v.isNaN()) return null
            v
        }

        return when (fn) {
            "pareto" -> {
                if (args.size < 2) return null
                val x = args[0]
                val alpha = args[1]
                val xm = args.getOrElse(2) { 1.0 }
                if (alpha <= 0 || xm <= 0) return null
                val survival = if (x < xm) 1.0 else (xm / x).pow(alpha)
                val cdf = 1.0 - survival
                val pdf = if (x < xm) 0.0 else (alpha * xm.pow(alpha)) / x.pow(alpha + 1.0)
                val moments = when {
                    alpha <= 1.0 -> "E[X] = ∞, Var = ∞"
                    alpha <= 2.0 -> "E[X] = ${fmt(alpha * xm / (alpha - 1.0))}, Var = ∞"
                    else -> {
                        val mean = alpha * xm / (alpha - 1.0)
                        val v = (xm * xm * alpha) / ((alpha - 1.0).pow(2) * (alpha - 2.0))
                        "E[X]=${fmt(mean)}, SD=${fmt(sqrt(v))}"
                    }
                }
                MathEvalOutcome(
                    title = "Pareto(α=${fmt(alpha)}, xm=${fmt(xm)}) at x=${fmt(x)}",
                    primaryValue = "P(X > x) = ${fmtProb(survival)}",
                    badge = if (alpha <= 2.0) "Fat Tail (α≤2)" else "Power Law",
                    subtitle = "CDF: ${fmtProb(cdf)} • PDF: ${fmt(pdf)} • $moments",
                    copyText = survival.toString()
                )
            }
            "levy" -> {
                if (args.size < 2) return null
                val x = args[0]
                val c = args[1]
                val mu = args.getOrElse(2) { 0.0 }
                if (c <= 0 || x <= mu) return null
                val z = sqrt(c / (2.0 * (x - mu)))
                val cdf = erfc(z)
                val survival = 1.0 - cdf
                val pdf = sqrt(c / (2.0 * PI)) * exp(-c / (2.0 * (x - mu))) / (x - mu).pow(1.5)
                MathEvalOutcome(
                    title = "Lévy(c=${fmt(c)}, μ=${fmt(mu)}) at x=${fmt(x)}",
                    primaryValue = "P(X > x) = ${fmtProb(survival)}",
                    badge = "Stable α=0.5",
                    subtitle = "CDF: ${fmtProb(cdf)} • PDF: ${fmt(pdf)} • E[X]=∞, Var=∞",
                    copyText = survival.toString()
                )
            }
            "cauchy" -> {
                val x = args[0]
                val x0 = args.getOrElse(1) { 0.0 }
                val gamma = args.getOrElse(2) { 1.0 }
                if (gamma <= 0) return null
                val z = (x - x0) / gamma
                val cdf = (1.0 / PI) * atan(z) + 0.5
                val survival = 1.0 - cdf
                val pdf = 1.0 / (PI * gamma * (1.0 + z * z))
                MathEvalOutcome(
                    title = "Cauchy(x0=${fmt(x0)}, γ=${fmt(gamma)}) at x=${fmt(x)}",
                    primaryValue = "P(X > x) = ${fmtProb(survival)}",
                    badge = "Stable α=1",
                    subtitle = "CDF: ${fmtProb(cdf)} • PDF: ${fmt(pdf)} • Mean/Var undefined",
                    copyText = survival.toString()
                )
            }
            "t", "student" -> {
                if (args.size < 2) return null
                val x = args[0]
                val df = args[1]
                if (df <= 0) return null
                val cdf = studentTCdf(x, df)
                val survival = 1.0 - cdf
                val pdf = exp(lnGamma((df + 1.0) / 2.0) - lnGamma(df / 2.0)) /
                        sqrt(df * PI) * (1.0 + (x * x) / df).pow(-(df + 1.0) / 2.0)
                val twoSided = 2.0 * minOf(cdf, survival)
                MathEvalOutcome(
                    title = "Student-t(df=${fmt(df)}) at t=${fmt(x)}",
                    primaryValue = "P(T > t) = ${fmtProb(survival)}",
                    badge = "2-tail p=${fmtProb(twoSided)}",
                    subtitle = "CDF: ${fmtProb(cdf)} • PDF: ${fmt(pdf)}",
                    copyText = survival.toString()
                )
            }
            "lognorm", "lnorm" -> {
                val x = args[0]
                val mu = args.getOrElse(1) { 0.0 }
                val sigma = args.getOrElse(2) { 1.0 }
                if (x <= 0 || sigma <= 0) return null
                val z = (ln(x) - mu) / sigma
                val cdf = normalCdf(z)
                val survival = 1.0 - cdf
                val pdf = exp(-0.5 * z * z) / (x * sigma * sqrt(2.0 * PI))
                val median = exp(mu)
                val mean = exp(mu + 0.5 * sigma * sigma)
                MathEvalOutcome(
                    title = "LogNormal(μ=${fmt(mu)}, σ=${fmt(sigma)}) at x=${fmt(x)}",
                    primaryValue = "P(X > x) = ${fmtProb(survival)}",
                    badge = "Mean/Med=${fmt(mean / median)}x",
                    subtitle = "CDF: ${fmtProb(cdf)} • PDF: ${fmt(pdf)} • E[X]=${fmt(mean)}",
                    copyText = survival.toString()
                )
            }
            "frechet" -> {
                if (args.size < 2) return null
                val x = args[0]
                val alpha = args[1]
                val s = args.getOrElse(2) { 1.0 }
                val m = args.getOrElse(3) { 0.0 }
                if (alpha <= 0 || s <= 0 || x <= m) return null
                val z = (x - m) / s
                val cdf = exp(-z.pow(-alpha))
                val survival = 1.0 - cdf
                val pdf = (alpha / s) * z.pow(-1.0 - alpha) * cdf
                MathEvalOutcome(
                    title = "Fréchet(α=${fmt(alpha)}, s=${fmt(s)}) at x=${fmt(x)}",
                    primaryValue = "P(X > x) = ${fmtProb(survival)}",
                    badge = "EVD Type II",
                    subtitle = "CDF: ${fmtProb(cdf)} • PDF: ${fmt(pdf)}",
                    copyText = survival.toString()
                )
            }
            "weibull" -> {
                if (args.size < 2) return null
                val x = args[0]
                val k = args[1]
                val lam = args.getOrElse(2) { 1.0 }
                if (x < 0 || k <= 0 || lam <= 0) return null
                val survival = exp(-(x / lam).pow(k))
                val cdf = 1.0 - survival
                val pdf = (k / lam) * (x / lam).pow(k - 1.0) * survival
                MathEvalOutcome(
                    title = "Weibull(k=${fmt(k)}, λ=${fmt(lam)}) at x=${fmt(x)}",
                    primaryValue = "P(X > x) = ${fmtProb(survival)}",
                    badge = if (k < 1.0) "Subexponential" else "Thin Tail",
                    subtitle = "CDF: ${fmtProb(cdf)} • PDF: ${fmt(pdf)}",
                    copyText = survival.toString()
                )
            }
            "norm", "normal" -> {
                val x = args[0]
                val mu = args.getOrElse(1) { 0.0 }
                val sigma = args.getOrElse(2) { 1.0 }
                if (sigma <= 0) return null
                val z = (x - mu) / sigma
                val cdf = normalCdf(z)
                val survival = normalCdf(-z)
                val pdf = exp(-0.5 * z * z) / (sigma * sqrt(2.0 * PI))
                MathEvalOutcome(
                    title = "Normal(μ=${fmt(mu)}, σ=${fmt(sigma)}) at x=${fmt(x)}",
                    primaryValue = "Φ(x) = ${fmtProb(cdf)}",
                    badge = "z = ${fmt(z)}",
                    subtitle = "Upper Tail P(X > x): ${fmtProb(survival)} • PDF: ${fmt(pdf)}",
                    copyText = cdf.toString()
                )
            }
            "exp" -> {
                val x = args[0]
                val lam = args.getOrElse(1) { 1.0 }
                if (x < 0 || lam <= 0) return null
                val survival = exp(-lam * x)
                val cdf = 1.0 - survival
                val pdf = lam * survival
                MathEvalOutcome(
                    title = "Exponential(λ=${fmt(lam)}) at x=${fmt(x)}",
                    primaryValue = "P(X > x) = ${fmtProb(survival)}",
                    badge = "E[X] = ${fmt(1.0 / lam)}",
                    subtitle = "CDF: ${fmtProb(cdf)} • PDF: ${fmt(pdf)}",
                    copyText = survival.toString()
                )
            }
            "pois", "poisson" -> {
                if (args.size < 2) return null
                val k = args[0].toInt()
                val lam = args[1]
                if (k < 0 || lam <= 0) return null
                val pmf = exp(k * ln(lam) - lam - lnGamma(k + 1.0))
                var cdf = 0.0
                for (i in 0..k) cdf += exp(i * ln(lam) - lam - lnGamma(i + 1.0))
                val survival = (1.0 - cdf).coerceAtLeast(0.0)
                MathEvalOutcome(
                    title = "Poisson(λ=${fmt(lam)}) at k=$k",
                    primaryValue = "P(X = $k) = ${fmtProb(pmf)}",
                    badge = "P(X > $k) = ${fmtProb(survival)}",
                    subtitle = "CDF P(X ≤ $k): ${fmtProb(cdf)}",
                    copyText = pmf.toString()
                )
            }
            "binom" -> {
                if (args.size < 3) return null
                val k = args[0].toInt()
                val n = args[1].toInt()
                val p = args[2]
                if (k < 0 || n <= 0 || k > n || p !in 0.0..1.0) return null
                fun pmfAt(i: Int): Double = when {
                    p == 0.0 -> if (i == 0) 1.0 else 0.0
                    p == 1.0 -> if (i == n) 1.0 else 0.0
                    else -> exp(lnChoose(n, i) + i * ln(p) + (n - i) * ln(1.0 - p))
                }
                val pmf = pmfAt(k)
                var cdf = 0.0
                for (i in 0..k) cdf += pmfAt(i)
                MathEvalOutcome(
                    title = "Binomial(n=$n, p=${fmt(p)}) at k=$k",
                    primaryValue = "P(X = $k) = ${fmtProb(pmf)}",
                    badge = "E[X] = ${fmt(n * p)}",
                    subtitle = "CDF P(X ≤ $k): ${fmtProb(cdf.coerceAtMost(1.0))} • P(X > $k): ${fmtProb((1.0 - cdf).coerceAtLeast(0.0))}",
                    copyText = pmf.toString()
                )
            }
            else -> null
        }
    }

    private fun tryEvaluateArithmetic(expr: String): MathEvalOutcome? {
        val v = Parser(expr).parse()
        if (v.isNaN() || v.isInfinite()) return null
        if (expr.toDoubleOrNull() != null) return null
        val formatted = fmt(v)
        return MathEvalOutcome(
            title = expr,
            primaryValue = "= $formatted",
            badge = "Math",
            subtitle = "Tap to copy result • Tap (i) for fat-tail & stats functions",
            copyText = formatted
        )
    }

    private fun normalCdf(z: Double): Double = 0.5 * erfc(-z / sqrt(2.0))

    private fun erfc(x: Double): Double {
        val z = abs(x)
        val t = 1.0 / (1.0 + 0.5 * z)
        val r = t * exp(
            -z * z - 1.26551223 + t * (1.00002368 + t * (0.37409196 + t * (0.09678418 +
                    t * (-0.18628806 + t * (0.27886807 + t * (-1.13520398 + t * (1.48851587 +
                    t * (-0.82215223 + t * 0.17087277))))))))
        )
        return if (x >= 0.0) r else 2.0 - r
    }

    private fun lnGamma(xx: Double): Double {
        val cof = doubleArrayOf(
            76.18009172947146, -86.50532032941677, 24.01409824083091,
            -1.231739572450155, 0.1208650973866179e-2, -0.5395239384953e-5
        )
        var x = xx
        var y = xx
        var tmp = x + 5.5
        tmp -= (x + 0.5) * ln(tmp)
        var ser = 1.000000000190015
        for (j in 0..5) {
            y += 1.0
            ser += cof[j] / y
        }
        return -tmp + ln(2.5066282746310005 * ser / x)
    }

    private fun lnChoose(n: Int, k: Int): Double {
        if (k < 0 || k > n) return Double.NEGATIVE_INFINITY
        return lnGamma(n + 1.0) - lnGamma(k + 1.0) - lnGamma(n - k + 1.0)
    }

    private fun regIncompleteBeta(x: Double, a: Double, b: Double): Double {
        if (x <= 0.0) return 0.0
        if (x >= 1.0) return 1.0
        val bt = exp(lnGamma(a + b) - lnGamma(a) - lnGamma(b) + a * ln(x) + b * ln(1.0 - x))
        return if (x < (a + 1.0) / (a + b + 2.0)) {
            bt * betacf(x, a, b) / a
        } else {
            1.0 - bt * betacf(1.0 - x, b, a) / b
        }
    }

    private fun betacf(x: Double, a: Double, b: Double): Double {
        val fpMin = 1e-30
        val qab = a + b
        val qap = a + 1.0
        val qam = a - 1.0
        var c = 1.0
        var d = 1.0 - qab * x / qap
        if (abs(d) < fpMin) d = fpMin
        d = 1.0 / d
        var h = d
        for (m in 1..100) {
            val m2 = 2 * m
            var aa = m * (b - m) * x / ((qam + m2) * (a + m2))
            d = 1.0 + aa * d
            if (abs(d) < fpMin) d = fpMin
            c = 1.0 + aa / c
            if (abs(c) < fpMin) c = fpMin
            d = 1.0 / d
            h *= d * c
            aa = -(a + m) * (qab + m) * x / ((a + m2) * (qap + m2))
            d = 1.0 + aa * d
            if (abs(d) < fpMin) d = fpMin
            c = 1.0 + aa / c
            if (abs(c) < fpMin) c = fpMin
            d = 1.0 / d
            val del = d * c
            h *= del
            if (abs(del - 1.0) < 3e-7) break
        }
        return h
    }

    private fun studentTCdf(t: Double, df: Double): Double {
        val x = df / (df + t * t)
        val ib = 0.5 * regIncompleteBeta(x, df / 2.0, 0.5)
        return if (t >= 0.0) 1.0 - ib else ib
    }

    private fun fmt(v: Double): String {
        if (abs(v) >= 1e9 || (abs(v) > 0 && abs(v) < 1e-4)) {
            return String.format(Locale.US, "%.4e", v)
        }
        val rounded = kotlin.math.round(v)
        if (abs(v - rounded) < 1e-4) return rounded.toLong().toString()
        val l = v.toLong()
        if (abs(v - l) < 1e-9) return l.toString()
        return String.format(Locale.US, "%.5f", v).trimEnd('0').trimEnd('.')
    }

    private fun fmtProb(p: Double): String {
        val c = p.coerceIn(0.0, 1.0)
        return if (c > 0.0 && c < 1e-4) {
            String.format(Locale.US, "%.3e", c)
        } else {
            "${String.format(Locale.US, "%.4f", c).trimEnd('0').trimEnd('.')} (${String.format(Locale.US, "%.2f%%", c * 100.0)})"
        }
    }

    private class Parser(private val s: String) {
        private var pos = -1
        private var ch = 0

        private fun nextChar() {
            ch = if (++pos < s.length) s[pos].code else -1
        }

        private fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < s.length) return Double.NaN
            return x
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+'.code) -> x += parseTerm()
                    eat('-'.code) -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*'.code) -> x *= parseFactor()
                    eat('/'.code) -> x /= parseFactor()
                    eat('%'.code) -> x %= parseFactor()
                    else -> return x
                }
            }
        }

        private fun parseFactor(): Double {
            if (eat('+'.code)) return parseFactor()
            if (eat('-'.code)) return -parseFactor()
            var x: Double
            val startPos = pos
            if (eat('('.code)) {
                x = parseExpression()
                if (!eat(')'.code)) return Double.NaN
            } else if (ch in '0'.code..'9'.code || ch == '.'.code) {
                while (ch in '0'.code..'9'.code || ch == '.'.code) nextChar()
                if (ch == 'e'.code || ch == 'E'.code) {
                    nextChar()
                    if (ch == '+'.code || ch == '-'.code) nextChar()
                    while (ch in '0'.code..'9'.code) nextChar()
                }
                x = s.substring(startPos, pos).toDoubleOrNull() ?: return Double.NaN
            } else if (ch in 'a'.code..'z'.code || ch in 'A'.code..'Z'.code) {
                while (ch in 'a'.code..'z'.code || ch in 'A'.code..'Z'.code) nextChar()
                val func = s.substring(startPos, pos).lowercase(Locale.ROOT)
                if (func == "pi") x = PI
                else if (func == "e") x = E
                else if (eat('('.code)) {
                    val a = parseExpression()
                    val b = if (eat(','.code)) parseExpression() else Double.NaN
                    if (!eat(')'.code)) return Double.NaN
                    x = when (func) {
                        "sqrt" -> sqrt(a)
                        "sin" -> sin(a)
                        "cos" -> cos(a)
                        "tan" -> tan(a)
                        "ln" -> ln(a)
                        "log" -> log10(a)
                        "abs" -> abs(a)
                        "fact" -> kotlin.math.round(exp(lnGamma(a + 1.0)))
                        "choose" -> if (!b.isNaN()) kotlin.math.round(exp(lnChoose(a.toInt(), b.toInt()))) else Double.NaN
                        else -> Double.NaN
                    }
                } else return Double.NaN
            } else {
                return Double.NaN
            }
            if (eat('^'.code)) x = x.pow(parseFactor())
            return x
        }
    }
}