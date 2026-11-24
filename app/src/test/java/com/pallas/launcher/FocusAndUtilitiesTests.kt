package com.pallas.launcher

import com.pallas.launcher.data.repository.FocusModeManager
import com.pallas.launcher.data.repository.MacroScheduler
import com.pallas.launcher.domain.math.MathCheatsheets
import com.pallas.launcher.domain.model.CommandConfig
import com.pallas.launcher.domain.model.FocusDndMode
import com.pallas.launcher.domain.model.FocusProfile
import com.pallas.launcher.domain.model.GestureAction
import com.pallas.launcher.domain.model.MacroAudioStream
import com.pallas.launcher.domain.model.MacroMediaAction
import com.pallas.launcher.domain.model.MacroRingerMode
import com.pallas.launcher.domain.model.MacroRoutine
import com.pallas.launcher.domain.model.MacroStep
import com.pallas.launcher.domain.model.MacroSystemSetting
import com.pallas.launcher.domain.model.MacroToggleMode
import com.pallas.launcher.domain.search.AsyncCommandRequest
import com.pallas.launcher.domain.search.CALENDAR_GRANT_PERMISSION_ID
import com.pallas.launcher.domain.search.FILES_GRANT_PERMISSION_ID
import com.pallas.launcher.domain.search.FOCUS_OFF_COMMAND_ID
import com.pallas.launcher.domain.search.FOCUS_ON_COMMAND_ID
import com.pallas.launcher.domain.search.FOCUS_SETUP_COMMAND_ID
import com.pallas.launcher.domain.search.FocusCommand
import com.pallas.launcher.domain.search.FocusCommandParser
import com.pallas.launcher.domain.search.LauncherSearchResult
import com.pallas.launcher.domain.search.MACRO_COMMAND_PREFIX
import com.pallas.launcher.domain.search.SmartCommandEngine
import com.pallas.launcher.domain.search.SmartCommandType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class FocusAndUtilitiesTests {

    private val sleepProfile = FocusProfile(
        id = "p_sleep",
        name = "sleep",
        allowedScreenIds = setOf("scr_1"),
        syncDnd = true,
        dndMode = FocusDndMode.ALARMS_ONLY
    )

    private val studyProfile = FocusProfile(
        id = "p_study",
        name = "study",
        allowedAppKeys = setOf("com.example.reader/Main#0"),
        syncDnd = false,
        disabledCommands = setOf("web", "stock")
    )

    private val profiles = listOf(studyProfile, sleepProfile)

    @Test
    fun focusProfileNameValidationRejectsReservedAndInvalidNames() {
        assertNotNull(FocusProfile.validateName(""))
        assertNotNull(FocusProfile.validateName("my profile"))
        assertNotNull(FocusProfile.validateName("off"))
        assertNotNull(FocusProfile.validateName("OFF"))
        assertNotNull(FocusProfile.validateName("dnd"))
        assertNotNull(FocusProfile.validateName("give"))
        assertNotNull(FocusProfile.validateName("focus"))
        assertNotNull(FocusProfile.validateName("60"))
        assertNotNull(FocusProfile.validateName("3h"))
        assertNotNull(FocusProfile.validateName("30mins"))
        assertNotNull(FocusProfile.validateName("work+play"))
        assertNotNull(FocusProfile.validateName("study", existingNames = listOf("Study")))

        assertNull(FocusProfile.validateName("study"))
        assertNull(FocusProfile.validateName("sleep"))
        assertNull(FocusProfile.validateName("deep_work"))
        assertNull(FocusProfile.validateName("Study", existingNames = listOf("Study"), ignoreExistingName = "study"))
    }

    @Test
    fun focusCommandParserHandlesProfilesDurationsDndAndTurnOff() {
        assertEquals(FocusCommand.TurnOff, FocusCommandParser.parse("give up", profiles, "p_study"))
        assertEquals(FocusCommand.TurnOff, FocusCommandParser.parse("FOCUS OFF", profiles, "p_study"))

        assertEquals(
            FocusCommand.NoProfilesConfigured,
            FocusCommandParser.parse("focus", emptyList(), null)
        )

        val bare = FocusCommandParser.parse("focus", profiles, "p_study") as FocusCommand.TurnOn
        assertEquals("p_study", bare.profile?.id)
        assertNull(bare.durationSeconds)
        assertNull(bare.dndOverride)

        val sixty = FocusCommandParser.parse("focus 60", profiles, "p_study") as FocusCommand.TurnOn
        assertEquals("p_study", sixty.profile?.id)
        assertEquals(3600, sixty.durationSeconds)

        val sixtyMins = FocusCommandParser.parse("focus 60 mins", profiles, "p_study") as FocusCommand.TurnOn
        assertEquals(3600, sixtyMins.durationSeconds)

        val fourHours = FocusCommandParser.parse("focus 4h", profiles, "p_study") as FocusCommand.TurnOn
        assertEquals(4 * 3600, fourHours.durationSeconds)

        val sleep3h = FocusCommandParser.parse("focus sleep 3h", profiles, "p_study") as FocusCommand.TurnOn
        assertEquals("p_sleep", sleep3h.profile?.id)
        assertEquals(3 * 3600, sleep3h.durationSeconds)
        assertNull(sleep3h.dndOverride)

        val sleepPlusDnd5h = FocusCommandParser.parse("focus sleep + dnd 5h", profiles, "p_study") as FocusCommand.TurnOn
        assertEquals("p_sleep", sleepPlusDnd5h.profile?.id)
        assertEquals(5 * 3600, sleepPlusDnd5h.durationSeconds)
        assertEquals(true, sleepPlusDnd5h.dndOverride)

        val noDnd = FocusCommandParser.parse("focus sleep -dnd 2h", profiles, "p_study") as FocusCommand.TurnOn
        assertEquals("p_sleep", noDnd.profile?.id)
        assertEquals(2 * 3600, noDnd.durationSeconds)
        assertEquals(false, noDnd.dndOverride)
    }

    @Test
    fun focusScheduleWindowHandlesOvernightAndDaytimeRanges() {
        // Overnight 23:00 (1380) to 07:00 (420)
        assertTrue(FocusModeManager.isMinuteInWindow(23 * 60 + 30, 23 * 60, 7 * 60))
        assertTrue(FocusModeManager.isMinuteInWindow(2 * 60 + 15, 23 * 60, 7 * 60))
        assertFalse(FocusModeManager.isMinuteInWindow(7 * 60, 23 * 60, 7 * 60))
        assertFalse(FocusModeManager.isMinuteInWindow(14 * 60, 23 * 60, 7 * 60))

        // Daytime 09:00 to 17:00
        assertTrue(FocusModeManager.isMinuteInWindow(12 * 60, 9 * 60, 17 * 60))
        assertFalse(FocusModeManager.isMinuteInWindow(18 * 60, 9 * 60, 17 * 60))

        // Dismissal window end calculation for 23:30 in 23:00..07:00 should point to next day 07:00
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 24, 23, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endMs = FocusModeManager.scheduleWindowEndMillis(cal.timeInMillis, 23 * 60, 7 * 60)
        val endCal = Calendar.getInstance().apply { timeInMillis = endMs }
        assertEquals(25, endCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(7, endCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, endCal.get(Calendar.MINUTE))
    }

    @Test
    fun smartCommandsHandleHardwareCalendarFilesMacrosAndFocusDisabling() {
        val macro = MacroRoutine(
            id = "m_deep",
            name = "Deep Work",
            triggerWord = "deep",
            steps = listOf(
                MacroStep.SetAutoBrightness(false),
                MacroStep.SetDnd(MacroToggleMode.ON),
                MacroStep.StartTimer(5400),
                MacroStep.OpenNotebook
            )
        )

        val flEval = SmartCommandEngine.evaluateCommands("fl", emptyList())
        assertTrue(flEval.immediateResults.any { it is LauncherSearchResult.Command && it.id == "cmd_hw_flashlight_toggle" && it.type == SmartCommandType.HARDWARE })

        val dndEval = SmartCommandEngine.evaluateCommands("dnd", emptyList(), isDndEnabled = true)
        val dndCmd = dndEval.immediateResults.first { it.id == "cmd_hw_dnd_toggle" } as LauncherSearchResult.Command
        assertTrue(dndCmd.title.contains("OFF"))

        val calNoPerm = SmartCommandEngine.evaluateCommands("show cal", emptyList(), hasCalendarPermission = false)
        assertTrue(calNoPerm.immediateResults.any { it.id == CALENDAR_GRANT_PERMISSION_ID })

        val calWithPerm = SmartCommandEngine.evaluateCommands("show cal", emptyList(), hasCalendarPermission = true)
        assertTrue(calWithPerm.asyncRequest is AsyncCommandRequest.CalendarAgenda)

        val fileSearch = SmartCommandEngine.evaluateCommands("#budget", emptyList(), hasFileSearchPermission = true)
        assertTrue(fileSearch.asyncRequest is AsyncCommandRequest.LocalFiles)
        assertEquals("budget", (fileSearch.asyncRequest as AsyncCommandRequest.LocalFiles).query)

        val fileNoPerm = SmartCommandEngine.evaluateCommands("#budget", emptyList(), hasFileSearchPermission = false)
        assertNull(fileNoPerm.asyncRequest)
        assertTrue(fileNoPerm.immediateResults.any { it.id == FILES_GRANT_PERMISSION_ID })

        // Hex color #D4AF37 must NOT be hijacked by file search
        val hexEval = SmartCommandEngine.evaluateCommands("#D4AF37", emptyList())
        assertNull(hexEval.asyncRequest)
        assertTrue(hexEval.immediateResults.any { it is LauncherSearchResult.RichCard && it.type == SmartCommandType.COLOR_BASE })

        val macroEval = SmartCommandEngine.evaluateCommands("deep", emptyList(), macros = listOf(macro))
        assertTrue(macroEval.immediateResults.any { it.id == "${MACRO_COMMAND_PREFIX}m_deep" })

        val focusOnEval = SmartCommandEngine.evaluateCommands(
            "focus sleep 2h",
            emptyList(),
            focusProfiles = profiles,
            defaultFocusProfileId = "p_study"
        )
        assertTrue(focusOnEval.immediateResults.any { it.id.startsWith(FOCUS_ON_COMMAND_ID) })

        val giveUpEval = SmartCommandEngine.evaluateCommands("give up", emptyList(), focusProfiles = profiles)
        assertTrue(giveUpEval.immediateResults.any { it.id == FOCUS_OFF_COMMAND_ID })

        val emptySetupEval = SmartCommandEngine.evaluateCommands("focus", emptyList(), focusProfiles = emptyList())
        assertTrue(emptySetupEval.immediateResults.any { it.id == FOCUS_SETUP_COMMAND_ID })

        // Disabled command during Focus Profile ("web" is in studyProfile.disabledCommands)
        val webDuringFocus = SmartCommandEngine.evaluateCommands(
            "q hello",
            emptyList(),
            configs = CommandConfig.DEFAULTS,
            activeFocusProfile = studyProfile
        )
        assertTrue(webDuringFocus.immediateResults.none { it is LauncherSearchResult.Command && it.type == SmartCommandType.WEB_SEARCH })
    }

    @Test
    fun gestureActionsRoundTripForToolsFocusAndMacros() {
        val actions = listOf(
            GestureAction.OpenNotebook,
            GestureAction.OpenLatexStudio,
            GestureAction.OpenAtlas,
            GestureAction.ToggleFocusProfile(GestureAction.DEFAULT_FOCUS_PROFILE_ID),
            GestureAction.ToggleFocusProfile("p_sleep"),
            GestureAction.RunMacro("m_deep")
        )
        actions.forEach { action ->
            val serialized = GestureAction.toString(action)
            val parsed = GestureAction.fromStringOrNull(serialized)
            assertEquals("Round-trip failed for $serialized", action, parsed)
        }
    }

    @Test
    fun cheatsheetsExpandedAndDeepSearchFindsJensenAndBuckling() {
        val linAlg = MathCheatsheets.topicById("linear_algebra")!!
        val probStats = MathCheatsheets.topicById("probability_statistics")!!
        assertTrue("Linear Algebra should have ~25 formulas (was ${linAlg.formulaCount})", linAlg.formulaCount >= 24)
        assertTrue("Probability & Stats should have ~24 formulas (was ${probStats.formulaCount})", probStats.formulaCount >= 22)

        val jensenHits = MathCheatsheets.searchFormulas("jensen")
        assertTrue(jensenHits.isNotEmpty())
        assertTrue(jensenHits.first().formula.title.contains("Jensen", ignoreCase = true))

        val bucklingHits = MathCheatsheets.searchFormulas("buckling")
        assertTrue(bucklingHits.isNotEmpty())
        assertTrue(bucklingHits.any { it.topic.id == "engineering" })

        val woodburyHits = MathCheatsheets.searchFormulas("woodbury")
        assertTrue(woodburyHits.isNotEmpty())
        assertEquals("linear_algebra", woodburyHits.first().topic.id)
    }

    @Test
    fun macroSchedulerFiresOnceOnScheduledDaysWithinCatchUpWindow() {
        val tuesdayCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 24, 8, 2, 0) // 2026-11-24 is Tuesday (DAY_OF_WEEK = 3)
            set(Calendar.MILLISECOND, 0)
        }
        val macro = MacroRoutine(
            id = "m_sched",
            name = "Morning",
            triggerWord = "morn",
            steps = listOf(MacroStep.SetVolume(MacroAudioStream.MEDIA, 40)),
            scheduleEnabled = true,
            scheduleHour = 8,
            scheduleMinute = 0,
            scheduleDays = setOf(Calendar.TUESDAY)
        )
        val todayKey = MacroScheduler.dateKeyFor(tuesdayCal.timeInMillis)
        assertTrue(MacroScheduler.shouldFireAt(macro, tuesdayCal.timeInMillis, null))
        assertFalse(MacroScheduler.shouldFireAt(macro, tuesdayCal.timeInMillis, todayKey))

        val wrongDay = (tuesdayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        assertFalse(MacroScheduler.shouldFireAt(macro, wrongDay.timeInMillis, null))

        val tooLate = (tuesdayCal.clone() as Calendar).apply { set(Calendar.MINUTE, 10) }
        assertFalse(MacroScheduler.shouldFireAt(macro, tooLate.timeInMillis, null))

        val tooEarly = (tuesdayCal.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, 7); set(Calendar.MINUTE, 59) }
        assertFalse(MacroScheduler.shouldFireAt(macro, tooEarly.timeInMillis, null))
    }

    @Test
    fun expandedMacroStepsSerializeAndDeserializeCleanly() {
        val original = MacroRoutine(
            id = "m_full",
            name = "Full",
            triggerWord = "full",
            steps = listOf(
                MacroStep.SetVolume(MacroAudioStream.CALL, 75),
                MacroStep.SetRingerMode(MacroRingerMode.VIBRATE),
                MacroStep.MediaControl(MacroMediaAction.NEXT),
                MacroStep.SetBrightness(60),
                MacroStep.OpenSystemSetting(MacroSystemSetting.BLUETOOTH),
                MacroStep.SetAlarm(6, 30),
                MacroStep.AppendQuickNote("Test"),
                MacroStep.WaitDelay(5),
                MacroStep.OpenQuickCapture
            ),
            scheduleEnabled = true,
            scheduleHour = 6,
            scheduleMinute = 30,
            scheduleDays = setOf(Calendar.MONDAY, Calendar.FRIDAY)
        )
        val restored = MacroRoutine.fromJson(original.toJson())
        assertEquals(original, restored)
    }
}
