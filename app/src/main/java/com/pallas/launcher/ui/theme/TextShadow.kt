package com.pallas.launcher.ui.theme

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import com.pallas.launcher.domain.model.AppsHorizontalAlignment
import com.pallas.launcher.domain.model.TextShadowMode
import kotlin.math.abs

/**
 * Wallpaper-aware contrast state for home screen typography without relying on ugly drop shadows.
 *
 * Modes (mapped onto [TextShadowMode] for full backward compatibility with settings & backups):
 *  - `OFF`: No shadow, no local scrim, no color shift.
 *  - `AUTO`: Samples [WallpaperColors] (zero permissions required). When the wallpaper luminance clashes with the
 *            active theme's text color, it applies a proportional soft radial/horizontal local scrim behind the text
 *            column (`0f..0.38f` alpha) and nudges low-contrast text toward high-contrast ink.
 *  - `ON` (Scrim): Always applies a soft local gradient scrim behind the home text block (no drop shadow).
 */
@Immutable
data class HomeContrastState(
    val mode: TextShadowMode = TextShadowMode.AUTO,
    val wallpaperLuminance: Float = 0.15f,
    val supportsDarkTextHint: Boolean = false,
    val clashFactor: Float = 0f,
    val localScrimAlpha: Float = 0f,
    val scrimColor: Color = Color.Black
) {
    fun resolvePrimaryText(defaultColor: Color): Color {
        if (mode != TextShadowMode.AUTO || clashFactor < 0.55f) return defaultColor
        // If user is in a light-text theme on a blindingly bright wallpaper (or vice versa), crisp up the ink
        return if (supportsDarkTextHint && defaultColor.luminance() > 0.7f) {
            Color(0xFFFDFBF7)
        } else {
            defaultColor
        }
    }

    fun resolveSecondaryText(defaultColor: Color): Color {
        if (mode == TextShadowMode.OFF) return defaultColor
        return if (clashFactor > 0.35f) defaultColor.copy(alpha = 0.92f) else defaultColor
    }
}

internal object LiveHomeContrastHolder {
    var current by mutableStateOf(HomeContrastState())
}

val LocalHomeContrastState = compositionLocalOf { HomeContrastState() }

/**
 * Samples system [WallpaperColors] without any storage permissions and listens for live wallpaper changes.
 */
@Composable
fun rememberWallpaperLuminance(): Pair<Float, Boolean> {
    val context = LocalContext.current.applicationContext
    var lum by remember { mutableFloatStateOf(0.15f) }
    var darkHint by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val wm = try {
            WallpaperManager.getInstance(context)
        } catch (_: Throwable) {
            null
        }

        fun updateFrom(colors: WallpaperColors?) {
            if (colors == null) return
            val primary = colors.primaryColor
            val secondary = colors.secondaryColor
            val pLum = Color(primary.toArgb()).luminance()
            val sLum = secondary?.let { Color(it.toArgb()).luminance() } ?: pLum
            lum = (pLum * 0.7f + sLum * 0.3f).coerceIn(0f, 1f)
            darkHint = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (colors.colorHints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT) != 0
            } else {
                lum > 0.62f
            }
        }

        if (wm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            runCatching { updateFrom(wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)) }
            val listener = WallpaperManager.OnColorsChangedListener { colors, which ->
                if ((which and WallpaperManager.FLAG_SYSTEM) != 0) {
                    updateFrom(colors)
                }
            }
            runCatching { wm.addOnColorsChangedListener(listener, Handler(Looper.getMainLooper())) }
            onDispose {
                runCatching { wm.removeOnColorsChangedListener(listener) }
            }
        } else {
            onDispose { }
        }
    }

    return lum to darkHint
}

/**
 * Replaces the old drop-shadow resolver with wallpaper-aware contrast.
 * Returns `null` so `ClockHeader` and `AppListView` never draw a dirty drop shadow, while updating the shared
 * [HomeContrastState] used by `AppListView` for proportional local scrims.
 */
@Composable
fun resolveHomeTextShadow(
    mode: TextShadowMode,
    showWallpaperScrim: Boolean,
    legacyShadowFallback: Boolean = false
): Shadow? {
    val colors = LocalLauncherColors.current
    val (wallLum, darkTextHint) = rememberWallpaperLuminance()
    val textLum = colors.textPrimary.luminance()

    // Clash is high when text luminance and wallpaper luminance are too close (e.g. white text on bright wallpaper)
    val lumDiff = abs(textLum - wallLum)
    val rawClash = ((0.65f - lumDiff) / 0.65f).coerceIn(0f, 1f)
    val effectiveClash = if (showWallpaperScrim) (rawClash * 0.4f) else rawClash

    val localScrimAlpha = when (mode) {
        TextShadowMode.OFF -> 0f
        TextShadowMode.AUTO -> if (effectiveClash > 0.18f) (effectiveClash * 0.42f).coerceIn(0.10f, 0.38f) else 0f
        TextShadowMode.ON -> if (showWallpaperScrim) 0.24f else 0.36f
    }

    val scrimColor = if (textLum > 0.5f) Color.Black else Color.White

    val state = HomeContrastState(
        mode = mode,
        wallpaperLuminance = wallLum,
        supportsDarkTextHint = darkTextHint,
        clashFactor = effectiveClash,
        localScrimAlpha = localScrimAlpha,
        scrimColor = scrimColor
    )

    SideEffect {
        LiveHomeContrastHolder.current = state
    }

    if (legacyShadowFallback && mode == TextShadowMode.ON) {
        return Shadow(
            color = scrimColor.copy(alpha = 0.45f),
            offset = Offset(0f, 2f),
            blurRadius = 6f
        )
    }
    return null
}

/**
 * Draws a soft, feathered local gradient behind the home-screen app labels proportional to the measured wallpaper clash.
 */
fun Modifier.homeLocalContrastScrim(
    alignment: AppsHorizontalAlignment,
    scrimColor: Color,
    scrimAlpha: Float
): Modifier {
    if (scrimAlpha <= 0.01f) return this
    return this.drawBehind {
        val peak = scrimColor.copy(alpha = scrimAlpha)
        val mid = scrimColor.copy(alpha = scrimAlpha * 0.45f)
        val clear = Color.Transparent
        val brush = when (alignment) {
            AppsHorizontalAlignment.RIGHT -> Brush.horizontalGradient(
                0.0f to clear,
                0.35f to clear,
                0.70f to mid,
                1.0f to peak
            )
            AppsHorizontalAlignment.LEFT -> Brush.horizontalGradient(
                0.0f to peak,
                0.30f to mid,
                0.65f to clear,
                1.0f to clear
            )
            AppsHorizontalAlignment.CENTER -> Brush.radialGradient(
                colors = listOf(peak, mid, clear),
                center = Offset(size.width / 2f, size.height / 2f),
                radius = maxOf(size.width, size.height) * 0.65f
            )
        }
        drawRect(brush = brush)
    }
}