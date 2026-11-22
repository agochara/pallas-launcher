package com.pallas.launcher.platform

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Silently caches conversion rates for 160+ world currencies (and major crypto) relative to USD.
 *
 * - Keeps an in-memory [ConcurrentHashMap] pre-seeded with baseline rates so conversions are
 *   0ms synchronous on every keystroke and work offline or in unit tests.
 * - Persists the latest fetched rates (~3–4 KB JSON) in SharedPreferences ("pallas_currency_rates").
 * - Silently refreshes in the background when the cache is older than [CACHE_TTL_MS] (6 hours),
 *   falling back across multiple free, keyless endpoints and Yahoo Finance.
 */
object CurrencyRateService {

    private const val PREFS_NAME = "pallas_currency_rates"
    private const val KEY_RATES_JSON = "rates_usd_v1"
    private const val KEY_UPDATED_AT = "updated_at_ms"

    /** Refresh silently at most once every 6 hours. */
    const val CACHE_TTL_MS = 6L * 3600L * 1000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isRefreshing = AtomicBoolean(false)

    @Volatile
    private var prefs: SharedPreferences? = null

    @Volatile
    var lastUpdatedMs: Long = 0L
        private set

    /**
     * Baseline fallback rates (units per 1 USD) so offline / first-boot / unit-test conversions
     * work instantaneously even before the first silent network sync completes.
     */
    private val BASELINE_USD_RATES: Map<String, Double> = mapOf(
        "USD" to 1.0,
        "EUR" to 0.92,
        "GBP" to 0.79,
        "INR" to 84.40,
        "JPY" to 154.50,
        "CAD" to 1.39,
        "AUD" to 1.53,
        "CHF" to 0.88,
        "CNY" to 7.24,
        "HKD" to 7.78,
        "SGD" to 1.34,
        "NZD" to 1.69,
        "KRW" to 1395.0,
        "SEK" to 10.85,
        "NOK" to 11.05,
        "DKK" to 6.87,
        "PLN" to 4.02,
        "CZK" to 23.40,
        "HUF" to 376.0,
        "RON" to 4.58,
        "BGN" to 1.80,
        "ISK" to 138.0,
        "TRY" to 34.50,
        "ILS" to 3.74,
        "ZAR" to 18.10,
        "BRL" to 5.78,
        "MXN" to 20.30,
        "ARS" to 995.0,
        "CLP" to 975.0,
        "COP" to 4400.0,
        "PEN" to 3.78,
        "UYU" to 42.50,
        "AED" to 3.6725,
        "SAR" to 3.75,
        "QAR" to 3.64,
        "KWD" to 0.307,
        "BHD" to 0.376,
        "OMR" to 0.385,
        "JOD" to 0.709,
        "EGP" to 49.30,
        "MAD" to 9.95,
        "NGN" to 1670.0,
        "KES" to 129.0,
        "GHS" to 16.20,
        "TZS" to 2650.0,
        "UGX" to 3680.0,
        "THB" to 34.60,
        "MYR" to 4.46,
        "IDR" to 15850.0,
        "PHP" to 58.70,
        "VND" to 25380.0,
        "TWD" to 32.50,
        "PKR" to 278.0,
        "BDT" to 119.50,
        "LKR" to 292.0,
        "NPR" to 135.0,
        "RUB" to 98.0,
        "UAH" to 41.30,
        "KZT" to 495.0,
        "UZS" to 12800.0,
        "GEL" to 2.73,
        "AMD" to 388.0,
        "AZN" to 1.70,
        "BYN" to 3.27,
        "MDL" to 17.90,
        "ALL" to 92.50,
        "MKD" to 56.80,
        "RSD" to 108.0,
        "BAM" to 1.80,
        "CRC" to 512.0,
        "DOP" to 60.20,
        "GTQ" to 7.72,
        "HNL" to 25.10,
        "NIO" to 36.80,
        "PAB" to 1.0,
        "PYG" to 7800.0,
        "BOB" to 6.91,
        "VES" to 45.0,
        "JMD" to 158.0,
        "TTD" to 6.78,
        "BBD" to 2.0,
        "BSD" to 1.0,
        "BZD" to 2.0,
        "HTG" to 131.0,
        "XCD" to 2.70,
        "AWG" to 1.79,
        "ANG" to 1.79,
        "KYD" to 0.83,
        "BMD" to 1.0,
        "DZD" to 134.0,
        "TND" to 3.13,
        "LYD" to 4.86,
        "ETB" to 122.0,
        "ZMW" to 27.30,
        "BWP" to 13.60,
        "MUR" to 46.50,
        "NAD" to 18.10,
        "SCR" to 13.60,
        "MZN" to 63.90,
        "AOA" to 915.0,
        "XOF" to 605.0,
        "XAF" to 605.0,
        "CDF" to 2850.0,
        "RWF" to 1370.0,
        "MWK" to 1735.0,
        "LBP" to 89500.0,
        "IQD" to 1310.0,
        "IRR" to 42000.0,
        "SYP" to 13000.0,
        "YER" to 250.0,
        "AFN" to 68.50,
        "MMK" to 2100.0,
        "KHR" to 4060.0,
        "LAK" to 21900.0,
        "MNT" to 3400.0,
        "BND" to 1.34,
        "MOP" to 8.01,
        "MVR" to 15.40,
        "FJD" to 2.26,
        "PGK" to 3.98,
        "XPF" to 110.0,
        "XAU" to 0.00038,
        "XAG" to 0.032,
        "BTC" to 0.0000112,
        "ETH" to 0.00032,
        "SOL" to 0.0048
    )

