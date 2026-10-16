package com.pallas.launcher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.pallas.launcher.domain.model.ThemeMode

@Immutable
data class LauncherColors(
    val textPrimary: Color,
    val textSecondary: Color,
    val textDimmed: Color,
    val isDark: Boolean
)

val LocalLauncherColors = staticCompositionLocalOf {
    LauncherColors(
        textPrimary = Color(0xFFF5F2EB),
        textSecondary = Color(0xFFC7C2B8),
        textDimmed = Color(0xFF8E8A82),
        isDark = true
    )
}

/**
 * Callback hook provided by [com.pallas.launcher.MainActivity] to animate window-level background blur
 * on Android 12+ (API 31+) without touching the Window from inside composables.
 */
val LocalWindowBlurController = staticCompositionLocalOf<(Int) -> Unit> { {} }

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE2C48E),
    onPrimary = Color(0xFF3E2D00),
    primaryContainer = Color(0xFF594319),
    onPrimaryContainer = Color(0xFFFFDEA6),
    inversePrimary = Color(0xFF725A2E),
    secondary = Color(0xFFD5C4A8),
    onSecondary = Color(0xFF392F1B),
    secondaryContainer = Color(0xFF504530),
    onSecondaryContainer = Color(0xFFF2E0C3),
    tertiary = Color(0xFFB5CEA8),
    onTertiary = Color(0xFF21361B),
    tertiaryContainer = Color(0xFF374D2F),
    onTertiaryContainer = Color(0xFFD1EAC3),
    background = Color(0xFF121316),
    onBackground = Color(0xFFE5E2E6),
    surface = Color(0xFF121316),
    onSurface = Color(0xFFE5E2E6),
    surfaceVariant = Color(0xFF49463F),
    onSurfaceVariant = Color(0xFFC6C6D0),
    surfaceTint = Color(0xFFE2C48E),
    inverseSurface = Color(0xFFE5E2E6),
    inverseOnSurface = Color(0xFF2F3033),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF958F84),
    outlineVariant = Color(0xFF49463F),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF38393D),
    surfaceDim = Color(0xFF121316),
    surfaceContainerLowest = Color(0xFF0D0E11),
    surfaceContainerLow = Color(0xFF1A1B1F),
    surfaceContainer = Color(0xFF1E1F23),
    surfaceContainerHigh = Color(0xFF282A2E),
    surfaceContainerHighest = Color(0xFF33353A)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF725A2E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDEA6),
    onPrimaryContainer = Color(0xFF271900),
    inversePrimary = Color(0xFFE2C48E),
    secondary = Color(0xFF685D46),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF1E0C4),
    onSecondaryContainer = Color(0xFF221A09),
    tertiary = Color(0xFF4E6545),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD0EBC3),
    onTertiaryContainer = Color(0xFF0C2008),
    background = Color(0xFFFAF9FC),
    onBackground = Color(0xFF1A1B1E),
    surface = Color(0xFFFAF9FC),
    onSurface = Color(0xFF1A1B1E),
    surfaceVariant = Color(0xFFE7E1D7),
    onSurfaceVariant = Color(0xFF45464F),
    surfaceTint = Color(0xFF725A2E),
    inverseSurface = Color(0xFF2F3033),
    inverseOnSurface = Color(0xFFF1F0F4),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF7B766C),
    outlineVariant = Color(0xFFCBC5BB),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFAF9FC),
    surfaceDim = Color(0xFFDAD9DD),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF4F3F7),
    surfaceContainer = Color(0xFFEEEDF1),
    surfaceContainerHigh = Color(0xFFE8E7EC),
    surfaceContainerHighest = Color(0xFFE2E2E6)
)

/**
 * Gradient backdrop for overlays (Drawer & Search), with lowered 0.55–0.65 alpha so wallpaper
 * and optional window blur show through cleanly.
 */
@Composable
fun launcherBackdropBrush(): Brush {
    val cs = MaterialTheme.colorScheme
    return Brush.verticalGradient(
        colors = listOf(
            cs.surface.copy(alpha = 0.55f),
            cs.surface.copy(alpha = 0.60f),
            cs.surface.copy(alpha = 0.65f)
        )
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PallasLauncherTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    onWindowBlurChange: (Int) -> Unit = {},
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val launcherColors = if (isDark) {
        LauncherColors(
            textPrimary = Color(0xFFF5F2EB),
            textSecondary = Color(0xFFC7C2B8),
            textDimmed = Color(0xFF8E8A82),
            isDark = true
        )
    } else {
        LauncherColors(
            textPrimary = Color(0xFF1A1917),
            textSecondary = Color(0xFF4A4742),
            textDimmed = Color(0xFF7A756E),
            isDark = false
        )
    }

    CompositionLocalProvider(
        LocalLauncherColors provides launcherColors,
        LocalHomeContrastState provides LiveHomeContrastHolder.current,
        LocalWindowBlurController provides onWindowBlurChange
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            shapes = ExpressiveShapes,
            typography = Typography,
            content = content
        )
    }
}