package ru.vladigeras.weatherapp.util

import io.mockk.mockk
import ru.vladigeras.weatherapp.data.WeatherDisplayPrefs
import ru.vladigeras.weatherapp.data.WeatherResponse
import ru.vladigeras.weatherapp.network.OpenMeteoWeatherProvider
import ru.vladigeras.weatherapp.repository.WeatherParamsBuilder

fun WeatherResponse.asProviderWeather(prefs: WeatherDisplayPrefs = WeatherDisplayPrefs()) =
    OpenMeteoWeatherProvider(mockk(), WeatherParamsBuilder()).normalize(this, prefs)
