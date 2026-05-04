package ru.vladigeras.weatherapp.repository

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CityNameResolverImpl @Inject constructor(
    private val languagePreferenceRepository: LanguagePreferenceRepository,
    private val androidGeocoder: AndroidGeocoder
) : CityNameResolver {

    override suspend fun resolveCityName(latitude: Double, longitude: Double, savedName: String?, timezone: String): String {
        val locale = languagePreferenceRepository.getAppLocale()

        val fallbackName = savedName ?: parseTimezone(timezone) ?: "Unknown"

        return try {
            val addresses = runCatching {
                androidGeocoder.getFromLocation(latitude, longitude, 1, locale)
            }.getOrNull()

            if (addresses.isNullOrEmpty()) {
                return fallbackName
            }

            val resolvedName = addresses.firstOrNull()?.let { address ->
                val builder = StringBuilder()
                address.locality?.let { builder.append(it) }
                    ?: address.subAdminArea?.let { builder.append(it) }
                    ?: address.adminArea?.let { builder.append(it) }
                address.countryName?.let { country -> builder.append(", $country") }
                builder.toString()
            }?.takeIf { it.isNotBlank() }

            resolvedName ?: fallbackName
        } catch (e: Exception) {
            Log.w("CityNameResolver", "Failed to resolve city name", e)
            fallbackName
        }
    }

    private fun parseTimezone(timezone: String): String? {
        return try {
            timezone.substringBefore("/").replace("_", " ").takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }
}