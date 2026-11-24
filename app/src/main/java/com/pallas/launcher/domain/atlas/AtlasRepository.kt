package com.pallas.launcher.domain.atlas

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sin

data class AtlasRing(
    val coords: FloatArray,
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float
)

data class AtlasPolygon(
    val outer: AtlasRing,
    val holes: List<AtlasRing> = emptyList()
)

data class AtlasCountry(
    val id: String,
    val name: String,
    val aliases: List<String>,
    val region: String,
    val capital: String,
    val colorIndex: Int,
    val minLat: Double,
    val minLon: Double,
    val maxLat: Double,
    val maxLon: Double,
    val centerLat: Double,
    val centerLon: Double,
    val mercMinX: Float,
    val mercMinY: Float,
    val mercMaxX: Float,
    val mercMaxY: Float,
    val mercCenterX: Float,
    val mercCenterY: Float,
    val polygons: List<AtlasPolygon>
)

data class AtlasCity(
    val name: String,
    val country: String,
    val isCapital: Boolean,
    val lat: Double,
    val lon: Double,
    val mercX: Float,
    val mercY: Float
)

data class AtlasData(
    val landRings: List<AtlasRing>,
    val lakeRings: List<AtlasRing>,
    val countries: List<AtlasCountry>,
    val cities: List<AtlasCity>
)

sealed interface AtlasSearchHit {
    val label: String
    val secondary: String

    data class Country(val country: AtlasCountry) : AtlasSearchHit {
        override val label: String get() = country.name
        override val secondary: String
            get() = if (country.capital.isNotBlank()) {
                "${country.region} · Capital: ${country.capital}"
            } else {
                country.region
            }
    }

    data class City(val city: AtlasCity) : AtlasSearchHit {
        override val label: String get() = city.name
        override val secondary: String
            get() = if (city.isCapital) "${city.country} · Capital" else city.country
    }
}

object AtlasRepository {

    @Volatile
    private var cachedData: AtlasData? = null

    fun lonToMercX(lon: Double): Float =
        ((lon.coerceIn(-180.0, 180.0) + 180.0) / 360.0).toFloat()

    fun latToMercY(lat: Double): Float {
        val clamped = lat.coerceIn(-82.0, 84.0)
        val rad = Math.toRadians(clamped)
        val s = sin(rad)
        val y = 0.5 - ln((1.0 + s) / (1.0 - s)) / (4.0 * PI)
        return y.toFloat().coerceIn(0f, 1f)
    }

    suspend fun load(context: Context): AtlasData {
        cachedData?.let { return it }
        val appCtx = context.applicationContext
        return withContext(Dispatchers.IO) {
            cachedData?.let { return@withContext it }
            val landJson = readAssetText(appCtx, "atlas/land.json")
            val countriesJson = readAssetText(appCtx, "atlas/countries.json")
            val citiesJson = readAssetText(appCtx, "atlas/cities.json")
            parseFromStrings(landJson, countriesJson, citiesJson).also {
                cachedData = it
            }
        }
    }

    private fun readAssetText(context: Context, assetPath: String): String =
        context.assets.open(assetPath).bufferedReader(Charsets.UTF_8).use { it.readText() }

    fun parseFromStrings(
        landJson: String,
        countriesJson: String,
        citiesJson: String
    ): AtlasData {
        val (landRings, lakeRings) = parseLandPayload(landJson)
        val countries = parseCountriesPayload(countriesJson)
        val cities = parseCitiesPayload(citiesJson)
        return AtlasData(
            landRings = landRings,
            lakeRings = lakeRings,
            countries = countries,
            cities = cities
        )
    }

    private fun parseLandPayload(json: String): Pair<List<AtlasRing>, List<AtlasRing>> {
        val scanner = FastJsonScanner(json)
        var qScale = 0.0
        val landRings = ArrayList<AtlasRing>(1500)
        val lakeRings = ArrayList<AtlasRing>(450)

        scanner.readObject { key ->
            when (key) {
                "q" -> qScale = scanner.readDouble()
                "land" -> scanner.readArray {
                    scanner.readRingCoords(qScale)?.let { landRings.add(it) }
                }
                "lakes" -> scanner.readArray {
                    scanner.readRingCoords(qScale)?.let { lakeRings.add(it) }
                }
                else -> scanner.skipValue()
            }
        }
        return landRings to lakeRings
    }

