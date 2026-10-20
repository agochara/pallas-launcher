package com.pallas.launcher.domain.search

import com.pallas.launcher.domain.search.MathStatsEngine.fmt
import java.util.Random
import kotlin.math.*

interface MathDistribution {
    val name: String
    val paramsString: String
    val badge: String?
    fun pdf(x: Double): Double
    fun cdf(x: Double): Double
    fun survival(x: Double): Double = (1.0 - cdf(x)).coerceIn(0.0, 1.0)
    fun hazard(x: Double): Double {
        val s = survival(x)
        return if (s > 0.0) pdf(x) / s else Double.POSITIVE_INFINITY
    }
    fun mean(): Double
    fun variance(): Double
    fun standardDeviation(): Double = sqrt(variance().coerceAtLeast(0.0))
    fun median(): Double
    fun quantile(q: Double): Double
    fun randomVariate(rng: Random): Double

    fun pdfSymbolic(varName: String): String = "PDF[$name[$paramsString], $varName]"
    fun cdfSymbolic(varName: String): String = "CDF[$name[$paramsString], $varName]"
    fun survivalSymbolic(varName: String): String = "SurvivalFunction[$name[$paramsString], $varName]"
    fun hazardSymbolic(varName: String): String = "HazardFunction[$name[$paramsString], $varName]"
}

// =========================================================================
// Special Math Functions
// =========================================================================

fun normalCdf(z: Double): Double = 0.5 * erfc(-z / sqrt(2.0))

fun erfc(x: Double): Double {
    val z = abs(x)
    val t = 1.0 / (1.0 + 0.5 * z)
    val r = t * exp(
        -z * z - 1.26551223 + t * (1.00002368 + t * (0.37409196 + t * (0.09678418 +
                t * (-0.18628806 + t * (0.27886807 + t * (-1.13520398 + t * (1.48851587 +
                t * (-0.82215223 + t * 0.17087277))))))))
    )
    return if (x >= 0.0) r else 2.0 - r
}

fun erf(x: Double): Double = 1.0 - erfc(x)

/** Rational approximation for inverse normal CDF (probit). Accurate to ~1e-9 */
fun normalQuantile(p: Double): Double {
    if (p <= 0.0) return Double.NEGATIVE_INFINITY
    if (p >= 1.0) return Double.POSITIVE_INFINITY
    if (abs(p - 0.5) < 1e-15) return 0.0

    val q = if (p < 0.5) p else 1.0 - p
    val r = sqrt(-2.0 * ln(q))
    val c0 = 2.515517
    val c1 = 0.802853
    val c2 = 0.010328
    val d1 = 1.432788
    val d2 = 0.189269
    val d3 = 0.001308
    val z = r - ((c2 * r + c1) * r + c0) / (((d3 * r + d2) * r + d1) * r + 1.0)
    return if (p < 0.5) -z else z
}

fun lnGamma(xx: Double): Double {
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

fun gamma(x: Double): Double = exp(lnGamma(x))

fun lnChoose(n: Int, k: Int): Double {
    if (k < 0 || k > n) return Double.NEGATIVE_INFINITY
    return lnGamma(n + 1.0) - lnGamma(k + 1.0) - lnGamma(n - k + 1.0)
}

fun regIncompleteBeta(x: Double, a: Double, b: Double): Double {
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
        val aa = m * (b - m) * x / ((qam + m2) * (a + m2))
        d = 1.0 + aa * d
        if (abs(d) < fpMin) d = fpMin
        c = 1.0 + aa / c
        if (abs(c) < fpMin) c = fpMin
        d = 1.0 / d
        h *= d * c
        val aa2 = -(a + m) * (qab + m) * x / ((a + m2) * (qap + m2))
        d = 1.0 + aa2 * d
        if (abs(d) < fpMin) d = fpMin
        c = 1.0 + aa2 / c
        if (abs(c) < fpMin) c = fpMin
        d = 1.0 / d
        val del = d * c
        h *= del
        if (abs(del - 1.0) < 3e-7) break
    }
    return h
}

fun studentTCdf(t: Double, df: Double): Double {
    val x = df / (df + t * t)
    val ib = 0.5 * regIncompleteBeta(x, df / 2.0, 0.5)
    return if (t >= 0.0) 1.0 - ib else ib
}

