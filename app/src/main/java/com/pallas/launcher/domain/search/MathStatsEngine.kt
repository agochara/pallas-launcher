package com.pallas.launcher.domain.search

import java.util.Locale
import java.util.Random
import kotlin.math.*

object MathStatsEngine {

    val MANUAL_ENTRIES: List<ManualEntry> = listOf(
        // Arithmetic & Elementary Math
        ManualEntry("Plus[a, b, ...] / a + b", "Arithmetic & Elementary Math", "Addition of numbers or vectors.", "= 15 + 27"),
        ManualEntry("Power[a, b] / a ^ b", "Arithmetic & Elementary Math", "Raises a to the power of b.", "= 2^10"),
        ManualEntry("Sqrt[x] / CubeRoot[x]", "Arithmetic & Elementary Math", "Square root and cube root.", "= Sqrt[256]"),
        ManualEntry("Abs[x] / Sign[x]", "Arithmetic & Elementary Math", "Absolute value and signum (-1, 0, 1).", "= Abs[-42]"),
        ManualEntry("Log[b, x] / Log[x] / Ln[x]", "Arithmetic & Elementary Math", "Logarithm to base b of x (e.g. Log[2, 64]=6), natural log ln(x), or common Log10[x]. Note argument order: Log[base, x].", "= Log[2, 64]"),
        ManualEntry("Sin[x] / Cos[x] / Tan[x]", "Arithmetic & Elementary Math", "Trigonometric functions (radians; use Deg/Rad to convert).", "= Sin[Pi / 2]"),
        ManualEntry("ArcSin[x] / ArcCos[x] / ArcTan[x]", "Arithmetic & Elementary Math", "Inverse trigonometric functions.", "= ArcTan[1]"),
        ManualEntry("Round[x] / Floor[x] / Ceiling[x]", "Arithmetic & Elementary Math", "Rounding to nearest integer, floor, or ceiling.", "= Round[3.74]"),
        ManualEntry("Mod[m, d] / Quotient[m, d]", "Arithmetic & Elementary Math", "Remainder and integer quotient of m divided by d.", "= Mod[17, 5]"),
        ManualEntry("GCD[a, b, ...] / LCM[a, b, ...]", "Arithmetic & Elementary Math", "Greatest common divisor and least common multiple.", "= GCD[24, 36]"),
        ManualEntry("PrimeQ[n] / NextPrime[n]", "Arithmetic & Elementary Math", "Tests if n is prime, or finds the next prime after n.", "= PrimeQ[97]"),
        ManualEntry("Fibonacci[n]", "Arithmetic & Elementary Math", "Calculates the n-th Fibonacci number.", "= Fibonacci[12]"),
        ManualEntry("Clip[x, min, max]", "Arithmetic & Elementary Math", "Clamps value x between min and max.", "= Clip[15, 0, 10]"),
        ManualEntry("Binomial[n, k]", "Arithmetic & Elementary Math", "Binomial coefficient n choose k.", "= Binomial[52, 5]"),
        ManualEntry("Factorial[n] / n!", "Arithmetic & Elementary Math", "Factorial of non-negative integer n.", "= 6!"),

        // Lists & Vectors
        ManualEntry("{x1, x2, ...} (List)", "Lists & Vectors", "List creation with vector arithmetic.", "= {1, 2, 3} * 2"),
        ManualEntry("Range[n] / Range[min, max, [step]]", "Lists & Vectors", "Generates an arithmetic sequence as a list.", "= Range[10]"),
        ManualEntry("Total[list] / Length[list]", "Lists & Vectors", "Sum of elements and list length.", "= Total[Range[100]]"),
        ManualEntry("First[list] / Last[list]", "Lists & Vectors", "Returns first or last element of a list.", "= First[{10, 20, 30}]"),
        ManualEntry("Take[list, n] / Drop[list, n]", "Lists & Vectors", "Takes or drops the first n elements.", "= Take[Range[10], 4]"),
        ManualEntry("Sort[list] / Reverse[list]", "Lists & Vectors", "Sorts list ascending or reverses order.", "= Sort[{5, 2, 8, 1, 9}]"),
        ManualEntry("Differences[list] / Accumulate[list]", "Lists & Vectors", "Successive differences or running prefix sums.", "= Accumulate[{1, 2, 3, 4}]"),
        ManualEntry("DeleteDuplicates[list]", "Lists & Vectors", "Removes duplicates from list preserving first occurrence order.", "= DeleteDuplicates[{1, 2, 2, 3, 1, 4}]"),
        ManualEntry("Table[expr, {i, max}] / Table[expr, {i, min, max, [step]}]", "Lists & Vectors", "Generates a list by evaluating expression for values of iterator i.", "= Table[i^2, {i, 5}]"),
        ManualEntry("Fold[f, [x], list]", "Lists & Vectors", "Successive reduction of list using binary function f.", "= Fold[Plus, 0, {1, 2, 3, 4}]"),
        ManualEntry("Nest[f, expr, n]", "Lists & Vectors", "Applies function f to expression n times.", "= Nest[Sqrt, 256, 3]"),

        // Statistics
        ManualEntry("Mean[list]", "Statistics", "Calculates the arithmetic mean.", "= Mean[{2, 4, 6, 8, 10}]"),
        ManualEntry("Median[list]", "Statistics", "Calculates the median value of a list.", "= Median[{1, 3, 3, 6, 7, 8, 9}]"),
        ManualEntry("Variance[list]", "Statistics", "Calculates the sample variance (n - 1 denominator).", "= Variance[{2, 4, 4, 4, 5, 5, 7, 9}]"),
        ManualEntry("StandardDeviation[list]", "Statistics", "Calculates the sample standard deviation.", "= StandardDeviation[{2, 4, 4, 4, 5, 5, 7, 9}]"),
        ManualEntry("Quantile[list, q]", "Statistics", "Empirical quantile for quantile q in [0, 1].", "= Quantile[Range[100], 0.75]"),
        ManualEntry("InterquartileRange[list]", "Statistics", "IQR difference between Q3 (75%) and Q1 (25%).", "= InterquartileRange[{1, 2, 3, 4, 5, 6, 7, 8}]"),
        ManualEntry("GeometricMean[list]", "Statistics", "Geometric mean of positive numbers.", "= GeometricMean[{1, 2, 4, 8}]"),
        ManualEntry("HarmonicMean[list]", "Statistics", "Harmonic mean of positive numbers.", "= HarmonicMean[{1, 2, 4}]"),
        ManualEntry("RootMeanSquare[list]", "Statistics", "Root mean square (quadratic mean).", "= RootMeanSquare[{1, -2, 3}]"),
        ManualEntry("Skewness[list] / Kurtosis[list]", "Statistics", "Measures asymmetry and tail thickness of data.", "= Skewness[{1, 2, 2, 3, 4, 5, 9}]"),
        ManualEntry("Correlation[list1, list2]", "Statistics", "Pearson correlation coefficient between two lists.", "= Correlation[{1, 2, 3}, {2, 4, 6}]"),
        ManualEntry("Standardize[list]", "Statistics", "Normalizes data to mean 0 and standard deviation 1.", "= Standardize[{10, 20, 30, 40, 50}]"),
        ManualEntry("MovingAverage[list, r]", "Statistics", "Calculates the moving average of window size r.", "= MovingAverage[{1, 2, 3, 4, 5, 6}, 3]"),
        ManualEntry("MaxDrawdown[list]", "Statistics", "Maximum peak-to-trough decline in a price or value series.", "= MaxDrawdown[{100, 120, 90, 110, 80, 150}]"),

        // Probability Distributions
        ManualEntry("NormalDistribution[μ=0, σ=1]", "Distributions", "Gaussian bell curve with mean μ and standard deviation σ.", "= NormalDistribution[100, 15]"),
        ManualEntry("UniformDistribution[min=0, max=1]", "Distributions", "Continuous uniform distribution between min and max.", "= UniformDistribution[0, 10]"),
        ManualEntry("ExponentialDistribution[λ=1]", "Distributions", "Memoryless continuous decay with rate λ.", "= ExponentialDistribution[1.5]"),
        ManualEntry("PoissonDistribution[μ]", "Distributions", "Discrete count distribution with event rate μ.", "= PoissonDistribution[4]"),
        ManualEntry("BinomialDistribution[n, p]", "Distributions", "Number of successes in n independent Bernoulli trials with probability p.", "= BinomialDistribution[20, 0.5]"),
        ManualEntry("StudentTDistribution[ν]", "Distributions", "Student's t-distribution with ν degrees of freedom.", "= StudentTDistribution[3]"),
        ManualEntry("CauchyDistribution[x0=0, γ=1]", "Distributions", "Lorentz distribution with location x0 and scale γ. Undefined mean.", "= CauchyDistribution[0, 1]"),
        ManualEntry("GammaDistribution[α, β=1]", "Distributions", "Gamma distribution with shape α and scale β.", "= GammaDistribution[2, 1]"),
        ManualEntry("BetaDistribution[α, β]", "Distributions", "Beta distribution on interval [0, 1] with shape parameters α and β.", "= BetaDistribution[2, 5]"),
        ManualEntry("GeometricDistribution[p]", "Distributions", "Number of Bernoulli trials until first success with success probability p.", "= GeometricDistribution[0.2]"),

        // Heavy Tails & Power Laws
        ManualEntry("ParetoDistribution[k=1, α=1]", "Heavy Tails & Power Laws", "Power law survival P(X>x)=(k/x)^α. Minimum value k, tail exponent α.", "= ParetoDistribution[1, 2]"),
        ManualEntry("LevyDistribution[μ=0, c=1]", "Heavy Tails & Power Laws", "Lévy stable distribution (α=1/2). Heavy tail, infinite mean and variance.", "= LevyDistribution[0, 1]"),
        ManualEntry("LogNormalDistribution[μ=0, σ=1]", "Heavy Tails & Power Laws", "Subexponential distribution with high variance and positive skew.", "= LogNormalDistribution[0, 1.2]"),
        ManualEntry("FrechetDistribution[α, [s=1], [m=0]]", "Heavy Tails & Power Laws", "Extreme Value Type II fat-tailed maximum domain of attraction.", "= FrechetDistribution[2, 1, 0]"),
        ManualEntry("WeibullDistribution[k, [λ=1]]", "Heavy Tails & Power Laws", "Subexponential when shape k < 1; thin-tailed when k > 1.", "= WeibullDistribution[0.7, 1]"),
        ManualEntry("HillEstimator[list, [k]]", "Heavy Tails & Power Laws", "Extreme value theory estimator for Pareto tail exponent α.", "= HillEstimator[{10, 15, 25, 40, 90, 200}]"),
        ManualEntry("ExpectedShortfall[dist | list, [q=0.95]]", "Heavy Tails & Power Laws", "Conditional Value at Risk (CVaR) average beyond quantile q.", "= ExpectedShortfall[NormalDistribution[0, 1], 0.95]"),

        // Distribution Operations
        ManualEntry("PDF[dist, x]", "Probability Ops", "Evaluates Probability Density Function (or PMF) at x or list of x.", "= PDF[NormalDistribution[0, 1], 0]"),
        ManualEntry("CDF[dist, x]", "Probability Ops", "Cumulative Distribution Function: P(X ≤ x).", "= CDF[NormalDistribution[0, 1], 1.96]"),
        ManualEntry("SurvivalFunction[dist, x]", "Probability Ops", "Upper tail probability: P(X > x) = 1 - CDF(x).", "= SurvivalFunction[ParetoDistribution[1, 2], 2]"),
        ManualEntry("HazardFunction[dist, x]", "Probability Ops", "Hazard rate: PDF(x) / SurvivalFunction(x).", "= HazardFunction[ExponentialDistribution[2], 1]"),
        ManualEntry("Mean[dist] / Variance[dist]", "Probability Ops", "Theoretical expected value E[X] or variance Var(X) of distribution.", "= Mean[NormalDistribution[50, 10]]"),
        ManualEntry("Quantile[dist, q]", "Probability Ops", "Inverse CDF: value x such that CDF(x) = q.", "= Quantile[NormalDistribution[0, 1], 0.975]"),
        ManualEntry("InverseCDF[dist, q]", "Probability Ops", "Inverse CDF: value x such that CDF(x) = q.", "= InverseCDF[NormalDistribution[0, 1], 0.975]"),
        ManualEntry("Expectation[dist]", "Probability Ops", "Theoretical expected value E[X] of distribution.", "= Expectation[ParetoDistribution[1, 2]]"),
        ManualEntry("Moment[dist | list, r] / CentralMoment[dist | list, r]", "Probability Ops", "Theoretical or empirical raw moment E[X^r] or central moment E[(X-μ)^r].", "= Moment[NormalDistribution[0, 1], 2]"),

        // Random Generation
        ManualEntry("RandomInteger[] / RandomInteger[max] / RandomInteger[{min, max}, [n]]", "Random Generation", "Generates pseudo-random integer(s) in specified range.", "= RandomInteger[{1, 6}, 5]"),
        ManualEntry("RandomReal[] / RandomReal[max] / RandomReal[{min, max}, [n]]", "Random Generation", "Generates pseudo-random real number(s) in specified range.", "= RandomReal[{0, 10}, 3]"),
        ManualEntry("RandomChoice[list, [n]]", "Random Generation", "Randomly selects item(s) from a list with replacement.", "= RandomChoice[{10, 20, 30, 40}, 2]"),
        ManualEntry("RandomSample[list, [n]]", "Random Generation", "Randomly permutes or samples without replacement from a list.", "= RandomSample[Range[10], 4]"),
        ManualEntry("RandomVariate[dist, [n]]", "Random Generation", "Generates pseudo-random variate(s) from any distribution.", "= Mean[RandomVariate[NormalDistribution[0, 1], 1000]]"),

        // Launcher Smart Commands
        ManualEntry("= <expression>", "Launcher Commands", "Evaluates mathematical expressions, statistics, vectors, and distributions.", "= Mean[{12, 18, 25, 40}]"),
        ManualEntry("w <city | lat, lon>", "Launcher Commands", "Live weather forecast with temperatures, condition, wind, and sunrise/sunset.", "w london"),
        ManualEntry("sunrise <city | lat, lon>", "Launcher Commands", "Civil sunrise and sunset times in local time.", "sunrise tokyo"),
        ManualEntry("sunset <city | lat, lon>", "Launcher Commands", "Civil sunset and sunrise times in local time.", "sunset 35.6762, 139.6503"),
        ManualEntry("st <ticker | index> [range]", "Launcher Commands", "Real-time stock quotes, indices, and charts. Configurable default range or inline: 1d, 5d, 1mo, 6mo, 1y, max.", "st nvda 1mo"),
        ManualEntry("alarm <time>", "Launcher Commands", "Sets an Android system clock alarm for specified time.", "alarm 7:30 AM"),
        ManualEntry("timer <duration>", "Launcher Commands", "Starts a system countdown timer.", "timer 5m"),
        ManualEntry("call <contact>", "Launcher Commands", "Places a phone call or opens dialer for matching contact.", "call Mom"),
        ManualEntry("pallas settings", "Launcher Commands", "Quickly navigates to Pallas Launcher settings.", "pallas settings")
    )

