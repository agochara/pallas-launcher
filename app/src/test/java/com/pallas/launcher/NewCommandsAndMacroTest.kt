package com.pallas.launcher

import com.pallas.launcher.domain.math.MathCheatsheets
import com.pallas.launcher.domain.model.CommandConfig
import com.pallas.launcher.domain.model.FocusProfile
import com.pallas.launcher.domain.model.MacroQuickPanel
import com.pallas.launcher.domain.model.MacroRoutine
import com.pallas.launcher.domain.model.MacroStep
import com.pallas.launcher.domain.search.DirectAppSearchEngine
import com.pallas.launcher.domain.search.LauncherSearchResult
import com.pallas.launcher.domain.search.MathStatsEngine
import com.pallas.launcher.domain.search.PALLAS_NAV_PREFIX
import com.pallas.launcher.domain.search.PallasCommandParser
import com.pallas.launcher.domain.search.PallasTarget
import com.pallas.launcher.domain.search.SmartCommandEngine
import com.pallas.launcher.domain.search.SmartCommandType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NewCommandsAndMacroTest {

    @Test
    fun pallasCommandParserResolvesTabsToolsAndSettings() {
        assertEquals(PallasTarget.SettingsMain, PallasCommandParser.resolve("pallas").first().target)
        assertEquals(PallasTarget.SettingsMain, PallasCommandParser.resolve("pallas settings").first().target)
        assertEquals(PallasTarget.GuideTab(0), PallasCommandParser.resolve("pallas engine").first().target)
        assertEquals(PallasTarget.GuideTab(1), PallasCommandParser.resolve("pallas cheatsheets").first().target)
        assertEquals(PallasTarget.GuideTab(1), PallasCommandParser.resolve("pallas cheatsheet").first().target)
        assertEquals(PallasTarget.GuideTab(2), PallasCommandParser.resolve("pallas commands").first().target)
        assertEquals(PallasTarget.WorldAtlas, PallasCommandParser.resolve("pallas atlas").first().target)
        assertEquals(PallasTarget.Notebook, PallasCommandParser.resolve("pallas notebook").first().target)
        assertEquals(PallasTarget.LatexStudio, PallasCommandParser.resolve("pallas latex").first().target)
        assertEquals(PallasTarget.FocusSettings, PallasCommandParser.resolve("pallas focus").first().target)
        assertEquals(PallasTarget.MacrosSettings, PallasCommandParser.resolve("pallas macros").first().target)
    }

    @Test
    fun pallasCommandParserResolvesCheatsheetsAndFormulas() {
        val stats = PallasCommandParser.resolve("pallas stats").first().target
        assertTrue(stats is PallasTarget.Cheatsheet)
        assertTrue((stats as PallasTarget.Cheatsheet).topicTitle.contains("Prob", ignoreCase = true) || stats.topicId.contains("prob", ignoreCase = true))

        val info = PallasCommandParser.resolve("pallas info theory").first().target
        assertTrue(info is PallasTarget.Cheatsheet)
        assertTrue((info as PallasTarget.Cheatsheet).topicTitle.contains("Info", ignoreCase = true) || info.topicId.contains("info", ignoreCase = true))

        val calc = PallasCommandParser.resolve("pallas calc").first().target
        assertTrue(calc is PallasTarget.Cheatsheet)
        assertTrue((calc as PallasTarget.Cheatsheet).topicTitle.contains("Calc", ignoreCase = true) || calc.topicId.contains("calc", ignoreCase = true))

        val jensenHits = PallasCommandParser.resolve("pallas jensen")
        assertTrue(jensenHits.any { match ->
            val t = match.target
            t is PallasTarget.Cheatsheet && (t.formulaTitle?.contains("Jensen", ignoreCase = true) == true)
        })
    }

    @Test
    fun pallasTargetEncodeDecodeRoundTrip() {
        val firstTopic = MathCheatsheets.TOPICS.first()
        val statsTopic = MathCheatsheets.TOPICS.first { it.shortTitle.contains("Prob", ignoreCase = true) || it.title.contains("Prob", ignoreCase = true) }
        val targets = listOf(
            PallasTarget.SettingsMain,
            PallasTarget.FocusSettings,
            PallasTarget.MacrosSettings,
            PallasTarget.WorldAtlas,
            PallasTarget.Notebook,
            PallasTarget.LatexStudio,
            PallasTarget.GuideTab(0),
            PallasTarget.GuideTab(2),
            PallasTarget.Cheatsheet(firstTopic.id, firstTopic.shortTitle),
            PallasTarget.Cheatsheet(statsTopic.id, statsTopic.shortTitle, "Jensen's Inequality")
        )
        targets.forEach { target ->
            val decoded = PallasTarget.decode(target.encode())
            assertEquals(target, decoded)
        }
    }

    @Test
    fun directAppSearchParsesKnownAppsAndRejectsUnknown() {
        val yt = DirectAppSearchEngine.parse("yt three blue one brown")
        assertNotNull(yt)
        assertEquals("YouTube", yt!!.target.displayName)
        assertEquals("three blue one brown", yt.query)

        val az = DirectAppSearchEngine.parse("az GPU")
        assertNotNull(az)
        assertEquals("Amazon", az!!.target.displayName)
        assertEquals("GPU", az.query)

        val sp = DirectAppSearchEngine.parse("sp")
        assertNotNull(sp)
        assertEquals("Spotify", sp!!.target.displayName)
        assertEquals("", sp.query)

        assertNull(DirectAppSearchEngine.parse("unknownapp query"))
        assertNull(DirectAppSearchEngine.parse(""))
    }

    @Test
    fun smartCommandEngineEmitsDirectAppSearchAndPallasNav() {
        val appEval = SmartCommandEngine.evaluateCommands(
            rawQuery = "!yt fourier series",
            contacts = emptyList()
        )
        assertTrue(appEval.immediateResults.any {
            it is LauncherSearchResult.Command && it.type == SmartCommandType.APP_SEARCH
        })

        val pallasEval = SmartCommandEngine.evaluateCommands(
            rawQuery = "pallas engine",
            contacts = emptyList()
        )
        assertTrue(pallasEval.immediateResults.any {
            it is LauncherSearchResult.Command && it.id.startsWith(PALLAS_NAV_PREFIX)
        })
    }

    @Test
    fun focusProfileCanDisableInlineMathAndLatex() {
        val profile = FocusProfile(
            id = "fp-study",
            name = "study",
            disabledCommands = setOf("math", "tex"),
            onEnterMacroId = "macro-1"
        )
        val roundTripped = FocusProfile.fromJson(profile.toJson())
        assertEquals(profile, roundTripped)

        val mathWhenAllowed = SmartCommandEngine.evaluateCommands(
            rawQuery = "= 2 + 2",
            contacts = emptyList()
        )
        assertTrue(mathWhenAllowed.immediateResults.any {
            it is LauncherSearchResult.RichCard && it.type == SmartCommandType.MATH_STATS
        })

        val mathWhenBlocked = SmartCommandEngine.evaluateCommands(
            rawQuery = "= 2 + 2",
            contacts = emptyList(),
            isFocusActive = true,
            activeFocusProfile = profile
        )
        assertTrue(mathWhenBlocked.immediateResults.none {
            it is LauncherSearchResult.RichCard && it.type == SmartCommandType.MATH_STATS
        })

        val texWhenBlocked = SmartCommandEngine.evaluateCommands(
            rawQuery = "tex x^2",
            contacts = emptyList(),
            isFocusActive = true,
            activeFocusProfile = profile
        )
        assertTrue(texWhenBlocked.immediateResults.none {
            it is LauncherSearchResult.RichCard && it.type == SmartCommandType.LATEX
        })
    }

    @Test
    fun macroNewSystemStepsJsonRoundTrip() {
        val macro = MacroRoutine(
            id = "m-sys",
            name = "Display & Network",
            triggerWord = "disp",
            steps = listOf(
                MacroStep.SetAutoBrightness(true),
                MacroStep.SetAutoRotate(false),
                MacroStep.SetScreenTimeout(300),
                MacroStep.OpenQuickPanel(MacroQuickPanel.INTERNET),
                MacroStep.OpenQuickPanel(MacroQuickPanel.BLUETOOTH)
            )
        )
        val decoded = MacroRoutine.fromJson(macro.toJson())
        assertEquals(macro, decoded)
    }

    @Test
    fun manualCommandsTabIncludesNewCommands() {
        val entries = MathStatsEngine.manualEntriesFor(CommandConfig.DEFAULTS)
        val commands = entries.filter { it.category == "Launcher Commands" }.map { it.signature }
        assertTrue(commands.any { it.contains("focus", ignoreCase = true) })
        assertTrue(commands.any { it.contains("#") })
        assertTrue(commands.any { it.contains("cal") })
        assertTrue(commands.any { it.contains("fl") })
        assertTrue(commands.any { it.contains("dnd") })
        assertTrue(commands.any { it.contains("!") })
        assertTrue(commands.any { it.contains("pallas", ignoreCase = true) })
    }

    @Test
    fun focusModeBlocksDirectAppSearchForHiddenAppsAndDisabledCallContacts() {
        val restrictedProfile = FocusProfile(
            id = "fp-strict",
            name = "strict",
            allowedAppKeys = setOf("com.allowed.app"),
            disabledCommands = setOf("call")
        )
        val blockedYt = SmartCommandEngine.evaluateCommands(
            rawQuery = "!yt physics",
            contacts = emptyList(),
            isFocusActive = true,
            activeFocusProfile = restrictedProfile,
            focusAllowedAppKeys = setOf("com.allowed.app")
        )
        assertTrue(blockedYt.immediateResults.none {
            it is LauncherSearchResult.Command && it.type == SmartCommandType.APP_SEARCH
        })

        val contacts = listOf(com.pallas.launcher.domain.model.SearchContact("1", "alice", "Alice", "1234567890"))
        val blockedAtContact = SmartCommandEngine.evaluateCommands(
            rawQuery = "@alice",
            contacts = contacts,
            isFocusActive = true,
            activeFocusProfile = restrictedProfile
        )
        assertTrue(blockedAtContact.immediateResults.none { it is LauncherSearchResult.ContactCall })
    }

    @Test
    fun mathAssignmentAndPrefixCollisionsResolveCleanly() {
        val noteMath = SmartCommandEngine.evaluateCommands(
            rawQuery = "n = 5",
            contacts = emptyList()
        )
        assertTrue(noteMath.immediateResults.any {
            it is LauncherSearchResult.RichCard && it.type == SmartCommandType.MATH_STATS && it.primaryValue == "= 5"
        })
        assertTrue(noteMath.immediateResults.none {
            it is LauncherSearchResult.RichCard && it.type == SmartCommandType.NOTE
        })

        val wMath = SmartCommandEngine.evaluateCommands(
            rawQuery = "w = 10",
            contacts = emptyList()
        )
        assertTrue(wMath.immediateResults.any {
            it is LauncherSearchResult.RichCard && it.type == SmartCommandType.MATH_STATS
        })
        assertNull(wMath.asyncRequest)
    }

    @Test
    fun autoLaunchWaitsForPotentialCommandAliasMacroAndContactPrefixes() {
        val blockedPrefixes = listOf("fo", "foc", "pal", "giv", "set", "wi", "tim", "show c", "fl")
        blockedPrefixes.forEach { prefix ->
            assertTrue(
                "Expected '$prefix' to block premature single-app auto-launch",
                SmartCommandEngine.hasPotentialCommandOrAliasMatch(rawQuery = prefix)
            )
        }

        // Unrelated app query with no matching command/alias/macro may auto-launch
        assertTrue(
            !SmartCommandEngine.hasPotentialCommandOrAliasMatch(rawQuery = "fi")
        )

        // Custom aliases: partial match always blocks; exact match allows only when it targets that exact app and no longer alias shares prefix
        val aliases = mapOf("calc" to "com.calc.app", "gh" to "com.github.android")
        assertTrue(
            SmartCommandEngine.hasPotentialCommandOrAliasMatch(
                rawQuery = "ca",
                customAliases = aliases,
                matchedAppUniqueKey = "com.camera.app",
                matchedAppPackage = "com.camera.app"
            )
        )
        assertTrue(
            !SmartCommandEngine.hasPotentialCommandOrAliasMatch(
                rawQuery = "gh",
                customAliases = aliases,
                matchedAppUniqueKey = "com.github.android",
                matchedAppPackage = "com.github.android"
            )
        )
        assertTrue(
            SmartCommandEngine.hasPotentialCommandOrAliasMatch(
                rawQuery = "gh",
                customAliases = aliases + ("ghp" to "com.other.app"),
                matchedAppUniqueKey = "com.github.android",
                matchedAppPackage = "com.github.android"
            )
        )

        // Macro trigger word prefix blocks auto-launch
        val macros = listOf(MacroRoutine(id = "m1", name = "Work", triggerWord = "deep", steps = emptyList()))
        assertTrue(
            SmartCommandEngine.hasPotentialCommandOrAliasMatch(
                rawQuery = "de",
                macros = macros
            )
        )

        // Contact nickname prefix blocks auto-launch when call command is enabled
        val contacts = listOf(com.pallas.launcher.domain.model.SearchContact("1", "mom", "Mom", "1234567890"))
        assertTrue(
            SmartCommandEngine.hasPotentialCommandOrAliasMatch(
                rawQuery = "mo",
                contacts = contacts
            )
        )
        assertTrue(
            !SmartCommandEngine.hasPotentialCommandOrAliasMatch(
                rawQuery = "mo",
                contacts = contacts,
                activeFocusProfile = FocusProfile(id = "fp1", name = "quiet", disabledCommands = setOf("call"))
            )
        )
    }
}
