package com.pallas.launcher

import com.pallas.launcher.data.repository.SettingsBackup
import com.pallas.launcher.domain.model.CommandConfig
import com.pallas.launcher.domain.model.GestureAction
import com.pallas.launcher.domain.model.LauncherSettings
import com.pallas.launcher.domain.model.SearchContact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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
            type == Float::class.javaPrimitiveType -> (default as Float) + 0.25f
            type.isEnum -> type.enumConstants.first { it != default }
            type == String::class.java -> if (default == "x1") "x2" else "x1"
            GestureAction::class.java.isAssignableFrom(type) -> GestureAction.LaunchApp("com.example.target")
            Set::class.java.isAssignableFrom(type) -> setOf("a.b.c", "d.e.f")
            List::class.java.isAssignableFrom(type) ->
                listOf(SearchContact(id = "id-1", nickname = "mom", name = "Mom", phoneNumber = "+911234567890"))
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
}