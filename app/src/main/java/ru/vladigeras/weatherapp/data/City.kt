package ru.vladigeras.weatherapp.data

import kotlinx.serialization.Serializable

/** Minimal city information for search */
@Serializable
data class City(
    val name: String,
    val country: String?,
    val latitude: Double,
    val longitude: Double
)