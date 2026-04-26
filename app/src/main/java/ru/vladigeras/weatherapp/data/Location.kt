package ru.vladigeras.weatherapp.data

import kotlinx.serialization.Serializable

/** Simple representation of a geographic location */
@Serializable
data class Location(
    val latitude: Double,
    val longitude: Double,
    val name: String? = null,
    val isAutoDetected: Boolean = false
)