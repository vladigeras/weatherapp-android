package ru.vladigeras.weatherapp.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.vladigeras.weatherapp.repository.SelectedLocationRepository
import ru.vladigeras.weatherapp.repository.SelectedLocationRepositoryImpl
import ru.vladigeras.weatherapp.repository.WeatherRepository
import ru.vladigeras.weatherapp.repository.WeatherRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindWeatherRepository(
        weatherRepositoryImpl: WeatherRepositoryImpl
    ): WeatherRepository

    @Binds
    @Singleton
    abstract fun bindSelectedLocationRepository(
        selectedLocationRepositoryImpl: SelectedLocationRepositoryImpl
    ): SelectedLocationRepository
}