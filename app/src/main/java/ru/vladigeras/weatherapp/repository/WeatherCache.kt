package ru.vladigeras.weatherapp.repository

import androidx.annotation.VisibleForTesting
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.vladigeras.weatherapp.data.WeatherResponse
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Cache for weather responses with TTL-based expiration.
 * Uses rounded coordinates as keys to group nearby locations.
 */
@Singleton
open class WeatherCache @Inject constructor(
    private val timeProvider: () -> Long = { System.currentTimeMillis() }
) {
    private val CACHE_TTL_MS = 30 * 60 * 1000L // 30 minutes
    
    private val cache = ConcurrentHashMap<String, CachedWeather>()
    
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
    fun getWeather(latitude: Double, longitude: Double): WeatherResponse? {
        val key = createKey(latitude, longitude)
        val cached = cache[key] ?: return null
        
        // Check if expired
        if (timeProvider() - cached.timestamp > CACHE_TTL_MS) {
            // Remove expired entry
            cache.remove(key)
            return null
        }
        
        return cached.response
    }
    
    /**
     * Stores weather data in cache with current timestamp.
     */
    fun putWeather(latitude: Double, longitude: Double, response: WeatherResponse) {
        val key = createKey(latitude, longitude)
        cache[key] = CachedWeather(response, timeProvider())
    }
    
    /**
     * Removes the cached entry for the given coordinates, if present.
     */
    fun evict(latitude: Double, longitude: Double) {
        val key = createKey(latitude, longitude)
        cache.remove(key)
    }
    
    /** Optional: Clear all cached data */
    fun clear() {
        cache.clear()
    }
    
    private data class CachedWeather(val response: WeatherResponse, val timestamp: Long)
}

/** Hilt module for WeatherCache */
@Module
@InstallIn(SingletonComponent::class)
object WeatherCacheModule {
    @Provides
    @Singleton
    fun provideWeatherCache(): WeatherCache = WeatherCache()
}