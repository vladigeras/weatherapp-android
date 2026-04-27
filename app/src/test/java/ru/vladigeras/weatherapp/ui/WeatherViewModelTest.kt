package ru.vladigeras.weatherapp.ui

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.vladigeras.weatherapp.data.CurrentWeather
import ru.vladigeras.weatherapp.data.HourlyWeather
import ru.vladigeras.weatherapp.data.Location
import ru.vladigeras.weatherapp.data.WeatherResponse
import ru.vladigeras.weatherapp.repository.LocationRepository
import ru.vladigeras.weatherapp.repository.SelectedLocationRepository
import ru.vladigeras.weatherapp.repository.WeatherRepository

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {

    private lateinit var weatherRepository: WeatherRepository
    private lateinit var locationRepository: LocationRepository
    private lateinit var selectedLocationRepository: SelectedLocationRepository
    private lateinit var weatherViewModel: WeatherViewModel

    private val mockLocation = Location(55.7558, 37.6173, "Moscow")
    
    private val mockResponse = WeatherResponse(
        latitude = 55.7558,
        longitude = 37.6173,
        generationtimeMs = 0.1,
        utcOffsetSeconds = 10800,
        timezone = "Europe/Moscow",
        timezoneAbbreviation = "MSK",
        elevation = 155.0,
        currentWeather = CurrentWeather(
            time = "2026-04-26T14:00",
            interval = 900,
            temperature = 20.5,
            windSpeed = 5.2,
            windDirection = 180,
            isDay = 1,
            weatherCode = 0
        ),
        hourly = HourlyWeather(
            time = listOf("2026-04-26T15:00"),
            temperature2m = listOf(21.0),
            relativehumidity2m = listOf(65)
        )
    )
    
    private val mockResponse2 = WeatherResponse(
        latitude = 48.8566,
        longitude = 2.3522,
        generationtimeMs = 0.1,
        utcOffsetSeconds = 7200,
        timezone = "Europe/Paris",
        timezoneAbbreviation = "CEST",
        elevation = 35.0,
        currentWeather = CurrentWeather(
            time = "2026-04-26T14:00",
            interval = 900,
            temperature = 18.2,
            windSpeed = 3.1,
            windDirection = 220,
            isDay = 1,
            weatherCode = 1
        ),
        hourly = HourlyWeather(
            time = listOf("2026-04-26T15:00"),
            temperature2m = listOf(19.0),
            relativehumidity2m = listOf(70)
        )
    )
       
    @Before
    fun setup() {
        weatherRepository = mockk()
        locationRepository = mockk()
        selectedLocationRepository = mockk()
        weatherViewModel = WeatherViewModel(weatherRepository, locationRepository, selectedLocationRepository)

        Dispatchers.setMain(Dispatchers.Unconfined)
        ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) {
                runnable.run()
            }

            override fun executeOnMainThread(runnable: Runnable) {
                runnable.run()
            }

            override fun postToMainThread(runnable: Runnable) {
                runnable.run()
            }

            override fun isMainThread(): Boolean = true
        })
    }
       
    @After
    fun tearDown() {
        Dispatchers.resetMain()
        ArchTaskExecutor.getInstance().setDelegate(null)
    }
    
    @Test
    fun `should load weather for location without delay`() = runTest {
        coEvery { weatherRepository.getWeather(55.7558, 37.6173) } returns Result.success(mockResponse)
        
        weatherViewModel.loadWeather(55.7558, 37.6173)
        
        advanceTimeBy(50)
        assertTrue(weatherViewModel.uiState.value is WeatherUiState.Success)
        val state = weatherViewModel.uiState.value as WeatherUiState.Success
        assertEquals(20.5, state.temperature, 0.001)
    }
    
    @Test
    fun `should load weather for selected location`() = runTest {
        coEvery { weatherRepository.getWeather(55.7558, 37.6173) } returns Result.success(mockResponse)
        coEvery { selectedLocationRepository.getSelectedLocation() } returns flowOf(mockLocation)
        
        weatherViewModel.loadSavedLocation()
        
        advanceTimeBy(50)
        assertTrue(weatherViewModel.uiState.value is WeatherUiState.Success)
        val successState = weatherViewModel.uiState.value as WeatherUiState.Success
        assertEquals(20.5, successState.temperature, 0.001)
    }
    
    @Test
    fun `should cancel previous request when loading new location`() = runTest {
        coEvery { weatherRepository.getWeather(55.7558, 37.6173) } returns Result.success(mockResponse)
        coEvery { selectedLocationRepository.getSelectedLocation() } returns flowOf(mockLocation)
        coEvery { weatherRepository.getWeather(48.8566, 2.3522) } returns Result.success(mockResponse2)
        
        weatherViewModel.loadSavedLocation()
        weatherViewModel.loadWeather(48.8566, 2.3522)
        
        advanceTimeBy(50)
        assertTrue(weatherViewModel.uiState.value is WeatherUiState.Success)
        val successState = weatherViewModel.uiState.value as WeatherUiState.Success
        assertEquals(18.2, successState.temperature, 0.001)
    }
    
    @Test
    fun `should handle rapid location changes correctly`() = runTest {
        coEvery { weatherRepository.getWeather(55.7558, 37.6173) } returns Result.success(mockResponse)
        coEvery { weatherRepository.getWeather(51.5074, -0.1278) } returns Result.success(mockResponse2)
        coEvery { weatherRepository.getWeather(40.7128, -74.0060) } returns Result.success(mockResponse)
        
        weatherViewModel.loadWeather(55.7558, 37.6173)
        weatherViewModel.loadWeather(51.5074, -0.1278)
        weatherViewModel.loadWeather(40.7128, -74.0060)
        
        advanceTimeBy(50)
        assertTrue(weatherViewModel.uiState.value is WeatherUiState.Success)
        val successState = weatherViewModel.uiState.value as WeatherUiState.Success
        assertEquals(20.5, successState.temperature, 0.001)
    }
    
    @Test
    fun `should show error state when weather request fails`() = runTest {
        coEvery { weatherRepository.getWeather(55.7558, 37.6173) } returns Result.failure(Exception("Network error"))
        
        weatherViewModel.loadWeather(55.7558, 37.6173)
        
        advanceTimeBy(50)
        assertTrue(weatherViewModel.uiState.value is WeatherUiState.Error)
    }
    
    @Test
    fun `should show empty state when no location selected`() = runTest {
        coEvery { selectedLocationRepository.getSelectedLocation() } returns flowOf(null)
        
        weatherViewModel.loadSavedLocation()
        
        advanceTimeBy(50)
        assertTrue(weatherViewModel.uiState.value is WeatherUiState.Empty)
    }
}