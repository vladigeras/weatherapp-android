package ru.vladigeras.weatherapp.repository

interface CityNameResolver {
    suspend fun resolveCityName(latitude: Double, longitude: Double, savedName: String?, timezone: String): String
}