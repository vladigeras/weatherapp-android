package ru.vladigeras.weatherapp.repository

import kotlinx.coroutines.flow.Flow
import ru.vladigeras.weatherapp.data.Location

interface LocationRepository {
    suspend fun getLocation(forceRefresh: Boolean = false): Result<Location>
    fun hasLocationPermission(): Boolean
}