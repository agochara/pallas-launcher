package com.pallas.launcher

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import com.pallas.launcher.data.repository.ScreenRepository
import com.pallas.launcher.data.repository.SettingsBackup
import com.pallas.launcher.domain.model.CommandConfig
import com.pallas.launcher.domain.model.EdgeScrubPosition
import com.pallas.launcher.domain.model.FocusDndMode
import com.pallas.launcher.domain.model.FocusProfile
import com.pallas.launcher.domain.model.GestureAction
import com.pallas.launcher.domain.model.HomeScreen
import com.pallas.launcher.domain.model.LauncherSettings
import com.pallas.launcher.domain.model.MacroAudioStream
import com.pallas.launcher.domain.model.MacroMediaAction
import com.pallas.launcher.domain.model.MacroRingerMode
import com.pallas.launcher.domain.model.MacroRoutine
import com.pallas.launcher.domain.model.MacroStep
import com.pallas.launcher.domain.model.MacroSystemSetting
import com.pallas.launcher.domain.model.MacroToggleMode
import com.pallas.launcher.domain.model.ScreenApp
import com.pallas.launcher.domain.model.SearchContact
import com.pallas.launcher.domain.model.ThemeMode
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType

/**
 * Guards the "one serialization path" rule. Builds a LauncherSettings in which EVERY field differs from its default
 * (by reflection, so new fields are covered automatically), exports it, parses it back and requires equality.
 * Forget to add a new field to SettingsBackup.settingsToJson/settingsFromJson and this test fails, naming the field.
 *
 * Needs org.json on the unit-test classpath (Robolectric, or testImplementation("org.json:json:20240303")) -
 * use whichever the existing backup/screen tests already use.
 */
class SettingsBackupRoundTripTest {

    /** Deliberately NOT part of a backup. */
    private val excluded = setOf("wolframAppId")

    private fun fields(): List<Field> =
        LauncherSettings::class.java.declaredFields
            .filter { !Modifier.isStatic(it.modifiers) && !it.isSynthetic }
            .onEach { it.isAccessible = true }

