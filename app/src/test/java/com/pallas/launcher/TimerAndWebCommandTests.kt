package com.pallas.launcher

import com.pallas.launcher.domain.search.LauncherSearchResult
import com.pallas.launcher.domain.search.SmartCommandEngine
import com.pallas.launcher.domain.search.SmartCommandType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerAndWebCommandTests {

    @Test fun trailingGarbageIsRejected() {
        assertNull(SmartCommandEngine.parseTimer("timer 5m garbage"))
        assertNull(SmartCommandEngine.parseTimer("timer 5mx"))
        assertNull(SmartCommandEngine.parseTimer("timer 1h5"))
    }

    @Test fun duplicateUnitsAreRejected() {
        assertNull(SmartCommandEngine.parseTimer("timer 5m 3m"))
    }

    @Test fun overflowAndOverLimitAreRejected() {
        assertNull(SmartCommandEngine.parseTimer("timer 999999999999h"))
        assertNull(SmartCommandEngine.parseTimer("timer 25h"))
        assertNotNull(SmartCommandEngine.parseTimer("timer 24h"))
    }

    @Test fun validFormsStillWork() {
        assertEquals(3900, SmartCommandEngine.parseTimer("timer 1h 5m")!!.totalSeconds)
        assertEquals(90, SmartCommandEngine.parseTimer("timer 90s")!!.totalSeconds)
        assertEquals(3900, SmartCommandEngine.parseTimer("timer 1:05")!!.totalSeconds)
    }

    @Test fun webCommandOnlyAppearsWhenTypedFully() {
        assertNull(SmartCommandEngine.parseWebQuery("q"))
        assertNull(SmartCommandEngine.parseWebQuery("quick"))
        assertNull(SmartCommandEngine.parseWebQuery("qr code"))
        assertEquals("kotlin flows", SmartCommandEngine.parseWebQuery("q kotlin flows"))
        assertEquals("weather", SmartCommandEngine.parseWebQuery("query weather"))

        val none = SmartCommandEngine.evaluateCommands("quick", emptyList()).immediateResults
        assertTrue(none.none { it is LauncherSearchResult.Command && it.type == SmartCommandType.WEB_SEARCH })
        val hit = SmartCommandEngine.evaluateCommands("q hello", emptyList()).immediateResults
        assertTrue(hit.any { it is LauncherSearchResult.Command && it.type == SmartCommandType.WEB_SEARCH })
    }
}
