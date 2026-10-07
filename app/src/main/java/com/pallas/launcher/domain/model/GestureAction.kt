package com.pallas.launcher.domain.model

sealed interface GestureAction {
    data object OpenSearch : GestureAction
    data object OpenAppDrawer : GestureAction          // NEW: the Niagara-style apps screen
    data object OpenNotifications : GestureAction
    data object OpenQuickSettings : GestureAction
    data object OpenCamera : GestureAction
    data object OpenDialer : GestureAction
    data class LaunchApp(val packageName: String) : GestureAction
    data object None : GestureAction

    companion object {
        fun fromString(value: String): GestureAction = when {
            value == "search" -> OpenSearch
            value == "drawer" -> OpenAppDrawer
            value == "notifications" -> OpenNotifications
            value == "quick_settings" -> OpenQuickSettings
            value == "camera" -> OpenCamera
            value == "dialer" -> OpenDialer
            value.startsWith("app:") -> LaunchApp(value.removePrefix("app:"))
            else -> None
        }

        fun toString(action: GestureAction): String = when (action) {
            is OpenSearch -> "search"
            is OpenAppDrawer -> "drawer"
            is OpenNotifications -> "notifications"
            is OpenQuickSettings -> "quick_settings"
            is OpenCamera -> "camera"
            is OpenDialer -> "dialer"
            is LaunchApp -> "app:${action.packageName}"
            is None -> "none"
        }
    }
}