    private fun differentValueFor(field: Field, default: Any?): Any? {
        val type = field.type
        return when {
            type == Boolean::class.javaPrimitiveType -> !(default as Boolean)
            type == Int::class.javaPrimitiveType -> (default as Int) + 3
            type == Float::class.javaPrimitiveType -> {
                val f = default as Float
                if (f >= 1.0f) f - 0.25f else f + 0.25f
            }
            type.isEnum -> type.enumConstants!!.first { it != default }
            type == String::class.java -> when (field.name) {
                "stockDefaultRange" -> "1y"
                else -> if (default == "x1") "x2" else "x1"
            }
            GestureAction::class.java.isAssignableFrom(type) -> GestureAction.LaunchApp("com.example.target")
            Set::class.java.isAssignableFrom(type) -> setOf("a.b.c", "d.e.f")
            List::class.java.isAssignableFrom(type) -> {
                val elemType = (field.genericType as ParameterizedType).actualTypeArguments[0]
                when (elemType) {
                    FocusProfile::class.java -> listOf(
                        FocusProfile(
                            id = "fp-1",
                            name = "sleep",
                            allowedScreenIds = setOf("scr_1"),
                            allowedAppKeys = setOf("com.example.clock/Main#0"),
                            syncDnd = true,
                            dndMode = FocusDndMode.ALARMS_ONLY,
                            muteRinger = true,
                            disabledCommands = setOf("web", "stock")
                        )
                    )
                    MacroRoutine::class.java -> listOf(
                        MacroRoutine(
                            id = "m-1",
                            name = "Routine",
                            triggerWord = "trig",
                            steps = listOf(
                                MacroStep.SetVolume(MacroAudioStream.MEDIA, 30),
                                MacroStep.SetRingerMode(MacroRingerMode.SILENT),
                                MacroStep.MediaControl(MacroMediaAction.PLAY_PAUSE),
                                MacroStep.SetDnd(MacroToggleMode.ON, FocusDndMode.ALARMS_ONLY),
                                MacroStep.SetFlashlight(MacroToggleMode.OFF),
                                MacroStep.SetBrightness(40),
                                MacroStep.OpenSystemSetting(MacroSystemSetting.WIFI),
                                MacroStep.SetThemeMode(ThemeMode.LIGHT),
                                MacroStep.SetHideStatusBar(true),
                                MacroStep.SetShowClock(false),
                                MacroStep.SetShowDate(false),
                                MacroStep.SetShowBattery(false),
                                MacroStep.SetWallpaperScrim(true),
                                MacroStep.SetEdgeScrubPosition(EdgeScrubPosition.LEFT),
                                MacroStep.SetAutoLaunchSingleMatch(true),
                                MacroStep.SwitchHomeScreen("scr_1"),
                                MacroStep.SetAutoBrightness(false),
                                MacroStep.SetAutoRotate(true),
                                MacroStep.SetScreenTimeout(120),
                                MacroStep.OpenQuickPanel(com.pallas.launcher.domain.model.MacroQuickPanel.INTERNET),
                                MacroStep.StartTimer(5400, "Session"),
                                MacroStep.SetAlarm(7, 15),
                                MacroStep.AppendQuickNote("Check"),
                                MacroStep.WaitDelay(3),
                                MacroStep.OpenNotebook
                            ),
                            scheduleEnabled = true,
                            scheduleHour = 21,
                            scheduleMinute = 45,
                            scheduleDays = setOf(2, 4, 6)
                        )
                    )
                    else -> listOf(SearchContact(id = "id-1", nickname = "mom", name = "Mom", phoneNumber = "+911234567890"))
                }
            }
            Map::class.java.isAssignableFrom(type) -> {
                val args = (field.genericType as ParameterizedType).actualTypeArguments
                if (args[1] == String::class.java) {
                    mapOf("wa" to "com.whatsapp")
                } else {
                    // Map<String, CommandConfig>: flip one command and change a prefix.
                    CommandConfig.ALL_ENABLED.toMutableMap().also {
                        it["weather"] = it.getValue("weather").copy(enabled = false, prefix = "wx")
                    }
                }
            }
            else -> error("Unsupported LauncherSettings field type ${type.name} for '${field.name}' - teach this test about it")
        }
    }

    private fun nonDefaultSettings(): LauncherSettings {
        val default = LauncherSettings()
        val fs = fields()
        val args = fs.map { f ->
            if (f.name in excluded) f.get(default) else differentValueFor(f, f.get(default))
        }
        val ctor = LauncherSettings::class.java.declaredConstructors
            .filter { !it.isSynthetic && it.parameterCount == fs.size }
            .first()
        return ctor.newInstance(*args.toTypedArray()) as LauncherSettings
    }

    @Test
    fun everySettingSurvivesExportAndImport() {
        val original = nonDefaultSettings()
        val default = LauncherSettings()

        // Sanity: the fixture really changed every non-excluded field (so the test can't pass vacuously).
        fields().filter { it.name !in excluded }.forEach { f ->
            assertNotEquals("fixture did not change '${f.name}'", f.get(default), f.get(original))
        }

        val restored = SettingsBackup.settingsFromJson(SettingsBackup.settingsToJson(original))

        fields().filter { it.name !in excluded }.forEach { f ->
            assertEquals("'${f.name}' was lost in export/import", f.get(original), f.get(restored))
        }
    }

    @Test
    fun secretsAreNeverExported() {
        val withSecret = LauncherSettings(wolframAppId = "SECRET-APP-ID-123")
        val json = SettingsBackup.settingsToJson(withSecret).toString()
        assertFalse("API key leaked into export", json.contains("SECRET-APP-ID-123"))
        assertFalse("secret key present in export", json.contains("wolframAppId"))
        assertEquals("", SettingsBackup.settingsFromJson(SettingsBackup.settingsToJson(withSecret)).wolframAppId)
    }

