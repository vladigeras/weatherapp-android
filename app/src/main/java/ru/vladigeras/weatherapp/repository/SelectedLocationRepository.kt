package ru.vladigeras.weatherapp.repository

import kotlinx.coroutines.flow.Flow
import ru.vladigeras.weatherapp.data.Location

interface SelectedLocationRepository {
    fun getSelectedLocation(): Flow<Location?>
    suspend fun saveSelectedLocation(location: Location)
    suspend fun clearSelectedLocation()
}