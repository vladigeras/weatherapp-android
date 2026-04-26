package ru.vladigeras.weatherapp.location

import kotlinx.coroutines.flow.Flow
import ru.vladigeras.weatherapp.data.Location

interface LocationService {
    /** Emits the latest known location or null if unavailable */
    fun getCurrentLocation(): Flow<Location?>
}
