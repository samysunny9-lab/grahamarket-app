package com.grahamarket.data

import com.grahamarket.astro.MarketScorer
import kotlinx.coroutines.flow.first
import java.util.Date

/** The combined result shown on the main screen: live price + graha outlook. */
data class SymbolReport(
    val quote: Quote?,
    val quoteError: String?,
    val outlook: MarketScorer.Outlook,
    val projectedPrice: Double?
)

/**
 * Orchestrates a lookup: resolves the active provider from settings, fetches the
 * quote, computes the astrological outlook, and derives a projected price.
 */
class MarketRepository(private val settings: SettingsStore) {

    suspend fun lookup(symbol: String): SymbolReport {
        val type = settings.providerType.first()
        val apiKey = settings.apiKeyFor(type).first()
        val provider = ProviderFactory.create(type)

        val result = provider.fetchQuote(symbol, apiKey)
        val outlook = MarketScorer.outlook(symbol, Date())

        return when (result) {
            is QuoteResult.Success -> {
                val projected = result.quote.price * (1 + outlook.estimatedPercent / 100.0)
                SymbolReport(result.quote, null, outlook, projected)
            }
            is QuoteResult.Error -> SymbolReport(null, result.message, outlook, null)
        }
    }
}
