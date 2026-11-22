package com.pallas.launcher.domain.search

import android.content.Context
import com.pallas.launcher.domain.model.AppItem

enum class SmartCommandType {
    ALARM,
    TIMER,
    CALL,
    WEB_SEARCH,
    MATH_STATS,
    LATEX,
    CONVERT,
    FINANCE,
    WEATHER,
    SOLAR,
    WOLFRAM,
    SETTINGS
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
        val hasInfoManual: Boolean = false,
        val execute: (Context) -> Unit
    ) : LauncherSearchResult

    data class RichCard(
        override val id: String,
        val type: SmartCommandType,
        val title: String,
        val primaryValue: String = "",
        val latexSource: String? = null,
        val chart: MathChartData? = null,
        val badgeText: String? = null,
        val isBadgePositive: Boolean? = null,
        val subtitle: String = "",
        val sparkline: List<Float> = emptyList(),
        val isLoading: Boolean = false,
        val hasInfoManual: Boolean = false,
        val consoleOutput: String? = null,
        val execute: (Context) -> Unit = {}
    ) : LauncherSearchResult

    data class ContactCall(
        override val id: String,
        val nickname: String,
        val contactName: String,
        val phoneNumber: String,
        val execute: (Context) -> Unit
    ) : LauncherSearchResult
}

/** Id of the built-in "open Pallas settings" command row. */
const val SETTINGS_COMMAND_ID = "cmd_open_settings"

/**
 * Enter may only trigger rows that DO something. Hint rows ("_hint" ids), loading cards and error/empty cards are
 * informational: Enter skips them and picks the next row (so "w" + Enter launches WhatsApp, not nothing).
 */
val LauncherSearchResult.isEnterEligible: Boolean
    get() = when (this) {
        is LauncherSearchResult.App, is LauncherSearchResult.ContactCall -> true
        is LauncherSearchResult.Command -> !id.endsWith("_hint")
        is LauncherSearchResult.RichCard -> !isLoading && (primaryValue.isNotEmpty() || !latexSource.isNullOrBlank() || chart != null)
    }

/** Tapping these must NOT dismiss search (they have nothing to run). */
val LauncherSearchResult.isInertOnTap: Boolean
    get() = when (this) {
        is LauncherSearchResult.RichCard -> isLoading
        is LauncherSearchResult.Command ->
            id.endsWith("_hint") && (type == SmartCommandType.MATH_STATS ||
                type == SmartCommandType.LATEX ||
                type == SmartCommandType.CONVERT ||
                type == SmartCommandType.FINANCE || type == SmartCommandType.WOLFRAM)
        else -> false
    }

