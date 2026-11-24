package com.pallas.launcher.domain.search

import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

data class ColorBaseResult(
    val title: String,
    val primaryValue: String,
    val badge: String,
    val subtitle: String,
    val copyText: String,
    val colorSwatchArgb: Int? = null
)

/**
 * Pure-Kotlin Color Swatch & Radix/Base Inspector:
 * - Hex / RGB / HSL color parsing (`#D4AF37`, `#fff`, `rgb(212, 175, 55)`, `hsl(46, 65%, 52%)`)
 * - Base conversion (`hex 255`, `bin 42`, `oct 255`, `dec 0xff`, `0xFF`, `0b101010`, `0o377`)
 */
object ColorAndBaseInspector {

    private val HEX_COLOR_REGEX = Regex("""^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$""")
    private val RGB_COLOR_REGEX = Regex(
        """^rgba?\(\s*(\d{1,3})\s*,\s*(\d{1,3})\s*,\s*(\d{1,3})(?:\s*,\s*(0?\.\d+|1(?:\.0+)?|0|1))?\s*\)$""",
        RegexOption.IGNORE_CASE
    )
    private val HSL_COLOR_REGEX = Regex(
        """^hsla?\(\s*(-?\d+(?:\.\d+)?)\s*,\s*(\d{1,3}(?:\.\d+)?)%?\s*,\s*(\d{1,3}(?:\.\d+)?)%?(?:\s*,\s*(0?\.\d+|1(?:\.0+)?|0|1))?\s*\)$""",
        RegexOption.IGNORE_CASE
    )

    private val BASE_COMMAND_REGEX = Regex("""^(hex|bin|oct|dec)\s+(.+)$""", RegexOption.IGNORE_CASE)
    private val LITERAL_HEX_REGEX = Regex("""^(-?)0x([0-9a-fA-F]+)$""", RegexOption.IGNORE_CASE)
    private val LITERAL_BIN_REGEX = Regex("""^(-?)0b([01]+)$""", RegexOption.IGNORE_CASE)
    private val LITERAL_OCT_REGEX = Regex("""^(-?)0o([0-7]+)$""", RegexOption.IGNORE_CASE)

    fun isHexColor(raw: String): Boolean = HEX_COLOR_REGEX.matches(raw.trim())

    fun evaluate(rawQuery: String): ColorBaseResult? {
        val clean = rawQuery.trim()
        if (clean.isEmpty()) return null

        evaluateColor(clean)?.let { return it }
        evaluateBase(clean)?.let { return it }
        return null
    }

    private fun evaluateColor(clean: String): ColorBaseResult? {
        HEX_COLOR_REGEX.matchEntire(clean)?.let { m ->
            val digits = m.groupValues[1].uppercase(Locale.ROOT)
            val (a, r, g, b) = when (digits.length) {
                3 -> {
                    val rr = "${digits[0]}${digits[0]}".toInt(16)
                    val gg = "${digits[1]}${digits[1]}".toInt(16)
                    val bb = "${digits[2]}${digits[2]}".toInt(16)
                    listOf(255, rr, gg, bb)
                }
                6 -> {
                    val rr = digits.substring(0, 2).toInt(16)
                    val gg = digits.substring(2, 4).toInt(16)
                    val bb = digits.substring(4, 6).toInt(16)
                    listOf(255, rr, gg, bb)
                }
                else -> {
                    val aa = digits.substring(0, 2).toInt(16)
                    val rr = digits.substring(2, 4).toInt(16)
                    val gg = digits.substring(4, 6).toInt(16)
                    val bb = digits.substring(6, 8).toInt(16)
                    listOf(aa, rr, gg, bb)
                }
            }
            return buildColorOutcome(a, r, g, b)
        }

        RGB_COLOR_REGEX.matchEntire(clean)?.let { m ->
            val r = m.groupValues[1].toIntOrNull() ?: return null
            val g = m.groupValues[2].toIntOrNull() ?: return null
            val b = m.groupValues[3].toIntOrNull() ?: return null
            if (r !in 0..255 || g !in 0..255 || b !in 0..255) return null
            val alphaFloat = m.groupValues[4].takeIf { it.isNotEmpty() }?.toDoubleOrNull() ?: 1.0
            val a = (alphaFloat.coerceIn(0.0, 1.0) * 255.0).roundToInt()
            return buildColorOutcome(a, r, g, b)
        }

        HSL_COLOR_REGEX.matchEntire(clean)?.let { m ->
            val hRaw = m.groupValues[1].toDoubleOrNull() ?: return null
            val sPct = m.groupValues[2].toDoubleOrNull() ?: return null
            val lPct = m.groupValues[3].toDoubleOrNull() ?: return null
            if (sPct !in 0.0..100.0 || lPct !in 0.0..100.0) return null
            val alphaFloat = m.groupValues[4].takeIf { it.isNotEmpty() }?.toDoubleOrNull() ?: 1.0
            val a = (alphaFloat.coerceIn(0.0, 1.0) * 255.0).roundToInt()
            val (r, g, b) = hslToRgb(hRaw, sPct, lPct)
            return buildColorOutcome(a, r, g, b)
        }

        return null
    }

