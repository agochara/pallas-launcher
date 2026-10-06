package com.pallas.launcher

import android.content.Context
import android.content.SharedPreferences
import android.os.UserHandle
import com.pallas.launcher.data.repository.ScreenRepository
import com.pallas.launcher.domain.model.AppItem
import com.pallas.launcher.domain.model.ScreenApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class ScreenRepositoryTests {

    private fun createFakePrefs(): SharedPreferences {
        val storage = mutableMapOf<String, Any?>()
        val editor = object : SharedPreferences.Editor {
            private val pending = mutableMapOf<String, Any?>()
            override fun putString(key: String, value: String?): SharedPreferences.Editor {
                pending[key] = value
                return this
            }
            override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor = this
            override fun putInt(key: String, value: Int): SharedPreferences.Editor = this
            override fun putLong(key: String, value: Long): SharedPreferences.Editor = this
            override fun putFloat(key: String, value: Float): SharedPreferences.Editor = this
            override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
                pending[key] = value
                return this
            }
            override fun remove(key: String): SharedPreferences.Editor {
                pending[key] = null
                return this
            }
            override fun clear(): SharedPreferences.Editor {
                pending.clear()
                return this
            }
            override fun commit(): Boolean {
                storage.putAll(pending)
                return true
            }
            override fun apply() {
                storage.putAll(pending)
            }
        }

        return object : SharedPreferences {
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
    }

    private fun createFakeContext(prefs: SharedPreferences): Context {
        return object : android.content.ContextWrapper(null) {
            override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = prefs
            override fun getSystemService(name: String): Any? = null
        }
    }

    @Test
    fun testScreenAppProfileRemovedFlag() {
        val normalPrimaryApp = ScreenApp(
            uniqueKey = "com.example.app/MainActivity#0",
            packageName = "com.example.app",
            activityName = "MainActivity",
            userSerialNumber = 0L,
            userHandle = null
        )
        assertFalse(normalPrimaryApp.isProfileRemoved)

        val removedProfileApp = ScreenApp(
            uniqueKey = "com.example.app/MainActivity#10",
            packageName = "com.example.app",
            activityName = "MainActivity",
            userSerialNumber = 10L,
            userHandle = null
        )
        assertTrue(removedProfileApp.isProfileRemoved)
    }

    @Test
    fun testDefaultScreenHasNoNoisyGeneralTag() {
        val prefs = createFakePrefs()
        val context = createFakeContext(prefs)
        val repository = ScreenRepository(context)

        val screens = repository.screens.value
        assertEquals(1, screens.size)
        assertNull(screens.first().contextTag)
    }

    @Test
    fun testAddScreenNormalizesBlankContextTagToNull() {
        val prefs = createFakePrefs()
        val context = createFakeContext(prefs)
        val repository = ScreenRepository(context)

        repository.addScreen("Work", "   ")
        val screens = repository.screens.value
        val workScreen = screens.find { it.name == "Work" }
        assertEquals("Work", workScreen?.name)
        assertNull(workScreen?.contextTag)
    }

    @Test
    fun testRemoveAppFromScreenMatchesExactUniqueKeyOnly() {
        val prefs = createFakePrefs()
        val context = createFakeContext(prefs)
        val repository = ScreenRepository(context)

        val screenId = repository.screens.value.first().id

        val personalApp = AppItem(
            packageName = "com.example.app",
            activityName = "MainActivity",
            label = "App (Personal)",
            userSerialNumber = 0L
        )
        val workApp = AppItem(
            packageName = "com.example.app",
            activityName = "MainActivity",
            label = "App (Work)",
            userSerialNumber = 10L
        )

        repository.addAppToScreen(screenId, personalApp)
        repository.addAppToScreen(screenId, workApp)

        val initialApps = repository.screens.value.first().apps
        assertEquals(2, initialApps.size)

        // Removing the personal app must NOT remove the work clone with the same package name
        repository.removeAppFromScreen(screenId, personalApp.uniqueKey)

        val updatedApps = repository.screens.value.first().apps
        assertEquals(1, updatedApps.size)
        assertEquals(workApp.uniqueKey, updatedApps.first().uniqueKey)
    }

    @Test
    fun testUpdateAppLabelMatchesExactUniqueKeyOnly() {
        val prefs = createFakePrefs()
        val context = createFakeContext(prefs)
        val repository = ScreenRepository(context)

        val screenId = repository.screens.value.first().id

        val personalApp = AppItem(
            packageName = "com.example.app",
            activityName = "MainActivity",
            label = "App (Personal)",
            userSerialNumber = 0L
        )
        val workApp = AppItem(
            packageName = "com.example.app",
            activityName = "MainActivity",
            label = "App (Work)",
            userSerialNumber = 10L
        )

        repository.addAppToScreen(screenId, personalApp)
        repository.addAppToScreen(screenId, workApp)

        repository.updateAppLabel(screenId, personalApp.uniqueKey, "Custom Personal")

        val apps = repository.screens.value.first().apps
        val personalUpdated = apps.find { it.uniqueKey == personalApp.uniqueKey }
        val workUpdated = apps.find { it.uniqueKey == workApp.uniqueKey }

        assertEquals("Custom Personal", personalUpdated?.customLabel)
        assertNull(workUpdated?.customLabel)
    }
}
