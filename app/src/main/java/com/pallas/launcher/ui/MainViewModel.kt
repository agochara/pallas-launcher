package com.pallas.launcher.ui

import android.app.Application
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Process
import android.os.SystemClock
import android.os.UserHandle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pallas.launcher.data.repository.AppRepository
import com.pallas.launcher.data.repository.AppUsageTracker
import com.pallas.launcher.data.repository.FocusModeManager
import com.pallas.launcher.data.repository.FocusState
import com.pallas.launcher.data.repository.MacroScheduler
import com.pallas.launcher.data.repository.QuickCaptureRepository
import com.pallas.launcher.data.repository.ScreenRepository
import com.pallas.launcher.data.repository.SettingsRepository
import com.pallas.launcher.domain.model.AppItem
import com.pallas.launcher.domain.model.FocusProfile
import com.pallas.launcher.domain.model.GestureAction
import com.pallas.launcher.domain.model.HomeScreen
import com.pallas.launcher.domain.model.LauncherSettings
import com.pallas.launcher.domain.model.MacroRoutine
import com.pallas.launcher.domain.model.NameLengthSortOrder
import com.pallas.launcher.domain.model.ScreenApp
import com.pallas.launcher.domain.model.WolframRequestTrigger
import com.pallas.launcher.domain.model.WolframSubmitMode
import com.pallas.launcher.domain.search.AsyncCommandRequest
import com.pallas.launcher.domain.search.CALENDAR_GRANT_PERMISSION_ID
import com.pallas.launcher.domain.search.FILES_GRANT_PERMISSION_ID
import com.pallas.launcher.domain.search.FOCUS_OFF_COMMAND_ID
import com.pallas.launcher.domain.search.FOCUS_ON_COMMAND_ID
import com.pallas.launcher.domain.search.FOCUS_SETUP_COMMAND_ID
import com.pallas.launcher.domain.search.LauncherSearchResult
import com.pallas.launcher.domain.search.MACRO_COMMAND_PREFIX
import com.pallas.launcher.domain.search.MathStatsEngine
import com.pallas.launcher.domain.search.PALLAS_NAV_PREFIX
import com.pallas.launcher.domain.search.PallasTarget
import com.pallas.launcher.domain.search.QUICK_CAPTURE_INBOX_ID
import com.pallas.launcher.domain.search.SETTINGS_COMMAND_ID
import com.pallas.launcher.domain.search.SearchEngine
import com.pallas.launcher.domain.search.SmartCommandEngine
import com.pallas.launcher.domain.search.SmartCommandType
import com.pallas.launcher.domain.search.WebSearchConfig
import com.pallas.launcher.domain.search.isInertOnTap
import com.pallas.launcher.platform.BatteryManagerHelper
import com.pallas.launcher.platform.BrowserHelper
import com.pallas.launcher.platform.CalendarAgendaService
import com.pallas.launcher.platform.CurrencyRateService
import com.pallas.launcher.platform.LiveCommandService
import com.pallas.launcher.platform.LocalFileSearchService
import com.pallas.launcher.platform.MacroExecutor
import com.pallas.launcher.platform.SystemActionsManager
import com.pallas.launcher.ui.apps.AppIconHelper
import com.pallas.launcher.ui.apps.AppIconShape
import com.pallas.launcher.ui.settings.SettingsSubpage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.pallas.launcher.domain.model.SearchContact
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface LauncherOverlay {
    data object None : LauncherOverlay
    data object Drawer : LauncherOverlay
    data object Search : LauncherOverlay
    data class Settings(
        val initialSubpage: SettingsSubpage = SettingsSubpage.MAIN,
        val guideTab: Int = 0,
        val guideTopicId: String? = null,
        val guideFormulaTitle: String? = null,
        val launchId: Long = System.nanoTime()
    ) : LauncherOverlay
    data class LatexStudio(val initialContent: String? = null, val launchId: Long = System.nanoTime()) : LauncherOverlay
    data class Notebook(val initialExpression: String? = null, val launchId: Long = System.nanoTime()) : LauncherOverlay
    data object WorldAtlas : LauncherOverlay
    data object QuickCaptureInbox : LauncherOverlay
    data object AppPicker : LauncherOverlay
    data class AppOptions(val app: ScreenApp, val returnToDrawer: Boolean = false) : LauncherOverlay
    data class SetAlias(val app: ScreenApp, val returnToDrawer: Boolean = false) : LauncherOverlay
    data class RenameApp(val app: ScreenApp, val returnToDrawer: Boolean = false) : LauncherOverlay
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    val appUsageTracker = AppUsageTracker.getInstance(context)
    private val settingsRepository = SettingsRepository.getInstance(context)
    val appRepository = AppRepository.getInstance(context)
    private val screenRepository = ScreenRepository.getInstance(context)
    private val quickCaptureRepository = QuickCaptureRepository.getInstance(context)
    private val focusModeManager = FocusModeManager.getInstance(context)
    private val macroScheduler = MacroScheduler(context)

    private val uiPrefs = context.getSharedPreferences("pallas_ui_state", Context.MODE_PRIVATE)

    val screens: StateFlow<List<HomeScreen>> = screenRepository.screens
    val customAppLabels: StateFlow<Map<String, String>> = screenRepository.customAppLabels
    val settings: StateFlow<LauncherSettings> = settingsRepository.settings
    val focusState: StateFlow<FocusState> = focusModeManager.state

    val activeFocusProfile: StateFlow<FocusProfile?> =
        combine(focusState, settings) { fs, s ->
            if (!fs.isActive) null
            else s.focusProfiles.firstOrNull { it.id == fs.activeProfileId }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val focusAllowedAppKeys: StateFlow<Set<String>?> =
        combine(activeFocusProfile, screenRepository.screens) { profile, screenList ->
            if (profile == null) null
            else {
                val screenKeys = screenList
                    .filter { it.id in profile.allowedScreenIds }
                    .flatMap { it.apps }
                    .map { it.uniqueKey }
                (screenKeys + profile.allowedAppKeys).toSet()
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val allInstalledApps: StateFlow<List<AppItem>> =
        combine(appRepository.installedApps, screenRepository.customAppLabels, screenRepository.screens) { apps, globalCustom, screenList ->
            val screenCustom = screenList.flatMap { it.apps }
                .filter { !it.customLabel.isNullOrBlank() }
                .associate { it.uniqueKey to it.customLabel!! }
            val mergedCustom = screenCustom + globalCustom
            if (mergedCustom.isEmpty()) {
                apps
            } else {
                apps.map { app ->
                    val custom = mergedCustom[app.uniqueKey]
                    if (!custom.isNullOrBlank()) app.copy(label = custom) else app
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, appRepository.installedApps.value)

    val installedApps: StateFlow<List<AppItem>> =
        combine(allInstalledApps, focusAllowedAppKeys) { apps, allowedKeys ->
            if (allowedKeys == null) apps
            else apps.filter { it.uniqueKey in allowedKeys || it.packageName in allowedKeys }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, appRepository.installedApps.value)

    val isAppsLoaded: StateFlow<Boolean> = appRepository.isLoaded

    val favoriteApps: StateFlow<List<AppItem>> =
        combine(installedApps, appUsageTracker.recentAppKeys, settings) { apps, recents, s ->
            val byKey = apps.associateBy { it.uniqueKey }
            val byPackage = apps.groupBy { it.packageName }
            recents.asSequence()
                .filterNot { key ->
                    key in s.hiddenFromAppList || key.substringBefore('/') in s.hiddenFromAppList
                }
                .mapNotNull { key -> byKey[key] ?: byPackage[key]?.firstOrNull() }
                .take(8)
                .toList()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val appUsageScores: StateFlow<Map<String, Double>> = appUsageTracker.usageScoresSnapshot

    val displayScreens: StateFlow<List<HomeScreen>> =
        combine(
            screenRepository.screens,
            settingsRepository.settings.map { it.nameLengthSortOrder }.distinctUntilChanged(),
            activeFocusProfile,
            allInstalledApps
        ) { list, order, profile, allApps ->
            val focusFiltered = if (profile == null) {
                list
            } else {
                val selectedScreens = list.filter { it.id in profile.allowedScreenIds }
                if (selectedScreens.isNotEmpty()) {
                    selectedScreens
                } else {
                    val byKey = allApps.associateBy { it.uniqueKey }
                    val extraApps = profile.allowedAppKeys.mapNotNull { key ->
                        byKey[key]?.let { app ->
                            ScreenApp(
                                uniqueKey = app.uniqueKey,
                                packageName = app.packageName,
                                activityName = app.activityName,
                                customLabel = null,
                                defaultLabel = app.label,
                                userSerialNumber = app.userSerialNumber,
                                userHandle = app.userHandle
                            )
                        }
                    }
                    listOf(HomeScreen(id = "focus_virtual", name = profile.name, apps = extraApps))
                }
            }
            if (order == NameLengthSortOrder.OFF) focusFiltered
            else focusFiltered.map { it.copy(apps = screenRepository.sortAppsByNameLength(it.apps, order)) }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, screenRepository.screens.value)

    private fun savedScreenIndex(): Int {
        val id = uiPrefs.getString(KEY_CURRENT_SCREEN_ID, null) ?: return 0
        return displayScreens.value.indexOfFirst { it.id == id }.coerceAtLeast(0)
    }

    private fun persistCurrentScreen() {
        val id = displayScreens.value.getOrNull(_currentScreenIndex.value)?.id ?: return
        if (id == "focus_virtual") return
        uiPrefs.edit().putString(KEY_CURRENT_SCREEN_ID, id).apply()
    }

    private val _currentScreenIndex = MutableStateFlow(savedScreenIndex())
    val currentScreenIndex: StateFlow<Int> = combine(_currentScreenIndex, displayScreens) { index, screenList ->
        if (screenList.isEmpty()) 0
        else index.coerceIn(0, screenList.size - 1)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, _currentScreenIndex.value)

    private val _screenSwipeDirection = MutableStateFlow(1)
    val screenSwipeDirection: StateFlow<Int> = _screenSwipeDirection.asStateFlow()

    private fun formatCurrentTime(is24Hour: Boolean): String {
        val pattern = if (is24Hour) "H:mm" else "h:mm"
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date())
    }

    private fun formatCurrentDate(showBattery: Boolean, bat: Int): String {
        val now = Date()
        val bestPattern = android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEdMMM")
        val dateStr = SimpleDateFormat(bestPattern, Locale.getDefault()).format(now)
        return if (showBattery && bat >= 0) {
            "$dateStr, $bat%"
        } else {
            dateStr
        }
    }

    private val _batteryLevel = MutableStateFlow(BatteryManagerHelper.getBatteryPercentage(context))
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val _currentTime = MutableStateFlow(formatCurrentTime(settingsRepository.settings.value.is24HourClock))
    val currentTime: StateFlow<String> = _currentTime.asStateFlow()

    private val _currentDate = MutableStateFlow(formatCurrentDate(settingsRepository.settings.value.showBattery, _batteryLevel.value))
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<LauncherSearchResult>>(emptyList())
    val searchResults: StateFlow<List<LauncherSearchResult>> = _searchResults.asStateFlow()
    private val deviceContacts = MutableStateFlow<List<SearchContact>>(emptyList())

    private var liveCommandJob: Job? = null

    private val _overlay = MutableStateFlow<LauncherOverlay>(LauncherOverlay.None)
    val overlay: StateFlow<LauncherOverlay> = _overlay.asStateFlow()

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

    private val searchEngine = SearchEngine(
        appUsageTracker = appUsageTracker
    )

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) {
                    val pct = level * 100 / scale
                    if (pct != _batteryLevel.value) {
                        _batteryLevel.value = pct
                    }
                }
            }
        }
    }

    private fun syncFocusAndRunEnterMacro() {
        val scheduledActivated = focusModeManager.syncState(settings.value)
        if (scheduledActivated != null) {
            runOnEnterMacro(scheduledActivated)
        }
    }

    private fun runOnEnterMacro(profile: FocusProfile) {
        val macroId = profile.onEnterMacroId ?: return
        settings.value.macros.firstOrNull { it.id == macroId }?.let { executeMacro(it) }
    }

    private val timeTickReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            updateTimeAndDate()
            syncFocusAndRunEnterMacro()
            runDueScheduledMacros()
        }
    }

    init {
        CurrencyRateService.init(context)
        syncFocusAndRunEnterMacro()
        runDueScheduledMacros()
        ContextCompat.registerReceiver(
            context,
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        val timeFilter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(
            context,
            timeTickReceiver,
            timeFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        updateTimeAndDate()

        viewModelScope.launch {
            settings.collect {
                updateTimeAndDate()
                syncFocusAndRunEnterMacro()
            }
        }

        viewModelScope.launch {
            displayScreens.collect { screenList ->
                if (screenList.isEmpty()) return@collect
                val savedId = uiPrefs.getString(KEY_CURRENT_SCREEN_ID, null)
                val byId = savedId?.let { id -> screenList.indexOfFirst { it.id == id } } ?: -1
                _currentScreenIndex.value = if (byId >= 0) byId else _currentScreenIndex.value.coerceIn(0, screenList.size - 1)
                if (activeFocusProfile.value == null) {
                    persistCurrentScreen()
                }
            }
        }

        viewModelScope.launch {
            combine(
                appRepository.installedApps,
                settings.map { it.customAliases }.distinctUntilChanged(),
                screens,
                customAppLabels,
                focusAllowedAppKeys
            ) { apps, aliases, screenList, globalCustom, allowedKeys ->
                val filteredApps = if (allowedKeys == null) apps
                else apps.filter { it.uniqueKey in allowedKeys || it.packageName in allowedKeys }
                val screenCustom = screenList.flatMap { it.apps }
                    .filter { !it.customLabel.isNullOrBlank() }
                    .associate { it.uniqueKey to it.customLabel!! }
                Triple(filteredApps, aliases, screenCustom + globalCustom)
            }.distinctUntilChanged().collect { (apps, aliases, custom) -> searchEngine.updateIndex(apps, aliases, custom) }
        }

        viewModelScope.launch {
            allInstalledApps.collect { apps ->
                if (apps.isNotEmpty()) {
                    if (!startupMigrationsDone) {
                        startupMigrationsDone = true
                        appUsageTracker.migratePackageKeysToUniqueKeys(apps)
                        settingsRepository.migrateHiddenApps(apps)
                    }
                    prewarmIcons(apps)
                }
            }
        }
    }

    private var startupMigrationsDone = false
    private var lastContactsRefreshElapsedMs = 0L
    private var iconsPrewarmed = false
    private fun prewarmIcons(apps: List<AppItem>) {
        if (iconsPrewarmed) return
        iconsPrewarmed = true
        viewModelScope.launch(Dispatchers.Default) {
            val homeKeys = screenRepository.screens.value.flatMap { it.apps }.map { it.uniqueKey }.toSet()
            val recentKeys = appUsageTracker.recentAppKeys.value.take(12).toSet()
            val priorityKeys = homeKeys + recentKeys
            val toWarm = apps.filter { it.uniqueKey in priorityKeys }.take(24)
            for (app in toWarm) {
                AppIconHelper.getAppIcon(
                    context = context,
                    packageName = app.packageName,
                    activityName = app.activityName,
                    userHandle = app.userHandle,
                    userSerialNumber = app.userSerialNumber,
                    uniqueKey = app.uniqueKey,
                    iconShape = AppIconShape.CIRCLE
                )
            }
        }
    }

    private fun updateTimeAndDate() {
        _currentTime.value = formatCurrentTime(settings.value.is24HourClock)
        _currentDate.value = formatCurrentDate(settings.value.showBattery, _batteryLevel.value)
    }

    fun onScreenSwiped(direction: Int) {
        val screenList = displayScreens.value
        if (screenList.size <= 1) return
        val current = _currentScreenIndex.value.coerceIn(0, screenList.size - 1)

        val nextIndex = if (direction > 0) {
            (current + 1) % screenList.size
        } else if (direction < 0) {
            (current - 1 + screenList.size) % screenList.size
        } else {
            current
        }
        _screenSwipeDirection.value = if (direction >= 0) 1 else -1
        _currentScreenIndex.value = nextIndex
        persistCurrentScreen()
    }

    fun setScreenIndex(index: Int) {
        if (index in displayScreens.value.indices) {
            _screenSwipeDirection.value = if (index >= _currentScreenIndex.value) 1 else -1
            _currentScreenIndex.value = index
            persistCurrentScreen()
        }
    }

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
        if (_overlay.value !is LauncherOverlay.Search) {
            liveCommandJob?.cancel()
            liveCommandJob = null
            _searchQuery.value = ""
            _searchResults.value = emptyList()
        }
        _overlay.value = LauncherOverlay.Search
        syncFocusAndRunEnterMacro()
    }

    fun closeSearch() {
        if (_overlay.value is LauncherOverlay.Search) {
            liveCommandJob?.cancel()
            liveCommandJob = null
            _overlay.value = LauncherOverlay.None
            _searchQuery.value = ""
            _searchResults.value = emptyList()
        }
    }

    fun setSearchActive(active: Boolean) {
        if (active) openSearch() else closeSearch()
    }

    fun openSettings(
        initialSubpage: SettingsSubpage = SettingsSubpage.MAIN,
        guideTab: Int = 0,
        guideTopicId: String? = null,
        guideFormulaTitle: String? = null
    ) {
        _overlay.value = LauncherOverlay.Settings(
            initialSubpage = initialSubpage,
            guideTab = guideTab,
            guideTopicId = guideTopicId,
            guideFormulaTitle = guideFormulaTitle
        )
    }

    fun closeSettings() {
        if (_overlay.value is LauncherOverlay.Settings) {
            _currentScreenIndex.value = savedScreenIndex()
            _overlay.value = LauncherOverlay.None
        }
    }

    fun openLatexStudio(initialContent: String? = null) {
        liveCommandJob?.cancel()
        liveCommandJob = null
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _overlay.value = LauncherOverlay.LatexStudio(initialContent = initialContent)
    }

    fun closeLatexStudio() {
        if (_overlay.value is LauncherOverlay.LatexStudio) {
            _overlay.value = LauncherOverlay.None
        }
    }

    fun openNotebook(initialExpression: String? = null) {
        liveCommandJob?.cancel()
        liveCommandJob = null
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _overlay.value = LauncherOverlay.Notebook(initialExpression = initialExpression)
    }

    fun closeNotebook() {
        if (_overlay.value is LauncherOverlay.Notebook) {
            _overlay.value = LauncherOverlay.None
        }
    }

    fun openAtlas() {
        liveCommandJob?.cancel()
        liveCommandJob = null
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _overlay.value = LauncherOverlay.WorldAtlas
    }

    fun closeAtlas() {
        if (_overlay.value is LauncherOverlay.WorldAtlas) {
            _overlay.value = LauncherOverlay.None
        }
    }

    fun openQuickCaptureInbox() {
        liveCommandJob?.cancel()
        liveCommandJob = null
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _overlay.value = LauncherOverlay.QuickCaptureInbox
    }

    fun closeQuickCaptureInbox() {
        if (_overlay.value is LauncherOverlay.QuickCaptureInbox) {
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

    fun showAppOptions(app: ScreenApp, returnToDrawer: Boolean = _overlay.value is LauncherOverlay.Drawer) {
        _overlay.value = LauncherOverlay.AppOptions(app, returnToDrawer = returnToDrawer)
    }

    fun openSetAlias(app: ScreenApp) {
        val fromDrawer = (_overlay.value as? LauncherOverlay.AppOptions)?.returnToDrawer
            ?: (_overlay.value is LauncherOverlay.Drawer)
        _overlay.value = LauncherOverlay.SetAlias(app, returnToDrawer = fromDrawer)
    }

    fun openRenameApp(app: ScreenApp) {
        val fromDrawer = (_overlay.value as? LauncherOverlay.AppOptions)?.returnToDrawer
            ?: (_overlay.value is LauncherOverlay.Drawer)
        _overlay.value = LauncherOverlay.RenameApp(app, returnToDrawer = fromDrawer)
    }

    fun dismissAppOptions(forceCloseAll: Boolean = false) {
        val current = _overlay.value
        if (current is LauncherOverlay.AppOptions) {
            _overlay.value = if (!forceCloseAll && current.returnToDrawer) LauncherOverlay.Drawer else LauncherOverlay.None
        }
    }

    fun setAppToCustomize(app: ScreenApp?) {
        if (app != null) showAppOptions(app) else dismissAppOptions()
    }

    fun dismissOverlay() {
        when (val current = _overlay.value) {
            is LauncherOverlay.Search -> {
                liveCommandJob?.cancel()
                liveCommandJob = null
                _searchQuery.value = ""
                _searchResults.value = emptyList()
                _overlay.value = LauncherOverlay.None
            }
            is LauncherOverlay.AppOptions -> {
                _overlay.value = if (current.returnToDrawer) LauncherOverlay.Drawer else LauncherOverlay.None
            }
            is LauncherOverlay.SetAlias -> {
                _overlay.value = if (current.returnToDrawer) LauncherOverlay.Drawer else LauncherOverlay.None
            }
            is LauncherOverlay.RenameApp -> {
                _overlay.value = if (current.returnToDrawer) LauncherOverlay.Drawer else LauncherOverlay.None
            }
            is LauncherOverlay.Settings -> {
                _currentScreenIndex.value = savedScreenIndex()
                _overlay.value = LauncherOverlay.None
            }
            else -> {
                _overlay.value = LauncherOverlay.None
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        if (_overlay.value !is LauncherOverlay.Search) return
        _searchQuery.value = query
        liveCommandJob?.cancel()
        liveCommandJob = null
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        val s = settings.value
        val capturedNotes = quickCaptureRepository.notes.value
        val fs = focusState.value
        val activeProfile = activeFocusProfile.value
        val webConfig = webSearchConfig(s)
        val (rawImmediate, asyncRequest) = searchEngine.searchAll(
            rawQuery = query,
            contacts = s.searchContacts,
            webSearch = webConfig,
            configs = s.commandConfigs,
            defaultWeatherCity = s.defaultWeatherCity,
            deviceContacts = deviceContacts.value,
            is24HourClock = s.is24HourClock,
            dateArithmeticEnabled = s.dateArithmeticEnabled,
            colorBaseInspectorEnabled = s.colorBaseInspectorEnabled,
            quickCaptureCount = capturedNotes.size,
            quickCapturePreview = capturedNotes.firstOrNull()?.text.orEmpty(),
            focusProfiles = s.focusProfiles,
            defaultFocusProfileId = s.defaultFocusProfileId,
            isFocusActive = fs.isActive,
            activeFocusProfile = activeProfile,
            focusAllowedAppKeys = focusAllowedAppKeys.value,
            macros = s.macros,
            isDndEnabled = SystemActionsManager.isDndEnabled(context),
            hasCalendarPermission = CalendarAgendaService.hasCalendarPermission(context),
            hasFileSearchPermission = LocalFileSearchService.hasFileSearchAccess(context)
        )
        val browserPkg = webConfig.browserPackage
        val waDelaySec = s.wolframIdleDelaySeconds.coerceIn(1, 5)
        val wikiDefDelaySec = s.wikiDefIdleDelaySeconds.coerceIn(1, 5)
        val stockWxDelaySec = s.stockWeatherIdleDelaySeconds.coerceIn(1, 5)

        val immediate = rawImmediate.map { item ->
            if (item !is LauncherSearchResult.RichCard) return@map item
            when {
                item.type == SmartCommandType.WOLFRAM && asyncRequest is AsyncCommandRequest.Wolfram -> {
                    val waArg = asyncRequest.query
                    when {
                        s.wolframSubmitMode == WolframSubmitMode.WEBSITE -> item.copy(
                            id = "wa_web_$waArg",
                            isLoading = false,
                            primaryValue = "Wolfram|Alpha",
                            subtitle = "",
                            execute = { ctx ->
                                SystemActionsManager.openUrl(
                                    ctx,
                                    "https://www.wolframalpha.com/input?i=${android.net.Uri.encode(waArg)}",
                                    browserPkg
                                )
                            }
                        )
                        s.wolframRequestTrigger == WolframRequestTrigger.ENTER -> item.copy(
                            id = "wa_pending_$waArg",
                            isLoading = false,
                            primaryValue = "Wolfram|Alpha",
                            subtitle = "",
                            execute = {}
                        )
                        else -> item.copy(
                            id = "wa_waiting_$waArg",
                            isLoading = true,
                            primaryValue = "",
                            subtitle = "querying Wolfram|Alpha in ${waDelaySec}s…"
                        )
                    }
                }
                item.type == SmartCommandType.WIKI && asyncRequest is AsyncCommandRequest.Wiki -> {
                    val wikiArg = asyncRequest.query
                    if (s.wikiDefRequestTrigger == WolframRequestTrigger.ENTER) {
                        item.copy(
                            id = "wiki_pending_$wikiArg",
                            isLoading = false,
                            primaryValue = "Wikipedia",
                            subtitle = "",
                            badgeText = "Wikipedia",
                            isBadgePositive = true,
                            execute = {}
                        )
                    } else {
                        item.copy(
                            id = "wiki_waiting_$wikiArg",
                            isLoading = true,
                            primaryValue = "",
                            subtitle = "fetching Wikipedia in ${wikiDefDelaySec}s…"
                        )
                    }
                }
                item.type == SmartCommandType.DEFINE && asyncRequest is AsyncCommandRequest.Define -> {
                    val defWord = asyncRequest.word
                    if (s.wikiDefRequestTrigger == WolframRequestTrigger.ENTER) {
                        item.copy(
                            id = "def_pending_$defWord",
                            isLoading = false,
                            primaryValue = "Dictionary",
                            subtitle = "",
                            badgeText = "Define",
                            isBadgePositive = true,
                            execute = {}
                        )
                    } else {
                        item.copy(
                            id = "def_waiting_$defWord",
                            isLoading = true,
                            primaryValue = "",
                            subtitle = "looking up definition in ${wikiDefDelaySec}s…"
                        )
                    }
                }
                item.type == SmartCommandType.FINANCE && asyncRequest is AsyncCommandRequest.Finance -> {
                    val finArg = asyncRequest.query
                    if (s.stockWeatherRequestTrigger == WolframRequestTrigger.ENTER) {
                        item.copy(
                            id = "fin_pending_$finArg",
                            isLoading = false,
                            primaryValue = "Markets",
                            subtitle = "Press Enter or tap to fetch ${finArg.uppercase()}",
                            badgeText = "Stock",
                            isBadgePositive = true,
                            execute = {}
                        )
                    } else {
                        item.copy(
                            id = "fin_waiting_$finArg",
                            isLoading = true,
                            primaryValue = "",
                            subtitle = "fetching market quote in ${stockWxDelaySec}s…"
                        )
                    }
                }
                (item.type == SmartCommandType.WEATHER || item.type == SmartCommandType.SOLAR) &&
                    asyncRequest is AsyncCommandRequest.Weather -> {
                    val cityArg = asyncRequest.city
                    val isSolar = asyncRequest.isSolarOnly
                    val keyPrefix = if (isSolar) "solar" else "wx"
                    if (s.stockWeatherRequestTrigger == WolframRequestTrigger.ENTER) {
                        item.copy(
                            id = "${keyPrefix}_pending_$cityArg",
                            isLoading = false,
                            primaryValue = if (isSolar) "Sunrise & Sunset" else "Weather",
                            subtitle = "Press Enter or tap for $cityArg",
                            badgeText = if (isSolar) "Solar" else "Weather",
                            isBadgePositive = true,
                            execute = {}
                        )
                    } else {
                        item.copy(
                            id = "${keyPrefix}_waiting_$cityArg",
                            isLoading = true,
                            primaryValue = "",
                            subtitle = if (isSolar) "calculating solar times in ${stockWxDelaySec}s…" else "fetching weather in ${stockWxDelaySec}s…"
                        )
                    }
                }
                else -> item
            }
        }
        _searchResults.value = immediate

        if (immediate.any { it is LauncherSearchResult.RichCard && it.type == SmartCommandType.CONVERT } &&
            CurrencyRateService.isStale()
        ) {
            CurrencyRateService.refreshInBackground {
                if (_searchQuery.value == query && _overlay.value is LauncherOverlay.Search) {
                    onSearchQueryChanged(query)
                }
            }
        }

        if (s.autoLaunchSingleMatch && asyncRequest == null && query.trim().length >= 2) {
            val only = immediate.singleOrNull()
            if (only is LauncherSearchResult.App &&
                only.score >= AUTO_LAUNCH_MIN_SCORE &&
                !SmartCommandEngine.hasPotentialCommandOrAliasMatch(
                    rawQuery = query,
                    configs = s.commandConfigs,
                    activeFocusProfile = activeProfile,
                    macros = s.macros,
                    customAliases = s.customAliases,
                    contacts = s.searchContacts,
                    matchedAppUniqueKey = only.app.uniqueKey,
                    matchedAppPackage = only.app.packageName
                )
            ) {
                launchSearchResult(only)
                return
            }
        }

        val shouldAutoFetch = when (asyncRequest) {
            null -> false
            is AsyncCommandRequest.Wolfram ->
                s.wolframSubmitMode == WolframSubmitMode.API && s.wolframRequestTrigger == WolframRequestTrigger.IDLE
            is AsyncCommandRequest.Wiki, is AsyncCommandRequest.Define ->
                s.wikiDefRequestTrigger == WolframRequestTrigger.IDLE
            is AsyncCommandRequest.Finance, is AsyncCommandRequest.Weather ->
                s.stockWeatherRequestTrigger == WolframRequestTrigger.IDLE
            else -> true
        }
        if (asyncRequest != null && shouldAutoFetch) {
            liveCommandJob = viewModelScope.launch {
                val debounceMs = when (asyncRequest) {
                    is AsyncCommandRequest.Wolfram -> waDelaySec * 1000L
                    is AsyncCommandRequest.Wiki, is AsyncCommandRequest.Define -> wikiDefDelaySec * 1000L
                    is AsyncCommandRequest.Finance, is AsyncCommandRequest.Weather -> stockWxDelaySec * 1000L
                    is AsyncCommandRequest.MathPlot -> 260L
                    is AsyncCommandRequest.LocalFiles -> 140L
                    is AsyncCommandRequest.CalendarAgenda -> 0L
                }
                if (debounceMs > 0L) delay(debounceMs)
                if (_searchQuery.value != query) return@launch
                when (asyncRequest) {
                    is AsyncCommandRequest.Wolfram -> {
                        _searchResults.value = _searchResults.value.map { cur ->
                            if (cur is LauncherSearchResult.RichCard && cur.type == SmartCommandType.WOLFRAM) {
                                cur.copy(id = "wa_fetching_${asyncRequest.query}", isLoading = true, subtitle = "querying Wolfram|Alpha…")
                            } else cur
                        }
                    }
                    is AsyncCommandRequest.Wiki -> {
                        _searchResults.value = _searchResults.value.map { cur ->
                            if (cur is LauncherSearchResult.RichCard && cur.type == SmartCommandType.WIKI) {
                                cur.copy(id = "wiki_fetching_${asyncRequest.query}", isLoading = true, subtitle = "fetching Wikipedia summary…")
                            } else cur
                        }
                    }
                    is AsyncCommandRequest.Define -> {
                        _searchResults.value = _searchResults.value.map { cur ->
                            if (cur is LauncherSearchResult.RichCard && cur.type == SmartCommandType.DEFINE) {
                                cur.copy(id = "def_fetching_${asyncRequest.word}", isLoading = true, subtitle = "looking up definition…")
                            } else cur
                        }
                    }
                    is AsyncCommandRequest.Finance -> {
                        _searchResults.value = _searchResults.value.map { cur ->
                            if (cur is LauncherSearchResult.RichCard && cur.type == SmartCommandType.FINANCE) {
                                cur.copy(id = "fin_fetching_${asyncRequest.query}", isLoading = true, subtitle = "fetching market quote…")
                            } else cur
                        }
                    }
                    is AsyncCommandRequest.Weather -> {
                        val keyPrefix = if (asyncRequest.isSolarOnly) "solar" else "wx"
                        val cardType = if (asyncRequest.isSolarOnly) SmartCommandType.SOLAR else SmartCommandType.WEATHER
                        _searchResults.value = _searchResults.value.map { cur ->
                            if (cur is LauncherSearchResult.RichCard && cur.type == cardType) {
                                cur.copy(
                                    id = "${keyPrefix}_fetching_${asyncRequest.city}",
                                    isLoading = true,
                                    subtitle = if (asyncRequest.isSolarOnly) "calculating solar times…" else "fetching current conditions…"
                                )
                            } else cur
                        }
                    }
                    else -> Unit
                }
                if (asyncRequest is AsyncCommandRequest.LocalFiles) {
                    val files = LocalFileSearchService.searchFiles(context, asyncRequest.query)
                    if (_searchQuery.value != query) return@launch
                    _searchResults.value = _searchResults.value.flatMap { cur ->
                        if (cur is LauncherSearchResult.RichCard && cur.type == SmartCommandType.FILE && cur.isLoading) {
                            if (files.isNotEmpty()) files
                            else listOf(
                                LauncherSearchResult.RichCard(
                                    id = "files_empty_${asyncRequest.query}",
                                    type = SmartCommandType.FILE,
                                    title = "Files matching \"${asyncRequest.query}\"",
                                    subtitle = "No matching files found"
                                )
                            )
                        } else listOf(cur)
                    }
                    return@launch
                }
                val resolved = resolveLiveCommand(asyncRequest)
                if (_searchQuery.value != query) return@launch
                _searchResults.value = _searchResults.value.map { current ->
                    if (current is LauncherSearchResult.RichCard && current.isLoading && current.type == resolved.type) resolved else current
                }
            }
        }
    }

    /** Handles Enter or card tap for deferred/pending API lookups (Wolfram, Wikipedia, Dictionary) and Math plots. */
    fun submitWolframQuery(): Boolean {
        val loadingPlot = _searchResults.value.firstOrNull {
            it is LauncherSearchResult.RichCard && it.type == SmartCommandType.MATH_STATS && it.isLoading
        }
        if (loadingPlot != null) {
            liveCommandJob?.cancel()
            val q = _searchQuery.value
            val mathPrefix = settings.value.commandConfigs["math"]?.takeIf { it.enabled }?.prefix?.trim().orEmpty()
            liveCommandJob = viewModelScope.launch {
                val resolved = resolveLiveCommand(AsyncCommandRequest.MathPlot(q, mathPrefix))
                if (_searchQuery.value != q) return@launch
                _searchResults.value = _searchResults.value.map { current ->
                    if (current is LauncherSearchResult.RichCard && current.type == SmartCommandType.MATH_STATS && current.isLoading) resolved else current
                }
            }
            return true
        }

        val s = settings.value
        val query = _searchQuery.value

        triggerDeferredApiCard(
            query = query,
            cardType = SmartCommandType.WIKI,
            prefixKey = "wiki_fetching_",
            pendingKey = "wiki_pending_",
            waitingKey = "wiki_waiting_",
            loadingSubtitle = "fetching Wikipedia summary…"
        ) {
            val arg = commandArgument(query, s, "wiki") ?: return@triggerDeferredApiCard null
            AsyncCommandRequest.Wiki(arg) to "wiki_fetching_$arg"
        }?.let { return it }

        triggerDeferredApiCard(
            query = query,
            cardType = SmartCommandType.DEFINE,
            prefixKey = "def_fetching_",
            pendingKey = "def_pending_",
            waitingKey = "def_waiting_",
            loadingSubtitle = "looking up definition…"
        ) {
            val arg = commandArgument(query, s, "def") ?: return@triggerDeferredApiCard null
            AsyncCommandRequest.Define(arg) to "def_fetching_$arg"
        }?.let { return it }

        triggerDeferredApiCard(
            query = query,
            cardType = SmartCommandType.FINANCE,
            prefixKey = "fin_fetching_",
            pendingKey = "fin_pending_",
            waitingKey = "fin_waiting_",
            loadingSubtitle = "fetching market quote…"
        ) {
            val arg = commandArgument(query, s, "stock") ?: return@triggerDeferredApiCard null
            AsyncCommandRequest.Finance(arg) to "fin_fetching_$arg"
        }?.let { return it }

        triggerDeferredApiCard(
            query = query,
            cardType = SmartCommandType.WEATHER,
            prefixKey = "wx_fetching_",
            pendingKey = "wx_pending_",
            waitingKey = "wx_waiting_",
            loadingSubtitle = "fetching current conditions…"
        ) {
            val fallbackCity = s.defaultWeatherCity.trim().ifEmpty { LauncherSettings.DEFAULT_WEATHER_CITY }
            val arg = commandArgument(query, s, "weather")
                ?: bareOrPrefixedArg(query, "weather")
                ?: return@triggerDeferredApiCard null
            val city = arg.ifEmpty { fallbackCity }
            AsyncCommandRequest.Weather(city, isSolarOnly = false) to "wx_fetching_$city"
        }?.let { return it }

        triggerDeferredApiCard(
            query = query,
            cardType = SmartCommandType.SOLAR,
            prefixKey = "solar_fetching_",
            pendingKey = "solar_pending_",
            waitingKey = "solar_waiting_",
            loadingSubtitle = "calculating solar times…"
        ) {
            val fallbackCity = s.defaultWeatherCity.trim().ifEmpty { LauncherSettings.DEFAULT_WEATHER_CITY }
            val arg = commandArgument(query, s, "solar")
                ?: bareOrPrefixedArg(query, "sunrise")
                ?: bareOrPrefixedArg(query, "sunset")
                ?: return@triggerDeferredApiCard null
            val city = arg.ifEmpty { fallbackCity }
            AsyncCommandRequest.Weather(city, isSolarOnly = true) to "solar_fetching_$city"
        }?.let { return it }

        val waArg = commandArgument(query, s, "wolfram") ?: return false
        if (s.wolframSubmitMode == WolframSubmitMode.WEBSITE) {
            liveCommandJob?.cancel()
            dismissOverlay()
            SystemActionsManager.openUrl(
                context,
                "https://www.wolframalpha.com/input?i=${android.net.Uri.encode(waArg)}",
                webSearchConfig(s).browserPackage
            )
            return true
        }
        return triggerDeferredApiCard(
            query = query,
            cardType = SmartCommandType.WOLFRAM,
            prefixKey = "wa_fetching_",
            pendingKey = "wa_pending_",
            waitingKey = "wa_waiting_",
            loadingSubtitle = "querying Wolfram|Alpha…"
        ) {
            AsyncCommandRequest.Wolfram(waArg) to "wa_fetching_$waArg"
        } ?: false
    }

    private fun triggerDeferredApiCard(
        query: String,
        cardType: SmartCommandType,
        prefixKey: String,
        pendingKey: String,
        waitingKey: String,
        loadingSubtitle: String,
        buildRequestAndFetchingId: () -> Pair<AsyncCommandRequest, String>?
    ): Boolean? {
        val currentCard = _searchResults.value.firstOrNull {
            it is LauncherSearchResult.RichCard && it.type == cardType
        } as? LauncherSearchResult.RichCard ?: return null
        if (currentCard.id.startsWith(prefixKey)) return true
        val canTrigger = currentCard.isLoading ||
            currentCard.id.startsWith(pendingKey) ||
            currentCard.id.startsWith(waitingKey)
        if (!canTrigger) return false

        val (request, fetchingId) = buildRequestAndFetchingId() ?: return false
        liveCommandJob?.cancel()
        _searchResults.value = _searchResults.value.map { current ->
            if (current is LauncherSearchResult.RichCard && current.type == cardType) {
                current.copy(id = fetchingId, isLoading = true, primaryValue = "", subtitle = loadingSubtitle)
            } else current
        }
        liveCommandJob = viewModelScope.launch {
            val resolved = resolveLiveCommand(request)
            if (_searchQuery.value != query) return@launch
            _searchResults.value = _searchResults.value.map { current ->
                if (current is LauncherSearchResult.RichCard && current.type == cardType) resolved else current
            }
        }
        return true
    }

    private fun bareOrPrefixedArg(query: String, keyword: String): String? {
        val trimmed = query.trim()
        if (trimmed.equals(keyword, ignoreCase = true)) return ""
        if (trimmed.startsWith("$keyword ", ignoreCase = true)) return trimmed.drop(keyword.length + 1).trim()
        return null
    }

    private fun commandArgument(query: String, s: LauncherSettings, commandId: String): String? {
        val prefix = s.commandConfigs[commandId]?.takeIf { it.enabled }?.prefix?.trim().orEmpty()
        if (prefix.isEmpty()) return null
        val trimmed = query.trim()
        if (!trimmed.startsWith(prefix, ignoreCase = true)) return null
        val isSymbolPrefix = prefix.all { !it.isLetterOrDigit() }
        if (!isSymbolPrefix && trimmed.length > prefix.length && !trimmed[prefix.length].isWhitespace()) return null
        if ((commandId == "weather" || commandId == "solar") && trimmed.length == prefix.length) return ""
        return trimmed.drop(prefix.length).trim().takeIf { it.isNotEmpty() }
    }

    fun refreshDeviceContacts(force: Boolean = false) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return
        val nowElapsed = SystemClock.elapsedRealtime()
        if (!force &&
            lastContactsRefreshElapsedMs != 0L &&
            deviceContacts.value.isNotEmpty() &&
            nowElapsed - lastContactsRefreshElapsedMs < CONTACTS_REFRESH_TTL_MS
        ) {
            return
        }
        lastContactsRefreshElapsedMs = nowElapsed
        viewModelScope.launch(Dispatchers.IO) {
            val loaded = runCatching {
                val contacts = mutableListOf<SearchContact>()
                val projection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                )
                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    null,
                    null,
                    "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE NOCASE ASC"
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    val nameColumn = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numberColumn = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    while (cursor.moveToNext()) {
                        val id = if (idColumn >= 0) cursor.getString(idColumn).orEmpty() else ""
                        val name = if (nameColumn >= 0) cursor.getString(nameColumn).orEmpty() else ""
                        val number = if (numberColumn >= 0) cursor.getString(numberColumn).orEmpty() else ""
                        if (name.isNotBlank() && number.isNotBlank()) {
                            contacts += SearchContact("device:$id:$number", name, name, number)
                        }
                    }
                }
                contacts.distinctBy { it.phoneNumber.filter(Char::isDigit).ifEmpty { it.phoneNumber } }
            }.getOrDefault(emptyList())
            deviceContacts.value = loaded
            withContext(Dispatchers.Main) {
                if (_searchQuery.value.startsWith("@")) onSearchQueryChanged(_searchQuery.value)
            }
        }
    }

    private suspend fun resolveLiveCommand(request: AsyncCommandRequest): LauncherSearchResult.RichCard {
        val browserPkg = webSearchConfig(settings.value).browserPackage
        return when (request) {
            is AsyncCommandRequest.Finance -> LiveCommandService.fetchMarketQuote(
                rawQuery = request.query,
                range = settings.value.stockDefaultRange,
                interval = settings.value.stockDefaultInterval,
                showSparkline = settings.value.stockShowSparkline,
                browserPackage = browserPkg
            )
            is AsyncCommandRequest.Weather -> LiveCommandService.fetchWeatherOrSolar(request.city, request.isSolarOnly)
            is AsyncCommandRequest.Wolfram -> LiveCommandService.fetchWolframShortAnswer(
                query = request.query,
                appId = settings.value.wolframAppId,
                browserPackage = browserPkg
            )
            is AsyncCommandRequest.Wiki -> LiveCommandService.fetchWikipediaSummary(request.query, browserPkg)
            is AsyncCommandRequest.Define -> LiveCommandService.fetchDefinition(request.word, browserPkg)
            is AsyncCommandRequest.CalendarAgenda -> CalendarAgendaService.fetchAgendaCard(context, settings.value.is24HourClock)
            is AsyncCommandRequest.LocalFiles -> LauncherSearchResult.RichCard(
                id = "files_unused",
                type = SmartCommandType.FILE,
                title = "Files"
            )
            is AsyncCommandRequest.MathPlot -> withContext(Dispatchers.Default) {
                val out = MathStatsEngine.evaluate(request.rawInput, request.prefix)
                    ?: MathStatsEngine.evaluate(request.rawInput, "")
                val cleanPlotInput = request.rawInput.trim().let { t ->
                    if (request.prefix.isNotEmpty() && t.startsWith(request.prefix, ignoreCase = true)) {
                        t.substring(request.prefix.length).trim()
                    } else t
                }
                if (out != null) {
                    LauncherSearchResult.RichCard(
                        id = "cmd_math_${out.title}",
                        type = SmartCommandType.MATH_STATS,
                        title = out.title,
                        primaryValue = out.primaryValue,
                        consoleOutput = out.consoleOutput,
                        chart = out.chart,
                        badgeText = out.badge,
                        isBadgePositive = true,
                        subtitle = out.subtitle,
                        hasInfoManual = false,
                        rawInput = cleanPlotInput,
                        execute = { ctx ->
                            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            cm?.setPrimaryClip(ClipData.newPlainText("Pallas Math", out.copyText))
                            Toast.makeText(ctx, "Copied ${out.copyText}", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    LauncherSearchResult.RichCard(
                        id = "cmd_math_plot_error",
                        type = SmartCommandType.MATH_STATS,
                        title = request.rawInput,
                        subtitle = "Could not evaluate plot — verify domain or syntax",
                        isLoading = false
                    )
                }
            }
        }
    }

    private val browserLabelCache = HashMap<String, String?>()

    private fun webSearchConfig(s: LauncherSettings): WebSearchConfig {
        val pkg = s.preferredBrowserPackage
        val label = if (pkg == null) null else browserLabelCache.getOrPut(pkg) { BrowserHelper.labelFor(context, pkg) }
        return WebSearchConfig(s.webSearchEngine, pkg.takeIf { label != null }, label)
    }

    fun launchSearchResult(result: LauncherSearchResult) {
        if (result is LauncherSearchResult.RichCard &&
            (result.id.startsWith("wa_pending_") ||
                result.id.startsWith("wa_waiting_") ||
                result.id.startsWith("wiki_pending_") ||
                result.id.startsWith("wiki_waiting_") ||
                result.id.startsWith("def_pending_") ||
                result.id.startsWith("def_waiting_") ||
                result.id.startsWith("fin_pending_") ||
                result.id.startsWith("fin_waiting_") ||
                result.id.startsWith("wx_pending_") ||
                result.id.startsWith("wx_waiting_") ||
                result.id.startsWith("solar_pending_") ||
                result.id.startsWith("solar_waiting_"))
        ) {
            submitWolframQuery()
            return
        }
        if (result.isInertOnTap) return
        dismissOverlay()
        when (result) {
            is LauncherSearchResult.App -> {
                launchApp(result.app.packageName, result.app.activityName, result.app.userHandle)
            }
            is LauncherSearchResult.Command -> {
                when {
                    result.id == SETTINGS_COMMAND_ID -> openSettings()
                    result.id.startsWith(PALLAS_NAV_PREFIX) -> {
                        val target = PallasTarget.decode(result.id.removePrefix(PALLAS_NAV_PREFIX))
                        when (target) {
                            PallasTarget.SettingsMain, null -> openSettings()
                            PallasTarget.FocusSettings -> openSettings(SettingsSubpage.FOCUS_MODE)
                            PallasTarget.MacrosSettings -> openSettings(SettingsSubpage.MACROS)
                            PallasTarget.WorldAtlas -> openAtlas()
                            PallasTarget.Notebook -> openNotebook()
                            PallasTarget.LatexStudio -> openLatexStudio()
                            is PallasTarget.GuideTab -> openSettings(
                                initialSubpage = SettingsSubpage.MATH_MANUAL,
                                guideTab = target.tabIndex
                            )
                            is PallasTarget.Cheatsheet -> openSettings(
                                initialSubpage = SettingsSubpage.MATH_MANUAL,
                                guideTab = 1,
                                guideTopicId = target.topicId,
                                guideFormulaTitle = target.formulaTitle
                            )
                        }
                    }
                    result.id == QUICK_CAPTURE_INBOX_ID -> openQuickCaptureInbox()
                    result.id == FOCUS_SETUP_COMMAND_ID -> openSettings(SettingsSubpage.FOCUS_MODE)
                    result.id == FOCUS_OFF_COMMAND_ID -> {
                        focusModeManager.deactivate(settings.value, isManualDismiss = true)
                    }
                    result.id.startsWith(FOCUS_ON_COMMAND_ID) -> {
                        val parts = result.id.removePrefix("$FOCUS_ON_COMMAND_ID:").split(":")
                        val profileId = parts.getOrNull(0).orEmpty()
                        val secs = parts.getOrNull(1)?.toIntOrNull() ?: 0
                        val dndOverride = when (parts.getOrNull(2)) {
                            "true" -> true
                            "false" -> false
                            else -> null
                        }
                        val profile = settings.value.focusProfiles.firstOrNull { it.id == profileId }
                            ?: settings.value.defaultFocusProfileId?.let { defId ->
                                settings.value.focusProfiles.firstOrNull { it.id == defId }
                            }
                            ?: settings.value.focusProfiles.firstOrNull()
                        if (profile != null) {
                            val willDnd = dndOverride ?: profile.syncDnd
                            if ((willDnd || profile.muteRinger) && !SystemActionsManager.isDndAccessGranted(context)) {
                                SystemActionsManager.openDndAccessSettings(context)
                            }
                            focusModeManager.activate(
                                profile = profile,
                                durationMillis = if (secs > 0) secs * 1000L else null,
                                forceDndOverride = dndOverride,
                                isScheduled = false
                            )
                            runOnEnterMacro(profile)
                        }
                    }
                    result.id.startsWith(MACRO_COMMAND_PREFIX) -> {
                        val macroId = result.id.removePrefix(MACRO_COMMAND_PREFIX)
                        settings.value.macros.firstOrNull { it.id == macroId }?.let { executeMacro(it) }
                    }
                    else -> result.execute(context)
                }
            }
            is LauncherSearchResult.RichCard -> result.execute(context)
            is LauncherSearchResult.ContactCall -> result.execute(context)
        }
    }

    private fun isPackageOrKeyAllowedInFocus(
        keyOrPkg: String,
        allowedKeys: Set<String>? = focusAllowedAppKeys.value
    ): Boolean {
        if (allowedKeys == null) return true
        val pkg = keyOrPkg.substringBefore('/')
        return keyOrPkg in allowedKeys || allowedKeys.any { it.substringBefore('/') == pkg }
    }

    private fun resolveDefaultHandlerPackage(intent: Intent): String? =
        context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName

    private fun runDueScheduledMacros() {
        val due = macroScheduler.collectDueMacros(settings.value.macros)
        due.forEach { executeMacro(it) }
    }

    fun executeMacro(macro: MacroRoutine) {
        viewModelScope.launch {
            MacroExecutor.execute(
                context = context,
                macro = macro,
                settingsRepository = settingsRepository,
                appRepository = appRepository,
                quickCaptureRepository = quickCaptureRepository,
                allowedKeys = focusAllowedAppKeys.value,
                onOverlay = { cmd ->
                    when (cmd) {
                        MacroExecutor.OverlayCommand.Notebook -> openNotebook()
                        MacroExecutor.OverlayCommand.LatexStudio -> openLatexStudio()
                        MacroExecutor.OverlayCommand.Atlas -> openAtlas()
                        MacroExecutor.OverlayCommand.QuickCapture -> openQuickCaptureInbox()
                        is MacroExecutor.OverlayCommand.SwitchScreen -> {
                            val idx = displayScreens.value.indexOfFirst { it.id == cmd.screenId }
                            if (idx >= 0) setScreenIndex(idx)
                        }
                    }
                }
            )
        }
    }

    fun executeGesture(action: GestureAction) {
        val allowed = focusAllowedAppKeys.value
        when (action) {
            is GestureAction.OpenSearch -> openSearch()
            is GestureAction.OpenAppDrawer -> openDrawer()
            is GestureAction.OpenNotifications -> SystemActionsManager.expandStatusBar(context)
            is GestureAction.OpenQuickSettings -> SystemActionsManager.expandQuickSettings(context)
            is GestureAction.OpenCamera -> {
                if (allowed != null) {
                    val pkg = resolveDefaultHandlerPackage(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
                    if (pkg == null || !isPackageOrKeyAllowedInFocus(pkg, allowed)) return
                }
                SystemActionsManager.openCamera(context)
            }
            is GestureAction.OpenDialer -> {
                if (allowed != null) {
                    val pkg = resolveDefaultHandlerPackage(Intent(Intent.ACTION_DIAL))
                    if (pkg == null || !isPackageOrKeyAllowedInFocus(pkg, allowed)) return
                }
                SystemActionsManager.openDialer(context)
            }
            is GestureAction.LockScreen -> SystemActionsManager.lockScreen(context)
            is GestureAction.OpenNotebook -> openNotebook()
            is GestureAction.OpenLatexStudio -> openLatexStudio()
            is GestureAction.OpenAtlas -> openAtlas()
            is GestureAction.ToggleFocusProfile -> {
                val s = settings.value
                val profile = if (action.profileId == GestureAction.DEFAULT_FOCUS_PROFILE_ID) {
                    s.focusProfiles.firstOrNull { it.id == s.defaultFocusProfileId } ?: s.focusProfiles.firstOrNull()
                } else {
                    s.focusProfiles.firstOrNull { it.id == action.profileId }
                }
                if (profile != null) {
                    val wasAlreadyThisProfile = focusState.value.isActive && focusState.value.activeProfileId == profile.id
                    if (!wasAlreadyThisProfile && (profile.syncDnd || profile.muteRinger) && !SystemActionsManager.isDndAccessGranted(context)) {
                        SystemActionsManager.openDndAccessSettings(context)
                    }
                    focusModeManager.toggleProfile(profile, s)
                    if (!wasAlreadyThisProfile) {
                        runOnEnterMacro(profile)
                    }
                }
            }
            is GestureAction.RunMacro -> {
                settings.value.macros.firstOrNull { it.id == action.macroId }?.let { executeMacro(it) }
            }
            is GestureAction.LaunchApp -> {
                if (!isPackageOrKeyAllowedInFocus(action.uniqueKey, allowed)) return
                val app = allInstalledApps.value.firstOrNull { it.uniqueKey == action.uniqueKey }
                    ?: allInstalledApps.value.firstOrNull { it.packageName == action.packageName }
                if (app != null) {
                    launchApp(app.packageName, app.activityName, app.userHandle)
                } else {
                    launchApp(action.packageName)
                }
            }
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
        val targetUser = userHandle ?: Process.myUserHandle()
        val app = installedApps.value.firstOrNull {
            it.packageName == packageName &&
                    (activityName == null || it.activityName == activityName) &&
                    (it.userHandle == null || it.userHandle == targetUser)
        }
        app?.uniqueKey?.let { appUsageTracker.recordAppLaunch(it) }
        appRepository.launchApp(packageName, activityName, targetUser)
    }

    fun openAppInfo(packageName: String, activityName: String? = null, userHandle: UserHandle? = null) {
        appRepository.openAppInfo(packageName, activityName, userHandle)
    }

    fun getShortcutsForApp(packageName: String, userHandle: UserHandle? = null): List<android.content.pm.ShortcutInfo> =
        appRepository.getAppShortcuts(packageName, userHandle)

    fun launchShortcut(packageName: String, shortcutId: String, userHandle: UserHandle? = null) {
        val handle = userHandle ?: Process.myUserHandle()
        SystemActionsManager.launchShortcut(context, packageName, shortcutId, handle)
    }

    fun onResume(preserveSearch: Boolean = false) {
        syncFocusAndRunEnterMacro()
        runDueScheduledMacros()
        refreshDeviceContacts()
        if (!preserveSearch && _overlay.value is LauncherOverlay.Search && _searchQuery.value.isNotBlank()) {
            dismissOverlay()
        }
    }

    fun onHomeIntent(launcherWasVisible: Boolean) {
        val current = _overlay.value
        if (launcherWasVisible) {
            if (current !is LauncherOverlay.None) dismissOverlay()
            else if (_currentScreenIndex.value != 0) setScreenIndex(0)
        } else {
            when (current) {
                is LauncherOverlay.Search -> closeSearch()
                is LauncherOverlay.Drawer,
                is LauncherOverlay.AppOptions, is LauncherOverlay.AppPicker,
                is LauncherOverlay.SetAlias, is LauncherOverlay.RenameApp -> {
                    _overlay.value = LauncherOverlay.None
                }
                else -> Unit
            }
        }
    }

    fun sortedForDisplay(apps: List<ScreenApp>): List<ScreenApp> =
        screenRepository.sortAppsByNameLength(apps, settings.value.nameLengthSortOrder)

    fun addScreen(name: String, contextTag: String? = null) = screenRepository.addScreen(name, contextTag)
    fun renameScreen(screenId: String, newName: String, newContextTag: String? = null) = screenRepository.renameScreen(screenId, newName, newContextTag)
    fun moveScreen(fromIndex: Int, toIndex: Int) = screenRepository.moveScreen(fromIndex, toIndex)
    fun removeScreen(screenId: String) = screenRepository.removeScreen(screenId)
    fun addAppToScreen(screenId: String, appItem: AppItem, customLabel: String? = null) =
        screenRepository.addAppToScreen(screenId, appItem, customLabel, NameLengthSortOrder.OFF)
    fun addAppsToScreen(screenId: String, appItems: List<AppItem>) =
        screenRepository.addAppsToScreen(screenId, appItems, NameLengthSortOrder.OFF)
    fun removeAppFromScreen(screenId: String, uniqueKey: String) = screenRepository.removeAppFromScreen(screenId, uniqueKey)
    fun removeAppFromAllScreens(uniqueKey: String) = screenRepository.removeAppFromAllScreens(uniqueKey)
    fun updateAppLabelAcrossScreens(uniqueKey: String, newLabel: String) =
        screenRepository.updateAppLabelAcrossScreens(uniqueKey, newLabel, NameLengthSortOrder.OFF)
    fun updateAppLabel(screenId: String, uniqueKey: String, newLabel: String) =
        screenRepository.updateAppLabel(screenId, uniqueKey, newLabel, NameLengthSortOrder.OFF)
    fun reorderApps(screenId: String, reordered: List<ScreenApp>) = screenRepository.reorderApps(screenId, reordered)

    fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) =
        settingsRepository.update(transform)

    fun setAlias(alias: String, target: String) = settingsRepository.setAlias(alias, target)
    fun removeAlias(alias: String) = settingsRepository.removeAlias(alias)
    fun hideApp(uniqueKey: String) = settingsRepository.hideApp(uniqueKey)
    fun unhideApp(uniqueKey: String) = settingsRepository.unhideApp(uniqueKey)
    fun hideFromAppList(uniqueKey: String) = settingsRepository.hideFromAppList(uniqueKey)
    fun unhideFromAppList(uniqueKey: String) = settingsRepository.unhideFromAppList(uniqueKey)

    fun requestSetDefaultLauncher() =
        SystemActionsManager.requestSetDefaultLauncher(context)

    override fun onCleared() {
        runCatching { context.unregisterReceiver(batteryReceiver) }
        runCatching { context.unregisterReceiver(timeTickReceiver) }
        super.onCleared()
    }

    private companion object {
        const val KEY_CURRENT_SCREEN_ID = "current_screen_id"
        const val AUTO_LAUNCH_MIN_SCORE = 3500.0
        const val CONTACTS_REFRESH_TTL_MS = 15L * 60L * 1000L
    }
}