    private val ratesPerUsd = ConcurrentHashMap<String, Double>(BASELINE_USD_RATES)

    fun init(context: Context) {
        val appContext = context.applicationContext
        if (prefs == null) {
            synchronized(this) {
                if (prefs == null) {
                    val sp = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    prefs = sp
                    loadFromPrefs(sp)
                }
            }
        }
        if (isStale()) {
            refreshInBackground()
        }
    }

    private fun loadFromPrefs(sp: SharedPreferences) {
        val raw = sp.getString(KEY_RATES_JSON, null) ?: return
        val stamp = sp.getLong(KEY_UPDATED_AT, 0L)
        try {
            val obj = JSONObject(raw)
            val loaded = parseJsonRatesMap(obj)
            if (loaded.isNotEmpty()) {
                ratesPerUsd.putAll(loaded)
                ratesPerUsd["USD"] = 1.0
                lastUpdatedMs = stamp
            }
        } catch (_: Exception) {
        }
    }

    private fun saveToPrefs(rates: Map<String, Double>, timestamp: Long) {
        val sp = prefs ?: return
        try {
            val obj = JSONObject()
            for ((code, rate) in rates) {
                if (rate.isFinite() && rate > 0.0) {
                    obj.put(code, rate)
                }
            }
            sp.edit()
                .putString(KEY_RATES_JSON, obj.toString())
                .putLong(KEY_UPDATED_AT, timestamp)
                .apply()
        } catch (_: Exception) {
        }
    }

    fun isStale(nowMs: Long = System.currentTimeMillis()): Boolean {
        val stamp = lastUpdatedMs
        return stamp <= 0L || (nowMs - stamp) > CACHE_TTL_MS
    }

    fun getRatePerUsd(currencyCode: String): Double? {
        val code = currencyCode.uppercase(Locale.ROOT)
        return ratesPerUsd[code]
    }

    fun hasCurrency(currencyCode: String): Boolean {
        return ratesPerUsd.containsKey(currencyCode.uppercase(Locale.ROOT))
    }

    fun convert(amount: Double, fromCode: String, toCode: String): Double? {
        val from = fromCode.uppercase(Locale.ROOT)
        val to = toCode.uppercase(Locale.ROOT)
        if (from == to) return amount
        val fromRate = ratesPerUsd[from] ?: return null
        val toRate = ratesPerUsd[to] ?: return null
        if (fromRate <= 0.0 || toRate <= 0.0) return null
        return amount * (toRate / fromRate)
    }

    fun updateRatesForTest(customRates: Map<String, Double>, timestampMs: Long = System.currentTimeMillis()) {
        for ((k, v) in customRates) {
            if (v.isFinite() && v > 0.0) {
                ratesPerUsd[k.uppercase(Locale.ROOT)] = v
            }
        }
        ratesPerUsd["USD"] = 1.0
        lastUpdatedMs = timestampMs
    }

    fun resetToBaselineForTest() {
        ratesPerUsd.clear()
        ratesPerUsd.putAll(BASELINE_USD_RATES)
        lastUpdatedMs = 0L
    }

    /** Triggers a silent, non-blocking refresh if not already in flight. */
    fun refreshInBackground(onUpdated: (() -> Unit)? = null) {
        if (!isRefreshing.compareAndSet(false, true)) return
        scope.launch {
            try {
                val updated = fetchAndCacheRates()
                if (updated && onUpdated != null) {
                    withContext(Dispatchers.Main) {
                        onUpdated()
                    }
                }
            } finally {
                isRefreshing.set(false)
            }
        }
    }

