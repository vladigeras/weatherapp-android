package ru.vladigeras.weatherapp.data

/**
 * User preferences for which weather data groups to display.
 */
data class WeatherDisplayPrefs(
    val showHumidity: Boolean = true,
    val showWind: Boolean = true,
    val showPrecipitation: Boolean = true,
    val showSunTimes: Boolean = true,
    val showUvIndex: Boolean = true,
    val showForecastDays: Boolean = false,
    val forecastDays: Int = 1,
    val showHourlyForecast: Boolean = false,
    val hourlyForecastHours: Int = 24
)