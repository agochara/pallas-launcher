package com.pallas.launcher

import com.pallas.launcher.data.repository.QuickCaptureRepository
import com.pallas.launcher.domain.model.CommandConfig
import com.pallas.launcher.domain.search.AsyncCommandRequest
import com.pallas.launcher.domain.search.ColorAndBaseInspector
import com.pallas.launcher.domain.search.DateArithmeticEngine
import com.pallas.launcher.domain.search.EmiCalculatorEngine
import com.pallas.launcher.domain.search.LauncherSearchResult
import com.pallas.launcher.domain.search.QUICK_CAPTURE_INBOX_ID
import com.pallas.launcher.domain.search.QrCodeGenerator
import com.pallas.launcher.domain.search.SearchEngine
import com.pallas.launcher.domain.search.SmartCommandEngine
import com.pallas.launcher.domain.search.SmartCommandType
import com.pallas.launcher.domain.search.WorldTimeEngine
import com.pallas.launcher.platform.LiveCommandService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class NewUtilitiesTests {

    @Test
    fun worldTimeHandlesCitiesZonesUtcOffsetsAndConversions() {
        val fixedNow = Instant.parse("2026-11-23T14:00:00Z") // 19:30 IST, 09:00 EST, 14:00 UTC, 23:00 JST
        val ist = ZoneId.of("Asia/Kolkata")

        val nyc = WorldTimeEngine.evaluate("nyc", is24Hour = true, now = fixedNow, localZone = ist)
        assertNotNull(nyc)
        assertEquals("09:00", nyc!!.copyText)
        assertEquals("-10h 30m", nyc.badge)
        assertTrue(nyc.title.contains("New York"))

        val london = WorldTimeEngine.evaluate("london", is24Hour = true, now = fixedNow, localZone = ist)
        assertNotNull(london)
        assertEquals("14:00", london!!.copyText)
        assertEquals("-5h 30m", london.badge)

        val utc = WorldTimeEngine.evaluate("utc", is24Hour = false, now = fixedNow, localZone = ist)
        assertNotNull(utc)
        assertEquals("2:00 PM", utc!!.copyText)

        val tokyo = WorldTimeEngine.evaluate("tokyo", is24Hour = true, now = fixedNow, localZone = ist)
        assertNotNull(tokyo)
        assertEquals("23:00", tokyo!!.copyText)
        assertEquals("+3h 30m", tokyo.badge)

        val customOffset = WorldTimeEngine.evaluate("utc+5:30", is24Hour = true, now = fixedNow, localZone = ist)
        assertNotNull(customOffset)
        assertEquals("19:30", customOffset!!.copyText)
        assertEquals("Local", customOffset.badge)

        val iana = WorldTimeEngine.evaluate("Europe/Zurich", is24Hour = true, now = fixedNow, localZone = ist)
        assertNotNull(iana)
        assertEquals("15:00", iana!!.copyText)

        // 9:00 AM EST in NYC -> 19:30 IST local
        val converted = WorldTimeEngine.evaluate("9am nyc", is24Hour = true, now = fixedNow, localZone = ist)
        assertNotNull(converted)
        assertEquals("19:30", converted!!.copyText)
    }

    @Test
    fun dateArithmeticEvaluatesNaturalDateQueriesWithoutFalsePositives() {
        val today = LocalDate.of(2026, 11, 23)

        val daysToMay = DateArithmeticEngine.evaluate("days to 15 may", today)
        assertNotNull(daysToMay)
        assertEquals("173", daysToMay!!.copyText)
        assertTrue(daysToMay.primaryValue.contains("173 days"))

        val daysToXmas = DateArithmeticEngine.evaluate("days until dec 25", today)
        assertNotNull(daysToXmas)
        assertEquals("32", daysToXmas!!.copyText)

        val daysSince = DateArithmeticEngine.evaluate("days since 2025-11-23", today)
        assertNotNull(daysSince)
        assertEquals("365", daysSince!!.copyText)
        assertTrue(daysSince.primaryValue.contains("ago"))

        val daysBetween = DateArithmeticEngine.evaluate("days between 2026-05-15 and 2026-08-01", today)
        assertNotNull(daysBetween)
        assertEquals("78", daysBetween!!.copyText)

        val add90d = DateArithmeticEngine.evaluate("today + 90d", today)
        assertNotNull(add90d)
        assertEquals("2027-02-21", add90d!!.copyText)

        val sub3w = DateArithmeticEngine.evaluate("today - 3w", today)
        assertNotNull(sub3w)
        assertEquals("2026-11-02", sub3w!!.copyText)

        val chained = DateArithmeticEngine.evaluate("today + 1y 2m", today)
        assertNotNull(chained)
        assertEquals("2028-01-23", chained!!.copyText)

        val explicitAdd = DateArithmeticEngine.evaluate("2026-05-15 + 30d", today)
        assertNotNull(explicitAdd)
        assertEquals("2026-06-14", explicitAdd!!.copyText)

        val weekday = DateArithmeticEngine.evaluate("weekday 2026-12-25", today)
        assertNotNull(weekday)
        assertEquals("Friday", weekday!!.primaryValue)

        // Plain words / regular arithmetic must never trigger
        assertNull(DateArithmeticEngine.evaluate("today", today))
        assertNull(DateArithmeticEngine.evaluate("may", today))
        assertNull(DateArithmeticEngine.evaluate("15 + 27", today))
        assertNull(DateArithmeticEngine.evaluate("whatsapp", today))
    }

    @Test
    fun dictionaryJsonParserHandlesPhoneticAndMeanings() {
        val sampleJson = """
            [{
              "word": "ephemeral",
              "phonetics": [{"text": "/ɪˈfɛm.ə.ɹəl/"}],
              "meanings": [{
                "partOfSpeech": "adjective",
                "definitions": [{
                  "definition": "Lasting for a markedly brief time.",
                  "example": "fame in politics is largely ephemeral"
                }]
              }],
              "sourceUrls": ["https://en.wiktionary.org/wiki/ephemeral"]
            }]
        """.trimIndent()

        val entry = LiveCommandService.parseDictionaryJson(sampleJson, "ephemeral")
        assertNotNull(entry)
        assertEquals("ephemeral", entry!!.word)
        assertEquals("/ɪˈfɛm.ə.ɹəl/", entry.phonetic)
        assertEquals("adjective", entry.partOfSpeech)
        assertTrue(entry.definitions.first().contains("Lasting for a markedly brief time."))
        assertEquals("fame in politics is largely ephemeral", entry.example)

        assertNull(LiveCommandService.parseDictionaryJson("""{"title":"No Definitions Found"}""", "zzzzzz"))
    }

    @Test
    fun quickCaptureRepositoryAppendsPersistsDeletesAndRestores() {
        val dir = Files.createTempDirectory("pallas_notes_test").toFile()
        try {
            val file = dir.resolve("pallas_inbox.md")
            val repo = QuickCaptureRepository(file)

            val n1 = repo.appendNote("First note", nowMs = 1_700_000_000_000L)
            val n2 = repo.appendNote("Second\nline note", nowMs = 1_700_000_060_000L)
            assertNotNull(n1)
            assertNotNull(n2)
            assertEquals("Second line note", n2!!.text)
            assertEquals(2, repo.notes.value.size)
            // UI list orders newest first
            assertEquals("Second line note", repo.notes.value.first().text)

            // File contains standard Markdown bullets
            val diskText = file.readText(Charsets.UTF_8)
            assertTrue(diskText.contains("- ["))
            assertTrue(diskText.contains("First note"))
            assertTrue(diskText.contains("Second line note"))

            // Fresh instance reloads persisted notes
            val reloaded = QuickCaptureRepository(file)
            assertEquals(2, reloaded.notes.value.size)
            val toDelete = reloaded.notes.value.first()
            reloaded.deleteNote(toDelete)
            assertEquals(1, reloaded.notes.value.size)

            reloaded.restoreNote(toDelete)
            assertEquals(2, reloaded.notes.value.size)

            reloaded.clearAll()
            assertTrue(reloaded.notes.value.isEmpty())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun qrCodeGeneratorBuildsValidQrStructuralPatternsAcrossVersions() {
        val v1 = QrCodeGenerator.encode("HELLO WORLD")
        assertNotNull(v1)
        assertEquals(21, v1!!.size)

        // Check all 3 7x7 finder corners (top-left, top-right, bottom-left) + timing + dark module
        fun assertFinderAt(matrixSize: Int, matrixGet: (Int, Int) -> Boolean, ox: Int, oy: Int) {
            for (dy in 0..6) {
                for (dx in 0..6) {
                    val dist = maxOf(kotlin.math.abs(dy - 3), kotlin.math.abs(dx - 3))
                    val expectedDark = dist == 0 || dist == 1 || dist == 3
                    assertEquals("Finder mismatch at ($ox+$dx,$oy+$dy)", expectedDark, matrixGet(ox + dx, oy + dy))
                }
            }
        }
        assertFinderAt(v1.size, v1::get, 0, 0)
        assertFinderAt(v1.size, v1::get, v1.size - 7, 0)
        assertFinderAt(v1.size, v1::get, 0, v1.size - 7)
        // Dark module (8, size - 8)
        assertTrue(v1.get(8, v1.size - 8))
        // Timing pattern alternates
        assertTrue(v1.get(8, 6))
        assertFalse(v1.get(9, 6))
        assertTrue(v1.get(6, 8))
        assertFalse(v1.get(6, 9))

        // Long payload triggers Version 7+ (size >= 45) & Level L fallback above 213 bytes
        val longPayload = "https://pallas.example.com/" + "a".repeat(250)
        val longQr = QrCodeGenerator.encode(longPayload)
        assertNotNull(longQr)
        assertTrue(longQr!!.size >= 45)
        assertFinderAt(longQr.size, longQr::get, 0, 0)
        assertFinderAt(longQr.size, longQr::get, longQr.size - 7, 0)
        assertFinderAt(longQr.size, longQr::get, 0, longQr.size - 7)
        assertTrue(longQr.get(8, longQr.size - 8))

        // Empty or >458 bytes rejected cleanly
        assertNull(QrCodeGenerator.encode(""))
        assertNull(QrCodeGenerator.encode("x".repeat(500)))
    }

    @Test
    fun colorAndBaseInspectorParsesColorsAndNumberBasesCleanly() {
        val gold = ColorAndBaseInspector.evaluate("#D4AF37")
        assertNotNull(gold)
        assertEquals(0xFFD4AF37.toInt(), gold!!.colorSwatchArgb)
        assertTrue(gold.primaryValue.contains("#D4AF37"))
        assertTrue(gold.primaryValue.contains("rgb(212, 175, 55)"))
        assertTrue(gold.subtitle.contains("hsl("))

        val shortHex = ColorAndBaseInspector.evaluate("#fff")
        assertNotNull(shortHex)
        assertEquals("#FFFFFF", shortHex!!.copyText)
        assertEquals(0xFFFFFFFF.toInt(), shortHex.colorSwatchArgb)

        val rgbInput = ColorAndBaseInspector.evaluate("rgb(212, 175, 55)")
        assertNotNull(rgbInput)
        assertEquals("#D4AF37", rgbInput!!.copyText)

        val hslInput = ColorAndBaseInspector.evaluate("hsl(46, 65%, 52%)")
        assertNotNull(hslInput)
        assertNotNull(hslInput!!.colorSwatchArgb)

        // Short `#1` slot syntax must not false-trigger color inspector
        assertNull(ColorAndBaseInspector.evaluate("#1"))
        assertNull(ColorAndBaseInspector.evaluate("#12"))

        // Base conversions
        val hex255 = ColorAndBaseInspector.evaluate("hex 255")
        assertEquals("0xFF", hex255?.primaryValue)

        val bin42 = ColorAndBaseInspector.evaluate("bin 42")
        assertEquals("0b101010", bin42?.primaryValue)

        val oct255 = ColorAndBaseInspector.evaluate("oct 255")
        assertEquals("0o377", oct255?.primaryValue)

        val decHex = ColorAndBaseInspector.evaluate("dec 0xff")
        assertEquals("255", decHex?.primaryValue)

        val bareHexLit = ColorAndBaseInspector.evaluate("0xFF")
        assertEquals("255", bareHexLit?.primaryValue)

        val bareBinLit = ColorAndBaseInspector.evaluate("0b101010")
        assertEquals("42", bareBinLit?.primaryValue)
    }

    @Test
    fun emiCalculatorComputesExactEmiAndAmortizationSummary() {
        val loan = EmiCalculatorEngine.evaluate("500000 8.5 5y")
        assertNotNull(loan)
        // Standard EMI for 500,000 @ 8.5% over 60 months = 10,258.27
        assertEquals("10258.27", loan!!.copyText)
        assertTrue(loan.primaryValue.contains("10,258.27/mo"))
        assertTrue(loan.bodyText.contains("Year 1:"))
        assertTrue(loan.bodyText.contains("Final (M60):"))
        assertTrue(loan.bodyText.endsWith("Bal 0"))

        val lakhLoan = EmiCalculatorEngine.evaluate("₹50 lakh 8.5% 20 years")
        assertNotNull(lakhLoan)
        assertTrue(lakhLoan!!.primaryValue.startsWith("₹"))
        assertEquals("43391.16", lakhLoan.copyText)

        val zeroInterest = EmiCalculatorEngine.evaluate("100000 0 10m")
        assertNotNull(zeroInterest)
        assertEquals("10000.00", zeroInterest!!.copyText)
    }

    @Test
    fun smartCommandAndSearchEngineIntegrateAllNewUtilitiesAndToggles() {
        // 1. Prefix commands work end-to-end
        val tzCard = SmartCommandEngine.evaluateCommands("tz nyc", emptyList()).immediateResults.single() as LauncherSearchResult.RichCard
        assertEquals(SmartCommandType.WORLD_TIME, tzCard.type)

        val defEval = SmartCommandEngine.evaluateCommands("def ephemeral", emptyList())
        assertTrue(defEval.asyncRequest is AsyncCommandRequest.Define)
        assertEquals("ephemeral", (defEval.asyncRequest as AsyncCommandRequest.Define).word)

        val noteInbox = SmartCommandEngine.evaluateCommands("n", emptyList()).immediateResults.single() as LauncherSearchResult.Command
        assertEquals(QUICK_CAPTURE_INBOX_ID, noteInbox.id)

        val noteAppend = SmartCommandEngine.evaluateCommands("n call Sam #urgent", emptyList()).immediateResults.single() as LauncherSearchResult.RichCard
        assertEquals(SmartCommandType.NOTE, noteAppend.type)
        assertEquals("call Sam #urgent", noteAppend.primaryValue)

        val qrCard = SmartCommandEngine.evaluateCommands("qr WIFI:T:WPA;S:Studio;P:secret;;", emptyList()).immediateResults.single() as LauncherSearchResult.RichCard
        assertEquals(SmartCommandType.QR, qrCard.type)
        assertNotNull(qrCard.qrMatrix)

        val emiCard = SmartCommandEngine.evaluateCommands("emi 500000 8.5 5y", emptyList()).immediateResults.single() as LauncherSearchResult.RichCard
        assertEquals(SmartCommandType.EMI, emiCard.type)

        // 2. Prefixless Date Arithmetic & Color/Base toggles
        val dateActive = SmartCommandEngine.evaluateCommands("days to 15 may", emptyList(), dateArithmeticEnabled = true)
        assertTrue(dateActive.immediateResults.any { it is LauncherSearchResult.RichCard && it.type == SmartCommandType.DATE_ARITHMETIC })
        val dateDisabled = SmartCommandEngine.evaluateCommands("days to 15 may", emptyList(), dateArithmeticEnabled = false)
        assertTrue(dateDisabled.immediateResults.isEmpty())

        val colorActive = SmartCommandEngine.evaluateCommands("#D4AF37", emptyList(), colorBaseInspectorEnabled = true)
        assertTrue(colorActive.immediateResults.any { it is LauncherSearchResult.RichCard && it.type == SmartCommandType.COLOR_BASE })
        val colorDisabled = SmartCommandEngine.evaluateCommands("#D4AF37", emptyList(), colorBaseInspectorEnabled = false)
        assertTrue(colorDisabled.immediateResults.none { it is LauncherSearchResult.RichCard && it.type == SmartCommandType.COLOR_BASE })

        // 3. SearchEngine suppresses apps on active QR / Color / EMI cards
        val searchEngine = SearchEngine()
        assertTrue(searchEngine.searchAll("#D4AF37").first.any { it is LauncherSearchResult.RichCard && it.type == SmartCommandType.COLOR_BASE })
        assertTrue(searchEngine.searchAll("qr hello").first.none { it is LauncherSearchResult.App })

        // 4. Custom prefix override respected
        val customCfg = CommandConfig.DEFAULTS.toMutableMap().apply {
            this["tz"] = getValue("tz").copy(prefix = "time")
        }
        val customTz = SmartCommandEngine.evaluateCommands("time london", emptyList(), configs = customCfg)
        assertTrue(customTz.immediateResults.any { it is LauncherSearchResult.RichCard && it.type == SmartCommandType.WORLD_TIME })
    }
}
