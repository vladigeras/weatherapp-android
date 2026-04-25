package ru.vladigeras.weatherapp.ui

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import ru.vladigeras.weatherapp.data.WeatherResponse
import ru.vladigeras.weatherapp.repository.WeatherRepository

@RunWith(MockitoJUnitRunner::class)
class WeatherViewModelTest {
    
    @Mock
    lateinit var weatherRepository: WeatherRepository
    
    @InjectMocks
    lateinit var weatherViewModel: WeatherViewModel
    
    @OptIn(ExperimentalCoroutinesApi::class)
    private val testDispatcher = TestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) {
                runnable.run()
            }
            
            override fun executeOnMainThread(runnable: Runnable) {
                runnable.run()
            }
        })
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
        ArchTaskExecutor.getInstance().setDelegate(null)
    }
    
    @Test
    fun `initial state is Loading`() = runTest(testDispatcher) {
        val initialState = weatherViewModel.uiState.first()
        assertTrue(initialState is WeatherUiState.Loading)
    }
    
    @Test
    fun `loadWeather returns success when repository returns success`() = runTest(testDispatcher) {
        val mockResponse = createMockWeatherResponse()
        Mockito.`when`(weatherRepository.getWeather(55.7558, 37.6173))
            .thenReturn(Result.success(mockResponse))
        
        weatherViewModel.loadWeather(55.7558, 37.6173)
        
        val state = weatherViewModel.uiState.first()
        assertTrue(state is WeatherUiState.Success)
    }
    
    @Test
    fun `loadWeather returns error when repository returns failure`() = runTest(testDispatcher) {
        Mockito.`when`(weatherRepository.getWeather(55.7558, 37.6173))
            .thenReturn(Result.failure(RuntimeException("Network error")))
        
        weatherViewModel.loadWeather(55.7558, 37.6173)
        
        val state = weatherViewModel.uiState.first()
        assertTrue(state is WeatherUiState.Error)
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
        ),
        hourly = ru.vladigeras.weatherapp.data.HourlyWeather(
            time = listOf("2026-04-25T16:00"),
            temperature2m = listOf(9.3),
            relativehumidity2m = listOf(65),
            windspeed10m = listOf(2.5)
        )
    )
}