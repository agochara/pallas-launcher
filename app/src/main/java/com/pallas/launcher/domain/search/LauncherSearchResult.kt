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
    WIKI,
    WORLD_TIME,
    DATE_ARITHMETIC,
    DEFINE,
    NOTE,
    QR,
    COLOR_BASE,
    EMI,
    FOCUS,
    FILE,
    CALENDAR,
    MACRO,
    HARDWARE,
    APP_SEARCH,
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
        val bodyText: String? = null,
        val copyText: String? = null,
        val rawInput: String? = null,
        val qrMatrix: QrMatrix? = null,
        val colorSwatchArgb: Int? = null,
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

/** Id of the built-in "open Quick Capture inbox" command row. */
const val QUICK_CAPTURE_INBOX_ID = "cmd_note_inbox"

const val FOCUS_ON_COMMAND_ID = "cmd_focus_on"
const val FOCUS_OFF_COMMAND_ID = "cmd_focus_off"
const val FOCUS_SETUP_COMMAND_ID = "cmd_focus_setup"
const val CALENDAR_GRANT_PERMISSION_ID = "cmd_cal_grant_perm"
const val FILES_GRANT_PERMISSION_ID = "cmd_files_grant_perm"
const val MACRO_COMMAND_PREFIX = "cmd_macro_"
const val PALLAS_NAV_PREFIX = "cmd_pallas_nav:"

/**
 * Enter may only trigger rows that DO something. Hint rows ("_hint" ids), loading cards and error/empty cards are
 * informational: Enter skips them and picks the next row (so "w" + Enter launches WhatsApp, not nothing).
 */
val LauncherSearchResult.isEnterEligible: Boolean
    get() = when (this) {
        is LauncherSearchResult.App, is LauncherSearchResult.ContactCall -> true
        is LauncherSearchResult.Command -> !id.endsWith("_hint")
        is LauncherSearchResult.RichCard -> !isLoading && (
            primaryValue.isNotEmpty() ||
                !latexSource.isNullOrBlank() ||
                chart != null ||
                !bodyText.isNullOrBlank() ||
                qrMatrix != null ||
                colorSwatchArgb != null ||
                id.startsWith("wiki_err_") ||
                id.startsWith("def_err_") ||
                id.startsWith("wa_err_") ||
                id.startsWith("fin_err_") ||
                id.startsWith("wx_err_") ||
                id.startsWith("solar_err_")
            )
    }

/** Tapping these must NOT dismiss search (they have nothing to run). */
val LauncherSearchResult.isInertOnTap: Boolean
    get() = when (this) {
        is LauncherSearchResult.RichCard -> isLoading
        is LauncherSearchResult.Command ->
            id.endsWith("_hint") && (type == SmartCommandType.MATH_STATS ||
                type == SmartCommandType.LATEX ||
                type == SmartCommandType.CONVERT ||
                type == SmartCommandType.FINANCE ||
                type == SmartCommandType.WOLFRAM ||
                type == SmartCommandType.WIKI ||
                type == SmartCommandType.WORLD_TIME ||
                type == SmartCommandType.DEFINE ||
                type == SmartCommandType.QR ||
                type == SmartCommandType.EMI ||
                type == SmartCommandType.FILE)
        else -> false
    }

