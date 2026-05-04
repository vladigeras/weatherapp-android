package ru.vladigeras.weatherapp.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import ru.vladigeras.weatherapp.data.Location
import ru.vladigeras.weatherapp.location.LocationService
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes

@Singleton
class LocationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationService: LocationService,
    private val androidGeocoder: AndroidGeocoder
) : LocationRepository {

    private val cache = MutableStateFlow<CachedLocation?>(null)
    private val cacheValidity = 10.minutes

    override suspend fun getLocation(): Result<Location> {
        if (!hasLocationPermission()) {
            return Result.failure(SecurityException("Location permission not granted"))
        }

        cache.value?.let { cached ->
            if (System.currentTimeMillis() - cached.timestamp < cacheValidity.inWholeMilliseconds) {
                return Result.success(cached.location)
            }
        }

        return try {
            val loc = locationService.getCurrentLocation().getOrNull()
                ?: return Result.failure(IllegalStateException("Location unavailable"))

            val locationName = getLocationNameAsync(loc.latitude, loc.longitude)
            val locationWithName = loc.copy(name = locationName)

            cache.value = CachedLocation(locationWithName, System.currentTimeMillis())
            Result.success(locationWithName)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getLocationNameAsync(latitude: Double, longitude: Double): String? {
        return try {
            val addresses = androidGeocoder.getFromLocation(latitude, longitude, 1)
            addresses?.firstOrNull()?.let { address ->
                buildString {
                    address.locality?.let { append(it) }
                        ?: address.subAdminArea?.let { append(it) }
                        ?: address.adminArea?.let { append(it) }
                    address.countryName?.let { append(", $it") }
                }.takeIf { it.isNotBlank() }
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private data class CachedLocation(val location: Location, val timestamp: Long)
}