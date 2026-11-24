package com.pallas.launcher.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.pallas.launcher.domain.model.MacroRoutine
import java.util.Calendar
import java.util.Locale

class MacroScheduler(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val lock = Any()

    fun collectDueMacros(
        macros: List<MacroRoutine>,
        nowMillis: Long = System.currentTimeMillis()
    ): List<MacroRoutine> {
        if (macros.none { it.scheduleEnabled }) return emptyList()
        synchronized(lock) {
            val todayKey = dateKeyFor(nowMillis)
            val due = mutableListOf<MacroRoutine>()
            val editor = prefs.edit()
            var changed = false

            for (macro in macros) {
                if (!macro.scheduleEnabled) continue
                val prefKey = KEY_LAST_FIRED_PREFIX + macro.id
                val lastFired = prefs.getString(prefKey, null)
                if (shouldFireAt(macro, nowMillis, lastFired)) {
                    due += macro
                    editor.putString(prefKey, todayKey)
                    changed = true
                }
            }
            if (changed) editor.apply()
            return due
        }
    }

    companion object {
        private const val PREFS_NAME = "pallas_macro_schedule"
        private const val KEY_LAST_FIRED_PREFIX = "last_fired_"
        private const val CATCH_UP_WINDOW_MINUTES = 5

        fun dateKeyFor(nowMillis: Long): String {
            val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
            return String.format(
                Locale.US,
                "%04d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
            )
        }

        fun shouldFireAt(
            macro: MacroRoutine,
            nowMillis: Long,
            lastFiredDateKey: String?
        ): Boolean {
            if (!macro.scheduleEnabled) return false
            val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun .. 7=Sat
            if (dayOfWeek !in macro.scheduleDays) return false

            val todayKey = dateKeyFor(nowMillis)
            if (lastFiredDateKey == todayKey) return false

            val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val targetMinutes = macro.scheduleHour * 60 + macro.scheduleMinute
            return nowMinutes in targetMinutes..(targetMinutes + CATCH_UP_WINDOW_MINUTES)
        }
    }
}
