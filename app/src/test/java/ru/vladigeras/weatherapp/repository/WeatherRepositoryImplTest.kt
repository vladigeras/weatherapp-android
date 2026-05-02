package ru.vladigeras.weatherapp.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.vladigeras.weatherapp.data.Current
import ru.vladigeras.weatherapp.data.CurrentUnits
import ru.vladigeras.weatherapp.data.DailyWeather
import ru.vladigeras.weatherapp.data.HourlyWeather
import ru.vladigeras.weatherapp.data.WeatherDisplayPrefs
import ru.vladigeras.weatherapp.data.WeatherResponse
import ru.vladigeras.weatherapp.network.WeatherApiService
import java.io.File

private fun createMockWeatherResponse() = WeatherResponse(
    latitude = 55.7558,
    longitude = 37.6173,
    generationtimeMs = 0.1,
    utcOffsetSeconds = 0,
    timezone = "GMT",
    elevation = 149.0,
    current = Current(
        time = "2026-04-25T16:00",
        interval = 900,
        temperature = 9.3,
        apparentTemperature = null,
        windSpeed = 2.5,
        windDirection = 225,
        weatherCode = 3,
        isDay = 1
    ),
    currentUnits = CurrentUnits(temperatureUnit = "°C"),
    hourly = HourlyWeather(
        time = listOf("2026-04-25T17:00"),
        temperature2m = listOf(10.0),
        relativehumidity2m = listOf(65)
    ),
    daily = DailyWeather(
        time = listOf("2026-04-26"),
        weatherCode = listOf(3),
        temperature2mMax = listOf(15.0),
        temperature2mMin = listOf(5.0)
    )
)

class WeatherRepositoryImplTest {

    private lateinit var weatherRepository: WeatherRepositoryImpl
    private lateinit var mockWeatherApiService: TestWeatherApiService
    private lateinit var weatherCache: WeatherCache
    private lateinit var dataStore: DataStore<Preferences>
    private val defaultPrefs = WeatherDisplayPrefs()

    @Before
    fun setup() {
        val tempFile = File.createTempFile("test_weather_cache", ".preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.Unconfined),
            produceFile = { tempFile }
        )
        mockWeatherApiService = TestWeatherApiService()
        weatherCache = WeatherCache(dataStore)
        weatherRepository = WeatherRepositoryImpl(mockWeatherApiService, weatherCache)
    }

    @Test
    fun `getWeather returns success when API call succeeds`() = runTest {
        val mockResponse = createMockWeatherResponse()
        mockWeatherApiService.setResponse(mockResponse)

        val result = weatherRepository.getWeather(55.7558, 37.6173, defaultPrefs)

        assertTrue(result.isSuccess)
        assertEquals(9.3, result.getOrNull()!!.current?.temperature ?: 0.0, 0.001)
        assertEquals(1, mockWeatherApiService.callCount)
    }

    @Test
    fun `getWeather returns cached result on second call within TTL`() = runTest {
        val mockResponse = createMockWeatherResponse()
        mockWeatherApiService.setResponse(mockResponse)

        // First call - should hit API
        val result1 = weatherRepository.getWeather(55.7558, 37.6173)
        assertTrue(result1.isSuccess)
        assertEquals(1, mockWeatherApiService.callCount)

        // Second call immediately - should hit cache
        val result2 = weatherRepository.getWeather(55.7558, 37.6173)
        assertTrue(result2.isSuccess)
        assertEquals(mockResponse, result2.getOrNull())
        assertEquals(1, mockWeatherApiService.callCount) // Still only 1 API call
    }

    @Test
    fun `getWeather does not cache error responses`() = runTest {
        // Setup API to throw exception
        mockWeatherApiService.setException(Exception("API Error"))

        // First call - should fail and not cache
        val result1 = weatherRepository.getWeather(55.7558, 37.6173)
        assertTrue(result1.isFailure)
        assertEquals(1, mockWeatherApiService.callCount)

        // Second call immediately - should fail again and call API again (error not cached)
        val result2 = weatherRepository.getWeather(55.7558, 37.6173)
        assertTrue(result2.isFailure)
        assertEquals(2, mockWeatherApiService.callCount) // API called twice
    }

    @Test
    fun `forceRefresh skips cache and hits API`() = runTest {
        val mockResponse = createMockWeatherResponse()
        mockWeatherApiService.setResponse(mockResponse)

        // First call (caches)
        weatherRepository.getWeather(55.7558, 37.6173, defaultPrefs, forceRefresh = false)
        assertEquals(1, mockWeatherApiService.callCount)

        // Force refresh (bypasses cache)
        weatherRepository.getWeather(55.7558, 37.6173, defaultPrefs, forceRefresh = true)
        assertEquals(2, mockWeatherApiService.callCount)
    }

    @Test
    fun `getWeather uses separate cache entries for different coordinates`() = runTest {
        val mockResponse1 = createMockWeatherResponse().copy(
            current = createMockWeatherResponse().current?.copy(temperature = 10.0)
        )
        val mockResponse2 = createMockWeatherResponse().copy(
            current = createMockWeatherResponse().current?.copy(temperature = 20.0)
        )

        // Setup API to return different responses based on call count
        mockWeatherApiService.setResponses(listOf(mockResponse1, mockResponse2, mockResponse1, mockResponse2))

        // First location
        val result1a = weatherRepository.getWeather(55.7558, 37.6173)
        assertTrue(result1a.isSuccess)
        assertEquals(10.0, result1a.getOrNull()?.current?.temperature ?: 0.0, 0.001)

        // Second location
        val result2a = weatherRepository.getWeather(56.0, 38.0)
        assertTrue(result2a.isSuccess)
        assertEquals(20.0, result2a.getOrNull()?.current?.temperature ?: 0.0, 0.001)

        // First location again (should hit cache)
        val result1b = weatherRepository.getWeather(55.7558, 37.6173)
        assertTrue(result1b.isSuccess)
        assertEquals(10.0, result1b.getOrNull()?.current?.temperature ?: 0.0, 0.001)

        // Second location again (should hit cache)
        val result2b = weatherRepository.getWeather(56.0, 38.0)
        assertTrue(result2b.isSuccess)
        assertEquals(20.0, result2b.getOrNull()?.current?.temperature ?: 0.0, 0.001)

        // Should have made only 2 API calls (one for each unique location)
        assertEquals(2, mockWeatherApiService.callCount)
    }

    private class TestWeatherApiService : WeatherApiService {
        var callCount: Int = 0
        private var responses: List<WeatherResponse> = emptyList()
        private var exceptionToThrow: Exception? = null
        private var responseIndex: Int = 0

        fun setResponse(response: WeatherResponse) {
            responses = listOf(response)
            exceptionToThrow = null
            responseIndex = 0
        }

        fun setResponses(responses: List<WeatherResponse>) {
            this.responses = responses
            exceptionToThrow = null
            responseIndex = 0
        }

        fun setException(exception: Exception) {
            exceptionToThrow = exception
            responses = emptyList()
            responseIndex = 0
        }

        override suspend fun getWeather(
            latitude: Double,
            longitude: Double,
            currentParams: String,
            hourlyParams: String,
            dailyParams: String,
            forecastDays: Int
        ): WeatherResponse {
            callCount++
            if (exceptionToThrow != null) {
                throw exceptionToThrow!!
            }
            val idx = if (responseIndex > responses.lastIndex) responses.lastIndex else responseIndex
            return responses.getOrNull(idx)?.also {
                responseIndex++
            } ?: createMockWeatherResponse()
        }
    }
}
