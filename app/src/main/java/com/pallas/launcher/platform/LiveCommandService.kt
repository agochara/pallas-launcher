package com.pallas.launcher.platform

import android.net.Uri
import android.util.LruCache
import com.pallas.launcher.domain.search.LauncherSearchResult
import com.pallas.launcher.domain.search.SmartCommandType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.util.Locale
import kotlin.math.abs

object LiveCommandService {

    private val MARKET_ALIASES = mapOf(
        "nifty" to "^NSEI",
        "nifty50" to "^NSEI",
        "sensex" to "^BSESN",
        "banknifty" to "^NSEBANK",
        "indiavix" to "^INDIAVIX",
        "sp500" to "^GSPC",
        "spx" to "^GSPC",
        "nasdaq" to "^IXIC",
        "dow" to "^DJI",
        "vix" to "^VIX",
        "ftse" to "^FTSE",
        "nikkei" to "^N225",
        "gold" to "GC=F",
        "silver" to "SI=F",
        "brent" to "BZ=F",
        "oil" to "CL=F",
        "crude" to "CL=F",
        "usdinr" to "INR=X",
        "eurusd" to "EURUSD=X",
        "btc" to "BTC-USD",
        "eth" to "ETH-USD"
    )

    private data class HttpOutcome(val body: String?, val error: String?)

    private val responseCache = LruCache<String, Pair<Long, String>>(64)
    private const val CACHE_TTL_MS = 60_000L

