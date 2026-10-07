package com.pallas.launcher.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.os.UserManager
import com.pallas.launcher.domain.model.AppItem
import com.pallas.launcher.domain.model.HomeScreen
import com.pallas.launcher.domain.model.NameLengthSortOrder
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
                    contextTag = null,
                    apps = emptyList()
                )
            )
        }

        val myHandle = android.os.Process.myUserHandle()
        val mySerial = try { userManager?.getSerialNumberForUser(myHandle) ?: 0L } catch (e: Exception) { 0L }

        val result = mutableListOf<HomeScreen>()
        try {
            val jsonArray = JSONArray(rawJson)
            for (i in 0 until jsonArray.length()) {
                val screenObj = jsonArray.getJSONObject(i)
                val id = screenObj.optString("id", UUID.randomUUID().toString())
                val name = screenObj.optString("name", "Screen ${i + 1}")
                val rawTag = if (screenObj.has("contextTag")) screenObj.getString("contextTag") else null
                val contextTag = rawTag?.trim()?.ifBlank { null }?.takeUnless { it.equals("General", ignoreCase = true) }
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
                    val handle = if (serial == mySerial || (serial == 0L && mySerial == 0L)) {
                        myHandle
                    } else {
                        userManager?.getUserForSerialNumber(serial)
                    }

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
            listOf(HomeScreen(id = "home_main", name = "Home", contextTag = null, apps = emptyList()))
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

    fun addScreen(name: String, contextTag: String? = null) {
        val current = _screens.value.toMutableList()
        current.add(
            HomeScreen(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                contextTag = contextTag?.trim()?.ifBlank { null },
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

    private val textPaint: android.graphics.Paint by lazy {
        android.graphics.Paint().apply {
            textSize = 24f
            isAntiAlias = true
        }
    }

    fun measureVisualWidth(text: String): Float {
        if (text.isEmpty()) return 0f
        return try {
            val measured = textPaint.measureText(text)
            if (measured > 0f) {
                measured
            } else {
                estimateVisualWidth(text)
            }
        } catch (_: Throwable) {
            estimateVisualWidth(text)
        }
    }

    private fun estimateVisualWidth(text: String): Float {
        var width = 0f
        for (ch in text) {
            width += when (ch) {
                'W', 'M', 'Q', '—', '…' -> 1.35f
                'w', 'm' -> 1.15f
                'i', 'l', 'j', '!', '|', ':', ';', '\'', '`', '.', ',' -> 0.35f
                'f', 'r', 't', 'I', ' ', '-', '(', ')' -> 0.55f
                in 'A'..'Z' -> 0.95f
                in '0'..'9' -> 0.70f
                else -> 0.70f
            }
        }
        return width
    }

    fun sortAppsByNameLength(apps: List<ScreenApp>, order: NameLengthSortOrder): List<ScreenApp> {
        return when (order) {
            NameLengthSortOrder.OFF -> apps
            NameLengthSortOrder.ASCENDING -> apps.sortedWith(
                compareBy<ScreenApp> { measureVisualWidth(it.displayLabel) }
                    .thenBy { it.displayLabel.lowercase() }
            )
            NameLengthSortOrder.DESCENDING -> apps.sortedWith(
                compareByDescending<ScreenApp> { measureVisualWidth(it.displayLabel) }
                    .thenBy { it.displayLabel.lowercase() }
            )
        }
    }

    fun addAppToScreen(
        screenId: String,
        appItem: AppItem,
        customLabel: String? = null,
        sortOrder: NameLengthSortOrder = NameLengthSortOrder.OFF
    ) {
        addAppsToScreen(screenId, listOf(appItem), sortOrder, customLabel)
    }

    fun addAppsToScreen(
        screenId: String,
        appItems: List<AppItem>,
        sortOrder: NameLengthSortOrder = NameLengthSortOrder.OFF,
        customLabel: String? = null
    ) {
        val current = _screens.value.toMutableList()
        val index = current.indexOfFirst { it.id == screenId }
        if (index != -1) {
            val screen = current[index]
            val existingKeys = screen.apps.map { it.uniqueKey }.toSet()
            val newApps = appItems
                .filterNot { existingKeys.contains(it.uniqueKey) }
                .map { appItem ->
                    ScreenApp(
                        uniqueKey = appItem.uniqueKey,
                        packageName = appItem.packageName,
                        activityName = appItem.activityName,
                        customLabel = customLabel,
                        defaultLabel = appItem.label,
                        userSerialNumber = appItem.userSerialNumber,
                        userHandle = appItem.userHandle
                    )
                }
            if (newApps.isNotEmpty()) {
                val combined = screen.apps + newApps
                val finalApps = sortAppsByNameLength(combined, sortOrder)
                current[index] = screen.copy(apps = finalApps)
                saveScreens(current)
            }
        }
    }

    fun sortAllScreensByNameLength(sortOrder: NameLengthSortOrder) {
        if (sortOrder == NameLengthSortOrder.OFF) return
        val current = _screens.value.map { screen ->
            screen.copy(apps = sortAppsByNameLength(screen.apps, sortOrder))
        }
        saveScreens(current)
    }

    fun removeAppFromScreen(screenId: String, uniqueKey: String) {
        val current = _screens.value.toMutableList()
        val index = current.indexOfFirst { it.id == screenId }
        if (index != -1) {
            val screen = current[index]
            current[index] = screen.copy(apps = screen.apps.filterNot { it.uniqueKey == uniqueKey })
            saveScreens(current)
        }
    }

    fun updateAppLabelAcrossScreens(
        uniqueKey: String,
        newLabel: String,
        sortOrder: NameLengthSortOrder = NameLengthSortOrder.OFF
    ) {
        val current = _screens.value.map { screen ->
            val updatedApps = screen.apps.map { app ->
                if (app.uniqueKey == uniqueKey) {
                    app.copy(customLabel = newLabel.ifBlank { null })
                } else {
                    app
                }
            }
            val finalApps = sortAppsByNameLength(updatedApps, sortOrder)
            screen.copy(apps = finalApps)
        }
        saveScreens(current)
    }

    fun updateAppLabel(
        screenId: String,
        uniqueKey: String,
        newLabel: String,
        sortOrder: NameLengthSortOrder = NameLengthSortOrder.OFF
    ) {
        val current = _screens.value.toMutableList()
        val index = current.indexOfFirst { it.id == screenId }
        if (index != -1) {
            val screen = current[index]
            val updatedApps = screen.apps.map { app ->
                if (app.uniqueKey == uniqueKey) {
                    app.copy(customLabel = newLabel.ifBlank { null })
                } else {
                    app
                }
            }
            val finalApps = sortAppsByNameLength(updatedApps, sortOrder)
            current[index] = screen.copy(apps = finalApps)
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