    private fun buildColorOutcome(a: Int, r: Int, g: Int, b: Int): ColorBaseResult {
        val hexStr = if (a < 255) {
            String.format(Locale.US, "#%02X%02X%02X%02X", a, r, g, b)
        } else {
            String.format(Locale.US, "#%02X%02X%02X", r, g, b)
        }
        val rgbStr = if (a < 255) {
            String.format(Locale.US, "rgba(%d, %d, %d, %.2f)", r, g, b, a / 255.0)
        } else {
            "rgb($r, $g, $b)"
        }
        val (h, s, l) = rgbToHsl(r, g, b)
        val hslStr = "hsl($h°, $s%, $l%)"

        val lum = relativeLuminance(r, g, b)
        val contrastWhite = (1.0 + 0.05) / (lum + 0.05)
        val contrastDark = (lum + 0.05) / (0.0 + 0.05)
        val argb = (a shl 24) or (r shl 16) or (g shl 8) or b

        return ColorBaseResult(
            title = "Color • $hexStr",
            primaryValue = "$hexStr • $rgbStr",
            badge = "Color",
            subtitle = String.format(
                Locale.US,
                "%s • Contrast %.1f:1 vs Dark / %.1f:1 vs White",
                hslStr, contrastDark, contrastWhite
            ),
            copyText = hexStr,
            colorSwatchArgb = argb
        )
    }

    private fun rgbToHsl(r: Int, g: Int, b: Int): Triple<Int, Int, Int> {
        val rf = r / 255.0
        val gf = g / 255.0
        val bf = b / 255.0
        val max = maxOf(rf, gf, bf)
        val min = minOf(rf, gf, bf)
        val l = (max + min) / 2.0
        val d = max - min
        if (d < 1e-9) {
            return Triple(0, 0, (l * 100.0).roundToInt())
        }
        val s = if (l > 0.5) d / (2.0 - max - min) else d / (max + min)
        val rawH = when (max) {
            rf -> (gf - bf) / d + (if (gf < bf) 6.0 else 0.0)
            gf -> (bf - rf) / d + 2.0
            else -> (rf - gf) / d + 4.0
        }
        val h = ((rawH / 6.0) * 360.0).roundToInt() % 360
        return Triple(h, (s * 100.0).roundToInt(), (l * 100.0).roundToInt())
    }

    private fun hslToRgb(hDeg: Double, sPct: Double, lPct: Double): Triple<Int, Int, Int> {
        val h = ((hDeg % 360.0) + 360.0) % 360.0 / 360.0
        val s = (sPct / 100.0).coerceIn(0.0, 1.0)
        val l = (lPct / 100.0).coerceIn(0.0, 1.0)
        if (s == 0.0) {
            val v = (l * 255.0).roundToInt().coerceIn(0, 255)
            return Triple(v, v, v)
        }
        val q = if (l < 0.5) l * (1.0 + s) else l + s - l * s
        val p = 2.0 * l - q
        fun hueToChannel(tIn: Double): Int {
            var t = tIn
            if (t < 0.0) t += 1.0
            if (t > 1.0) t -= 1.0
            val v = when {
                t < 1.0 / 6.0 -> p + (q - p) * 6.0 * t
                t < 0.5 -> q
                t < 2.0 / 3.0 -> p + (q - p) * (2.0 / 3.0 - t) * 6.0
                else -> p
            }
            return (v * 255.0).roundToInt().coerceIn(0, 255)
        }
        return Triple(
            hueToChannel(h + 1.0 / 3.0),
            hueToChannel(h),
            hueToChannel(h - 1.0 / 3.0)
        )
    }

