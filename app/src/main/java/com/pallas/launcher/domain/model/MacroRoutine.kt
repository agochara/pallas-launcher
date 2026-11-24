package com.pallas.launcher.domain.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

enum class MacroToggleMode {
    ON,
    OFF,
    TOGGLE
}

enum class MacroAudioStream(val label: String) {
    MEDIA("Media"),
    RING("Ring"),
    NOTIFICATION("Notifications"),
    ALARM("Alarm"),
    CALL("In-Call")
}

enum class MacroRingerMode(val label: String) {
    NORMAL("Normal"),
    VIBRATE("Vibrate"),
    SILENT("Silent")
}

enum class MacroMediaAction(val label: String) {
    PLAY_PAUSE("Play / Pause"),
    NEXT("Next Track"),
    PREVIOUS("Previous Track"),
    STOP("Stop")
}

enum class MacroSystemSetting(val label: String) {
    WIFI("Wi-Fi"),
    BLUETOOTH("Bluetooth"),
    MOBILE_DATA("Mobile Data"),
    AIRPLANE("Airplane Mode"),
    LOCATION("Location"),
    BATTERY_SAVER("Battery Saver"),
    DISPLAY("Display"),
    SOUND("Sound & Vibration"),
    DND("Do Not Disturb")
}

enum class MacroQuickPanel(val label: String) {
    INTERNET("Internet (Wi-Fi & Mobile Data)"),
    WIFI("Wi-Fi Panel"),
    BLUETOOTH("Bluetooth"),
    NFC("NFC Panel"),
    VOLUME("Volume Panel")
}

sealed interface MacroStep {
    // Audio & Media
    data class SetVolume(val stream: MacroAudioStream, val levelPercent: Int) : MacroStep
    data class SetRingerMode(val mode: MacroRingerMode) : MacroStep
    data class MediaControl(val action: MacroMediaAction) : MacroStep

    // System & Hardware
    data class SetDnd(
        val mode: MacroToggleMode = MacroToggleMode.ON,
        val dndMode: FocusDndMode = FocusDndMode.PRIORITY
    ) : MacroStep
    data class SetFlashlight(val mode: MacroToggleMode = MacroToggleMode.TOGGLE) : MacroStep
    data class SetBrightness(val levelPercent: Int) : MacroStep
    data class SetAutoBrightness(val enabled: Boolean) : MacroStep
    data class SetAutoRotate(val enabled: Boolean) : MacroStep
    data class SetScreenTimeout(val seconds: Int) : MacroStep
    data class OpenQuickPanel(val panel: MacroQuickPanel) : MacroStep
    data class OpenSystemSetting(val target: MacroSystemSetting) : MacroStep

    // Pallas Launcher Settings
    data class SetThemeMode(val mode: ThemeMode) : MacroStep
    data class SetHideStatusBar(val hidden: Boolean) : MacroStep
    data class SetShowClock(val show: Boolean) : MacroStep
    data class SetShowDate(val show: Boolean) : MacroStep
    data class SetShowBattery(val show: Boolean) : MacroStep
    data class SetWallpaperScrim(val enabled: Boolean) : MacroStep
    data class SetEdgeScrubPosition(val position: EdgeScrubPosition) : MacroStep
    data class SetAutoLaunchSingleMatch(val enabled: Boolean) : MacroStep
    data class SwitchHomeScreen(val screenId: String) : MacroStep

    // Productivity & Utilities
    data class StartTimer(val durationSeconds: Int, val label: String = "") : MacroStep
    data class SetAlarm(val hour: Int, val minute: Int) : MacroStep
    data class AppendQuickNote(val text: String) : MacroStep
    data class WaitDelay(val seconds: Int) : MacroStep

    // Apps & Pallas Tools
    data class LaunchApp(val uniqueKey: String) : MacroStep
    data object OpenNotebook : MacroStep
    data object OpenLatexStudio : MacroStep
    data object OpenAtlas : MacroStep
    data object OpenQuickCapture : MacroStep
    data class OpenUrl(val url: String) : MacroStep

