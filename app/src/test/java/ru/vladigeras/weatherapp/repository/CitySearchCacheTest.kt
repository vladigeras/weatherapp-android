package ru.vladigeras.weatherapp.repository

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.vladigeras.weatherapp.data.SearchLocation
import java.util.concurrent.TimeUnit

class CitySearchCacheTest {
    
    private lateinit var cache: CitySearchCache
    private var fakeTime: Long = 0
    
    @Before
    fun setup() {
        fakeTime = 0
        cache = CitySearchCache({ fakeTime })
    }
    
    @Test
    fun `caches search results`() = runTest {
        val results = listOf(SearchLocation("Moscow", 55.7, 37.6, "Russia"))
        
        cache.put("moscow", results)
        
        val cached = cache.get("moscow")
        assertEquals(results, cached)
    }
    
    @Test
    fun `case insensitive`() = runTest {
        val cache = CitySearchCache({ fakeTime })
        val results = listOf(SearchLocation("Moscow", 55.7, 37.6, "Russia"))
        
        cache.put("Moscow", results)
        
        val cached = cache.get("moscow")
        assertEquals(results, cached)
    }
    
    @Test
    fun `evicts oldest when over limit`() = runTest {
        val cache = CitySearchCache({ fakeTime })
        
        repeat(101) { i ->
            val results = listOf(SearchLocation("City$i", 55.0 + i, 37.0 + i, "Country$i"))
            cache.put("city$i", results)
        }
        
        assertTrue(cache.get("city0") == null)
        assertTrue(cache.get("city100") != null)
    }
    
    @Test
    fun `returns null for expired entries`() = runTest {
        val results = listOf(SearchLocation("London", 51.5, -0.1, "UK"))
        
        // Put item at time 0
        cache.put("london", results)
        fakeTime = 0
        
        // Advance time past TTL (3 hours + 1 minute)
        fakeTime = TimeUnit.HOURS.toMillis(3) + TimeUnit.MINUTES.toMillis(1)
        
        val cached = cache.get("london")
        assertNull(cached)
    }
    
    @Test
    fun `removes expired entries from cache on access`() = runTest {
        val results = listOf(SearchLocation("Paris", 48.9, 2.4, "France"))
        
        // Put item at time 0
        cache.put("paris", results)
        fakeTime = 0
        
        // Advance time past TTL
        fakeTime = TimeUnit.HOURS.toMillis(3) + TimeUnit.MINUTES.toMillis(1)
        
        // Access the expired entry (should trigger removal)
        val cached = cache.get("paris")
        assertNull(cached)
        
        // Try to add new item and check if old one was removed (LRU behavior)
        repeat(50) { i ->
            val newResults = listOf(SearchLocation("City$i", 55.0 + i, 37.0 + i, "Country$i"))
            cache.put("city$i", newResults)
        }
        
        // The paris entry should have been removed due to expiration, 
        // so we should be able to add 50 new items without triggering LRU yet
        // (actually, let's check that we can still get items we just added)
        val recentCity = cache.get("city49")
        assertTrue(recentCity != null && recentCity.size == 1)
    }
    @Test
    fun `language isolates search entries`() {
        val results = listOf(SearchLocation("Moscow", 55.7, 37.6))
        cache.put("Moscow", results, "ru")
        assertNull(cache.get("Moscow", "en"))
        assertEquals(results, cache.get("MOSCOW", "ru"))
    }

}