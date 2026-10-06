package com.pallas.launcher.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.os.UserManager
import com.pallas.launcher.domain.model.AppItem
import com.pallas.launcher.domain.model.HomeScreen
import com.pallas.launcher.domain.model.ScreenApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ScreenRepository(
    private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("pallas_screens", Context.MODE_PRIVATE)

    private val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager

    private val _screens = MutableStateFlow<List<HomeScreen>>(loadScreens())
    val screens: StateFlow<List<HomeScreen>> = _screens.asStateFlow()

    private fun loadScreens(): List<HomeScreen> {
        val rawJson = prefs.getString(KEY_SCREENS, null)
        if (rawJson.isNullOrBlank()) {
            return listOf(
                HomeScreen(
                    id = "home_main",
                    name = "Home",
                    contextTag = "General",
                    apps = emptyList()
                )
            )
        }

        val result = mutableListOf<HomeScreen>()
        try {
            val jsonArray = JSONArray(rawJson)
            for (i in 0 until jsonArray.length()) {
                val screenObj = jsonArray.getJSONObject(i)
                val id = screenObj.optString("id", UUID.randomUUID().toString())
                val name = screenObj.optString("name", "Screen ${i + 1}")
                val contextTag = if (screenObj.has("contextTag")) screenObj.getString("contextTag") else null
                val appsArray = screenObj.optJSONArray("apps") ?: JSONArray()
                val apps = mutableListOf<ScreenApp>()
                for (j in 0 until appsArray.length()) {
                    val appObj = appsArray.getJSONObject(j)
                    val pkg = appObj.optString("packageName")
                    val activity = appObj.optString("activityName")
                    val serial = appObj.optLong("userSerialNumber", 0L)
                    val uniqueKey = appObj.optString("uniqueKey").ifBlank { "$pkg/$activity#$serial" }
                    val customLabel = if (appObj.has("customLabel")) appObj.getString("customLabel") else null
                    val defaultLabel = appObj.optString("defaultLabel", "")
                    val handle = userManager?.getUserForSerialNumber(serial)

                    if (pkg.isNotBlank()) {
                        apps.add(
                            ScreenApp(
                                uniqueKey = uniqueKey,
                                packageName = pkg,
                                activityName = activity,
                                customLabel = customLabel,
                                defaultLabel = defaultLabel,
                                userSerialNumber = serial,
                                userHandle = handle
                            )
                        )
                    }
                }
                result.add(HomeScreen(id = id, name = name, contextTag = contextTag, apps = apps))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return if (result.isEmpty()) {
            listOf(HomeScreen(id = "home_main", name = "Home", contextTag = "General", apps = emptyList()))
        } else {
            result
        }
    }

    private fun saveScreens(screenList: List<HomeScreen>) {
        try {
            val jsonArray = JSONArray()
            for (screen in screenList) {
                val screenObj = JSONObject()
                screenObj.put("id", screen.id)
                screenObj.put("name", screen.name)
                if (screen.contextTag != null) {
                    screenObj.put("contextTag", screen.contextTag)
                }
                val appsArray = JSONArray()
                for (app in screen.apps) {
                    val appObj = JSONObject()
                    appObj.put("uniqueKey", app.uniqueKey)
                    appObj.put("packageName", app.packageName)
                    appObj.put("activityName", app.activityName)
                    appObj.put("userSerialNumber", app.userSerialNumber)
                    if (app.customLabel != null) {
                        appObj.put("customLabel", app.customLabel)
                    }
                    appObj.put("defaultLabel", app.defaultLabel)
                    appsArray.put(appObj)
                }
                screenObj.put("apps", appsArray)
                jsonArray.put(screenObj)
            }
            prefs.edit().putString(KEY_SCREENS, jsonArray.toString()).apply()
            _screens.value = screenList
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun initDefaultScreenWithApps(installedApps: List<AppItem>) {
        if (prefs.getBoolean("key_screens_seeded", false)) return
        prefs.edit().putBoolean("key_screens_seeded", true).apply()
    }

    fun addScreen(name: String, contextTag: String? = null) {
        val current = _screens.value.toMutableList()
        current.add(
            HomeScreen(
                id = UUID.randomUUID().toString(),
                name = name,
                contextTag = contextTag,
                apps = emptyList()
            )
        )
        saveScreens(current)
    }

    fun renameScreen(screenId: String, newName: String, newContextTag: String? = null) {
        val current = _screens.value.toMutableList()
        val index = current.indexOfFirst { it.id == screenId }
        if (index != -1) {
            current[index] = current[index].copy(
                name = newName.trim(),
                contextTag = newContextTag?.trim()?.ifBlank { null }
            )
            saveScreens(current)
        }
    }

    fun moveScreen(fromIndex: Int, toIndex: Int) {
        val current = _screens.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices && fromIndex != toIndex) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            saveScreens(current)
        }
    }

    fun removeScreen(screenId: String) {
        val current = _screens.value.toMutableList()
        if (current.size <= 1) return
        current.removeAll { it.id == screenId }
        saveScreens(current)
    }

    fun addAppToScreen(screenId: String, appItem: AppItem, customLabel: String? = null) {
        val current = _screens.value.toMutableList()
        val index = current.indexOfFirst { it.id == screenId }
        if (index != -1) {
            val screen = current[index]
            val exists = screen.apps.any { it.uniqueKey == appItem.uniqueKey }
            if (!exists) {
                val newApp = ScreenApp(
                    uniqueKey = appItem.uniqueKey,
                    packageName = appItem.packageName,
                    activityName = appItem.activityName,
                    customLabel = customLabel,
                    defaultLabel = appItem.label,
                    userSerialNumber = appItem.userSerialNumber,
                    userHandle = appItem.userHandle
                )
                current[index] = screen.copy(apps = screen.apps + newApp)
                saveScreens(current)
            }
        }
    }

    fun removeAppFromScreen(screenId: String, uniqueKey: String) {
        val current = _screens.value.toMutableList()
        val index = current.indexOfFirst { it.id == screenId }
        if (index != -1) {
            val screen = current[index]
            current[index] = screen.copy(apps = screen.apps.filterNot { it.uniqueKey == uniqueKey || it.packageName == uniqueKey })
            saveScreens(current)
        }
    }

    fun updateAppLabelAcrossScreens(uniqueKey: String, newLabel: String) {
        val current = _screens.value.map { screen ->
            val updatedApps = screen.apps.map { app ->
                if (app.uniqueKey == uniqueKey || app.packageName == uniqueKey) {
                    app.copy(customLabel = newLabel.ifBlank { null })
                } else {
                    app
                }
            }
            screen.copy(apps = updatedApps)
        }
        saveScreens(current)
    }

    fun updateAppLabel(screenId: String, uniqueKey: String, newLabel: String) {
        val current = _screens.value.toMutableList()
        val index = current.indexOfFirst { it.id == screenId }
        if (index != -1) {
            val screen = current[index]
            val updatedApps = screen.apps.map { app ->
                if (app.uniqueKey == uniqueKey || app.packageName == uniqueKey) {
                    app.copy(customLabel = newLabel.ifBlank { null })
                } else {
                    app
                }
            }
            current[index] = screen.copy(apps = updatedApps)
            saveScreens(current)
        }
    }

    fun reorderApps(screenId: String, reordered: List<ScreenApp>) {
        val current = _screens.value.toMutableList()
        val index = current.indexOfFirst { it.id == screenId }
        if (index != -1) {
            current[index] = current[index].copy(apps = reordered)
            saveScreens(current)
        }
    }

    companion object {
        private const val KEY_SCREENS = "key_screens_json"
    }
}
