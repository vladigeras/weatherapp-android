package ru.vladigeras.weatherapp.ui

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceTimeBy
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {

    private lateinit var weatherRepository: WeatherRepository
    private lateinit var locationRepository: LocationRepository
    private lateinit var selectedLocationRepository: SelectedLocationRepository
    private lateinit var weatherViewModel: WeatherViewModel

    private val mockLocation = Location(55.7558, 37.6173, "Moscow")
    private val mockLocation2 = Location(48.8566, 2.3522, "Paris")
    
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
    fun `should load weather for selected location and cancel previous requests`() = runTest {
        // Arrange
        coEvery { weatherRepository.getWeather(55.7558, 37.6173) } returns Result.success(mockResponse)
        coEvery { weatherRepository.getWeather(48.8566, 2.3522) } returns delay(100).thenReturn(Result.success(mockResponse2)) // Задержка для имитации медленного запроса
        
        coEvery { selectedLocationRepository.getSelectedLocation() } returns flowOf(mockLocation)
        
        // Act
        weatherViewModel.loadSavedLocation() // Загружаем первую локацию (Москва)
        weatherViewModel.loadWeather(48.8566, 2.3522) // Запрашиваем вторую локацию (Париж)
        
        // Assert
        advanceTimeBy(150) // Ждем достаточно времени для завершения обоих запросов
        assertTrue(weatherViewModel.uiState.value is WeatherUiState.Success)
        val successState = weatherViewModel.uiState.value as WeatherUiState.Success
        assertEquals(18.2, successState.temperature, 0.001) // Должна быть температура Парижа, а не Москвы
    }
    
    @Test
    fun `should handle rapid location changes correctly`() = runTest {
        // Arrange
        val locations = listOf(
            mockLocation,
            Location(51.5074, -0.1278, "London"),
            Location(40.7128, -74.0060, "New York")
        )
        
        val responses = listOf(mockResponse, mockResponse2)
        
        // Настраиваем моки для возврата разных ответов с задержкой
        coEvery { weatherRepository.getWeather(55.7558, 37.6173) } returns delay(50).thenReturn(Result.success(responses[0]))
        coEvery { weatherRepository.getWeather(51.5074, -0.1278) } returns delay(100).thenReturn(Result.success(responses[1]))
        coEvery { weatherRepository.getWeather(40.7128, -74.0060) } returns delay(150).thenReturn(Result.success(responses[0])) // Возвращаем первый ответ для проверки
        
        // Act - быстро меняем локации
        weatherViewModel.loadWeather(55.7558, 37.6173) // Лондон
        weatherViewModel.loadWeather(51.5074, -0.1278) // Париж
        weatherViewModel.loadWeather(40.7128, -74.0060) // Нью-Йорк
        
        // Assert
        advanceTimeBy(200) // Ждем завершения всех запросов
        assertTrue(weatherViewModel.uiState.value is WeatherUiState.Success)
        val successState = weatherViewModel.uiState.value as WeatherUiState.Success
        // Должен быть результат последнего запроса (Нью-Йорк, который возвращает данные Лондона из-за настройки моков)
        assertEquals(20.5, successState.temperature, 0.001)
    }
}