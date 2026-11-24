package com.pallas.launcher.domain.search

import com.pallas.launcher.domain.model.CommandConfig
import java.util.Locale
import java.util.Random
import kotlin.math.*

object MathStatsEngine {

    private data class EngineEntryTemplate(
        val signature: String,
        val category: String,
        val description: String,
        val expression: String
    )

    private val ENGINE_TEMPLATES: List<EngineEntryTemplate> = listOf(
        // 1. Arithmetic & Number Theory
        EngineEntryTemplate("Plus[a, b, ...] / a + b", "Arithmetic & Number Theory", "Addition, subtraction, multiplication, and division on numbers or vectors.", "15 + 27"),
        EngineEntryTemplate("Power[a, b] / a ^ b", "Arithmetic & Number Theory", "Exponentiation of numbers or vectors.", "2^10"),
        EngineEntryTemplate("Sqrt[x] / CubeRoot[x]", "Arithmetic & Number Theory", "Principal square root and real cube root.", "Sqrt[256]"),
        EngineEntryTemplate("N[expr, [digits]]", "Arithmetic & Number Theory", "Numerical evaluation rounded to optional significant digits.", "N[Pi, 8]"),
        EngineEntryTemplate("Abs[x] / Sign[x] / Clip[x, min, max]", "Arithmetic & Number Theory", "Magnitude, sign (-1, 0, 1), and interval clamping.", "Clip[15, 0, 10]"),
        EngineEntryTemplate("Log[b, x] / Log[x] / Log10[x] / Exp[x]", "Arithmetic & Number Theory", "Logarithm base b (argument order: Log[base, x]), natural log, base-10 log, and exponential.", "Log[2, 64]"),
        EngineEntryTemplate("Sin[x] / Cos[x] / Tan[x] / Cot[x] / Sec[x] / Csc[x]", "Arithmetic & Number Theory", "Trigonometric functions in radians (use ° for degrees, e.g. 90°).", "Sin[Pi / 2]"),
        EngineEntryTemplate("ArcSin[x] / ArcCos[x] / ArcTan[x, [y]]", "Arithmetic & Number Theory", "Inverse trigonometric functions; two-argument ArcTan[x, y] returns quadrant-aware angle.", "ArcTan[1, 1]"),
        EngineEntryTemplate("Sinh[x] / Cosh[x] / Tanh[x] / ArcSinh[x]", "Arithmetic & Number Theory", "Hyperbolic functions and inverse hyperbolic functions (also Coth, Sech, Csch, ArcCosh, ArcTanh).", "Tanh[1]"),
        EngineEntryTemplate("Gamma[z] / Erf[x] / Beta[a, b] / Sinc[x] / LogisticSigmoid[x]", "Arithmetic & Number Theory", "Special functions: Gamma, LogGamma, Erf/Erfc, Beta, normalized/cardinal Sinc, HarmonicNumber, and logistic sigmoid.", "Gamma[5]"),
        EngineEntryTemplate("Round[x] / Floor[x] / Ceiling[x]", "Arithmetic & Number Theory", "Nearest integer, floor, and ceiling rounding.", "Round[3.74]"),
        EngineEntryTemplate("Mod[m, d] / Quotient[m, d] / Divisible[m, d]", "Arithmetic & Number Theory", "Modular remainder (Mathematica sign convention), integer quotient, and divisibility predicate.", "Mod[-1, 5]"),
        EngineEntryTemplate("GCD[a, b, ...] / LCM[a, b, ...] / PowerMod[a, b, m]", "Arithmetic & Number Theory", "Greatest common divisor, least common multiple, and modular exponentiation a^b mod m.", "PowerMod[2, 10, 1000]"),
        EngineEntryTemplate("PrimeQ[n] / NextPrime[n] / Prime[k] / PrimePi[x]", "Arithmetic & Number Theory", "Primality test, next prime, k-th prime, and prime-counting function π(x).", "Prime[10]"),
        EngineEntryTemplate("FactorInteger[n] / Divisors[n]", "Arithmetic & Number Theory", "Prime factorization {{p, e}, ...} and positive divisors list.", "FactorInteger[360]"),
        EngineEntryTemplate("IntegerDigits[n, [b]] / FromDigits[digits, [b]]", "Arithmetic & Number Theory", "Converts integers to/from digit lists in base b (default 10).", "IntegerDigits[1234]"),
        EngineEntryTemplate("Fibonacci[n] / Binomial[n, k] / Factorial[n]", "Arithmetic & Number Theory", "Fibonacci numbers, binomial coefficients n choose k, and factorial n!.", "Binomial[52, 5]"),

        // 2. Calculus & Solvers
        EngineEntryTemplate("Sum[expr, {i, min, max}] / Product[expr, {i, min, max}]", "Calculus & Solvers", "Finite or series sums and products over iterator {i, min, max, [step]}.", "Sum[1/k^2, {k, 1, 100}]"),
        EngineEntryTemplate("NIntegrate[expr, {x, a, b}]", "Calculus & Solvers", "Adaptive numerical definite integral over interval [a, b].", "NIntegrate[Sin[x]^2, {x, 0, Pi}]"),
        EngineEntryTemplate("ND[expr, x, x0]", "Calculus & Solvers", "Numerical derivative d/dx at x = x0.", "ND[x^3, x, 2]"),
        EngineEntryTemplate("FindRoot[expr == rhs, {x, x0}]", "Calculus & Solvers", "Finds numerical root near initial guess x0 or within bracket {x, a, b}.", "FindRoot[Cos[x] == x, {x, 0.7}]"),
        EngineEntryTemplate("NMinimize[expr, var] / NMaximize[expr, var]", "Calculus & Solvers", "Numerical global optimization over variable x or bounded interval {x, a, b}.", "NMinimize[x^2 - 4*x + 7, x]"),

        // 3. Lists & Linear Algebra
        EngineEntryTemplate("{x1, x2, ...} (List)", "Lists & Linear Algebra", "List creation with elementwise vector arithmetic.", "{1, 2, 3} * 2"),
        EngineEntryTemplate("Range[n] / Subdivide[min, max, n] / ConstantArray[c, n]", "Lists & Linear Algebra", "Arithmetic sequences, evenly spaced grids, and constant vectors.", "Range[10]"),
        EngineEntryTemplate("Table[expr, {i, min, max, [step]}]", "Lists & Linear Algebra", "Generates a list or matrix by evaluating expr over one or more iterators.", "Table[i^2, {i, 5}]"),
        EngineEntryTemplate("Part[list, i] / list[[i]] / list[[a ;; b]]", "Lists & Linear Algebra", "1-based indexing (negative from end), multi-index [[i, j]], and Span slices [[a ;; b ;; s]].", "Range[10][[3 ;; 7]]"),
        EngineEntryTemplate("First[list] / Last[list] / Take[list, n] / Drop[list, n]", "Lists & Linear Algebra", "Head, tail, and prefix/suffix slicing.", "Take[Range[10], 4]"),
        EngineEntryTemplate("Length[list] / Total[list] / Differences[list] / Accumulate[list]", "Lists & Linear Algebra", "Element count, sum, successive differences, and running prefix sums.", "Accumulate[{1, 2, 3, 4}]"),
        EngineEntryTemplate("Sort[list] / SortBy[list, f] / Reverse[list]", "Lists & Linear Algebra", "Ascending sort, key-function sort, and reversal.", "Sort[{5, 2, 8, 1, 9}]"),
        EngineEntryTemplate("Flatten[list] / Partition[list, n] / Riffle[list, x]", "Lists & Linear Algebra", "Flattens nested lists, groups into length-n sublists, or interleaves elements.", "Partition[{1, 2, 3, 4, 5, 6}, 2]"),
        EngineEntryTemplate("Join[l1, l2] / Append[list, x] / Prepend[list, x]", "Lists & Linear Algebra", "Concatenates lists or inserts elements at end/front.", "Join[{1, 2}, {3, 4}]"),
        EngineEntryTemplate("DeleteDuplicates[list] / Union[a, b] / Intersection[a, b] / Complement[a, b]", "Lists & Linear Algebra", "Deduplication and sorted set operations.", "Intersection[{1, 2, 3}, {2, 3, 4}]"),
        EngineEntryTemplate("Select[list, crit] / Count[list, crit] / MemberQ[list, x] / Position[list, x]", "Lists & Linear Algebra", "Filters, counts, checks membership, and locates matching positions.", "Select[Range[10], EvenQ]"),
        EngineEntryTemplate("AllTrue[list, p] / AnyTrue[list, p] / NoneTrue[list, p]", "Lists & Linear Algebra", "Quantified predicate tests over list elements.", "AllTrue[{2, 4, 6}, EvenQ]"),
        EngineEntryTemplate("Fold[f, x, list] / FoldList[f, x, list]", "Lists & Linear Algebra", "Sequential reduction and intermediate states using binary function f.", "FoldList[Plus, 0, {1, 2, 3}]"),
        EngineEntryTemplate("Nest[f, expr, n] / NestList[f, expr, n]", "Lists & Linear Algebra", "Iterated function application n times.", "NestList[# * 2 &, 1, 4]"),
        EngineEntryTemplate("Dot[a, b] / Norm[v] / Cross[u, v]", "Lists & Linear Algebra", "Vector/matrix product, Euclidean norm (p-norm), and 3D vector cross product.", "Dot[{1, 2, 3}, {4, 5, 6}]"),
        EngineEntryTemplate("Transpose[m] / Tr[m] / Dimensions[m]", "Lists & Linear Algebra", "Matrix transpose, trace, and list/matrix dimensions.", "Transpose[{{1, 2, 3}, {4, 5, 6}}]"),
        EngineEntryTemplate("Det[m] / Inverse[m] / IdentityMatrix[n] / DiagonalMatrix[v]", "Lists & Linear Algebra", "Matrix determinant, inverse, identity matrix, and diagonal construction.", "Det[{{1, 2}, {3, 4}}]"),

        // 4. Statistics
        EngineEntryTemplate("Mean[list] / Median[list] / MinMax[list]", "Statistics", "Arithmetic mean, sample median, and {min, max} pair.", "Mean[{2, 4, 6, 8, 10}]"),
        EngineEntryTemplate("Variance[list] / StandardDeviation[list]", "Statistics", "Sample variance and sample standard deviation (n - 1 denominator).", "StandardDeviation[{2, 4, 4, 4, 5, 5, 7, 9}]"),
        EngineEntryTemplate("Quantile[list, q] / InterquartileRange[list]", "Statistics", "Empirical quantile q in [0, 1] and IQR (Q3 - Q1).", "Quantile[Range[100], 0.75]"),
        EngineEntryTemplate("GeometricMean[list] / HarmonicMean[list] / RootMeanSquare[list]", "Statistics", "Pythagorean means and quadratic mean (RMS).", "GeometricMean[{1, 2, 4, 8}]"),
        EngineEntryTemplate("Skewness[list] / Kurtosis[list]", "Statistics", "Sample asymmetry (3rd) and tail thickness (4th standardized moment).", "Skewness[{1, 2, 2, 3, 4, 5, 9}]"),
        EngineEntryTemplate("Covariance[list1, list2] / Correlation[list1, list2]", "Statistics", "Sample covariance and Pearson correlation coefficient.", "Correlation[{1, 2, 3}, {2, 4, 6}]"),
        EngineEntryTemplate("Standardize[list] / MovingAverage[list, r]", "Statistics", "Z-score standardization and sliding-window moving average.", "MovingAverage[{1, 2, 3, 4, 5, 6}, 3]"),
        EngineEntryTemplate("MaxDrawdown[list] / ExpectedShortfall[src, q] / HillEstimator[list, [k]]", "Statistics", "Peak-to-trough drawdown, CVaR tail expectation, and Pareto tail exponent α.", "MaxDrawdown[{100, 120, 90, 110, 80, 150}]"),

        // 5. Distributions
        EngineEntryTemplate("NormalDistribution[μ=0, σ=1]", "Distributions", "Continuous Gaussian bell curve with mean μ and standard deviation σ.", "NormalDistribution[100, 15]"),
        EngineEntryTemplate("UniformDistribution[min=0, max=1]", "Distributions", "Continuous uniform distribution on [min, max].", "UniformDistribution[0, 10]"),
        EngineEntryTemplate("ExponentialDistribution[λ=1]", "Distributions", "Continuous memoryless decay distribution with rate λ.", "ExponentialDistribution[1.5]"),
        EngineEntryTemplate("StudentTDistribution[ν]", "Distributions", "Continuous Student's t-distribution with ν degrees of freedom.", "StudentTDistribution[3]"),
        EngineEntryTemplate("CauchyDistribution[x0=0, γ=1]", "Distributions", "Continuous Lorentz/Cauchy distribution with undefined mean and variance.", "CauchyDistribution[0, 1]"),
        EngineEntryTemplate("GammaDistribution[α, β=1] / BetaDistribution[α, β]", "Distributions", "Continuous Gamma(α, β) on (0, ∞) and Beta(α, β) on [0, 1].", "BetaDistribution[2, 5]"),
        EngineEntryTemplate("LogNormalDistribution[μ=0, σ=1]", "Distributions", "Subexponential positive continuous distribution with heavy right tail.", "LogNormalDistribution[0, 1.2]"),
        EngineEntryTemplate("ParetoDistribution[k=1, α=1] / LevyDistribution[μ=0, c=1]", "Distributions", "Power-law P(X > x) = (k/x)^α and Lévy α = 1/2 stable distribution.", "ParetoDistribution[1, 2]"),
        EngineEntryTemplate("FrechetDistribution[α, [s=1], [m=0]] / WeibullDistribution[k, [λ=1]]", "Distributions", "Extreme Value Type II (Fréchet) and Weibull extreme-value/reliability models.", "WeibullDistribution[0.7, 1]"),
        EngineEntryTemplate("PoissonDistribution[μ] / BinomialDistribution[n, p] / GeometricDistribution[p]", "Distributions", "Discrete count, success-trial, and waiting-time distributions.", "BinomialDistribution[20, 0.5]"),

        // 6. Probability & Random
        EngineEntryTemplate("PDF[dist, x] / CDF[dist, x]", "Probability & Random", "Density/PMF f(x) and cumulative probability P(X ≤ x).", "CDF[NormalDistribution[0, 1], 1.96]"),
        EngineEntryTemplate("SurvivalFunction[dist, x] / HazardFunction[dist, x]", "Probability & Random", "Upper tail P(X > x) and hazard rate f(x) / S(x).", "SurvivalFunction[ParetoDistribution[1, 2], 2]"),
        EngineEntryTemplate("InverseCDF[dist, q] / Quantile[dist, q]", "Probability & Random", "Quantile x satisfying CDF(dist, x) = q.", "InverseCDF[NormalDistribution[0, 1], 0.975]"),
        EngineEntryTemplate("Expectation[dist] / Mean[dist] / Variance[dist]", "Probability & Random", "Theoretical E[X] and Var(X) of a distribution.", "Mean[NormalDistribution[50, 10]]"),
        EngineEntryTemplate("Moment[dist | list, r] / CentralMoment[dist | list, r]", "Probability & Random", "Raw r-th moment E[X^r] or central r-th moment E[(X - μ)^r].", "Moment[NormalDistribution[0, 1], 2]"),
        EngineEntryTemplate("RandomInteger[spec, [n]] / RandomReal[spec, [n]]", "Probability & Random", "Generates random integer or real numbers/lists over {min, max} or [0, max].", "RandomInteger[{1, 6}, 5]"),
        EngineEntryTemplate("RandomChoice[list, [n]] / RandomSample[list, [n]]", "Probability & Random", "Samples items from a list with or without replacement.", "RandomSample[Range[10], 4]"),
        EngineEntryTemplate("RandomVariate[dist, [n]] / SeedRandom[seed]", "Probability & Random", "Draws random variate(s) from a distribution; optionally sets PRNG seed.", "Mean[RandomVariate[NormalDistribution[0, 1], 1000]]"),

        // 7. Operators & Control Flow
        EngineEntryTemplate("f @ x (Prefix) / x // f (Postfix)", "Operators & Control Flow", "Prefix f[x] and postfix pipeline x // f application.", "{1, 2, 3} // Total"),
        EngineEntryTemplate("f /@ list (Map) / f @@ expr (Apply) / f @@@ list", "Operators & Control Flow", "Maps f over elements, replaces head of expr, or applies f at level 1.", "Plus @@@ {{1, 2}, {3, 4}}"),
        EngineEntryTemplate("f @* g / g /* f (Composition)", "Operators & Control Flow", "Right-to-left (@*) or left-to-right (/*) function composition.", "(Sqrt @* (# + 1 &))[15]"),
        EngineEntryTemplate("expr & / # (Pure Function)", "Operators & Control Flow", "Anonymous function with positional slot # or #1, #2.", "(#^2) & /@ {1, 2, 3, 4}"),
        EngineEntryTemplate("expr /. rule / expr //. rule (Replace)", "Operators & Control Flow", "Single-pass ReplaceAll (/.) and fixed-point ReplaceRepeated (//.).", "x^2 + 2*x + 1 /. x -> 3"),
        EngineEntryTemplate("x = val / f[x_] := expr (Assignment)", "Operators & Control Flow", "Immediate assignment (=) and delayed definition/function (:=).", "f[x_] := x^2 + 1; f[5]"),
        EngineEntryTemplate("a; b / (* comment *) / % (Compound & Last)", "Operators & Control Flow", "Sequential evaluation (a; b), block comments, and previous output %.", "q = 10; q"),
        EngineEntryTemplate("Module[{x = v}, body] / With[{...}, body] / Block / Clear", "Operators & Control Flow", "Lexical/local variable scoping and symbol clearing.", "Module[{a = 3, b = 4}, Sqrt[a^2 + b^2]]"),
        EngineEntryTemplate("If[c, t, f] / Which[...] / Switch[expr, ...]", "Operators & Control Flow", "Conditional branching and pattern/value dispatch.", "Which[2 < 0, -1, 2 == 0, 0, 2 > 0, 1]"),
        EngineEntryTemplate("While[test, body] / For[start, test, incr, body] / Do[...]", "Operators & Control Flow", "Bounded iteration loops with automatic execution-step safety.", "s = 0; For[i = 1, i <= 4, i++, s += i^2]; s"),
        EngineEntryTemplate("Return[x] / Break[] / Continue[] / Boole[cond]", "Operators & Control Flow", "Early loop/function control and 1/0 boolean conversion.", "Boole[3 > 2]"),
        EngineEntryTemplate("Print[expr, ...] / Echo[expr, [label]]", "Operators & Control Flow", "Emits values to the card's console output block; Echo passes its value through.", "q = 10; Print[q^2]; q"),
        EngineEntryTemplate("a <> b (StringJoin) / ToString / StringLength / ToUpperCase", "Operators & Control Flow", "String concatenation, conversion, length, reversal, and case transforms.", "\"hello\" <> \" world\""),

        // 8. Plotting & Charts
        EngineEntryTemplate("Plot[f, {x, min, max}]", "Plotting & Charts", "Plots function(s) {f1, f2, ...} or any Distribution object over [min, max].", "Plot[{Sin[x], Cos[x]}, {x, -Pi, Pi}]"),
        EngineEntryTemplate("LogPlot[f, {x, min, max}] / LogLinearPlot / LogLogPlot", "Plotting & Charts", "2D plots with base-10 logarithmic y, x, or log-log axes.", "LogPlot[Exp[x], {x, 0, 5}]"),
        EngineEntryTemplate("ParametricPlot[{fx, fy}, {u, min, max}] / PolarPlot", "Plotting & Charts", "Equal-aspect 2D parametric and polar curves.", "ParametricPlot[{Sin[3 t], Cos[2 t]}, {t, 0, 2 Pi}]"),
        EngineEntryTemplate("ListPlot[data] / ListLinePlot[data]", "Plotting & Charts", "Scatter and line charts from {y1, ...}, {{x1, y1}, ...}, or Table[{i, f[i]}, ...].", "ListLinePlot[Table[Fibonacci[n], {n, 10}]]"),
        EngineEntryTemplate("ListLogPlot / ListLogLinearPlot / ListLogLogPlot", "Plotting & Charts", "Discrete/line dataset plots on logarithmic axes.", "ListLogPlot[Table[2^n, {n, 10}]]"),
        EngineEntryTemplate("Histogram[data, [bins], [mode]]", "Plotting & Charts", "Empirical or distribution histogram with Count, Probability, or PDF scaling.", "Histogram[RandomVariate[NormalDistribution[0, 1], 400]]"),
        EngineEntryTemplate("BarChart[data] / PieChart[data]", "Plotting & Charts", "Vertical bar and pie charts from numeric lists or labeled rules.", "BarChart[{Q1 -> 24, Q2 -> 38, Q3 -> 31, Q4 -> 49}]"),
        EngineEntryTemplate("BoxWhiskerChart[data]", "Plotting & Charts", "Five-number summary box plot showing median, IQR, whiskers, and outliers.", "BoxWhiskerChart[RandomVariate[LogNormalDistribution[0, 0.6], 200]]")
    )

