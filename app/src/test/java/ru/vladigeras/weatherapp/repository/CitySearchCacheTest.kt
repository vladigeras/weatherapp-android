package ru.vladigeras.weatherapp.repository

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.vladigeras.weatherapp.network.GeocodingResult

class CitySearchCacheTest {
    
    @Test
    fun `caches search results`() = runTest {
        val cache = CitySearchCache()
        val results = listOf(GeocodingResult(1, "Moscow", 55.7, 37.6, "Russia", "RU", null))
        
        cache.put("moscow", results)
        
        val cached = cache.get("moscow")
        assertEquals(results, cached)
    }
    
    @Test
    fun `case insensitive`() = runTest {
        val cache = CitySearchCache()
        val results = listOf(GeocodingResult(1, "Moscow", 55.7, 37.6, "Russia", "RU", null))
        
        cache.put("Moscow", results)
        
        val cached = cache.get("moscow")
        assertEquals(results, cached)
    }
    
    @Test
    fun `evicts oldest when over limit`() = runTest {
        val cache = CitySearchCache()
        
        repeat(101) { i ->
            val results = listOf(GeocodingResult(i, "City$i", 55.0 + i, 37.0 + i, "Country$i", "C$i", null))
            cache.put("city$i", results)
        }
        
        assertTrue(cache.get("city0") == null)
        assertTrue(cache.get("city100") != null)
    }
}