fun regIncompleteGamma(a: Double, x: Double): Double {
    if (a <= 0.0 || x <= 0.0) return 0.0
    return if (x < a + 1.0) {
        val gln = lnGamma(a)
        var ap = a
        var del = 1.0 / a
        var sum = del
        for (n in 1..200) {
            ap += 1.0
            del *= x / ap
            sum += del
            if (abs(del) < abs(sum) * 1e-15) break
        }
        sum * exp(-x + a * ln(x) - gln)
    } else {
        val gln = lnGamma(a)
        var b = x + 1.0 - a
        var c = 1.0 / 1e-30
        var d = 1.0 / b
        var h = d
        for (i in 1..200) {
            val an = -i * (i - a)
            b += 2.0
            d = an * d + b
            if (abs(d) < 1e-30) d = 1e-30
            c = b + an / c
            if (abs(c) < 1e-30) c = 1e-30
            d = 1.0 / d
            val del = d * c
            h *= del
            if (abs(del - 1.0) < 1e-15) break
        }
        1.0 - exp(-x + a * ln(x) - gln) * h
    }.coerceIn(0.0, 1.0)
}

fun gammaQuantile(q: Double, alpha: Double, beta: Double = 1.0): Double {
    if (q <= 0.0) return 0.0
    if (q >= 1.0) return Double.POSITIVE_INFINITY
    if (alpha <= 0.0 || beta <= 0.0) return Double.NaN

    val w = normalQuantile(q)
    var z = if (alpha >= 1.0) {
        val term = 1.0 - 1.0 / (9.0 * alpha) + w / sqrt(9.0 * alpha)
        alpha * term.pow(3).coerceAtLeast(1e-6)
    } else {
        val gln = lnGamma(alpha)
        val approx = exp((ln(q) + gln + ln(alpha)) / alpha)
        approx.coerceIn(1e-12, alpha * 2.0)
    }

    val gln = lnGamma(alpha)
    for (iter in 0..20) {
        val p = regIncompleteGamma(alpha, z)
        val f = p - q
        if (abs(f) < 1e-12) break
        val logPdf = (alpha - 1.0) * ln(z) - z - gln
        val fPrime = exp(logPdf).coerceAtLeast(1e-300)
        val fDoublePrime = fPrime * ((alpha - 1.0) / z - 1.0)
        val halleyDenom = fPrime - (f * fDoublePrime) / (2.0 * fPrime)
        val step = if (abs(halleyDenom) > 1e-15) f / halleyDenom else f / fPrime
        val newZ = z - step
        z = if (newZ <= 0.0) z * 0.5 else newZ
    }

    return z * beta
}

// =========================================================================
// Distribution Implementations
// =========================================================================

class NormalDist(val mu: Double = 0.0, val sigma: Double = 1.0) : MathDistribution {
    override val name: String = "NormalDistribution"
    override val paramsString: String = "μ=${fmt(mu)}, σ=${fmt(sigma)}"
    override val badge: String = "Gaussian"
    override fun pdf(x: Double): Double = exp(-0.5 * ((x - mu) / sigma).pow(2)) / (sigma * sqrt(2.0 * PI))
    override fun cdf(x: Double): Double = normalCdf((x - mu) / sigma)
    override fun mean(): Double = mu
    override fun variance(): Double = sigma * sigma
    override fun median(): Double = mu
    override fun quantile(q: Double): Double = mu + sigma * normalQuantile(q)
    override fun randomVariate(rng: Random): Double = mu + sigma * rng.nextGaussian()

    override fun pdfSymbolic(varName: String): String =
        if (mu == 0.0 && sigma == 1.0) "Exp[-$varName^2 / 2] / Sqrt[2 π]"
        else "Exp[-($varName - ${fmt(mu)})^2 / (2 * ${fmt(sigma * sigma)})] / (${fmt(sigma)} * Sqrt[2 π])"
    override fun cdfSymbolic(varName: String): String =
        if (mu == 0.0 && sigma == 1.0) "1/2 * (1 + Erf[$varName / Sqrt[2]])"
        else "1/2 * (1 + Erf[($varName - ${fmt(mu)}) / (${fmt(sigma)} * Sqrt[2])])"
}