    internal val rng = Random()

    @Volatile
    internal var lastEvaluatedValue: MathValue? = null

    private val KNOWN_FUNCS = setOf(
        "mean", "median", "variance", "standarddeviation", "stddev", "sd", "total", "sum",
        "length", "range", "reverse", "sort", "first", "last", "take", "drop",
        "differences", "accumulate", "count", "quantile", "percentile", "iqr", "interquartilerange",
        "geometricmean", "harmonicmean", "rootmeansquare", "rms", "skewness", "kurtosis",
        "covariance", "correlation", "deleteduplicates",
        "normaldistribution", "uniformdistribution", "exponentialdistribution", "poissondistribution",
        "binomialdistribution", "studenttdistribution", "cauchydistribution", "paretodistribution",
        "levydistribution", "frechetdistribution", "weibulldistribution", "lognormaldistribution",
        "gammadistribution", "betadistribution", "geometricdistribution",
        "pdf", "cdf", "survivalfunction", "hazardfunction", "randomvariate",
        "randominteger", "randomreal", "randomchoice", "randomsample",
        "plus", "subtract", "times", "divide", "power", "sqrt", "cuberoot", "abs", "sign",
        "round", "floor", "ceiling", "mod", "quotient", "divisible", "gcd", "lcm",
        "factorial", "binomial", "primeq", "nextprime", "fibonacci", "clip", "min", "max",
        "sin", "cos", "tan", "cot", "sec", "csc", "arcsin", "arccos", "arctan",
        "sinh", "cosh", "tanh", "exp", "log", "log2", "log10", "ln",
        "expectation", "moment", "centralmoment", "inversecdf",
        "hillestimator", "expectedshortfall", "cvar", "maxdrawdown",
        "standardize", "movingaverage", "table", "fold", "nest"
    )

