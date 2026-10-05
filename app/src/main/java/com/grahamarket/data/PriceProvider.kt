package com.grahamarket.data

/** A live/last price quote for a symbol. */
data class Quote(
    val symbol: String,
    val price: Double,
    val currency: String,
    val changePercent: Double?,
    val source: String
)

sealed class QuoteResult {
    data class Success(val quote: Quote) : QuoteResult()
    data class Error(val message: String) : QuoteResult()
}

/** Identifies the configurable price providers available in the Settings menu. */
enum class ProviderType(val id: String, val displayName: String, val needsKey: Boolean) {
    YAHOO("yahoo", "Yahoo Finance (no key, default)", false),
    TWELVE_DATA("twelvedata", "Twelve Data (free key)", true),
    FINNHUB("finnhub", "Finnhub (free key)", true);

    companion object {
        fun fromId(id: String?): ProviderType =
            entries.firstOrNull { it.id == id } ?: YAHOO
    }
}

/** Common interface — swapping providers is a single-line change at the call site. */
interface PriceProvider {
    val type: ProviderType
    suspend fun fetchQuote(symbol: String, apiKey: String?): QuoteResult
}