    fun toJson(): JSONObject = JSONObject().apply {
        when (this@MacroStep) {
            is SetVolume -> {
                put("type", "set_volume")
                put("stream", stream.name)
                put("levelPercent", levelPercent)
            }
            is SetRingerMode -> {
                put("type", "set_ringer_mode")
                put("mode", mode.name)
            }
            is MediaControl -> {
                put("type", "media_control")
                put("action", action.name)
            }
            is SetDnd -> {
                put("type", "set_dnd")
                put("mode", mode.name)
                put("dndMode", dndMode.name)
            }
            is SetFlashlight -> {
                put("type", "set_flashlight")
                put("mode", mode.name)
            }
            is SetBrightness -> {
                put("type", "set_brightness")
                put("levelPercent", levelPercent)
            }
            is SetAutoBrightness -> {
                put("type", "set_auto_brightness")
                put("enabled", enabled)
            }
            is SetAutoRotate -> {
                put("type", "set_auto_rotate")
                put("enabled", enabled)
            }
            is SetScreenTimeout -> {
                put("type", "set_screen_timeout")
                put("seconds", seconds)
            }
            is OpenQuickPanel -> {
                put("type", "open_quick_panel")
                put("panel", panel.name)
            }
            is OpenSystemSetting -> {
                put("type", "open_system_setting")
                put("target", target.name)
            }
            is SetThemeMode -> {
                put("type", "set_theme_mode")
                put("mode", mode.name)
            }
            is SetHideStatusBar -> {
                put("type", "set_hide_status_bar")
                put("hidden", hidden)
            }
            is SetShowClock -> {
                put("type", "set_show_clock")
                put("show", show)
            }
            is SetShowDate -> {
                put("type", "set_show_date")
                put("show", show)
            }
            is SetShowBattery -> {
                put("type", "set_show_battery")
                put("show", show)
            }
            is SetWallpaperScrim -> {
                put("type", "set_wallpaper_scrim")
                put("enabled", enabled)
            }
            is SetEdgeScrubPosition -> {
                put("type", "set_edge_scrub_position")
                put("position", position.name)
            }
            is SetAutoLaunchSingleMatch -> {
                put("type", "set_auto_launch")
                put("enabled", enabled)
            }
            is SwitchHomeScreen -> {
                put("type", "switch_home_screen")
                put("screenId", screenId)
            }
            is StartTimer -> {
                put("type", "start_timer")
                put("durationSeconds", durationSeconds)
                put("label", label)
            }
            is SetAlarm -> {
                put("type", "set_alarm")
                put("hour", hour)
                put("minute", minute)
            }
            is AppendQuickNote -> {
                put("type", "append_quick_note")
                put("text", text)
            }
            is WaitDelay -> {
                put("type", "wait_delay")
                put("seconds", seconds)
            }
            is LaunchApp -> {
                put("type", "launch_app")
                put("uniqueKey", uniqueKey)
            }
            is OpenNotebook -> put("type", "open_notebook")
            is OpenLatexStudio -> put("type", "open_latex_studio")
            is OpenAtlas -> put("type", "open_atlas")
            is OpenQuickCapture -> put("type", "open_quick_capture")
            is OpenUrl -> {
                put("type", "open_url")
                put("url", url)
            }
        }
    }

