package com.grahamarket.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grahamarket.data.ProviderType

@Composable
fun SettingsScreen(
    current: ProviderType,
    apiKey: String,
    onSelectProvider: (ProviderType) -> Unit,
    onApiKeyChange: (String) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = StarGold
        )

        SectionCard(title = "Price data provider") {
            Spacer(Modifier.height(8.dp))
            Text(
                "Choose where live prices come from. The default needs no key.",
                color = MutedText,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            ProviderType.entries.forEach { type ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(selected = type == current, onClick = { onSelectProvider(type) })
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = type == current, onClick = { onSelectProvider(type) })
                    Spacer(Modifier.height(0.dp))
                    Column(Modifier.padding(start = 8.dp)) {
                        Text(type.displayName, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            if (type.needsKey) "Requires a free API key" else "No key required",
                            color = MutedText,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        if (current.needsKey) {
            SectionCard(title = "${current.displayName} — API key") {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = onApiKeyChange,
                    label = { Text("Paste your API key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Stored privately on this device. Get a free key from the provider's website.",
                    color = MutedText,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        DisclaimerBanner()
    }
}
