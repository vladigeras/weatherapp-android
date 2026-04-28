package ru.vladigeras.weatherapp.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import ru.vladigeras.weatherapp.BuildConfig
import ru.vladigeras.weatherapp.data.WeatherResponse

interface WeatherApiService {
    suspend fun getCurrentWeather(latitude: Double, longitude: Double): WeatherResponse
}

class WeatherApiServiceImpl(
    private val httpClient: HttpClient
) : WeatherApiService {
    
    override suspend fun getCurrentWeather(latitude: Double, longitude: Double): WeatherResponse {
        return httpClient.get(BuildConfig.API_URL) {
            parameter("latitude", latitude)
            parameter("longitude", longitude)
            parameter("current", "temperature_2m,windspeed_10m,winddirection_10m,weathercode,is_day,apparent_temperature")
            parameter("hourly", "temperature_2m,relativehumidity_2m,windspeed_10m")
            parameter("daily", "weathercode,temperature_2m_max,temperature_2m_min,precipitation_sum,sunrise,sunset,windspeed_10m_max,winddirection_10m_dominant,uv_index_max")
            parameter("forecast_days", "16")
            parameter("timezone", "auto")
        }.body()
    }
}