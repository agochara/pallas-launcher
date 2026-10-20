package com.pallas.launcher

import com.pallas.launcher.data.repository.AppUsageTracker
import com.pallas.launcher.domain.model.AppItem
import com.pallas.launcher.domain.model.EdgeScrubPosition
import com.pallas.launcher.domain.model.EdgeScrubReleaseAction
import com.pallas.launcher.domain.model.GestureAction
import com.pallas.launcher.domain.model.LauncherSettings
import com.pallas.launcher.domain.search.AppSearchIndex
import com.pallas.launcher.domain.search.AppSearchResult
import com.pallas.launcher.domain.search.SearchRanking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureNanoTime

class SearchTests {

    private fun createApp(
        packageName: String,
        label: String,
        activityName: String = "MainActivity",
        userSerialNumber: Long = 0L
    ): AppItem {
        return AppItem(
            packageName = packageName,
            activityName = activityName,
            label = label,
            userHandle = null,
            userSerialNumber = userSerialNumber
        )
    }

    @Test
    fun testFireAndFirefoxReturnFirefoxFirst() {
        val apps = listOf(
            createApp("org.mozilla.firefox", "Firefox"),
            createApp("com.example.firewall", "Firewall"),
            createApp("com.example.crossfire", "Crossfire")
        )
        val index = AppSearchIndex()
        index.buildIndex(apps, emptyMap())

        // "firefox" exact match returns Firefox first
        val firefoxResults = index.search("firefox")
        assertFalse(firefoxResults.isEmpty())
        assertEquals("Firefox", firefoxResults.first().app.label)

        // "fire" prefix match returns Firefox first
        val fireResults = index.search("fire")
        assertFalse(fireResults.isEmpty())
        assertEquals("Firefox", fireResults.first().app.label)
    }

    @Test
    fun testWaReturnsWhatsAppWithFrecency() {
        val usageTracker = AppUsageTracker()
        val apps = listOf(
            createApp("com.waze", "Waze"),
            createApp("com.whatsapp", "WhatsApp")
        )
        val index = AppSearchIndex(usageTracker)
        index.buildIndex(apps, emptyMap())

        // Without usage bonus, Waze ranks ahead of WhatsApp alphabetically
        val initialResults = index.search("wa")
        assertEquals("Waze", initialResults.first().app.label)

        // With usage, frecency must decide "wa", so WhatsApp outranks Waze
        repeat(5) {
            usageTracker.recordAppLaunch("com.whatsapp")
        }

        val resultsAfterUsage = index.search("wa")
        assertEquals(2, resultsAfterUsage.size)
        assertEquals("WhatsApp", resultsAfterUsage.first().app.label)
        assertEquals("Waze", resultsAfterUsage[1].app.label)
    }

    @Test
    fun testInitialsAndAcronyms() {
        val apps = listOf(
            createApp("com.google.android.youtube", "YouTube"),
            createApp("com.yahoo.mobile", "Yahoo"),
            createApp("com.google.android.apps.photos", "Google Photos")
        )
        val index = AppSearchIndex()
        index.buildIndex(apps, emptyMap())

        // "yt" matches YouTube initials
        val ytResults = index.search("yt")
        assertFalse("yt should match YouTube", ytResults.isEmpty())
        assertEquals("YouTube", ytResults.first().app.label)

        // "gp" matches Google Photos initials
        val gpResults = index.search("gp")
        assertFalse("gp should match Google Photos", gpResults.isEmpty())
        assertEquals("Google Photos", gpResults.first().app.label)
    }

    @Test
    fun testFuzzyRequiresThreeCharacters() {
        val apps = listOf(
            createApp("org.mozilla.firefox", "Firefox"),
            createApp("org.telegram.messenger", "Telegram"),
            createApp("com.google.android.calculator", "Calculator"),
            createApp("com.android.camera", "Camera")
        )
        val index = AppSearchIndex()
        index.buildIndex(apps, emptyMap())

        // 2-character subsequences are not fuzzy-matched by the new index
        assertTrue("ff should not fuzzy-match Firefox", index.search("ff").isEmpty())
        assertTrue("tg should not fuzzy-match Telegram", index.search("tg").isEmpty())

        // "calc" matches Calculator (word/label prefix)
        val calcResults = index.search("calc")
        assertFalse("calc should match Calculator", calcResults.isEmpty())
        assertEquals("Calculator", calcResults.first().app.label)
    }

