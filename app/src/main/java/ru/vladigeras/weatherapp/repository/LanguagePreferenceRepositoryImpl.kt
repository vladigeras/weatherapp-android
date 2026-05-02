package ru.vladigeras.weatherapp.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanguagePreferenceRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : LanguagePreferenceRepository {

    override suspend fun getLanguagePreference(): LanguagePreference {
        return try {
            val prefs = context.getSharedPreferences("weatherapp_prefs", Context.MODE_PRIVATE)
            val ordinal = prefs.getInt("language_preference", -1) // Using -1 to detect unset values
            
            val result = when (ordinal) {
                0 -> LanguagePreference.SYSTEM
                1 -> LanguagePreference.RUSSIAN
                2 -> LanguagePreference.ENGLISH
                else -> LanguagePreference.SYSTEM
            }
            
            result
        } catch (e: Exception) {
            e.printStackTrace()
            LanguagePreference.SYSTEM
        }
    }

    override suspend fun saveLanguagePreference(preference: LanguagePreference) {
        val langKey = "language_preference"
        val ordinal = when (preference) {
            LanguagePreference.SYSTEM -> 0
            LanguagePreference.RUSSIAN -> 1
            LanguagePreference.ENGLISH -> 2
        }
        val prefs = context.getSharedPreferences("weatherapp_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt(langKey, ordinal).apply()
    }

    override suspend fun getEffectiveLocaleCode(): String {
        val pref = getLanguagePreference()
        val result = when (pref) {
            LanguagePreference.RUSSIAN -> "ru"
            LanguagePreference.ENGLISH -> "en"
            LanguagePreference.SYSTEM -> {
                val deviceLang = java.util.Locale.getDefault().language.take(2)
                val finalLang = if (deviceLang == "ru" || deviceLang == "en") deviceLang else "en"
                finalLang
            }
        }
        return result
    }

    override fun getAppLocale(): java.util.Locale {
        val prefs = context.getSharedPreferences("weatherapp_prefs", Context.MODE_PRIVATE)
        val ordinal = prefs.getInt("language_preference", -1)

        return when (ordinal) {
            1 -> java.util.Locale("ru", "RU")
            2 -> java.util.Locale.ENGLISH
            else -> {
                val deviceLang = java.util.Locale.getDefault()
                if (deviceLang.language == "ru") {
                    java.util.Locale("ru", "RU")
                } else {
                    java.util.Locale.ENGLISH
                }
            }
        }
    }
}