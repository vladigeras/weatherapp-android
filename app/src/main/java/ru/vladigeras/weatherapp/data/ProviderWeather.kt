package ru.vladigeras.weatherapp.data

import kotlinx.serialization.Serializable

@Serializable
enum class WeatherProviderId(val value: String) {
    OPEN_METEO("open_meteo"), WTTR("wttr"), SEVEN_TIMER("7timer"), YR("yr");

    companion object {
        fun fromValue(value: String?) = entries.firstOrNull { it.value == value } ?: OPEN_METEO
    }
}

@Serializable
enum class WeatherCondition(val displayCode: Int) {
    CLEAR(0), MOSTLY_CLEAR(1), PARTLY_CLOUDY(2), OVERCAST(3), FOG(45), RIME_FOG(48),
    LIGHT_DRIZZLE(51), DRIZZLE(53), HEAVY_DRIZZLE(55), FREEZING_DRIZZLE(56), HEAVY_FREEZING_DRIZZLE(57),
    LIGHT_RAIN(61), RAIN(63), HEAVY_RAIN(65), FREEZING_RAIN(66), HEAVY_FREEZING_RAIN(67),
    LIGHT_SNOW(71), SNOW(73), HEAVY_SNOW(75), SNOW_GRAINS(77),
    LIGHT_SHOWERS(80), SHOWERS(81), HEAVY_SHOWERS(82), SNOW_SHOWERS(85), HEAVY_SNOW_SHOWERS(86),
    THUNDERSTORM(95), THUNDERSTORM_HAIL(96), HEAVY_THUNDERSTORM_HAIL(99), CLOUDY(-2), SLEET(-3), ICE_PELLETS(-4), FREEZING_RAIN_UNSPECIFIED(-5), RAIN_UNSPECIFIED(-6), SNOW_UNSPECIFIED(-7);

}

@Serializable
data class ProviderWeather(
    val provider: WeatherProviderId,
    val timezone: String? = null,
    val current: CurrentWeather = CurrentWeather(),
    val temperatureUnit: String = "°C",
    val daily: List<ForecastDay> = emptyList(),
    val hourly: List<ForecastHour> = emptyList()
)

@Serializable
data class CurrentWeather(
    val temperature: Double? = null,
    val feelsLike: Double? = null,
    val humidity: Int? = null,
    val windSpeed: Double? = null,
    val condition: WeatherCondition? = null,
    val isDay: Int? = null
)

@Serializable
data class ForecastDay(
    val date: String,
    val condition: WeatherCondition? = null,
    val temperatureMin: Double? = null,
    val temperatureMax: Double? = null,
    val precipitationSum: Double? = null,
    val sunrise: String? = null,
    val sunset: String? = null,
    val windSpeedMax: Double? = null,
    val windDirectionDominant: Int? = null,
    val uvIndexMax: Double? = null
)

@Serializable
data class ForecastHour(
    val epochSeconds: Long,
    val condition: WeatherCondition? = null,
    val temperature: Double? = null,
    val humidity: Int? = null,
    val windSpeed: Double? = null
)

data class SearchLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    val admin1: String? = null
)

data class ProviderCapabilities(
    val maxForecastDays: Int,
    val dailyPrecipitation: Boolean = true,
    val dailyUv: Boolean = true,
    val dailyWind: Boolean = true,
    val dayNight: Boolean = true,
    val wind: Boolean = true,
    val sunTimes: Boolean = true
) {
    fun effectivePrefs(prefs: WeatherDisplayPrefs) = prefs.copy(
        forecastDays = prefs.forecastDays.coerceAtMost(maxForecastDays),
        showForecastDays = prefs.showForecastDays && maxForecastDays > 0,
        showWind = prefs.showWind && wind,
        showSunTimes = prefs.showSunTimes && sunTimes,
        showPrecipitation = prefs.showPrecipitation && dailyPrecipitation,
        showUvIndex = prefs.showUvIndex && dailyUv
    )
}
