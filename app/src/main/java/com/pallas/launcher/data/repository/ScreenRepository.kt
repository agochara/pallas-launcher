package com.pallas.launcher.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.os.UserManager
import android.util.Log
import com.pallas.launcher.domain.model.AppItem
import com.pallas.launcher.domain.model.HomeScreen
import com.pallas.launcher.domain.model.NameLengthSortOrder
import com.pallas.launcher.domain.model.ScreenApp
import com.pallas.launcher.domain.util.VisualTextMetrics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private const val TAG = "ScreenRepository"

class ScreenRepository(
    private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("pallas_screens", Context.MODE_PRIVATE)

    private val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager

    private val _screens = MutableStateFlow<List<HomeScreen>>(loadScreens())
    val screens: StateFlow<List<HomeScreen>> = _screens.asStateFlow()

    private val _customAppLabels = MutableStateFlow<Map<String, String>>(loadCustomAppLabels())
    val customAppLabels: StateFlow<Map<String, String>> = _customAppLabels.asStateFlow()

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) optString(key) else null

    private fun loadScreens(): List<HomeScreen> = parseScreens(prefs.getString(KEY_SCREENS, null))

    private fun loadCustomAppLabels(): Map<String, String> {
        val raw = prefs.getString(KEY_CUSTOM_APP_LABELS, null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            buildMap {
                obj.keys().forEach { k ->
                    val cleanKey = k.trim()
                    val v = obj.optStringOrNull(k)?.trim()
                    if (cleanKey.isNotEmpty() && !v.isNullOrEmpty()) put(cleanKey, v)
                }
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private val lock = Any()

    private fun mutateScreens(block: (List<HomeScreen>) -> List<HomeScreen>) {
        val updated = synchronized(lock) {
            val res = block(_screens.value)
            try {
                prefs.edit().putString(KEY_SCREENS, serializeScreens(res).toString()).commit()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save screens to SharedPreferences", e)
            }
            res
        }
        _screens.value = updated
    }

    private fun mutateCustomAppLabels(block: (Map<String, String>) -> Map<String, String>) {
        val updated = synchronized(lock) {
            val res = block(_customAppLabels.value)
            try {
                val obj = JSONObject()
                res.forEach { (k, v) -> if (v.isNotBlank()) obj.put(k, v) }
                prefs.edit().putString(KEY_CUSTOM_APP_LABELS, obj.toString()).commit()
            } catch (_: Exception) {
            }
            res
        }
        _customAppLabels.value = updated
    }

    /** Parses the persisted/exported screens JSON. Never throws; falls back to a single empty "Home" screen. */
    fun parseScreens(rawJson: String?): List<HomeScreen> {
        val fallback = listOf(HomeScreen(id = "home_main", name = "Home", contextTag = null, apps = emptyList()))
        if (rawJson.isNullOrBlank()) return fallback

        val myHandle = android.os.Process.myUserHandle()
        val mySerial = try { userManager?.getSerialNumberForUser(myHandle) ?: 0L } catch (e: Exception) { 0L }

        val result = mutableListOf<HomeScreen>()
        try {
            val jsonArray = JSONArray(rawJson)
            for (i in 0 until jsonArray.length()) {
                val screenObj = jsonArray.optJSONObject(i) ?: continue
                try {
                    val id = screenObj.optStringOrNull("id")?.trim().orEmpty().ifBlank { UUID.randomUUID().toString() }
                    val name = screenObj.optStringOrNull("name")?.trim().orEmpty().ifBlank { "Screen ${i + 1}" }
                    val rawTag = screenObj.optStringOrNull("contextTag")
                    val contextTag = rawTag?.trim()?.ifBlank { null }?.takeUnless { it.equals("General", ignoreCase = true) }
                    val appsArray = screenObj.optJSONArray("apps") ?: JSONArray()
                    val apps = mutableListOf<ScreenApp>()
                    for (j in 0 until appsArray.length()) {
                        val appObj = appsArray.optJSONObject(j) ?: continue
                        val pkg = appObj.optStringOrNull("packageName")?.trim().orEmpty()
                        if (pkg.isBlank()) continue
                        val activity = appObj.optStringOrNull("activityName")?.trim().orEmpty()
                        val serial = appObj.optLong("userSerialNumber", 0L)
                        val uniqueKey = appObj.optStringOrNull("uniqueKey")?.trim().orEmpty().ifBlank { "$pkg/$activity#$serial" }
                        val customLabel = appObj.optStringOrNull("customLabel")?.trim()?.ifBlank { null }
                        val defaultLabel = appObj.optStringOrNull("defaultLabel")?.trim().orEmpty()
                        val handle = if (serial == mySerial || (serial == 0L && mySerial == 0L)) {
                            myHandle
                        } else {
                            try { userManager?.getUserForSerialNumber(serial) } catch (_: Exception) { null }
                        }

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
                    result.add(HomeScreen(id = id, name = name, contextTag = contextTag, apps = apps))
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse single screen entry from JSON", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse screens JSON array", e)
        }

        return result.ifEmpty { fallback }
    }

    fun serializeScreens(screenList: List<HomeScreen>): JSONArray {
        val jsonArray = JSONArray()
        for (screen in screenList) {
            val screenObj = JSONObject()
            screenObj.put("id", screen.id)
            screenObj.put("name", screen.name)
            if (screen.contextTag != null) screenObj.put("contextTag", screen.contextTag)
            val appsArray = JSONArray()
            for (app in screen.apps) {
                val appObj = JSONObject()
                appObj.put("uniqueKey", app.uniqueKey)
                appObj.put("packageName", app.packageName)
                appObj.put("activityName", app.activityName)
                appObj.put("userSerialNumber", app.userSerialNumber)
                if (app.customLabel != null) appObj.put("customLabel", app.customLabel)
                appObj.put("defaultLabel", app.defaultLabel)
                appsArray.put(appObj)
            }
            screenObj.put("apps", appsArray)
            jsonArray.put(screenObj)
        }
        return jsonArray
    }

    private fun saveScreens(screenList: List<HomeScreen>) {
        mutateScreens { screenList }
    }

    /** Import: replace every screen with an already-parsed list. */
    fun replaceAll(newScreens: List<HomeScreen>) {
        if (newScreens.isEmpty()) return
        saveScreens(newScreens)
    }

    /** Import: replace the global custom app label map atomically. */
    fun replaceCustomAppLabels(labels: Map<String, String>) {
        mutateCustomAppLabels {
            buildMap {
                labels.forEach { (k, v) ->
                    val cleanKey = k.trim()
                    val cleanVal = v.trim()
                    if (cleanKey.isNotEmpty() && cleanVal.isNotEmpty()) {
                        put(cleanKey, cleanVal)
                    }
                }
            }
        }
    }

    fun addScreen(name: String, contextTag: String? = null) {
        mutateScreens { current ->
            current + HomeScreen(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                contextTag = contextTag?.trim()?.ifBlank { null },
                apps = emptyList()
            )
        }
    }

    fun renameScreen(screenId: String, newName: String, newContextTag: String? = null) {
        mutateScreens { current ->
            current.map { screen ->
                if (screen.id == screenId) {
                    screen.copy(
                        name = newName.trim(),
                        contextTag = newContextTag?.trim()?.ifBlank { null }
                    )
                } else {
                    screen
                }
            }
        }
    }

    fun moveScreen(fromIndex: Int, toIndex: Int) {
        mutateScreens { current ->
            if (fromIndex in current.indices && toIndex in current.indices && fromIndex != toIndex) {
                val list = current.toMutableList()
                val item = list.removeAt(fromIndex)
                list.add(toIndex, item)
                list
            } else {
                current
            }
        }
    }

    fun removeScreen(screenId: String) {
        mutateScreens { current ->
            if (current.size <= 1) current
            else current.filterNot { it.id == screenId }
        }
    }

    init {
        VisualTextMetrics.init(context)
    }

    fun measureVisualWidth(text: String): Float = VisualTextMetrics.measure(text)

    /**
     * Orders labels by the width they really take on screen. Ties are broken alphabetically (locale-aware) so the
     * result is fully deterministic. Used both for display-time sorting and (legacy) persisted sorting.
     */
    fun sortAppsByNameLength(apps: List<ScreenApp>, order: NameLengthSortOrder): List<ScreenApp> {
        if (order == NameLengthSortOrder.OFF || apps.size <= 1) return apps
        val widths = apps.associateWith { measureVisualWidth(it.displayLabel) }
        val collator = java.text.Collator.getInstance().apply { strength = java.text.Collator.PRIMARY }
        val comparator = Comparator<ScreenApp> { a, b ->
            val wA = widths[a] ?: 0f
            val wB = widths[b] ?: 0f
            val cmp = if (order == NameLengthSortOrder.ASCENDING) wA.compareTo(wB) else wB.compareTo(wA)
            if (cmp != 0) cmp else collator.compare(a.displayLabel, b.displayLabel)
        }
        return apps.sortedWith(comparator)
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
        mutateScreens { current ->
            current.map { screen ->
                if (screen.id == screenId) {
                    val existingKeys = screen.apps.map { it.uniqueKey }.toSet()
                    val newApps = appItems
                        .filterNot { existingKeys.contains(it.uniqueKey) }
                        .map { appItem ->
                            val resolvedCustom = customLabel?.ifBlank { null }
                                ?: _customAppLabels.value[appItem.uniqueKey]
                            ScreenApp(
                                uniqueKey = appItem.uniqueKey,
                                packageName = appItem.packageName,
                                activityName = appItem.activityName,
                                customLabel = resolvedCustom,
                                defaultLabel = appItem.label,
                                userSerialNumber = appItem.userSerialNumber,
                                userHandle = appItem.userHandle
                            )
                        }
                    if (newApps.isNotEmpty()) {
                        val combined = screen.apps + newApps
                        val finalApps = sortAppsByNameLength(combined, sortOrder)
                        screen.copy(apps = finalApps)
                    } else {
                        screen
                    }
                } else {
                    screen
                }
            }
        }
    }

    fun sortAllScreensByNameLength(sortOrder: NameLengthSortOrder) {
        if (sortOrder == NameLengthSortOrder.OFF) return
        mutateScreens { current ->
            current.map { screen ->
                screen.copy(apps = sortAppsByNameLength(screen.apps, sortOrder))
            }
        }
    }

    fun removeAppFromScreen(screenId: String, uniqueKey: String) {
        mutateScreens { current ->
            current.map { screen ->
                if (screen.id == screenId) {
                    screen.copy(apps = screen.apps.filterNot { it.uniqueKey == uniqueKey })
                } else {
                    screen
                }
            }
        }
    }

    fun removeAppFromAllScreens(uniqueKey: String) {
        mutateScreens { current ->
            current.map { screen ->
                screen.copy(apps = screen.apps.filterNot { it.uniqueKey == uniqueKey })
            }
        }
    }

    fun updateAppLabelAcrossScreens(
        uniqueKey: String,
        newLabel: String,
        sortOrder: NameLengthSortOrder = NameLengthSortOrder.OFF
    ) {
        val clean = newLabel.trim()
        mutateCustomAppLabels { current ->
            val updated = current.toMutableMap()
            if (clean.isEmpty()) {
                updated.remove(uniqueKey)
            } else {
                updated[uniqueKey] = clean
            }
            updated
        }

        mutateScreens { current ->
            current.map { screen ->
                val updatedApps = screen.apps.map { app ->
                    if (app.uniqueKey == uniqueKey) {
                        app.copy(customLabel = clean.ifBlank { null })
                    } else {
                        app
                    }
                }
                val finalApps = sortAppsByNameLength(updatedApps, sortOrder)
                screen.copy(apps = finalApps)
            }
        }
    }

    fun updateAppLabel(
        screenId: String,
        uniqueKey: String,
        newLabel: String,
        sortOrder: NameLengthSortOrder = NameLengthSortOrder.OFF
    ) {
        val clean = newLabel.trim()
        mutateCustomAppLabels { current ->
            val updated = current.toMutableMap()
            if (clean.isEmpty()) {
                updated.remove(uniqueKey)
            } else {
                updated[uniqueKey] = clean
            }
            updated
        }

        mutateScreens { current ->
            current.map { screen ->
                if (screen.id == screenId) {
                    val updatedApps = screen.apps.map { app ->
                        if (app.uniqueKey == uniqueKey) {
                            app.copy(customLabel = clean.ifBlank { null })
                        } else {
                            app
                        }
                    }
                    val finalApps = sortAppsByNameLength(updatedApps, sortOrder)
                    screen.copy(apps = finalApps)
                } else {
                    screen
                }
            }
        }
    }

    fun reorderApps(screenId: String, reordered: List<ScreenApp>) {
        mutateScreens { current ->
            current.map { screen ->
                if (screen.id == screenId) {
                    screen.copy(apps = reordered)
                } else {
                    screen
                }
            }
        }
    }

    companion object {
        private const val KEY_SCREENS = "key_screens_json"
        private const val KEY_CUSTOM_APP_LABELS = "key_custom_app_labels_json"

        @Volatile
        private var instance: ScreenRepository? = null

        fun getInstance(context: Context): ScreenRepository =
            instance ?: synchronized(this) {
                instance ?: ScreenRepository(context.applicationContext).also { instance = it }
            }
    }
}
