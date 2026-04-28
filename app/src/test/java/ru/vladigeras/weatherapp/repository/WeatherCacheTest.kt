package ru.vladigeras.weatherapp.repository

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import ru.vladigeras.weatherapp.data.CurrentWeather
import ru.vladigeras.weatherapp.data.WeatherResponse
import java.util.concurrent.TimeUnit

class WeatherCacheTest {
    
    private lateinit var cache: WeatherCache
    private var fakeTime: Long = 0
    private val testLatitude = 55.7558
    private val testLongitude = 37.6173
    
    @Before
    fun setup() {
        fakeTime = 0
        cache = WeatherCache({ fakeTime })
    }
    
    private fun createTestWeatherResponse(): WeatherResponse {
        return WeatherResponse(
            latitude = testLatitude,
            longitude = testLongitude,
            generationtimeMs = 0.1,
            utcOffsetSeconds = 0,
            timezone = "GMT",
            elevation = 149.0,
            currentWeather = CurrentWeather(
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
    fun `returns null for expired cache entry`() = runTest {
        val weatherResponse = createTestWeatherResponse()
        
        // Put in cache at time 0
        cache.putWeather(testLatitude, testLongitude, weatherResponse)
        fakeTime = 0
        
        // Advance time past TTL (30 minutes + 1 second)
        fakeTime = TimeUnit.MINUTES.toMillis(30) + 1000
        
        val cached = cache.getWeather(testLatitude, testLongitude)
        assertNull(cached)
    }
    
    @Test
    fun `removes expired entries from cache on access`() = runTest {
        val weatherResponse = createTestWeatherResponse()
        
        // Put in cache at time 0
        cache.putWeather(testLatitude, testLongitude, weatherResponse)
        fakeTime = 0
        
        // Advance time past TTL
        fakeTime = TimeUnit.MINUTES.toMillis(30) + 10000 // 30 min + 10 sec
        
        // Access the expired entry (should trigger removal)
        val cached = cache.getWeather(testLatitude, testLongitude)
        assertNull(cached)
        
        // Try to add new item and verify cache is clean
        val newResponse = createTestWeatherResponse().copy(
            currentWeather = createTestWeatherResponse().currentWeather.copy(temperature = 15.0)
        )
        cache.putWeather(testLatitude, testLongitude, newResponse)
        
        val cachedAgain = cache.getWeather(testLatitude, testLongitude)
        assertEquals(newResponse, cachedAgain)
    }
    
    @Test
    fun `differentCoordinatesUseDifferentCacheEntries`() = runTest {
        val weatherResponse1 = createTestWeatherResponse()
        val weatherResponse2 = createTestWeatherResponse().copy(
            currentWeather = createTestWeatherResponse().currentWeather.copy(temperature = 15.0)
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
            currentWeather = createTestWeatherResponse().currentWeather.copy(temperature = 15.0)
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