    fun manualEntriesFor(configs: Map<String, CommandConfig>): List<ManualEntry> {
        fun cfg(id: String) = configs[id] ?: CommandConfig.DEFAULTS.getValue(id)

        val mathCfg = cfg("math")
        val texCfg = cfg("tex")
        val convertCfg = cfg("convert")
        val wxCfg = cfg("weather")
        val solarCfg = cfg("solar")
        val stockCfg = cfg("stock")
        val waCfg = cfg("wolfram")
        val wikiCfg = cfg("wiki")
        val tzCfg = cfg("tz")
        val defCfg = cfg("def")
        val noteCfg = cfg("note")
        val qrCfg = cfg("qr")
        val emiCfg = cfg("emi")
        val webCfg = cfg("web")
        val alCfg = cfg("alarm")
        val timerCfg = cfg("timer")
        val callCfg = cfg("call")
        val filesCfg = cfg("files")
        val calCfg = cfg("calendar")
        val flCfg = cfg("flashlight")
        val dndCfg = cfg("dnd")
        val appSearchCfg = cfg("app_search")

        val mathPrefix = mathCfg.prefix.ifBlank { "=" }
        val engineEntries = ENGINE_TEMPLATES.map { t ->
            ManualEntry(
                signature = t.signature,
                category = t.category,
                description = t.description,
                example = "$mathPrefix ${t.expression}",
                isEnabled = true
            )
        }

        val commandEntries = listOf(
            ManualEntry(
                signature = "${mathCfg.prefix} <expression>",
                category = "Launcher Commands",
                description = "Evaluates mathematical expressions, statistics, vectors, and distributions.",
                example = "${mathCfg.prefix} Mean[{12, 18, 25, 40}]",
                isEnabled = mathCfg.enabled
            ),
            ManualEntry(
                signature = "${texCfg.prefix} <latex>",
                category = "Launcher Commands",
                description = "Renders LaTeX math (with AMS packages) to a crisp preview image you can share, save as PNG, or copy.",
                example = "${texCfg.prefix} \\int_0^\\infty e^{-x^2}\\,dx = \\frac{\\sqrt{\\pi}}{2}",
                isEnabled = texCfg.enabled
            ),
            ManualEntry(
                signature = "${convertCfg.prefix} <query>",
                category = "Launcher Commands",
                description = "Converts units and 150+ currencies with cached FX rates. Supports 'convert rates', 'convert USD', 'convert EUR/USD' or 'EURUSD', plus inline '100 km to mi'.",
                example = "${convertCfg.prefix} 100 usd to inr",
                isEnabled = convertCfg.enabled
            ),
            ManualEntry(
                signature = "${wxCfg.prefix} <city | lat, lon>",
                category = "Launcher Commands",
                description = "Live weather forecast with temperatures, condition, wind, and sunrise/sunset ('weather' also works).",
                example = "${wxCfg.prefix} london",
                isEnabled = wxCfg.enabled
            ),
            ManualEntry(
                signature = "${solarCfg.prefix} <city | lat, lon>",
                category = "Launcher Commands",
                description = "Civil sunrise and sunset times in local time ('sunrise' and 'sunset' also work).",
                example = "${solarCfg.prefix} tokyo",
                isEnabled = solarCfg.enabled
            ),
            ManualEntry(
                signature = "${stockCfg.prefix} <ticker | index> [range]",
                category = "Launcher Commands",
                description = "Real-time stock quotes, indices, and charts. Configurable default range or inline: 1d, 5d, 1mo, 6mo, 1y, max.",
                example = "${stockCfg.prefix} nvda 1mo",
                isEnabled = stockCfg.enabled
            ),
            ManualEntry(
                signature = "${waCfg.prefix} <query>",
                category = "Launcher Commands",
                description = "Queries Wolfram|Alpha Short Answer API (or opens the website depending on Search Settings).",
                example = "${waCfg.prefix} integrate sin(x)^2",
                isEnabled = waCfg.enabled
            ),
            ManualEntry(
                signature = "${wikiCfg.prefix} <topic>",
                category = "Launcher Commands",
                description = "Fetches a concise Wikipedia summary card. Tap to open the article or copy the summary.",
                example = "${wikiCfg.prefix} Euler's identity",
                isEnabled = wikiCfg.enabled
            ),
            ManualEntry(
                signature = "${tzCfg.prefix} <city | zone> / <time> <zone>",
                category = "Launcher Commands",
                description = "Offline world clock showing current local time, UTC offset, offset difference, or converted time across zones.",
                example = "${tzCfg.prefix} nyc",
                isEnabled = tzCfg.enabled
            ),
            ManualEntry(
                signature = "${defCfg.prefix} <word>",
                category = "Launcher Commands",
                description = "Fetches word definitions, phonetic pronunciation, and example usage.",
                example = "${defCfg.prefix} ephemeral",
                isEnabled = defCfg.enabled
            ),
            ManualEntry(
                signature = "${noteCfg.prefix} <text>",
                category = "Launcher Commands",
                description = "Appends a timestamped Markdown note to your Quick Capture inbox (type '${noteCfg.prefix}' alone to view inbox).",
                example = "${noteCfg.prefix} Read Feynman Ch. 4",
                isEnabled = noteCfg.enabled
            ),
            ManualEntry(
                signature = "${qrCfg.prefix} <text | url | wifi>",
                category = "Launcher Commands",
                description = "Generates a scannable offline QR code inline. Tap to enlarge, save PNG, or share.",
                example = "${qrCfg.prefix} https://example.com",
                isEnabled = qrCfg.enabled
            ),
            ManualEntry(
                signature = "${emiCfg.prefix} <principal> <rate%> <tenure>",
                category = "Launcher Commands",
                description = "Computes monthly loan EMI, total interest payable, and an amortization milestone schedule.",
                example = "${emiCfg.prefix} 500000 8.5 5y",
                isEnabled = emiCfg.enabled
            ),
            ManualEntry(
                signature = "days to <date> / today + 90d",
                category = "Launcher Commands",
                description = "Date arithmetic without a prefix: days until/since/between dates, weekday lookup, or add/subtract d/w/m/y.",
                example = "days to 15 may",
                isEnabled = true
            ),
            ManualEntry(
                signature = "#RRGGBB / rgb(...) / hex <n> / bin <n>",
                category = "Launcher Commands",
                description = "Color & Base Inspector without a prefix: live swatch with RGB/HSL/contrast or instant hex/bin/oct/dec conversion.",
                example = "#D4AF37",
                isEnabled = true
            ),
            ManualEntry(
                signature = "${webCfg.prefix} <terms>",
                category = "Launcher Commands",
                description = "Searches your configured web engine in your preferred browser ('query' also works).",
                example = "${webCfg.prefix} fourier transform",
                isEnabled = webCfg.enabled
            ),
            ManualEntry(
                signature = "${alCfg.prefix} <time>",
                category = "Launcher Commands",
                description = "Sets an Android system clock alarm for the specified time ('alarm' also works).",
                example = "${alCfg.prefix} 7:30 AM",
                isEnabled = alCfg.enabled
            ),
            ManualEntry(
                signature = "${timerCfg.prefix} <duration>",
                category = "Launcher Commands",
                description = "Starts a system countdown timer (supports h/m/s or HH:MM).",
                example = "${timerCfg.prefix} 5m",
                isEnabled = timerCfg.enabled
            ),
            ManualEntry(
                signature = "${callCfg.prefix} <nickname>",
                category = "Launcher Commands",
                description = "Calls favorite/pinned contacts with saved nicknames only ('@name' searches the full device contacts list).",
                example = "${callCfg.prefix} Mom",
                isEnabled = callCfg.enabled
            ),
            ManualEntry(
                signature = "focus [profile] [duration] [+ dnd] / give up",
                category = "Launcher Commands",
                description = "Hides non-profile screens and apps (via negativa). Supports durations like '60', '3h', '+ dnd', and 'focus off' / 'give up' to end.",
                example = "focus 60m",
                isEnabled = true
            ),
            ManualEntry(
                signature = "${filesCfg.prefix}<filename>",
                category = "Launcher Commands",
                description = "Fast local file search across Downloads, Documents, and media using Android's indexed MediaStore.",
                example = "${filesCfg.prefix}report",
                isEnabled = filesCfg.enabled
            ),
            ManualEntry(
                signature = "show cal / ${calCfg.prefix} / agenda",
                category = "Launcher Commands",
                description = "Displays an offline 7-day calendar agenda inline using Android's CalendarContract.",
                example = "show cal",
                isEnabled = calCfg.enabled
            ),
            ManualEntry(
                signature = "${flCfg.prefix} [on | off]",
                category = "Launcher Commands",
                description = "Instant hardware flashlight toggle.",
                example = flCfg.prefix,
                isEnabled = flCfg.enabled
            ),
            ManualEntry(
                signature = "${dndCfg.prefix} [on | off]",
                category = "Launcher Commands",
                description = "Instant Android Do Not Disturb toggle.",
                example = dndCfg.prefix,
                isEnabled = dndCfg.enabled
            ),
            ManualEntry(
                signature = "${appSearchCfg.prefix}<app> <query>",
                category = "Launcher Commands",
                description = "Searches directly inside installed apps: yt (YouTube), az (Amazon), sp (Spotify), gm (Maps), play, rd (Reddit), gh (GitHub), x.",
                example = "${appSearchCfg.prefix}yt linear algebra",
                isEnabled = appSearchCfg.enabled
            ),
            ManualEntry(
                signature = "pallas [engine | cheatsheets | <topic> | <formula>]",
                category = "Launcher Commands",
                description = "Jumps to Settings, Guide tabs, specific cheatsheets (e.g. 'pallas stats', 'pallas info theory'), formulas ('pallas jensen'), Atlas, Notebook, or Studio.",
                example = "pallas stats",
                isEnabled = true
            ),
            ManualEntry(
                signature = "settings",
                category = "Launcher Commands",
                description = "Quickly navigates to Pallas Launcher settings.",
                example = "settings",
                isEnabled = true
            )
        )

        return engineEntries + commandEntries
    }

