package ru.vladigeras.weatherapp.repository

import android.content.Context
import android.location.Geocoder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.net.SocketTimeoutException

@Singleton
class AndroidGeocoder @Inject constructor(@ApplicationContext private val context: Context) {
    suspend fun getFromLocationName(query: String, maxResults: Int, locale: Locale): List<android.location.Address> {
        if (!Geocoder.isPresent()) throw CitySearchUnavailableException()
        return withTimeoutOrNull(5_000) {
            suspendCancellableCoroutine { cont ->
                try {
                    Geocoder(context, locale).getFromLocationName(query, maxResults, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<android.location.Address>) {
                            if (cont.isActive) cont.resume(addresses)
                        }

                        override fun onError(errorMessage: String?) {
                            if (cont.isActive) cont.resumeWithException(CitySearchUnavailableException())
                        }
                    })
                } catch (e: Exception) {
                    if (cont.isActive) cont.resumeWithException(e)
                }
            }
        } ?: throw SocketTimeoutException()
    }

    suspend fun getFromLocation(lat: Double, lng: Double, maxResults: Int, locale: Locale = Locale.getDefault()): List<android.location.Address>? =
        withTimeoutOrNull(5_000) {
            suspendCancellableCoroutine { cont ->
                val geocoder = Geocoder(context, locale)
                try {
                    geocoder.getFromLocation(lat, lng, maxResults, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<android.location.Address>) {
                            if (cont.isActive) cont.resume(addresses)
                        }

                        override fun onError(errorMessage: String?) {
                            if (cont.isActive) cont.resume(emptyList())
                        }
                    })
                } catch (e: Exception) {
                    if (cont.isActive) cont.resumeWithException(e)
                }
            }
        }
}

class CitySearchUnavailableException : IllegalStateException()