    private fun httpGetDetailed(urlStr: String, attempts: Int = 2): HttpOutcome {
        responseCache.get(urlStr)?.let { (stamp, body) ->
            if (System.currentTimeMillis() - stamp < CACHE_TTL_MS) return HttpOutcome(body, null)
        }
        var lastError: String? = null
        repeat(attempts) {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                )
                setRequestProperty("Accept", "application/json,text/plain,*/*")
            }
            try {
                val code = conn.responseCode
                if (code in 200..299) {
                    val body = conn.inputStream.bufferedReader().use { r -> r.readText() }
                    responseCache.put(urlStr, System.currentTimeMillis() to body)
                    return HttpOutcome(body, null)
                }
                lastError = "HTTP $code"
                if (code in 400..499 && code != 429) return HttpOutcome(null, lastError)
            } catch (_: UnknownHostException) {
                lastError = "DNS failed (check Private DNS / ad blocker)"
            } catch (_: SocketTimeoutException) {
                lastError = "timed out"
            } catch (e: Exception) {
                lastError = e.javaClass.simpleName
            } finally {
                conn.disconnect()
            }
        }
        return HttpOutcome(null, lastError)
    }

    private fun httpGet(urlStr: String): String? = httpGetDetailed(urlStr).body

    suspend fun fetchMarketQuote(rawQuery: String): LauncherSearchResult.RichCard = withContext(Dispatchers.IO) {
        val clean = rawQuery.trim()
        val key = clean.lowercase(Locale.ROOT).replace("\\s+".toRegex(), "")
        val resolvedSymbol = MARKET_ALIASES[key] ?: resolveYahooSymbol(clean) ?: clean.uppercase(Locale.ROOT)
        val encoded = Uri.encode(resolvedSymbol)

        val primaryUrl = "https://query1.finance.yahoo.com/v8/finance/chart/$encoded?interval=1d&range=5d"
        val fallbackUrl = "https://query2.finance.yahoo.com/v8/finance/chart/$encoded?interval=1d&range=5d"

        val outcome = httpGetDetailed(primaryUrl, attempts = 1).let { first ->
            if (first.body != null) first else httpGetDetailed(fallbackUrl, attempts = 1)
        }
        val body = outcome.body
            ?: return@withContext LauncherSearchResult.RichCard(
                id = "fin_err_$clean",
                type = SmartCommandType.FINANCE,
                title = clean.uppercase(Locale.ROOT),
                subtitle = "Could not fetch quote (${outcome.error ?: "check ticker"})"
            )

        try {
            val result = JSONObject(body).getJSONObject("chart").getJSONArray("result").getJSONObject(0)
            val meta = result.getJSONObject("meta")
            val symbol = meta.optString("symbol", resolvedSymbol)
            val shortName = meta.optString("shortName", meta.optString("longName", symbol)).ifBlank { symbol }
            val currency = meta.optString("currency", "")
            val price = meta.optDouble("regularMarketPrice", Double.NaN)
            val prevClose = meta.optDouble("chartPreviousClose", meta.optDouble("previousClose", Double.NaN))
            val exchange = meta.optString("exchangeName", "")

            val closes = mutableListOf<Float>()
            val quoteArr = result.optJSONObject("indicators")?.optJSONArray("quote")?.optJSONObject(0)?.optJSONArray("close")
            if (quoteArr != null) {
                for (i in 0 until quoteArr.length()) {
                    val v = quoteArr.optDouble(i, Double.NaN)
                    if (!v.isNaN()) closes.add(v.toFloat())
                }
            }

            val curSymbol = when (currency.uppercase(Locale.ROOT)) {
                "INR" -> "₹"
                "USD" -> "$"
                "EUR" -> "€"
                "GBP" -> "£"
                "JPY" -> "¥"
                else -> if (currency.isNotEmpty()) "$currency " else ""
            }

            val change = if (!price.isNaN() && !prevClose.isNaN() && prevClose != 0.0) price - prevClose else 0.0
            val pct = if (!prevClose.isNaN() && prevClose != 0.0) (change / prevClose) * 100.0 else 0.0
            val sign = if (change >= 0) "+" else "−"
            val badge = String.format(Locale.US, "%s%.2f%%", sign, abs(pct))

            LauncherSearchResult.RichCard(
                id = "fin_$symbol",
                type = SmartCommandType.FINANCE,
                title = shortName,
                primaryValue = if (price.isNaN()) "—" else "$curSymbol${String.format(Locale.US, "%,.2f", price)}",
                badgeText = badge,
                isBadgePositive = change >= 0,
                subtitle = "$symbol • $exchange • Tap to open chart",
                sparkline = closes,
                execute = { ctx ->
                    SystemActionsManager.openUrl(ctx, "https://finance.yahoo.com/quote/${Uri.encode(symbol)}")
                }
            )
        } catch (_: Exception) {
            LauncherSearchResult.RichCard(
                id = "fin_err_$clean",
                type = SmartCommandType.FINANCE,
                title = clean.uppercase(Locale.ROOT),
                subtitle = "No market data found for \"$clean\""
            )
        }
    }

    private fun resolveYahooSymbol(query: String): String? {
        val searchUrl = "https://query2.finance.yahoo.com/v1/finance/search?q=${Uri.encode(query)}&quotesCount=4&newsCount=0"
        val json = httpGet(searchUrl) ?: return null
        return try {
            val quotes = JSONObject(json).optJSONArray("quotes") ?: return null
            if (quotes.length() == 0) null else quotes.getJSONObject(0).optString("symbol").takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchWeatherOrSolar(
        cityQuery: String,
        isSolarOnly: Boolean
    ): LauncherSearchResult.RichCard = withContext(Dispatchers.IO) {
        val city = cityQuery.trim()
        if (city.isEmpty()) {
            return@withContext LauncherSearchResult.RichCard(
                id = "weather_no_city",
                type = if (isSolarOnly) SmartCommandType.SOLAR else SmartCommandType.WEATHER,
                title = if (isSolarOnly) "Sunrise & Sunset" else "Weather",
                subtitle = "Pass a city (e.g. 'w london') or set a default city in Search Settings"
            )
        }

        val geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=${Uri.encode(city)}&count=1&language=en&format=json"
        val geoOutcome = httpGetDetailed(geoUrl)
        val loc = try {
            JSONObject(geoOutcome.body ?: "").optJSONArray("results")?.optJSONObject(0)
        } catch (_: Exception) {
            null
        } ?: return@withContext LauncherSearchResult.RichCard(
            id = "weather_err_$city",
            type = if (isSolarOnly) SmartCommandType.SOLAR else SmartCommandType.WEATHER,
            title = city,
            subtitle = if (geoOutcome.error != null) "Weather unreachable (${geoOutcome.error})" else "City not found • Check spelling"
        )

        val lat = loc.optDouble("latitude")
        val lon = loc.optDouble("longitude")
        val name = loc.optString("name", city)
        val country = loc.optString("country", "")
        val placeLabel = if (country.isNotBlank()) "$name, $country" else name

        val wxUrl = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,apparent_temperature,weather_code,wind_speed_10m" +
                "&daily=sunrise,sunset,temperature_2m_max,temperature_2m_min&timezone=auto&forecast_days=1"
        val wx = httpGetDetailed(wxUrl)
        val wxJson = wx.body ?: return@withContext LauncherSearchResult.RichCard(
            id = "weather_net_$city",
            type = if (isSolarOnly) SmartCommandType.SOLAR else SmartCommandType.WEATHER,
            title = placeLabel,
            subtitle = "Weather unreachable - ${wx.error ?: "unknown error"}"
        )

        try {
            val root = JSONObject(wxJson)
            val current = root.getJSONObject("current")
            val daily = root.getJSONObject("daily")
            val temp = current.optDouble("temperature_2m")
            val feels = current.optDouble("apparent_temperature")
            val code = current.optInt("weather_code", 0)
            val wind = current.optDouble("wind_speed_10m")

            val hi = daily.optJSONArray("temperature_2m_max")?.optDouble(0) ?: temp
            val lo = daily.optJSONArray("temperature_2m_min")?.optDouble(0) ?: temp
            val sunriseRaw = daily.optJSONArray("sunrise")?.optString(0, "")?.substringAfter("T") ?: "--:--"
            val sunsetRaw = daily.optJSONArray("sunset")?.optString(0, "")?.substringAfter("T") ?: "--:--"

            if (isSolarOnly) {
                LauncherSearchResult.RichCard(
                    id = "solar_$name",
                    type = SmartCommandType.SOLAR,
                    title = placeLabel,
                    primaryValue = "↑ $sunriseRaw   ↓ $sunsetRaw",
                    badgeText = wmoDescription(code),
                    isBadgePositive = true,
                    subtitle = "Sunrise $sunriseRaw • Sunset $sunsetRaw (local time)"
                )
            } else {
                LauncherSearchResult.RichCard(
                    id = "wx_$name",
                    type = SmartCommandType.WEATHER,
                    title = placeLabel,
                    primaryValue = String.format(Locale.US, "%.1f°C", temp),
                    badgeText = wmoDescription(code),
                    isBadgePositive = true,
                    subtitle = "Feels ${String.format(Locale.US, "%.0f°", feels)} • H:${String.format(Locale.US, "%.0f°", hi)} L:${String.format(Locale.US, "%.0f°", lo)} • Wind ${String.format(Locale.US, "%.0f", wind)} km/h • ☀ $sunriseRaw–$sunsetRaw"
                )
            }
        } catch (_: Exception) {
            LauncherSearchResult.RichCard(
                id = "weather_parse_$city",
                type = SmartCommandType.WEATHER,
                title = placeLabel,
                subtitle = "Unable to parse weather response"
            )
        }
    }

    suspend fun fetchWolframShortAnswer(query: String, appId: String): LauncherSearchResult.RichCard = withContext(Dispatchers.IO) {
        if (appId.isBlank()) {
            return@withContext LauncherSearchResult.RichCard(
                id = "wa_no_key",
                type = SmartCommandType.WOLFRAM,
                title = "Wolfram Alpha",
                subtitle = "Add your free Wolfram AppID in Search Settings to enable 'wa' queries"
            )
        }
        val url = "https://api.wolframalpha.com/v1/result?i=${Uri.encode(query)}&appid=${Uri.encode(appId)}"
        val outcome = httpGetDetailed(url)
        val answer = outcome.body
        if (answer != null) {
            LauncherSearchResult.RichCard(
                id = "wa_$query",
                type = SmartCommandType.WOLFRAM,
                title = query,
                primaryValue = answer,
                badgeText = "Wolfram",
                isBadgePositive = true,
                subtitle = "Tap to open full Wolfram|Alpha analysis",
                execute = { ctx ->
                    SystemActionsManager.openUrl(ctx, "https://www.wolframalpha.com/input?i=${Uri.encode(query)}")
                }
            )
        } else {
            LauncherSearchResult.RichCard(
                id = "wa_err_$query",
                type = SmartCommandType.WOLFRAM,
                title = query,
                subtitle = "Wolfram could not compute a short answer (${outcome.error ?: "no result"}) • Tap to open in browser",
                execute = { ctx ->
                    SystemActionsManager.openUrl(ctx, "https://www.wolframalpha.com/input?i=${Uri.encode(query)}")
                }
            )
        }
    }

    private fun wmoDescription(code: Int): String = when (code) {
        0 -> "Clear"
        1, 2, 3 -> "Partly Cloudy"
        45, 48 -> "Fog"
        51, 53, 55 -> "Drizzle"
        61, 63, 65 -> "Rain"
        71, 73, 75 -> "Snow"
        80, 81, 82 -> "Showers"
        95, 96, 99 -> "Thunderstorm"
        else -> "Cloudy"
    }
}