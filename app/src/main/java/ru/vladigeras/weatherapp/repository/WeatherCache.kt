package ru.vladigeras.weatherapp.repository

import androidx.annotation.VisibleForTesting
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import ru.vladigeras.weatherapp.data.WeatherResponse
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Persistent cache for weather responses with TTL-based expiration.
 * Uses rounded coordinates as keys to group nearby locations.
 * Persisted using DataStore Preferences.
 */
@Singleton
class WeatherCache @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val CACHE_TTL_MS = 30 * 60 * 1000L // 30 minutes

    /**
     * Creates a cache key from coordinates by rounding to 3 decimal places.
     * This groups nearby locations (within ~110 meters) under the same key.
     */
    @VisibleForTesting
    fun createKey(latitude: Double, longitude: Double): String {
        val latRounded = (latitude * 1000).roundToInt() / 1000.0
        val lngRounded = (longitude * 1000).roundToInt() / 1000.0
        return "${latRounded}_${lngRounded}"
    }

    /**
     * Retrieves cached weather data if it exists and hasn't expired.
     * @return cached WeatherResponse or null if not found/expired
     */
    suspend fun getWeather(latitude: Double, longitude: Double): WeatherResponse? {
        val key = createKey(latitude, longitude)
        val prefsKey = stringPreferencesKey("weather_cache_$key")
        val prefs = dataStore.data.first()
        val jsonString = prefs[prefsKey] ?: return null

        return try {
            val cached = Json.decodeFromString<CachedWeatherData>(jsonString)
            // Check if expired
            if (System.currentTimeMillis() - cached.timestamp > CACHE_TTL_MS) {
                // Remove expired entry
                dataStore.edit { it.remove(prefsKey) }
                null
            } else {
                cached.response
            }
        } catch (e: Exception) {
            // If deserialization fails, remove the corrupted entry
            dataStore.edit { it.remove(prefsKey) }
            null
        }
    }

    /**
     * Stores weather data in cache with current timestamp.
     */
    suspend fun putWeather(latitude: Double, longitude: Double, response: WeatherResponse) {
        val key = createKey(latitude, longitude)
        val prefsKey = stringPreferencesKey("weather_cache_$key")
        val cached = CachedWeatherData(response, System.currentTimeMillis())
        val jsonString = Json.encodeToString(cached)
        dataStore.edit { it[prefsKey] = jsonString }
    }

    /**
     * Removes the cached entry for the given coordinates, if present.
     */
    suspend fun evict(latitude: Double, longitude: Double) {
        val key = createKey(latitude, longitude)
        val prefsKey = stringPreferencesKey("weather_cache_$key")
        dataStore.edit { it.remove(prefsKey) }
    }

    @Serializable
    private data class CachedWeatherData(val response: WeatherResponse, val timestamp: Long)
}
