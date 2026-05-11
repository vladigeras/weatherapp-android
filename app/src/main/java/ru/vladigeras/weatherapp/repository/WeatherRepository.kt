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
    private val weatherCache: WeatherCache,
    private val paramsBuilder: WeatherParamsBuilder
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
            val (currentParams, hourlyParams, dailyParams) = paramsBuilder.build(prefs)
            val response = weatherApiService.getWeather(
                latitude = latitude,
                longitude = longitude,
                currentParams = currentParams,
                hourlyParams = hourlyParams,
                dailyParams = dailyParams,
                forecastDays = if (prefs.showForecastDays) prefs.forecastDays else 0,
                forecastHours = if (prefs.showHourlyForecast) prefs.hourlyForecastHours else 0
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
}