package com.pallas.launcher.domain.model

import android.os.UserHandle

data class AppItem(
    val packageName: String,
    val activityName: String,
    val label: String,
    val userHandle: UserHandle? = null,
    val userSerialNumber: Long = 0L,
    val installTime: Long = 0L
) {
    val uniqueKey: String
        get() = "$packageName/$activityName#$userSerialNumber"
}

data class ScreenApp(
    val uniqueKey: String,
    val packageName: String,
    val activityName: String,
    val customLabel: String? = null,
    val defaultLabel: String = "",
    val userSerialNumber: Long = 0L,
    val userHandle: UserHandle? = null
) {
    val displayLabel: String
        get() = customLabel?.takeIf { it.isNotBlank() } ?: defaultLabel

    val isProfileRemoved: Boolean
        get() = userSerialNumber != 0L && userHandle == null
}
