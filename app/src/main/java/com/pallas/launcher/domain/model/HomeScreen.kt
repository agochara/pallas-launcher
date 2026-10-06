package com.pallas.launcher.domain.model

data class HomeScreen(
    val id: String,
    val name: String,
    val contextTag: String? = null,
    val apps: List<ScreenApp> = emptyList()
)
