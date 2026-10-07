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

enum class NameLengthSortOrder {
    OFF,
    ASCENDING,
    DESCENDING
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
    val customAliases: Map<String, String> = emptyMap(), // alias -> package or uniqueKey
    val searchContacts: List<SearchContact> = emptyList(),
    val appFontScale: Float = 1.0f,
    val screensFontScale: Float = 1.0f,
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
    val edgeScrubReleaseAction: EdgeScrubReleaseAction = EdgeScrubReleaseAction.STAY_OPEN
)
