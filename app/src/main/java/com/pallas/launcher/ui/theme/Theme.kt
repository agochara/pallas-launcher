package com.pallas.launcher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import com.pallas.launcher.domain.model.ThemeMode

/**
 * Scoped colors specifically for high-contrast home screen elements
 * (Clock, date, battery, home app list) rendered over arbitrary user wallpapers.
 */
data class HomeLauncherColors(
    val isDark: Boolean,
    val textPrimary: Color,
    val textSecondary: Color,
    val textDimmed: Color,
    val surface: Color,
    val cardBackground: Color,
    val cardBackgroundLight: Color,
    val accent: Color,
    val shadow: Shadow
)

// Legacy alias kept for backward compatibility with existing home callers
typealias LauncherColors = HomeLauncherColors

val DarkLauncherColors = HomeLauncherColors(
    isDark = true,
    textPrimary = TextWhite,
    textSecondary = TextMuted,
    textDimmed = TextDimmed,
    surface = Color(0xFF121212),
    cardBackground = CardBackground,
    cardBackgroundLight = CardBackgroundLight,
    accent = AccentGold,
    shadow = Shadow(
        color = Color.Black.copy(alpha = 0.5f),
        offset = Offset(0f, 2f),
        blurRadius = 6f
    )
)

val LightLauncherColors = HomeLauncherColors(
    isDark = false,
    textPrimary = TextDarkPrimary,
    textSecondary = TextDarkSecondary,
    textDimmed = TextDarkDimmed,
    surface = Color(0xFFF2F2F7),
    cardBackground = CardBackgroundLightMode,
    cardBackgroundLight = CardBackgroundLightModeSubtle,
    accent = AccentGoldDark,
    shadow = Shadow(
        color = Color.White.copy(alpha = 0.65f),
        offset = Offset(0f, 1f),
        blurRadius = 4f
    )
)

val LocalLauncherColors = staticCompositionLocalOf { DarkLauncherColors }

/**
 * Root Launcher theme. Supplies dynamic M3 colors and scoped home wallpaper colors.
 */
@Composable
fun PallasLauncherTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colors = if (isDark) DarkLauncherColors else LightLauncherColors
    val context = LocalContext.current
    val colorScheme = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

    CompositionLocalProvider(LocalLauncherColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = ExpressiveShapes,
            typography = Typography,
            content = content
        )
    }
}

/**
 * Material 3 Expressive theme for Settings, secondary dialogs, and picker sheets.
 * Incorporates dynamic colors, full Typography, and ExpressiveShapes.
 */
@Composable
fun MaterialExpressiveSettingsTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val context = LocalContext.current
    val colorScheme = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = ExpressiveShapes,
        typography = Typography,
        content = content
    )
}