    @Test
    fun testHiddenAppAbsent() {
        // Simulating AppRepository filtering: hidden apps are not provided to search index
        val allApps = listOf(
            createApp("com.secret.notes", "Secret Notes"),
            createApp("com.android.camera", "Camera")
        )
        val hiddenApps = setOf("com.secret.notes")
        val visibleApps = allApps.filterNot { hiddenApps.contains(it.packageName) }

        val index = AppSearchIndex()
        index.buildIndex(visibleApps, emptyMap())

        val results = index.search("secret")
        assertTrue("Hidden app should not be present in search results", results.isEmpty())

        val camResults = index.search("camera")
        assertEquals(1, camResults.size)
        assertEquals("Camera", camResults.first().app.label)
    }

    @Test
    fun testEmptyQueryReturnsEmpty() {
        val apps = listOf(
            createApp("com.android.camera", "Camera")
        )
        val index = AppSearchIndex()
        index.buildIndex(apps, emptyMap())

        assertTrue(index.search("").isEmpty())
        assertTrue(index.search("   ").isEmpty())
    }

    @Test
    fun testNoMatchQueryReturnsEmpty() {
        val apps = listOf(
            createApp("com.android.camera", "Camera")
        )
        val index = AppSearchIndex()
        index.buildIndex(apps, emptyMap())

        assertTrue(index.search("xyz123456").isEmpty())
    }

    @Test
    fun testAssertOnlyAppsAreReturned() {
        val apps = listOf(
            createApp("com.google.android.calculator", "Calculator"),
            createApp("com.android.calendar", "Calendar")
        )
        val index = AppSearchIndex()
        index.buildIndex(apps, emptyMap())

        val results = index.search("cal")
        assertFalse(results.isEmpty())
        for (res in results) {
            assertTrue(res is AppSearchResult)
            assertTrue(res.app is AppItem)
        }
    }

    @Test
    fun testUserAliasOutranksEverything() {
        val apps = listOf(
            createApp("com.google.android.apps.maps", "Google Maps"),
            createApp("com.waze", "Waze"),
            createApp("com.whatsapp", "WhatsApp")
        )
        val aliases = mapOf("m" to "com.google.android.apps.maps")
        val index = AppSearchIndex()
        index.buildIndex(apps, aliases)

        val results = index.search("m")
        assertFalse(results.isEmpty())
        assertEquals("Google Maps", results.first().app.label)
    }

    @Test
    fun testLatencyMeasurementWithHundredsOfApps() {
        val apps = (1..500).map { i ->
            createApp("com.app.test$i", "Application $i Number $i")
        }
        val index = AppSearchIndex()
        index.buildIndex(apps, emptyMap())

        val queries = listOf("app", "app 1", "app 250", "number 400", "500", "nonexistent")

        // Warm up
        for (q in queries) {
            index.search(q)
        }

        // Measure latency
        var totalNanos = 0L
        val iterations = 50
        for (i in 0 until iterations) {
            for (q in queries) {
                totalNanos += measureNanoTime {
                    val res = index.search(q)
                    assertFalse(res.size > 8) // Cap at 8
                }
            }
        }

        val avgNanosPerSearch = totalNanos / (iterations * queries.size)
        val avgMillisPerSearch = avgNanosPerSearch / 1_000_000.0

        println("Average search latency across 500 apps: $avgMillisPerSearch ms")
        assertTrue("Search latency should be well under 5ms, actual: $avgMillisPerSearch ms", avgMillisPerSearch < 5.0)
    }

