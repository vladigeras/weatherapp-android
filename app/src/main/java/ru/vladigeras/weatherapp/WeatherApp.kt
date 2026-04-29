package ru.vladigeras.weatherapp

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WeatherApp : Application() {

    override fun attachBaseContext(base: Context) {
        val locale = getSavedLocale(base)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val config = Configuration(base.resources.configuration)
            config.setLocale(locale)
            val newContext = base.createConfigurationContext(config)
            super.attachBaseContext(newContext)
        } else {
            @Suppress("DEPRECATION")
            val legacyConfig = base.resources.configuration
            legacyConfig.setLocale(locale)
            @Suppress("DEPRECATION")
            base.resources.updateConfiguration(legacyConfig, base.resources.displayMetrics)
            super.attachBaseContext(base)
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Just verify resources are configured correctly

        // NOTE: We do NOT call Locale.getDefault() because Android may reset it
        // Compose uses the context's configuration, not Locale.getDefault()
    }

    private fun getSavedLocale(context: Context): java.util.Locale {
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