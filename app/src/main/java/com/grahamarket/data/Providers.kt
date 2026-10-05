package com.grahamarket.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private val http: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(10, TimeUnit.SECONDS)
    .build()

private fun get(url: String, headers: Map<String, String> = emptyMap()): String {
    val builder = Request.Builder().url(url)
        .header("User-Agent", "GrahaMarket/1.0 (educational)")
    headers.forEach { (k, v) -> builder.header(k, v) }
    http.newCall(builder.build()).execute().use { resp ->
        val body = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) throw RuntimeException("HTTP ${resp.code}")
        return body
    }
}

/**
 * Default provider — Yahoo Finance's public quote endpoint. No API key required.
 * NSE symbols use the ".NS" suffix (e.g. RELIANCE.NS, TCS.NS); BSE uses ".BO".
 * This is an unofficial endpoint and may change; the Settings menu lets users
 * switch to a keyed provider for reliability.
 */
class YahooProvider : PriceProvider {
    override val type = ProviderType.YAHOO
    override suspend fun fetchQuote(symbol: String, apiKey: String?): QuoteResult =
        withContext(Dispatchers.IO) {
            try {
                val sym = normalizeNse(symbol)
                val url = "https://query1.finance.yahoo.com/v8/finance/chart/$sym?interval=1d&range=1d"
                val json = JSONObject(get(url))
                val result = json.getJSONObject("chart").getJSONArray("result").getJSONObject(0)
                val meta = result.getJSONObject("meta")
                val price = meta.getDouble("regularMarketPrice")
                val prevClose = meta.optDouble("chartPreviousClose", meta.optDouble("previousClose", price))
                val currency = meta.optString("currency", "INR")
                val changePct = if (prevClose != 0.0) (price - prevClose) / prevClose * 100.0 else null
                QuoteResult.Success(Quote(sym, price, currency, changePct, "Yahoo Finance"))
            } catch (e: Exception) {
                QuoteResult.Error("Yahoo lookup failed: ${e.message}")
            }
        }
}

/** Twelve Data — free API key, supports NSE symbols. */
class TwelveDataProvider : PriceProvider {
    override val type = ProviderType.TWELVE_DATA
    override suspend fun fetchQuote(symbol: String, apiKey: String?): QuoteResult =
        withContext(Dispatchers.IO) {
            if (apiKey.isNullOrBlank()) return@withContext QuoteResult.Error("Twelve Data API key required (set it in Settings).")
            try {
                val sym = normalizeNse(symbol)
                val url = "https://api.twelvedata.com/quote?symbol=$sym&apikey=$apiKey"
                val json = JSONObject(get(url))
                if (json.has("code") && json.optInt("code") >= 400) {
                    return@withContext QuoteResult.Error(json.optString("message", "Twelve Data error"))
                }
                val price = json.getString("close").toDouble()
                val changePct = json.optString("percent_change").toDoubleOrNull()
                val currency = json.optString("currency", "INR")
                QuoteResult.Success(Quote(sym, price, currency, changePct, "Twelve Data"))
            } catch (e: Exception) {
                QuoteResult.Error("Twelve Data lookup failed: ${e.message}")
            }
        }
}

/** Finnhub — free API key. US-centric but kept as an alternate provider. */
class FinnhubProvider : PriceProvider {
    override val type = ProviderType.FINNHUB
    override suspend fun fetchQuote(symbol: String, apiKey: String?): QuoteResult =
        withContext(Dispatchers.IO) {
            if (apiKey.isNullOrBlank()) return@withContext QuoteResult.Error("Finnhub API key required (set it in Settings).")
            try {
                val sym = symbol.uppercase()
                val url = "https://finnhub.io/api/v1/quote?symbol=$sym&token=$apiKey"
                val json = JSONObject(get(url))
                val price = json.getDouble("c")
                if (price == 0.0) return@withContext QuoteResult.Error("No data for '$sym' on Finnhub.")
                val changePct = json.optDouble("dp").takeIf { !it.isNaN() }
                QuoteResult.Success(Quote(sym, price, "USD", changePct, "Finnhub"))
            } catch (e: Exception) {
                QuoteResult.Error("Finnhub lookup failed: ${e.message}")
            }
        }
}

/** Default to NSE (.NS) if the user typed a bare Indian ticker. */
private fun normalizeNse(symbol: String): String {
    val s = symbol.trim().uppercase()
    return if (s.contains(".")) s else "$s.NS"
}

/** Factory used by the repository to resolve the active provider. */
object ProviderFactory {
    fun create(type: ProviderType): PriceProvider = when (type) {
        ProviderType.YAHOO -> YahooProvider()
        ProviderType.TWELVE_DATA -> TwelveDataProvider()
        ProviderType.FINNHUB -> FinnhubProvider()
    }
}
