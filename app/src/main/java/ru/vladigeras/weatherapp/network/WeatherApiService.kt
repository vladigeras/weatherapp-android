package ru.vladigeras.weatherapp.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import ru.vladigeras.weatherapp.BuildConfig
import ru.vladigeras.weatherapp.data.WeatherResponse

interface WeatherApiService {
    suspend fun getWeather(
        latitude: Double,
        longitude: Double,
        currentParams: String,
        hourlyParams: String,
        dailyParams: String,
        forecastDays: Int
    ): WeatherResponse
}

class WeatherApiServiceImpl(
    private val httpClient: HttpClient
) : WeatherApiService {
    override suspend fun getWeather(
        latitude: Double,
        longitude: Double,
        currentParams: String,
        hourlyParams: String,
        dailyParams: String,
        forecastDays: Int
    ): WeatherResponse {
        return httpClient.get(BuildConfig.API_URL) {
            parameter("latitude", latitude)
            parameter("longitude", longitude)
            parameter("current", currentParams)
            parameter("hourly", hourlyParams)
            parameter("daily", dailyParams)
            parameter("forecast_days", forecastDays.toString())
            parameter("timezone", "auto")
        }.body()
    }
}