class UniformDist(val min: Double = 0.0, val max: Double = 1.0) : MathDistribution {
    override val name: String = "UniformDistribution"
    override val paramsString: String = "min=${fmt(min)}, max=${fmt(max)}"
    override val badge: String = "Bounded Flat"
    override fun pdf(x: Double): Double = if (x in min..max) 1.0 / (max - min) else 0.0
    override fun cdf(x: Double): Double = when {
        x < min -> 0.0
        x > max -> 1.0
        else -> (x - min) / (max - min)
    }
    override fun mean(): Double = (min + max) / 2.0
    override fun variance(): Double = (max - min).pow(2) / 12.0
    override fun median(): Double = (min + max) / 2.0
    override fun quantile(q: Double): Double = min + q.coerceIn(0.0, 1.0) * (max - min)
    override fun randomVariate(rng: Random): Double = min + rng.nextDouble() * (max - min)

    override fun pdfSymbolic(varName: String): String =
        "${fmt(1.0 / (max - min))}  (for ${fmt(min)} <= $varName <= ${fmt(max)})"
    override fun cdfSymbolic(varName: String): String =
        "($varName - ${fmt(min)}) / ${fmt(max - min)}  (for ${fmt(min)} <= $varName <= ${fmt(max)})"
}

class ExponentialDist(val lambda: Double = 1.0) : MathDistribution {
    override val name: String = "ExponentialDistribution"
    override val paramsString: String = "λ=${fmt(lambda)}"
    override val badge: String = "Memoryless (λ=${fmt(lambda)})"
    override fun pdf(x: Double): Double = if (x < 0.0) 0.0 else lambda * exp(-lambda * x)
    override fun cdf(x: Double): Double = if (x < 0.0) 0.0 else 1.0 - exp(-lambda * x)
    override fun survival(x: Double): Double = if (x < 0.0) 1.0 else exp(-lambda * x)
    override fun hazard(x: Double): Double = if (x < 0.0) 0.0 else lambda
    override fun mean(): Double = 1.0 / lambda
    override fun variance(): Double = 1.0 / (lambda * lambda)
    override fun median(): Double = ln(2.0) / lambda
    override fun quantile(q: Double): Double = -ln(1.0 - q.coerceIn(0.0, 0.9999999)) / lambda
    override fun randomVariate(rng: Random): Double = -ln(1.0 - rng.nextDouble()) / lambda

    override fun pdfSymbolic(varName: String): String =
        "${fmt(lambda)} * Exp[-${fmt(lambda)} * $varName]  (for $varName >= 0)"
    override fun cdfSymbolic(varName: String): String =
        "1 - Exp[-${fmt(lambda)} * $varName]  (for $varName >= 0)"
    override fun survivalSymbolic(varName: String): String =
        "Exp[-${fmt(lambda)} * $varName]  (for $varName >= 0)"
    override fun hazardSymbolic(varName: String): String =
        fmt(lambda)
}

class PoissonDist(val mu: Double) : MathDistribution {
    override val name: String = "PoissonDistribution"
    override val paramsString: String = "μ=${fmt(mu)}"
    override val badge: String = "Discrete Poisson"
    override fun pdf(x: Double): Double {
        val k = kotlin.math.round(x).toInt()
        if (k < 0) return 0.0
        return exp(k * ln(mu) - mu - lnGamma(k + 1.0))
    }
    override fun cdf(x: Double): Double {
        val k = kotlin.math.floor(x).toInt()
        if (k < 0) return 0.0
        var s = 0.0
        for (i in 0..k) s += pdf(i.toDouble())
        return s.coerceAtMost(1.0)
    }
    override fun mean(): Double = mu
    override fun variance(): Double = mu
    override fun median(): Double = kotlin.math.floor(mu + 1.0 / 3.0 - 0.02 / mu)
    override fun quantile(q: Double): Double {
        var sum = 0.0
        var k = 0
        while (k < 10000) {
            sum += pdf(k.toDouble())
            if (sum >= q) return k.toDouble()
            k++
        }
        return k.toDouble()
    }
    override fun randomVariate(rng: Random): Double {
        val l = exp(-mu)
        var k = 0
        var p = 1.0
        do {
            k++
            p *= rng.nextDouble()
        } while (p > l)
        return (k - 1).toDouble()
    }
}