    private fun relativeLuminance(r: Int, g: Int, b: Int): Double {
        fun lin(c: Int): Double {
            val v = c / 255.0
            return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * lin(r) + 0.7152 * lin(g) + 0.0722 * lin(b)
    }

    private fun evaluateBase(clean: String): ColorBaseResult? {
        BASE_COMMAND_REGEX.matchEntire(clean)?.let { m ->
            val targetBase = m.groupValues[1].lowercase(Locale.ROOT)
            val arg = m.groupValues[2].trim()
            val value = parsePrefixedOrDecimalLong(arg, allowBareHex = targetBase == "dec") ?: return null
            return formatBaseResult(value, targetBase)
        }

        // Bare prefixed literals like "0xD4AF", "0b101010", "0o377"
        LITERAL_HEX_REGEX.matchEntire(clean)?.let {
            val v = parsePrefixedOrDecimalLong(clean, allowBareHex = false) ?: return null
            return formatBaseResult(v, "dec")
        }
        LITERAL_BIN_REGEX.matchEntire(clean)?.let {
            val v = parsePrefixedOrDecimalLong(clean, allowBareHex = false) ?: return null
            return formatBaseResult(v, "dec")
        }
        LITERAL_OCT_REGEX.matchEntire(clean)?.let {
            val v = parsePrefixedOrDecimalLong(clean, allowBareHex = false) ?: return null
            return formatBaseResult(v, "dec")
        }

        return null
    }

    private fun parsePrefixedOrDecimalLong(raw: String, allowBareHex: Boolean): Long? {
        val clean = raw.trim().replace("_", "")
        if (clean.isEmpty()) return null

        LITERAL_HEX_REGEX.matchEntire(clean)?.let { m ->
            val neg = m.groupValues[1] == "-"
            val mag = m.groupValues[2].toLongOrNull(16) ?: return null
            return if (neg) -mag else mag
        }
        LITERAL_BIN_REGEX.matchEntire(clean)?.let { m ->
            val neg = m.groupValues[1] == "-"
            val mag = m.groupValues[2].toLongOrNull(2) ?: return null
            return if (neg) -mag else mag
        }
        LITERAL_OCT_REGEX.matchEntire(clean)?.let { m ->
            val neg = m.groupValues[1] == "-"
            val mag = m.groupValues[2].toLongOrNull(8) ?: return null
            return if (neg) -mag else mag
        }

        if (allowBareHex && clean.matches(Regex("""^-?[0-9a-fA-F]+$""")) && clean.any { it in 'a'..'f' || it in 'A'..'F' }) {
            return clean.toLongOrNull(16)
        }

        return clean.toLongOrNull(10)
    }

    private fun formatBaseResult(value: Long, target: String): ColorBaseResult {
        val negPrefix = if (value < 0) "-" else ""
        val mag = if (value == Long.MIN_VALUE) return ColorBaseResult(
            title = "Base Converter",
            primaryValue = Long.MIN_VALUE.toString(),
            badge = "Base",
            subtitle = "64-bit signed minimum",
            copyText = Long.MIN_VALUE.toString()
        ) else abs(value)

        val hexStr = "${negPrefix}0x${mag.toString(16).uppercase(Locale.ROOT)}"
        val binStr = "${negPrefix}0b${mag.toString(2)}"
        val octStr = "${negPrefix}0o${mag.toString(8)}"
        val decStr = value.toString()

        val (primary, badge, title, extras) = when (target) {
            "hex" -> Quad(hexStr, "Hex", "Decimal $decStr → Hexadecimal", "Dec: $decStr • Bin: $binStr • Oct: $octStr")
            "bin" -> Quad(binStr, "Binary", "Decimal $decStr → Binary", "Dec: $decStr • Hex: $hexStr • Oct: $octStr")
            "oct" -> Quad(octStr, "Octal", "Decimal $decStr → Octal", "Dec: $decStr • Hex: $hexStr • Bin: $binStr")
            else -> Quad(decStr, "Decimal", "Base Conversion → Decimal", "Hex: $hexStr • Bin: $binStr • Oct: $octStr")
        }

        val bitWidth = if (mag == 0L) 1 else (64 - java.lang.Long.numberOfLeadingZeros(mag))
        return ColorBaseResult(
            title = title,
            primaryValue = primary,
            badge = badge,
            subtitle = "$extras • $bitWidth-bit",
            copyText = primary
        )
    }

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}
