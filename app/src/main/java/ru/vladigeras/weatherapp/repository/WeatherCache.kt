package ru.vladigeras.weatherapp.repository

import android.content.Context
import androidx.annotation.VisibleForTesting
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import ru.vladigeras.weatherapp.data.ProviderWeather
import ru.vladigeras.weatherapp.data.WeatherDisplayPrefs
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
    fun createKey(latitude: Double, longitude: Double, prefs: WeatherDisplayPrefs = WeatherDisplayPrefs()): String {
        val latRounded = (latitude * 1000).roundToInt() / 1000.0
        val lngRounded = (longitude * 1000).roundToInt() / 1000.0
        val flags = listOf(prefs.showHumidity, prefs.showWind, prefs.showPrecipitation, prefs.showSunTimes, prefs.showUvIndex, prefs.showForecastDays, prefs.showHourlyForecast).joinToString("") { if (it) "1" else "0" }
        return "v2_${prefs.provider.value}_${latRounded}_${lngRounded}_${flags}_${prefs.forecastDays}_${prefs.hourlyForecastHours}.json"
    }

    private fun getCurrentTimeMillis(): Long = timeProvider()

    suspend fun getWeather(latitude: Double, longitude: Double, prefs: WeatherDisplayPrefs = WeatherDisplayPrefs()): ProviderWeather? = withContext(Dispatchers.IO) {
        val key = createKey(latitude, longitude, prefs)
        val cacheFile = File(cacheDir, key)

        if (!cacheFile.exists()) {
            return@withContext null
        }

        try {
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

    suspend fun putWeather(latitude: Double, longitude: Double, response: ProviderWeather, prefs: WeatherDisplayPrefs = WeatherDisplayPrefs()): Unit = withContext(Dispatchers.IO) {
        val key = createKey(latitude, longitude, prefs)
        val cacheFile = File(cacheDir, key)
        val cached = CachedWeatherData(response, getCurrentTimeMillis())
        val jsonString = Json.encodeToString(cached)
        cacheFile.writeText(jsonString)
    }

    suspend fun evict(latitude: Double, longitude: Double, prefs: WeatherDisplayPrefs = WeatherDisplayPrefs()): Unit = withContext(Dispatchers.IO) {
        val key = createKey(latitude, longitude, prefs)
        val cacheFile = File(cacheDir, key)
        cacheFile.delete()
    }

    @Serializable
    private data class CachedWeatherData(val response: ProviderWeather, val timestamp: Long)
}