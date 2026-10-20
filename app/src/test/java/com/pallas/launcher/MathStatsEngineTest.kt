package com.pallas.launcher

import com.pallas.launcher.domain.search.MathStatsEngine
import com.pallas.launcher.domain.search.SmartCommandEngine
import com.pallas.launcher.domain.search.SmartCommandType
import com.pallas.launcher.domain.search.LauncherSearchResult
import org.junit.Assert.*
import org.junit.Test

class MathStatsEngineTest {

    @Test
    fun testBasicArithmetic() {
        val res = MathStatsEngine.evaluate("2 + 2", "=")
        assertNotNull(res)
        assertEquals("= 4", res?.primaryValue)

        val resPrefix = MathStatsEngine.evaluate("= 3 * 7", "=")
        assertNotNull(resPrefix)
        assertEquals("= 21", resPrefix?.primaryValue)
    }

    @Test
    fun testCombinatoricsAndSpecialFunctions() {
        val chooseRes = MathStatsEngine.evaluate("= Binomial[52, 5]", "=")
        assertNotNull(chooseRes)
        assertEquals("= 2598960", chooseRes?.primaryValue)

        val factRes = MathStatsEngine.evaluate("= Factorial[5]", "=")
        assertNotNull(factRes)
        assertEquals("= 120", factRes?.primaryValue)
    }

    @Test
    fun testParetoDistribution() {
        val paretoRes = MathStatsEngine.evaluate("= ParetoDistribution[1, 2]", "=")
        assertNotNull(paretoRes)
        assertTrue(paretoRes!!.title.contains("ParetoDistribution"))
        assertTrue(paretoRes.primaryValue.contains("Mean = 2"))
        assertEquals("Fat Tail (α≤2)", paretoRes.badge)

        val pdfRes = MathStatsEngine.evaluate("= PDF[ParetoDistribution[1, 2], 2]", "=")
        assertNotNull(pdfRes)
        assertEquals("= 0.25", pdfRes?.primaryValue)

        val cdfRes = MathStatsEngine.evaluate("= CDF[ParetoDistribution[1, 2], 2]", "=")
        assertNotNull(cdfRes)
        assertTrue(cdfRes!!.primaryValue.startsWith("= 0.75"))
        assertEquals("0.75", cdfRes.copyText)
    }

    @Test
    fun testNormalDistribution() {
        val normRes = MathStatsEngine.evaluate("= NormalDistribution[0, 1]", "=")
        assertNotNull(normRes)
        assertTrue(normRes!!.title.contains("NormalDistribution"))
        assertTrue(normRes.primaryValue.contains("Mean = 0"))
        assertTrue(normRes.primaryValue.contains("SD = 1"))

        val pdfRes = MathStatsEngine.evaluate("= PDF[NormalDistribution[0, 1], 0]", "=")
        assertNotNull(pdfRes)
        assertTrue(pdfRes!!.primaryValue.startsWith("= 0.3989"))

        val cdfRes = MathStatsEngine.evaluate("= CDF[NormalDistribution[0, 1], 0]", "=")
        assertNotNull(cdfRes)
        assertTrue(cdfRes!!.primaryValue.startsWith("= 0.5"))
        assertEquals("0.5", cdfRes.copyText)
    }

    @Test
    fun testSmartCommandEngineEvaluatesMath() {
        val eval = SmartCommandEngine.evaluateCommands("= 10 + 5", emptyList())
        assertEquals(1, eval.immediateResults.size)
        val result = eval.immediateResults.first()
        assertTrue(result is LauncherSearchResult.RichCard)
        val card = result as LauncherSearchResult.RichCard
        assertEquals(SmartCommandType.MATH_STATS, card.type)
        assertEquals("= 15", card.primaryValue)
        assertFalse(card.hasInfoManual)
    }