    private fun parseCountriesPayload(json: String): List<AtlasCountry> {
        val scanner = FastJsonScanner(json)
        var qScale = 0.0
        val countries = ArrayList<AtlasCountry>(250)

        fun readCountryObj(index: Int) {
            var id = ""
            var name = ""
            val aliases = ArrayList<String>(4)
            var region = ""
            var capital = ""
            var colorIndex = index % 5
            var minLat = 0.0
            var minLon = 0.0
            var maxLat = 0.0
            var maxLon = 0.0
            var centerLat = 0.0
            var centerLon = 0.0
            val polygons = ArrayList<AtlasPolygon>(8)

            scanner.readObject { key ->
                when (key) {
                    "id" -> id = scanner.readString()
                    "n" -> name = scanner.readString()
                    "a" -> scanner.readArray { aliases.add(scanner.readString()) }
                    "r" -> region = scanner.readString()
                    "cp" -> capital = scanner.readString()
                    "ci" -> colorIndex = scanner.readLong().toInt()
                    "b" -> {
                        var bIdx = 0
                        scanner.readArray {
                            val v = scanner.readDouble()
                            when (bIdx++) {
                                0 -> minLat = v
                                1 -> minLon = v
                                2 -> maxLat = v
                                3 -> maxLon = v
                            }
                        }
                    }
                    "c" -> {
                        var cIdx = 0
                        scanner.readArray {
                            val v = scanner.readDouble()
                            when (cIdx++) {
                                0 -> centerLat = v
                                1 -> centerLon = v
                            }
                        }
                    }
                    "p" -> {
                        if (qScale > 0.0) {
                            // Schema v2: p = [ [[outerDelta...], [holeDelta...], ...], ... ]
                            scanner.readArray {
                                var outer: AtlasRing? = null
                                var holes: ArrayList<AtlasRing>? = null
                                scanner.readArray {
                                    val ring = scanner.readRingCoords(qScale)
                                    if (ring != null) {
                                        if (outer == null) {
                                            outer = ring
                                        } else {
                                            val h = holes ?: ArrayList<AtlasRing>(2).also { holes = it }
                                            h.add(ring)
                                        }
                                    }
                                }
                                outer?.let { polygons.add(AtlasPolygon(it, holes ?: emptyList())) }
                            }
                        } else {
                            // Legacy flat ring list fallback
                            scanner.readArray {
                                scanner.readRingCoords(0.0)?.let { polygons.add(AtlasPolygon(it)) }
                            }
                        }
                    }
                    else -> scanner.skipValue()
                }
            }

            countries.add(
                AtlasCountry(
                    id = id,
                    name = name,
                    aliases = aliases,
                    region = region,
                    capital = capital,
                    colorIndex = colorIndex,
                    minLat = minLat,
                    minLon = minLon,
                    maxLat = maxLat,
                    maxLon = maxLon,
                    centerLat = centerLat,
                    centerLon = centerLon,
                    mercMinX = lonToMercX(minLon),
                    mercMinY = latToMercY(maxLat),
                    mercMaxX = lonToMercX(maxLon),
                    mercMaxY = latToMercY(minLat),
                    mercCenterX = lonToMercX(centerLon),
                    mercCenterY = latToMercY(centerLat),
                    polygons = polygons
                )
            )
        }

        if (scanner.peekChar() == '[') {
            var idx = 0
            scanner.readArray { readCountryObj(idx++) }
        } else {
            scanner.readObject { key ->
                when (key) {
                    "q" -> qScale = scanner.readDouble()
                    "countries" -> {
                        var idx = 0
                        scanner.readArray { readCountryObj(idx++) }
                    }
                    else -> scanner.skipValue()
                }
            }
        }
        return countries
    }

    private fun parseCitiesPayload(json: String): List<AtlasCity> {
        val scanner = FastJsonScanner(json)
        val cities = ArrayList<AtlasCity>(600)
        scanner.readArray {
            var name = ""
            var country = ""
            var isCapital = false
            var lat = 0.0
            var lon = 0.0
            scanner.readObject { key ->
                when (key) {
                    "n" -> name = scanner.readString().replace(Regex("\\s+"), " ")
                    "c" -> country = scanner.readString()
                    "k" -> isCapital = scanner.readBoolean()
                    "la" -> lat = scanner.readDouble()
                    "lo" -> lon = scanner.readDouble()
                    else -> scanner.skipValue()
                }
            }
            cities.add(
                AtlasCity(
                    name = name,
                    country = country,
                    isCapital = isCapital,
                    lat = lat,
                    lon = lon,
                    mercX = lonToMercX(lon),
                    mercY = latToMercY(lat)
                )
            )
        }
        return cities
    }

    fun hitTestCountry(data: AtlasData, mercX: Float, mercY: Float): AtlasCountry? {
        // Iterate in reverse render order so enclaves (VAT, SMR, LSO) & PSE match before surrounding states.
        for (i in data.countries.indices.reversed()) {
            val c = data.countries[i]
            for (poly in c.polygons) {
                if (pointInPolygon(mercX, mercY, poly)) {
                    return c
                }
            }
        }
        return null
    }

