package com.pallas.launcher.platform

import android.app.NotificationManager
import android.app.role.RoleManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast
import com.pallas.launcher.domain.model.MacroAudioStream
import com.pallas.launcher.domain.model.MacroMediaAction
import com.pallas.launcher.domain.model.MacroQuickPanel
import com.pallas.launcher.domain.model.MacroRingerMode
import com.pallas.launcher.domain.model.MacroSystemSetting
import com.pallas.launcher.domain.model.WebSearchEngine
import java.lang.reflect.Method

object SystemActionsManager {

    fun openClock(context: Context) {
        val intents = listOf(
            Intent(AlarmClock.ACTION_SHOW_ALARMS),
            Intent(AlarmClock.ACTION_SET_ALARM),
            Intent(AlarmClock.ACTION_SHOW_TIMERS)
        )
        for (intent in intents) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
                return
            } catch (_: Exception) {}
        }
        Toast.makeText(context, "Clock app not found", Toast.LENGTH_SHORT).show()
    }

    fun openCalendar(context: Context) {
        val builder = CalendarContract.CONTENT_URI.buildUpon()
        builder.appendPath("time")
        builder.appendPath(System.currentTimeMillis().toString())
        val intent = Intent(Intent.ACTION_VIEW, builder.build())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallback = Intent.makeMainSelectorActivity(
                    Intent.ACTION_MAIN,
                    Intent.CATEGORY_APP_CALENDAR
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(fallback)
            } catch (_: Exception) {
                Toast.makeText(context, "Calendar app not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun openCamera(context: Context) {
        val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Camera not found", Toast.LENGTH_SHORT).show()
        }
    }

    fun openDialer(context: Context) {
        val intent = Intent(Intent.ACTION_DIAL)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Dialer not found", Toast.LENGTH_SHORT).show()
        }
    }

    fun callPhoneNumber(context: Context, phoneNumber: String) {
        val uri = Uri.fromParts("tel", phoneNumber, null)
        val intent = Intent(Intent.ACTION_DIAL, uri)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Could not dial number", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendSms(context: Context, phoneNumber: String) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(phoneNumber)}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Could not open messaging app", Toast.LENGTH_SHORT).show()
        }
    }

    fun openWhatsAppChat(context: Context, phoneNumber: String) {
        val clean = phoneNumber.filter { it.isDigit() }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$clean"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Could not open WhatsApp", Toast.LENGTH_SHORT).show()
        }
    }

    @Volatile
    private var isTorchOn = false
    @Volatile
    private var torchCallbackRegistered = false
    @Volatile
    private var flashCameraId: String? = null

    private fun resolveFlashCameraId(cameraManager: CameraManager): String? {
        flashCameraId?.let { return it }
        val resolved = try {
            cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (_: Exception) {
            null
        }
        if (resolved != null) flashCameraId = resolved
        return resolved
    }

    private fun registerTorchCallbackIfNeeded(context: Context, cameraManager: CameraManager) {
        if (!torchCallbackRegistered) {
            try {
                val appContext = context.applicationContext
                cameraManager.registerTorchCallback(appContext.mainExecutor, object : CameraManager.TorchCallback() {
                    override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                        val targetId = flashCameraId ?: resolveFlashCameraId(cameraManager)
                        if (targetId == null || cameraId == targetId) {
                            isTorchOn = enabled
                        }
                    }

                    override fun onTorchModeUnavailable(cameraId: String) {
                        val targetId = flashCameraId
                        if (targetId == null || cameraId == targetId) {
                            isTorchOn = false
                        }
                    }
                })
                torchCallbackRegistered = true
            } catch (_: Exception) {}
        }
    }

    fun toggleTorch(context: Context): Boolean {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return false
        registerTorchCallbackIfNeeded(context, cameraManager)
        return setTorch(context, !isTorchOn)
    }

    fun setTorch(context: Context, enabled: Boolean, showToast: Boolean = true): Boolean {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return false
        registerTorchCallbackIfNeeded(context, cameraManager)
        return try {
            val cameraId = resolveFlashCameraId(cameraManager) ?: return false
            cameraManager.setTorchMode(cameraId, enabled)
            isTorchOn = enabled
            if (showToast) {
                Toast.makeText(context, if (enabled) "Flashlight ON" else "Flashlight OFF", Toast.LENGTH_SHORT).show()
            }
            true
        } catch (e: Exception) {
            if (showToast) {
                Toast.makeText(context, "Flashlight unavailable", Toast.LENGTH_SHORT).show()
            }
            false
        }
    }

    fun isDndAccessGranted(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return false
        return nm.isNotificationPolicyAccessGranted
    }

    fun getCurrentInterruptionFilter(context: Context): Int {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return NotificationManager.INTERRUPTION_FILTER_ALL
        return nm.currentInterruptionFilter
    }

    fun isDndEnabled(context: Context): Boolean {
        val filter = getCurrentInterruptionFilter(context)
        return filter != NotificationManager.INTERRUPTION_FILTER_ALL &&
            filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
    }

    fun openDndAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            Toast.makeText(context, "Grant Do Not Disturb access to Pallas Launcher", Toast.LENGTH_LONG).show()
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Cannot open Do Not Disturb settings", Toast.LENGTH_SHORT).show()
        }
    }

    fun setDnd(
        context: Context,
        enabled: Boolean,
        filter: Int = NotificationManager.INTERRUPTION_FILTER_PRIORITY,
        showToast: Boolean = true
    ): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return false
        if (!nm.isNotificationPolicyAccessGranted) {
            if (showToast) {
                openDndAccessSettings(context)
            }
            return false
        }
        return try {
            val targetFilter = if (enabled) filter else NotificationManager.INTERRUPTION_FILTER_ALL
            nm.setInterruptionFilter(targetFilter)
            if (showToast) {
                Toast.makeText(context, if (enabled) "Do Not Disturb ON" else "Do Not Disturb OFF", Toast.LENGTH_SHORT).show()
            }
            true
        } catch (_: SecurityException) {
            if (showToast) {
                openDndAccessSettings(context)
            }
            false
        }
    }

    fun toggleDnd(context: Context): Boolean {
        val currentlyOn = isDndEnabled(context)
        return setDnd(context, enabled = !currentlyOn)
    }

    fun setStreamVolumePercent(context: Context, stream: MacroAudioStream, percent: Int) {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val androidStream = when (stream) {
            MacroAudioStream.MEDIA -> AudioManager.STREAM_MUSIC
            MacroAudioStream.RING -> AudioManager.STREAM_RING
            MacroAudioStream.NOTIFICATION -> AudioManager.STREAM_NOTIFICATION
            MacroAudioStream.ALARM -> AudioManager.STREAM_ALARM
            MacroAudioStream.CALL -> AudioManager.STREAM_VOICE_CALL
        }
        try {
            val min = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) audio.getStreamMinVolume(androidStream) else 0
            val max = audio.getStreamMaxVolume(androidStream)
            val clampedPct = percent.coerceIn(0, 100)
            val target = min + ((max - min) * (clampedPct / 100.0)).toInt().coerceIn(0, max - min)
            audio.setStreamVolume(androidStream, target, 0)
        } catch (_: SecurityException) {
            openDndAccessSettings(context)
        } catch (_: Exception) {
        }
    }

    fun setRingerMode(context: Context, mode: MacroRingerMode) {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val target = when (mode) {
            MacroRingerMode.NORMAL -> AudioManager.RINGER_MODE_NORMAL
            MacroRingerMode.VIBRATE -> AudioManager.RINGER_MODE_VIBRATE
            MacroRingerMode.SILENT -> AudioManager.RINGER_MODE_SILENT
        }
        try {
            audio.ringerMode = target
        } catch (_: SecurityException) {
            openDndAccessSettings(context)
        }
    }

    fun dispatchMediaAction(context: Context, action: MacroMediaAction) {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val keyCode = when (action) {
            MacroMediaAction.PLAY_PAUSE -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            MacroMediaAction.NEXT -> KeyEvent.KEYCODE_MEDIA_NEXT
            MacroMediaAction.PREVIOUS -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            MacroMediaAction.STOP -> KeyEvent.KEYCODE_MEDIA_STOP
        }
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    fun setScreenBrightnessPercent(context: Context, percent: Int) {
        if (!Settings.System.canWrite(context)) {
            openWriteSettingsAccess(context)
            return
        }
        try {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            val value = ((percent.coerceIn(0, 100) / 100.0) * 255.0).toInt().coerceIn(1, 255)
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
        } catch (_: Exception) {
        }
    }

    private fun openWriteSettingsAccess(context: Context) {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            Toast.makeText(context, "Allow Modify System Settings", Toast.LENGTH_LONG).show()
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun setAutoBrightness(context: Context, enabled: Boolean) {
        if (!Settings.System.canWrite(context)) {
            openWriteSettingsAccess(context)
            return
        }
        try {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                if (enabled) Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
        } catch (_: Exception) {}
    }

    fun setAutoRotate(context: Context, enabled: Boolean) {
        if (!Settings.System.canWrite(context)) {
            openWriteSettingsAccess(context)
            return
        }
        try {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                if (enabled) 1 else 0
            )
        } catch (_: Exception) {}
    }

    fun setScreenTimeoutSeconds(context: Context, seconds: Int) {
        if (!Settings.System.canWrite(context)) {
            openWriteSettingsAccess(context)
            return
        }
        try {
            val ms = seconds.coerceIn(15, 1800) * 1000
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, ms)
        } catch (_: Exception) {}
    }

    fun openQuickPanel(context: Context, panel: MacroQuickPanel) {
        when (panel) {
            MacroQuickPanel.INTERNET -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        context.startActivity(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        return
                    } catch (_: Exception) {}
                }
                openSystemSetting(context, Settings.ACTION_WIRELESS_SETTINGS)
            }
            MacroQuickPanel.WIFI -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        context.startActivity(Intent(Settings.Panel.ACTION_WIFI).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        return
                    } catch (_: Exception) {}
                }
                openSystemSetting(context, Settings.ACTION_WIFI_SETTINGS)
            }
            MacroQuickPanel.NFC -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        context.startActivity(Intent(Settings.Panel.ACTION_NFC).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        return
                    } catch (_: Exception) {}
                }
                openSystemSetting(context, Settings.ACTION_NFC_SETTINGS)
            }
            MacroQuickPanel.VOLUME -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        context.startActivity(Intent(Settings.Panel.ACTION_VOLUME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        return
                    } catch (_: Exception) {}
                }
                openSystemSetting(context, Settings.ACTION_SOUND_SETTINGS)
            }
            MacroQuickPanel.BLUETOOTH -> {
                openSystemSetting(context, Settings.ACTION_BLUETOOTH_SETTINGS)
            }
        }
    }

    fun openSystemSettingTarget(context: Context, target: MacroSystemSetting) {
        val action = when (target) {
            MacroSystemSetting.WIFI -> Settings.ACTION_WIFI_SETTINGS
            MacroSystemSetting.BLUETOOTH -> Settings.ACTION_BLUETOOTH_SETTINGS
            MacroSystemSetting.MOBILE_DATA -> Settings.ACTION_DATA_ROAMING_SETTINGS
            MacroSystemSetting.AIRPLANE -> Settings.ACTION_AIRPLANE_MODE_SETTINGS
            MacroSystemSetting.LOCATION -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
            MacroSystemSetting.BATTERY_SAVER -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            MacroSystemSetting.DISPLAY -> Settings.ACTION_DISPLAY_SETTINGS
            MacroSystemSetting.SOUND -> Settings.ACTION_SOUND_SETTINGS
            MacroSystemSetting.DND -> Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
        }
        openSystemSetting(context, action)
    }

    fun lockScreen(context: Context) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? android.app.admin.DevicePolicyManager
        try {
            dpm?.lockNow()
        } catch (_: SecurityException) {
            Toast.makeText(context, "Device admin permission required to lock screen", Toast.LENGTH_SHORT).show()
        }
    }

    fun expandStatusBar(context: Context) {
        try {
            val statusBarService = context.getSystemService("statusbar")
            val statusBarManager = Class.forName("android.app.StatusBarManager")
            val expand: Method = statusBarManager.getMethod("expandNotificationsPanel")
            expand.invoke(statusBarService)
        } catch (e: Exception) {
            Toast.makeText(context, "Notification panel expansion not supported on this device", Toast.LENGTH_SHORT).show()
        }
    }

    fun expandQuickSettings(context: Context) {
        try {
            val statusBarService = context.getSystemService("statusbar")
            val statusBarManager = Class.forName("android.app.StatusBarManager")
            val expand = statusBarManager.getMethod("expandSettingsPanel")
            expand.invoke(statusBarService)
        } catch (e: Exception) {
            openSystemSetting(context, Settings.ACTION_SETTINGS)
        }
    }

    fun openSystemSetting(context: Context, action: String) {
        val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Setting unavailable", Toast.LENGTH_SHORT).show()
        }
    }

    fun setTimer(context: Context, durationSeconds: Int, message: String? = null, skipUi: Boolean = false) {
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, durationSeconds)
            if (!message.isNullOrBlank()) {
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
            }
            putExtra(AlarmClock.EXTRA_SKIP_UI, skipUi)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Timer app not available", Toast.LENGTH_SHORT).show()
        }
    }

    fun setAlarm(context: Context, hour: Int, minute: Int) {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Alarm app not available", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchShortcut(
        context: Context,
        packageName: String,
        shortcutId: String,
        userHandle: UserHandle? = null
    ) {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps ?: return
        val user = userHandle ?: Process.myUserHandle()
        try {
            launcherApps.startShortcut(packageName, shortcutId, null, null, user)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open shortcut", Toast.LENGTH_SHORT).show()
        }
    }

    fun isDefaultLauncher(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                return roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            }
        }
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName == context.packageName
    }

    fun requestSetDefaultLauncher(
        context: Context,
        launcher: androidx.activity.result.ActivityResultLauncher<Intent>? = null
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                if (launcher != null) {
                    try {
                        launcher.launch(intent)
                        return
                    } catch (_: Exception) {}
                } else if (context is android.app.Activity) {
                    try {
                        context.startActivityForResult(intent, 1001)
                        return
                    } catch (_: Exception) {}
                }
            }
        }

        // Fallback 1: ACTION_HOME_SETTINGS
        val homeIntent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
            if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            if (launcher != null) {
                launcher.launch(homeIntent)
            } else {
                context.startActivity(homeIntent)
            }
            return
        } catch (_: Exception) {}

        // Fallback 2: ACTION_MANAGE_DEFAULT_APPS_SETTINGS
        val manageAppsIntent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
            if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            if (launcher != null) {
                launcher.launch(manageAppsIntent)
            } else {
                context.startActivity(manageAppsIntent)
            }
            return
        } catch (_: Exception) {}

        // Fallback 3: ACTION_APPLICATION_DETAILS_SETTINGS
        val appDetailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            if (launcher != null) {
                launcher.launch(appDetailsIntent)
            } else {
                context.startActivity(appDetailsIntent)
            }
        } catch (_: Exception) {
            Toast.makeText(context, "Cannot open default app settings", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Opens a web search in the browser chosen in Settings (null = system default). If the chosen browser was
     * uninstalled or refuses the intent we silently fall back to the default browser instead of failing.
     * An empty query just opens the engine's start page.
     */
    fun openWebSearch(
        context: Context,
        query: String,
        engine: WebSearchEngine = WebSearchEngine.GOOGLE,
        browserPackage: String? = null
    ) {
        val trimmed = query.trim()
        val url = if (trimmed.isEmpty()) {
            Uri.parse(engine.urlTemplate).let { "${it.scheme}://${it.authority}/" }
        } else {
            engine.urlTemplate.replace("%s", Uri.encode(trimmed))
        }
        val base = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        if (!browserPackage.isNullOrBlank()) {
            try {
                context.startActivity(Intent(base).setPackage(browserPackage))
                return
            } catch (_: Exception) {
                // fall through to the default browser
            }
        }
        try {
            context.startActivity(base)
        } catch (_: Exception) {
            Toast.makeText(context, "No web browser found", Toast.LENGTH_SHORT).show()
        }
    }

    fun openUrl(context: Context, url: String, browserPackage: String? = null) {
        val targetUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else url
        val base = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (!browserPackage.isNullOrBlank()) {
            try {
                context.startActivity(Intent(base).setPackage(browserPackage))
                return
            } catch (_: Exception) {
                // fall through to the default browser
            }
        }
        try {
            context.startActivity(base)
        } catch (_: Exception) {
            Toast.makeText(context, "Cannot open URL", Toast.LENGTH_SHORT).show()
        }
    }

    fun uninstallApp(context: Context, packageName: String, userHandle: UserHandle? = null) {
        val targetUser = userHandle ?: android.os.Process.myUserHandle()
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
                putExtra(Intent.EXTRA_USER, targetUser)
                if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                @Suppress("DEPRECATION")
                val fallback = Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
                    data = Uri.parse("package:$packageName")
                    putExtra(Intent.EXTRA_USER, targetUser)
                    if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            } catch (e: Exception) {
                Toast.makeText(context, "Cannot request uninstall", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