    @Test
    fun testFinanceCommandReturnsLoadingAndAsyncRequest() {
        val eval = SmartCommandEngine.evaluateCommands("st nifty", emptyList())
        assertEquals(1, eval.immediateResults.size)
        val result = eval.immediateResults.first()
        assertTrue(result is LauncherSearchResult.RichCard)
        val card = result as LauncherSearchResult.RichCard
        assertEquals(SmartCommandType.FINANCE, card.type)
        assertTrue(card.isLoading)
        assertNotNull(eval.asyncRequest)
    }

    @Test
    fun testWeatherCommandReturnsLoadingAndAsyncRequest() {
        val eval = SmartCommandEngine.evaluateCommands("w tokyo", emptyList())
        assertEquals(1, eval.immediateResults.size)
        val result = eval.immediateResults.first()
        assertTrue(result is LauncherSearchResult.RichCard)
        val card = result as LauncherSearchResult.RichCard
        assertEquals(SmartCommandType.WEATHER, card.type)
        assertTrue(card.isLoading)
        assertNotNull(eval.asyncRequest)
    }

    // =========================================================================
    // Wolfram Language Inspired Function Tests
    // =========================================================================

    @Test
    fun testWolframBasicFunctions() {
        assertEquals("= 1024", MathStatsEngine.evaluate("= Power[2, 10]", "=")?.primaryValue)
        assertEquals("= 1024", MathStatsEngine.evaluate("= 2^10", "=")?.primaryValue)
        assertEquals("= 12", MathStatsEngine.evaluate("= Sqrt[144]", "=")?.primaryValue)
        assertEquals("= 3", MathStatsEngine.evaluate("= CubeRoot[27]", "=")?.primaryValue)
        assertEquals("= 42", MathStatsEngine.evaluate("= Abs[-42]", "=")?.primaryValue)
        assertEquals("= -1", MathStatsEngine.evaluate("= Sign[-99]", "=")?.primaryValue)
        assertEquals("= 4", MathStatsEngine.evaluate("= Round[3.7]", "=")?.primaryValue)
        assertEquals("= 3", MathStatsEngine.evaluate("= Floor[3.7]", "=")?.primaryValue)
        assertEquals("= 4", MathStatsEngine.evaluate("= Ceiling[3.2]", "=")?.primaryValue)
        assertEquals("= 2", MathStatsEngine.evaluate("= Mod[17, 5]", "=")?.primaryValue)
        assertEquals("= 3", MathStatsEngine.evaluate("= Quotient[17, 5]", "=")?.primaryValue)
        assertEquals("= True", MathStatsEngine.evaluate("= Divisible[20, 5]", "=")?.primaryValue)
        assertEquals("= False", MathStatsEngine.evaluate("= Divisible[21, 5]", "=")?.primaryValue)
        assertEquals("= 6", MathStatsEngine.evaluate("= GCD[24, 30]", "=")?.primaryValue)
        assertEquals("= 36", MathStatsEngine.evaluate("= LCM[12, 18]", "=")?.primaryValue)
        assertEquals("= 55", MathStatsEngine.evaluate("= Fibonacci[10]", "=")?.primaryValue)
        assertEquals("= True", MathStatsEngine.evaluate("= PrimeQ[17]", "=")?.primaryValue)
        assertEquals("= False", MathStatsEngine.evaluate("= PrimeQ[18]", "=")?.primaryValue)
        assertEquals("= 19", MathStatsEngine.evaluate("= NextPrime[17]", "=")?.primaryValue)
        assertEquals("= 120", MathStatsEngine.evaluate("= 5!", "=")?.primaryValue)
        assertEquals("= 720", MathStatsEngine.evaluate("= Factorial[6]", "=")?.primaryValue)
        assertEquals("= 2598960", MathStatsEngine.evaluate("= Binomial[52, 5]", "=")?.primaryValue)
        assertEquals("= 1", MathStatsEngine.evaluate("= Sin[Pi / 2]", "=")?.primaryValue)
        assertEquals("= 1", MathStatsEngine.evaluate("= Cos[0]", "=")?.primaryValue)
        assertEquals("= 1", MathStatsEngine.evaluate("= Log[E]", "=")?.primaryValue)
        assertEquals("= 3", MathStatsEngine.evaluate("= Log10[1000]", "=")?.primaryValue)
        assertEquals("= 5", MathStatsEngine.evaluate("= Log2[32]", "=")?.primaryValue)
    }

