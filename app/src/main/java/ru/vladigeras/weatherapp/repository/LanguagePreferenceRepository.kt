package ru.vladigeras.weatherapp.repository

interface LanguagePreferenceRepository {
    suspend fun getLanguagePreference(): LanguagePreference
    suspend fun saveLanguagePreference(preference: LanguagePreference)
    suspend fun getEffectiveLocaleCode(): String
    suspend fun getAppLocale(): java.util.Locale
}