class BinomialDist(val n: Int, val p: Double) : MathDistribution {
    override val name: String = "BinomialDistribution"
    override val paramsString: String = "n=$n, p=${fmt(p)}"
    override val badge: String = "Bernoulli Trials"
    override fun pdf(x: Double): Double {
        val k = kotlin.math.round(x).toInt()
        if (k !in 0..n) return 0.0
        return when {
            p == 0.0 -> if (k == 0) 1.0 else 0.0
            p == 1.0 -> if (k == n) 1.0 else 0.0
            else -> exp(lnChoose(n, k) + k * ln(p) + (n - k) * ln(1.0 - p))
        }
    }
    override fun cdf(x: Double): Double {
        val k = kotlin.math.floor(x).toInt()
        if (k < 0) return 0.0
        if (k >= n) return 1.0
        var s = 0.0
        for (i in 0..k) s += pdf(i.toDouble())
        return s.coerceAtMost(1.0)
    }
    override fun mean(): Double = n * p
    override fun variance(): Double = n * p * (1.0 - p)
    override fun median(): Double = kotlin.math.round(n * p)
    override fun quantile(q: Double): Double {
        var sum = 0.0
        for (i in 0..n) {
            sum += pdf(i.toDouble())
            if (sum >= q) return i.toDouble()
        }
        return n.toDouble()
    }
    override fun randomVariate(rng: Random): Double {
        var successes = 0
        for (i in 1..n) {
            if (rng.nextDouble() < p) successes++
        }
        return successes.toDouble()
    }
}

class StudentTDist(val nu: Double = 1.0) : MathDistribution {
    override val name: String = "StudentTDistribution"
    override val paramsString: String = "ν=${fmt(nu)}"
    override val badge: String = if (nu <= 2.0) "Fat Tail (df≤2)" else "Power Law (df=${fmt(nu)})"
    override fun pdf(x: Double): Double = exp(lnGamma((nu + 1.0) / 2.0) - lnGamma(nu / 2.0)) /
            sqrt(nu * PI) * (1.0 + (x * x) / nu).pow(-(nu + 1.0) / 2.0)
    override fun cdf(x: Double): Double = studentTCdf(x, nu)
    override fun mean(): Double = if (nu > 1.0) 0.0 else Double.NaN
    override fun variance(): Double = when {
        nu > 2.0 -> nu / (nu - 2.0)
        nu > 1.0 -> Double.POSITIVE_INFINITY
        else -> Double.NaN
    }
    override fun median(): Double = 0.0
    override fun quantile(q: Double): Double {
        var x = normalQuantile(q)
        for (iter in 0..10) {
            val f = cdf(x) - q
            val dfVal = pdf(x)
            if (abs(f) < 1e-8 || dfVal == 0.0) break
            x -= f / dfVal
        }
        return x
    }
    override fun randomVariate(rng: Random): Double {
        val z = rng.nextGaussian()
        val v = GammaDist(nu / 2.0, 2.0).randomVariate(rng)
        return z / sqrt(v / nu)
    }

    override fun pdfSymbolic(varName: String): String =
        "Γ[${fmt((nu + 1.0) / 2.0)}] / (Sqrt[${fmt(nu)} π] * Γ[${fmt(nu / 2.0)}]) * (1 + $varName^2 / ${fmt(nu)})^(-${fmt((nu + 1.0) / 2.0)})"
}

class CauchyDist(val a: Double = 0.0, val b: Double = 1.0) : MathDistribution {
    override val name: String = "CauchyDistribution"
    override val paramsString: String = "x0=${fmt(a)}, γ=${fmt(b)}"
    override val badge: String = "Stable α=1"
    override fun pdf(x: Double): Double = 1.0 / (PI * b * (1.0 + ((x - a) / b).pow(2)))
    override fun cdf(x: Double): Double = (1.0 / PI) * atan((x - a) / b) + 0.5
    override fun mean(): Double = Double.NaN
    override fun variance(): Double = Double.NaN
    override fun median(): Double = a
    override fun quantile(q: Double): Double = a + b * tan(PI * (q - 0.5))
    override fun randomVariate(rng: Random): Double = a + b * tan(PI * (rng.nextDouble() - 0.5))

