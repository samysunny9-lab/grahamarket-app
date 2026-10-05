package com.grahamarket.data

import com.grahamarket.astro.ForecastEngine
import com.grahamarket.astro.MarketScorer
import com.grahamarket.astro.NumerologyEngine
import kotlinx.coroutines.flow.first
import java.util.Date

/**
 * The combined result shown on the main screen: live price + graha outlook +
 * 7-day and 30-day day-by-day forecasts.
 */
data class SymbolReport(
    val quote: Quote?,
    val quoteError: String?,
    val outlook: MarketScorer.Outlook,
    val projectedPrice: Double?,
    val forecast7: ForecastEngine.HorizonForecast,
    val forecast30: ForecastEngine.HorizonForecast
)

/** Numerology counterpart of SymbolReport. */
data class NumerologyReport(
    val quote: Quote?,
    val quoteError: String?,
    val reading: NumerologyEngine.NumerologyReading,
    val horizon7: NumerologyEngine.NumerologyHorizon,
    val horizon30: NumerologyEngine.NumerologyHorizon
)

/**
 * Orchestrates lookups: resolves the active provider from settings, fetches the
 * quote, and computes the astrological / numerological forecasts.
 */
class MarketRepository(private val settings: SettingsStore) {

    private suspend fun quote(symbol: String): QuoteResult {
        val type = settings.providerType.first()
        val apiKey = settings.apiKeyFor(type).first()
        return ProviderFactory.create(type).fetchQuote(symbol, apiKey)
    }

    /** Graha-based report with 7-day and 30-day day-by-day forecasts. */
    suspend fun lookup(symbol: String): SymbolReport {
        val now = Date()
        val result = quote(symbol)
        val outlook = MarketScorer.outlook(symbol, now)
        val f7 = ForecastEngine.horizon(symbol, 7, now)
        val f30 = ForecastEngine.horizon(symbol, 30, now)

        return when (result) {
            is QuoteResult.Success -> {
                val projected = result.quote.price * (1 + outlook.estimatedPercent / 100.0)
                SymbolReport(result.quote, null, outlook, projected, f7, f30)
            }
            is QuoteResult.Error -> SymbolReport(null, result.message, outlook, null, f7, f30)
        }
    }

    /** Numerology-based report with 7-day and 30-day day-by-day forecasts. */
    suspend fun lookupNumerology(symbol: String): NumerologyReport {
        val now = Date()
        val result = quote(symbol)
        val reading = NumerologyEngine.reading(symbol, now)
        val h7 = NumerologyEngine.horizon(symbol, 7, now)
        val h30 = NumerologyEngine.horizon(symbol, 30, now)
        return when (result) {
            is QuoteResult.Success -> NumerologyReport(result.quote, null, reading, h7, h30)
            is QuoteResult.Error -> NumerologyReport(null, result.message, reading, h7, h30)
        }
    }

    /** Just the live price (used to refresh watchlist rows). */
    suspend fun currentQuote(symbol: String): QuoteResult = quote(symbol)
}
