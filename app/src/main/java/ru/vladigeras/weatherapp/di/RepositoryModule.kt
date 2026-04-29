package ru.vladigeras.weatherapp.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.vladigeras.weatherapp.repository.CityNameResolver
import ru.vladigeras.weatherapp.repository.CityNameResolverImpl
import ru.vladigeras.weatherapp.repository.LanguagePreferenceRepository
import ru.vladigeras.weatherapp.repository.LanguagePreferenceRepositoryImpl
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

    @Binds
    @Singleton
    abstract fun bindLanguagePreferenceRepository(
        languagePreferenceRepositoryImpl: LanguagePreferenceRepositoryImpl
    ): LanguagePreferenceRepository

    @Binds
    @Singleton
    abstract fun bindCityNameResolver(
        cityNameResolverImpl: CityNameResolverImpl
    ): CityNameResolver
}