    val MANUAL_ENTRIES: List<ManualEntry> = manualEntriesFor(CommandConfig.DEFAULTS)

    private val threadLocalRng = ThreadLocal.withInitial { Random() }
    internal val rng: Random
        get() = threadLocalRng.get() ?: Random()

    @Volatile
    internal var lastEvaluatedValue: MathValue? = null

    private val KNOWN_FUNCS = setOf(
        "mean", "median", "variance", "standarddeviation", "stddev", "sd", "total", "sum", "product",
        "length", "range", "reverse", "sort", "sortby", "first", "last", "take", "drop",
        "differences", "accumulate", "count", "quantile", "percentile", "iqr", "interquartilerange",
        "geometricmean", "harmonicmean", "rootmeansquare", "rms", "skewness", "kurtosis",
        "covariance", "correlation", "deleteduplicates",
        "flatten", "join", "append", "prepend", "partition", "riffle", "subdivide", "constantarray",
        "minmax", "union", "intersection", "complement", "memberq", "position", "list", "span",
        "alltrue", "anytrue", "nonetrue", "scan",
        "dot", "norm", "cross", "transpose", "tr", "dimensions", "identitymatrix", "diagonalmatrix", "det", "inverse",
        "normaldistribution", "uniformdistribution", "exponentialdistribution", "poissondistribution",
        "binomialdistribution", "studenttdistribution", "cauchydistribution", "paretodistribution",
        "levydistribution", "frechetdistribution", "weibulldistribution", "lognormaldistribution",
        "gammadistribution", "betadistribution", "geometricdistribution",
        "pdf", "cdf", "survivalfunction", "hazardfunction", "randomvariate",
        "randominteger", "randomreal", "randomchoice", "randomsample", "seedrandom",
        "plus", "subtract", "times", "divide", "power", "sqrt", "cuberoot", "abs", "sign",
        "round", "floor", "ceiling", "mod", "quotient", "divisible", "gcd", "lcm",
        "factorial", "binomial", "primeq", "nextprime", "fibonacci",
        "factorinteger", "divisors", "prime", "primepi", "powermod", "integerdigits", "fromdigits",
        "clip", "min", "max", "n",
        "sin", "cos", "tan", "cot", "sec", "csc", "arcsin", "arccos", "arctan",
        "sinh", "cosh", "tanh", "coth", "sech", "csch", "arcsinh", "arccosh", "arctanh", "arccot", "arcsec", "arccsc",
        "exp", "log", "log2", "log10", "ln",
        "gamma", "loggamma", "erf", "erfc", "beta", "sinc", "logisticsigmoid", "harmonicnumber", "boole",
        "nintegrate", "nd", "findroot", "nminimize", "nmaximize",
        "expectation", "moment", "centralmoment", "inversecdf",
        "hillestimator", "expectedshortfall", "cvar", "maxdrawdown",
        "standardize", "movingaverage", "table", "fold", "nest",
        "if", "which", "switch", "while", "for", "do", "select", "map", "apply", "part",
        "module", "block", "with", "function", "clear", "print", "echo",
        "stringjoin", "tostring", "stringlength", "stringreverse", "touppercase", "tolowercase",
        "evenq", "oddq", "integerq", "positive", "negative", "zeroq",
        "nestlist", "foldlist", "break", "continue", "return",
        "plot", "logplot", "loglinearplot", "loglogplot", "parametricplot", "polarplot",
        "listplot", "listlineplot", "listlogplot", "listloglinearplot", "listloglogplot",
        "histogram", "barchart", "piechart", "boxwhiskerchart",
        "texform"
    )

