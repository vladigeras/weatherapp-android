package ru.vladigeras.weatherapp.repository

import android.location.Address
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import ru.vladigeras.weatherapp.data.SearchLocation
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class CitySearchRepository @Inject constructor(
    private val geocoder: AndroidGeocoder,
    private val cache: CitySearchCache
) {
    suspend fun searchLocations(query: String, language: String): Result<List<SearchLocation>> {
        cache.get(query, language)?.let { return Result.success(it) }
        return try {
            val results = geocoder.getFromLocationName(query, 5, Locale.forLanguageTag(language))
                .mapNotNull { it.asSearchLocation() }.take(5)
            currentCoroutineContext().ensureActive()
            if (results.isNotEmpty()) cache.put(query, results, language)
            Result.success(results)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun Address.asSearchLocation(): SearchLocation? {
        val name = listOf(locality, subAdminArea, adminArea).firstOrNull { !it.isNullOrBlank() } ?: return null
        if (!hasLatitude() || !hasLongitude() || latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
        return SearchLocation(name, latitude, longitude, countryName, adminArea)
    }
}
