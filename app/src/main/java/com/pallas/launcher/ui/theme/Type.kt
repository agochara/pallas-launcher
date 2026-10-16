package com.pallas.launcher.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import com.pallas.launcher.R

val GaramondFontFamily = FontFamily(
    Font(R.font.garamond_regular, FontWeight.Normal),
    Font(R.font.garamond_medium, FontWeight.Medium),
    Font(R.font.garamond_semibold, FontWeight.SemiBold),
    Font(R.font.garamond_bold, FontWeight.Bold)
)

@OptIn(ExperimentalTextApi::class)
val InterFamily = FontFamily(
    Font(
        resId = R.font.inter_variable,
        weight = FontWeight.Normal,
        style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(FontWeight.Normal, FontStyle.Normal)
    ),
    Font(
        resId = R.font.inter_variable,
        weight = FontWeight.Medium,
        style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(FontWeight.Medium, FontStyle.Normal)
    ),
    Font(
        resId = R.font.inter_variable,
        weight = FontWeight.SemiBold,
        style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(FontWeight.SemiBold, FontStyle.Normal)
    ),
    Font(
        resId = R.font.inter_variable,
        weight = FontWeight.Bold,
        style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(FontWeight.Bold, FontStyle.Normal)
    )
)

object LauncherFonts {
    val serif = GaramondFontFamily
    val sans = InterFamily
}

/**
 * Scales [TextStyle.fontSize] and [TextStyle.lineHeight] proportionally by [f].
 */
fun TextStyle.scaled(f: Float): TextStyle = copy(
    fontSize = if (fontSize.isSpecified) fontSize * f else TextUnit.Unspecified,
    lineHeight = if (lineHeight.isSpecified) lineHeight * f else TextUnit.Unspecified
)

/**
 * Named typography tokens for Pallas Launcher.
 * Garamond = the voice (clock, home app labels, Settings display/headline).
 * Inter = the tools (date/battery, gesture hint, drawer rows, rail, search, rich cards, dialogs, modals).
 */
object LauncherTypography {
    val clock = TextStyle(
        fontFamily = GaramondFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 78.sp,
        lineHeight = 82.sp,
        letterSpacing = (-1.5).sp,
        fontFeatureSettings = "lnum, tnum"
    )

    val appLabel = TextStyle(
        fontFamily = GaramondFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 25.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.sp
    )

    val dateAndBattery = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.3.sp,
        fontFeatureSettings = "tnum"
    )

    val gestureHint = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.8.sp
    )

    val drawerRow = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.1).sp
    )

    val railLetter = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.sp
    )

    val searchInput = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    )

    val searchResultTitle = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    )

    val searchResultSubtitle = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp
    )

    val cardValue = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = "tnum"
    )

    val badge = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.1.sp
    )

    val settingsSectionHeader = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.6.sp
    )

    val chip: TextStyle
        get() = Typography.labelLarge

    val modalTitle: TextStyle
        get() = Typography.titleLarge

    val dialogBody: TextStyle
        get() = Typography.bodyMedium
}

/**
 * Material 3 Expressive Typography:
 * - display* and headline* use Garamond (voice / large collapsing top bar titles).
 * - title*, body*, label* use Inter (tools / settings rows, dialogs, modals, chips).
 */
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = GaramondFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = GaramondFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = GaramondFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = GaramondFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = GaramondFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = GaramondFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodySmall = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    ),
    labelLarge = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    ),
    labelSmall = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    )
)