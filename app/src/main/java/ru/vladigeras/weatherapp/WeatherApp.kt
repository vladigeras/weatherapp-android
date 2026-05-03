package ru.vladigeras.weatherapp

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.runBlocking
import ru.vladigeras.weatherapp.repository.LanguagePreference
import ru.vladigeras.weatherapp.repository.LanguagePreferenceRepository
import javax.inject.Inject

@HiltAndroidApp
class WeatherApp : Application() {

    @Inject
    lateinit var languagePreferenceRepository: LanguagePreferenceRepository

    override fun onCreate() {
        super.onCreate()
        applyLocale()
    }

    private fun applyLocale() {
        val localeCode = runBlocking { languagePreferenceRepository.getEffectiveLocaleCode() }
        val localeList = LocaleListCompat.forLanguageTags(localeCode)
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}