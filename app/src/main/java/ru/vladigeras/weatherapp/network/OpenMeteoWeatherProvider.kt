package ru.vladigeras.weatherapp.network

import ru.vladigeras.weatherapp.data.CurrentWeather
import ru.vladigeras.weatherapp.data.ForecastDay
import ru.vladigeras.weatherapp.data.ForecastHour
import ru.vladigeras.weatherapp.data.ProviderCapabilities
import ru.vladigeras.weatherapp.data.ProviderWeather
import ru.vladigeras.weatherapp.data.WeatherCondition
import ru.vladigeras.weatherapp.data.WeatherDisplayPrefs
import ru.vladigeras.weatherapp.data.WeatherProviderId
import ru.vladigeras.weatherapp.data.WeatherResponse
import ru.vladigeras.weatherapp.repository.WeatherParamsBuilder
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class OpenMeteoWeatherProvider @Inject constructor(
    private val weatherApi: WeatherApiService,
    private val paramsBuilder: WeatherParamsBuilder
) : WeatherProvider {
    override val id = WeatherProviderId.OPEN_METEO
    override val capabilities = ProviderCapabilities(maxForecastDays = 16, hourlyStepHours = 1)

    override suspend fun getWeather(latitude: Double, longitude: Double, prefs: WeatherDisplayPrefs): ProviderWeather {
        val (current, hourly, daily) = paramsBuilder.build(prefs)
        return normalize(weatherApi.getWeather(
            latitude, longitude, current, hourly, daily,
            if (prefs.showForecastDays) prefs.forecastDays else 0,
            if (prefs.showHourlyForecast) prefs.hourlyForecastHours + 1 else 0
        ), prefs)
    }

    internal fun normalize(response: WeatherResponse, prefs: WeatherDisplayPrefs): ProviderWeather {
        val zone = ZoneId.of(response.timezone)
        val daily = response.daily
        val hourly = response.hourly
        val conditionsByDate = daily?.time.orEmpty().mapIndexed { i, date ->
            date to condition(daily?.weatherCode?.getOrNull(i))
        }.toMap()
        return ProviderWeather(
            provider = id,
            timezone = zone.id,
            current = CurrentWeather(
                temperature = response.current?.temperature,
                feelsLike = response.current?.apparentTemperature,
                humidity = response.current?.humidity,
                windSpeed = response.current?.windSpeed,
                condition = condition(response.current?.weatherCode),
                isDay = response.current?.isDay
            ),
            temperatureUnit = response.currentUnits?.temperatureUnit ?: "°C",
            daily = if (!prefs.showForecastDays) emptyList() else daily?.time.orEmpty().take(prefs.forecastDays).mapIndexed { i, date ->
                ForecastDay(
                    date, conditionsByDate[date], daily?.temperature2mMin?.getOrNull(i), daily?.temperature2mMax?.getOrNull(i),
                    if (prefs.showPrecipitation) daily?.precipitationSum?.getOrNull(i) else null,
                    if (prefs.showSunTimes) localTime(daily?.sunrise?.getOrNull(i)) else null,
                    if (prefs.showSunTimes) localTime(daily?.sunset?.getOrNull(i)) else null,
                    if (prefs.showWind) daily?.windspeed10mMax?.getOrNull(i) else null,
                    if (prefs.showWind) daily?.winddirection10mDominant?.getOrNull(i) else null,
                    if (prefs.showUvIndex) daily?.uvIndexMax?.getOrNull(i) else null
                )
            },
            hourly = if (!prefs.showHourlyForecast) emptyList() else hourly?.time.orEmpty().take(prefs.hourlyForecastHours + 1).mapIndexed { i, time ->
                val local = LocalDateTime.parse(time)
                ForecastHour(
                    local.atZone(zone).toEpochSecond(), condition(hourly?.weatherCode?.getOrNull(i)),
                    hourly?.temperature2m?.getOrNull(i), hourly?.relativehumidity2m?.getOrNull(i), hourly?.windspeed10m?.getOrNull(i)
                )
            }
        )
    }

    private fun condition(code: Int?) = WeatherCondition.entries.firstOrNull { it.displayCode == code && it.displayCode >= 0 }

    private fun localTime(value: String?): String? = value?.let {
        runCatching { LocalDateTime.parse(it).format(DateTimeFormatter.ofPattern("HH:mm")) }.getOrNull()
    }
}