    internal val PAREN_CALL_FUNCS: Set<String> = KNOWN_FUNCS - setOf("n")

    private val FUNC_SYNTAX_REGEX = Regex("\\b([a-zA-Z]+)(?:\\s*\\[|\\(|\\s*@)")
    private val ASSIGNMENT_REGEX = Regex("^\\s*[a-zA-Z][a-zA-Z0-9$]*(\\s*\\[[^\\]]*\\])?\\s*(:?=)(?!=)")

    private val PLOT_HEAD_REGEX = Regex(
        "\\b(plot|logplot|loglinearplot|loglogplot|parametricplot|polarplot|listplot|listlineplot|listlogplot|listloglinearplot|listloglogplot|histogram|barchart|piechart|boxwhiskerchart)\\s*(\\[|@|$)",
        RegexOption.IGNORE_CASE
    )

    fun containsPlotOrChart(rawInput: String, prefix: String): Boolean {
        val trimmed = rawInput.trim()
        val hasPrefix = prefix.isNotEmpty() && (trimmed.startsWith("$prefix ", ignoreCase = true) ||
                (trimmed.startsWith(prefix, ignoreCase = true) && trimmed.length > prefix.length))
        val expr = if (hasPrefix) trimmed.substring(prefix.length).trim() else trimmed
        if (expr.isEmpty()) return false
        if (PLOT_HEAD_REGEX.containsMatchIn(expr)) return true
        // Also support postfix e.g. data // ListPlot or data // Histogram
        val lower = expr.lowercase(Locale.ROOT)
        if (lower.contains("//")) {
            return WolframPlotEvaluator.PLOT_FUNCTION_NAMES.any { fn ->
                lower.contains("//$fn") || lower.contains("// $fn")
            }
        }
        return false
    }

