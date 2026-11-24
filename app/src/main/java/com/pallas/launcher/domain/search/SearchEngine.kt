package com.pallas.launcher.domain.search

import com.pallas.launcher.data.repository.AppUsageTracker
import com.pallas.launcher.domain.model.AppItem
import com.pallas.launcher.domain.model.CommandConfig
import com.pallas.launcher.domain.model.FocusProfile
import com.pallas.launcher.domain.model.MacroRoutine
import com.pallas.launcher.domain.model.SearchContact
import java.util.Locale

class SearchEngine(
    private val appUsageTracker: AppUsageTracker? = null
) {
    val searchIndex = AppSearchIndex(appUsageTracker)

    fun updateIndex(
        apps: List<AppItem>,
        aliases: Map<String, String>,
        customLabels: Map<String, String> = emptyMap()
    ) {
        searchIndex.buildIndex(apps, aliases, customLabels)
    }

    fun search(rawQuery: String): List<AppSearchResult> = searchIndex.search(rawQuery)

    /**
     * Ordering contract (index 0 = best = the row next to the search pill, Enter picks the first actionable row):
     *  - A query that is JUST a short command prefix ("w", "al", "q", "st", "wa", "sun", "=") is far more likely an
     *    app search than a command. Apps come first, command hint rows trail, and NO network request is fired.
     *    If no app matches at all, the hints/cards are shown exactly as before.
     *  - Full-word commands ("weather", "sunrise", "alarm", "timer", "call", ...) and anything with an argument
     *    ("w tokyo", "al 530") keep command rows first.
     *  - The "Pallas settings" row always trails the app results, so typing "settings" still finds the system app.
     */
    fun searchAll(
        rawQuery: String,
        contacts: List<SearchContact> = emptyList(),
        webSearch: WebSearchConfig = WebSearchConfig(),
        configs: Map<String, CommandConfig> = CommandConfig.ALL_ENABLED,
        defaultWeatherCity: String = "",
        deviceContacts: List<SearchContact> = emptyList(),
        is24HourClock: Boolean = false,
        dateArithmeticEnabled: Boolean = true,
        colorBaseInspectorEnabled: Boolean = true,
        quickCaptureCount: Int = 0,
        quickCapturePreview: String = "",
        focusProfiles: List<FocusProfile> = emptyList(),
        defaultFocusProfileId: String? = null,
        isFocusActive: Boolean = false,
        activeFocusProfile: FocusProfile? = null,
        focusAllowedAppKeys: Set<String>? = null,
        macros: List<MacroRoutine> = emptyList(),
        isDndEnabled: Boolean = false,
        hasCalendarPermission: Boolean = true,
        hasFileSearchPermission: Boolean = true
    ): Pair<List<LauncherSearchResult>, AsyncCommandRequest?> {
        val trimmed = rawQuery.trim()
        if (trimmed.isEmpty()) return emptyList<LauncherSearchResult>() to null

        val eval = SmartCommandEngine.evaluateCommands(
            rawQuery = trimmed,
            contacts = contacts,
            webSearch = webSearch,
            configs = configs,
            defaultWeatherCity = defaultWeatherCity,
            deviceContacts = deviceContacts,
            is24HourClock = is24HourClock,
            dateArithmeticEnabled = dateArithmeticEnabled,
            colorBaseInspectorEnabled = colorBaseInspectorEnabled,
            quickCaptureCount = quickCaptureCount,
            quickCapturePreview = quickCapturePreview,
            focusProfiles = focusProfiles,
            defaultFocusProfileId = defaultFocusProfileId,
            isFocusActive = isFocusActive,
            activeFocusProfile = activeFocusProfile,
            focusAllowedAppKeys = focusAllowedAppKeys,
            macros = macros,
            isDndEnabled = isDndEnabled,
            hasCalendarPermission = hasCalendarPermission,
            hasFileSearchPermission = hasFileSearchPermission
        )
        if (trimmed.startsWith("@")) {
            return eval.immediateResults to null
        }
        val mathCfg = configs["math"] ?: CommandConfig.DEFAULTS.getValue("math")
        val mathEnabled = mathCfg.enabled && (activeFocusProfile == null || "math" !in activeFocusProfile.disabledCommands)
        val isMathPrefix = mathEnabled && mathCfg.prefix.isNotEmpty() && trimmed.startsWith(mathCfg.prefix)
        val suppressForDirectCommand = eval.immediateResults.any {
            it is LauncherSearchResult.Command && (it.type == SmartCommandType.APP_SEARCH || it.id.startsWith(PALLAS_NAV_PREFIX))
        }
        val suppressAppsForRichCard = suppressForDirectCommand || eval.immediateResults.any {
            it is LauncherSearchResult.RichCard && (
                (it.type == SmartCommandType.MATH_STATS && mathEnabled) ||
                    it.type == SmartCommandType.LATEX ||
                    it.type == SmartCommandType.CONVERT ||
                    it.type == SmartCommandType.WIKI ||
                    it.type == SmartCommandType.WORLD_TIME ||
                    it.type == SmartCommandType.DATE_ARITHMETIC ||
                    it.type == SmartCommandType.DEFINE ||
                    it.type == SmartCommandType.NOTE ||
                    it.type == SmartCommandType.QR ||
                    it.type == SmartCommandType.COLOR_BASE ||
                    it.type == SmartCommandType.EMI ||
                    it.type == SmartCommandType.FILE ||
                    it.type == SmartCommandType.CALENDAR
                )
        }
        val appResults = if (isMathPrefix || suppressAppsForRichCard) {
            emptyList()
        } else {
            searchIndex.search(trimmed).map { LauncherSearchResult.App(it) }
        }

        val trailing = eval.immediateResults.filter { it.id == SETTINGS_COMMAND_ID }
        val commandRows = eval.immediateResults.filter { it.id != SETTINGS_COMMAND_ID }

        return (commandRows + appResults + trailing) to eval.asyncRequest
    }
}
