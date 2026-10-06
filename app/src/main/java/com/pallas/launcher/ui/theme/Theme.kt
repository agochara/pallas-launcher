package com.pallas.launcher.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import com.pallas.launcher.domain.model.ThemeMode

data class LauncherColors(
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

val DarkLauncherColors = LauncherColors(
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

val LightLauncherColors = LauncherColors(
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

private val DarkColorScheme = darkColorScheme(
    primary = AccentGold,
    onPrimary = Color(0xFF1E1E1E),
    primaryContainer = Color(0xFF3E3524),
    onPrimaryContainer = AccentGold,
    secondary = TextMuted,
    onSecondary = Color.Black,
    tertiary = AccentGold,
    background = Color(0xFF121212),
    surface = Color(0xFF161616),
    surfaceContainer = Color(0xFF1E1E1E),
    surfaceContainerLow = Color(0xFF1A1A1A),
    surfaceContainerHigh = Color(0xFF262626),
    surfaceContainerHighest = Color(0xFF303030),
    onBackground = TextWhite,
    onSurface = TextWhite,
    onSurfaceVariant = TextMuted
)

private val LightColorScheme = lightColorScheme(
    primary = AccentGoldDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF3E7C4),
    onPrimaryContainer = Color(0xFF4C3600),
    secondary = TextDarkSecondary,
    onSecondary = Color.White,
    tertiary = AccentGoldDark,
    background = Color(0xFFF2F2F7),
    surface = Color(0xFFFBFBFD),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F7FA),
    surfaceContainerHigh = Color(0xFFEBEBF0),
    surfaceContainerHighest = Color(0xFFE2E2E8),
    onBackground = TextDarkPrimary,
    onSurface = TextDarkPrimary,
    onSurfaceVariant = TextDarkDimmed
)

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
    
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (isDark) DarkColorScheme else LightColorScheme
    }

    CompositionLocalProvider(LocalLauncherColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
