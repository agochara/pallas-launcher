package com.pallas.launcher.domain.search

import java.text.NumberFormat
import java.time.LocalDate
import java.time.Month
import java.time.Period
import java.time.Year
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

data class DateArithmeticResult(
    val title: String,
    val primaryValue: String,
    val badge: String,
    val subtitle: String,
    val copyText: String
)

/**
 * Pure-Kotlin offline date arithmetic evaluator for natural prefixless queries:
 * - `days to 15 may`, `days until dec 25, 2027`, `days since 2025-01-01`
 * - `days between 2026-05-15 and 2026-08-01`
 * - `today + 90d`, `today - 3w`, `15 may + 2m 10d`, `tomorrow + 1y`
 * - `weekday 2026-12-25`
 */
object DateArithmeticEngine {

    private val MONTHS: Map<String, Month> = mapOf(
        "jan" to Month.JANUARY, "january" to Month.JANUARY,
        "feb" to Month.FEBRUARY, "february" to Month.FEBRUARY,
        "mar" to Month.MARCH, "march" to Month.MARCH,
        "apr" to Month.APRIL, "april" to Month.APRIL,
        "may" to Month.MAY,
        "jun" to Month.JUNE, "june" to Month.JUNE,
        "jul" to Month.JULY, "july" to Month.JULY,
        "aug" to Month.AUGUST, "august" to Month.AUGUST,
        "sep" to Month.SEPTEMBER, "sept" to Month.SEPTEMBER, "september" to Month.SEPTEMBER,
        "oct" to Month.OCTOBER, "october" to Month.OCTOBER,
        "nov" to Month.NOVEMBER, "november" to Month.NOVEMBER,
        "dec" to Month.DECEMBER, "december" to Month.DECEMBER
    )

    private val DAYS_TO_REGEX = Regex(
        """^(?:how\s+many\s+)?days\s+(to|until|till|before|since|from)\s+(.+)$""",
        RegexOption.IGNORE_CASE
    )
    private val DAYS_BETWEEN_REGEX = Regex(
        """^(?:how\s+many\s+)?days\s+between\s+(.+?)\s+(?:and|to|-)\s+(.+)$""",
        RegexOption.IGNORE_CASE
    )
    private val WEEKDAY_REGEX = Regex(
        """^(?:weekday|day\s+of|what\s+day\s+is)\s+(.+)$""",
        RegexOption.IGNORE_CASE
    )
    private val ADD_SUB_REGEX = Regex(
        """^(.+?)\s*([+-]|plus|minus)\s+(.+)$""",
        RegexOption.IGNORE_CASE
    )
    private val OFFSET_TERM_REGEX = Regex(
        """(\d{1,5})\s*(d|day|days|w|wk|wks|week|weeks|m|mo|mos|mon|month|months|y|yr|yrs|year|years)\b""",
        RegexOption.IGNORE_CASE
    )

    private val ISO_DATE_REGEX = Regex("""^(\d{4})-(\d{1,2})-(\d{1,2})$""")
    private val DAY_MONTH_REGEX = Regex("""^(\d{1,2})(?:st|nd|rd|th)?[\s,-]+([a-zA-Z]+)(?:[\s,-]+(\d{4}))?$""")
    private val MONTH_DAY_REGEX = Regex("""^([a-zA-Z]+)[\s.-]+(\d{1,2})(?:st|nd|rd|th)?(?:[\s,]+(\d{4}))?$""")
    private val SLASH_DATE_REGEX = Regex("""^(\d{1,2})[/.](\d{1,2})[/.](\d{4})$""")

