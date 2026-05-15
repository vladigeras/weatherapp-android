package ru.vladigeras.weatherapp.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import ru.vladigeras.weatherapp.data.Current
import ru.vladigeras.weatherapp.data.WeatherResponse
import java.io.File
import java.util.concurrent.atomic.AtomicLong

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WeatherCacheTest {

    private lateinit var cache: WeatherCache
    private lateinit var tempDir: File
    private val testLatitude = 55.7558
    private val testLongitude = 37.6173
    private val timeMillis = AtomicLong(System.currentTimeMillis())

    @Before
    fun setup() {
        val testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        tempDir = RuntimeEnvironment.getApplication().cacheDir
        cache = WeatherCache(RuntimeEnvironment.getApplication()).apply {
            timeProvider = { timeMillis.get() }
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        runBlocking {
            delay(100)
        }
    }

    private fun advanceCacheTime(minutes: Long) {
        timeMillis.addAndGet(minutes * 60 * 1000)
    }

    private fun createTestWeatherResponse(): WeatherResponse {
        return WeatherResponse(
            latitude = testLatitude,
            longitude = testLongitude,
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
            )
        )
    }

    @Test
    fun `key generation creates valid file name`() = runTest {
        val key = cache.createKey(55.7558, 37.6173)
        assertEquals("weather_55.756_37.617.json", key)
    }

    @Test
    fun `caches and retrieves weather data`() = runTest {
        val weatherResponse = createTestWeatherResponse()
        cache.putWeather(testLatitude, testLongitude, weatherResponse)
        val cached = cache.getWeather(testLatitude, testLongitude)
        assertEquals(weatherResponse, cached)
    }

    @Test
    fun `returns null for non-existent cache entry`() = runTest {
        val cached = cache.getWeather(testLatitude, testLongitude)
        assertNull(cached)
    }

    @Test
    fun `different coordinates use different cache entries`() = runTest {
        val weatherResponse1 = createTestWeatherResponse()
        val weatherResponse2 = weatherResponse1.copy(
            current = weatherResponse1.current?.copy(temperature = 15.0)
        )

        cache.putWeather(testLatitude, testLongitude, weatherResponse1)
        cache.putWeather(testLatitude + 1, testLongitude + 1, weatherResponse2)

        val cached1 = cache.getWeather(testLatitude, testLongitude)
        val cached2 = cache.getWeather(testLatitude + 1, testLongitude + 1)

        assertEquals(weatherResponse1, cached1)
        assertEquals(weatherResponse2, cached2)
    }

    @Test
    fun `evict removes cache entry`() = runTest {
        val weatherResponse = createTestWeatherResponse()
        cache.putWeather(testLatitude, testLongitude, weatherResponse)
        cache.evict(testLatitude, testLongitude)
        val cached = cache.getWeather(testLatitude, testLongitude)
        assertNull(cached)
    }

    @Test
    fun `cache entry expired after TTL returns null`() = runTest {
        val weatherResponse = createTestWeatherResponse()
        cache.putWeather(testLatitude, testLongitude, weatherResponse)

        // Advance time by 31 minutes (> 30 min TTL)
        advanceCacheTime(31)

        val cached = cache.getWeather(testLatitude, testLongitude)
        assertNull(cached)
    }

    @Test
    fun `cache entry within TTL returns data`() = runTest {
        val weatherResponse = createTestWeatherResponse()
        cache.putWeather(testLatitude, testLongitude, weatherResponse)

        // Advance time by 29 minutes (< 30 min TTL)
        advanceCacheTime(29)

        val cached = cache.getWeather(testLatitude, testLongitude)
        assertEquals(weatherResponse, cached)
    }

    @Test
    fun `corrupted cache file returns null and deletes file`() = runTest {
        val cacheDir = File(tempDir, "weather_cache")
        val cacheFile = File(cacheDir, "weather_55.756_37.617.json")
        cacheDir.mkdirs()
        cacheFile.writeText("corrupted json data")
        assertTrue(cacheFile.exists())

        val cached = cache.getWeather(testLatitude, testLongitude)
        assertNull(cached)
        assertFalse(cacheFile.exists())  // File should be deleted
    }
}
