package com.grahamarket.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.grahamarket.astro.GrahaEngine
import com.grahamarket.astro.GrahaPosition
import com.grahamarket.data.MarketRepository
import com.grahamarket.data.NumerologyReport
import com.grahamarket.data.ProviderType
import com.grahamarket.data.Quote
import com.grahamarket.data.QuoteResult
import com.grahamarket.data.SettingsStore
import com.grahamarket.data.SymbolReport
import com.grahamarket.data.WatchItem
import com.grahamarket.data.WatchlistStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date

/** A watchlist row augmented with the freshly-fetched current price. */
data class WatchRow(
    val item: WatchItem,
    val currentPrice: Double?,
    val currentError: String?
) {
    val changePercent: Double?
        get() = currentPrice?.let { cur ->
            if (item.priceWhenAdded != 0.0) (cur - item.priceWhenAdded) / item.priceWhenAdded * 100.0 else null
        }
}

data class UiState(
    val query: String = "",
    val loading: Boolean = false,
    val report: SymbolReport? = null,
    val grahas: List<GrahaPosition> = emptyList(),
    val provider: ProviderType = ProviderType.YAHOO,
    val apiKey: String = "",
    // Numerology tab
    val numQuery: String = "",
    val numLoading: Boolean = false,
    val numReport: NumerologyReport? = null,
    // Watchlist tab
    val watchItems: List<WatchItem> = emptyList(),
    val watchRows: List<WatchRow> = emptyList(),
    val watchRefreshing: Boolean = false,
    val lastActionMessage: String? = null
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = SettingsStore(app)
    private val watchlist = WatchlistStore(app)
    private val repo = MarketRepository(settings)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    init {
        _state.value = _state.value.copy(grahas = GrahaEngine.allPositions(Date()))
        viewModelScope.launch {
            val p = settings.providerType.first()
            _state.value = _state.value.copy(provider = p, apiKey = settings.apiKeyFor(p).first())
        }
        // Observe the persisted watchlist; keep UI in sync across restarts.
        viewModelScope.launch {
            watchlist.items.collect { items ->
                _state.value = _state.value.copy(
                    watchItems = items,
                    // Preserve any already-fetched current prices for unchanged symbols.
                    watchRows = items.map { item ->
                        _state.value.watchRows.firstOrNull { it.item.symbol == item.symbol }
                            ?.copy(item = item)
                            ?: WatchRow(item, null, null)
                    }
                )
            }
        }
    }

    // ---- Outlook (graha) tab ----

    fun onQueryChange(q: String) { _state.value = _state.value.copy(query = q) }

    fun analyze() {
        val symbol = _state.value.query.trim()
        if (symbol.isEmpty()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            val report = repo.lookup(symbol)
            _state.value = _state.value.copy(loading = false, report = report)
        }
    }

    // ---- Numerology tab ----

    fun onNumQueryChange(q: String) { _state.value = _state.value.copy(numQuery = q) }

    fun analyzeNumerology() {
        val symbol = _state.value.numQuery.trim()
        if (symbol.isEmpty()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(numLoading = true)
            val report = repo.lookupNumerology(symbol)
            _state.value = _state.value.copy(numLoading = false, numReport = report)
        }
    }

    // ---- Sky tab ----

    fun refreshGrahas() {
        _state.value = _state.value.copy(grahas = GrahaEngine.allPositions(Date()))
    }

    // ---- Watchlist ----

    /** Adds the currently-analysed symbol (with its current price) to the watchlist. */
    fun addCurrentToWatchlist() {
        val report = _state.value.report ?: return
        val quote = report.quote ?: run {
            _state.value = _state.value.copy(lastActionMessage = "Can't add: no live price available.")
            return
        }
        viewModelScope.launch {
            watchlist.add(
                WatchItem(
                    symbol = quote.symbol,
                    addedAtMillis = System.currentTimeMillis(),
                    priceWhenAdded = quote.price,
                    currency = quote.currency
                )
            )
            _state.value = _state.value.copy(lastActionMessage = "${quote.symbol} added to watchlist.")
        }
    }

    fun addSymbolToWatchlist(symbol: String) {
        viewModelScope.launch {
            when (val r = repo.currentQuote(symbol)) {
                is QuoteResult.Success -> {
                    val q = r.quote
                    watchlist.add(WatchItem(q.symbol, System.currentTimeMillis(), q.price, q.currency))
                    _state.value = _state.value.copy(lastActionMessage = "${q.symbol} added.")
                }
                is QuoteResult.Error ->
                    _state.value = _state.value.copy(lastActionMessage = r.message)
            }
        }
    }

    fun removeFromWatchlist(symbol: String) {
        viewModelScope.launch {
            watchlist.remove(symbol)
            _state.value = _state.value.copy(lastActionMessage = "$symbol removed from watchlist.")
        }
    }

    /** Fetches the live price for every watchlist row. */
    fun refreshWatchlist() {
        val items = _state.value.watchItems
        if (items.isEmpty()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(watchRefreshing = true)
            val rows = items.map { item ->
                when (val r = repo.currentQuote(item.symbol)) {
                    is QuoteResult.Success -> WatchRow(item, r.quote.price, null)
                    is QuoteResult.Error -> WatchRow(item, null, r.message)
                }
            }
            _state.value = _state.value.copy(watchRows = rows, watchRefreshing = false)
        }
    }

    fun clearMessage() { _state.value = _state.value.copy(lastActionMessage = null) }

    fun isWatched(symbol: String): Boolean =
        _state.value.watchItems.any { it.symbol.equals(symbol, ignoreCase = true) ||
            it.symbol.equals(if (symbol.contains(".")) symbol else "$symbol.NS", ignoreCase = true) }

    // ---- Settings ----

    fun setProvider(type: ProviderType) {
        viewModelScope.launch {
            settings.setProvider(type)
            val key = settings.apiKeyFor(type).first()
            _state.value = _state.value.copy(provider = type, apiKey = key)
        }
    }

    fun setApiKey(key: String) {
        val type = _state.value.provider
        _state.value = _state.value.copy(apiKey = key)
        viewModelScope.launch { settings.setApiKey(type, key) }
    }
}
