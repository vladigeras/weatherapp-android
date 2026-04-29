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
    val time: String? = null,
    val interval: Int? = null,
    @SerialName("temperature_2m")
    val temperature: Double? = null,
    @SerialName("apparent_temperature")
    val apparentTemperature: Double? = null,
    @SerialName("windspeed_10m")
    val windSpeed: Double? = null,
    @SerialName("winddirection_10m")
    val windDirection: Int? = null,
    @SerialName("weathercode")
    val weatherCode: Int? = null,
    @SerialName("is_day")
    val isDay: Int? = null
)

@Serializable
data class CurrentUnits(
    val time: String? = null,
    val interval: String? = null,
    @SerialName("temperature_2m")
    val temperatureUnit: String? = null,
    @SerialName("apparent_temperature")
    val apparentTemperatureUnit: String? = null,
    @SerialName("windspeed_10m")
    val windSpeedUnit: String? = null,
    @SerialName("winddirection_10m")
    val windDirectionUnit: String? = null,
    @SerialName("weathercode")
    val weatherCodeUnit: String? = null,
    @SerialName("is_day")
    val isDayUnit: String? = null
)

@Serializable
data class HourlyWeather(
    val time: List<String>,
    @SerialName("temperature_2m")
    val temperature2m: List<Double?>,
    @SerialName("relativehumidity_2m")
    val relativehumidity2m: List<Int?>? = null,
    @SerialName("windspeed_10m")
    val windspeed10m: List<Double?>? = null
)

@Serializable
data class HourlyUnits(
    @SerialName("time")
    val timeUnit: String? = null,
    @SerialName("temperature_2m")
    val temperature2mUnit: String? = null,
    @SerialName("relativehumidity_2m")
    val relativehumidity2mUnit: String? = null,
    @SerialName("windspeed_10m")
    val windspeed10mUnit: String? = null
)

@Serializable
data class DailyWeather(
    @SerialName("time")
    val time: List<String>,
    @SerialName("weathercode")
    val weatherCode: List<Int?>? = null,
    @SerialName("temperature_2m_max")
    val temperature2mMax: List<Double?>? = null,
    @SerialName("temperature_2m_min")
    val temperature2mMin: List<Double?>? = null,
    @SerialName("precipitation_sum")
    val precipitationSum: List<Double?>? = null,
    @SerialName("sunrise")
    val sunrise: List<String?>? = null,
    @SerialName("sunset")
    val sunset: List<String?>? = null,
    @SerialName("windspeed_10m_max")
    val windspeed10mMax: List<Double?>? = null,
    @SerialName("winddirection_10m_dominant")
    val winddirection10mDominant: List<Int?>? = null,
    @SerialName("uv_index_max")
    val uvIndexMax: List<Double?>? = null
)

@Serializable
data class DailyUnits(
    @SerialName("time")
    val timeUnit: String? = null,
    @SerialName("weathercode")
    val weatherCodeUnit: String? = null,
    @SerialName("temperature_2m_max")
    val temperature2mMaxUnit: String? = null,
    @SerialName("temperature_2m_min")
    val temperature2mMinUnit: String? = null,
    @SerialName("precipitation_sum")
    val precipitationSumUnit: String? = null,
    @SerialName("sunrise")
    val sunriseUnit: String? = null,
    @SerialName("sunset")
    val sunsetUnit: String? = null,
    @SerialName("windspeed_10m_max")
    val windspeed10mMaxUnit: String? = null,
    @SerialName("winddirection_10m_dominant")
    val winddirection10mDominantUnit: String? = null,
    @SerialName("uv_index_max")
    val uvIndexMaxUnit: String? = null
)
