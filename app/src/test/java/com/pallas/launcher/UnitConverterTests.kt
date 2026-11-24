package com.pallas.launcher

import com.pallas.launcher.domain.model.AppItem
import com.pallas.launcher.domain.model.CommandConfig
import com.pallas.launcher.domain.search.LauncherSearchResult
import com.pallas.launcher.domain.search.SearchEngine
import com.pallas.launcher.domain.search.SmartCommandEngine
import com.pallas.launcher.domain.search.SmartCommandType
import com.pallas.launcher.domain.search.UnitConverterEngine
import com.pallas.launcher.domain.search.isEnterEligible
import com.pallas.launcher.domain.search.isInertOnTap
import com.pallas.launcher.platform.CurrencyRateService
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UnitConverterTests {

    @Before
    fun setUp() {
        CurrencyRateService.updateRatesForTest(
            mapOf(
                "USD" to 1.0,
                "INR" to 85.0,
                "EUR" to 0.90,
                "GBP" to 0.80,
                "JPY" to 150.0
            )
        )
    }

    @After
    fun tearDown() {
        CurrencyRateService.resetToBaselineForTest()
    }

    @Test
    fun temperatureConversionsAndOverview() {
        val cToF = UnitConverterEngine.evaluate("100 c to f", false)
        assertNotNull(cToF)
        assertEquals("212 °F", cToF!!.primaryValue)
        assertEquals("212", cToF.copyText)
        assertEquals("Temperature", cToF.badge)

        val fToC = UnitConverterEngine.evaluate("-40 f to c", false)
        assertNotNull(fToC)
        assertEquals("-40 °C", fToC!!.primaryValue)

        val kToC = UnitConverterEngine.evaluate("0 k to c", false)
        assertNotNull(kToC)
        assertEquals("-273.15 °C", kToC!!.primaryValue)

        val overview = UnitConverterEngine.evaluate("100c", true)
        assertNotNull(overview)
        assertEquals("212 °F", overview!!.primaryValue)
        assertTrue(overview.subtitle.contains("373.15 K"))

        // "=" must NOT be treated as a unit conversion connector
        assertNull(UnitConverterEngine.evaluate("10 c = f", false))
    }

    @Test
    fun lengthAndCompoundFeetInchesConversions() {
        val ftInToCm = UnitConverterEngine.evaluate("5ft 10in to cm", false)
        assertNotNull(ftInToCm)
        assertEquals("177.8 cm", ftInToCm!!.primaryValue)

        val quoteFtIn = UnitConverterEngine.evaluate("5'10\" to cm", false)
        assertNotNull(quoteFtIn)
        assertEquals("177.8 cm", quoteFtIn!!.primaryValue)

        val cmToFt = UnitConverterEngine.evaluate("180 cm to ft", false)
        assertNotNull(cmToFt)
        assertEquals("5.9055 ft", cmToFt!!.primaryValue)
        assertTrue(cmToFt.subtitle.contains("5 ft 10.866 in"))

        val cmToFtIn = UnitConverterEngine.evaluate("180 cm to ft in", false)
        assertNotNull(cmToFtIn)
        assertEquals("5 ft 10.866 in", cmToFtIn!!.primaryValue)
        assertTrue(cmToFtIn.subtitle.contains("5.9055 ft"))

        // Connector vs "in" (inches) ambiguity
        assertEquals("25.4 cm", UnitConverterEngine.evaluate("10 in to cm", false)?.primaryValue)
        assertEquals("39.37 in", UnitConverterEngine.evaluate("100 cm in in", false)?.primaryValue)
        assertEquals("25.4 cm", UnitConverterEngine.evaluate("10 in in cm", false)?.primaryValue)
        assertEquals("62.137 mi", UnitConverterEngine.evaluate("100 km to mi", false)?.primaryValue)
    }

    @Test
    fun weightAndCompoundMassConversions() {
        assertEquals("154.324 lb", UnitConverterEngine.evaluate("70 kg to lb", false)?.primaryValue)
        assertEquals("71.668 kg", UnitConverterEngine.evaluate("11st 4lb to kg", false)?.primaryValue)
        assertEquals("2,494.76 g", UnitConverterEngine.evaluate("5lb 8oz to g", false)?.primaryValue)
        assertEquals("45.359 kg", UnitConverterEngine.evaluate("100 pounds to kg", false)?.primaryValue)
    }

    @Test
    fun volumeAndContextualUnitDisambiguation() {
        // "c" as cup vs celsius
        assertEquals("473.176 mL", UnitConverterEngine.evaluate("2 c to ml", false)?.primaryValue)
        // "oz" as fluid ounce vs weight ounce
        assertEquals("236.588 mL", UnitConverterEngine.evaluate("8 oz to ml", false)?.primaryValue)
        assertEquals("226.796 g", UnitConverterEngine.evaluate("8 oz to g", false)?.primaryValue)
        assertEquals("3.7854 L", UnitConverterEngine.evaluate("1 gal to l", false)?.primaryValue)
    }

    @Test
    fun areaSpeedTimeDataBandwidthPowerPressureFuel() {
        assertEquals("4,046.86 m²", UnitConverterEngine.evaluate("1 acre to m2", false)?.primaryValue)
        assertEquals("62.137 mph", UnitConverterEngine.evaluate("100 km/h to mph", false)?.primaryValue)

        // "m" resolves as minute when target is hour, or meter when target is km
        assertEquals("1.6667 hr", UnitConverterEngine.evaluate("100 m to hr", false)?.primaryValue)
        assertEquals("0.1 km", UnitConverterEngine.evaluate("100 m to km", false)?.primaryValue)

        val gbToMb = UnitConverterEngine.evaluate("5 gb to mb", false)
        assertNotNull(gbToMb)
        assertEquals("5,000 MB", gbToMb!!.primaryValue)
        assertTrue(gbToMb.subtitle.contains("Binary: 4,768.37 MiB"))

        assertEquals("12.5 MB/s", UnitConverterEngine.evaluate("100 mbps to mb/s", false)?.primaryValue)
        assertEquals("2.0684 bar", UnitConverterEngine.evaluate("30 psi to bar", false)?.primaryValue)
        assertEquals("2,092 kJ", UnitConverterEngine.evaluate("500 kcal to kj", false)?.primaryValue)
        assertEquals("100 mW", UnitConverterEngine.evaluate("20 dbm to mw", false)?.primaryValue)
        assertEquals("20 dBm", UnitConverterEngine.evaluate("100 mw to dbm", false)?.primaryValue)
        assertEquals("3.1416 rad", UnitConverterEngine.evaluate("180 deg to rad", false)?.primaryValue)
        assertEquals("22.481 lbf", UnitConverterEngine.evaluate("100 n to lbf", false)?.primaryValue)
        assertEquals("2,400 MHz", UnitConverterEngine.evaluate("2.4 ghz to mhz", false)?.primaryValue)

        // Reciprocal fuel economy
        assertEquals("7.8405 L/100km", UnitConverterEngine.evaluate("30 mpg to l/100km", false)?.primaryValue)
        assertEquals("30.002 mpg", UnitConverterEngine.evaluate("7.84 l/100km to mpg", false)?.primaryValue)
    }

    @Test
    fun numberBasesAndRomanNumerals() {
        assertEquals("0xFF", UnitConverterEngine.evaluate("255 to hex", false)?.primaryValue)
        assertEquals("255", UnitConverterEngine.evaluate("0xff to dec", false)?.primaryValue)
        assertEquals("0b101010", UnitConverterEngine.evaluate("42 to bin", false)?.primaryValue)
        assertEquals("255", UnitConverterEngine.evaluate("ff base 16 to dec", false)?.primaryValue)
        assertEquals("10", UnitConverterEngine.evaluate("1010 base 2 to dec", false)?.primaryValue)
        assertEquals("MMXXVI", UnitConverterEngine.evaluate("2026 to roman", false)?.primaryValue)
        assertEquals("2026", UnitConverterEngine.evaluate("MMXXVI to dec", false)?.primaryValue)
    }

    @Test
    fun currencyConversionsAndCacheParsing() {
        val usdInr = UnitConverterEngine.evaluate("100 usd to inr", false)
        assertNotNull(usdInr)
        assertTrue(usdInr!!.isCurrency)
        assertEquals("₹8,500.00 INR", usdInr.primaryValue)
        assertEquals("8500.00", usdInr.copyText)

        assertEquals("€90.00 EUR", UnitConverterEngine.evaluate("\$100 to eur", false)?.primaryValue)
        assertEquals("₹8,500,000.00 INR", UnitConverterEngine.evaluate("100k usd to inr", false)?.primaryValue)
        assertEquals("$11,764.71 USD", UnitConverterEngine.evaluate("10 lakh inr to usd", false)?.primaryValue)
        assertEquals("$125.00 USD", UnitConverterEngine.evaluate("100 pounds to usd", false)?.primaryValue)

        val overview = UnitConverterEngine.evaluate("100 usd", true)
        assertNotNull(overview)
        assertTrue(overview!!.isCurrency)
        assertTrue(overview.primaryValue.contains("EUR") || overview.primaryValue.contains("INR"))

        val parsed = CurrencyRateService.parseJsonRatesMap(
            JSONObject("""{"eur": 0.92, "inr": 84.5, "bad": -1, "zero": 0}""")
        )
        assertEquals(2, parsed.size)
        assertEquals(0.92, parsed["EUR"]!!, 1e-6)
        assertEquals(84.5, parsed["INR"]!!, 1e-6)
    }

    @Test
    fun smartCommandAndSearchEngineIntegration() {
        val hintEval = SmartCommandEngine.evaluateCommands("convert", emptyList())
        val hint = hintEval.immediateResults.single() as LauncherSearchResult.Command
        assertEquals("cmd_convert_hint", hint.id)
        assertEquals(SmartCommandType.CONVERT, hint.type)
        assertTrue(hint.isInertOnTap)
        assertFalse(hint.isEnterEligible)

        val convAlias = SmartCommandEngine.evaluateCommands("conv 100 c to f", emptyList())
        val convCard = convAlias.immediateResults.single() as LauncherSearchResult.RichCard
        assertEquals(SmartCommandType.CONVERT, convCard.type)
        assertEquals("212 °F", convCard.primaryValue)

        // Custom prefix respects enabled/disabled and does not trigger "conv" alias
        val customCfg = CommandConfig.DEFAULTS.toMutableMap().apply {
            this["convert"] = getValue("convert").copy(prefix = "cv")
        }
        val customHit = SmartCommandEngine.evaluateCommands("cv 100 c to f", emptyList(), configs = customCfg)
        assertEquals("212 °F", (customHit.immediateResults.single() as LauncherSearchResult.RichCard).primaryValue)

        val oldConvAlias = SmartCommandEngine.evaluateCommands("conv 100 c to f", emptyList(), configs = customCfg)
        assertTrue(oldConvAlias.immediateResults.none { it is LauncherSearchResult.RichCard && it.type == SmartCommandType.CONVERT })

        // Disabled convert command suppresses both prefixed and inline conversions
        val disabledCfg = CommandConfig.DEFAULTS.toMutableMap().apply {
            this["convert"] = getValue("convert").copy(enabled = false)
        }
        assertTrue(SmartCommandEngine.evaluateCommands("convert 100 c to f", emptyList(), configs = disabledCfg).immediateResults.isEmpty())
        assertTrue(SmartCommandEngine.evaluateCommands("100 km to mi", emptyList(), configs = disabledCfg).immediateResults.isEmpty())

        // Inline arrow conversion must NOT fall through to Wolfram RuleVal
        val inlineArrow = SmartCommandEngine.evaluateCommands("100 km -> mi", emptyList())
        assertEquals(1, inlineArrow.immediateResults.size)
        assertEquals(SmartCommandType.CONVERT, (inlineArrow.immediateResults.single() as LauncherSearchResult.RichCard).type)

        // Math replacement rules must still go to MathStatsEngine
        val mathRule = SmartCommandEngine.evaluateCommands("= x^2 + 1 /. x -> 3", emptyList())
        val mathCard = mathRule.immediateResults.single() as LauncherSearchResult.RichCard
        assertEquals(SmartCommandType.MATH_STATS, mathCard.type)
        assertEquals("= 10", mathCard.primaryValue)

        // SearchEngine keeps app results on bare "convert" and suppresses apps when conversion card is active
        val searchEngine = SearchEngine()
        searchEngine.updateIndex(
            listOf(AppItem("com.example.conv", "Main", "Unit Converter", null, 0L)),
            emptyMap()
        )
        val bareResults = searchEngine.searchAll("convert").first
        assertTrue(bareResults.any { it is LauncherSearchResult.App && it.app.label == "Unit Converter" })
        assertTrue(bareResults.any { it is LauncherSearchResult.Command && it.type == SmartCommandType.CONVERT })

        val activeResults = searchEngine.searchAll("convert 100 c to f").first
        assertTrue(activeResults.none { it is LauncherSearchResult.App })
        assertTrue(activeResults.any { it is LauncherSearchResult.RichCard && it.type == SmartCommandType.CONVERT })
    }

    @Test
    fun bareCurrencyRatesFxTableAndPairQuotesWorkCleanly() {
        // 1. convert USD -> currency rates grid
        val usdRates = UnitConverterEngine.evaluate("USD", true)
        assertNotNull(usdRates)
        assertTrue(usdRates!!.isCurrency)
        assertEquals("USD Rates", usdRates.badge)
        assertTrue(usdRates.primaryValue.contains('\n'))
        assertTrue(usdRates.primaryValue.contains("EUR"))
        assertTrue(usdRates.primaryValue.contains("INR"))
        assertTrue(usdRates.copyText.contains("1 USD = 85 INR"))

        val eurRates = UnitConverterEngine.evaluate("eur", true)
        assertNotNull(eurRates)
        assertEquals("EUR Rates", eurRates!!.badge)

        // 2. convert rates / convert fx -> major geopolitical FX pairs
        val fxRates = UnitConverterEngine.evaluate("rates", true)
        assertNotNull(fxRates)
        assertTrue(fxRates!!.isCurrency)
        assertEquals("FX Rates", fxRates.badge)
        assertEquals("Major FX Exchange Rates", fxRates.title)
        assertTrue(fxRates.primaryValue.contains("EUR/USD"))
        assertTrue(fxRates.primaryValue.contains("USD/JPY"))
        assertTrue(fxRates.primaryValue.contains("GBP/USD"))
        assertTrue(fxRates.primaryValue.contains("USD/INR"))
        assertTrue(fxRates.primaryValue.contains('\n'))

        assertEquals("FX Rates", UnitConverterEngine.evaluate("fx", true)?.badge)

        // 3. convert EURUSD & convert EUR/USD & bare EUR/USD & with amounts
        val eurUsdSlash = UnitConverterEngine.evaluate("EUR/USD", true)
        assertNotNull(eurUsdSlash)
        assertEquals("EUR/USD", eurUsdSlash!!.badge)
        assertTrue(eurUsdSlash.primaryValue.startsWith("1 EUR = 1.1111 USD"))

        val eurUsdCompact = UnitConverterEngine.evaluate("EURUSD", true)
        assertNotNull(eurUsdCompact)
        assertEquals("EUR/USD", eurUsdCompact!!.badge)
        assertTrue(eurUsdCompact.primaryValue.startsWith("1 EUR = 1.1111 USD"))

        val bareSlash = UnitConverterEngine.evaluate("eur/usd", false)
        assertNotNull(bareSlash)
        assertEquals("EUR/USD", bareSlash!!.badge)

        assertEquals("$111.11 USD", UnitConverterEngine.evaluate("100 EUR/USD", false)?.primaryValue)
        assertEquals("$111.11 USD", UnitConverterEngine.evaluate("100 EURUSD", true)?.primaryValue)

        // 4. Safeguards: unprefixed compact/single-currency/rates and 6-letter unit words do not false-trigger
        assertNull(UnitConverterEngine.evaluate("usd", false))
        assertNull(UnitConverterEngine.evaluate("rates", false))
        assertNull(UnitConverterEngine.evaluate("eurusd", false))
        assertNull(UnitConverterEngine.evaluate("gallon", true))
        assertNull(UnitConverterEngine.evaluate("100 km/h", false))

        // 5. SmartCommandEngine integration
        val cmdRates = SmartCommandEngine.evaluateCommands("convert rates", emptyList())
        val ratesCard = cmdRates.immediateResults.single() as LauncherSearchResult.RichCard
        assertEquals(SmartCommandType.CONVERT, ratesCard.type)
        assertEquals("FX Rates", ratesCard.badgeText)

        val cmdPair = SmartCommandEngine.evaluateCommands("convert EURUSD", emptyList())
        val pairCard = cmdPair.immediateResults.single() as LauncherSearchResult.RichCard
        assertEquals("EUR/USD", pairCard.badgeText)
    }

    @Test
    fun currencyAliasesAmountlessQueriesAndSupportedListWork() {
        val thbInr = UnitConverterEngine.evaluate("thb to inr", false)
        assertNotNull(thbInr)
        assertTrue(thbInr!!.isCurrency)
        assertTrue(thbInr.title.contains("THB/INR"))
        assertTrue(thbInr.primaryValue.contains("1 THB"))
        assertTrue(thbInr.primaryValue.contains("INR"))

        val bahtRs = UnitConverterEngine.evaluate("baht to rs", false)
        assertNotNull(bahtRs)
        assertTrue(bahtRs!!.isCurrency)
        assertTrue(bahtRs.primaryValue.contains("INR"))

        val bhatRupee = UnitConverterEngine.evaluate("bhat to rupee", false)
        assertNotNull(bhatRupee)
        assertTrue(bhatRupee!!.isCurrency)
        assertTrue(bhatRupee.primaryValue.contains("INR"))

        val yenDollar = UnitConverterEngine.evaluate("yen to dollar", false)
        assertNotNull(yenDollar)
        assertTrue(yenDollar!!.isCurrency)
        assertTrue(yenDollar.primaryValue.contains("USD"))

        val poundInr = UnitConverterEngine.evaluate("pound to inr", false)
        assertNotNull(poundInr)
        assertTrue(poundInr!!.isCurrency)
        assertTrue(poundInr.primaryValue.contains("INR"))

        val euroDollar = UnitConverterEngine.evaluate("euro to dollar", false)
        assertNotNull(euroDollar)
        assertTrue(euroDollar!!.isCurrency)
        assertTrue(euroDollar.primaryValue.contains("USD"))

        val yuanInr = UnitConverterEngine.evaluate("yuan to inr", false)
        assertNotNull(yuanInr)
        assertTrue(yuanInr!!.isCurrency)
        assertTrue(yuanInr.primaryValue.contains("INR"))

        val rmbUsd = UnitConverterEngine.evaluate("rmb to usd", false)
        assertNotNull(rmbUsd)
        assertTrue(rmbUsd!!.isCurrency)
        assertTrue(rmbUsd.primaryValue.contains("USD"))

        val francEur = UnitConverterEngine.evaluate("franc to eur", false)
        assertNotNull(francEur)
        assertTrue(francEur!!.isCurrency)
        assertTrue(francEur.primaryValue.contains("EUR"))

        val wonUsd = UnitConverterEngine.evaluate("won to usd", false)
        assertNotNull(wonUsd)
        assertTrue(wonUsd!!.isCurrency)
        assertTrue(wonUsd.primaryValue.contains("USD"))

        val dirhamInr = UnitConverterEngine.evaluate("dirham to inr", false)
        assertNotNull(dirhamInr)
        assertTrue(dirhamInr!!.isCurrency)
        assertTrue(dirhamInr.primaryValue.contains("INR"))

        val leadingRs = UnitConverterEngine.evaluate("rs 500 to usd", false)
        assertNotNull(leadingRs)
        assertTrue(leadingRs!!.isCurrency)
        assertTrue(leadingRs.primaryValue.contains("USD"))

        val convNoTo = UnitConverterEngine.evaluate("thb inr", true)
        assertNotNull(convNoTo)
        assertTrue(convNoTo!!.isCurrency)
        assertTrue(convNoTo.primaryValue.contains("INR"))

        // Mass disambiguation: pounds/lb -> kg stays mass, NOT GBP currency
        val massPounds = UnitConverterEngine.evaluate("100 pounds to kg", false)
        assertNotNull(massPounds)
        assertFalse(massPounds!!.isCurrency)
        assertTrue(massPounds.primaryValue.endsWith("kg"))

        val massLb = UnitConverterEngine.evaluate("10 lb to kg", false)
        assertNotNull(massLb)
        assertFalse(massLb!!.isCurrency)
        assertTrue(massLb.primaryValue.endsWith("kg"))

        // Supported currencies metadata list
        val codes = UnitConverterEngine.allSupportedCurrencies().map { it.code }.toSet()
        assertTrue(codes.containsAll(listOf("USD", "INR", "THB", "JPY", "GBP", "EUR", "CNY", "CHF", "KRW", "AED")))
    }
}
