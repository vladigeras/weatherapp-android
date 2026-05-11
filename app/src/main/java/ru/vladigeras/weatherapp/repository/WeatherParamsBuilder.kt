package ru.vladigeras.weatherapp.repository

import ru.vladigeras.weatherapp.data.WeatherDisplayPrefs

import javax.inject.Inject

class WeatherParamsBuilder @Inject constructor() {

    fun build(prefs: WeatherDisplayPrefs): Triple<String, String, String> {
        val current = mutableListOf<String>()
        val hourly = mutableListOf<String>()
        val daily = mutableListOf<String>()

        current += "temperature_2m,is_day"
        hourly += "temperature_2m"
        daily += "temperature_2m_max,temperature_2m_min"

        current += "weathercode,apparent_temperature"
        daily += "weathercode"

        if (prefs.showHumidity) {
            hourly += "relativehumidity_2m"
        }
        if (prefs.showWind) {
            current += "windspeed_10m"
            hourly += "windspeed_10m"
            daily += "windspeed_10m_max,winddirection_10m_dominant"
        }
        if (prefs.showPrecipitation) {
            daily += "precipitation_sum"
        }
        if (prefs.showSunTimes) {
            daily += "sunrise,sunset"
        }
        if (prefs.showUvIndex) {
            daily += "uv_index_max"
        }
        if (prefs.showHourlyForecast) {
            daily += "weathercode"
        }

        return Triple(
            current.distinct().joinToString(","),
            hourly.distinct().joinToString(","),
            daily.distinct().joinToString(",")
        )
    }
}