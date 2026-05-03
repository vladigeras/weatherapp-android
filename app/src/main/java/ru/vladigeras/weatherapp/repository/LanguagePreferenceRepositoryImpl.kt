package ru.vladigeras.weatherapp.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class LanguagePreferenceRepositoryImpl @Inject constructor(
    @Named("language") private val dataStore: DataStore<Preferences>
) : LanguagePreferenceRepository {

    companion object {
        private val LANGUAGE_KEY = intPreferencesKey("language_preference")
    }

    override suspend fun getLanguagePreference(): LanguagePreference {
        return try {
            val ordinal = dataStore.data.map { preferences ->
                preferences[LANGUAGE_KEY] ?: -1
            }.first()

            when (ordinal) {
                0 -> LanguagePreference.SYSTEM
                1 -> LanguagePreference.RUSSIAN
                2 -> LanguagePreference.ENGLISH
                else -> LanguagePreference.SYSTEM
            }
        } catch (e: Exception) {
            e.printStackTrace()
            LanguagePreference.SYSTEM
        }
    }

    override suspend fun saveLanguagePreference(preference: LanguagePreference) {
        val ordinal = when (preference) {
            LanguagePreference.SYSTEM -> 0
            LanguagePreference.RUSSIAN -> 1
            LanguagePreference.ENGLISH -> 2
        }
        dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = ordinal
        }
    }

    override suspend fun getEffectiveLocaleCode(): String {
        val pref = getLanguagePreference()
        return when (pref) {
            LanguagePreference.RUSSIAN -> "ru"
            LanguagePreference.ENGLISH -> "en"
            LanguagePreference.SYSTEM -> {
                val deviceLang = Locale.getDefault().language.take(2)
                if (deviceLang == "ru" || deviceLang == "en") deviceLang else "en"
            }
        }
    }

    override fun getAppLocale(): Locale {
        val ordinal = try {
            runBlocking {
                dataStore.data.map { preferences ->
                    preferences[LANGUAGE_KEY] ?: -1
                }.first()
            }
        } catch (e: Exception) {
            -1
        }

        return when (ordinal) {
            1 -> Locale("ru", "RU")
            2 -> Locale.ENGLISH
            else -> {
                val deviceLang = Locale.getDefault()
                if (deviceLang.language == "ru") {
                    Locale("ru", "RU")
                } else {
                    Locale.ENGLISH
                }
            }
        }
    }
}