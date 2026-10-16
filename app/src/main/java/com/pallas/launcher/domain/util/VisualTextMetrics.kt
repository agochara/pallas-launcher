package com.pallas.launcher.domain.util

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.util.LruCache
import androidx.core.content.res.ResourcesCompat
import com.pallas.launcher.R

/**
 * Measures how much horizontal room a label really takes on the home screen: same typeface (Garamond Medium), same
 * letter-spacing ratio as `LauncherTypography.appLabel`. The previous implementation measured with the platform's
 * default sans-serif, which ranks labels differently from how they actually look (Garamond is much narrower).
 *
 * Only RELATIVE widths matter for sorting, so we measure at a fixed text size and ignore the user's font scale.
 */
object VisualTextMetrics {
    private const val MEASURE_SIZE = 100f
    private const val LETTER_SPACING_EM = 0f // 0sp tracking on a 25sp label

    @Volatile private var paint: Paint? = null
    private val cache = LruCache<String, Float>(2048)

    /** Call once with any context. Safe to call repeatedly. */
    fun init(context: Context) {
        if (paint != null) return
        synchronized(this) {
            if (paint != null) return
            val tf = try {
                ResourcesCompat.getFont(context.applicationContext, R.font.garamond_medium)
            } catch (_: Throwable) {
                null
            } ?: Typeface.DEFAULT
            paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = tf
                textSize = MEASURE_SIZE
                letterSpacing = LETTER_SPACING_EM
            }
        }
    }

    /** Width in arbitrary-but-consistent units; falls back to a per-character estimate if no Paint is available. */
    fun measure(text: String): Float {
        if (text.isEmpty()) return 0f
        cache.get(text)?.let { return it }
        val p = paint
        val width = try {
            val measured = p?.measureText(text) ?: 0f
            if (measured > 0f) measured else estimate(text)
        } catch (_: Throwable) {
            estimate(text)
        }
        cache.put(text, width)
        return width
    }

    private fun estimate(text: String): Float {
        var w = 0f
        for (ch in text) {
            w += when (ch) {
                'W', 'M', 'Q', '—', '…' -> 135f
                'w', 'm' -> 115f
                'i', 'l', 'j', '!', '|', ':', ';', '\'', '`', '.', ',' -> 35f
                'f', 'r', 't', 'I', ' ', '-', '(', ')' -> 55f
                in 'A'..'Z' -> 95f
                in '0'..'9' -> 70f
                else -> 70f
            }
        }
        return w
    }
}
