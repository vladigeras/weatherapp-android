package ru.vladigeras.weatherapp.ui

data class HourlyForecast(
    val time: String,
    val weatherCode: Int?,
    val temperature: Double?,
    val humidity: Int?,
    val windSpeed: Double?
)