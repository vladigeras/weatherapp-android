package ru.vladigeras.weatherapp.repository

import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.withTimeoutOrNull
import ru.vladigeras.weatherapp.BuildConfig
import ru.vladigeras.weatherapp.data.ProviderWeather
import ru.vladigeras.weatherapp.data.ProviderCapabilities
import ru.vladigeras.weatherapp.data.WeatherDisplayPrefs
import ru.vladigeras.weatherapp.data.WeatherProviderId
import ru.vladigeras.weatherapp.network.WeatherProviders
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

interface WeatherRepository {
    fun capabilities(provider: WeatherProviderId): ProviderCapabilities
    suspend fun getWeather(latitude: Double, longitude: Double, prefs: WeatherDisplayPrefs = WeatherDisplayPrefs(), forceRefresh: Boolean = false): Result<ProviderWeather>
}

@Singleton
class WeatherRepositoryImpl @Inject constructor(
    private val providers: WeatherProviders,
    private val weatherCache: WeatherCache
) : WeatherRepository {
    override fun capabilities(provider: WeatherProviderId) = providers[provider].capabilities
    override suspend fun getWeather(latitude: Double, longitude: Double, prefs: WeatherDisplayPrefs, forceRefresh: Boolean): Result<ProviderWeather> {
        val provider = providers[prefs.provider]
        val effectivePrefs = provider.capabilities.effectivePrefs(prefs)
        if (!forceRefresh) {
            weatherCache.getWeather(latitude, longitude, effectivePrefs)?.let { return Result.success(it) }
        }
        return try {
            val response = withTimeoutOrNull(30_000) {
                provider.getWeather(latitude, longitude, effectivePrefs)
            } ?: throw HttpRequestTimeoutException(
                when (prefs.provider) {
                    WeatherProviderId.OPEN_METEO -> BuildConfig.API_URL
                    WeatherProviderId.WTTR -> BuildConfig.WTTR_API_URL
                    WeatherProviderId.SEVEN_TIMER -> BuildConfig.SEVEN_TIMER_API_URL
                },
                30_000
            )
            weatherCache.putWeather(latitude, longitude, response, effectivePrefs)
            Result.success(response)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}
