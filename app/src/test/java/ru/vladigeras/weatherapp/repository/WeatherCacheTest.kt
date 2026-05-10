package ru.vladigeras.weatherapp.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import ru.vladigeras.weatherapp.data.Current
import ru.vladigeras.weatherapp.data.WeatherResponse
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WeatherCacheTest {

    private lateinit var cache: WeatherCache
    private lateinit var tempDir: File
    private val testLatitude = 55.7558
    private val testLongitude = 37.6173

    @Before
    fun setup() {
        tempDir = RuntimeEnvironment.getApplication().cacheDir
        cache = WeatherCache(RuntimeEnvironment.getApplication())
    }

    @After
    fun tearDown() {
        runBlocking {
            delay(100)
        }
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
}