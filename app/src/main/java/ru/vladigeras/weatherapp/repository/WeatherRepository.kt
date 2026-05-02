package ru.vladigeras.weatherapp.repository

import ru.vladigeras.weatherapp.data.WeatherDisplayPrefs
import ru.vladigeras.weatherapp.data.WeatherResponse
import ru.vladigeras.weatherapp.network.WeatherApiService
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

interface WeatherRepository {
    suspend fun getWeather(
        latitude: Double,
        longitude: Double,
        prefs: WeatherDisplayPrefs = WeatherDisplayPrefs(),
        forceRefresh: Boolean = false
    ): Result<WeatherResponse>
}

@Singleton
class WeatherRepositoryImpl @Inject constructor(
    private val weatherApiService: WeatherApiService,
    private val weatherCache: WeatherCache
) : WeatherRepository {

    override suspend fun getWeather(
        latitude: Double,
        longitude: Double,
        prefs: WeatherDisplayPrefs,
        forceRefresh: Boolean
    ): Result<WeatherResponse> {
        // Check cache only if NOT force refresh
        if (!forceRefresh) {
            val cachedResponse = weatherCache.getWeather(latitude, longitude)
            if (cachedResponse != null) {
                return Result.success(cachedResponse)
            }
        }

        // If not in cache or force refresh, make API call with dynamic parameters
        return try {
            val (currentParams, hourlyParams, dailyParams) = buildParams(prefs)
            val response = weatherApiService.getWeather(
                latitude = latitude,
                longitude = longitude,
                currentParams = currentParams,
                hourlyParams = hourlyParams,
                dailyParams = dailyParams,
                forecastDays = if (prefs.showForecast) prefs.forecastDays else 1
            )
            // Store successful response in cache
            weatherCache.putWeather(latitude, longitude, response)
            Result.success(response)
        } catch (e: CancellationException) {
            Result.failure(e)
        } catch (e: Exception) {
            // Don't cache errors
            Result.failure(e)
        }
    }

    private fun buildParams(prefs: WeatherDisplayPrefs): Triple<String, String, String> {
        val current = mutableListOf<String>()
        val hourly = mutableListOf<String>()
        val daily = mutableListOf<String>()

        // Base current params: always needed (is_day for day/night icons, temperature for display)
        current += "temperature_2m,is_day"
        hourly += "temperature_2m"
        daily += "temperature_2m_max,temperature_2m_min"

        if (prefs.showCondition) {
            current += "weathercode,apparent_temperature"
            hourly += "weathercode"
            daily += "weathercode"
        }
        if (prefs.showHumidity) {
            hourly += "relativehumidity_2m"
        }
        if (prefs.showWind) {
            current += "windspeed_10m,winddirection_10m"
            hourly += "windspeed_10m"
            daily += "windspeed_10m_max,winddirection_10m_dominant"
        }
        if (prefs.showPrecipitation) {
            daily += "precipitation_sum"
        }
        if (prefs.showSunTimes) {
            daily += "sunrise,sunset"
        }
        if (prefs.showUvIndex) {
            daily += "uv_index_max"
        }

        return Triple(current.distinct().joinToString(","),
                      hourly.distinct().joinToString(","),
                      daily.distinct().joinToString(","))
    }
}