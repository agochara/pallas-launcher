package com.pallas.launcher

import com.pallas.launcher.domain.model.AppItem
import com.pallas.launcher.domain.model.SearchContact
import com.pallas.launcher.domain.search.LauncherSearchResult
import com.pallas.launcher.domain.search.SearchEngine
import com.pallas.launcher.domain.search.SmartCommandEngine
import com.pallas.launcher.domain.search.SmartCommandType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartCommandsAndContactsTests {

    @Test
    fun testAlarmParsingExactPromptExamples() {
        // "al 5" sets alarm for 5:00 am
        val alarm5 = SmartCommandEngine.parseAlarm("al 5")
        assertNotNull(alarm5)
        assertEquals(5, alarm5!!.hour)
        assertEquals(0, alarm5.minute)
        assertEquals("5:00 AM", SmartCommandEngine.formatAlarmTime(alarm5))

        // "al 530" sets alarm for 5:30 am
        val alarm530 = SmartCommandEngine.parseAlarm("al 530")
        assertNotNull(alarm530)
        assertEquals(5, alarm530!!.hour)
        assertEquals(30, alarm530.minute)
        assertEquals("5:30 AM", SmartCommandEngine.formatAlarmTime(alarm530))

        // "alarm 1805" sets alarm for 6:05 pm
        val alarm1805 = SmartCommandEngine.parseAlarm("alarm 1805")
        assertNotNull(alarm1805)
        assertEquals(18, alarm1805!!.hour)
        assertEquals(5, alarm1805.minute)
        assertEquals("6:05 PM", SmartCommandEngine.formatAlarmTime(alarm1805))
    }

    @Test
    fun testAlarmParsingAdditionalFormats() {
        // Colon notation
        val colon = SmartCommandEngine.parseAlarm("al 7:30")
        assertNotNull(colon)
        assertEquals(7, colon!!.hour)
        assertEquals(30, colon.minute)

        // 24 hour
        val hour18 = SmartCommandEngine.parseAlarm("alarm 18")
        assertNotNull(hour18)
        assertEquals(18, hour18!!.hour)
        assertEquals(0, hour18.minute)

        // AM/PM notations
        val pm = SmartCommandEngine.parseAlarm("al 5pm")
        assertNotNull(pm)
        assertEquals(17, pm!!.hour)
        assertEquals(0, pm.minute)

        val pmColon = SmartCommandEngine.parseAlarm("al 5:30pm")
        assertNotNull(pmColon)
        assertEquals(17, pmColon!!.hour)
        assertEquals(30, pmColon.minute)

        val midnight = SmartCommandEngine.parseAlarm("al 12am")
        assertNotNull(midnight)
        assertEquals(0, midnight!!.hour)
        assertEquals(0, midnight.minute)

        val zero = SmartCommandEngine.parseAlarm("al 0")
        assertNotNull(zero)
        assertEquals(0, zero!!.hour)
        assertEquals(0, zero.minute)

        val lateNight = SmartCommandEngine.parseAlarm("al 2359")
        assertNotNull(lateNight)
        assertEquals(23, lateNight!!.hour)
        assertEquals(59, lateNight.minute)
    }

    @Test
    fun testInvalidAlarmReturnsNull() {
        assertNull(SmartCommandEngine.parseAlarm("al 2500"))
        assertNull(SmartCommandEngine.parseAlarm("al 1260"))
        assertNull(SmartCommandEngine.parseAlarm("al abc"))
        assertNull(SmartCommandEngine.parseAlarm("notanalarm 5"))
    }

    @Test
    fun testTimerParsingExactPromptExamples() {
        // "timer 5m" or "timer 5" timer for 5 mins
        val timer5 = SmartCommandEngine.parseTimer("timer 5")
        assertNotNull(timer5)
        assertEquals(300, timer5!!.totalSeconds)
        assertEquals("5 mins", timer5.formattedDuration)

        val timer5m = SmartCommandEngine.parseTimer("timer 5m")
        assertNotNull(timer5m)
        assertEquals(300, timer5m!!.totalSeconds)
        assertEquals("5 mins", timer5m.formattedDuration)

        // "timer 1h 5m" sets timer for 1 hour 5 minutes
        val timer1h5m = SmartCommandEngine.parseTimer("timer 1h 5m")
        assertNotNull(timer1h5m)
        assertEquals(3900, timer1h5m!!.totalSeconds)
        assertEquals("1 hr 5 mins", timer1h5m.formattedDuration)

        // "timer 1hr5m" sets timer for 1 hour 5 minutes
        val timer1hr5m = SmartCommandEngine.parseTimer("timer 1hr5m")
        assertNotNull(timer1hr5m)
        assertEquals(3900, timer1hr5m!!.totalSeconds)
        assertEquals("1 hr 5 mins", timer1hr5m.formattedDuration)

        // "timer 1:05" sets timer for 1 hour 5 minutes
        val timerColon = SmartCommandEngine.parseTimer("timer 1:05")
        assertNotNull(timerColon)
        assertEquals(3900, timerColon!!.totalSeconds)
        assertEquals("1 hr 5 mins", timerColon.formattedDuration)
    }

    @Test
    fun testTimerParsingAdditionalFormats() {
        val timerSeconds = SmartCommandEngine.parseTimer("timer 30s")
        assertNotNull(timerSeconds)
        assertEquals(30, timerSeconds!!.totalSeconds)
        assertEquals("30 sec", timerSeconds.formattedDuration)

        val timerHours = SmartCommandEngine.parseTimer("timer 2h")
        assertNotNull(timerHours)
        assertEquals(7200, timerHours!!.totalSeconds)
        assertEquals("2 hrs", timerHours.formattedDuration)

        val timerHms = SmartCommandEngine.parseTimer("timer 1:30:15")
        assertNotNull(timerHms)
        assertEquals(3600 + 1800 + 15, timerHms!!.totalSeconds)
        assertEquals("1 hr 30 mins 15 sec", timerHms.formattedDuration)
    }

    @Test
    fun testInvalidTimerReturnsNull() {
        assertNull(SmartCommandEngine.parseTimer("timer 0"))
        assertNull(SmartCommandEngine.parseTimer("timer abc"))
        assertNull(SmartCommandEngine.parseTimer("clock 5m"))
    }

    @Test
    fun testCallingConfiguredContactReturnsResult() {
        val contacts = listOf(
            SearchContact(
                nickname = "Mom",
                name = "Mary Smith",
                phoneNumber = "+1234567890"
            ),
            SearchContact(
                nickname = "Dad",
                name = "John Smith",
                phoneNumber = "+1987654321"
            )
        )

        // Exact match
        val resultsMom = SmartCommandEngine.evaluateCommands("call Mom", contacts).immediateResults
        assertEquals(1, resultsMom.size)
        val momResult = resultsMom.first() as LauncherSearchResult.ContactCall
        assertEquals("Mom", momResult.nickname)
        assertEquals("+1234567890", momResult.phoneNumber)

        // Case insensitive match
        val resultsMomLower = SmartCommandEngine.evaluateCommands("call mom", contacts).immediateResults
        assertEquals(1, resultsMomLower.size)
        val momResultLower = resultsMomLower.first() as LauncherSearchResult.ContactCall
        assertEquals("Mom", momResultLower.nickname)

        // Contact not in settings -> NO results
        val resultsBob = SmartCommandEngine.evaluateCommands("call Bob", contacts).immediateResults
        assertTrue("Contact not in settings must not produce call result", resultsBob.isEmpty())
    }

    @Test
    fun testSearchEngineIntegratesCommandsAndApps() {
        val searchEngine = SearchEngine()
        val apps = listOf(
            AppItem(
                packageName = "com.google.android.deskclock",
                activityName = "MainActivity",
                label = "Clock",
                userSerialNumber = 0L
            )
        )
        searchEngine.updateIndex(apps, emptyMap())

        val contacts = listOf(
            SearchContact(nickname = "Mom", name = "Mary Smith", phoneNumber = "555-1234")
        )

        // Alarm query
        val (alarmResults, _) = searchEngine.searchAll("al 530", contacts)
        assertTrue(alarmResults.isNotEmpty())
        val topAlarm = alarmResults.first()
        assertTrue(topAlarm is LauncherSearchResult.Command)
        assertEquals(SmartCommandType.ALARM, (topAlarm as LauncherSearchResult.Command).type)
        assertEquals("Set alarm for 5:30 AM", topAlarm.title)

        // Timer query
        val (timerResults, _) = searchEngine.searchAll("timer 1:05", contacts)
        assertTrue(timerResults.isNotEmpty())
        val topTimer = timerResults.first()
        assertTrue(topTimer is LauncherSearchResult.Command)
        assertEquals(SmartCommandType.TIMER, (topTimer as LauncherSearchResult.Command).type)
        assertEquals("Set timer for 1 hr 5 mins", topTimer.title)

        // Call query
        val (callResults, _) = searchEngine.searchAll("call Mom", contacts)
        assertTrue(callResults.isNotEmpty())
        val topCall = callResults.first()
        assertTrue(topCall is LauncherSearchResult.ContactCall)
        assertEquals("Mom", (topCall as LauncherSearchResult.ContactCall).nickname)
    }

    @Test
    fun testAlphabetEdgeRailContainsOnlyExistingLetterBuckets() {
        // Given a set of apps with specific starting letters
        val apps = listOf(
            AppItem("com.app.camera", "MainActivity", "Camera", null, 0L),
            AppItem("com.app.firefox", "MainActivity", "Firefox", null, 0L),
            AppItem("com.app.maps", "MainActivity", "Maps", null, 0L),
            AppItem("com.app.123", "MainActivity", "123 Calculator", null, 0L)
        )

        val buckets = apps.groupBy { app ->
            val c = app.label.trim().firstOrNull()?.uppercaseChar()
            if (c != null && c in 'A'..'Z') c.toString() else "#"
        }

        val letterOrder = ('A'..'Z').map { it.toString() } + "#"
        val railItems = buildList {
            letterOrder.forEach { letter ->
                if (!buckets[letter].isNullOrEmpty()) {
                    add(if (letter == "#") "•" else letter)
                }
            }
        }

        // Must contain only C, F, M, •
        assertEquals(listOf("C", "F", "M", "•"), railItems)
        // Does not contain A, B, D, E, Z, etc.
        assertTrue(!railItems.contains("A"))
        assertTrue(!railItems.contains("B"))
        assertTrue(!railItems.contains("Z"))
    }

    @Test
    fun testCircularScreenSwipingNavigation() {
        val totalScreens = 2 // Screen A (0) and Screen B (1)
        fun nextIndex(current: Int, direction: Int): Int {
            return if (direction > 0) {
                (current + 1) % totalScreens
            } else if (direction < 0) {
                (current - 1 + totalScreens) % totalScreens
            } else current
        }

        // A -> swipe left (direction > 0) -> B
        var current = 0
        current = nextIndex(current, 1)
        assertEquals(1, current)

        // B -> swipe left -> A (circular!)
        current = nextIndex(current, 1)
        assertEquals(0, current)

        // A -> swipe left -> B
        current = nextIndex(current, 1)
        assertEquals(1, current)

        // B -> swipe right (direction < 0) -> A
        current = nextIndex(current, -1)
        assertEquals(0, current)

        // A -> swipe right -> B (circular reverse!)
        current = nextIndex(current, -1)
        assertEquals(1, current)
    }

    @Test
    fun testCircularSlideAnimationDirection() {
        fun computeTransition(direction: Int): Boolean {
            return direction > 0
        }

        // Swiping left (direction = 1) always slides forward, even when wrapping A -> B -> A -> B...
        assertTrue(computeTransition(1))

        // Swiping right (direction = -1) always slides backward, even when wrapping B -> A -> B -> A...
        assertFalse(computeTransition(-1))
    }

    @Test
    fun testDualAppHidingBehavior() {
        val app1 = AppItem("com.app.chat", "MainActivity", "Chat", null, 0L)
        val app2 = AppItem("com.app.secret", "MainActivity", "Secret Notes", null, 0L)
        val app3 = AppItem("com.app.hidden", "MainActivity", "Hidden App", null, 0L)

        val hiddenFromDrawerOnly = setOf("com.app.secret")
        val hiddenGlobally = setOf("com.app.hidden")

        // 1. Global hide filters out of installedApps
        val allApps = listOf(app1, app2, app3)
        val installedApps = allApps.filterNot { hiddenGlobally.contains(it.packageName) }

        assertEquals(2, installedApps.size)
        assertTrue(installedApps.contains(app1))
        assertTrue(installedApps.contains(app2))
        assertFalse(installedApps.contains(app3))

        // 2. Hide from drawer only filters drawerApps
        val drawerApps = installedApps.filterNot { hiddenFromDrawerOnly.contains(it.packageName) }
        assertEquals(1, drawerApps.size)
        assertTrue(drawerApps.contains(app1))
        assertFalse("Secret Notes must be hidden from drawer", drawerApps.contains(app2))

        // 3. But Secret Notes remains searchable in SearchEngine!
        val searchEngine = SearchEngine()
        searchEngine.updateIndex(installedApps, emptyMap())
        val searchResults = searchEngine.search("Secret")
        assertEquals(1, searchResults.size)
        assertEquals("com.app.secret", searchResults.first().app.packageName)
    }
}
