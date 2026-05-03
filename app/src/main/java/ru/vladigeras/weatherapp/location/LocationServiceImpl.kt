package ru.vladigeras.weatherapp.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import ru.vladigeras.weatherapp.data.Location
import javax.inject.Inject
import kotlin.coroutines.resume

class LocationServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : LocationService {

    private val fusedClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): Result<Location> = suspendCancellableCoroutine { cont ->
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { androidLocation ->
                val loc = androidLocation?.let { Location(it.latitude, it.longitude, null) }
                if (loc != null) cont.resume(Result.success(loc)) else cont.resume(Result.failure(Exception("Location is null")))
            }
            .addOnFailureListener { cont.resume(Result.failure(it)) }
    }
}