    private fun fakeContext(): Context {
        val storage = mutableMapOf<String, Any?>()
        val prefs = object : SharedPreferences {
            private val editor = object : SharedPreferences.Editor {
                private val pending = mutableMapOf<String, Any?>()
                override fun putString(key: String, value: String?): SharedPreferences.Editor { pending[key] = value; return this }
                override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor = this
                override fun putInt(key: String, value: Int): SharedPreferences.Editor = this
                override fun putLong(key: String, value: Long): SharedPreferences.Editor = this
                override fun putFloat(key: String, value: Float): SharedPreferences.Editor = this
                override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor { pending[key] = value; return this }
                override fun remove(key: String): SharedPreferences.Editor { pending[key] = null; return this }
                override fun clear(): SharedPreferences.Editor { pending.clear(); return this }
                override fun commit(): Boolean { storage.putAll(pending); return true }
                override fun apply() { storage.putAll(pending) }
            }
            override fun getAll(): Map<String, *> = storage
            override fun getString(key: String, defValue: String?): String? = (storage[key] as? String) ?: defValue
            override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? = defValues
            override fun getInt(key: String, defValue: Int): Int = defValue
            override fun getLong(key: String, defValue: Long): Long = defValue
            override fun getFloat(key: String, defValue: Float): Float = defValue
            override fun getBoolean(key: String, defValue: Boolean): Boolean = (storage[key] as? Boolean) ?: defValue
            override fun contains(key: String): Boolean = storage.containsKey(key)
            override fun edit(): SharedPreferences.Editor = editor
            override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
            override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        }
        return object : ContextWrapper(null) {
            override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = prefs
            override fun getSystemService(name: String): Any? = null
        }
    }

    @Test
    fun fullBackupExportAndParseRoundTripWithScreensAndCustomAppLabels() {
        val screenRepo = ScreenRepository(fakeContext())
        val originalSettings = nonDefaultSettings()
        val screens = listOf(
            HomeScreen(
                id = "scr_work",
                name = "Work",
                contextTag = "Focus",
                apps = listOf(
                    ScreenApp(
                        uniqueKey = "com.example.mail/Main#0",
                        packageName = "com.example.mail",
                        activityName = "Main",
                        customLabel = "Inbox",
                        defaultLabel = "Mail",
                        userSerialNumber = 0L,
                        userHandle = null
                    )
                )
            )
        )
        val labels = mapOf(
            "com.example.mail/Main#0" to "Inbox",
            "com.example.chat/Main#0" to "Messages"
        )

        val json = SettingsBackup.export(originalSettings, screens, screenRepo, labels)
        val parsedResult = SettingsBackup.parse(json)
        assertTrue(parsedResult is SettingsBackup.ParseResult.Ok)
        val parsed = (parsedResult as SettingsBackup.ParseResult.Ok).parsed

        assertEquals(1, parsed.screenCount)
        assertEquals(1, parsed.appCount)
        assertTrue(parsed.hasCustomAppLabels)
        assertEquals(labels, parsed.customAppLabels)

        fields().filter { it.name !in excluded }.forEach { f ->
            assertEquals("'${f.name}' mismatch in full export/parse", f.get(originalSettings), f.get(parsed.settings))
        }

        val restoredScreens = screenRepo.parseScreens(parsed.screensJson)
        assertEquals("Work", restoredScreens.single().name)
        assertEquals("Focus", restoredScreens.single().contextTag)
        assertEquals("Inbox", restoredScreens.single().apps.single().customLabel)
    }

