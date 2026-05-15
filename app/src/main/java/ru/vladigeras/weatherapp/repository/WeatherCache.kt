package ru.vladigeras.weatherapp.repository

import android.content.Context
import androidx.annotation.VisibleForTesting
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import ru.vladigeras.weatherapp.data.WeatherResponse
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class WeatherCache @Inject constructor(
    @ApplicationContext private val context: Context
) {
    @VisibleForTesting
    var timeProvider: () -> Long = { System.currentTimeMillis() }

    private val CACHE_TTL_MS = 30 * 60 * 1000L // 30 minutes
    private val cacheDir: File by lazy {
        File(context.cacheDir, "weather_cache").also { it.mkdirs() }
    }

    @VisibleForTesting
    fun createKey(latitude: Double, longitude: Double): String {
        val latRounded = (latitude * 1000).roundToInt() / 1000.0
        val lngRounded = (longitude * 1000).roundToInt() / 1000.0
        return "weather_${latRounded}_${lngRounded}.json"
    }

    private fun getCurrentTimeMillis(): Long = timeProvider()

    suspend fun getWeather(latitude: Double, longitude: Double): WeatherResponse? {
        val key = createKey(latitude, longitude)
        val cacheFile = File(cacheDir, key)

        if (!cacheFile.exists()) {
            return null
        }

        return try {
            val jsonString = cacheFile.readText()
            val cached = Json.decodeFromString<CachedWeatherData>(jsonString)

            if (getCurrentTimeMillis() - cached.timestamp > CACHE_TTL_MS) {
                cacheFile.delete()
                null
            } else {
                cached.response
            }
        } catch (e: Exception) {
            cacheFile.delete()
            null
        }
    }

    suspend fun putWeather(latitude: Double, longitude: Double, response: WeatherResponse) {
        val key = createKey(latitude, longitude)
        val cacheFile = File(cacheDir, key)
        val cached = CachedWeatherData(response, getCurrentTimeMillis())
        val jsonString = Json.encodeToString(cached)
        cacheFile.writeText(jsonString)
    }

    suspend fun evict(latitude: Double, longitude: Double) {
        val key = createKey(latitude, longitude)
        val cacheFile = File(cacheDir, key)
        cacheFile.delete()
    }

    @Serializable
    private data class CachedWeatherData(val response: WeatherResponse, val timestamp: Long)
}