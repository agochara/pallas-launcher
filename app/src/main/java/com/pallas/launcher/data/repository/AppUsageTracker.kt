package com.pallas.launcher.data.repository

import android.content.Context
import android.content.SharedPreferences
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.pow

class AppUsageTracker(context: Context? = null) {

    private val prefs: SharedPreferences? =
        context?.getSharedPreferences("pallas_app_usage", Context.MODE_PRIVATE)

    private val launchCounts = ConcurrentHashMap<String, Int>()
    private val lastUsedMap = ConcurrentHashMap<String, Long>()

    init {
        loadCache()
    }

    private fun loadCache() {
        prefs?.all?.forEach { (key, value) ->
            if (key.endsWith("_count") && value is Int) {
                val pkg = key.removeSuffix("_count")
                launchCounts[pkg] = value
            } else if (key.endsWith("_last_used") && value is Long) {
                val pkg = key.removeSuffix("_last_used")
                lastUsedMap[pkg] = value
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
    }

    fun getAppLaunchCount(packageName: String): Int {
        return launchCounts[packageName] ?: 0
    }

    fun getLastUsed(packageName: String): Long {
        return lastUsedMap[packageName] ?: 0L
    }

    fun getUsageBonus(packageName: String): Double {
        val count = getAppLaunchCount(packageName)
        if (count == 0) return 0.0

        val lastUsed = getLastUsed(packageName)
        if (lastUsed == 0L) {
            // Count exists without timestamp: give baseline frequency bonus
            return minOf(count * 10.0, 50.0)
        }

        val hoursSinceUse = (System.currentTimeMillis() - lastUsed) / (1000.0 * 3600.0)
        // Decay factor: half-life around 48 hours
        val recencyFactor = 0.5.pow(hoursSinceUse / 48.0)

        // Frequency bonus decayed by recency
        val bonus = minOf(count * 15.0, 100.0) * recencyFactor + (if (hoursSinceUse < 24.0) 50.0 else 0.0)
        return minOf(bonus, 150.0)
    }

    fun getRecentApps(limit: Int = 6): List<String> {
        return lastUsedMap.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key }
    }
}