    fun hitTestCountryByLatLon(data: AtlasData, lat: Double, lon: Double): AtlasCountry? =
        hitTestCountry(data, lonToMercX(lon), latToMercY(lat))

    fun findAllCountriesAtLatLon(data: AtlasData, lat: Double, lon: Double): List<AtlasCountry> {
        val mx = lonToMercX(lon)
        val my = latToMercY(lat)
        return data.countries.filter { c ->
            c.polygons.any { poly -> pointInPolygon(mx, my, poly) }
        }
    }

    private fun pointInPolygon(x: Float, y: Float, poly: AtlasPolygon): Boolean {
        if (!pointInRing(x, y, poly.outer)) return false
        for (hole in poly.holes) {
            if (pointInRing(x, y, hole)) return false
        }
        return true
    }

    private fun pointInRing(x: Float, y: Float, ring: AtlasRing): Boolean {
        if (x < ring.minX || x > ring.maxX || y < ring.minY || y > ring.maxY) return false
        val coords = ring.coords
        val n = coords.size / 2
        if (n < 3) return false
        var inside = false
        var j = n - 1
        for (i in 0 until n) {
            val xi = coords[i * 2]
            val yi = coords[i * 2 + 1]
            val xj = coords[j * 2]
            val yj = coords[j * 2 + 1]
            if ((yi > y) != (yj > y)) {
                val denom = yj - yi
                if (abs(denom) > 1e-12f && x < (xj - xi) * (y - yi) / denom + xi) {
                    inside = !inside
                }
            }
            j = i
        }
        return inside
    }

    fun searchAtlas(data: AtlasData, rawQuery: String, limit: Int = 25): List<AtlasSearchHit> {
        val q = rawQuery.trim().lowercase(Locale.ROOT)
        if (q.isEmpty()) return emptyList()

        val scored = ArrayList<Pair<Int, AtlasSearchHit>>()

        for (c in data.countries) {
            val nameLower = c.name.lowercase(Locale.ROOT)
            val idLower = c.id.lowercase(Locale.ROOT)
            val aliasMatches = c.aliases.map { it.lowercase(Locale.ROOT) }
            val score = when {
                nameLower == q || idLower == q || aliasMatches.any { it == q } -> 100
                nameLower.startsWith(q) || aliasMatches.any { it.startsWith(q) } -> 80
                nameLower.contains(q) || aliasMatches.any { it.contains(q) } -> 60
                else -> 0
            }
            if (score > 0) {
                scored.add(score to AtlasSearchHit.Country(c))
            }
        }

        for (city in data.cities) {
            val nameLower = city.name.lowercase(Locale.ROOT)
            val countryLower = city.country.lowercase(Locale.ROOT)
            val score = when {
                nameLower == q -> 95 + if (city.isCapital) 3 else 0
                nameLower.startsWith(q) -> 75 + if (city.isCapital) 3 else 0
                nameLower.contains(q) -> 55 + if (city.isCapital) 3 else 0
                countryLower.startsWith(q) -> 35 + if (city.isCapital) 5 else 0
                else -> 0
            }
            if (score > 0) {
                scored.add(score to AtlasSearchHit.City(city))
            }
        }

        return scored
            .sortedWith(compareByDescending<Pair<Int, AtlasSearchHit>> { it.first }.thenBy { it.second.label })
            .take(limit)
            .map { it.second }
    }

    /**
     * Zero-boxing streaming JSON scanner built for fast startup of 400k+ delta-encoded integer coordinates.
     */
    private class FastJsonScanner(private val s: String) {
        private var pos = 0
        private val len = s.length
        private var scratch = FloatArray(16384)
        private val sb = StringBuilder(64)

        private fun skipWs() {
            while (pos < len) {
                val c = s[pos]
                if (c == ' ' || c == '\n' || c == '\r' || c == '\t') pos++ else break
            }
        }

        fun peekChar(): Char {
            skipWs()
            return if (pos < len) s[pos] else '\u0000'
        }

        private fun expect(ch: Char) {
            skipWs()
            if (pos >= len || s[pos] != ch) {
                error("Expected '$ch' at $pos")
            }
            pos++
        }

        inline fun readObject(onField: (String) -> Unit) {
            expect('{')
            if (peekChar() == '}') {
                pos++
                return
            }
            while (true) {
                val key = readString()
                expect(':')
                onField(key)
                if (peekChar() == ',') {
                    pos++
                } else {
                    expect('}')
                    break
                }
            }
        }

