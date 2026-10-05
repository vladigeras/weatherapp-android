package ru.vladigeras.weatherapp.repository

import io.mockk.every
import io.mockk.mockk
import android.content.Context
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.test.runTest
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
import ru.vladigeras.weatherapp.data.ProviderWeather
import ru.vladigeras.weatherapp.data.WeatherDisplayPrefs
import ru.vladigeras.weatherapp.data.WeatherProviderId
import ru.vladigeras.weatherapp.util.asProviderWeather
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import java.time.Instant
import java.time.ZoneId
import java.util.TimeZone
import ru.vladigeras.weatherapp.data.ForecastDay

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
        tempDir = RuntimeEnvironment.getApplication().cacheDir
        cache = WeatherCache(RuntimeEnvironment.getApplication()).apply {
            timeProvider = { timeMillis.get() }
        }
    }

    private fun advanceCacheTime(minutes: Long) {
        timeMillis.addAndGet(minutes * 60 * 1000)
    }

    private fun createTestWeatherResponse(): ProviderWeather {
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
        ).asProviderWeather()
    }

    @Test
    fun `key generation creates valid file name`() = runTest {
        val key = cache.createKey(55.7558, 37.6173)
        assertEquals("v2_open_meteo_55.756_37.617_1111111_7_12.json", key)
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
            current = weatherResponse1.current.copy(temperature = 15.0)
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
        val cacheFile = File(cacheDir, "v2_open_meteo_55.756_37.617_1111111_7_12.json")
        cacheDir.mkdirs()
        cacheFile.writeText("corrupted json data")
        assertTrue(cacheFile.exists())

        val cached = cache.getWeather(testLatitude, testLongitude)
        assertNull(cached)
        assertFalse(cacheFile.exists())  // File should be deleted
    }
    @Test
    fun `provider and response parameters isolate cached weather`() = runTest {
        val prefs = WeatherDisplayPrefs()
        val weather = createTestWeatherResponse()
        cache.putWeather(testLatitude, testLongitude, weather, prefs)
        assertNull(cache.getWeather(testLatitude, testLongitude, prefs.copy(provider = WeatherProviderId.WTTR)))
        assertNull(cache.getWeather(testLatitude, testLongitude, prefs.copy(showHumidity = false)))
        assertNull(cache.getWeather(testLatitude, testLongitude, prefs.copy(forecastDays = 3)))
        assertNull(cache.getWeather(testLatitude, testLongitude, prefs.copy(hourlyForecastHours = 48)))
    }

    @Test
    fun `new provider cache versions exclude old calculated summaries`() = runTest {
        for (provider in WeatherProviderId.entries) {
            val prefs = WeatherDisplayPrefs(provider = provider)
            val weather = createTestWeatherResponse().copy(provider = provider, timezone = ZoneId.systemDefault().id)
            cache.putWeather(testLatitude, testLongitude, weather, prefs)
            val key = cache.createKey(testLatitude, testLongitude, prefs)
            if (provider in listOf(WeatherProviderId.SEVEN_TIMER, WeatherProviderId.YR)) {
                assertTrue(key.startsWith("v3_"))
                val file = File(tempDir, "weather_cache/$key")
                assertTrue(file.renameTo(File(file.parentFile, key.replaceFirst("v3_", "v2_"))))
                assertNull(cache.getWeather(testLatitude, testLongitude, prefs))
                cache.putWeather(testLatitude, testLongitude, weather, prefs)
            } else assertTrue(key.startsWith("v2_"))
            assertEquals(weather, cache.getWeather(testLatitude, testLongitude, prefs))
        }
    }

    @Test
    fun `phone zone forecasts reject fresh cache after zone change while place zone providers keep it`() = runTest {
        val original = TimeZone.getDefault()
        try {
            for (provider in WeatherProviderId.entries) {
                TimeZone.setDefault(TimeZone.getTimeZone("Europe/Moscow"))
                val prefs = WeatherDisplayPrefs(provider = provider)
                val weather = createTestWeatherResponse().copy(provider = provider, timezone = "Europe/Moscow")
                cache.putWeather(testLatitude, testLongitude, weather, prefs)
                assertEquals(weather, cache.getWeather(testLatitude, testLongitude, prefs))
                TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Honolulu"))
                val cached = cache.getWeather(testLatitude, testLongitude, prefs)
                if (provider in listOf(WeatherProviderId.SEVEN_TIMER, WeatherProviderId.YR)) assertNull(cached)
                else assertEquals(weather, cached)
            }
        } finally { TimeZone.setDefault(original) }
    }

    @Test
    fun `phone zone daily cache expires at local midnight while place zone providers keep it`() = runTest {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Moscow"))
            for (provider in WeatherProviderId.entries) {
                timeMillis.set(Instant.parse("2026-10-03T20:55:00Z").toEpochMilli())
                val prefs = WeatherDisplayPrefs(provider = provider)
                val weather = createTestWeatherResponse().copy(provider = provider, timezone = "Europe/Moscow", daily = listOf(ForecastDay("2026-10-03")))
                cache.putWeather(testLatitude, testLongitude, weather, prefs)
                advanceCacheTime(10)
                val cached = cache.getWeather(testLatitude, testLongitude, prefs)
                if (provider in listOf(WeatherProviderId.SEVEN_TIMER, WeatherProviderId.YR)) assertNull(cached)
                else assertEquals(weather, cached)
            }
        } finally { TimeZone.setDefault(original) }
    }

    @Test
    fun `cache file operations leave the caller thread`() = runTest {
        val caller = Thread.currentThread()
        val fileThreads = ConcurrentLinkedQueue<Thread>()
        val context = mockk<Context>()
        every { context.cacheDir } answers { fileThreads.add(Thread.currentThread()); tempDir }
        val weather = createTestWeatherResponse()
        WeatherCache(context).putWeather(testLatitude, testLongitude, weather)
        assertEquals(weather, WeatherCache(context).getWeather(testLatitude, testLongitude))
        WeatherCache(context).evict(testLatitude, testLongitude)
        assertEquals(3, fileThreads.size)
        assertTrue(fileThreads.all { it !== caller })
        assertNull(cache.getWeather(testLatitude, testLongitude))
    }

}
