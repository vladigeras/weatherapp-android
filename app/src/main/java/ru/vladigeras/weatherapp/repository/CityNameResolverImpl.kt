package ru.vladigeras.weatherapp.repository

import android.content.Context
import android.location.Geocoder
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CityNameResolverImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val languagePreferenceRepository: LanguagePreferenceRepository
) : CityNameResolver {

    override fun resolveCityName(latitude: Double, longitude: Double, savedName: String?, timezone: String): String {
        val locale = languagePreferenceRepository.getAppLocale()

        return try {
            val geocoder = Geocoder(context, locale)
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            val resolvedName = addresses?.firstOrNull()?.let { address ->
                val builder = StringBuilder()
                address.locality?.let { builder.append(it) }
                    ?: address.subAdminArea?.let { builder.append(it) }
                    ?: address.adminArea?.let { builder.append(it) }
                address.countryName?.let { country -> builder.append(", $country") }
                builder.toString()
            }?.takeIf { it.isNotBlank() }

            val finalName = resolvedName ?: savedName ?: timezone
            finalName
        } catch (e: Exception) {
            Log.e("CityNameResolver", "Failed to resolve city name", e)
            savedName ?: timezone
        }
    }
}