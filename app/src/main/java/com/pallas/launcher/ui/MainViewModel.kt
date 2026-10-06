package com.pallas.launcher.ui

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Process
import android.os.UserHandle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pallas.launcher.data.repository.AppRepository
import com.pallas.launcher.data.repository.AppUsageTracker
import com.pallas.launcher.data.repository.ScreenRepository
import com.pallas.launcher.data.repository.SettingsRepository
import com.pallas.launcher.domain.model.AppItem
import com.pallas.launcher.domain.model.AppsHorizontalAlignment
import com.pallas.launcher.domain.model.AppsVerticalPosition
import com.pallas.launcher.domain.model.ClockHorizontalAlignment
import com.pallas.launcher.domain.model.ClockVerticalPosition
import com.pallas.launcher.domain.model.EdgeScrubPosition
import com.pallas.launcher.domain.model.EdgeScrubReleaseAction
import com.pallas.launcher.domain.model.GestureAction
import com.pallas.launcher.domain.model.HomeScreen
import com.pallas.launcher.domain.model.LauncherSettings
import com.pallas.launcher.domain.model.ScreenApp
import com.pallas.launcher.domain.model.ThemeMode
import com.pallas.launcher.domain.search.AppSearchResult
import com.pallas.launcher.domain.search.SearchEngine
import com.pallas.launcher.platform.BatteryManagerHelper
import com.pallas.launcher.platform.SystemActionsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface LauncherOverlay {
    data object None : LauncherOverlay
    data object Drawer : LauncherOverlay
    data object Search : LauncherOverlay
    data object Settings : LauncherOverlay
    data object AppPicker : LauncherOverlay
    data class AppOptions(val app: ScreenApp) : LauncherOverlay
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    private val settingsRepository = SettingsRepository(context)
    val appRepository = AppRepository(context, viewModelScope, settingsRepository)
    private val screenRepository = ScreenRepository(context)

    val installedApps: StateFlow<List<AppItem>> = appRepository.installedApps
    val isAppsLoaded: StateFlow<Boolean> = appRepository.isLoaded
    val screens: StateFlow<List<HomeScreen>> = screenRepository.screens
    val settings: StateFlow<LauncherSettings> = settingsRepository.settings

    private val _currentScreenIndex = MutableStateFlow(0)
    val currentScreenIndex: StateFlow<Int> = combine(_currentScreenIndex, screenRepository.screens) { index, screenList ->
        if (screenList.isEmpty()) 0
        else index.coerceIn(0, screenList.size - 1)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    // Clock, date, battery
    private val _currentTime = MutableStateFlow("")
    val currentTime: StateFlow<String> = _currentTime.asStateFlow()

    private val _currentDate = MutableStateFlow("")
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    private val _batteryLevel = MutableStateFlow(BatteryManagerHelper.getBatteryPercentage(context))
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<AppSearchResult>>(emptyList())
    val searchResults: StateFlow<List<AppSearchResult>> = _searchResults.asStateFlow()

    // Overlay state
    private val _overlay = MutableStateFlow<LauncherOverlay>(LauncherOverlay.None)
    val overlay: StateFlow<LauncherOverlay> = _overlay.asStateFlow()

    // Convenience mappings for components observing individual overlay visibility
    val isAppDrawerOpen: StateFlow<Boolean> = _overlay
        .map { it is LauncherOverlay.Drawer }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isSearchActive: StateFlow<Boolean> = _overlay
        .map { it is LauncherOverlay.Search }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isSettingsOpen: StateFlow<Boolean> = _overlay
        .map { it is LauncherOverlay.Settings }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isAppPickerOpen: StateFlow<Boolean> = _overlay
        .map { it is LauncherOverlay.AppPicker }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val appToCustomize: StateFlow<ScreenApp?> = _overlay
        .map { (it as? LauncherOverlay.AppOptions)?.app }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val appUsageTracker = AppUsageTracker(context)

    private val searchEngine = SearchEngine(
        appUsageTracker = appUsageTracker
    )

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) {
                    _batteryLevel.value = (level * 100 / scale)
                    updateTimeAndDate()
                }
            }
        }
    }

    private val timeTickReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            updateTimeAndDate()
        }
    }

    init {
        context.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val timeFilter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        context.registerReceiver(timeTickReceiver, timeFilter)
        updateTimeAndDate()

        // Refresh time and date whenever settings change (e.g. 24-hour clock or show battery)
        viewModelScope.launch {
            settings.collect {
                updateTimeAndDate()
            }
        }

        viewModelScope.launch {
            screens.collect { screenList ->
                if (screenList.isNotEmpty() && _currentScreenIndex.value >= screenList.size) {
                    _currentScreenIndex.value = screenList.size - 1
                }
            }
        }

        viewModelScope.launch {
            combine(installedApps, settings) { apps, currentSettings ->
                apps to currentSettings.customAliases
            }.collect { (apps, aliases) ->
                searchEngine.updateIndex(apps, aliases)
            }
        }
    }

    private fun updateTimeAndDate() {
        val now = Date()
        val is24Hour = settings.value.is24HourClock
        val timePattern = if (is24Hour) "H:mm" else "h:mm"
        _currentTime.value = SimpleDateFormat(timePattern, Locale.getDefault()).format(now)

        val bestPattern = android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEdMMM")
        val dateStr = SimpleDateFormat(bestPattern, Locale.getDefault()).format(now)

        val bat = _batteryLevel.value
        _currentDate.value = if (settings.value.showBattery && bat >= 0) {
            "$dateStr, $bat%"
        } else {
            dateStr
        }
    }

    fun onScreenSwiped(direction: Int) {
        val screenList = screens.value
        if (screenList.isEmpty()) return
        val current = _currentScreenIndex.value.coerceIn(0, (screenList.size - 1).coerceAtLeast(0))
        _currentScreenIndex.value = current

        if (direction > 0) { // Swipe Left -> Go to Next Screen
            if (current < screenList.size - 1) {
                _currentScreenIndex.value = current + 1
            }
        } else if (direction < 0) { // Swipe Right -> Go to Prev Screen
            if (current > 0) {
                _currentScreenIndex.value = current - 1
            }
        }
    }

    fun setScreenIndex(index: Int) {
        if (index in screens.value.indices) {
            _currentScreenIndex.value = index
        }
    }

    // Overlay controls
    fun openDrawer() {
        _overlay.value = LauncherOverlay.Drawer
    }

    fun closeDrawer() {
        if (_overlay.value is LauncherOverlay.Drawer) {
            _overlay.value = LauncherOverlay.None
        }
    }

    fun setAppDrawerOpen(open: Boolean) {
        if (open) openDrawer() else closeDrawer()
    }

    fun openSearch() {
        _overlay.value = LauncherOverlay.Search
    }

    fun closeSearch() {
        if (_overlay.value is LauncherOverlay.Search) {
            _overlay.value = LauncherOverlay.None
            _searchQuery.value = ""
            _searchResults.value = emptyList()
        }
    }

    fun setSearchActive(active: Boolean) {
        if (active) openSearch() else closeSearch()
    }

    fun openSettings() {
        _overlay.value = LauncherOverlay.Settings
    }

    fun closeSettings() {
        if (_overlay.value is LauncherOverlay.Settings) {
            _overlay.value = LauncherOverlay.None
        }
    }

    fun openAppPicker() {
        _overlay.value = LauncherOverlay.AppPicker
    }

    fun closeAppPicker() {
        if (_overlay.value is LauncherOverlay.AppPicker) {
            _overlay.value = LauncherOverlay.None
        }
    }

    fun showAppOptions(app: ScreenApp) {
        _overlay.value = LauncherOverlay.AppOptions(app)
    }

    fun dismissAppOptions() {
        if (_overlay.value is LauncherOverlay.AppOptions) {
            _overlay.value = LauncherOverlay.None
        }
    }

    fun setAppToCustomize(app: ScreenApp?) {
        if (app != null) showAppOptions(app) else dismissAppOptions()
    }

    fun dismissOverlay() {
        if (_overlay.value is LauncherOverlay.Search) {
            _searchQuery.value = ""
            _searchResults.value = emptyList()
        }
        _overlay.value = LauncherOverlay.None
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        _searchResults.value = searchEngine.search(query)
    }

    fun launchSearchResult(result: AppSearchResult) {
        dismissOverlay()
        appUsageTracker.recordAppLaunch(result.app.packageName)
        appRepository.launchApp(result.app.packageName, result.app.activityName, result.app.userHandle)
    }

    fun executeGesture(action: GestureAction) {
        when (action) {
            is GestureAction.OpenSearch -> openSearch()
            is GestureAction.OpenNotifications -> SystemActionsManager.expandStatusBar(context)
            is GestureAction.OpenQuickSettings -> SystemActionsManager.expandQuickSettings(context)
            is GestureAction.OpenCamera -> SystemActionsManager.openCamera(context)
            is GestureAction.OpenDialer -> SystemActionsManager.openDialer(context)
            is GestureAction.LaunchApp -> appRepository.launchApp(action.packageName, null)
            is GestureAction.None -> Unit
        }
    }

    fun openClock() {
        SystemActionsManager.openClock(context)
    }

    fun openCalendar() {
        SystemActionsManager.openCalendar(context)
    }

    fun launchApp(packageName: String, activityName: String? = null, userHandle: UserHandle? = null) {
        appRepository.launchApp(packageName, activityName, userHandle)
    }

    fun openAppInfo(packageName: String, activityName: String? = null, userHandle: UserHandle? = null) {
        appRepository.openAppInfo(packageName, activityName, userHandle)
    }

    fun getShortcutsForApp(packageName: String): List<android.content.pm.ShortcutInfo> {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? android.content.pm.LauncherApps ?: return emptyList()
        val userManager = context.getSystemService(Context.USER_SERVICE) as? android.os.UserManager ?: return emptyList()
        val query = android.content.pm.LauncherApps.ShortcutQuery().apply {
            setPackage(packageName)
            setQueryFlags(
                android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
            )
        }
        return try {
            userManager.userProfiles.flatMap { profile ->
                launcherApps.getShortcuts(query, profile) ?: emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun launchShortcut(packageName: String, shortcutId: String, userHandle: UserHandle? = null) {
        val handle = userHandle ?: Process.myUserHandle()
        SystemActionsManager.launchShortcut(context, packageName, shortcutId, handle)
    }

    fun resetToHomeScreen() {
        dismissOverlay()
        _currentScreenIndex.value = 0
    }

    // --- Screen repository actions (ViewModel façade) ---
    fun addScreen(name: String, contextTag: String? = null) = screenRepository.addScreen(name, contextTag)
    fun renameScreen(screenId: String, newName: String, newContextTag: String? = null) = screenRepository.renameScreen(screenId, newName, newContextTag)
    fun moveScreen(fromIndex: Int, toIndex: Int) = screenRepository.moveScreen(fromIndex, toIndex)
    fun removeScreen(screenId: String) = screenRepository.removeScreen(screenId)
    fun addAppToScreen(screenId: String, appItem: AppItem, customLabel: String? = null) = screenRepository.addAppToScreen(screenId, appItem, customLabel)
    fun removeAppFromScreen(screenId: String, uniqueKey: String) = screenRepository.removeAppFromScreen(screenId, uniqueKey)
    fun updateAppLabelAcrossScreens(uniqueKey: String, newLabel: String) = screenRepository.updateAppLabelAcrossScreens(uniqueKey, newLabel)
    fun updateAppLabel(screenId: String, uniqueKey: String, newLabel: String) = screenRepository.updateAppLabel(screenId, uniqueKey, newLabel)
    fun reorderApps(screenId: String, reordered: List<ScreenApp>) = screenRepository.reorderApps(screenId, reordered)

    // --- Settings repository actions (ViewModel façade) ---
    fun updateThemeMode(theme: ThemeMode) = settingsRepository.updateThemeMode(theme)
    fun updateWallpaperScrim(scrim: Boolean) = settingsRepository.updateWallpaperScrim(scrim)
    fun updateHideStatusBar(hide: Boolean) = settingsRepository.updateHideStatusBar(hide)
    fun updateClockPosition(vertical: ClockVerticalPosition, horizontal: ClockHorizontalAlignment) = settingsRepository.updateClockPosition(vertical, horizontal)
    fun updateAppsPosition(vertical: AppsVerticalPosition, horizontal: AppsHorizontalAlignment) = settingsRepository.updateAppsPosition(vertical, horizontal)
    fun update24HourClock(is24Hour: Boolean) = settingsRepository.update24HourClock(is24Hour)
    fun updateShowDate(show: Boolean) = settingsRepository.updateShowDate(show)
    fun updateShowBattery(show: Boolean) = settingsRepository.updateShowBattery(show)
    fun updateGestureActions(
        swipeLeft: GestureAction? = null,
        swipeRight: GestureAction? = null,
        swipeUp: GestureAction? = null,
        swipeDown: GestureAction? = null
    ) = settingsRepository.updateGestureActions(swipeLeft, swipeRight, swipeUp, swipeDown)
    fun setAlias(alias: String, target: String) = settingsRepository.setAlias(alias, target)
    fun removeAlias(alias: String) = settingsRepository.removeAlias(alias)
    fun hideApp(packageName: String) = settingsRepository.hideApp(packageName)
    fun unhideApp(packageName: String) = settingsRepository.unhideApp(packageName)
    fun updateFontScale(scale: Float) = settingsRepository.updateFontScale(scale)
    fun updateEdgeScrubPosition(position: EdgeScrubPosition) = settingsRepository.updateEdgeScrubPosition(position)
    fun updateEdgeScrubReleaseAction(action: EdgeScrubReleaseAction) = settingsRepository.updateEdgeScrubReleaseAction(action)

    override fun onCleared() {
        try {
            context.unregisterReceiver(batteryReceiver)
            context.unregisterReceiver(timeTickReceiver)
        } catch (_: Exception) {}
        appRepository.unregister()
        super.onCleared()
    }
}