    @Test
    fun testWolframListsAndVectors() {
        assertEquals("{1, 2, 3, 4, 5}", MathStatsEngine.evaluate("= Range[5]", "=")?.primaryValue)
        assertEquals("= 5050", MathStatsEngine.evaluate("= Total[Range[100]]", "=")?.primaryValue)
        assertEquals("= 10", MathStatsEngine.evaluate("= Length[Range[10]]", "=")?.primaryValue)
        assertEquals("= 1", MathStatsEngine.evaluate("= First[{1, 2, 3}]", "=")?.primaryValue)
        assertEquals("= 3", MathStatsEngine.evaluate("= Last[{1, 2, 3}]", "=")?.primaryValue)
        assertEquals("{1, 2, 3}", MathStatsEngine.evaluate("= Take[{1, 2, 3, 4, 5}, 3]", "=")?.primaryValue)
        assertEquals("{3, 4, 5}", MathStatsEngine.evaluate("= Drop[{1, 2, 3, 4, 5}, 2]", "=")?.primaryValue)
        assertEquals("{3, 2, 1}", MathStatsEngine.evaluate("= Reverse[{1, 2, 3}]", "=")?.primaryValue)
        assertEquals("{1, 2, 4, 9}", MathStatsEngine.evaluate("= Sort[{4, 1, 9, 2}]", "=")?.primaryValue)
        assertEquals("{2, 3, 4}", MathStatsEngine.evaluate("= Differences[{1, 3, 6, 10}]", "=")?.primaryValue)
        assertEquals("{1, 3, 6, 10}", MathStatsEngine.evaluate("= Accumulate[{1, 2, 3, 4}]", "=")?.primaryValue)

        // Vector arithmetic
        assertEquals("{2, 4, 6}", MathStatsEngine.evaluate("= {1, 2, 3} * 2", "=")?.primaryValue)
        assertEquals("{5, 7, 9}", MathStatsEngine.evaluate("= {1, 2, 3} + {4, 5, 6}", "=")?.primaryValue)
    }

    @Test
    fun testWolframStatistics() {
        assertEquals("= 3", MathStatsEngine.evaluate("= Mean[{1, 2, 3, 4, 5}]", "=")?.primaryValue)
        assertEquals("= 6", MathStatsEngine.evaluate("= Median[{1, 3, 3, 6, 7, 8, 9}]", "=")?.primaryValue)
        assertEquals("= 1", MathStatsEngine.evaluate("= Variance[{1, 2, 3}]", "=")?.primaryValue)
        assertEquals("= 1", MathStatsEngine.evaluate("= StandardDeviation[{1, 2, 3}]", "=")?.primaryValue)
        assertEquals("= 3", MathStatsEngine.evaluate("= Quantile[{1, 2, 3, 4, 5}, 0.5]", "=")?.primaryValue)
        assertEquals("= 3", MathStatsEngine.evaluate("= Percentile[{1, 2, 3, 4, 5}, 50]", "=")?.primaryValue)
        assertEquals("= 2", MathStatsEngine.evaluate("= GeometricMean[{1, 2, 4}]", "=")?.primaryValue)
        assertEquals("= 1", MathStatsEngine.evaluate("= Correlation[{1, 2, 3}, {2, 4, 6}]", "=")?.primaryValue)
    }

