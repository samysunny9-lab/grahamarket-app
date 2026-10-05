package com.grahamarket.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.grahamarket.astro.Graha
import com.grahamarket.astro.GrahaEngine
import com.grahamarket.astro.GrahaPosition
import com.grahamarket.data.MarketRepository
import com.grahamarket.data.ProviderType
import com.grahamarket.data.SettingsStore
import com.grahamarket.data.SymbolReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date

data class UiState(
    val query: String = "",
    val loading: Boolean = false,
    val report: SymbolReport? = null,
    val grahas: List<GrahaPosition> = emptyList(),
    val provider: ProviderType = ProviderType.YAHOO,
    val apiKey: String = ""
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = SettingsStore(app)
    private val repo = MarketRepository(settings)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    init {
        // Load current sky immediately so the Graha screen is populated.
        _state.value = _state.value.copy(grahas = GrahaEngine.allPositions(Date()))
        viewModelScope.launch {
            val p = settings.providerType.first()
            _state.value = _state.value.copy(provider = p, apiKey = settings.apiKeyFor(p).first())
        }
    }

    fun onQueryChange(q: String) {
        _state.value = _state.value.copy(query = q)
    }

    fun analyze() {
        val symbol = _state.value.query.trim()
        if (symbol.isEmpty()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            val report = repo.lookup(symbol)
            _state.value = _state.value.copy(loading = false, report = report)
        }
    }

    fun refreshGrahas() {
        _state.value = _state.value.copy(grahas = GrahaEngine.allPositions(Date()))
    }

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
