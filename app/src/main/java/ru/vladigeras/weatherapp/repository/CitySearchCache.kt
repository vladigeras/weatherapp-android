package ru.vladigeras.weatherapp.repository

import ru.vladigeras.weatherapp.network.GeocodingResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CitySearchCache @Inject constructor(
    private val timeProvider: () -> Long
) {
    constructor() : this({ System.currentTimeMillis() })
    private val CACHE_TTL_MS = 3 * 60 * 60 * 1000L // 3 hours
    
    private data class CachedCitySearch(val results: List<GeocodingResult>, val timestamp: Long)
    
    private val cache = object : LinkedHashMap<String, CachedCitySearch>(100, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedCitySearch>?): Boolean {
            return size > 100
        }
    }
    
    fun get(query: String): List<GeocodingResult>? {
        val key = query.lowercase()
        val cached = cache[key] ?: return null
        
        // Check if expired
        if (timeProvider() - cached.timestamp > CACHE_TTL_MS) {
            // Remove expired entry
            cache.remove(key)
            return null
        }
        
        return cached.results
    }
    
    fun put(query: String, results: List<GeocodingResult>) {
        val key = query.lowercase()
        cache[key] = CachedCitySearch(results, timeProvider())
    }
    
    fun clear() = cache.clear()
}