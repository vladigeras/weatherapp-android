package ru.vladigeras.weatherapp.repository

import android.content.Context
import android.location.Geocoder
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class CityNameResolverImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val languagePreferenceRepository: LanguagePreferenceRepository
) : CityNameResolver {

    override suspend fun resolveCityName(latitude: Double, longitude: Double, savedName: String?, timezone: String): String {
        val locale = languagePreferenceRepository.getAppLocale()

        return try {
            val addresses = getFromLocationAsync(latitude, longitude, 1, locale)
            val resolvedName = addresses?.firstOrNull()?.let { address ->
                val builder = StringBuilder()
                address.locality?.let { builder.append(it) }
                    ?: address.subAdminArea?.let { builder.append(it) }
                    ?: address.adminArea?.let { builder.append(it) }
                address.countryName?.let { country -> builder.append(", $country") }
                builder.toString()
            }?.takeIf { it.isNotBlank() }

            resolvedName ?: savedName ?: timezone
        } catch (e: Exception) {
            Log.e("CityNameResolver", "Failed to resolve city name", e)
            savedName ?: timezone
        }
    }

    private suspend fun getFromLocationAsync(
        latitude: Double,
        longitude: Double,
        maxResults: Int,
        locale: Locale
    ): List<android.location.Address>? = suspendCancellableCoroutine { continuation ->
        val geocoder = Geocoder(context, locale)
        try {
            geocoder.getFromLocation(latitude, longitude, maxResults) { addresses ->
                if (continuation.isActive) {
                    continuation.resume(addresses)
                }
            }
        } catch (e: Exception) {
            if (continuation.isActive) {
                continuation.resumeWithException(e)
            }
        }
    }
}