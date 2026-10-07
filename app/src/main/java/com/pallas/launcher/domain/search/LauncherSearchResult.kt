package com.pallas.launcher.domain.search

import android.content.Context
import com.pallas.launcher.domain.model.AppItem

enum class SmartCommandType {
    ALARM,
    TIMER,
    CALL
}

sealed interface LauncherSearchResult {
    val id: String

    data class App(
        val result: AppSearchResult
    ) : LauncherSearchResult {
        override val id: String get() = result.id
        val app: AppItem get() = result.app
        val score: Double get() = result.score
        val matchedRanges: List<IntRange> get() = result.matchedRanges
    }

    data class Command(
        override val id: String,
        val title: String,
        val subtitle: String,
        val type: SmartCommandType,
        val execute: (Context) -> Unit
    ) : LauncherSearchResult

    data class ContactCall(
        override val id: String,
        val nickname: String,
        val contactName: String,
        val phoneNumber: String,
        val execute: (Context) -> Unit
    ) : LauncherSearchResult
}