    private val FULL_DATE_FMT = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.US)
    private val SHORT_DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)

    fun evaluate(rawQuery: String, today: LocalDate = LocalDate.now()): DateArithmeticResult? {
        val clean = rawQuery.trim().replace(Regex("""\s+"""), " ")
        if (clean.length < 6) return null

        // 1. "days between <d1> and <d2>"
        DAYS_BETWEEN_REGEX.matchEntire(clean)?.let { m ->
            val d1 = parseDate(m.groupValues[1], today, biasFutureWhenNoYear = null) ?: return null
            val d2 = parseDate(m.groupValues[2], today, biasFutureWhenNoYear = null) ?: return null
            val days = abs(ChronoUnit.DAYS.between(d1, d2))
            val earlier = if (d1.isBefore(d2)) d1 else d2
            val later = if (d1.isBefore(d2)) d2 else d1
            return DateArithmeticResult(
                title = "${SHORT_DATE_FMT.format(earlier)} → ${SHORT_DATE_FMT.format(later)}",
                primaryValue = formatDayCount(days),
                badge = formatWeeksBadge(days),
                subtitle = formatPeriodBreakdown(earlier, later),
                copyText = days.toString()
            )
        }

        // 2. "days to|until|since|from <date>"
        DAYS_TO_REGEX.matchEntire(clean)?.let { m ->
            val verb = m.groupValues[1].lowercase(Locale.ROOT)
            val isSince = verb == "since" || verb == "from"
            val target = parseDate(
                m.groupValues[2],
                today,
                biasFutureWhenNoYear = !isSince
            ) ?: return null

            val rawDelta = ChronoUnit.DAYS.between(today, target)
            val absDays = abs(rawDelta)
            val primary = when {
                rawDelta == 0L -> "Today"
                rawDelta > 0L -> formatDayCount(rawDelta)
                else -> "${formatDayCount(absDays)} ago"
            }
            val breakdown = if (rawDelta >= 0L) {
                formatPeriodBreakdown(today, target)
            } else {
                formatPeriodBreakdown(target, today)
            }
            val titleVerb = if (isSince) "since" else "until"
            return DateArithmeticResult(
                title = "Days $titleVerb ${SHORT_DATE_FMT.format(target)}",
                primaryValue = primary,
                badge = formatWeeksBadge(absDays),
                subtitle = "${FULL_DATE_FMT.format(target)} • $breakdown",
                copyText = absDays.toString()
            )
        }

        // 3. "weekday <date>"
        WEEKDAY_REGEX.matchEntire(clean)?.let { m ->
            val target = parseDate(m.groupValues[1], today, biasFutureWhenNoYear = true) ?: return null
            val weekday = target.format(DateTimeFormatter.ofPattern("EEEE", Locale.US))
            val delta = ChronoUnit.DAYS.between(today, target)
            val deltaText = when {
                delta == 0L -> "Today"
                delta == 1L -> "Tomorrow"
                delta == -1L -> "Yesterday"
                delta > 0L -> "In ${formatDayCount(delta)}"
                else -> "${formatDayCount(abs(delta))} ago"
            }
            return DateArithmeticResult(
                title = SHORT_DATE_FMT.format(target),
                primaryValue = weekday,
                badge = "Date",
                subtitle = "$deltaText • Day ${target.dayOfYear} of ${target.year}",
                copyText = FULL_DATE_FMT.format(target)
            )
        }

        // 4. "<anchor> +/- <terms>" (e.g. "today + 90d", "15 may - 2w")
        ADD_SUB_REGEX.matchEntire(clean)?.let { m ->
            val anchorStr = m.groupValues[1].trim()
            val opStr = m.groupValues[2].lowercase(Locale.ROOT)
            val termsStr = m.groupValues[3].trim()

            val sign = if (opStr == "-" || opStr == "minus") -1L else 1L
            val anchorDate = parseAnchor(anchorStr, today) ?: return null
            val parsedTerms = parseDurationTerms(termsStr) ?: return null

            var resultDate = anchorDate
            for ((qty, unit) in parsedTerms) {
                val signed = qty * sign
                resultDate = when (unit) {
                    'd' -> resultDate.plusDays(signed)
                    'w' -> resultDate.plusWeeks(signed)
                    'm' -> resultDate.plusMonths(signed)
                    'y' -> resultDate.plusYears(signed)
                    else -> resultDate
                }
            }

            val totalDeltaDays = ChronoUnit.DAYS.between(anchorDate, resultDate)
            val deltaFromToday = ChronoUnit.DAYS.between(today, resultDate)
            val iso = resultDate.toString()
            val formatted = FULL_DATE_FMT.format(resultDate)
            val opSymbol = if (sign > 0) "+" else "-"
            val absDelta = abs(totalDeltaDays)
            val todayNote = when {
                deltaFromToday == 0L -> "Today"
                deltaFromToday > 0L -> "In ${formatDayCount(deltaFromToday)}"
                else -> "${formatDayCount(abs(deltaFromToday))} ago"
            }

            return DateArithmeticResult(
                title = "${SHORT_DATE_FMT.format(anchorDate)} $opSymbol $termsStr",
                primaryValue = formatted,
                badge = iso,
                subtitle = "${formatDayCount(absDelta)} shift • $todayNote",
                copyText = iso
            )
        }

        return null
    }

    private fun parseAnchor(raw: String, today: LocalDate): LocalDate? {
        return when (raw.lowercase(Locale.ROOT)) {
            "today", "now" -> today
            "tomorrow", "tmrw" -> today.plusDays(1)
            "yesterday" -> today.minusDays(1)
            else -> parseDate(raw, today, biasFutureWhenNoYear = true)
        }
    }

    private fun parseDurationTerms(raw: String): List<Pair<Long, Char>>? {
        val matches = OFFSET_TERM_REGEX.findAll(raw).toList()
        if (matches.isEmpty()) return null
        // Verify the entire string is consumed by duration terms (+ optional whitespace / commas / "and")
        var stripped = raw
        for (match in matches) {
            stripped = stripped.replaceFirst(match.value, "")
        }
        val leftover = stripped.replace(Regex("""[\s,+&]|\band\b""", RegexOption.IGNORE_CASE), "")
        if (leftover.isNotEmpty()) return null

        val terms = mutableListOf<Pair<Long, Char>>()
        for (match in matches) {
            val qty = match.groupValues[1].toLongOrNull() ?: return null
            val unitToken = match.groupValues[2].lowercase(Locale.ROOT)
            val unit = when {
                unitToken.startsWith("d") -> 'd'
                unitToken.startsWith("w") -> 'w'
                unitToken.startsWith("m") -> 'm'
                unitToken.startsWith("y") -> 'y'
                else -> return null
            }
            if ((unit == 'y' && qty > 500L) || qty > 200_000L) return null
            terms.add(qty to unit)
        }
        return terms
    }

    internal fun parseDate(
        raw: String,
        today: LocalDate,
        biasFutureWhenNoYear: Boolean?
    ): LocalDate? {
        val clean = raw.trim().replace(Regex("""\s+"""), " ")
        if (clean.isEmpty()) return null
        val lower = clean.lowercase(Locale.ROOT)

        when (lower) {
            "today", "now" -> return today
            "tomorrow", "tmrw" -> return today.plusDays(1)
            "yesterday" -> return today.minusDays(1)
            "christmas", "xmas" -> return resolveMonthDay(Month.DECEMBER, 25, null, today, biasFutureWhenNoYear)
            "new year", "new years", "new year's" -> return resolveMonthDay(Month.JANUARY, 1, null, today, biasFutureWhenNoYear)
        }

        ISO_DATE_REGEX.matchEntire(clean)?.let { m ->
            val y = m.groupValues[1].toIntOrNull() ?: return null
            val mo = m.groupValues[2].toIntOrNull() ?: return null
            val d = m.groupValues[3].toIntOrNull() ?: return null
            return runCatching { LocalDate.of(y, mo, d) }.getOrNull()
        }

        DAY_MONTH_REGEX.matchEntire(clean)?.let { m ->
            val day = m.groupValues[1].toIntOrNull() ?: return null
            val month = MONTHS[m.groupValues[2].lowercase(Locale.ROOT)] ?: return null
            val year = m.groupValues[3].takeIf { it.isNotEmpty() }?.toIntOrNull()
            return resolveMonthDay(month, day, year, today, biasFutureWhenNoYear)
        }

        MONTH_DAY_REGEX.matchEntire(clean)?.let { m ->
            val month = MONTHS[m.groupValues[1].lowercase(Locale.ROOT)] ?: return null
            val day = m.groupValues[2].toIntOrNull() ?: return null
            val year = m.groupValues[3].takeIf { it.isNotEmpty() }?.toIntOrNull()
            return resolveMonthDay(month, day, year, today, biasFutureWhenNoYear)
        }

        SLASH_DATE_REGEX.matchEntire(clean)?.let { m ->
            val first = m.groupValues[1].toIntOrNull() ?: return null
            val second = m.groupValues[2].toIntOrNull() ?: return null
            val year = m.groupValues[3].toIntOrNull() ?: return null
            return if (second > 12) {
                runCatching { LocalDate.of(year, first, second) }.getOrNull()
            } else {
                runCatching { LocalDate.of(year, second, first) }.getOrNull()
            }
        }

        return null
    }

    private fun resolveMonthDay(
        month: Month,
        day: Int,
        explicitYear: Int?,
        today: LocalDate,
        biasFutureWhenNoYear: Boolean?
    ): LocalDate? {
        if (day !in 1..31) return null
        if (explicitYear != null) {
            return runCatching { LocalDate.of(explicitYear, month, day) }.getOrNull()
        }

        fun validDateForYear(y: Int): LocalDate? {
            if (month == Month.FEBRUARY && day == 29 && !Year.isLeap(y.toLong())) return null
            return if (day <= YearMonth.of(y, month).lengthOfMonth()) LocalDate.of(y, month, day) else null
        }

        when (biasFutureWhenNoYear) {
            true -> {
                var y = today.year
                while (true) {
                    val d = validDateForYear(y)
                    if (d != null && !d.isBefore(today)) return d
                    y++
                    if (y > today.year + 8) return null
                }
            }
            false -> {
                var y = today.year
                while (true) {
                    val d = validDateForYear(y)
                    if (d != null && !d.isAfter(today)) return d
                    y--
                    if (y < today.year - 8) return null
                }
            }
            null -> return validDateForYear(today.year)
        }
    }

    private fun formatDayCount(days: Long): String {
        val formatted = NumberFormat.getIntegerInstance(Locale.US).format(days)
        return if (days == 1L) "1 day" else "$formatted days"
    }

    private fun formatWeeksBadge(days: Long): String {
        if (days < 7L) return if (days == 1L) "1 day" else "$days days"
        val w = days / 7L
        val r = days % 7L
        return if (r == 0L) "${w}w" else "${w}w ${r}d"
    }

    private fun formatPeriodBreakdown(startInclusive: LocalDate, endExclusive: LocalDate): String {
        val period = Period.between(startInclusive, endExclusive)
        val parts = buildList {
            if (period.years > 0) add("${period.years}y")
            if (period.months > 0) add("${period.months}mo")
            if (period.days > 0) add("${period.days}d")
        }
        return if (parts.isEmpty()) "0d" else parts.joinToString(" ")
    }
}