    @Test
    fun testDiacriticsNormalization() {
        val cafeMatch = SearchRanking.evaluateMatch("cafe", "Café")
        assertTrue("cafe should match Café", cafeMatch.score >= 1500.0)

        val uberMatch = SearchRanking.evaluateMatch("uber", "Über")
        assertTrue("uber should match Über", uberMatch.score >= 1500.0)
    }

    @Test
    fun testLauncherSettingsDefaultsAndPositions() {
        val defaultSettings = LauncherSettings()
        assertEquals(com.pallas.launcher.domain.model.ThemeMode.DARK, defaultSettings.themeMode)
        assertEquals(com.pallas.launcher.domain.model.ClockVerticalPosition.TOP, defaultSettings.clockVerticalPosition)
        assertEquals(com.pallas.launcher.domain.model.ClockHorizontalAlignment.RIGHT, defaultSettings.clockHorizontalAlignment)
        assertEquals(com.pallas.launcher.domain.model.AppsVerticalPosition.BOTTOM, defaultSettings.appsVerticalPosition)
        assertEquals(com.pallas.launcher.domain.model.AppsHorizontalAlignment.RIGHT, defaultSettings.appsHorizontalAlignment)
        assertEquals(EdgeScrubPosition.RIGHT, defaultSettings.edgeScrubPosition)
        assertEquals(EdgeScrubReleaseAction.STAY_OPEN, defaultSettings.edgeScrubReleaseAction)
        assertFalse(defaultSettings.showWallpaperScrim)
        assertFalse(defaultSettings.hideStatusBar)
        assertEquals("London", LauncherSettings.DEFAULT_WEATHER_CITY)
        assertEquals("London", defaultSettings.defaultWeatherCity)
    }

    @Test
    fun testDiacriticsHighlightingAndMapping() {
        val app = createApp("com.isbank", "İş Bankası")
        val index = AppSearchIndex()
        index.buildIndex(listOf(app), emptyMap())

        val results = index.search("is")
        assertFalse("Should match 'İş Bankası' with query 'is'", results.isEmpty())
        val match = results.first()
        assertFalse("Should have matched ranges for highlighting", match.matchedRanges.isEmpty())
        match.matchedRanges.forEach { range ->
            assertTrue("Range start within rawLabel bounds", range.first in 0 until app.label.length)
            assertTrue("Range end within rawLabel bounds", range.last in 0 until app.label.length)
            val highlightedSub = app.label.substring(range.first, range.last + 1)
            assertEquals("İş", highlightedSub)
        }
    }

    @Test
    fun testGestureActionUniqueKeyAndFallback() {
        val actionWithProfile = GestureAction.LaunchApp("com.slack/com.slack.ui.MainActivity#10")
        assertEquals("com.slack", actionWithProfile.packageName)
        assertEquals("com.slack/com.slack.ui.MainActivity#10", actionWithProfile.uniqueKey)

        val serialized = GestureAction.toString(actionWithProfile)
        assertEquals("app:com.slack/com.slack.ui.MainActivity#10", serialized)

        val deserialized = GestureAction.fromString(serialized)
        assertEquals(actionWithProfile, deserialized)

        // Legacy format without slash or profile
        val legacy = GestureAction.fromString("app:com.twitter.android") as GestureAction.LaunchApp
        assertEquals(GestureAction.LaunchApp("com.twitter.android"), legacy)
        assertEquals("com.twitter.android", legacy.packageName)
    }

    @Test
    fun testRepeatedWordsAcronymMatching() {
        val match = SearchRanking.evaluateMatch("ccc", "Candy Crush Candy")
        assertTrue(match.score > 0.0)
        assertEquals(3, match.matchedRanges.size)
        // First range is start of first "Candy"
        assertEquals(0..0, match.matchedRanges[0])
        // Second range is start of "Crush"
        assertEquals(6..6, match.matchedRanges[1])
        // Third range is start of second "Candy" (offset 12)
        assertEquals(12..12, match.matchedRanges[2])
    }
}
