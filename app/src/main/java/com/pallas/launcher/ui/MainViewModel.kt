package com.pallas.launcher.ui

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pallas.launcher.data.repository.AppRepository
import com.pallas.launcher.data.repository.AppUsageTracker
import com.pallas.launcher.data.repository.ScreenRepository
import com.pallas.launcher.data.repository.SettingsRepository
import com.pallas.launcher.domain.model.AppItem
import com.pallas.launcher.domain.model.GestureAction
import com.pallas.launcher.domain.model.HomeScreen
import com.pallas.launcher.domain.model.LauncherSettings
import com.pallas.launcher.domain.model.ScreenApp
import com.pallas.launcher.domain.search.AppSearchResult
import com.pallas.launcher.domain.search.SearchEngine
import com.pallas.launcher.platform.BatteryManagerHelper
import com.pallas.launcher.platform.SystemActionsManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    val settingsRepository = SettingsRepository(context)
    val appRepository = AppRepository(context, viewModelScope, settingsRepository)
    val screenRepository = ScreenRepository(context)

    val installedApps: StateFlow<List<AppItem>> = appRepository.installedApps
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

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    // Screen editing & settings dialogs
    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _isScreenEditorOpen = MutableStateFlow(false)
    val isScreenEditorOpen: StateFlow<Boolean> = _isScreenEditorOpen.asStateFlow()

    private val _isAppPickerOpen = MutableStateFlow(false)
    val isAppPickerOpen: StateFlow<Boolean> = _isAppPickerOpen.asStateFlow()

    private val _appToCustomize = MutableStateFlow<ScreenApp?>(null)
    val appToCustomize: StateFlow<ScreenApp?> = _appToCustomize.asStateFlow()

    private val _isAppDrawerOpen = MutableStateFlow(false)
    val isAppDrawerOpen: StateFlow<Boolean> = _isAppDrawerOpen.asStateFlow()

    fun setAppDrawerOpen(open: Boolean) {
        _isAppDrawerOpen.value = open
    }

    val appUsageTracker = AppUsageTracker(context)

    private val searchEngine = SearchEngine(
        appUsageTracker = appUsageTracker
    )

    private var timeUpdateJob: Job? = null

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

        viewModelScope.launch {
            installedApps.collect { apps ->
                if (apps.isNotEmpty()) {
                    screenRepository.initDefaultScreenWithApps(apps)
                }
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

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) {
            _searchQuery.value = ""
            _searchResults.value = emptyList()
        }
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
        setSearchActive(false)
        appUsageTracker.recordAppLaunch(result.app.packageName)
        appRepository.launchApp(result.app.packageName, result.app.activityName, result.app.userHandle)
    }

    fun executeGesture(action: GestureAction) {
        when (action) {
            is GestureAction.OpenSearch -> setSearchActive(true)
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

    fun openSettings() {
        _isSettingsOpen.value = true
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
    }

    fun openScreenEditor() {
        _isScreenEditorOpen.value = true
    }

    fun closeScreenEditor() {
        _isScreenEditorOpen.value = false
    }

    fun openAppPicker() {
        _isAppPickerOpen.value = true
    }

    fun closeAppPicker() {
        _isAppPickerOpen.value = false
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

    fun launchShortcut(packageName: String, shortcutId: String, userHandle: android.os.UserHandle? = null) {
        val handle = userHandle ?: android.os.Process.myUserHandle()
        SystemActionsManager.launchShortcut(context, packageName, shortcutId, handle)
    }

    fun setAppToCustomize(app: ScreenApp?) {
        _appToCustomize.value = app
    }

    fun resetToHomeScreen() {
        _isSearchActive.value = false
        _isSettingsOpen.value = false
        _isScreenEditorOpen.value = false
        _isAppPickerOpen.value = false
        _isAppDrawerOpen.value = false
        _appToCustomize.value = null
        _currentScreenIndex.value = 0
    }

    override fun onCleared() {
        try {
            context.unregisterReceiver(batteryReceiver)
            context.unregisterReceiver(timeTickReceiver)
        } catch (_: Exception) {}
        appRepository.unregister()
        timeUpdateJob?.cancel()
        super.onCleared()
    }
}

