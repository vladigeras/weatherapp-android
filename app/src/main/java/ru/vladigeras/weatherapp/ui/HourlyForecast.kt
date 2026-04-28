package ru.vladigeras.weatherapp.ui

/**
 * Represents an hour's weather forecast for display in the UI.
 */
data class HourlyForecast(
    val time: String,          // Formatted time (e.g., "14:30")
    val temperature: Double,   // Temperature in Celsius
    val weatherCode: Int       // Weather condition code
)