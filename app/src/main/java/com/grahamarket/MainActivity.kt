package com.grahamarket

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.grahamarket.ui.GrahaMarketTheme
import com.grahamarket.ui.GrahaScreen
import com.grahamarket.ui.HomeScreen
import com.grahamarket.ui.MainViewModel
import com.grahamarket.ui.NumerologyScreen
import com.grahamarket.ui.SettingsScreen
import com.grahamarket.ui.WatchlistScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            GrahaMarketTheme { AppRoot() }
        }
    }
}

private data class Tab(val label: String, val icon: @Composable () -> Unit)

@Composable
private fun AppRoot(vm: MainViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }

    // Surface one-off action messages (add/remove results) as a snackbar.
    LaunchedEffect(state.lastActionMessage) {
        state.lastActionMessage?.let {
            snackbar.showSnackbar(it)
            vm.clearMessage()
        }
    }

    val tabs = listOf(
        Tab("Outlook") { Icon(Icons.Filled.ShowChart, contentDescription = null) },
        Tab("Numerology") { Icon(Icons.Filled.Calculate, contentDescription = null) },
        Tab("Watchlist") { Icon(Icons.Filled.Bookmarks, contentDescription = null) },
        Tab("Sky") { Icon(Icons.Filled.Public, contentDescription = null) },
        Tab("Settings") { Icon(Icons.Filled.Settings, contentDescription = null) }
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = {
                            tab = i
                            if (i == 3) vm.refreshGrahas()
                        },
                        icon = t.icon,
                        label = { Text(t.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                0 -> HomeScreen(
                    state = state,
                    onQueryChange = vm::onQueryChange,
                    onAnalyze = vm::analyze,
                    onAddToWatchlist = vm::addCurrentToWatchlist
                )
                1 -> NumerologyScreen(
                    state = state,
                    onQueryChange = vm::onNumQueryChange,
                    onAnalyze = vm::analyzeNumerology
                )
                2 -> WatchlistScreen(
                    state = state,
                    onRefresh = vm::refreshWatchlist,
                    onRemove = vm::removeFromWatchlist
                )
                3 -> GrahaScreen(grahas = state.grahas)
                else -> SettingsScreen(
                    current = state.provider,
                    apiKey = state.apiKey,
                    onSelectProvider = vm::setProvider,
                    onApiKeyChange = vm::setApiKey
                )
            }
        }
    }
}
