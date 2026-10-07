package com.pallas.launcher.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.pow

class AppUsageTracker(context: Context? = null) {

    private val prefs: SharedPreferences? =
        context?.getSharedPreferences("pallas_app_usage", Context.MODE_PRIVATE)

    private val launchCounts = ConcurrentHashMap<String, Int>()
    private val lastUsedMap = ConcurrentHashMap<String, Long>()

    // NOTE: must be declared BEFORE the init block that fills it.
    private val _recentPackages = MutableStateFlow<List<String>>(emptyList())

    /** Most recently launched packages (newest first). Emits on every launch so the UI stays live. */
    val recentPackages: StateFlow<List<String>> = _recentPackages.asStateFlow()

    init {
        loadCache()
        _recentPackages.value = getRecentApps(RECENT_LIMIT)
    }

    private fun loadCache() {
        prefs?.all?.forEach { (key, value) ->
            if (key.endsWith("_count") && value is Int) {
                launchCounts[key.removeSuffix("_count")] = value
            } else if (key.endsWith("_last_used") && value is Long) {
                lastUsedMap[key.removeSuffix("_last_used")] = value
            }
        }
    }

    fun recordAppLaunch(packageName: String) {
        val now = System.currentTimeMillis()
        val count = (launchCounts[packageName] ?: 0) + 1
        launchCounts[packageName] = count
        lastUsedMap[packageName] = now

        prefs?.edit()
            ?.putInt("${packageName}_count", count)
            ?.putLong("${packageName}_last_used", now)
            ?.apply()

        _recentPackages.value = getRecentApps(RECENT_LIMIT)
    }

    fun getAppLaunchCount(packageName: String): Int = launchCounts[packageName] ?: 0

    fun getLastUsed(packageName: String): Long = lastUsedMap[packageName] ?: 0L

    fun getUsageBonus(packageName: String): Double {
        val count = getAppLaunchCount(packageName)
        if (count == 0) return 0.0

        val lastUsed = getLastUsed(packageName)
        if (lastUsed == 0L) return minOf(count * 10.0, 50.0)

        val hoursSinceUse = (System.currentTimeMillis() - lastUsed) / (1000.0 * 3600.0)
        val recencyFactor = 0.5.pow(hoursSinceUse / 48.0) // half-life ~48h
        val bonus = minOf(count * 15.0, 100.0) * recencyFactor + (if (hoursSinceUse < 24.0) 50.0 else 0.0)
        return minOf(bonus, 150.0)
    }

    fun getRecentApps(limit: Int = 6): List<String> =
        lastUsedMap.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key }

    private companion object {
        const val RECENT_LIMIT = 12 // a few spare so uninstalled packages can be filtered out later
    }
}