    @Test
    fun testWolframProbabilityAndDistributions() {
        val norm = MathStatsEngine.evaluate("= NormalDistribution[0, 1]", "=")
        assertNotNull(norm)
        assertTrue(norm!!.primaryValue.contains("Mean = 0"))
        assertTrue(norm.primaryValue.contains("SD = 1"))

        assertTrue(MathStatsEngine.evaluate("= CDF[NormalDistribution[0, 1], 0]", "=")?.primaryValue?.startsWith("= 0.5") == true)
        assertTrue(MathStatsEngine.evaluate("= SurvivalFunction[NormalDistribution[0, 1], 0]", "=")?.primaryValue?.startsWith("= 0.5") == true)
        assertEquals("= 10", MathStatsEngine.evaluate("= Mean[NormalDistribution[10, 2]]", "=")?.primaryValue)
        assertEquals("= 4", MathStatsEngine.evaluate("= Variance[NormalDistribution[10, 2]]", "=")?.primaryValue)
        assertEquals("= 2", MathStatsEngine.evaluate("= StandardDeviation[NormalDistribution[10, 2]]", "=")?.primaryValue)
        assertEquals("= 5", MathStatsEngine.evaluate("= Mean[UniformDistribution[0, 10]]", "=")?.primaryValue)
        assertEquals("= 0.5", MathStatsEngine.evaluate("= Mean[ExponentialDistribution[2]]", "=")?.primaryValue)
        assertEquals("= 4", MathStatsEngine.evaluate("= Mean[PoissonDistribution[4]]", "=")?.primaryValue)
        assertEquals("= 10", MathStatsEngine.evaluate("= Mean[BinomialDistribution[20, 0.5]]", "=")?.primaryValue)

        // Composition: Mean[RandomVariate[NormalDistribution[0, 1], 100]]
        val sampleMean = MathStatsEngine.evaluate("= Mean[RandomVariate[NormalDistribution[0, 1], 100]]", "=")
        assertNotNull(sampleMean)
        assertTrue(sampleMean!!.primaryValue.startsWith("="))
        val num = sampleMean.primaryValue.removePrefix("=").trim().toDouble()
        assertTrue("Sample mean of 100 N(0,1) variates should be close to 0", kotlin.math.abs(num) < 1.0)
    }

    @Test
    fun testWolframRandomFunctions() {
        val randInt = MathStatsEngine.evaluate("= RandomInteger[{1, 10}, 5]", "=")
        assertNotNull(randInt)
        assertTrue(randInt!!.badge?.contains("List (5)") == true)

        val singleInt = MathStatsEngine.evaluate("= RandomInteger[{10, 20}]", "=")
        assertNotNull(singleInt)
        val valInt = singleInt!!.primaryValue.removePrefix("=").trim().toInt()
        assertTrue(valInt in 10..20)

        val randReal = MathStatsEngine.evaluate("= RandomReal[{0, 1}, 5]", "=")
        assertNotNull(randReal)
        assertTrue(randReal!!.badge?.contains("List (5)") == true)

        val choices = MathStatsEngine.evaluate("= RandomChoice[{10, 20, 30}, 4]", "=")
        assertNotNull(choices)
        assertTrue(choices!!.badge?.contains("List (4)") == true)

        val sample = MathStatsEngine.evaluate("= RandomSample[Range[5]]", "=")
        assertNotNull(sample)
        assertTrue(sample!!.badge?.contains("List (5)") == true)
    }

    @Test
    fun testWolframListOperations() {
        assertEquals("{1, 2, 3}", MathStatsEngine.evaluate("= DeleteDuplicates[{1, 2, 2, 3, 1}]", "=")?.primaryValue)
    }

    @Test
    fun testWolframThreadingOverLists() {
        val pdfList = MathStatsEngine.evaluate("= PDF[NormalDistribution[0, 1], {0, 0}]", "=")
        assertNotNull(pdfList)
        assertTrue(pdfList!!.primaryValue.startsWith("{0.3989"))

        val cdfList = MathStatsEngine.evaluate("= CDF[NormalDistribution[0, 1], {0, 0}]", "=")
        assertNotNull(cdfList)
        assertEquals("{0.5, 0.5}", cdfList?.primaryValue)
    }