    fun areDelimitersBalanced(input: String): Boolean {
        var sq = 0
        var cu = 0
        var pa = 0
        var inQuotes = false
        for (ch in input) {
            if (ch == '"') {
                inQuotes = !inQuotes
                continue
            }
            if (inQuotes) continue
            when (ch) {
                '[' -> sq++
                ']' -> {
                    sq--
                    if (sq < 0) return false
                }
                '{' -> cu++
                '}' -> {
                    cu--
                    if (cu < 0) return false
                }
                '(' -> pa++
                ')' -> {
                    pa--
                    if (pa < 0) return false
                }
            }
        }
        return !inQuotes && sq == 0 && cu == 0 && pa == 0
    }

    fun evaluate(rawInput: String, prefix: String): MathEvalOutcome? {
        val trimmed = rawInput.trim()
        val hasPrefix = prefix.isNotEmpty() && (trimmed.startsWith("$prefix ", ignoreCase = true) ||
                (trimmed.startsWith(prefix, ignoreCase = true) && trimmed.length > prefix.length))

        val expr = if (hasPrefix) trimmed.substring(prefix.length).trim() else trimmed
        if (expr.isEmpty()) return null
        // Slot markers are only meaningful inside a pure function expression; treating a
        // lone # as an expression creates a spurious command row in launcher search.
        if (expr.matches(Regex("#\\d*"))) return null

        if (!hasPrefix && !looksLikeMathOrDist(expr)) return null

        // Strictly Wolfram Language inspired AST evaluation
        return tryEvaluateWolframExpression(expr, hasPrefix)
    }

