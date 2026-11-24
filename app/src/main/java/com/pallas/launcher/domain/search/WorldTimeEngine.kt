package com.pallas.launcher.domain.search

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

data class WorldTimeResult(
    val title: String,
    val primaryValue: String,
    val badge: String,
    val subtitle: String,
    val copyText: String
)

object WorldTimeEngine {

    private val ALIASES: Map<String, Pair<String, String>> = linkedMapOf(
        // Universal / Standard Abbreviations
        "utc" to ("UTC" to "UTC"),
        "gmt" to ("GMT" to "GMT"),
        "z" to ("UTC" to "UTC"),
        "zulu" to ("UTC" to "UTC"),
        "est" to ("America/New_York" to "New York (ET)"),
        "edt" to ("America/New_York" to "New York (ET)"),
        "et" to ("America/New_York" to "New York (ET)"),
        "cst" to ("America/Chicago" to "Chicago (CT)"),
        "cdt" to ("America/Chicago" to "Chicago (CT)"),
        "ct" to ("America/Chicago" to "Chicago (CT)"),
        "mst" to ("America/Denver" to "Denver (MT)"),
        "mdt" to ("America/Denver" to "Denver (MT)"),
        "mt" to ("America/Denver" to "Denver (MT)"),
        "pst" to ("America/Los_Angeles" to "Los Angeles (PT)"),
        "pdt" to ("America/Los_Angeles" to "Los Angeles (PT)"),
        "pt" to ("America/Los_Angeles" to "Los Angeles (PT)"),
        "bst" to ("Europe/London" to "London"),
        "cet" to ("Europe/Paris" to "Central Europe"),
        "cest" to ("Europe/Paris" to "Central Europe"),
        "eet" to ("Europe/Athens" to "Eastern Europe"),
        "wet" to ("Europe/Lisbon" to "Western Europe"),
        "ist" to ("Asia/Kolkata" to "India (IST)"),
        "jst" to ("Asia/Tokyo" to "Japan (JST)"),
        "kst" to ("Asia/Seoul" to "Korea (KST)"),
        "sgt" to ("Asia/Singapore" to "Singapore (SGT)"),
        "hkt" to ("Asia/Hong_Kong" to "Hong Kong (HKT)"),
        "ict" to ("Asia/Bangkok" to "Indochina (ICT)"),
        "gst" to ("Asia/Dubai" to "Gulf (GST)"),
        "aest" to ("Australia/Sydney" to "Sydney (AET)"),
        "aedt" to ("Australia/Sydney" to "Sydney (AET)"),
        "nzst" to ("Pacific/Auckland" to "Auckland (NZT)"),
        "brt" to ("America/Sao_Paulo" to "São Paulo (BRT)"),

        // North America
        "nyc" to ("America/New_York" to "New York"),
        "ny" to ("America/New_York" to "New York"),
        "new york" to ("America/New_York" to "New York"),
        "manhattan" to ("America/New_York" to "New York"),
        "boston" to ("America/New_York" to "Boston"),
        "dc" to ("America/New_York" to "Washington D.C."),
        "washington" to ("America/New_York" to "Washington D.C."),
        "miami" to ("America/New_York" to "Miami"),
        "atlanta" to ("America/New_York" to "Atlanta"),
        "toronto" to ("America/Toronto" to "Toronto"),
        "montreal" to ("America/Toronto" to "Montreal"),
        "chicago" to ("America/Chicago" to "Chicago"),
        "dallas" to ("America/Chicago" to "Dallas"),
        "houston" to ("America/Chicago" to "Houston"),
        "austin" to ("America/Chicago" to "Austin"),
        "mexico city" to ("America/Mexico_City" to "Mexico City"),
        "denver" to ("America/Denver" to "Denver"),
        "phoenix" to ("America/Phoenix" to "Phoenix"),
        "sf" to ("America/Los_Angeles" to "San Francisco"),
        "san francisco" to ("America/Los_Angeles" to "San Francisco"),
        "bay area" to ("America/Los_Angeles" to "San Francisco"),
        "la" to ("America/Los_Angeles" to "Los Angeles"),
        "los angeles" to ("America/Los_Angeles" to "Los Angeles"),
        "seattle" to ("America/Los_Angeles" to "Seattle"),
        "vancouver" to ("America/Vancouver" to "Vancouver"),
        "honolulu" to ("Pacific/Honolulu" to "Honolulu"),
        "hawaii" to ("Pacific/Honolulu" to "Hawaii"),

        // South America
        "sao paulo" to ("America/Sao_Paulo" to "São Paulo"),
        "brazil" to ("America/Sao_Paulo" to "São Paulo"),
        "rio" to ("America/Sao_Paulo" to "Rio de Janeiro"),
        "buenos aires" to ("America/Argentina/Buenos_Aires" to "Buenos Aires"),
        "santiago" to ("America/Santiago" to "Santiago"),
        "bogota" to ("America/Bogota" to "Bogotá"),
        "lima" to ("America/Lima" to "Lima"),

        // Europe
        "london" to ("Europe/London" to "London"),
        "uk" to ("Europe/London" to "London"),
        "britain" to ("Europe/London" to "London"),
        "england" to ("Europe/London" to "London"),
        "dublin" to ("Europe/Dublin" to "Dublin"),
        "lisbon" to ("Europe/Lisbon" to "Lisbon"),
        "paris" to ("Europe/Paris" to "Paris"),
        "france" to ("Europe/Paris" to "Paris"),
        "berlin" to ("Europe/Berlin" to "Berlin"),
        "germany" to ("Europe/Berlin" to "Berlin"),
        "frankfurt" to ("Europe/Berlin" to "Frankfurt"),
        "munich" to ("Europe/Berlin" to "Munich"),
        "zurich" to ("Europe/Zurich" to "Zurich"),
        "geneva" to ("Europe/Zurich" to "Geneva"),
        "switzerland" to ("Europe/Zurich" to "Zurich"),
        "amsterdam" to ("Europe/Amsterdam" to "Amsterdam"),
        "brussels" to ("Europe/Brussels" to "Brussels"),
        "madrid" to ("Europe/Madrid" to "Madrid"),
        "rome" to ("Europe/Rome" to "Rome"),
        "milan" to ("Europe/Rome" to "Milan"),
        "vienna" to ("Europe/Vienna" to "Vienna"),
        "stockholm" to ("Europe/Stockholm" to "Stockholm"),
        "oslo" to ("Europe/Oslo" to "Oslo"),
        "copenhagen" to ("Europe/Copenhagen" to "Copenhagen"),
        "warsaw" to ("Europe/Warsaw" to "Warsaw"),
        "prague" to ("Europe/Prague" to "Prague"),
        "athens" to ("Europe/Athens" to "Athens"),
        "helsinki" to ("Europe/Helsinki" to "Helsinki"),
        "istanbul" to ("Europe/Istanbul" to "Istanbul"),
        "turkey" to ("Europe/Istanbul" to "Istanbul"),
        "moscow" to ("Europe/Moscow" to "Moscow"),

        // Middle East & Africa
        "dubai" to ("Asia/Dubai" to "Dubai"),
        "uae" to ("Asia/Dubai" to "Dubai"),
        "abu dhabi" to ("Asia/Dubai" to "Abu Dhabi"),
        "riyadh" to ("Asia/Riyadh" to "Riyadh"),
        "doha" to ("Asia/Qatar" to "Doha"),
        "tel aviv" to ("Asia/Jerusalem" to "Tel Aviv"),
        "jerusalem" to ("Asia/Jerusalem" to "Jerusalem"),
        "ramallah" to ("Asia/Hebron" to "Ramallah"),
        "cairo" to ("Africa/Cairo" to "Cairo"),
        "johannesburg" to ("Africa/Johannesburg" to "Johannesburg"),
        "nairobi" to ("Africa/Nairobi" to "Nairobi"),
        "lagos" to ("Africa/Lagos" to "Lagos"),

        // South Asia & East / SE Asia
        "india" to ("Asia/Kolkata" to "India"),
        "delhi" to ("Asia/Kolkata" to "New Delhi"),
        "new delhi" to ("Asia/Kolkata" to "New Delhi"),
        "mumbai" to ("Asia/Kolkata" to "Mumbai"),
        "bangalore" to ("Asia/Kolkata" to "Bengaluru"),
        "bengaluru" to ("Asia/Kolkata" to "Bengaluru"),
        "kolkata" to ("Asia/Kolkata" to "Kolkata"),
        "chennai" to ("Asia/Kolkata" to "Chennai"),
        "hyderabad" to ("Asia/Kolkata" to "Hyderabad"),
        "karachi" to ("Asia/Karachi" to "Karachi"),
        "dhaka" to ("Asia/Dhaka" to "Dhaka"),
        "kathmandu" to ("Asia/Kathmandu" to "Kathmandu"),
        "colombo" to ("Asia/Colombo" to "Colombo"),
        "bangkok" to ("Asia/Bangkok" to "Bangkok"),
        "thailand" to ("Asia/Bangkok" to "Bangkok"),
        "vietnam" to ("Asia/Ho_Chi_Minh" to "Ho Chi Minh City"),
        "hanoi" to ("Asia/Ho_Chi_Minh" to "Hanoi"),
        "jakarta" to ("Asia/Jakarta" to "Jakarta"),
        "singapore" to ("Asia/Singapore" to "Singapore"),
        "sg" to ("Asia/Singapore" to "Singapore"),
        "kuala lumpur" to ("Asia/Kuala_Lumpur" to "Kuala Lumpur"),
        "kl" to ("Asia/Kuala_Lumpur" to "Kuala Lumpur"),
        "hk" to ("Asia/Hong_Kong" to "Hong Kong"),
        "hong kong" to ("Asia/Hong_Kong" to "Hong Kong"),
        "taipei" to ("Asia/Taipei" to "Taipei"),
        "beijing" to ("Asia/Shanghai" to "Beijing"),
        "shanghai" to ("Asia/Shanghai" to "Shanghai"),
        "china" to ("Asia/Shanghai" to "Shanghai"),
        "manila" to ("Asia/Manila" to "Manila"),
        "seoul" to ("Asia/Seoul" to "Seoul"),
        "korea" to ("Asia/Seoul" to "Seoul"),
        "tokyo" to ("Asia/Tokyo" to "Tokyo"),
        "japan" to ("Asia/Tokyo" to "Tokyo"),
        "osaka" to ("Asia/Tokyo" to "Osaka"),

        // Oceania
        "sydney" to ("Australia/Sydney" to "Sydney"),
        "melbourne" to ("Australia/Melbourne" to "Melbourne"),
        "perth" to ("Australia/Perth" to "Perth"),
        "brisbane" to ("Australia/Brisbane" to "Brisbane"),
        "auckland" to ("Pacific/Auckland" to "Auckland"),
        "nz" to ("Pacific/Auckland" to "Auckland")
    )

