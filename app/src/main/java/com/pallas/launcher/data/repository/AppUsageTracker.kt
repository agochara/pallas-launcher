package com.pallas.launcher.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.pallas.launcher.domain.model.AppItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.pow

class AppUsageTracker(context: Context? = null) {

    private val prefs: SharedPreferences? =
        context?.applicationContext?.getSharedPreferences("pallas_app_usage", Context.MODE_PRIVATE)

    private val launchCounts = ConcurrentHashMap<String, Int>()
    private val lastUsedMap = ConcurrentHashMap<String, Long>()
    private val scores = ConcurrentHashMap<String, Double>()
    private val scoreStamps = ConcurrentHashMap<String, Long>()

    private val _recentAppKeys = MutableStateFlow<List<String>>(emptyList())
    private val _launchCountsFlow = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val _usageScoresFlow = MutableStateFlow<Map<String, Double>>(emptyMap())

    val recentAppKeys: StateFlow<List<String>> = _recentAppKeys.asStateFlow()
    val recentPackages: StateFlow<List<String>> = _recentAppKeys.asStateFlow()
    val launchCountsSnapshot: StateFlow<Map<String, Int>> = _launchCountsFlow.asStateFlow()
    val usageScoresSnapshot: StateFlow<Map<String, Double>> = _usageScoresFlow.asStateFlow()

    init {
        loadCache()
        publish()
    }

    private fun loadCache() {
        prefs?.all?.forEach { (key, value) ->
            when {
                key.endsWith("_count") && value is Int -> launchCounts[key.removeSuffix("_count")] = value
                key.endsWith("_last_used") && value is Long -> lastUsedMap[key.removeSuffix("_last_used")] = value
                key.endsWith("_score_ts") && value is Long -> scoreStamps[key.removeSuffix("_score_ts")] = value
                key.endsWith("_score") && value is Float -> scores[key.removeSuffix("_score")] = value.toDouble()
            }
        }
        val now = System.currentTimeMillis()
        for ((k, count) in launchCounts) {
            if (!scores.containsKey(k)) {
                scores[k] = count.toDouble()
                scoreStamps[k] = lastUsedMap[k] ?: now
            }
        }
    }

    /** Resolves a key whether it was stored/queried as uniqueKey ("pkg/act#0") or packageName ("pkg"). */
    private fun resolveStoredKey(map: Map<String, *>, key: String): String? {
        if (map.containsKey(key)) return key
        val pkg = key.substringBefore('/')
        if (map.containsKey(pkg)) return pkg
        return map.keys.firstOrNull { it.startsWith("$pkg/") }
    }

    fun migratePackageKeysToUniqueKeys(installedApps: List<AppItem>) {
        if (installedApps.isEmpty() || prefs == null) return
        val editor = prefs.edit()
        var changed = false

        for (app in installedApps) {
            val pkg = app.packageName
            val uniqueKey = app.uniqueKey

            if (launchCounts.containsKey(pkg) && !launchCounts.containsKey(uniqueKey)) {
                val count = launchCounts.remove(pkg) ?: 0
                val lastUsed = lastUsedMap.remove(pkg) ?: 0L
                val score = scores.remove(pkg) ?: count.toDouble()
                val scoreTs = scoreStamps.remove(pkg) ?: lastUsed

                launchCounts[uniqueKey] = count
                lastUsedMap[uniqueKey] = lastUsed
                scores[uniqueKey] = score
                scoreStamps[uniqueKey] = scoreTs

                editor.remove("${pkg}_count")
                    .remove("${pkg}_last_used")
                    .remove("${pkg}_score")
                    .remove("${pkg}_score_ts")

                editor.putInt("${uniqueKey}_count", count)
                    .putLong("${uniqueKey}_last_used", lastUsed)
                    .putFloat("${uniqueKey}_score", score.toFloat())
                    .putLong("${uniqueKey}_score_ts", scoreTs)

                changed = true
            }
        }

        if (changed) {
            editor.apply()
            publish()
        }
    }