    @Synchronized
    fun evaluate(rawInput: String, prefix: String): MathEvalOutcome? {
        val trimmed = rawInput.trim()
        val hasPrefix = prefix.isNotEmpty() && (trimmed.startsWith("$prefix ", ignoreCase = true) ||
                (trimmed.startsWith(prefix, ignoreCase = true) && trimmed.length > prefix.length))

        val expr = if (hasPrefix) trimmed.substring(prefix.length).trim() else trimmed
        if (expr.isEmpty()) return null

        if (!hasPrefix && !looksLikeMathOrDist(expr)) return null

        // Strictly Wolfram Language inspired AST evaluation
        return tryEvaluateWolframExpression(expr)
    }

    private fun looksLikeMathOrDist(s: String): Boolean {
        val lower = s.lowercase(Locale.ROOT)
        if (KNOWN_FUNCS.any { lower.startsWith("$it[") }) return true
        if (s.contains('{') && s.contains('}')) return true
        if (s.contains('%')) return true
        val hasOp = s.any { it in "+-*/^%!" }
        val hasDigit = s.any { it.isDigit() }
        return hasDigit && hasOp && s.none { it.isLetter() && it !in "eEpiPI" }
    }

    private fun tryEvaluateWolframExpression(expr: String): MathEvalOutcome? {
        if (expr.length > 1000) return null
        return try {
            val tokens = Tokenizer(expr).tokenize()
            if (tokens.isEmpty()) return null
            val parser = WolframParser(tokens)
            val ast = parser.parse() ?: return null

            val result = ast.eval()
            lastEvaluatedValue = result
            when (result) {
                is MathValue.NumberVal -> {
                    if (result.value.isNaN()) return null
                    val formatted = fmt(result.value)
                    val isProb = ast is MathNode.CallNode && ast.funcName.lowercase(Locale.ROOT) in setOf("cdf", "survivalfunction")
                    val displayValue = if (isProb) "= ${fmtProb(result.value)}" else "= $formatted"
                    MathEvalOutcome(
                        title = expr,
                        primaryValue = displayValue,
                        badge = "Math",
                        subtitle = "Tap to copy result",
                        copyText = formatted
                    )
                }
                is MathValue.BooleanVal -> {
                    val str = if (result.value) "True" else "False"
                    MathEvalOutcome(
                        title = expr,
                        primaryValue = "= $str",
                        badge = "Boolean",
                        subtitle = "Tap to copy result",
                        copyText = str
                    )
                }
                is MathValue.ListVal -> {
                    val list = result.elements
                    val fullList = if (list.isEmpty()) "{}" else list.joinToString(", ", prefix = "{", postfix = "}") { fmt(it) }
                    val sum = if (list.isNotEmpty()) fmt(list.sum()) else "0"
                    val mean = if (list.isNotEmpty()) fmt(list.average()) else "0"
                    MathEvalOutcome(
                        title = expr,
                        primaryValue = fullList,
                        badge = "List (${list.size})",
                        subtitle = "Length: ${list.size} • Total: $sum • Mean: $mean",
                        copyText = fullList
                    )
                }
                is MathValue.DistVal -> {
                    val dist = result.dist
                    val meanStr = if (dist.mean().isNaN()) "undefined" else fmt(dist.mean())
                    val sdStr = if (dist.standardDeviation().isNaN()) "undefined" else fmt(dist.standardDeviation())
                    val medStr = if (dist.median().isNaN()) "undefined" else fmt(dist.median())
                    val varStr = if (dist.variance().isNaN()) "undefined" else fmt(dist.variance())
                    MathEvalOutcome(
                        title = "${dist.name}[${dist.paramsString}]",
                        primaryValue = "Mean = $meanStr, SD = $sdStr",
                        badge = dist.badge,
                        subtitle = "Median: $medStr • Var: $varStr • Try PDF[dist, x], CDF[dist, x], RandomVariate[dist]",
                        copyText = "${dist.name}[${dist.paramsString}]"
                    )
                }
                is MathValue.ExprVal -> {
                    MathEvalOutcome(
                        title = expr,
                        primaryValue = "= ${result.text}",
                        badge = "Formula",
                        subtitle = "Symbolic formula • Tap to copy",
                        copyText = result.text
                    )
                }
                is MathValue.SymbolVal -> {
                    MathEvalOutcome(
                        title = expr,
                        primaryValue = result.name,
                        badge = "Symbol",
                        subtitle = "Undefined symbol",
                        copyText = result.name
                    )
                }
            }
        } catch (_: Throwable) {
            null
        }
    }

