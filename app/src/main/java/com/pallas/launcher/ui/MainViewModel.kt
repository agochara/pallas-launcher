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
import android.os.UserHandle
import android.provider.ContactsContract
import android.widget.Toast
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
import com.pallas.launcher.domain.model.WolframRequestTrigger
import com.pallas.launcher.domain.model.WolframSubmitMode
import com.pallas.launcher.domain.model.NameLengthSortOrder
import com.pallas.launcher.domain.model.ScreenApp
import com.pallas.launcher.domain.search.AsyncCommandRequest
import com.pallas.launcher.domain.search.LauncherSearchResult
import com.pallas.launcher.domain.search.MathStatsEngine
import com.pallas.launcher.domain.search.SETTINGS_COMMAND_ID
import com.pallas.launcher.domain.search.SearchEngine
import com.pallas.launcher.domain.search.SmartCommandType
import com.pallas.launcher.domain.search.WebSearchConfig
import com.pallas.launcher.domain.search.isInertOnTap
import com.pallas.launcher.platform.BatteryManagerHelper
import com.pallas.launcher.platform.BrowserHelper
import com.pallas.launcher.platform.CurrencyRateService
import com.pallas.launcher.platform.LiveCommandService
import com.pallas.launcher.platform.SystemActionsManager
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
    data object Settings : LauncherOverlay
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

    private val uiPrefs = context.getSharedPreferences("pallas_ui_state", Context.MODE_PRIVATE)

    val screens: StateFlow<List<HomeScreen>> = screenRepository.screens
    val customAppLabels: StateFlow<Map<String, String>> = screenRepository.customAppLabels
    val settings: StateFlow<LauncherSettings> = settingsRepository.settings

    val installedApps: StateFlow<List<AppItem>> =
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
        combine(screenRepository.screens, settingsRepository.settings.map { it.nameLengthSortOrder }.distinctUntilChanged()) { list, order ->
            if (order == NameLengthSortOrder.OFF) list
            else list.map { it.copy(apps = screenRepository.sortAppsByNameLength(it.apps, order)) }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, screenRepository.screens.value)

    private fun savedScreenIndex(): Int {
        val id = uiPrefs.getString(KEY_CURRENT_SCREEN_ID, null) ?: return 0
        return screenRepository.screens.value.indexOfFirst { it.id == id }.coerceAtLeast(0)
    }

    private fun persistCurrentScreen() {
        val id = screens.value.getOrNull(_currentScreenIndex.value)?.id ?: return
        uiPrefs.edit().putString(KEY_CURRENT_SCREEN_ID, id).apply()
    }

    private val _currentScreenIndex = MutableStateFlow(savedScreenIndex())
    val currentScreenIndex: StateFlow<Int> = combine(_currentScreenIndex, screenRepository.screens) { index, screenList ->
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
        CurrencyRateService.init(context)
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
            }
        }

        viewModelScope.launch {
            screens.collect { screenList ->
                if (screenList.isEmpty()) return@collect
                val savedId = uiPrefs.getString(KEY_CURRENT_SCREEN_ID, null)
                val byId = savedId?.let { id -> screenList.indexOfFirst { it.id == id } } ?: -1
                _currentScreenIndex.value = if (byId >= 0) byId else _currentScreenIndex.value.coerceIn(0, screenList.size - 1)
                persistCurrentScreen()
            }
        }

        viewModelScope.launch {
            combine(appRepository.installedApps, settings, screens, customAppLabels) { apps, s, screenList, globalCustom ->
                val screenCustom = screenList.flatMap { it.apps }
                    .filter { !it.customLabel.isNullOrBlank() }
                    .associate { it.uniqueKey to it.customLabel!! }
                Triple(apps, s.customAliases, screenCustom + globalCustom)
            }.distinctUntilChanged().collect { (apps, aliases, custom) -> searchEngine.updateIndex(apps, aliases, custom) }
        }

        viewModelScope.launch {
            installedApps.collect { apps ->
                if (apps.isNotEmpty()) {
                    appUsageTracker.migratePackageKeysToUniqueKeys(apps)
                    settingsRepository.migrateHiddenApps(apps)
                }
            }
        }
    }

    private fun updateTimeAndDate() {
        _currentTime.value = formatCurrentTime(settings.value.is24HourClock)
        _currentDate.value = formatCurrentDate(settings.value.showBattery, _batteryLevel.value)
    }

    fun onScreenSwiped(direction: Int) {
        val screenList = screens.value
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
        if (index in screens.value.indices) {
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
        _overlay.value = LauncherOverlay.Search
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

    fun openSettings() {
        _overlay.value = LauncherOverlay.Settings
    }

    fun closeSettings() {
        if (_overlay.value is LauncherOverlay.Settings) {
            _currentScreenIndex.value = savedScreenIndex()
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
        _searchQuery.value = query
        liveCommandJob?.cancel()
        liveCommandJob = null
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        val s = settings.value
        val contacts = mergeSearchContacts(s.searchContacts, deviceContacts.value)
        val (rawImmediate, asyncRequest) = searchEngine.searchAll(
            query,
            contacts,
            webSearchConfig(s),
            s.commandConfigs,
            s.defaultWeatherCity
        )
        val waQuery = wolframArgument(query, s)
        val immediate = if (waQuery != null && asyncRequest is AsyncCommandRequest.Wolfram) {
            when {
                s.wolframSubmitMode == WolframSubmitMode.WEBSITE -> rawImmediate.map {
                    if (it is LauncherSearchResult.RichCard && it.type == SmartCommandType.WOLFRAM && it.isLoading) {
                        it.copy(
                            isLoading = false,
                            primaryValue = "Open in browser",
                            subtitle = "",
                            execute = { ctx ->
                                SystemActionsManager.openUrl(ctx, "https://www.wolframalpha.com/input?i=${android.net.Uri.encode(waQuery)}")
                            }
                        )
                    } else it
                }
                s.wolframRequestTrigger == WolframRequestTrigger.ENTER -> rawImmediate.map {
                    if (it is LauncherSearchResult.RichCard && it.type == SmartCommandType.WOLFRAM && it.isLoading) {
                        it.copy(
                            isLoading = false,
                            primaryValue = "Query Wolfram|Alpha",
                            subtitle = "",
                            execute = {}
                        )
                    } else it
                }
                else -> rawImmediate
            }
        } else rawImmediate
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
            if (only is LauncherSearchResult.App && only.score >= AUTO_LAUNCH_MIN_SCORE) {
                launchSearchResult(only)
                return
            }
        }

        if (asyncRequest != null && (asyncRequest !is AsyncCommandRequest.Wolfram ||
                (s.wolframRequestTrigger == WolframRequestTrigger.IDLE && s.wolframSubmitMode == WolframSubmitMode.API))) {
            liveCommandJob = viewModelScope.launch {
                val debounceMs = when (asyncRequest) {
                    is AsyncCommandRequest.Wolfram -> s.wolframIdleDelaySeconds * 1000L
                    is AsyncCommandRequest.MathPlot -> 260L
                    else -> 180L
                }
                delay(debounceMs)
                if (_searchQuery.value != query) return@launch
                val resolved = resolveLiveCommand(asyncRequest)
                if (_searchQuery.value != query) return@launch
                _searchResults.value = _searchResults.value.map { current ->
                    if (current is LauncherSearchResult.RichCard && current.isLoading && current.type == resolved.type) resolved else current
                }
            }
        }
    }

    /** Handles Enter/search-button for explicit Wolfram queries or pending debounced plot evaluations; all other searches keep normal selection behavior. */
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
        val argument = wolframArgument(query, s) ?: return false
        if (s.wolframSubmitMode == WolframSubmitMode.WEBSITE) {
            liveCommandJob?.cancel()
            dismissOverlay()
            SystemActionsManager.openUrl(context, "https://www.wolframalpha.com/input?i=${android.net.Uri.encode(argument)}")
            return true
        }
        val currentCard = _searchResults.value.firstOrNull {
            it is LauncherSearchResult.RichCard && it.type == SmartCommandType.WOLFRAM
        } as? LauncherSearchResult.RichCard ?: return true
        val isPendingOrLoading = currentCard.isLoading || currentCard.id.startsWith("wa_loading_")
        if (!isPendingOrLoading) {
            return false
        }
        liveCommandJob?.cancel()
        _searchResults.value = _searchResults.value.map { current ->
            if (current is LauncherSearchResult.RichCard && current.type == SmartCommandType.WOLFRAM) {
                current.copy(isLoading = true, primaryValue = "", subtitle = "querying Wolfram|Alpha…")
            } else current
        }
        liveCommandJob = viewModelScope.launch {
            val resolved = resolveLiveCommand(AsyncCommandRequest.Wolfram(argument))
            if (_searchQuery.value != query) return@launch
            _searchResults.value = _searchResults.value.map { current ->
                if (current is LauncherSearchResult.RichCard && current.type == SmartCommandType.WOLFRAM) resolved else current
            }
        }
        return true
    }

    private fun wolframArgument(query: String, s: LauncherSettings): String? {
        val prefix = s.commandConfigs["wolfram"]?.takeIf { it.enabled }?.prefix?.trim().orEmpty()
        if (prefix.isEmpty()) return null
        val trimmed = query.trim()
        if (!trimmed.startsWith(prefix, ignoreCase = true)) return null
        if (trimmed.length > prefix.length && !trimmed[prefix.length].isWhitespace()) return null
        return trimmed.drop(prefix.length).trim().takeIf { it.isNotEmpty() }
    }

    fun refreshDeviceContacts() {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return
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
                contacts.distinctBy { it.phoneNumber.filter(Char::isDigit) }
            }.getOrDefault(emptyList())
            deviceContacts.value = loaded
            withContext(Dispatchers.Main) {
                if (_searchQuery.value.startsWith("@")) onSearchQueryChanged(_searchQuery.value)
            }
        }
    }

    private fun mergeSearchContacts(saved: List<SearchContact>, device: List<SearchContact>): List<SearchContact> {
        val savedNumbers = saved.map { it.phoneNumber.filter(Char::isDigit) }.filter { it.isNotEmpty() }.toSet()
        return saved + device.filter { it.phoneNumber.filter(Char::isDigit) !in savedNumbers }
    }

    private suspend fun resolveLiveCommand(request: AsyncCommandRequest): LauncherSearchResult.RichCard =
        when (request) {
            is AsyncCommandRequest.Finance -> LiveCommandService.fetchMarketQuote(
                rawQuery = request.query,
                range = settings.value.stockDefaultRange,
                interval = settings.value.stockDefaultInterval,
                showSparkline = settings.value.stockShowSparkline
            )
            is AsyncCommandRequest.Weather -> LiveCommandService.fetchWeatherOrSolar(request.city, request.isSolarOnly)
            is AsyncCommandRequest.Wolfram -> LiveCommandService.fetchWolframShortAnswer(request.query, settings.value.wolframAppId)
            is AsyncCommandRequest.MathPlot -> withContext(Dispatchers.Default) {
                val out = MathStatsEngine.evaluate(request.rawInput, request.prefix)
                    ?: MathStatsEngine.evaluate(request.rawInput, "")
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

    private val browserLabelCache = HashMap<String, String?>()

    private fun webSearchConfig(s: LauncherSettings): WebSearchConfig {
        val pkg = s.preferredBrowserPackage
        val label = if (pkg == null) null else browserLabelCache.getOrPut(pkg) { BrowserHelper.labelFor(context, pkg) }
        return WebSearchConfig(s.webSearchEngine, pkg.takeIf { label != null }, label)
    }

    fun launchSearchResult(result: LauncherSearchResult) {
        if (result.isInertOnTap) return
        if (result is LauncherSearchResult.RichCard &&
            result.type == SmartCommandType.WOLFRAM &&
            result.id.startsWith("wa_loading_")
        ) {
            submitWolframQuery()
            return
        }
        dismissOverlay()
        when (result) {
            is LauncherSearchResult.App -> {
                launchApp(result.app.packageName, result.app.activityName, result.app.userHandle)
            }
            is LauncherSearchResult.Command -> {
                if (result.id == SETTINGS_COMMAND_ID) openSettings() else result.execute(context)
            }
            is LauncherSearchResult.RichCard -> result.execute(context)
            is LauncherSearchResult.ContactCall -> result.execute(context)
        }
    }

    fun executeGesture(action: GestureAction) {
        when (action) {
            is GestureAction.OpenSearch -> openSearch()
            is GestureAction.OpenAppDrawer -> openDrawer()
            is GestureAction.OpenNotifications -> SystemActionsManager.expandStatusBar(context)
            is GestureAction.OpenQuickSettings -> SystemActionsManager.expandQuickSettings(context)
            is GestureAction.OpenCamera -> SystemActionsManager.openCamera(context)
            is GestureAction.OpenDialer -> SystemActionsManager.openDialer(context)
            is GestureAction.LockScreen -> SystemActionsManager.lockScreen(context)
            is GestureAction.LaunchApp -> {
                val app = installedApps.value.firstOrNull { it.uniqueKey == action.uniqueKey }
                    ?: installedApps.value.firstOrNull { it.packageName == action.packageName }
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
        try {
            context.unregisterReceiver(batteryReceiver)
            context.unregisterReceiver(timeTickReceiver)
        } catch (_: Exception) {}
        super.onCleared()
    }

    private companion object {
        const val KEY_CURRENT_SCREEN_ID = "current_screen_id"
        const val AUTO_LAUNCH_MIN_SCORE = 3500.0
    }
}