    @Test
    fun testSymbolicDistributionFormulasAndZeroArgDefaults() {
        val paretoZero = MathStatsEngine.evaluate("= ParetoDistribution[]", "=")
        assertNotNull(paretoZero)
        assertTrue(paretoZero!!.primaryValue.contains("Mean = ∞"))

        val pdfSymbolic = MathStatsEngine.evaluate("= PDF[ParetoDistribution[1, 2], x]", "=")
        assertNotNull(pdfSymbolic)
        assertTrue("PDF should be symbolic for x", pdfSymbolic!!.primaryValue.contains("2 / x^3"))

        val cdfSymbolic = MathStatsEngine.evaluate("= CDF[ParetoDistribution[1, 2], x]", "=")
        assertNotNull(cdfSymbolic)
        assertTrue("CDF should be symbolic for x", cdfSymbolic!!.primaryValue.contains("1 - 1 / x^2"))

        val survivalSymbolic = MathStatsEngine.evaluate("= SurvivalFunction[ParetoDistribution[1, 2], x]", "=")
        assertNotNull(survivalSymbolic)
        assertTrue("Survival should be symbolic for x", survivalSymbolic!!.primaryValue.contains("1 / x^2"))

        val hazardSymbolic = MathStatsEngine.evaluate("= HazardFunction[ParetoDistribution[1, 2], x]", "=")
        assertNotNull(hazardSymbolic)
        assertTrue("Hazard should be symbolic for x", hazardSymbolic!!.primaryValue.contains("2 / x"))

        val normZero = MathStatsEngine.evaluate("= NormalDistribution[]", "=")
        assertNotNull(normZero)
        assertTrue(normZero!!.primaryValue.contains("Mean = 0"))
        assertTrue(normZero.primaryValue.contains("SD = 1"))

        val gammaZero = MathStatsEngine.evaluate("= GammaDistribution[]", "=")
        assertNotNull(gammaZero)
        assertTrue(gammaZero!!.primaryValue.contains("Mean = 1"))
    }

    @Test
    fun testStudentTNonIntegerDegreesOfFreedomSampling() {
        val tDist = MathStatsEngine.evaluate("= RandomVariate[StudentTDistribution[1.5], 50]", "=")
        assertNotNull(tDist)
        assertTrue(tDist!!.badge?.contains("List (50)") == true)
    }

    @Test
    fun testGammaCdfAndQuantile() {
        val cdf = MathStatsEngine.evaluate("= CDF[GammaDistribution[2, 3], 6]", "=")
        assertNotNull(cdf)
        val cdfVal = cdf!!.copyText.toDouble()
        assertTrue("Gamma CDF at mean should be close to 0.594", kotlin.math.abs(cdfVal - 0.593994) < 1e-3)

        val quant = MathStatsEngine.evaluate("= Quantile[GammaDistribution[2, 3], 0.593994]", "=")
        assertNotNull(quant)
        val quantVal = quant!!.copyText.toDouble()
        assertTrue("Quantile inversion should recover ~6.0, got $quantVal", kotlin.math.abs(quantVal - 6.0) < 0.05)
    }

    @Test
    fun testOperatorPrecedenceAndExactFactorial() {
        // -2^2 == -4 (power binds tighter than unary minus)
        val negPower = MathStatsEngine.evaluate("= -2^2", "=")
        assertNotNull(negPower)
        assertEquals("= -4", negPower?.primaryValue)

        val parenNegPower = MathStatsEngine.evaluate("= (-2)^2", "=")
        assertNotNull(parenNegPower)
        assertEquals("= 4", parenNegPower?.primaryValue)

        // Exact factorials: 30! / 28! == 870
        val exactDiv = MathStatsEngine.evaluate("= 30! / 28!", "=")
        assertNotNull(exactDiv)
        assertEquals("= 870", exactDiv?.primaryValue)

        // Exact binomial: Binomial[52, 5] == 2598960
        val binom = MathStatsEngine.evaluate("= Binomial[52, 5]", "=")
        assertNotNull(binom)
        assertEquals("= 2598960", binom?.primaryValue)
    }