    companion object {
        private fun optStr(obj: JSONObject, key: String): String =
            if (obj.isNull(key)) "" else obj.optString(key, "").trim()

        private inline fun <reified E : Enum<E>> enumOrDefault(raw: String, default: E): E =
            enumValues<E>().firstOrNull { it.name == raw } ?: default

        fun fromJson(obj: JSONObject): MacroStep? {
            val type = optStr(obj, "type")
            return when (type) {
                "set_volume" -> SetVolume(
                    stream = enumOrDefault(optStr(obj, "stream"), MacroAudioStream.MEDIA),
                    levelPercent = obj.optInt("levelPercent", 50).coerceIn(0, 100)
                )
                "set_ringer_mode" -> SetRingerMode(
                    mode = enumOrDefault(optStr(obj, "mode"), MacroRingerMode.NORMAL)
                )
                "media_control" -> MediaControl(
                    action = enumOrDefault(optStr(obj, "action"), MacroMediaAction.PLAY_PAUSE)
                )
                "set_dnd" -> SetDnd(
                    mode = enumOrDefault(optStr(obj, "mode"), MacroToggleMode.ON),
                    dndMode = enumOrDefault(optStr(obj, "dndMode"), FocusDndMode.PRIORITY)
                )
                "set_flashlight" -> SetFlashlight(
                    mode = enumOrDefault(optStr(obj, "mode"), MacroToggleMode.TOGGLE)
                )
                "set_brightness" -> SetBrightness(
                    levelPercent = obj.optInt("levelPercent", 50).coerceIn(0, 100)
                )
                "set_auto_brightness" -> SetAutoBrightness(obj.optBoolean("enabled", true))
                "set_auto_rotate" -> SetAutoRotate(obj.optBoolean("enabled", true))
                "set_screen_timeout" -> SetScreenTimeout(
                    seconds = obj.optInt("seconds", 60).coerceIn(15, 1800)
                )
                "open_quick_panel" -> OpenQuickPanel(
                    panel = enumOrDefault(optStr(obj, "panel"), MacroQuickPanel.INTERNET)
                )
                "open_system_setting" -> OpenSystemSetting(
                    target = enumOrDefault(optStr(obj, "target"), MacroSystemSetting.WIFI)
                )
                "set_theme_mode" -> SetThemeMode(
                    mode = enumOrDefault(optStr(obj, "mode"), ThemeMode.DARK)
                )
                "set_hide_status_bar" -> SetHideStatusBar(obj.optBoolean("hidden", false))
                "set_show_clock" -> SetShowClock(obj.optBoolean("show", true))
                "set_show_date" -> SetShowDate(obj.optBoolean("show", true))
                "set_show_battery" -> SetShowBattery(obj.optBoolean("show", true))
                "set_wallpaper_scrim" -> SetWallpaperScrim(obj.optBoolean("enabled", false))
                "set_edge_scrub_position" -> SetEdgeScrubPosition(
                    position = enumOrDefault(optStr(obj, "position"), EdgeScrubPosition.RIGHT)
                )
                "set_auto_launch" -> SetAutoLaunchSingleMatch(obj.optBoolean("enabled", false))
                "switch_home_screen" -> {
                    val id = optStr(obj, "screenId")
                    if (id.isEmpty()) null else SwitchHomeScreen(id)
                }
                "start_timer" -> {
                    val secs = obj.optInt("durationSeconds", 0).coerceIn(1, 24 * 3600)
                    StartTimer(durationSeconds = secs, label = optStr(obj, "label"))
                }
                "set_alarm" -> SetAlarm(
                    hour = obj.optInt("hour", 7).coerceIn(0, 23),
                    minute = obj.optInt("minute", 0).coerceIn(0, 59)
                )
                "append_quick_note" -> {
                    val txt = optStr(obj, "text")
                    if (txt.isEmpty()) null else AppendQuickNote(txt)
                }
                "wait_delay" -> WaitDelay(seconds = obj.optInt("seconds", 1).coerceIn(1, 60))
                "launch_app" -> {
                    val key = optStr(obj, "uniqueKey")
                    if (key.isEmpty()) null else LaunchApp(key)
                }
                "open_notebook" -> OpenNotebook
                "open_latex_studio" -> OpenLatexStudio
                "open_atlas" -> OpenAtlas
                "open_quick_capture" -> OpenQuickCapture
                "open_url" -> {
                    val url = optStr(obj, "url")
                    if (url.isEmpty()) null else OpenUrl(url)
                }
                else -> null
            }
        }
    }
}

