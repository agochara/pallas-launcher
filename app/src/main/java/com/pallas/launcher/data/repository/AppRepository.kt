package com.pallas.launcher.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.net.Uri
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import android.widget.Toast
import com.pallas.launcher.domain.model.AppItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class AppRepository(
    private val context: Context,
    private val scope: CoroutineScope,
    private val settingsRepository: SettingsRepository? = null
) {
    private val launcherApps: LauncherApps by lazy {
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    }
    private val userManager: UserManager by lazy {
        context.getSystemService(Context.USER_SERVICE) as UserManager
    }

    private val _rawInstalledApps = MutableStateFlow<List<AppItem>>(emptyList())
    val rawInstalledApps: StateFlow<List<AppItem>> = _rawInstalledApps.asStateFlow()

    val installedApps: StateFlow<List<AppItem>> = if (settingsRepository != null) {
        combine(_rawInstalledApps, settingsRepository.settings) { apps, settings ->
            if (settings.hiddenApps.isEmpty()) apps
            else apps.filterNot { settings.hiddenApps.contains(it.packageName) }
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())
    } else {
        _rawInstalledApps.asStateFlow()
    }

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    private var launcherAppsCallback: LauncherApps.Callback? = null
    private var reloadJob: Job? = null

    init {
        registerLauncherCallback()
        reloadApps()
    }

    private fun registerLauncherCallback() {
        val callback = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String, user: UserHandle) {
                reloadApps()
            }

            override fun onPackageAdded(packageName: String, user: UserHandle) {
                reloadApps()
            }

            override fun onPackageChanged(packageName: String, user: UserHandle) {
                reloadApps()
            }

            override fun onPackagesAvailable(
                packageNames: Array<out String>,
                user: UserHandle,
                replacing: Boolean
            ) {
                reloadApps()
            }

            override fun onPackagesUnavailable(
                packageNames: Array<out String>,
                user: UserHandle,
                replacing: Boolean
            ) {
                reloadApps()
            }

            override fun onShortcutsChanged(
                packageName: String,
                shortcuts: MutableList<ShortcutInfo>,
                user: UserHandle
            ) {
                // no-op for app list
            }
        }
        launcherApps.registerCallback(callback)
        launcherAppsCallback = callback
    }

    fun unregister() {
        launcherAppsCallback?.let { launcherApps.unregisterCallback(it) }
        launcherAppsCallback = null
    }

    fun reloadApps() {
        reloadJob?.cancel()
        reloadJob = scope.launch(Dispatchers.IO) {
            val apps = queryInstalledApps()
            _rawInstalledApps.value = apps
            _isLoaded.value = true
        }
    }

    private suspend fun queryInstalledApps(): List<AppItem> = withContext(Dispatchers.IO) {
        val appList = mutableListOf<AppItem>()
        val collator = Collator.getInstance(Locale.getDefault())

        try {
            val profiles = userManager.userProfiles
            for (profile in profiles) {
                val activityList = launcherApps.getActivityList(null, profile)
                for (activity in activityList) {
                    val pkg = activity.applicationInfo.packageName
                    // Skip our own launcher package
                    if (pkg == context.packageName) continue

                    val label = activity.label?.toString() ?: pkg
                    val serial = userManager.getSerialNumberForUser(profile)
                    val appItem = AppItem(
                        packageName = pkg,
                        activityName = activity.componentName.className,
                        label = label,
                        userHandle = profile,
                        userSerialNumber = serial,
                        installTime = activity.firstInstallTime
                    )
                    appList.add(appItem)
                }
            }
            appList.sortWith(compareBy(collator) { it.label })
        } catch (e: Exception) {
            e.printStackTrace()
        }

        appList
    }

    fun launchApp(packageName: String, activityName: String?, userHandle: UserHandle? = null) {
        val targetUser = userHandle ?: Process.myUserHandle()
        val activityList = try {
            launcherApps.getActivityList(packageName, targetUser)
        } catch (_: Exception) {
            emptyList()
        }

        val targetComponent = if (!activityName.isNullOrBlank() && activityList.any { it.componentName.className == activityName }) {
            ComponentName(packageName, activityName)
        } else if (activityList.isNotEmpty()) {
            activityList.first().componentName
        } else {
            Toast.makeText(context, "App not found", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val opts = android.app.ActivityOptions.makeCustomAnimation(context, android.R.anim.fade_in, android.R.anim.fade_out).toBundle()
            launcherApps.startMainActivity(targetComponent, targetUser, null, opts)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to launch app", Toast.LENGTH_SHORT).show()
        }
    }

    fun openAppInfo(packageName: String, activityName: String? = null, userHandle: UserHandle? = null) {
        try {
            val targetUser = userHandle ?: Process.myUserHandle()
            val component = if (activityName != null) ComponentName(packageName, activityName) else ComponentName(packageName, "")
            launcherApps.startAppDetailsActivity(component, targetUser, null, null)
        } catch (e: Exception) {
            // Fallback
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (ex: Exception) {
                Toast.makeText(context, "Cannot open app info", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun getAppShortcuts(packageName: String, userHandle: UserHandle? = null): List<ShortcutInfo> {
        val targetUser = userHandle ?: Process.myUserHandle()
        return try {
            if (launcherApps.hasShortcutHostPermission()) {
                val query = LauncherApps.ShortcutQuery().apply {
                    setPackage(packageName)
                    setQueryFlags(
                        LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                                LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED or
                                LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
                    )
                }
                launcherApps.getShortcuts(query, targetUser) ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

