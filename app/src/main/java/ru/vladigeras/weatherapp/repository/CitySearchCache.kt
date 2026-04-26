package ru.vladigeras.weatherapp.repository

import ru.vladigeras.weatherapp.network.GeocodingResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CitySearchCache @Inject constructor() {
    private val cache = object : LinkedHashMap<String, List<GeocodingResult>>(100, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<GeocodingResult>>?): Boolean {
            return size > 100
        }
    }
    
    fun get(query: String): List<GeocodingResult>? = cache[query.lowercase()]
    
    fun put(query: String, results: List<GeocodingResult>) {
        cache[query.lowercase()] = results
    }
    
    fun clear() = cache.clear()
}