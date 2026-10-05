package ru.vladigeras.weatherapp.network

import ru.vladigeras.weatherapp.data.ProviderCapabilities
import ru.vladigeras.weatherapp.data.ProviderWeather
import ru.vladigeras.weatherapp.data.WeatherDisplayPrefs
import ru.vladigeras.weatherapp.data.WeatherProviderId

interface WeatherProvider {
    val id: WeatherProviderId
    val capabilities: ProviderCapabilities
    suspend fun getWeather(latitude: Double, longitude: Double, prefs: WeatherDisplayPrefs): ProviderWeather
}

class WeatherProviders(providers: List<WeatherProvider>) {
    private val byId = providers.associateBy { it.id }
    operator fun get(id: WeatherProviderId): WeatherProvider = requireNotNull(byId[id])
}