    override fun pdfSymbolic(varName: String): String =
        "1 / (π * ${fmt(b)} * (1 + (($varName - ${fmt(a)}) / ${fmt(b)})^2))"
    override fun cdfSymbolic(varName: String): String =
        "1/2 + (1 / π) * ArcTan[($varName - ${fmt(a)}) / ${fmt(b)}]"
}

class ParetoDist(val k: Double = 1.0, val alpha: Double = 1.0) : MathDistribution {
    override val name: String = "ParetoDistribution"
    override val paramsString: String = "k=${fmt(k)}, α=${fmt(alpha)}"
    override val badge: String = if (alpha <= 2.0) "Fat Tail (α≤2)" else "Power Law"
    override fun pdf(x: Double): Double = if (x < k) 0.0 else (alpha * k.pow(alpha)) / x.pow(alpha + 1.0)
    override fun cdf(x: Double): Double = if (x < k) 0.0 else 1.0 - (k / x).pow(alpha)
    override fun survival(x: Double): Double = if (x < k) 1.0 else (k / x).pow(alpha)
    override fun hazard(x: Double): Double = if (x < k) 0.0 else alpha / x
    override fun mean(): Double = if (alpha > 1.0) alpha * k / (alpha - 1.0) else Double.POSITIVE_INFINITY
    override fun variance(): Double = if (alpha > 2.0) (k * k * alpha) / ((alpha - 1.0).pow(2) * (alpha - 2.0)) else Double.POSITIVE_INFINITY
    override fun median(): Double = k * (2.0).pow(1.0 / alpha)
    override fun quantile(q: Double): Double = k / (1.0 - q.coerceIn(0.0, 0.9999999)).pow(1.0 / alpha)
    override fun randomVariate(rng: Random): Double = k / (1.0 - rng.nextDouble()).pow(1.0 / alpha)

    override fun pdfSymbolic(varName: String): String =
        if (k == 1.0) "${fmt(alpha)} / $varName^${fmt(alpha + 1.0)}  (for $varName >= 1)"
        else "(${fmt(alpha * k.pow(alpha))}) / $varName^${fmt(alpha + 1.0)}  (for $varName >= ${fmt(k)})"
    override fun cdfSymbolic(varName: String): String =
        if (k == 1.0) "1 - 1 / $varName^${fmt(alpha)}  (for $varName >= 1)"
        else "1 - (${fmt(k)} / $varName)^${fmt(alpha)}  (for $varName >= ${fmt(k)})"
    override fun survivalSymbolic(varName: String): String =
        if (k == 1.0) "1 / $varName^${fmt(alpha)}  (for $varName >= 1)"
        else "(${fmt(k)} / $varName)^${fmt(alpha)}  (for $varName >= ${fmt(k)})"
    override fun hazardSymbolic(varName: String): String =
        "${fmt(alpha)} / $varName  (for $varName >= ${fmt(k)})"
}

class LevyDist(val mu: Double = 0.0, val c: Double = 1.0) : MathDistribution {
    override val name: String = "LevyDistribution"
    override val paramsString: String = "μ=${fmt(mu)}, c=${fmt(c)}"
    override val badge: String = "Stable α=0.5"
    override fun pdf(x: Double): Double = if (x <= mu) 0.0 else sqrt(c / (2.0 * PI)) * exp(-c / (2.0 * (x - mu))) / (x - mu).pow(1.5)
    override fun cdf(x: Double): Double = if (x <= mu) 0.0 else erfc(sqrt(c / (2.0 * (x - mu))))
    override fun mean(): Double = Double.POSITIVE_INFINITY
    override fun variance(): Double = Double.POSITIVE_INFINITY
    override fun median(): Double = mu + c / (2.0 * 0.4549364 * 0.4549364)
    override fun quantile(q: Double): Double {
        val z = normalQuantile(1.0 - q / 2.0)
        return mu + c / (z * z)
    }
    override fun randomVariate(rng: Random): Double {
        val z = rng.nextGaussian()
        val safeZ = if (abs(z) < 1e-6) 1e-6 else z
        return mu + c / (safeZ * safeZ)
    }
}

