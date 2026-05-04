package ru.vladigeras.weatherapp.core.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import ru.vladigeras.weatherapp.repository.LanguagePreference

object LanguageManager {

    fun applyLocale(preference: LanguagePreference) {
        val localeTag = when (preference) {
            LanguagePreference.SYSTEM -> ""
            LanguagePreference.RUSSIAN -> "ru"
            LanguagePreference.ENGLISH -> "en"
        }
        if (localeTag.isEmpty()) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(localeTag))
        }
    }
}