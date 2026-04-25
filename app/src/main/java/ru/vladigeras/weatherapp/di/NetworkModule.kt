package ru.vladigeras.weatherapp.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import ru.vladigeras.weatherapp.network.WeatherApiService
import ru.vladigeras.weatherapp.network.WeatherApiServiceImpl
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
    fun provideHttpClient(json: Json): HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
        install(Logging) {
            level = LogLevel.BODY
        }
        engine {
            config {
                followRedirects(true)
                connectTimeoutMillis.toLong()
                readTimeoutMillis.toLong()
                writeTimeoutMillis.toLong()
            }
        }
    }
    
    @Provides
    @Singleton
    fun provideWeatherApiService(httpClient: HttpClient): WeatherApiService {
        return WeatherApiServiceImpl(httpClient)
    }
}