    @Test
    fun utf8BomJsonNullsNanAndUnknownValuesSanitizeCleanly() {
        val obj = SettingsBackup.settingsToJson(LauncherSettings()).apply {
            put("defaultWeatherCity", JSONObject.NULL)
            put("preferredBrowserPackage", JSONObject.NULL)
            put("stockDefaultRange", "bad_range")
            put("themeMode", "FUTURE_THEME")
            put("swipeLeftAction", "unknown_gesture")
            put("swipeRightAction", "app:   ")
            put("appFontScale", "NaN")
            put("customAliases", JSONObject().put(" WA ", " WhatsApp ").put("bad", JSONObject.NULL))
            put("searchContacts", JSONArray().put(
                JSONObject().put("id", "c1").put("nickname", "Mom").put("name", JSONObject.NULL).put("phoneNumber", "123")
            ))
        }
        val doc = JSONObject()
            .put("app", SettingsBackup.APP_ID)
            .put("schemaVersion", SettingsBackup.SCHEMA_VERSION)
            .put("settings", obj)
            .put("screens", JSONArray())

        val result = SettingsBackup.parse("\uFEFF" + doc.toString())
        assertTrue(result is SettingsBackup.ParseResult.Ok)
        val parsed = (result as SettingsBackup.ParseResult.Ok).parsed

        assertEquals(LauncherSettings.DEFAULT_WEATHER_CITY, parsed.settings.defaultWeatherCity)
        assertNull(parsed.settings.preferredBrowserPackage)
        assertEquals("5d", parsed.settings.stockDefaultRange)
        assertEquals(LauncherSettings().themeMode, parsed.settings.themeMode)
        assertEquals(LauncherSettings().shortSwipeLeftAction, parsed.settings.shortSwipeLeftAction)
        assertEquals(LauncherSettings().shortSwipeRightAction, parsed.settings.shortSwipeRightAction)
        assertEquals(1.0f, parsed.settings.appFontScale)
        assertEquals(mapOf("wa" to "WhatsApp"), parsed.settings.customAliases)
        assertEquals("", parsed.settings.searchContacts.single().name)
        assertNull(GestureAction.fromStringOrNull("app:   "))
        assertNull(GestureAction.fromStringOrNull("unknown_gesture"))
    }

    @Test
    fun pass2CommandPrefixCollisionPreventsDuplicateEnabledPrefixes() {
        val configsObj = JSONObject()
            .put("alarm", JSONObject().put("enabled", true).put("prefix", "q"))
            .put("timer", JSONObject().put("enabled", true).put("prefix", "foo"))
            .put("web", JSONObject().put("enabled", true).put("prefix", "foo"))
        val settingsObj = JSONObject().put("commandConfigs", configsObj)

        val restored = SettingsBackup.settingsFromJson(settingsObj)
        val enabledPrefixes = restored.commandConfigs.values
            .filter { it.enabled }
            .map { it.prefix.lowercase() }
        assertEquals(
            "Enabled command prefixes must be strictly unique: $enabledPrefixes",
            enabledPrefixes.size,
            enabledPrefixes.distinct().size
        )
        assertEquals("q", restored.commandConfigs.getValue("alarm").prefix)
        assertEquals("foo", restored.commandConfigs.getValue("timer").prefix)
        assertFalse(restored.commandConfigs.getValue("web").enabled)
    }

    @Test
    fun invalidSchemaVersionAndNonArrayScreensReturnError() {
        val newer = JSONObject()
            .put("app", SettingsBackup.APP_ID)
            .put("schemaVersion", 99)
            .put("settings", JSONObject())
            .toString()
        assertTrue(SettingsBackup.parse(newer) is SettingsBackup.ParseResult.Error)

        val badScreens = JSONObject()
            .put("app", SettingsBackup.APP_ID)
            .put("schemaVersion", 1)
            .put("settings", JSONObject())
            .put("screens", JSONObject())
            .toString()
        assertTrue(SettingsBackup.parse(badScreens) is SettingsBackup.ParseResult.Error)
    }

    @Test
    fun apiIdleDelaysAreClampedBetweenOneAndFiveSeconds() {
        val highObj = JSONObject()
            .put("wolframIdleDelaySeconds", 25)
            .put("wikiDefIdleDelaySeconds", 99)
        val highRestored = SettingsBackup.settingsFromJson(highObj)
        assertEquals(5, highRestored.wolframIdleDelaySeconds)
        assertEquals(5, highRestored.wikiDefIdleDelaySeconds)

        val lowObj = JSONObject()
            .put("wolframIdleDelaySeconds", 0)
            .put("wikiDefIdleDelaySeconds", -3)
        val lowRestored = SettingsBackup.settingsFromJson(lowObj)
        assertEquals(1, lowRestored.wolframIdleDelaySeconds)
        assertEquals(1, lowRestored.wikiDefIdleDelaySeconds)
    }
}