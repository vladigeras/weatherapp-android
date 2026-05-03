package ru.vladigeras.weatherapp.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import ru.vladigeras.weatherapp.network.GeocodingService
import ru.vladigeras.weatherapp.network.WeatherApiService
import ru.vladigeras.weatherapp.network.WeatherApiServiceImpl
import ru.vladigeras.weatherapp.repository.CitySearchCache
import ru.vladigeras.weatherapp.repository.LanguagePreferenceRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    
    @Provides
    @Singleton
    fun provideHttpClient(json: Json): HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
        install(Logging) {
            level = LogLevel.BODY
        }
        engine {
            requestTimeout = 30_000
            endpoint {
                connectTimeout = 30_000
                socketTimeout = 30_000
            }
        }
    }
    
    @Provides
    @Singleton
    fun provideWeatherApiService(httpClient: HttpClient): WeatherApiService {
        return WeatherApiServiceImpl(httpClient)
    }
    
    @Provides
    @Singleton
    fun provideGeocodingService(httpClient: HttpClient): GeocodingService {
        return GeocodingService(httpClient)
    }
    
    @Provides
    @Singleton
    fun provideCitySearchCache(): CitySearchCache = CitySearchCache()
}