    /**
     * Fetches the latest USD conversion table from free bulk endpoints (with fallback chain)
     * and persists it silently. Returns true if new rates were stored.
     */
    suspend fun fetchAndCacheRates(): Boolean = withContext(Dispatchers.IO) {
        val merged = HashMap<String, Double>()

        // 1. Primary: ExchangeRate-API Open Access (160+ ISO fiat currencies)
        fetchOpenErApi()?.let { merged.putAll(it) }

        // 2. Secondary / Crypto supplement: fawazahmed0 Currency API (170+ fiat & crypto like BTC/ETH/SOL)
        if (merged.isEmpty() || !merged.containsKey("BTC")) {
            fetchCurrencyApiPages()?.let { apiRates ->
                for ((k, v) in apiRates) {
                    if (!merged.containsKey(k)) merged[k] = v
                }
            }
        }

        // 3. Fallback: Frankfurter (European Central Bank reference rates)
        if (merged.isEmpty()) {
            fetchFrankfurterApi()?.let { merged.putAll(it) }
        }

        if (merged.isNotEmpty()) {
            merged["USD"] = 1.0
            ratesPerUsd.putAll(merged)
            val now = System.currentTimeMillis()
            lastUpdatedMs = now
            saveToPrefs(ratesPerUsd, now)
            true
        } else {
            false
        }
    }

    /**
     * Fallback via Yahoo Finance chart API for a specific currency pair (e.g. USDINR=X or BTC-USD)
     * when bulk endpoints are unreachable or an uncached ticker is queried.
     */
    suspend fun fetchYahooPairRate(fromCode: String, toCode: String): Double? = withContext(Dispatchers.IO) {
        val from = fromCode.uppercase(Locale.ROOT)
        val to = toCode.uppercase(Locale.ROOT)
        if (from == to) return@withContext 1.0

        val symbol = when {
            from in CRYPTO_CODES || to in CRYPTO_CODES -> "$from-$to"
            from == "USD" -> "$to=X"
            else -> "$from$to=X"
        }
        val url = "https://query1.finance.yahoo.com/v8/finance/chart/$symbol?interval=1d&range=1d"
        val body = httpGetJson(url) ?: return@withContext null
        try {
            val meta = JSONObject(body)
                .getJSONObject("chart")
                .getJSONArray("result")
                .getJSONObject(0)
                .getJSONObject("meta")
            val price = meta.optDouble("regularMarketPrice", Double.NaN)
            if (price.isFinite() && price > 0.0) {
                if (from == "USD") {
                    ratesPerUsd[to] = price
                    saveToPrefs(ratesPerUsd, System.currentTimeMillis())
                } else if (to == "USD") {
                    ratesPerUsd[from] = 1.0 / price
                    saveToPrefs(ratesPerUsd, System.currentTimeMillis())
                }
                price
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private val CRYPTO_CODES = setOf("BTC", "ETH", "SOL", "DOGE", "XRP", "ADA", "DOT", "LTC", "BNB")

    private fun fetchOpenErApi(): Map<String, Double>? {
        val body = httpGetJson("https://open.er-api.com/v6/latest/USD") ?: return null
        return try {
            val root = JSONObject(body)
            val ratesObj = root.optJSONObject("rates") ?: return null
            parseJsonRatesMap(ratesObj)
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchCurrencyApiPages(): Map<String, Double>? {
        val urls = listOf(
            "https://latest.currency-api.pages.dev/v1/currencies/usd.min.json",
            "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.min.json"
        )
        for (url in urls) {
            val body = httpGetJson(url) ?: continue
            try {
                val root = JSONObject(body)
                val usdObj = root.optJSONObject("usd") ?: continue
                val map = parseJsonRatesMap(usdObj)
                if (map.isNotEmpty()) return map
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun fetchFrankfurterApi(): Map<String, Double>? {
        val body = httpGetJson("https://api.frankfurter.app/latest?from=USD") ?: return null
        return try {
            val root = JSONObject(body)
            val ratesObj = root.optJSONObject("rates") ?: return null
            parseJsonRatesMap(ratesObj)
        } catch (_: Exception) {
            null
        }
    }

    internal fun parseJsonRatesMap(obj: JSONObject): Map<String, Double> {
        val result = HashMap<String, Double>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val v = obj.optDouble(k, Double.NaN)
            if (v.isFinite() && v > 0.0) {
                result[k.uppercase(Locale.ROOT)] = v
            }
        }
        return result
    }

    private fun httpGetJson(urlStr: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                )
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else null
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }
}