    private fun looksLikeMathOrDist(s: String): Boolean {
        if (FUNC_SYNTAX_REGEX.findAll(s).any { match ->
                val fn = match.groupValues[1].lowercase(Locale.ROOT)
                if (match.value.endsWith("(")) fn in PAREN_CALL_FUNCS else fn in KNOWN_FUNCS
            }
        ) return true
        if (ASSIGNMENT_REGEX.containsMatchIn(s)) return true
        if (s.contains('{') && s.contains('}')) return true
        if (s.contains('%')) return true
        if (s.contains("/@") || s.contains("@@") || s.contains("//") || s.contains("/*") ||
            s.contains("@*") || s.contains("@") || s.contains("/.") || s.contains("->") ||
            s.contains(":>") || s.contains("→") || s.contains(";;") || s.contains("(*") ||
            s.contains("<>") || s.contains('&') || s.contains('#') || s.contains(';') ||
            s.contains("[[") || s.contains(":=")
        ) return true
        val hasOp = s.any { it in "+-*/^%!<>=&|;°" }
        val hasDigit = s.any { it.isDigit() }
        return hasDigit && hasOp && s.none { it.isLetter() && it !in "eEpiPI°" }
    }

    data class KernelEvalResult(
        val outcome: MathEvalOutcome?,
        val latexOutput: String? = null,
        val errorMessage: String? = null,
        val updatedRng: Random = rng
    )

    private fun tryEvaluateWolframExpression(expr: String, hasPrefix: Boolean = false): MathEvalOutcome? {
        if (expr.length > 1000) return null
        return try {
            val tokens = Tokenizer(expr).tokenize()
            if (tokens.isEmpty()) return null
            val parser = WolframParser(tokens)
            val ast = parser.parse() ?: return null

            val isPlotExpr = PLOT_HEAD_REGEX.containsMatchIn(expr) || expr.contains("//")
            val ctx = ExecutionContext(
                maxSteps = if (isPlotExpr) 45_000 else 10_000,
                timeoutMs = if (isPlotExpr) 350L else 150L
            )
            lastEvaluatedValue?.let {
                ctx.env["%"] = it
                ctx.env["out"] = it
            }

            val result = try {
                ast.eval(ctx)
            } catch (r: ReturnException) {
                r.value
            }

            if (result is MathValue.SymbolVal && !hasPrefix && ctx.printedLines.isEmpty()) {
                return null
            }

            if (result !is MathValue.NullVal) {
                lastEvaluatedValue = result
            }
            val console = ctx.printedLines.takeIf { it.isNotEmpty() }?.joinToString("\n")
            formatOutcome(expr, result, ast, console)
        } catch (e: ComputationLimitException) {
            MathEvalOutcome(
                title = expr,
                primaryValue = "Computation timed out",
                badge = "Timeout",
                subtitle = "Calculation exceeded search time limit",
                copyText = ""
            )
        } catch (_: Throwable) {
            null
        }
    }