        inline fun readArray(onElement: () -> Unit) {
            expect('[')
            if (peekChar() == ']') {
                pos++
                return
            }
            while (true) {
                onElement()
                if (peekChar() == ',') {
                    pos++
                } else {
                    expect(']')
                    break
                }
            }
        }

        fun readString(): String {
            expect('"')
            val start = pos
            while (pos < len) {
                val c = s[pos]
                if (c == '"') {
                    val out = s.substring(start, pos)
                    pos++
                    return out
                }
                if (c == '\\') {
                    return readEscapedString(start)
                }
                pos++
            }
            error("Unterminated string at $start")
        }

        private fun readEscapedString(start: Int): String {
            sb.setLength(0)
            sb.append(s, start, pos)
            while (pos < len) {
                val c = s[pos++]
                if (c == '"') return sb.toString()
                if (c == '\\') {
                    val esc = s[pos++]
                    when (esc) {
                        '"', '\\', '/' -> sb.append(esc)
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            val code = s.substring(pos, pos + 4).toInt(16)
                            sb.append(code.toChar())
                            pos += 4
                        }
                        else -> sb.append(esc)
                    }
                } else {
                    sb.append(c)
                }
            }
            error("Unterminated string at $start")
        }

        fun readLong(): Long {
            skipWs()
            var neg = false
            if (pos < len && s[pos] == '-') {
                neg = true
                pos++
            }
            var acc = 0L
            while (pos < len) {
                val d = s[pos] - '0'
                if (d !in 0..9) break
                acc = acc * 10L + d
                pos++
            }
            return if (neg) -acc else acc
        }

        fun readDouble(): Double {
            skipWs()
            val start = pos
            while (pos < len) {
                val c = s[pos]
                if (c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E' || c in '0'..'9') {
                    pos++
                } else {
                    break
                }
            }
            return s.substring(start, pos).toDouble()
        }

        fun readBoolean(): Boolean {
            skipWs()
            return if (s.startsWith("true", pos)) {
                pos += 4
                true
            } else if (s.startsWith("false", pos)) {
                pos += 5
                false
            } else {
                error("Invalid boolean at $pos")
            }
        }

        fun skipValue() {
            when (peekChar()) {
                '{' -> readObject { skipValue() }
                '[' -> readArray { skipValue() }
                '"' -> readString()
                't' -> { pos += 4 }
                'f' -> { pos += 5 }
                'n' -> { pos += 4 }
                else -> readDouble()
            }
        }

        fun readRingCoords(qScale: Double): AtlasRing? {
            expect('[')
            if (peekChar() == ']') {
                pos++
                return null
            }
            var count = 0
            var minX = Float.POSITIVE_INFINITY
            var minY = Float.POSITIVE_INFINITY
            var maxX = Float.NEGATIVE_INFINITY
            var maxY = Float.NEGATIVE_INFINITY

            if (qScale > 0.0) {
                var curQx = 0L
                var curQy = 0L
                while (true) {
                    curQx += readLong()
                    expect(',')
                    curQy += readLong()
                    val mx = lonToMercX(curQx / qScale)
                    val my = latToMercY(curQy / qScale)
                    if (count * 2 + 1 >= scratch.size) {
                        scratch = scratch.copyOf(scratch.size * 2)
                    }
                    scratch[count * 2] = mx
                    scratch[count * 2 + 1] = my
                    if (mx < minX) minX = mx
                    if (mx > maxX) maxX = mx
                    if (my < minY) minY = my
                    if (my > maxY) maxY = my
                    count++

                    val next = peekChar()
                    if (next == ',') {
                        pos++
                    } else {
                        expect(']')
                        break
                    }
                }
            } else {
                while (true) {
                    val lon = readDouble()
                    expect(',')
                    val lat = readDouble()
                    val mx = lonToMercX(lon)
                    val my = latToMercY(lat)
                    if (count * 2 + 1 >= scratch.size) {
                        scratch = scratch.copyOf(scratch.size * 2)
                    }
                    scratch[count * 2] = mx
                    scratch[count * 2 + 1] = my
                    if (mx < minX) minX = mx
                    if (mx > maxX) maxX = mx
                    if (my < minY) minY = my
                    if (my > maxY) maxY = my
                    count++

                    val next = peekChar()
                    if (next == ',') {
                        pos++
                    } else {
                        expect(']')
                        break
                    }
                }
            }

            if (count < 3) return null
            return AtlasRing(
                coords = scratch.copyOf(count * 2),
                minX = minX,
                minY = minY,
                maxX = maxX,
                maxY = maxY
            )
        }
    }
}
