package ru.vladigeras.weatherapp.di

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.cache.storage.FileStorage
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import kotlinx.io.files.Path
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import ru.vladigeras.weatherapp.BuildConfig
import ru.vladigeras.weatherapp.network.WeatherApiService
import ru.vladigeras.weatherapp.network.WeatherApiServiceImpl
import ru.vladigeras.weatherapp.network.OpenMeteoWeatherProvider
import ru.vladigeras.weatherapp.network.WeatherProviders
import ru.vladigeras.weatherapp.network.WttrWeatherProvider
import ru.vladigeras.weatherapp.network.SevenTimerWeatherProvider
import ru.vladigeras.weatherapp.network.MetWeatherProvider
import ru.vladigeras.weatherapp.repository.CitySearchCache
import javax.inject.Singleton
import javax.inject.Named
import java.time.Clock

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    fun provideClock(): Clock = Clock.systemUTC()

    
    @Provides
    @Singleton
    @Named("met")
    fun provideMetHttpClient(@ApplicationContext context: Context, client: HttpClient): HttpClient = client.config {
        defaultRequest {
            header(HttpHeaders.UserAgent, "WeatherappAndroid/${BuildConfig.VERSION_NAME} (https://github.com/vladigeras/weatherapp-android)")
            header(HttpHeaders.Accept, "application/json")
            header(HttpHeaders.AcceptEncoding, "gzip, deflate")
        }
        install(HttpCache) {
            publicStorage(FileStorage(Path(context.cacheDir.resolve("met_http").path)))
        }
    }

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
        if (BuildConfig.DEBUG) {
            install(Logging) {
                level = LogLevel.BODY
            }
        }
        install(HttpTimeout) {
            connectTimeoutMillis = 5_000
            socketTimeoutMillis = 10_000
            requestTimeoutMillis = 15_000
        }
    }
    
    @Provides
    @Singleton
    fun provideWeatherApiService(httpClient: HttpClient): WeatherApiService {
        return WeatherApiServiceImpl(httpClient)
    }
    
    @Provides
    @Singleton
    fun provideCitySearchCache(): CitySearchCache = CitySearchCache()

    @Provides
    @Singleton
    fun provideWeatherProviders(openMeteo: OpenMeteoWeatherProvider, wttr: WttrWeatherProvider, sevenTimer: SevenTimerWeatherProvider, met: MetWeatherProvider) =
        WeatherProviders(listOf(openMeteo, wttr, sevenTimer, met))
}
