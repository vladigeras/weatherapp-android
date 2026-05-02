package ru.vladigeras.weatherapp.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

val Context.weatherCacheDataStore: DataStore<Preferences> by preferencesDataStore(name = "weather_cache")

@Module
@InstallIn(SingletonComponent::class)
object WeatherCacheModule {
    @Provides
    @Singleton
    fun provideWeatherCacheDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.weatherCacheDataStore
    }
}
