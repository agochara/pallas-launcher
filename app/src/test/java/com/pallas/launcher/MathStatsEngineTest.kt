package com.pallas.launcher

import com.pallas.launcher.domain.model.AppItem
import com.pallas.launcher.domain.search.LauncherSearchResult
import com.pallas.launcher.domain.search.MathChartData
import com.pallas.launcher.domain.search.MathStatsEngine
import com.pallas.launcher.domain.search.SearchEngine
import com.pallas.launcher.domain.search.SmartCommandEngine
import com.pallas.launcher.domain.search.SmartCommandType
import kotlin.math.PI
import kotlin.math.abs
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
    fun testWolframMiniCommandsAndOperators() {
        // Prefix @
        val prefixRes = MathStatsEngine.evaluate("= Sqrt @ 16", "=")
        assertNotNull(prefixRes)
        assertEquals("= 4", prefixRes?.primaryValue)

        // Postfix //
        val postfixRes = MathStatsEngine.evaluate("= 16 // Sqrt", "=")
        assertNotNull(postfixRes)
        assertEquals("= 4", postfixRes?.primaryValue)

        val postfixTotal = MathStatsEngine.evaluate("= {1, 2, 3} // Total", "=")
        assertNotNull(postfixTotal)
        assertEquals("= 6", postfixTotal?.primaryValue)

        // Map /@
        val mapRes = MathStatsEngine.evaluate("= Sqrt /@ {4, 9, 16}", "=")
        assertNotNull(mapRes)
        assertEquals("{2, 3, 4}", mapRes?.primaryValue)

        // Apply @@
        val applyRes = MathStatsEngine.evaluate("= Plus @@ {1, 2, 3, 4}", "=")
        assertNotNull(applyRes)
        assertEquals("= 10", applyRes?.primaryValue)

        // ReplaceAll /. with Rule ->
        val replaceRes = MathStatsEngine.evaluate("= x^2 + 2*x + 1 /. x -> 3", "=")
        assertNotNull(replaceRes)
        assertEquals("= 16", replaceRes?.primaryValue)

        // Pure functions # and &
        val pureRes = MathStatsEngine.evaluate("= (#^2) & /@ {1, 2, 3}", "=")
        assertNotNull(pureRes)
        assertEquals("{1, 4, 9}", pureRes?.primaryValue)

        // Additional pure function syntaxes
        assertEquals("{1, 4, 9}", MathStatsEngine.evaluate("= (#^2 &) /@ {1, 2, 3}", "=")?.primaryValue)
        assertEquals("{1, 4, 9}", MathStatsEngine.evaluate("= #^2 & /@ {1, 2, 3}", "=")?.primaryValue)
        assertEquals("= 5", MathStatsEngine.evaluate("= Sqrt @ 16 + 1", "=")?.primaryValue)
        assertEquals("= 5", MathStatsEngine.evaluate("= (Sqrt /@ {4, 9}) // Total", "=")?.primaryValue)

        // Part indexing [[ ]]
        val partRes1 = MathStatsEngine.evaluate("= {10, 20, 30}[[2]]", "=")
        assertNotNull(partRes1)
        assertEquals("= 20", partRes1?.primaryValue)

        val partResNeg = MathStatsEngine.evaluate("= {10, 20, 30}[[-1]]", "=")
        assertNotNull(partResNeg)
        assertEquals("= 30", partResNeg?.primaryValue)

        // Nested brackets
        val nestedBrackets = MathStatsEngine.evaluate("= Total[Range[100]]", "=")
        assertNotNull(nestedBrackets)
        assertEquals("= 5050", nestedBrackets?.primaryValue)
    }

    @Test
    fun testWolframControlFlowAndLoops() {
        // Compound assignments
        assertEquals("= 7", MathStatsEngine.evaluate("= x = 5; x += 2; x", "=")?.primaryValue)

        // If
        val ifTrue = MathStatsEngine.evaluate("= If[10 > 5, 42, 0]", "=")
        assertEquals("= 42", ifTrue?.primaryValue)
        val ifFalse = MathStatsEngine.evaluate("= If[10 < 5, 42, 0]", "=")
        assertEquals("= 0", ifFalse?.primaryValue)

        // Which
        val whichRes = MathStatsEngine.evaluate("= Which[1 < 0, 10, 1 > 0, 20]", "=")
        assertEquals("= 20", whichRes?.primaryValue)

        // While loop
        val whileRes = MathStatsEngine.evaluate("= s = 0; i = 1; While[i <= 5, s += i; i++]; s", "=")
        assertEquals("= 15", whileRes?.primaryValue)

        // For loop
        val forRes = MathStatsEngine.evaluate("= s = 0; For[i = 1, i <= 4, i++, s += i]; s", "=")
        assertEquals("= 10", forRes?.primaryValue)

        // Do loop
        val doRes = MathStatsEngine.evaluate("= s = 0; Do[s += i, {i, 5}]; s", "=")
        assertEquals("= 15", doRes?.primaryValue)

        // Select with EvenQ
        val selectRes = MathStatsEngine.evaluate("= Select[{1, 2, 3, 4, 5, 6}, EvenQ]", "=")
        assertEquals("{2, 4, 6}", selectRes?.primaryValue)

        // NestList & FoldList
        assertEquals("{1, 2, 4, 8, 16}", MathStatsEngine.evaluate("= NestList[# * 2 &, 1, 4]", "=")?.primaryValue)
        assertEquals("{0, 1, 3, 6}", MathStatsEngine.evaluate("= FoldList[Plus, 0, {1, 2, 3}]", "=")?.primaryValue)
    }

    @Test
    fun testWolframComputationTimeoutSafety() {
        // Infinite While loop should terminate quickly and safely
        val start = System.currentTimeMillis()
        val timeoutRes = MathStatsEngine.evaluate("= While[True, 1]", "=")
        val elapsed = System.currentTimeMillis() - start
        assertNotNull(timeoutRes)
        assertEquals("Timeout", timeoutRes?.badge)
        assertTrue("Timeout must terminate within 1000ms, elapsed: ${elapsed}ms", elapsed < 1000)
    }

    @Test
    fun testExpectedShortfallBoundary() {
        val res = MathStatsEngine.evaluate("= ExpectedShortfall[{10, 20, 30, 40, 50}, 0.95]", "=")
        assertNotNull(res)
    }

    @Test
    fun testVariadicGcdLcmAndLogListThreading() {
        assertEquals("= 12", MathStatsEngine.evaluate("= GCD[24, 36, 60]", "=")?.primaryValue)
        assertEquals("= 12", MathStatsEngine.evaluate("= GCD[{24, 36, 60}]", "=")?.primaryValue)
        assertEquals("= 24", MathStatsEngine.evaluate("= LCM[4, 6, 8]", "=")?.primaryValue)
        assertEquals("{1, 2, 3}", MathStatsEngine.evaluate("= Log[2, {2, 4, 8}]", "=")?.primaryValue)
        assertEquals("= 10", MathStatsEngine.evaluate("= Plus[{1, 2, 3, 4}]", "=")?.primaryValue)
        assertEquals("= 24", MathStatsEngine.evaluate("= Times[{1, 2, 3, 4}]", "=")?.primaryValue)
    }

    @Test
    fun testBareAssignmentsAndWebAppNonHijacking() {
        val compact = MathStatsEngine.evaluate("q=10;q", "=")
        assertEquals("= 10", compact?.primaryValue)

        val spaced = MathStatsEngine.evaluate("q = 10; q", "=")
        assertEquals("= 10", spaced?.primaryValue)

        // Semicolon-terminated statement yields Null without overwriting %
        assertEquals("= 42", MathStatsEngine.evaluate("= 42", "=")?.primaryValue)
        val suppressed = MathStatsEngine.evaluate("= q = 10;", "=")
        assertEquals("Done", suppressed?.badge)
        assertEquals("= Null", suppressed?.primaryValue)
        assertEquals("= 42", MathStatsEngine.evaluate("= %", "=")?.primaryValue)

        // SmartCommandEngine routes `q = 10; q` to Math without Web Search
        val mathEval = SmartCommandEngine.evaluateCommands("q = 10; q", emptyList())
        assertTrue(mathEval.immediateResults.any {
            it is LauncherSearchResult.RichCard && it.type == SmartCommandType.MATH_STATS && it.primaryValue == "= 10"
        })
        assertFalse(mathEval.immediateResults.any {
            it is LauncherSearchResult.Command && it.type == SmartCommandType.WEB_SEARCH
        })

        // Regular `q <terms>` still routes to Web Search
        val webEval = SmartCommandEngine.evaluateCommands("q weather today", emptyList())
        assertTrue(webEval.immediateResults.any {
            it is LauncherSearchResult.Command && it.type == SmartCommandType.WEB_SEARCH
        })
        assertFalse(webEval.immediateResults.any {
            it is LauncherSearchResult.RichCard && it.type == SmartCommandType.MATH_STATS
        })

        // SearchEngine suppresses app hits for bare math cards like `q = 10; q`
        val searchEngine = SearchEngine(null)
        searchEngine.updateIndex(
            listOf(AppItem("com.example.q", "MainActivity", "QuickNote")),
            emptyMap()
        )
        val (mathSearchResults, _) = searchEngine.searchAll("q = 10; q")
        assertFalse(mathSearchResults.any { it is LauncherSearchResult.App })
        val (appSearchResults, _) = searchEngine.searchAll("qu")
        assertTrue(appSearchResults.any { it is LauncherSearchResult.App && it.app.label == "QuickNote" })

        // Ordinary app queries starting with built-in function words do not hijack
        assertNull(MathStatsEngine.evaluate("prime video", "="))
        assertNull(MathStatsEngine.evaluate("map", "="))
        assertNull(MathStatsEngine.evaluate("print", "="))
    }

    @Test
    fun testPrintAndEchoConsoleOutput() {
        val p1 = MathStatsEngine.evaluate("= Print[\"hello\"]", "=")
        assertNotNull(p1)
        assertEquals("Print", p1?.badge)
        assertEquals("", p1?.primaryValue)
        assertEquals("hello", p1?.consoleOutput)
        assertEquals("hello", p1?.copyText)

        val p2 = MathStatsEngine.evaluate("q = 10; Print[q^2]; q", "=")
        assertNotNull(p2)
        assertEquals("= 10", p2?.primaryValue)
        assertEquals("100", p2?.consoleOutput)

        val cmd = SmartCommandEngine.evaluateCommands("q = 10; Print[q^2]; q", emptyList())
        val card = cmd.immediateResults.firstOrNull { it is LauncherSearchResult.RichCard } as? LauncherSearchResult.RichCard
        assertEquals("100", card?.consoleOutput)

        val echo = MathStatsEngine.evaluate("= Echo[2 + 3, \"sum\"]", "=")
        assertNotNull(echo)
        assertEquals("= 5", echo?.primaryValue)
        assertTrue(echo?.consoleOutput?.contains("sum") == true)
        assertTrue(echo?.consoleOutput?.contains("5") == true)
    }

    @Test
    fun testSetDelayedFunctionsScopingAndClear() {
        assertEquals("= 26", MathStatsEngine.evaluate("= f[x_] := x^2 + 1; f[5]", "=")?.primaryValue)
        assertEquals(
            "= 720",
            MathStatsEngine.evaluate("= fact[n_] := If[n <= 1, 1, n * fact[n - 1]]; fact[6]", "=")?.primaryValue
        )
        assertEquals("= 5", MathStatsEngine.evaluate("= Module[{a = 3, b = 4}, Sqrt[a^2 + b^2]]", "=")?.primaryValue)
        assertEquals("= 100", MathStatsEngine.evaluate("= With[{k = 10}, k^2]", "=")?.primaryValue)
        assertEquals("= 14", MathStatsEngine.evaluate("= Block[{x = 7}, x * 2]", "=")?.primaryValue)

        val cleared = MathStatsEngine.evaluate("= x = 9; Clear[x]; x", "=")
        assertEquals("Symbol", cleared?.badge)
        assertEquals("x", cleared?.primaryValue)
    }

    @Test
    fun testNumericalCalculusAndSolvers() {
        val sumVal = MathStatsEngine.evaluate("= Sum[1/k^2, {k, 1, 100}]", "=")!!.copyText.toDouble()
        assertEquals(1.6349839, sumVal, 1e-4)

        assertEquals("= 120", MathStatsEngine.evaluate("= Product[k, {k, 1, 5}]", "=")?.primaryValue)

        val intVal = MathStatsEngine.evaluate("= NIntegrate[Sin[x]^2, {x, 0, Pi}]", "=")!!.copyText.toDouble()
        assertEquals(PI / 2.0, intVal, 1e-4)

        val derivVal = MathStatsEngine.evaluate("= ND[x^3, x, 2]", "=")!!.copyText.toDouble()
        assertEquals(12.0, derivVal, 1e-3)

        val root = MathStatsEngine.evaluate("= FindRoot[Cos[x] == x, {x, 0.7}]", "=")
        assertNotNull(root)
        assertTrue("Expected root near 0.73909, got ${root?.primaryValue}", root?.primaryValue?.contains("0.7390") == true)

        val minRes = MathStatsEngine.evaluate("= NMinimize[x^2 - 4*x + 7, x]", "=")
        assertNotNull(minRes)
        assertTrue("Expected min value 3 near x -> 2, got ${minRes?.primaryValue}", minRes?.primaryValue?.startsWith("{3,") == true)

        val maxRes = MathStatsEngine.evaluate("= NMaximize[-x^2 + 6*x, {x, 0, 10}]", "=")
        assertNotNull(maxRes)
        assertTrue("Expected max value 9 near x -> 3, got ${maxRes?.primaryValue}", maxRes?.primaryValue?.startsWith("{9,") == true)

        assertEquals("3.1415927", MathStatsEngine.evaluate("= N[Pi, 8]", "=")?.primaryValue)
    }

    @Test
    fun testSpanIndexingMatricesAndListOps() {
        assertEquals("{3, 4, 5, 6, 7}", MathStatsEngine.evaluate("= Range[10][[3 ;; 7]]", "=")?.primaryValue)
        assertEquals("{1, 3, 5, 7, 9}", MathStatsEngine.evaluate("= Range[10][[1 ;; 10 ;; 2]]", "=")?.primaryValue)
        assertEquals("= 3", MathStatsEngine.evaluate("= {{1, 2}, {3, 4}}[[2, 1]]", "=")?.primaryValue)

        assertEquals("= -2", MathStatsEngine.evaluate("= Det[{{1, 2}, {3, 4}}]", "=")?.primaryValue)
        val inv = MathStatsEngine.evaluate("= Inverse[{{1, 2}, {3, 4}}]", "=")
        assertEquals("Matrix (2×2)", inv?.badge)
        assertEquals("= 1", MathStatsEngine.evaluate("= Tr[Dot[{{1, 2}, {3, 4}}, Inverse[{{1, 2}, {3, 4}}]]]/2", "=")?.primaryValue)

        val tr = MathStatsEngine.evaluate("= Transpose[{{1, 2, 3}, {4, 5, 6}}]", "=")
        assertEquals("Matrix (3×2)", tr?.badge)
        assertEquals("{{1, 4}, {2, 5}, {3, 6}}", tr?.primaryValue)

        assertEquals("= 32", MathStatsEngine.evaluate("= Dot[{1, 2, 3}, {4, 5, 6}]", "=")?.primaryValue)
        assertEquals("= 5", MathStatsEngine.evaluate("= Tr[{{1, 2}, {3, 4}}]", "=")?.primaryValue)
        assertEquals("= 5", MathStatsEngine.evaluate("= Norm[{3, 4}]", "=")?.primaryValue)
        assertEquals("{0, 0, 1}", MathStatsEngine.evaluate("= Cross[{1, 0, 0}, {0, 1, 0}]", "=")?.primaryValue)

        assertEquals("{1, 2, 3, 4}", MathStatsEngine.evaluate("= Flatten[{{1, {2}}, {3, 4}}]", "=")?.primaryValue)
        assertEquals("{{1, 2}, {3, 4}, {5, 6}}", MathStatsEngine.evaluate("= Partition[{1, 2, 3, 4, 5, 6}, 2]", "=")?.primaryValue)
        assertEquals("{1, 2, 3}", MathStatsEngine.evaluate("= Union[{3, 1, 2, 1}]", "=")?.primaryValue)
        assertEquals("{2, 3}", MathStatsEngine.evaluate("= Intersection[{1, 2, 3}, {2, 3, 4}]", "=")?.primaryValue)
        assertEquals("{1, 3}", MathStatsEngine.evaluate("= Complement[{1, 2, 3, 4}, {2, 4}]", "=")?.primaryValue)
    }

    @Test
    fun testNumberTheoryAndSpecialFunctions() {
        assertEquals("{{2, 3}, {3, 2}, {5, 1}}", MathStatsEngine.evaluate("= FactorInteger[360]", "=")?.primaryValue)
        assertEquals("{1, 2, 3, 4, 6, 12}", MathStatsEngine.evaluate("= Divisors[12]", "=")?.primaryValue)
        assertEquals("= 29", MathStatsEngine.evaluate("= Prime[10]", "=")?.primaryValue)
        assertEquals("= 10", MathStatsEngine.evaluate("= PrimePi[30]", "=")?.primaryValue)
        assertEquals("= 24", MathStatsEngine.evaluate("= PowerMod[2, 10, 1000]", "=")?.primaryValue)
        assertEquals("{1, 2, 3, 4}", MathStatsEngine.evaluate("= IntegerDigits[1234]", "=")?.primaryValue)
        assertEquals("= 123", MathStatsEngine.evaluate("= FromDigits[{1, 2, 3}]", "=")?.primaryValue)

        // Mathematica ArcTan[x, y] takes x first, y second
        val atan11 = MathStatsEngine.evaluate("= ArcTan[1, 1]", "=")!!.copyText.toDouble()
        assertEquals(PI / 4.0, atan11, 1e-4)
        val atan01 = MathStatsEngine.evaluate("= ArcTan[0, 1]", "=")!!.copyText.toDouble()
        assertEquals(PI / 2.0, atan01, 1e-4)
        val atanNeg = MathStatsEngine.evaluate("= ArcTan[-1, 0]", "=")!!.copyText.toDouble()
        assertEquals(PI, atanNeg, 1e-4)

        assertEquals("= 4", MathStatsEngine.evaluate("= Mod[-1, 5]", "=")?.primaryValue)
        assertEquals("= 24", MathStatsEngine.evaluate("= Gamma[5]", "=")?.primaryValue)
        assertEquals("= 1", MathStatsEngine.evaluate("= Boole[3 > 2]", "=")?.primaryValue)
        assertEquals("= 1", MathStatsEngine.evaluate("= Sin[90°]", "=")?.primaryValue)
    }

    @Test
    fun testWolframSyntaxExtensionsAndTablePlot() {
        assertEquals("= 5", MathStatsEngine.evaluate("= (* comment *) 2 + 3", "=")?.primaryValue)
        assertEquals("= True", MathStatsEngine.evaluate("= 1 < 2 < 3", "=")?.primaryValue)
        assertEquals("= False", MathStatsEngine.evaluate("= 1 < 3 < 2", "=")?.primaryValue)

        // Parenthesized function calls for known functions vs implicit multiplication for variables
        assertEquals("= 55", MathStatsEngine.evaluate("= f[n_] := n(n + 1)/2; f[10]", "=")?.primaryValue)
        assertEquals("= 1", MathStatsEngine.evaluate("= Sin(Pi/2)", "=")?.primaryValue)

        assertEquals("hello world", MathStatsEngine.evaluate("= \"hello\" <> \" \" <> \"world\"", "=")?.primaryValue)
        assertEquals("= 4", MathStatsEngine.evaluate("= (Sqrt @* (# + 1 &))[15]", "=")?.primaryValue)
        assertEquals("= 3", MathStatsEngine.evaluate("= x //. {x -> y + 1, y -> 2}", "=")?.primaryValue)
        assertEquals("{3, 7}", MathStatsEngine.evaluate("= Plus @@@ {{1, 2}, {3, 4}}", "=")?.primaryValue)

        val lp = MathStatsEngine.evaluate("= ListPlot[Table[{i, i^2}, {i, 5}]]", "=")
        assertNotNull(lp)
        val chart = lp?.chart as? MathChartData.Cartesian2D
        assertNotNull(chart)
        val pts = chart!!.series.first().segments.flatten()
        assertEquals(5, pts.size)
        assertEquals(1.0, pts[0].x, 1e-6)
        assertEquals(25.0, pts[4].y, 1e-6)
    }
}

