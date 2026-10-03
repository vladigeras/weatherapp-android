package ru.vladigeras.weatherapp.util

import io.mockk.mockk
import ru.vladigeras.weatherapp.data.WeatherDisplayPrefs
import ru.vladigeras.weatherapp.data.WeatherResponse
import ru.vladigeras.weatherapp.network.GeocodingResult
import ru.vladigeras.weatherapp.data.SearchLocation
import ru.vladigeras.weatherapp.network.OpenMeteoWeatherProvider
import ru.vladigeras.weatherapp.repository.WeatherParamsBuilder

fun WeatherResponse.asProviderWeather(prefs: WeatherDisplayPrefs = WeatherDisplayPrefs()) =
    OpenMeteoWeatherProvider(mockk(), mockk(), WeatherParamsBuilder()).normalize(this, prefs)

fun GeocodingResult.asSearchLocation() = SearchLocation(id.toString(), name, latitude, longitude, country, countryCode, admin1)
