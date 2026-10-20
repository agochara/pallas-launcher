package com.pallas.launcher.domain.model

enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM
}

enum class ClockVerticalPosition {
    TOP,
    BOTTOM
}

enum class ClockHorizontalAlignment {
    LEFT,
    CENTER,
    RIGHT
}

enum class AppsVerticalPosition {
    TOP,
    CENTER,
    BOTTOM
}

enum class AppsHorizontalAlignment {
    LEFT,
    CENTER,
    RIGHT
}

enum class EdgeScrubPosition {
    RIGHT,
    LEFT,
    OFF
}

enum class EdgeScrubReleaseAction {
    STAY_OPEN,
    LAUNCH_TOP
}

enum class TextShadowMode {
    OFF,
    AUTO,
    ON
}

enum class WebSearchEngine(val label: String, val urlTemplate: String) {
    GOOGLE("Google", "https://www.google.com/search?q=%s"),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q=%s"),
    BRAVE("Brave", "https://search.brave.com/search?q=%s"),
    BING("Bing", "https://www.bing.com/search?q=%s")
}

enum class NameLengthSortOrder {
    OFF,
    ASCENDING,
    DESCENDING
}

data class CommandConfig(
    val id: String,
    val label: String,
    val enabled: Boolean = true,
    val prefix: String
) {
    companion object {
        val DEFAULTS: Map<String, CommandConfig> = mapOf(
            "alarm" to CommandConfig("alarm", "Alarm", true, "al"),
            "timer" to CommandConfig("timer", "Timer", true, "timer"),
            "web" to CommandConfig("web", "Web Search", true, "q"),
            "call" to CommandConfig("call", "Direct Call", true, "call"),
            "math" to CommandConfig("math", "Math & Fat-Tail Stats", true, "="),
            "stock" to CommandConfig("stock", "Markets & Finance", true, "st"),
            "weather" to CommandConfig("weather", "Weather", true, "w"),
            "solar" to CommandConfig("solar", "Sunrise & Sunset", true, "sun"),
            "wolfram" to CommandConfig("wolfram", "Wolfram Alpha", true, "wa")
        )

        val ALL_ENABLED: Map<String, CommandConfig> = DEFAULTS
    }
}

data class LauncherSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val showWallpaperScrim: Boolean = false,
    val hideStatusBar: Boolean = false,
    val clockVerticalPosition: ClockVerticalPosition = ClockVerticalPosition.TOP,
    val clockHorizontalAlignment: ClockHorizontalAlignment = ClockHorizontalAlignment.RIGHT,
    val appsVerticalPosition: AppsVerticalPosition = AppsVerticalPosition.BOTTOM,
    val appsHorizontalAlignment: AppsHorizontalAlignment = AppsHorizontalAlignment.RIGHT,
    val is24HourClock: Boolean = false,
    val showDate: Boolean = true,
    val showBattery: Boolean = true,
    val shortSwipeLeftAction: GestureAction = GestureAction.OpenCamera,
    val shortSwipeRightAction: GestureAction = GestureAction.OpenDialer,
    val swipeUpAction: GestureAction = GestureAction.OpenAppDrawer,
    val swipeDownAction: GestureAction = GestureAction.OpenNotifications,
    val doubleTapAction: GestureAction = GestureAction.LockScreen,
    val customAliases: Map<String, String> = emptyMap(),
    val searchContacts: List<SearchContact> = emptyList(),
    /** Scales the home screen clock and date/battery line (ClockHeader). */
    val appFontScale: Float = 1.0f,
    /** Scales the home screen app labels (AppListView). */
    val screensFontScale: Float = 1.0f,
    /** Scales the app drawer row labels and icons (AlphabeticalAppDrawer). */
    val appListFontScale: Float = 1.0f,
    val clockPaddingDp: Int = 24,
    val clockPaddingLeftDp: Int = 24,
    val clockPaddingRightDp: Int = 24,
    val clockPaddingTopDp: Int = 24,
    val clockPaddingBottomDp: Int = 12,
    val screensPaddingDp: Int = 32,
    val screensPaddingLeftDp: Int = 32,
    val screensPaddingRightDp: Int = 32,
    val screensPaddingTopDp: Int = 12,
    val screensPaddingBottomDp: Int = 24,
    val nameLengthSortOrder: NameLengthSortOrder = NameLengthSortOrder.OFF,
    val hiddenApps: Set<String> = emptySet(),
    val hiddenFromAppList: Set<String> = emptySet(),
    val edgeScrubPosition: EdgeScrubPosition = EdgeScrubPosition.RIGHT,
    val edgeScrubReleaseAction: EdgeScrubReleaseAction = EdgeScrubReleaseAction.STAY_OPEN,
    val textShadowMode: TextShadowMode = TextShadowMode.AUTO,
    val preferredBrowserPackage: String? = null,
    val webSearchEngine: WebSearchEngine = WebSearchEngine.GOOGLE,
    val commandConfigs: Map<String, CommandConfig> = CommandConfig.DEFAULTS,
    val defaultWeatherCity: String = DEFAULT_WEATHER_CITY,
    val wolframAppId: String = "",
    val autoLaunchSingleMatch: Boolean = false,
    val stockDefaultRange: String = "5d",
    val stockDefaultInterval: String = "1d",
    val stockShowSparkline: Boolean = true
) {
    companion object {
        const val DEFAULT_WEATHER_CITY = "London"
    }
}