    private fun publish() {
        _recentAppKeys.value = getRecentApps(RECENT_LIMIT)
        _launchCountsFlow.value = HashMap(launchCounts)
        val now = System.currentTimeMillis()
        _usageScoresFlow.value = scores.keys.associateWith { getDecayedScore(it, now) }
    }

    fun recordAppLaunch(uniqueKey: String) {
        val now = System.currentTimeMillis()
        val count = getAppLaunchCount(uniqueKey) + 1
        launchCounts[uniqueKey] = count
        lastUsedMap[uniqueKey] = now

        val newScore = getDecayedScore(uniqueKey, now) + 1.0
        scores[uniqueKey] = newScore
        scoreStamps[uniqueKey] = now

        prefs?.edit()
            ?.putInt("${uniqueKey}_count", count)
            ?.putLong("${uniqueKey}_last_used", now)
            ?.putFloat("${uniqueKey}_score", newScore.toFloat())
            ?.putLong("${uniqueKey}_score_ts", now)
            ?.apply()

        publish()
    }

    fun forget(keysOrPackages: Collection<String>) {
        if (keysOrPackages.isEmpty()) return
        var changed = false
        val editor = prefs?.edit()
        for (target in keysOrPackages) {
            val matchingKeys = launchCounts.keys.filter { it == target || it.startsWith("$target/") }
            for (key in matchingKeys) {
                if (launchCounts.remove(key) != null) changed = true
                if (lastUsedMap.remove(key) != null) changed = true
                if (scores.remove(key) != null) changed = true
                scoreStamps.remove(key)
                editor?.remove("${key}_count")?.remove("${key}_last_used")
                    ?.remove("${key}_score")?.remove("${key}_score_ts")
            }
        }
        editor?.apply()
        if (changed) publish()
    }

    fun getAppLaunchCount(key: String): Int {
        val k = resolveStoredKey(launchCounts, key) ?: return 0
        return launchCounts[k] ?: 0
    }

    fun getLastUsed(key: String): Long {
        val k = resolveStoredKey(lastUsedMap, key) ?: return 0L
        return lastUsedMap[k] ?: 0L
    }

    fun getDecayedScore(key: String, now: Long = System.currentTimeMillis()): Double {
        val k = resolveStoredKey(scores, key) ?: return 0.0
        val score = scores[k] ?: return 0.0
        val stamp = scoreStamps[k] ?: return score
        val elapsedMs = (now - stamp).coerceAtLeast(0L).toDouble()
        return score * 0.5.pow(elapsedMs / SCORE_HALF_LIFE_MS)
    }

    fun getUsageBonus(key: String): Double {
        val count = getAppLaunchCount(key)
        if (count == 0) return 0.0

        val lastUsed = getLastUsed(key)
        if (lastUsed == 0L) return minOf(count * 10.0, 50.0)

        val hoursSinceUse = (System.currentTimeMillis() - lastUsed) / (1000.0 * 3600.0)
        val recencyFactor = 0.5.pow(hoursSinceUse / 48.0)
        val bonus = minOf(count * 15.0, 100.0) * recencyFactor + (if (hoursSinceUse < 24.0) 50.0 else 0.0)
        return minOf(bonus, 150.0)
    }

    fun getRecentApps(limit: Int = 6): List<String> =
        lastUsedMap.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key }

    companion object {
        private const val RECENT_LIMIT = 24
        private const val SCORE_HALF_LIFE_DAYS = 21.0
        private const val SCORE_HALF_LIFE_MS = SCORE_HALF_LIFE_DAYS * 24.0 * 3600.0 * 1000.0

        @Volatile
        private var instance: AppUsageTracker? = null

        fun getInstance(context: Context): AppUsageTracker =
            instance ?: synchronized(this) {
                instance ?: AppUsageTracker(context.applicationContext).also { instance = it }
            }
    }
}