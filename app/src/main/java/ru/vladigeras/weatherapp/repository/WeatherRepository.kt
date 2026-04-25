package ru.vladigeras.weatherapp.repository

import ru.vladigeras.weatherapp.data.WeatherResponse
import ru.vladigeras.weatherapp.network.WeatherApiService
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

interface WeatherRepository {
    suspend fun getWeather(latitude: Double, longitude: Double): Result<WeatherResponse>
}

@Singleton
class WeatherRepositoryImpl @Inject constructor(
    private val weatherApiService: WeatherApiService
) : WeatherRepository {
    
    override suspend fun getWeather(latitude: Double, longitude: Double): Result<WeatherResponse> {
        return try {
            val response = weatherApiService.getCurrentWeather(latitude, longitude)
            Result.success(response)
        } catch (e: CancellationException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}