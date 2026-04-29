package ru.vladigeras.weatherapp.repository

interface LanguagePreferenceRepository {
    suspend fun getLanguagePreference(): LanguagePreference
    suspend fun saveLanguagePreference(preference: LanguagePreference)
    suspend fun getEffectiveLocaleCode(): String
    fun getAppLocale(): java.util.Locale
}