    // =========================================================================
    // Combinatorics, Primes & Number Theory
    // =========================================================================

    val FACTORIAL_TABLE: DoubleArray = DoubleArray(171).apply {
        this[0] = 1.0
        for (i in 1..170) {
            this[i] = this[i - 1] * i.toDouble()
        }
    }

    fun exactFactorial(n: Int): Double {
        if (n < 0) return Double.NaN
        if (n <= 170) return FACTORIAL_TABLE[n]
        return Double.POSITIVE_INFINITY
    }

    fun exactBinomial(n: Int, k: Int): Double {
        if (k < 0 || k > n || n < 0) return 0.0
        if (k == 0 || k == n) return 1.0
        val m = minOf(k, n - k)
        if (n <= 170) {
            var res = 1.0
            for (i in 1..m) {
                res = res * (n - m + i) / i
            }
            return kotlin.math.round(res)
        }
        return kotlin.math.round(exp(lnChoose(n, k)))
    }

    fun gcd(a: Long, b: Long): Long {
        var x = abs(a)
        var y = abs(b)
        while (y != 0L) {
            val t = y
            y = x % y
            x = t
        }
        return x
    }

    fun lcm(a: Long, b: Long): Long {
        if (a == 0L || b == 0L) return 0L
        return abs(a / gcd(a, b) * b)
    }