class FrechetDist(val alpha: Double, val s: Double = 1.0, val m: Double = 0.0) : MathDistribution {
    override val name: String = "FrechetDistribution"
    override val paramsString: String = "α=${fmt(alpha)}, s=${fmt(s)}, m=${fmt(m)}"
    override val badge: String = "EVD Type II"
    override fun pdf(x: Double): Double {
        if (x <= m) return 0.0
        val z = (x - m) / s
        return (alpha / s) * z.pow(-1.0 - alpha) * exp(-z.pow(-alpha))
    }
    override fun cdf(x: Double): Double {
        if (x <= m) return 0.0
        val z = (x - m) / s
        return exp(-z.pow(-alpha))
    }
    override fun mean(): Double = if (alpha > 1.0) m + s * gamma(1.0 - 1.0 / alpha) else Double.POSITIVE_INFINITY
    override fun variance(): Double = if (alpha > 2.0) s * s * (gamma(1.0 - 2.0 / alpha) - gamma(1.0 - 1.0 / alpha).pow(2)) else Double.POSITIVE_INFINITY
    override fun median(): Double = m + s * (ln(2.0)).pow(-1.0 / alpha)
    override fun quantile(q: Double): Double = m + s * (-ln(q.coerceIn(1e-9, 0.9999999))).pow(-1.0 / alpha)
    override fun randomVariate(rng: Random): Double = quantile(rng.nextDouble())
}

class WeibullDist(val alpha: Double, val beta: Double = 1.0) : MathDistribution {
    override val name: String = "WeibullDistribution"
    override val paramsString: String = "k=${fmt(alpha)}, λ=${fmt(beta)}"
    override val badge: String = if (alpha < 1.0) "Subexponential" else "Thin Tail"
    override fun pdf(x: Double): Double = if (x < 0.0) 0.0 else (alpha / beta) * (x / beta).pow(alpha - 1.0) * exp(-(x / beta).pow(alpha))
    override fun cdf(x: Double): Double = if (x < 0.0) 0.0 else 1.0 - exp(-(x / beta).pow(alpha))
    override fun survival(x: Double): Double = if (x < 0.0) 1.0 else exp(-(x / beta).pow(alpha))
    override fun mean(): Double = beta * gamma(1.0 + 1.0 / alpha)
    override fun variance(): Double = beta * beta * (gamma(1.0 + 2.0 / alpha) - gamma(1.0 + 1.0 / alpha).pow(2))
    override fun median(): Double = beta * (ln(2.0)).pow(1.0 / alpha)
    override fun quantile(q: Double): Double = beta * (-ln(1.0 - q.coerceIn(0.0, 0.9999999))).pow(1.0 / alpha)
    override fun randomVariate(rng: Random): Double = quantile(rng.nextDouble())
}

class LogNormalDist(val mu: Double = 0.0, val sigma: Double = 1.0) : MathDistribution {
    override val name: String = "LogNormalDistribution"
    override val paramsString: String = "μ=${fmt(mu)}, σ=${fmt(sigma)}"
    override val badge: String = "Subexponential"
    override fun pdf(x: Double): Double = if (x <= 0.0) 0.0 else exp(-0.5 * ((ln(x) - mu) / sigma).pow(2)) / (x * sigma * sqrt(2.0 * PI))
    override fun cdf(x: Double): Double = if (x <= 0.0) 0.0 else normalCdf((ln(x) - mu) / sigma)
    override fun mean(): Double = exp(mu + 0.5 * sigma * sigma)
    override fun variance(): Double = (exp(sigma * sigma) - 1.0) * exp(2.0 * mu + sigma * sigma)
    override fun median(): Double = exp(mu)
    override fun quantile(q: Double): Double = exp(mu + sigma * normalQuantile(q))
    override fun randomVariate(rng: Random): Double = exp(mu + sigma * rng.nextGaussian())
}

