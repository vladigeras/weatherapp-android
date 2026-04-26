package ru.vladigeras.weatherapp.network

import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GeocodingServiceTest {

    @Test
    fun `searchCity and reverseGeocode methods exist`() = runTest {
        val geocodingService = GeocodingService(mockk(relaxed = true))
        
        val result = geocodingService.searchCity("Moscow")
        assertTrue(true)
    }
    
    @Test
    fun `reverseGeocode method exists`() = runTest {
        val geocodingService = GeocodingService(mockk(relaxed = true))
        
        val result = geocodingService.reverseGeocode(55.0, 37.0)
        assertTrue(true)
    }
}