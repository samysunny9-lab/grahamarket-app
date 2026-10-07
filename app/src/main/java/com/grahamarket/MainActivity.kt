package com.grahamarket

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.grahamarket.ui.DeeperScreen
import com.grahamarket.ui.GrahaMarketTheme
import com.grahamarket.ui.GrahaScreen
import com.grahamarket.ui.HomeScreen
import com.grahamarket.ui.MainViewModel
import com.grahamarket.ui.MutedText
import com.grahamarket.ui.NumerologyScreen
import com.grahamarket.ui.SettingsScreen
import com.grahamarket.ui.StarGold
import com.grahamarket.ui.WatchlistScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { GrahaMarketTheme { AppRoot() } }
    }
}

private data class Tab(val label: String, val icon: @Composable () -> Unit)

// Primary bottom-nav destinations.
private const val T_OUTLOOK = 0
private const val T_NUMEROLOGY = 1
private const val T_DEEPER = 2
private const val T_WATCHLIST = 3
private const val T_MORE = 4
// "More" sub-destinations.
private const val T_SKY = 10
private const val T_SETTINGS = 11

@Composable
private fun AppRoot(vm: MainViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var tab by remember { mutableIntStateOf(T_OUTLOOK) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.lastActionMessage) {
        state.lastActionMessage?.let {
            snackbar.showSnackbar(it)
            vm.clearMessage()
        }
    }

    val tabs = listOf(
        T_OUTLOOK to Tab("Outlook") { Icon(Icons.Filled.ShowChart, contentDescription = null) },
        T_NUMEROLOGY to Tab("Numerology") { Icon(Icons.Filled.Calculate, contentDescription = null) },
        T_DEEPER to Tab("Deeper+Check") { Icon(Icons.Filled.AutoGraph, contentDescription = null) },
        T_WATCHLIST to Tab("Watchlist") { Icon(Icons.Filled.Bookmarks, contentDescription = null) },
        T_MORE to Tab("More") { Icon(Icons.Filled.MoreHoriz, contentDescription = null) }
    )

    // Which primary item should appear selected (sub-screens map back to "More").
    val selectedPrimary = when (tab) {
        T_SKY, T_SETTINGS, T_MORE -> T_MORE
        else -> tab
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                tabs.forEach { (id, t) ->
                    NavigationBarItem(
                        selected = selectedPrimary == id,
                        onClick = {
                            tab = id
                            if (id == T_SKY) vm.refreshGrahas()
                        },
                        icon = t.icon,
                        label = { Text(t.label, maxLines = 1) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                T_OUTLOOK -> HomeScreen(
                    state = state,
                    onQueryChange = vm::onQueryChange,
                    onAnalyze = vm::analyze,
                    onAddToWatchlist = vm::addCurrentToWatchlist
                )
                T_NUMEROLOGY -> NumerologyScreen(
                    state = state,
                    onQueryChange = vm::onNumQueryChange,
                    onAnalyze = vm::analyzeNumerology
                )
                T_DEEPER -> DeeperScreen(
                    state = state,
                    onQueryChange = vm::onDeepQueryChange,
                    onAnalyze = vm::analyzeDeeper
                )
                T_WATCHLIST -> WatchlistScreen(
                    state = state,
                    onRefresh = vm::refreshWatchlist,
                    onRemove = vm::removeFromWatchlist
                )
                T_MORE -> MoreMenu(
                    onSky = { tab = T_SKY; vm.refreshGrahas() },
                    onSettings = { tab = T_SETTINGS }
                )
                T_SKY -> GrahaScreen(grahas = state.grahas)
                T_SETTINGS -> SettingsScreen(
                    current = state.provider,
                    apiKey = state.apiKey,
                    onSelectProvider = vm::setProvider,
                    onApiKeyChange = vm::setApiKey
                )
            }
        }
    }
}

@Composable
private fun MoreMenu(onSky: () -> Unit, onSettings: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("More", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, color = StarGold)
        Text("Additional screens.", color = MutedText, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onSky, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Public, contentDescription = null)
            Text("  Current Sky (Grahas)")
        }
        OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Settings, contentDescription = null)
            Text("  Settings (data provider)")
        }
    }
}
