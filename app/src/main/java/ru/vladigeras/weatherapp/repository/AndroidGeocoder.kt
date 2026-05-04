package ru.vladigeras.weatherapp.repository

import android.content.Context
import android.location.Geocoder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class AndroidGeocoder @Inject constructor(@ApplicationContext private val context: Context) {
    suspend fun getFromLocation(lat: Double, lng: Double, maxResults: Int, locale: Locale = Locale.getDefault()): List<android.location.Address>? =
        suspendCancellableCoroutine { cont ->
            val geocoder = Geocoder(context, locale)
            try {
                geocoder.getFromLocation(lat, lng, maxResults) { addresses ->
                    if (cont.isActive) cont.resume(addresses)
                }
            } catch (e: Exception) {
                if (cont.isActive) cont.resumeWithException(e)
            }
        }
}