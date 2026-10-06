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
    val swipeUpAction: GestureAction = GestureAction.OpenSearch,
    val swipeDownAction: GestureAction = GestureAction.OpenNotifications,
    val customAliases: Map<String, String> = emptyMap(), // alias -> package or uniqueKey
    val appFontScale: Float = 1.0f,
    val hiddenApps: Set<String> = emptySet(),
    val edgeScrubPosition: EdgeScrubPosition = EdgeScrubPosition.RIGHT,
    val edgeScrubReleaseAction: EdgeScrubReleaseAction = EdgeScrubReleaseAction.STAY_OPEN
)