    fun evaluateInKernel(
        rawInput: String,
        env: MutableMap<String, MathValue>,
        maxSteps: Int = 100_000,
        timeoutMs: Long = 1500L,
        rng: Random = this.rng
    ): KernelEvalResult {
        val trimmed = rawInput.trim()
        val expr = if (trimmed.startsWith("=") && !trimmed.startsWith("==")) {
            trimmed.substring(1).trim()
        } else {
            trimmed
        }
        if (expr.isEmpty()) {
            return KernelEvalResult(outcome = null, errorMessage = "Empty expression", updatedRng = rng)
        }
        if (expr.length > 10_000) {
            return KernelEvalResult(outcome = null, errorMessage = "Expression exceeds 10,000 character limit", updatedRng = rng)
        }

        val texInner = extractTeXFormInner(expr)
        if (texInner != null) {
            if (texInner.isEmpty()) {
                return KernelEvalResult(outcome = null, errorMessage = "TeXForm requires an expression", updatedRng = rng)
            }
            return try {
                val tokens = Tokenizer(texInner).tokenize()
                if (tokens.isEmpty()) {
                    return KernelEvalResult(outcome = null, errorMessage = "Syntax error — empty TeXForm argument", updatedRng = rng)
                }
                val ast = WolframParser(tokens).parse()
                    ?: return KernelEvalResult(outcome = null, errorMessage = "Syntax error — check brackets or expression", updatedRng = rng)
                val ctx = ExecutionContext(
                    maxSteps = maxSteps,
                    timeoutMs = timeoutMs,
                    env = env,
                    rng = rng,
                    isolated = true
                )
                val result = try {
                    ast.eval(ctx)
                } catch (r: ReturnException) {
                    r.value
                }
                if (result !is MathValue.NullVal) {
                    env["%"] = result
                    env["out"] = result
                }
                val console = ctx.printedLines.takeIf { it.isNotEmpty() }?.joinToString("\n")
                val tex = mathValueToLatex(result)
                KernelEvalResult(
                    outcome = MathEvalOutcome(
                        title = expr,
                        primaryValue = tex,
                        badge = "LaTeX",
                        subtitle = "",
                        copyText = tex,
                        consoleOutput = console
                    ),
                    latexOutput = tex,
                    updatedRng = ctx.rng
                )
            } catch (e: ComputationLimitException) {
                KernelEvalResult(outcome = null, errorMessage = e.message ?: "Computation timed out", updatedRng = rng)
            } catch (t: Throwable) {
                KernelEvalResult(outcome = null, errorMessage = t.message?.takeIf { it.isNotBlank() } ?: "Evaluation error", updatedRng = rng)
            }
        }

        return try {
            val tokens = Tokenizer(expr).tokenize()
            if (tokens.isEmpty()) {
                return KernelEvalResult(outcome = null, errorMessage = "Syntax error — check expression", updatedRng = rng)
            }
            val ast = WolframParser(tokens).parse()
                ?: return KernelEvalResult(outcome = null, errorMessage = "Syntax error — check brackets or expression", updatedRng = rng)
            val ctx = ExecutionContext(
                maxSteps = maxSteps,
                timeoutMs = timeoutMs,
                env = env,
                rng = rng,
                isolated = true
            )
            val result = try {
                ast.eval(ctx)
            } catch (r: ReturnException) {
                r.value
            }
            if (result !is MathValue.NullVal) {
                env["%"] = result
                env["out"] = result
            }
            val console = ctx.printedLines.takeIf { it.isNotEmpty() }?.joinToString("\n")
            val outcome = formatOutcome(expr, result, ast, console)
                ?: return KernelEvalResult(outcome = null, errorMessage = "Result is undefined (NaN)", updatedRng = ctx.rng)
            KernelEvalResult(
                outcome = outcome,
                latexOutput = null,
                errorMessage = null,
                updatedRng = ctx.rng
            )
        } catch (e: ComputationLimitException) {
            KernelEvalResult(outcome = null, errorMessage = e.message ?: "Computation timed out", updatedRng = rng)
        } catch (t: Throwable) {
            KernelEvalResult(outcome = null, errorMessage = t.message?.takeIf { it.isNotBlank() } ?: "Evaluation error", updatedRng = rng)
        }
    }

    private fun extractTeXFormInner(expr: String): String? {
        // 1. Prefix TeXForm[...]
        if (expr.length >= 9 && expr.substring(0, 7).equals("texform", ignoreCase = true)) {
            var idx = 7
            while (idx < expr.length && expr[idx].isWhitespace()) idx++
            if (idx < expr.length && expr[idx] == '[') {
                var depth = 0
                var inQuotes = false
                var i = idx
                while (i < expr.length) {
                    val ch = expr[i]
                    if (ch == '"' && (i == 0 || expr[i - 1] != '\\')) {
                        inQuotes = !inQuotes
                    } else if (!inQuotes) {
                        if (ch == '[') depth++
                        else if (ch == ']') {
                            depth--
                            if (depth == 0) {
                                return if (i == expr.length - 1) {
                                    expr.substring(idx + 1, i).trim()
                                } else {
                                    break
                                }
                            }
                        }
                    }
                    i++
                }
            }
        }

        // 2. Postfix ... // TeXForm (check last top-level //)
        var sq = 0
        var cu = 0
        var pa = 0
        var inQuotes = false
        var lastSlashSlash = -1
        var i = 0
        while (i < expr.length - 1) {
            val ch = expr[i]
            if (ch == '"' && (i == 0 || expr[i - 1] != '\\')) {
                inQuotes = !inQuotes
                i++
                continue
            }
            if (!inQuotes) {
                when (ch) {
                    '[' -> sq++
                    ']' -> sq--
                    '{' -> cu++
                    '}' -> cu--
                    '(' -> pa++
                    ')' -> pa--
                    '/' -> if (sq == 0 && cu == 0 && pa == 0 && expr[i + 1] == '/') {
                        lastSlashSlash = i
                        i += 2
                        continue
                    }
                }
            }
            i++
        }
        if (lastSlashSlash > 0) {
            val suffix = expr.substring(lastSlashSlash + 2).trim()
            if (suffix.equals("texform", ignoreCase = true)) {
                return expr.substring(0, lastSlashSlash).trim()
            }
        }
        return null
    }

