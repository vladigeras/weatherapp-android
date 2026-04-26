package ru.vladigeras.weatherapp.repository

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.vladigeras.weatherapp.data.WeatherResponse
import ru.vladigeras.weatherapp.network.WeatherApiService

class WeatherRepositoryImplTest {
    
    @Test
    fun `getWeather returns success when API call succeeds`() = runTest {
        val mockResponse = createMockWeatherResponse()
        val weatherApiService = TestWeatherApiService(mockResponse)
        val weatherRepository = WeatherRepositoryImpl(weatherApiService)
        
        val result = weatherRepository.getWeather(55.7558, 37.6173)
        
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.currentWeather?.temperature == 9.3)
    }
    
    private fun createMockWeatherResponse() = WeatherResponse(
        latitude = 55.7558,
        longitude = 37.6173,
        generationtimeMs = 0.1,
        utcOffsetSeconds = 0,
        timezone = "GMT",
        elevation = 149.0,
        currentWeather = ru.vladigeras.weatherapp.data.CurrentWeather(
            time = "2026-04-25T16:00",
            interval = 900,
            temperature = 9.3,
            windSpeed = 2.5,
            windDirection = 225,
            isDay = 1,
            weatherCode = 3
        )
    )
    
    private class TestWeatherApiService(private val response: WeatherResponse) : WeatherApiService {
        override suspend fun getCurrentWeather(latitude: Double, longitude: Double): WeatherResponse = response
    }
}