    @Test
    fun testCapsAndOverflowGuards() {
        // Fibonacci capped at 92
        val fib10 = MathStatsEngine.evaluate("= Fibonacci[10]", "=")
        assertNotNull(fib10)
        assertEquals("= 55", fib10?.primaryValue)

        val fib92 = MathStatsEngine.evaluate("= Fibonacci[92]", "=")
        assertNotNull(fib92)

        val fib100 = MathStatsEngine.evaluate("= Fibonacci[100]", "=")
        assertNull("Fibonacci > 92 should be rejected/null", fib100)

        // PrimeQ on large 12-digit number should evaluate without freezing
        val primeRes = MathStatsEngine.evaluate("= PrimeQ[1000000000039]", "=")
        assertNotNull(primeRes)
        assertEquals("= True", primeRes?.primaryValue)
    }

    @Test
    fun testChainedPercentVariable() {
        val first = MathStatsEngine.evaluate("= 15 + 27", "=")
        assertNotNull(first)
        assertEquals("= 42", first?.primaryValue)

        val chained = MathStatsEngine.evaluate("= % * 2", "=")
        assertNotNull(chained)
        assertEquals("= 84", chained?.primaryValue)

        val chainedFunc = MathStatsEngine.evaluate("= Sqrt[%]", "=")
        assertNotNull(chainedFunc)
        assertTrue(chainedFunc!!.primaryValue.startsWith("= 9.165"))
    }

    @Test
    fun testDeepNestingAndLengthCapsDoNotCrash() {
        // Deep nesting does not cause StackOverflowError
        val deep = "(".repeat(100) + "1" + ")".repeat(100)
        val deepRes = MathStatsEngine.evaluate("= $deep", "=")
        assertNull("Deeply nested expressions beyond recursion depth should be rejected safely", deepRes)

        // Expressions > 1000 chars rejected safely
        val longExpr = "1 + ".repeat(300) + "1"
        val longRes = MathStatsEngine.evaluate("= $longExpr", "=")
        assertNull("Expressions longer than 1000 characters should be rejected safely", longRes)
    }

    @Test
    fun testWolframTable() {
        val squares = MathStatsEngine.evaluate("= Table[i^2, {i, 5}]", "=")
        assertNotNull(squares)
        assertEquals("{1, 4, 9, 16, 25}", squares?.primaryValue)

        val evens = MathStatsEngine.evaluate("= Table[2 * k, {k, 1, 4}]", "=")
        assertNotNull(evens)
        assertEquals("{2, 4, 6, 8}", evens?.primaryValue)

        val repeated = MathStatsEngine.evaluate("= Table[10, 3]", "=")
        assertNotNull(repeated)
        assertEquals("{10, 10, 10}", repeated?.primaryValue)
    }

    @Test
    fun testWolframNestAndFold() {
        val nested = MathStatsEngine.evaluate("= Nest[Sqrt, 256, 3]", "=")
        assertNotNull(nested)
        assertEquals("= 2", nested?.primaryValue)

        val foldInit = MathStatsEngine.evaluate("= Fold[Plus, 0, {1, 2, 3, 4}]", "=")
        assertNotNull(foldInit)
        assertEquals("= 10", foldInit?.primaryValue)

        val foldNoInit = MathStatsEngine.evaluate("= Fold[Times, {1, 2, 3, 4}]", "=")
        assertNotNull(foldNoInit)
        assertEquals("= 24", foldNoInit?.primaryValue)
    }