data class MacroRoutine(
    val id: String,
    val name: String,
    val triggerWord: String,
    val steps: List<MacroStep>,
    val scheduleEnabled: Boolean = false,
    val scheduleHour: Int = 8,
    val scheduleMinute: Int = 0,
    val scheduleDays: Set<Int> = ALL_DAYS
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("triggerWord", triggerWord)
        put("steps", JSONArray().also { arr -> steps.forEach { arr.put(it.toJson()) } })
        put("scheduleEnabled", scheduleEnabled)
        put("scheduleHour", scheduleHour)
        put("scheduleMinute", scheduleMinute)
        put("scheduleDays", JSONArray(scheduleDays.sorted()))
    }

    companion object {
        val ALL_DAYS: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7)

        private val RESERVED_TRIGGERS = setOf(
            "focus", "give", "fl", "dnd", "cal", "agenda", "settings", "pallas",
            "al", "alarm", "timer", "q", "query", "call", "tex", "convert",
            "st", "w", "weather", "sun", "sunrise", "sunset", "wa", "wiki",
            "tz", "def", "n", "qr", "emi"
        )

        fun validateTrigger(
            raw: String,
            existingTriggers: Collection<String> = emptyList(),
            ignoreExistingTrigger: String? = null
        ): String? {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return "Trigger word cannot be empty"
            if (trimmed.any { it.isWhitespace() }) return "Trigger word must be a single word"
            if (trimmed.startsWith("@") || trimmed.startsWith("#") || trimmed.startsWith("=")) {
                return "Trigger word cannot start with @, #, or ="
            }
            val lower = trimmed.lowercase(Locale.ROOT)
            if (lower in RESERVED_TRIGGERS) return "'$trimmed' is a built-in command"
            val conflict = existingTriggers.any { existing ->
                existing.trim().equals(trimmed, ignoreCase = true) &&
                    (ignoreExistingTrigger == null || !existing.trim().equals(ignoreExistingTrigger.trim(), ignoreCase = true))
            }
            if (conflict) return "Trigger '$trimmed' is already used by another macro"
            return null
        }

        fun fromJson(obj: JSONObject): MacroRoutine? {
            val id = if (obj.isNull("id")) "" else obj.optString("id", "").trim()
            val name = if (obj.isNull("name")) "" else obj.optString("name", "").trim()
            val triggerWord = if (obj.isNull("triggerWord")) "" else obj.optString("triggerWord", "").trim()
            if (id.isEmpty() || name.isEmpty() || triggerWord.isEmpty()) return null

            val stepsArr = obj.optJSONArray("steps") ?: JSONArray()
            val steps = buildList {
                for (i in 0 until stepsArr.length()) {
                    val stepObj = stepsArr.optJSONObject(i) ?: continue
                    MacroStep.fromJson(stepObj)?.let { add(it) }
                }
            }
            val daysArr = obj.optJSONArray("scheduleDays")
            val days = if (daysArr == null) {
                ALL_DAYS
            } else {
                buildSet {
                    for (i in 0 until daysArr.length()) {
                        val d = daysArr.optInt(i, -1)
                        if (d in 1..7) add(d)
                    }
                }.ifEmpty { ALL_DAYS }
            }
            return MacroRoutine(
                id = id,
                name = name,
                triggerWord = triggerWord,
                steps = steps,
                scheduleEnabled = obj.optBoolean("scheduleEnabled", false),
                scheduleHour = obj.optInt("scheduleHour", 8).coerceIn(0, 23),
                scheduleMinute = obj.optInt("scheduleMinute", 0).coerceIn(0, 59),
                scheduleDays = days
            )
        }
    }
}