    fun mathValueToLatex(value: MathValue): String = when (value) {
        is MathValue.NumberVal -> {
            val v = value.value
            when {
                v.isNaN() -> "\\text{NaN}"
                v == Double.POSITIVE_INFINITY -> "\\infty"
                v == Double.NEGATIVE_INFINITY -> "-\\infty"
                else -> {
                    val s = fmt(v)
                    val eIdx = s.indexOfAny(charArrayOf('e', 'E'))
                    if (eIdx > 0) {
                        val mantissa = s.substring(0, eIdx).trimEnd('0').trimEnd('.')
                        val rawExp = s.substring(eIdx + 1)
                        val expSign = if (rawExp.startsWith("-")) "-" else ""
                        val expDigits = rawExp.trimStart('+', '-').trimStart('0').ifEmpty { "0" }
                        "$mantissa \\times 10^{$expSign$expDigits}"
                    } else {
                        s
                    }
                }
            }
        }
        is MathValue.BooleanVal -> if (value.value) "\\text{True}" else "\\text{False}"
        is MathValue.StringVal -> {
            val escaped = buildString(value.text.length) {
                for (ch in value.text) {
                    when (ch) {
                        '\\' -> append("\\textbackslash{}")
                        '{', '}', '%', '_', '&', '#', '$' -> append('\\').append(ch)
                        else -> append(ch)
                    }
                }
            }
            "\\text{$escaped}"
        }
        is MathValue.ListVal -> {
            val inner = value.elements.joinToString(", ") { mathValueToLatex(MathValue.NumberVal(it)) }
            "\\left\\{ $inner \\right\\}"
        }
        is MathValue.MixedListVal -> {
            val matrix = value.toNumericMatrix()
            if (matrix != null) {
                val rows = matrix.joinToString(" \\\\ ") { row ->
                    row.joinToString(" & ") { mathValueToLatex(MathValue.NumberVal(it)) }
                }
                "\\begin{pmatrix} $rows \\end{pmatrix}"
            } else {
                val inner = value.items.joinToString(", ") { mathValueToLatex(it) }
                "\\left\\{ $inner \\right\\}"
            }
        }
        is MathValue.DistVal -> "\\operatorname{${value.dist.name}}\\left(${value.dist.paramsString}\\right)"
        is MathValue.ExprVal -> value.text
        is MathValue.SymbolVal -> when (value.name.lowercase(Locale.ROOT)) {
            "pi", "π" -> "\\pi"
            "e" -> "e"
            "infinity", "inf", "∞" -> "\\infty"
            else -> value.name
        }
        is MathValue.RuleVal -> "${value.lhs} \\to ${mathValueToLatex(value.rhs)}"
        is MathValue.NullVal -> "\\text{Null}"
        is MathValue.FunctionVal -> "\\text{Function[...]}"
        is MathValue.DelayedVal -> "\\text{Delayed[...]}"
        is MathValue.ChartVal -> "\\text{${value.chart.title}}"
    }

    private fun formatOutcome(
        expr: String,
        result: MathValue,
        ast: MathNode?,
        console: String?
    ): MathEvalOutcome? = when (result) {
        is MathValue.NumberVal -> {
            if (result.value.isNaN()) {
                null
            } else {
                val formatted = fmt(result.value)
                val isProb = ast is MathNode.CallNode && ast.funcName.lowercase(Locale.ROOT) in setOf("cdf", "survivalfunction")
                val displayValue = if (isProb) "= ${fmtProb(result.value)}" else "= $formatted"
                MathEvalOutcome(
                    title = expr,
                    primaryValue = displayValue,
                    badge = "Math",
                    subtitle = "",
                    copyText = formatted,
                    consoleOutput = console
                )
            }
        }
        is MathValue.StringVal -> {
            MathEvalOutcome(
                title = expr,
                primaryValue = result.text,
                badge = "String",
                subtitle = "",
                copyText = result.text,
                consoleOutput = console
            )
        }
        is MathValue.BooleanVal -> {
            val str = if (result.value) "True" else "False"
            MathEvalOutcome(
                title = expr,
                primaryValue = "= $str",
                badge = "Boolean",
                subtitle = "",
                copyText = str,
                consoleOutput = console
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
                copyText = fullList,
                consoleOutput = console
            )
        }
        is MathValue.MixedListVal -> {
            val display = result.toDisplayString()
            val matrix = result.toNumericMatrix()
            val badge = if (matrix != null) {
                "Matrix (${matrix.size}×${matrix[0].size})"
            } else {
                "List (${result.items.size})"
            }
            val subtitle = if (matrix != null) {
                val rows = matrix.size
                val cols = matrix[0].size
                if (rows == cols && rows <= 4) {
                    val tr = (0 until rows).sumOf { matrix[it][it] }
                    "Trace: ${fmt(tr)}"
                } else {
                    "${rows}×${cols} matrix"
                }
            } else {
                ""
            }
            MathEvalOutcome(
                title = expr,
                primaryValue = display,
                badge = badge,
                subtitle = subtitle,
                copyText = display,
                consoleOutput = console
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
                subtitle = "Median: $medStr • Var: $varStr",
                copyText = "${dist.name}[${dist.paramsString}]",
                consoleOutput = console
            )
        }
        is MathValue.ExprVal -> {
            MathEvalOutcome(
                title = expr,
                primaryValue = "= ${result.text}",
                badge = "Formula",
                subtitle = "",
                copyText = result.text,
                consoleOutput = console
            )
        }
        is MathValue.SymbolVal -> {
            MathEvalOutcome(
                title = expr,
                primaryValue = result.name,
                badge = "Symbol",
                subtitle = "",
                copyText = result.name,
                consoleOutput = console
            )
        }
        is MathValue.NullVal -> {
            if (console != null) {
                MathEvalOutcome(
                    title = expr,
                    primaryValue = "",
                    badge = "Print",
                    subtitle = "",
                    copyText = console,
                    consoleOutput = console
                )
            } else {
                MathEvalOutcome(
                    title = expr,
                    primaryValue = "= Null",
                    badge = "Done",
                    subtitle = "",
                    copyText = "Null",
                    consoleOutput = null
                )
            }
        }
        is MathValue.FunctionVal -> {
            MathEvalOutcome(
                title = expr,
                primaryValue = "Function[...]",
                badge = "Function",
                subtitle = "",
                copyText = expr,
                consoleOutput = console
            )
        }
        is MathValue.DelayedVal -> {
            MathEvalOutcome(
                title = expr,
                primaryValue = ":= ...",
                badge = "Rule",
                subtitle = "",
                copyText = expr,
                consoleOutput = console
            )
        }
        is MathValue.RuleVal -> {
            val display = result.toDisplayString()
            MathEvalOutcome(
                title = expr,
                primaryValue = "= $display",
                badge = "Rule",
                subtitle = "",
                copyText = display,
                consoleOutput = console
            )
        }
        is MathValue.ChartVal -> {
            val c = result.chart
            MathEvalOutcome(
                title = c.title,
                primaryValue = "",
                badge = c.badge,
                subtitle = c.summary,
                copyText = expr,
                chart = c,
                consoleOutput = console
            )
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