    fun isPrime(n: Long): Boolean {
        if (n <= 1L) return false
        if (n <= 3L) return true
        if (n % 2L == 0L || n % 3L == 0L) return false
        if (n > 100_000_000_000L) {
            return java.math.BigInteger.valueOf(n).isProbablePrime(30)
        }
        var i = 5L
        while (i * i <= n) {
            if (n % i == 0L || n % (i + 2L) == 0L) return false
            i += 6L
        }
        return true
    }

    fun nextPrime(n: Long): Long {
        if (n < 2L) return 2L
        if (n >= 1_000_000_000_000L) return -1L
        var p = n + 1L
        while (!isPrime(p)) {
            p++
        }
        return p
    }

    fun fibonacci(n: Int): Long {
        if (n <= 0) return 0L
        if (n > 92) return -1L
        if (n == 1) return 1L
        var a = 0L
        var b = 1L
        for (i in 2..n) {
            val c = a + b
            a = b
            b = c
        }
        return b
    }

    fun fmt(v: Double): String {
        if (v.isNaN()) return "NaN"
        if (v == Double.POSITIVE_INFINITY) return "∞"
        if (v == Double.NEGATIVE_INFINITY) return "-∞"
        if (abs(v) >= 1e9 || (abs(v) > 0.0 && abs(v) < 1e-4)) {
            return String.format(Locale.US, "%.4e", v)
        }
        val rounded = kotlin.math.round(v)
        if (abs(v - rounded) < 1e-7) return rounded.toLong().toString()
        val l = v.toLong()
        if (abs(v - l) < 1e-9) return l.toString()
        return String.format(Locale.US, "%.5f", v).trimEnd('0').trimEnd('.')
    }

    fun fmtProb(p: Double): String {
        val c = p.coerceIn(0.0, 1.0)
        return if (c > 0.0 && c < 1e-4) {
            String.format(Locale.US, "%.3e", c)
        } else {
            "${String.format(Locale.US, "%.4f", c).trimEnd('0').trimEnd('.')} (${String.format(Locale.US, "%.2f%%", c * 100.0)})"
        }
    }
}