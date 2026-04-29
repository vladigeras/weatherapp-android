package ru.vladigeras.weatherapp.repository

interface CityNameResolver {
    fun resolveCityName(latitude: Double, longitude: Double, savedName: String?, timezone: String): String
}