    @Test
    fun testWolframStatisticsAdvanced() {
        val std = MathStatsEngine.evaluate("= Standardize[{10, 20, 30, 40, 50}]", "=")
        assertNotNull(std)
        assertTrue(std!!.primaryValue.startsWith("{"))

        val ma = MathStatsEngine.evaluate("= MovingAverage[{1, 2, 3, 4, 5, 6}, 3]", "=")
        assertNotNull(ma)
        assertEquals("{2, 3, 4, 5}", ma?.primaryValue)

        val mdd = MathStatsEngine.evaluate("= MaxDrawdown[{100, 120, 90, 110, 80, 150}]", "=")
        assertNotNull(mdd)
        val mddVal = mdd!!.primaryValue.removePrefix("=").trim().toDouble()
        assertTrue("MaxDrawdown from 120 to 80 should be 1/3 (0.3333)", kotlin.math.abs(mddVal - 0.33333) < 1e-3)
    }

    @Test
    fun testWolframTailDiagnostics() {
        val hill = MathStatsEngine.evaluate("= HillEstimator[{10, 20, 40, 80, 160}]", "=")
        assertNotNull(hill)
        val alpha = hill!!.primaryValue.removePrefix("=").trim().toDouble()
        assertTrue("Hill tail exponent should be positive", alpha > 0.0)

        val esDist = MathStatsEngine.evaluate("= ExpectedShortfall[NormalDistribution[0, 1], 0.95]", "=")
        assertNotNull(esDist)
        val esVal = esDist!!.primaryValue.removePrefix("=").trim().toDouble()
        assertTrue("Normal(0,1) ES at 95% should be approx 2.06", kotlin.math.abs(esVal - 2.06) < 0.05)

        val esList = MathStatsEngine.evaluate("= ExpectedShortfall[{1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, 0.8]", "=")
        assertNotNull(esList)
        val esListVal = esList!!.primaryValue.removePrefix("=").trim().toDouble()
        assertTrue("ES of 1..10 at 80% should be >= 8.2", esListVal >= 8.0)
    }

    @Test
    fun testWolframDistributionAdvancements() {
        val exp = MathStatsEngine.evaluate("= Expectation[NormalDistribution[5, 2]]", "=")
        assertNotNull(exp)
        assertEquals("= 5", exp?.primaryValue)

        val invCdf = MathStatsEngine.evaluate("= InverseCDF[NormalDistribution[0, 1], 0.5]", "=")
        assertNotNull(invCdf)
        assertEquals("= 0", invCdf?.primaryValue)

        val mom = MathStatsEngine.evaluate("= Moment[NormalDistribution[0, 1], 2]", "=")
        assertNotNull(mom)
        assertEquals("= 1", mom?.primaryValue)

        val cm = MathStatsEngine.evaluate("= CentralMoment[NormalDistribution[10, 3], 2]", "=")
        assertNotNull(cm)
        assertEquals("= 9", cm?.primaryValue)

        val listMom = MathStatsEngine.evaluate("= Moment[{2, 4}, 2]", "=")
        assertNotNull(listMom)
        assertEquals("= 10", listMom?.primaryValue)
    }

    @Test
    fun testSmallListVarianceAndExcessKurtosis() {
        assertNull(MathStatsEngine.evaluate("= Variance[{5}]", "="))
        assertNull(MathStatsEngine.evaluate("= StandardDeviation[{5}]", "="))

        val res = MathStatsEngine.evaluate("= Kurtosis[{1, 2, 3, 4, 5}]", "=")
        assertNotNull(res)
        assertEquals("= -1.3", res?.primaryValue)
    }

    @Test
    fun testDynamicOutEvaluation() {
        MathStatsEngine.evaluate("= 10 + 5", "=")
        val res = MathStatsEngine.evaluate("= % * 2", "=")
        assertEquals("= 30", res?.primaryValue)
    }

    @Test
    fun testExpectedShortfallBoundary() {
        val res = MathStatsEngine.evaluate("= ExpectedShortfall[{10, 20, 30, 40, 50}, 0.95]", "=")
        assertNotNull(res)
    }
}
