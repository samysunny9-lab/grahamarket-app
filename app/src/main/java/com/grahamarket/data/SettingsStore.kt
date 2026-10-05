package com.grahamarket.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "grahamarket_settings")

/** Persists the chosen price provider and its API key across launches. */
class SettingsStore(private val context: Context) {

    private val providerKey = stringPreferencesKey("provider_type")
    private fun keyFor(type: ProviderType) = stringPreferencesKey("apikey_${type.id}")

    val providerType: Flow<ProviderType> = context.dataStore.data
        .map { ProviderType.fromId(it[providerKey]) }

    fun apiKeyFor(type: ProviderType): Flow<String> = context.dataStore.data
        .map { it[keyFor(type)] ?: "" }

    suspend fun setProvider(type: ProviderType) {
        context.dataStore.edit { it[providerKey] = type.id }
    }

    suspend fun setApiKey(type: ProviderType, key: String) {
        context.dataStore.edit { it[keyFor(type)] = key }
    }
}
