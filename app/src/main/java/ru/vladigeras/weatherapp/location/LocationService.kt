package ru.vladigeras.weatherapp.location

import ru.vladigeras.weatherapp.data.Location

interface LocationService {
    suspend fun getCurrentLocation(): Result<Location>
}
