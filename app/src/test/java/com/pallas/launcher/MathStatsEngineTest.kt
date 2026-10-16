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
        val chooseRes = MathStatsEngine.evaluate("= choose(52, 5)", "=")
        assertNotNull(chooseRes)
        assertEquals("= 2598960", chooseRes?.primaryValue)

        val factRes = MathStatsEngine.evaluate("= fact(5)", "=")
        assertNotNull(factRes)
        assertEquals("= 120", factRes?.primaryValue)
    }

    @Test
    fun testParetoDistribution() {
        val paretoRes = MathStatsEngine.evaluate("= pareto(5, 1.5, 1)", "=")
        assertNotNull(paretoRes)
        assertTrue(paretoRes!!.title.contains("Pareto"))
        assertTrue(paretoRes.primaryValue.startsWith("P(X > x) ="))
        assertEquals("Fat Tail (α≤2)", paretoRes.badge)
    }

    @Test
    fun testNormalDistribution() {
        val normRes = MathStatsEngine.evaluate("= norm(1.96)", "=")
        assertNotNull(normRes)
        assertTrue(normRes!!.title.contains("Normal"))
        assertTrue(normRes.primaryValue.contains("Φ(x)"))
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
        assertTrue(card.hasInfoManual)
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
}
