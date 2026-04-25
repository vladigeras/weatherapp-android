package ru.vladigeras.weatherapp.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherResponse(
    val latitude: Double,
    val longitude: Double,
    @SerialName("generationtime_ms")
    val generationtimeMs: Double,
    @SerialName("utc_offset_seconds")
    val utcOffsetSeconds: Int,
    val timezone: String,
    @SerialName("timezone_abbreviation")
    val timezoneAbbreviation: String? = null,
    val elevation: Double,
    @SerialName("current_weather")
    val currentWeather: CurrentWeather,
    @SerialName("current_weather_units")
    val currentWeatherUnits: CurrentWeatherUnits? = null,
    val hourly: HourlyWeather? = null,
    @SerialName("hourly_units")
    val hourlyUnits: HourlyUnits? = null
)

@Serializable
data class CurrentWeather(
    val time: String,
    val interval: Int,
    val temperature: Double,
    @SerialName("temperature_unit")
    val temperatureUnit: String? = null,
    @SerialName("windspeed")
    val windSpeed: Double,
    @SerialName("winddirection")
    val windDirection: Int,
    @SerialName("wind_direction_unit")
    val windDirectionUnit: String? = null,
    @SerialName("is_day")
    val isDay: Int,
    @SerialName("weathercode")
    val weatherCode: Int,
    @SerialName("weathercode_unit")
    val weatherCodeUnit: String? = null
)

@Serializable
data class CurrentWeatherUnits(
    val time: String = "",
    val interval: String = "",
    @SerialName("temperature")
    val temperatureUnit: String = "",
    @SerialName("windspeed")
    val windSpeedUnit: String = "",
    @SerialName("winddirection")
    val windDirectionUnit: String = "",
    @SerialName("is_day")
    val isDayUnit: String = "",
    @SerialName("weathercode")
    val weatherCodeUnit: String = ""
)

@Serializable
data class HourlyWeather(
    val time: List<String>,
    @SerialName("temperature_2m")
    val temperature2m: List<Double>,
    @SerialName("relativehumidity_2m")
    val relativehumidity2m: List<Int>? = null,
    @SerialName("windspeed_10m")
    val windspeed10m: List<Double>? = null
)

@Serializable
data class HourlyUnits(
    @SerialName("time")
    val timeUnit: String = "",
    @SerialName("temperature_2m")
    val temperature2mUnit: String = "",
    @SerialName("relativehumidity_2m")
    val relativehumidity2mUnit: String = "",
    @SerialName("windspeed_10m")
    val windspeed10mUnit: String = ""
)