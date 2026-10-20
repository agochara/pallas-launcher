package com.pallas.launcher.platform

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
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
            if (intent.resolveActivity(context.packageManager) != null) {
                try {
                    context.startActivity(intent)
                    return
                } catch (_: Exception) {}
            }
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
            Toast.makeText(context, "Calendar app not found", Toast.LENGTH_SHORT).show()
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

    private var isTorchOn = false
    private var torchCallbackRegistered = false

    private fun registerTorchCallbackIfNeeded(context: Context, cameraManager: CameraManager) {
        if (!torchCallbackRegistered) {
            try {
                cameraManager.registerTorchCallback(context.mainExecutor, object : CameraManager.TorchCallback() {
                    override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                        isTorchOn = enabled
                    }

                    override fun onTorchModeUnavailable(cameraId: String) {
                        isTorchOn = false
                    }
                })
                torchCallbackRegistered = true
            } catch (_: Exception) {}
        }
    }

    fun toggleTorch(context: Context): Boolean {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return false
        registerTorchCallbackIfNeeded(context, cameraManager)
        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return false
            val newState = !isTorchOn
            cameraManager.setTorchMode(cameraId, newState)
            isTorchOn = newState
            Toast.makeText(context, if (newState) "Flashlight ON" else "Flashlight OFF", Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Flashlight unavailable", Toast.LENGTH_SHORT).show()
            false
        }
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

    fun setTimer(context: Context, durationSeconds: Int, message: String? = null) {
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, durationSeconds)
            if (!message.isNullOrBlank()) {
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
            }
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
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

    fun openUrl(context: Context, url: String) {
        val targetUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else url
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
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
