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
    val showForecastDays: Boolean = true,
    val forecastDays: Int = 7,
    val showHourlyForecast: Boolean = true,
    val hourlyForecastHours: Int = 12
)