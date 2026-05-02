package ru.vladigeras.weatherapp.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import ru.vladigeras.weatherapp.data.Current
import ru.vladigeras.weatherapp.data.WeatherResponse
import java.io.File
import java.util.concurrent.TimeUnit

class WeatherCacheTest {

    private lateinit var cache: WeatherCache
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var tempFile: File
    private val testLatitude = 55.7558
    private val testLongitude = 37.6173

    @Before
    fun setup() {
        tempFile = File.createTempFile("test_weather_cache", ".preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.Unconfined),
            produceFile = { tempFile }
        )
        cache = WeatherCache(dataStore)
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
                temperature = 9.3, // temperature_2m
                apparentTemperature = null,
                windSpeed = 2.5, // windspeed_10m
                windDirection = 225, // winddirection_10m
                weatherCode = 3,
                isDay = 1
            )
        )
    }

    @Test
    fun `key generation rounds coordinates to 3 decimal places`() = runTest {
        // Test that coordinates are rounded to 3 decimal places
        val key1 = cache.createKey(55.7558, 37.6173) // Should be "55.756_37.617"
        val key2 = cache.createKey(55.75584, 37.61734) // Should also be "55.756_37.617" due to rounding
        val key3 = cache.createKey(55.75575, 37.61725) // Should also be "55.756_37.617" due to rounding

        assertEquals("55.756_37.617", key1)
        assertEquals("55.756_37.617", key2)
        assertEquals("55.756_37.617", key3)

        // Test different rounding
        val key4 = cache.createKey(55.7554, 37.6174) // Should be "55.755_37.617"
        assertEquals("55.755_37.617", key4)
    }

    @Test
    fun `caches and retrieves weather data`() = runTest {
        val weatherResponse = createTestWeatherResponse()

        // Put in cache
        cache.putWeather(testLatitude, testLongitude, weatherResponse)

        // Retrieve from cache
        val cached = cache.getWeather(testLatitude, testLongitude)

        assertEquals(weatherResponse, cached)
    }

    @Test
    fun `returns null for non-existent cache entry`() = runTest {
        val cached = cache.getWeather(testLatitude, testLongitude)
        assertNull(cached)
    }

    @Test
    fun `differentCoordinatesUseDifferentCacheEntries`() = runTest {
        val weatherResponse1 = createTestWeatherResponse()
        val weatherResponse2 = weatherResponse1.copy(
            current = weatherResponse1.current?.copy(temperature = 15.0)
        )

        // Put two different locations
        cache.putWeather(testLatitude, testLongitude, weatherResponse1)
        cache.putWeather(testLatitude + 1, testLongitude + 1, weatherResponse2) // Different location

        // Retrieve both
        val cached1 = cache.getWeather(testLatitude, testLongitude)
        val cached2 = cache.getWeather(testLatitude + 1, testLongitude + 1)

        assertEquals(weatherResponse1, cached1)
        assertEquals(weatherResponse2, cached2)
    }

    @Test
    fun `nearbyCoordinatesShareCacheEntryDueToRounding`() = runTest {
        val weatherResponse1 = createTestWeatherResponse()
        val weatherResponse2 = createTestWeatherResponse().copy(
            current = createTestWeatherResponse().current?.copy(temperature = 15.0)
        )

        // These coordinates should round to the same key
        val lat1 = 55.7558
        val lng1 = 37.6173
        val lat2 = 55.75584 // Very close - should round to same value
        val lng2 = 37.61734

        // Put first
        cache.putWeather(lat1, lng1, weatherResponse1)

        // Put second (should overwrite due to same key)
        cache.putWeather(lat2, lng2, weatherResponse2)

        // Retrieve - should get the second one
        val cached = cache.getWeather(lat1, lng1)
        assertEquals(weatherResponse2, cached)
    }
}