    private val FIXED_OFFSET_REGEX = Regex("""^(?:utc|gmt)\s*([+-])\s*(\d{1,2})(?::(\d{2}))?$""", RegexOption.IGNORE_CASE)
    private val TIME_PREFIX_AMPM = Regex("""^(\d{1,2})(?::(\d{2}))?\s*(am|pm)\s+(.+)$""", RegexOption.IGNORE_CASE)
    private val TIME_PREFIX_24H = Regex("""^(\d{1,2}):(\d{2})\s+(.+)$""")

    private val AVAILABLE_ZONE_LOOKUP: Map<String, String> by lazy {
        val map = HashMap<String, String>(1024)
        for (id in ZoneId.getAvailableZoneIds()) {
            map[id.lowercase(Locale.ROOT)] = id
            val city = id.substringAfterLast('/').replace('_', ' ').lowercase(Locale.ROOT)
            map.putIfAbsent(city, id)
        }
        map
    }

    fun evaluate(
        query: String,
        is24Hour: Boolean = false,
        now: Instant = Instant.now(),
        localZone: ZoneId = ZoneId.systemDefault()
    ): WorldTimeResult? {
        val clean = query.trim()
        if (clean.isEmpty()) return null

        // Check optional leading time conversion (e.g. "9pm nyc", "14:30 london")
        parseTimeAndZone(clean)?.let { (localTime, zoneQuery) ->
            val resolved = resolveZone(zoneQuery) ?: return@let
            val (targetZone, displayName) = resolved
            val targetNow = now.atZone(targetZone)
            val targetZdt = ZonedDateTime.of(targetNow.toLocalDate(), localTime, targetZone)
            val localConverted = targetZdt.withZoneSameInstant(localZone)

            val timeFmt = DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a", Locale.US)
            val dateFmt = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)

            val dayDiff = ChronoUnit.DAYS.between(targetZdt.toLocalDate(), localConverted.toLocalDate())
            val dayNote = when (dayDiff) {
                1L -> " (Tomorrow)"
                -1L -> " (Yesterday)"
                0L -> ""
                else -> " (${dateFmt.format(localConverted)})"
            }

            val targetAbbrev = targetZdt.zone.getDisplayName(TextStyle.SHORT, Locale.US)
            val localAbbrev = localConverted.zone.getDisplayName(TextStyle.SHORT, Locale.US)
            val relBadge = formatRelativeOffset(
                targetOffsetSec = targetZdt.offset.totalSeconds,
                localOffsetSec = localConverted.offset.totalSeconds
            )
            val convertedStr = "${timeFmt.format(localConverted)}$dayNote"

            return WorldTimeResult(
                title = "${timeFmt.format(targetZdt)} $displayName ($targetAbbrev) → Local ($localAbbrev)",
                primaryValue = convertedStr,
                badge = relBadge,
                subtitle = "${dateFmt.format(localConverted)} • ${targetZone.id} (${formatUtcOffset(targetZdt.offset.totalSeconds)})",
                copyText = convertedStr
            )
        }

