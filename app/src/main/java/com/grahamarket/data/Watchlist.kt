package com.grahamarket.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/**
 * One watchlist entry. Records what the user asked to track:
 *  - the symbol,
 *  - the date it was added,
 *  - the price at the moment it was added (priceWhenAdded),
 *  - the currency.
 * The "current market price" is fetched live when the Watchlist screen opens
 * and is NOT stored here (it changes constantly).
 */
data class WatchItem(
    val symbol: String,
    val addedAtMillis: Long,
    val priceWhenAdded: Double,
    val currency: String
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("symbol", symbol)
        put("addedAt", addedAtMillis)
        put("priceWhenAdded", priceWhenAdded)
        put("currency", currency)
    }

    companion object {
        fun fromJson(o: JSONObject): WatchItem = WatchItem(
            symbol = o.getString("symbol"),
            addedAtMillis = o.getLong("addedAt"),
            priceWhenAdded = o.getDouble("priceWhenAdded"),
            currency = o.optString("currency", "INR")
        )
    }
}

private val Context.watchlistDataStore by preferencesDataStore(name = "grahamarket_watchlist")

/**
 * Persists the watchlist as a JSON array string in DataStore, so it survives app
 * restarts and process death. Adds are de-duplicated by symbol.
 */
class WatchlistStore(private val context: Context) {

    private val key = stringPreferencesKey("items_json")

    val items: Flow<List<WatchItem>> = context.watchlistDataStore.data.map { prefs ->
        parse(prefs[key])
    }

    suspend fun add(item: WatchItem) {
        context.watchlistDataStore.edit { prefs ->
            val current = parse(prefs[key]).toMutableList()
            // Replace any existing entry for the same symbol (keeps it unique).
            current.removeAll { it.symbol.equals(item.symbol, ignoreCase = true) }
            current.add(0, item)
            prefs[key] = serialize(current)
        }
    }

    suspend fun remove(symbol: String) {
        context.watchlistDataStore.edit { prefs ->
            val current = parse(prefs[key]).toMutableList()
            current.removeAll { it.symbol.equals(symbol, ignoreCase = true) }
            prefs[key] = serialize(current)
        }
    }

    private fun parse(raw: String?): List<WatchItem> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { WatchItem.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun serialize(items: List<WatchItem>): String {
        val arr = JSONArray()
        items.forEach { arr.put(it.toJson()) }
        return arr.toString()
    }
}
