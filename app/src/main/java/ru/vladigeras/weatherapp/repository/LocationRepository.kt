package ru.vladigeras.weatherapp.repository

import kotlinx.coroutines.flow.Flow
import ru.vladigeras.weatherapp.data.Location

interface LocationRepository {
    suspend fun getLocation(): Result<Location>
    fun hasLocationPermission(): Boolean
}