        val (targetZone, displayName) = resolveZone(clean) ?: return null
        val targetZdt = now.atZone(targetZone)
        val localZdt = now.atZone(localZone)

        val timeFmt = DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a", Locale.US)
        val dateFmt = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)

        val dayDiff = ChronoUnit.DAYS.between(localZdt.toLocalDate(), targetZdt.toLocalDate())
        val dayNote = when (dayDiff) {
            1L -> " • Tomorrow"
            -1L -> " • Yesterday"
            else -> ""
        }

        val formattedTime = "${timeFmt.format(targetZdt)}$dayNote"
        val relBadge = formatRelativeOffset(
            targetOffsetSec = targetZdt.offset.totalSeconds,
            localOffsetSec = localZdt.offset.totalSeconds
        )
        val abbrev = targetZdt.zone.getDisplayName(TextStyle.SHORT, Locale.US)
        val utcStr = formatUtcOffset(targetZdt.offset.totalSeconds)

        return WorldTimeResult(
            title = "$displayName ($abbrev)",
            primaryValue = formattedTime,
            badge = relBadge,
            subtitle = "${dateFmt.format(targetZdt)} • $utcStr • ${targetZone.id}",
            copyText = timeFmt.format(targetZdt)
        )
    }

    private fun parseTimeAndZone(clean: String): Pair<LocalTime, String>? {
        TIME_PREFIX_AMPM.matchEntire(clean)?.let { m ->
            val h12 = m.groupValues[1].toIntOrNull() ?: return null
            val min = m.groupValues[2].takeIf { it.isNotEmpty() }?.toIntOrNull() ?: 0
            val ampm = m.groupValues[3].lowercase(Locale.ROOT)
            val rest = m.groupValues[4].trim()
            if (h12 !in 1..12 || min !in 0..59) return null
            val h24 = when {
                ampm == "am" && h12 == 12 -> 0
                ampm == "pm" && h12 != 12 -> h12 + 12
                else -> h12
            }
            return LocalTime.of(h24, min) to rest
        }

        TIME_PREFIX_24H.matchEntire(clean)?.let { m ->
            val h24 = m.groupValues[1].toIntOrNull() ?: return null
            val min = m.groupValues[2].toIntOrNull() ?: return null
            val rest = m.groupValues[3].trim()
            if (h24 !in 0..23 || min !in 0..59) return null
            return LocalTime.of(h24, min) to rest
        }
        return null
    }

    fun resolveZone(raw: String): Pair<ZoneId, String>? {
        val clean = raw.trim()
        if (clean.isEmpty()) return null
        val lower = clean.lowercase(Locale.ROOT).replace(Regex("""\s+"""), " ")

        ALIASES[lower]?.let { (zoneId, label) ->
            return ZoneId.of(zoneId) to label
        }

        FIXED_OFFSET_REGEX.matchEntire(lower)?.let { m ->
            val sign = if (m.groupValues[1] == "-") -1 else 1
            val hrs = m.groupValues[2].toIntOrNull() ?: return null
            val mins = m.groupValues[3].takeIf { it.isNotEmpty() }?.toIntOrNull() ?: 0
            if (hrs > 18 || mins !in 0..59) return null
            val offset = ZoneOffset.ofTotalSeconds(sign * (hrs * 3600 + mins * 60))
            val label = formatUtcOffset(offset.totalSeconds)
            return ZoneId.ofOffset("UTC", offset) to label
        }

        runCatching { ZoneId.of(clean) }.getOrNull()?.let { z ->
            val label = z.id.substringAfterLast('/').replace('_', ' ')
            return z to label
        }

        AVAILABLE_ZONE_LOOKUP[lower]?.let { matchedId ->
            val label = matchedId.substringAfterLast('/').replace('_', ' ')
            return ZoneId.of(matchedId) to label
        }

        // Substring fallback on zone IDs if >= 4 chars
        if (lower.length >= 4) {
            val match = ZoneId.getAvailableZoneIds()
                .firstOrNull { id ->
                    id.substringAfterLast('/').replace('_', ' ').lowercase(Locale.ROOT).contains(lower)
                }
            if (match != null) {
                val label = match.substringAfterLast('/').replace('_', ' ')
                return ZoneId.of(match) to label
            }
        }
        return null
    }

    private fun formatRelativeOffset(targetOffsetSec: Int, localOffsetSec: Int): String {
        val diff = targetOffsetSec - localOffsetSec
        if (diff == 0) return "Local"
        val sign = if (diff > 0) "+" else "-"
        val totalMin = abs(diff) / 60
        val h = totalMin / 60
        val m = totalMin % 60
        return if (m == 0) "$sign${h}h" else "$sign${h}h ${m}m"
    }

    private fun formatUtcOffset(offsetSec: Int): String {
        if (offsetSec == 0) return "UTC"
        val sign = if (offsetSec > 0) "+" else "-"
        val totalMin = abs(offsetSec) / 60
        val h = totalMin / 60
        val m = totalMin % 60
        return if (m == 0) "UTC$sign$h" else String.format(Locale.US, "UTC%s%d:%02d", sign, h, m)
    }
}
