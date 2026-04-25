package ru.vladigeras.weatherapp.repository

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import ru.vladigeras.weatherapp.data.WeatherResponse
import ru.vladigeras.weatherapp.network.WeatherApiService
import kotlin.coroutines.cancellation.CancellationException

@RunWith(MockitoJUnitRunner::class)
class WeatherRepositoryImplTest {
    
    @Mock
    lateinit var weatherApiService: WeatherApiService
    
    @InjectMocks
    lateinit var weatherRepository: WeatherRepositoryImpl
    
    @Test
    fun `getWeather returns success when API call succeeds`() = runTest {
        val mockResponse = createMockWeatherResponse()
        Mockito.`when`(weatherApiService.getCurrentWeather(55.7558, 37.6173))
            .thenReturn(mockResponse)
        
        val result = weatherRepository.getWeather(55.7558, 37.6173)
        
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.currentWeather?.temperature == 9.3)
    }
    
    @Test
    fun `getWeather returns failure when API call fails with exception`() = runTest {
        Mockito.`when`(weatherApiService.getCurrentWeather(55.7558, 37.6173))
            .thenThrow(RuntimeException("Network error"))
        
        val result = weatherRepository.getWeather(55.7558, 37.6173)
        
        assertTrue(result.isFailure)
    }
    
    @Test
    fun `getWeather returns failure when API call fails with cancellation`() = runTest {
        Mockito.`when`(weatherApiService.getCurrentWeather(55.7558, 37.6173))
            .thenThrow(CancellationException("Cancelled"))
        
        val result = weatherRepository.getWeather(55.7558, 37.6173)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is CancellationException)
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
}