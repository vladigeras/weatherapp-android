package ru.vladigeras.weatherapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WeatherApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // AppCompatDelegate автоматически управляет локалью для AppCompatActivity.
        // Ручное применение через runBlocking удалено во избежание ANR и конфликтов.
    }
}
