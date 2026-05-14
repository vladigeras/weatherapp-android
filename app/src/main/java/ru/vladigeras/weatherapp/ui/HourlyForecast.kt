package ru.vladigeras.weatherapp.ui

import androidx.compose.runtime.Immutable

@Immutable
data class HourlyForecast(
    val time: String,
    val weatherCode: Int?,
    val temperature: Double?,
    val humidity: Int?,
    val windSpeed: Double?
)