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
    val current: Current? = null,
    @SerialName("current_units")
    val currentUnits: CurrentUnits? = null,
    val hourly: HourlyWeather? = null,
    @SerialName("hourly_units")
    val hourlyUnits: HourlyUnits? = null,
    val daily: DailyWeather? = null,
    @SerialName("daily_units")
    val dailyUnits: DailyUnits? = null
)

@Serializable
data class Current(
    val time: String,
    val interval: Int,
    @SerialName("temperature_2m")
    val temperature: Double,
    @SerialName("apparent_temperature")
    val apparentTemperature: Double? = null,
    @SerialName("windspeed_10m")
    val windSpeed: Double,
    @SerialName("winddirection_10m")
    val windDirection: Int,
    @SerialName("weathercode")
    val weatherCode: Int,
    @SerialName("is_day")
    val isDay: Int
)

@Serializable
data class CurrentUnits(
    val time: String = "",
    val interval: String = "",
    @SerialName("temperature_2m")
    val temperatureUnit: String = "",
    @SerialName("apparent_temperature")
    val apparentTemperatureUnit: String = "",
    @SerialName("windspeed_10m")
    val windSpeedUnit: String = "",
    @SerialName("winddirection_10m")
    val windDirectionUnit: String = "",
    @SerialName("weathercode")
    val weatherCodeUnit: String = "",
    @SerialName("is_day")
    val isDayUnit: String = ""
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

@Serializable
data class DailyWeather(
    @SerialName("time")
    val time: List<String>,
    @SerialName("weathercode")
    val weatherCode: List<Int>,
    @SerialName("temperature_2m_max")
    val temperature2mMax: List<Double>,
    @SerialName("temperature_2m_min")
    val temperature2mMin: List<Double>,
    @SerialName("precipitation_sum")
    val precipitationSum: List<Double>,
    @SerialName("sunrise")
    val sunrise: List<String>,
    @SerialName("sunset")
    val sunset: List<String>,
    @SerialName("windspeed_10m_max")
    val windspeed10mMax: List<Double>,
    @SerialName("winddirection_10m_dominant")
    val winddirection10mDominant: List<Int>,
    @SerialName("uv_index_max")
    val uvIndexMax: List<Double>
)

@Serializable
data class DailyUnits(
    @SerialName("time")
    val timeUnit: String = "",
    @SerialName("weathercode")
    val weatherCodeUnit: String = "",
    @SerialName("temperature_2m_max")
    val temperature2mMaxUnit: String = "",
    @SerialName("temperature_2m_min")
    val temperature2mMinUnit: String = "",
    @SerialName("precipitation_sum")
    val precipitationSumUnit: String = "",
    @SerialName("sunrise")
    val sunriseUnit: String = "",
    @SerialName("sunset")
    val sunsetUnit: String = "",
    @SerialName("windspeed_10m_max")
    val windspeed10mMaxUnit: String = "",
    @SerialName("winddirection_10m_dominant")
    val winddirection10mDominantUnit: String = "",
    @SerialName("uv_index_max")
    val uvIndexMaxUnit: String = ""
)
