package ru.vladigeras.weatherapp.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import ru.vladigeras.weatherapp.data.Location
import javax.inject.Inject

class LocationServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : LocationService {

    private val fusedClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    @SuppressLint("MissingPermission") // Permissions checked by caller/UI
    override fun getCurrentLocation(): Flow<Location?> = callbackFlow {
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { androidLocation ->
                val loc = androidLocation?.let {
                    Location(it.latitude, it.longitude, null)
                }
                trySend(loc)
            }
            .addOnFailureListener { close(it) }
        awaitClose { /* no-op */ }
    }
}
