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
import android.util.Log
import android.widget.Toast
import com.pallas.launcher.domain.model.AppItem
import com.pallas.launcher.ui.apps.AppIconHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale

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
        combine(
            _rawInstalledApps,
            settingsRepository.settings.map { it.hiddenApps }.distinctUntilChanged()
        ) { apps, hiddenApps ->
            if (hiddenApps.isEmpty()) {
                apps
            } else {
                apps.filterNot { app ->
                    hiddenApps.contains(app.uniqueKey) ||
                        hiddenApps.contains(app.packageName)
                }
            }
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
        reloadApps(clearAllIcons = false)
    }

    private fun registerLauncherCallback() {
        val callback = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String, user: UserHandle) {
                AppIconHelper.invalidate(packageName)
                reloadApps(clearAllIcons = false)
            }

            override fun onPackageAdded(packageName: String, user: UserHandle) {
                AppIconHelper.invalidate(packageName)
                reloadApps(clearAllIcons = false)
            }

            override fun onPackageChanged(packageName: String, user: UserHandle) {
                AppIconHelper.invalidate(packageName)
                reloadApps(clearAllIcons = false)
            }

            override fun onPackagesAvailable(
                packageNames: Array<out String>,
                user: UserHandle,
                replacing: Boolean
            ) {
                packageNames.forEach { AppIconHelper.invalidate(it) }
                reloadApps(clearAllIcons = false)
            }

            override fun onPackagesUnavailable(
                packageNames: Array<out String>,
                user: UserHandle,
                replacing: Boolean
            ) {
                packageNames.forEach { AppIconHelper.invalidate(it) }
                reloadApps(clearAllIcons = false)
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
        reloadJob?.cancel()
        reloadJob = null
        launcherAppsCallback?.let { launcherApps.unregisterCallback(it) }
        launcherAppsCallback = null
    }

    fun reloadApps(clearAllIcons: Boolean = true) {
        reloadJob?.cancel()
        reloadJob = scope.launch(Dispatchers.IO) {
            var delayMs = 500L
            for (attempt in 1..3) {
                val apps = queryInstalledApps()
                if (apps != null) {
                    if (clearAllIcons) AppIconHelper.clear()
                    _rawInstalledApps.value = apps
                    _isLoaded.value = true
                    return@launch
                }
                if (_rawInstalledApps.value.isNotEmpty()) {
                    Log.w("AppRepository", "queryInstalledApps failed on attempt $attempt; retaining existing app list.")
                    return@launch
                }
                delay(delayMs)
                delayMs *= 2
            }
        }
    }

    private suspend fun queryInstalledApps(): List<AppItem>? = withContext(Dispatchers.IO) {
        val appList = mutableListOf<AppItem>()
        val collator = Collator.getInstance(Locale.getDefault())

        try {
            val profiles = userManager.userProfiles ?: listOf(Process.myUserHandle())
            for (profile in profiles) {
                val activityList = launcherApps.getActivityList(null, profile)
                for (activity in activityList) {
                    val pkg = activity.applicationInfo.packageName
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
            appList
        } catch (e: Exception) {
            Log.e("AppRepository", "Error querying installed apps", e)
            null
        }
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
        val targetUser = userHandle ?: Process.myUserHandle()
        val component = if (!activityName.isNullOrBlank()) {
            ComponentName(packageName, activityName)
        } else {
            launcherApps.getActivityList(packageName, targetUser).firstOrNull()?.componentName
        }

        if (component != null) {
            try {
                launcherApps.startAppDetailsActivity(component, targetUser, null, null)
                return
            } catch (_: Exception) {
                // Fall through to system settings intent
            }
        }

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

    companion object {
        @Volatile
        private var instance: AppRepository? = null

        fun getInstance(context: Context): AppRepository =
            instance ?: synchronized(this) {
                val appContext = context.applicationContext
                instance ?: AppRepository(
                    context = appContext,
                    scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
                    settingsRepository = SettingsRepository.getInstance(appContext)
                ).also { instance = it }
            }

        fun resetInstance() {
            synchronized(this) {
                instance?.unregister()
                instance = null
            }
        }
    }
}