class GammaDist(val alpha: Double = 1.0, val beta: Double = 1.0) : MathDistribution {
    override val name: String = "GammaDistribution"
    override val paramsString: String = "α=${fmt(alpha)}, β=${fmt(beta)}"
    override val badge: String = "Gamma"
    override fun pdf(x: Double): Double = if (x <= 0.0) 0.0 else (1.0 / (beta.pow(alpha) * gamma(alpha))) * x.pow(alpha - 1.0) * exp(-x / beta)
    override fun cdf(x: Double): Double = if (x <= 0.0) 0.0 else regIncompleteGamma(alpha, x / beta)
    override fun mean(): Double = alpha * beta
    override fun variance(): Double = alpha * beta * beta
    override fun median(): Double = quantile(0.5)
    override fun quantile(q: Double): Double = gammaQuantile(q, alpha, beta)
    override fun randomVariate(rng: Random): Double {
        val a = if (alpha < 1.0) alpha + 1.0 else alpha
        val d = a - 1.0 / 3.0
        val c = 1.0 / sqrt(9.0 * d)
        var v: Double
        var x: Double
        while (true) {
            x = rng.nextGaussian()
            v = 1.0 + c * x
            if (v <= 0.0) continue
            v = v * v * v
            val u = rng.nextDouble()
            if (u < 1.0 - 0.0331 * x * x * x * x) break
            if (ln(u) < 0.5 * x * x + d * (1.0 - v + ln(v))) break
        }
        val res = d * v
        return if (alpha < 1.0) (res * rng.nextDouble().pow(1.0 / alpha)) * beta else res * beta
    }

    override fun pdfSymbolic(varName: String): String =
        "($varName^${fmt(alpha - 1.0)} * Exp[-$varName / ${fmt(beta)}]) / (${fmt(beta.pow(alpha))} * Γ[${fmt(alpha)}])  (for $varName > 0)"
    override fun cdfSymbolic(varName: String): String =
        "P[${fmt(alpha)}, $varName / ${fmt(beta)}]"
}

class BetaDist(val alpha: Double, val beta: Double) : MathDistribution {
    override val name: String = "BetaDistribution"
    override val paramsString: String = "α=${fmt(alpha)}, β=${fmt(beta)}"
    override val badge: String = "Beta"
    override fun pdf(x: Double): Double = if (x !in 0.0..1.0) 0.0 else exp(lnGamma(alpha + beta) - lnGamma(alpha) - lnGamma(beta)) * x.pow(alpha - 1.0) * (1.0 - x).pow(beta - 1.0)
    override fun cdf(x: Double): Double = regIncompleteBeta(x, alpha, beta)
    override fun mean(): Double = alpha / (alpha + beta)
    override fun variance(): Double = (alpha * beta) / ((alpha + beta).pow(2) * (alpha + beta + 1.0))
    override fun median(): Double = (alpha - 1.0 / 3.0) / (alpha + beta - 2.0 / 3.0)
    override fun quantile(q: Double): Double {
        var x = mean()
        for (i in 0..15) {
            val f = cdf(x) - q
            val df = pdf(x)
            if (abs(f) < 1e-7 || df == 0.0) break
            x = (x - f / df).coerceIn(0.0001, 0.9999)
        }
        return x
    }
    override fun randomVariate(rng: Random): Double {
        val g1 = GammaDist(alpha, 1.0).randomVariate(rng)
        val g2 = GammaDist(beta, 1.0).randomVariate(rng)
        return g1 / (g1 + g2)
    }
}

class GeometricDist(val p: Double) : MathDistribution {
    override val name: String = "GeometricDistribution"
    override val paramsString: String = "p=${fmt(p)}"
    override val badge: String = "Discrete Geometric"
    override fun pdf(x: Double): Double {
        val k = kotlin.math.round(x).toInt()
        return if (k < 1) 0.0 else (1.0 - p).pow(k - 1) * p
    }
    override fun cdf(x: Double): Double {
        val k = kotlin.math.floor(x).toInt()
        return if (k < 1) 0.0 else 1.0 - (1.0 - p).pow(k)
    }
    override fun mean(): Double = 1.0 / p
    override fun variance(): Double = (1.0 - p) / (p * p)
    override fun median(): Double = kotlin.math.ceil(-1.0 / (ln(1.0 - p) / ln(2.0)))
    override fun quantile(q: Double): Double = kotlin.math.ceil(ln(1.0 - q) / ln(1.0 - p))
    override fun randomVariate(rng: Random): Double = kotlin.math.ceil(ln(1.0 - rng.nextDouble()) / ln(1.0 - p))
}
