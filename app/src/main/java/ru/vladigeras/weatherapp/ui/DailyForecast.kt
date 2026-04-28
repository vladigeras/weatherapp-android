package ru.vladigeras.weatherapp.ui

/**
 * Represents a single day's weather forecast for display in the UI.
 */
data class DailyForecast(
    val date: String,          // Formatted date (e.g., "Tue, 28")
    val dayName: String,       // Day of week (e.g., "Tue")
    val weatherCode: Int,      // Weather condition code
    val temperatureMin: Double,// Minimum temperature
    val temperatureMax: Double,// Maximum temperature
    val precipitationSum: Double, // Sum of precipitation
    val sunrise: String,       // Sunrise time
    val sunset: String,        // Sunset time
    val windSpeedMax: Double,  // Maximum wind speed
    val windDirectionDominant: Int, // Dominant wind direction
    val uvIndexMax: Double     // Maximum UV index
)