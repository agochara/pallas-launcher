package com.pallas.launcher.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import com.pallas.launcher.domain.model.ThemeMode

/**
 * Shared tinted backdrop for the App Drawer and Search: transparent at the very top (wallpaper stays visible),
 * increasingly tinted towards the bottom - same idea as Niagara.
 */
@Composable
fun launcherBackdropBrush(): Brush {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = LocalLauncherColors.current.isDark
    val bottomColor = colorScheme.surfaceContainer.copy(alpha = if (isDark) 0.80f else 0.62f)
    val midColor = colorScheme.surface.copy(alpha = if (isDark) 0.40f else 0.25f)
    return Brush.verticalGradient(
        0.0f to Color.Transparent,
        0.28f to midColor,
        1.0f to bottomColor
    )
}

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
    shadow = Shadow(Color.Black.copy(alpha = 0.5f), Offset(0f, 2f), 6f)
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
    shadow = Shadow(Color.White.copy(alpha = 0.65f), Offset(0f, 1f), 4f)
)

val LocalLauncherColors = staticCompositionLocalOf { DarkLauncherColors }

@Composable
private fun resolveDark(themeMode: ThemeMode): Boolean = when (themeMode) {
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
}

/** dynamicXxxColorScheme() crashes below Android 12 - fall back to a static scheme there. */
@Composable
private fun launcherColorScheme(isDark: Boolean): ColorScheme {
    val context = LocalContext.current
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        isDark -> darkColorScheme(primary = AccentGold, secondary = AccentBlue, error = AccentRed)
        else -> lightColorScheme(primary = AccentGoldDark, secondary = AccentBlue, error = AccentRed)
    }
}

/** Root theme: dynamic M3 colors + scoped colors for the Olauncher-style home. */
@Composable
fun PallasLauncherTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val isDark = resolveDark(themeMode)
    val colors = if (isDark) DarkLauncherColors else LightLauncherColors

    CompositionLocalProvider(LocalLauncherColors provides colors) {
        MaterialTheme(
            colorScheme = launcherColorScheme(isDark),
            shapes = ExpressiveShapes,
            typography = Typography,
            content = content
        )
    }
}

/**
 * REAL Material 3 Expressive theme for Settings / sheets / dialogs: expressive motion scheme (springy,
 * shape-morphing components) instead of the standard easing curves. Needs material3 >= 1.4.0.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MaterialExpressiveSettingsTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    MaterialExpressiveTheme(
        colorScheme = launcherColorScheme(resolveDark(themeMode)),
        motionScheme = MotionScheme.expressive(),
        shapes = ExpressiveShapes,
        typography = Typography,
        content = content
    )
}