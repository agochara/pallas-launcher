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
import java.lang.reflect.Method

object SystemActionsManager {

    private var isTorchOn = false

    fun openClock(context: Context) {
        val intents = listOf(
            Intent(AlarmClock.ACTION_SHOW_ALARMS),
            Intent(AlarmClock.ACTION_SET_ALARM),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING)
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

    fun dialPhoneNumber(context: Context, phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phoneNumber)}"))
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

    fun toggleTorch(context: Context): Boolean {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return false
        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return false
            isTorchOn = !isTorchOn
            cameraManager.setTorchMode(cameraId, isTorchOn)
            Toast.makeText(context, if (isTorchOn) "Flashlight ON" else "Flashlight OFF", Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Flashlight unavailable", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun expandStatusBar(context: Context) {
        try {
            val statusBarService = context.getSystemService("statusbar")
            val statusBarManager = Class.forName("android.app.StatusBarManager")
            val expand: Method = statusBarManager.getMethod("expandNotificationsPanel")
            expand.invoke(statusBarService)
        } catch (e: Exception) {
            // Silently ignore if not supported or permission lacking
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

    fun setTimer(context: Context, durationMinutes: Int) {
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, durationMinutes * 60)
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

    fun openWebSearch(context: Context, query: String) {
        val uri = Uri.parse("https://www.google.com/search?q=${Uri.encode(query.trim())}")
        val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
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