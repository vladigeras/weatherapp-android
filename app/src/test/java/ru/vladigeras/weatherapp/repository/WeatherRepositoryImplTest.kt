package ru.vladigeras.weatherapp.repository

import android.content.Context
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.awaitCancellation
import io.ktor.client.plugins.HttpRequestTimeoutException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import ru.vladigeras.weatherapp.data.Current
import ru.vladigeras.weatherapp.data.CurrentUnits
import ru.vladigeras.weatherapp.data.DailyWeather
import ru.vladigeras.weatherapp.data.HourlyWeather
import ru.vladigeras.weatherapp.data.WeatherDisplayPrefs
import ru.vladigeras.weatherapp.data.WeatherResponse
import ru.vladigeras.weatherapp.network.WeatherApiService
import ru.vladigeras.weatherapp.network.OpenMeteoWeatherProvider
import ru.vladigeras.weatherapp.network.WeatherProviders
import ru.vladigeras.weatherapp.util.asProviderWeather
import io.mockk.mockk
import io.mockk.coEvery
import ru.vladigeras.weatherapp.domain.mapper.WeatherMapper
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WeatherRepositoryImplTest {

    private lateinit var weatherRepository: WeatherRepositoryImpl
    private lateinit var mockWeatherApiService: TestWeatherApiService
    private lateinit var weatherCache: WeatherCache
    private val defaultPrefs = WeatherDisplayPrefs()
    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Before
    fun setup() {
        mockWeatherApiService = TestWeatherApiService()
        weatherCache = WeatherCache(context)
        weatherRepository = WeatherRepositoryImpl(WeatherProviders(listOf(OpenMeteoWeatherProvider(mockWeatherApiService, WeatherParamsBuilder()))), weatherCache)
    }

    @Test
    fun `getWeather returns success when API call succeeds`() = runTest {
        val mockResponse = createMockWeatherResponse()
        mockWeatherApiService.setResponse(mockResponse)

        val result = weatherRepository.getWeather(55.7558, 37.6173, defaultPrefs)

        advanceUntilIdle()

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

        advanceUntilIdle()

        // Second call immediately - should hit cache
        val result2 = weatherRepository.getWeather(55.7558, 37.6173)
        assertTrue(result2.isSuccess)
        assertEquals(mockResponse.asProviderWeather(), result2.getOrNull())
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

        mockWeatherApiService.setResponses(listOf(mockResponse1, mockResponse2, mockResponse1, mockResponse2))

        // First location
        val result1a = weatherRepository.getWeather(55.7558, 37.6173)
        assertTrue(result1a.isSuccess)
        assertEquals(10.0, result1a.getOrNull()?.current?.temperature ?: 0.0, 0.001)
        advanceUntilIdle()

        // Second location
        val result2a = weatherRepository.getWeather(56.0, 38.0)
        assertTrue(result2a.isSuccess)
        assertEquals(20.0, result2a.getOrNull()?.current?.temperature ?: 0.0, 0.001)
        advanceUntilIdle()

        // First location again (should hit cache)
        val result1b = weatherRepository.getWeather(55.7558, 37.6173)
        assertTrue(result1b.isSuccess)
        assertEquals(10.0, result1b.getOrNull()?.current?.temperature ?: 0.0, 0.001)
        advanceUntilIdle()

        // Second location again (should hit cache)
        val result2b = weatherRepository.getWeather(56.0, 38.0)
        assertTrue(result2b.isSuccess)
        assertEquals(20.0, result2b.getOrNull()?.current?.temperature ?: 0.0, 0.001)
        advanceUntilIdle()

        // Should have made only 2 API calls (one for each unique location)
        assertEquals(2, mockWeatherApiService.callCount)
    }

    @Test
    fun `cached hours are filtered relative to current time on every display`() = runTest {
        val response = createMockWeatherResponse().copy(
            hourly = HourlyWeather(
                time = listOf("2026-04-25T20:00", "2026-04-25T21:00", "2026-04-25T22:00", "2026-04-26T08:00", "2026-04-26T09:00"),
                temperature2m = List(5) { 10.0 }
            )
        )
        mockWeatherApiService.setResponse(response)
        val language = mockk<LanguagePreferenceRepository>()
        coEvery { language.getAppLocale() } returns java.util.Locale.ENGLISH
        fun mapper(time: String) = WeatherMapper(language, Clock.fixed(Instant.parse(time), ZoneOffset.UTC))
        val first = weatherRepository.getWeather(55.7558, 37.6173).getOrThrow()
        assertEquals(listOf("21:00", "22:00", "08:00"), mapper("2026-04-25T20:25:00Z").mapToHourlyForecast(first.hourly, first.timezone, 12).map { it.time })
        val cached = weatherRepository.getWeather(55.7558, 37.6173).getOrThrow()
        assertEquals(listOf("22:00", "08:00", "09:00"), mapper("2026-04-25T21:25:00Z").mapToHourlyForecast(cached.hourly, cached.timezone, 12).map { it.time })
        assertEquals(1, mockWeatherApiService.callCount)
    }

    @Test
    fun `weather chain stops at thirty seconds and returns a timeout failure`() = runTest {
        mockWeatherApiService.suspendResponse = {
            delay(20_000)
            delay(20_000)
        }
        val result = weatherRepository.getWeather(55.7, 37.6, forceRefresh = true)
        assertTrue(result.exceptionOrNull() is HttpRequestTimeoutException)
        assertEquals(30_000L, testScheduler.currentTime)
        assertTrue(mockWeatherApiService.cancelled)
    }

    @Test
    fun `parent cancellation cancels the pending weather call`() = runTest {
        mockWeatherApiService.suspendResponse = { awaitCancellation() }
        val job = async { weatherRepository.getWeather(55.7, 37.6, forceRefresh = true) }
        testScheduler.runCurrent()
        job.cancel()
        job.join()
        assertTrue(job.isCancelled)
        assertTrue(mockWeatherApiService.cancelled)
    }

    private class TestWeatherApiService : WeatherApiService {
        var suspendResponse: (suspend () -> Unit)? = null
        var cancelled = false
        @Volatile var callCount: Int = 0
        private var responses: List<WeatherResponse> = emptyList()
        private var exceptionToThrow: Exception? = null
        @Volatile private var responseIndex: Int = 0

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
            forecastDays: Int,
            forecastHours: Int
        ): WeatherResponse {
            callCount++
            try {
                suspendResponse?.invoke()
            } catch (e: kotlinx.coroutines.CancellationException) {
                cancelled = true
                throw e
            }
            if (exceptionToThrow != null) {
                throw exceptionToThrow!!
            }
            val idx = synchronized(this) {
                if (responseIndex > responses.lastIndex) responses.lastIndex else responseIndex++
            }
            return responses.getOrNull(idx)?.also {
                // responseIndex already incremented above
            } ?: createMockWeatherResponse()
        }
    }
}
