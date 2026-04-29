package ru.vladigeras.weatherapp.repository

sealed class LanguagePreference {
    object SYSTEM : LanguagePreference()
    object RUSSIAN : LanguagePreference()
    object ENGLISH : LanguagePreference()
}