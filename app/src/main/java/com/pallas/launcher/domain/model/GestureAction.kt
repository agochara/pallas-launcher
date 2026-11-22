package com.pallas.launcher.domain.model

sealed interface GestureAction {
    data object OpenSearch : GestureAction
    data object OpenAppDrawer : GestureAction          // NEW: the Niagara-style apps screen
    data object OpenNotifications : GestureAction
    data object OpenQuickSettings : GestureAction
    data object OpenCamera : GestureAction
    data object OpenDialer : GestureAction
    data object LockScreen : GestureAction
    data class LaunchApp(val uniqueKey: String) : GestureAction {
        val packageName: String
            get() = uniqueKey.substringBefore('/')
    }
    data object None : GestureAction

    companion object {
        fun fromStringOrNull(value: String): GestureAction? {
            val clean = value.trim()
            return when {
                clean == "search" -> OpenSearch
                clean == "drawer" -> OpenAppDrawer
                clean == "notifications" -> OpenNotifications
                clean == "quick_settings" -> OpenQuickSettings
                clean == "camera" -> OpenCamera
                clean == "dialer" -> OpenDialer
                clean == "lock_screen" || clean == "lock" -> LockScreen
                clean == "none" -> None
                clean.startsWith("app:") -> {
                    val key = clean.removePrefix("app:").trim()
                    if (key.isEmpty()) null else LaunchApp(key)
                }
                else -> null
            }
        }

        fun fromString(value: String): GestureAction = fromStringOrNull(value) ?: None

        fun toString(action: GestureAction): String = when (action) {
            is OpenSearch -> "search"
            is OpenAppDrawer -> "drawer"
            is OpenNotifications -> "notifications"
            is OpenQuickSettings -> "quick_settings"
            is OpenCamera -> "camera"
            is OpenDialer -> "dialer"
            is LockScreen -> "lock_screen"
            is LaunchApp -> "app:${action.uniqueKey}"
            is None -> "none"
        }
    }
}