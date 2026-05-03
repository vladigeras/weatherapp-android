package ru.vladigeras.weatherapp.widget

import android.content.Context
import android.content.SharedPreferences

object WidgetPrefsManager {
    private const val PREFS_NAME = "widget_prefs"
    private const val KEY_CITY_NAME = "city_name"
    private const val KEY_TEMP = "temp"
    private const val KEY_FEELS_LIKE = "feels_like"
    private const val KEY_WEATHER_CODE = "weather_code"
    private const val KEY_IS_DAY = "is_day"
    private const val KEY_TEMP_UNIT = "temp_unit"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun save(
        context: Context,
        cityName: String,
        temperature: Double,
        feelsLike: Double?,
        weatherCode: Int,
        isDay: Int,
        tempUnit: String
    ) {
        val prefs = getPrefs(context)
        prefs.edit().apply {
            putString(KEY_CITY_NAME, cityName)
            putString(KEY_TEMP, "${temperature.toInt()}$tempUnit")
            if (feelsLike != null) {
                putString(KEY_FEELS_LIKE, "${feelsLike.toInt()}$tempUnit")
            } else {
                remove(KEY_FEELS_LIKE)
            }
            putInt(KEY_WEATHER_CODE, weatherCode)
            putInt(KEY_IS_DAY, isDay)
            putString(KEY_TEMP_UNIT, tempUnit)
            apply()
        }
    }

    fun getCityName(context: Context): String? {
        return getPrefs(context).getString(KEY_CITY_NAME, null)
    }

    fun getTemperature(context: Context): String? {
        return getPrefs(context).getString(KEY_TEMP, null)
    }

    fun getFeelsLike(context: Context): String? {
        return getPrefs(context).getString(KEY_FEELS_LIKE, null)
    }

    fun getWeatherCode(context: Context): Int? {
        val prefs = getPrefs(context)
        return if (prefs.contains(KEY_WEATHER_CODE)) prefs.getInt(KEY_WEATHER_CODE, 0) else null
    }

    fun getIsDay(context: Context): Int? {
        val prefs = getPrefs(context)
        return if (prefs.contains(KEY_IS_DAY)) prefs.getInt(KEY_IS_DAY, 1) else null
    }

    fun getTempUnit(context: Context): String? {
        return getPrefs(context).getString(KEY_TEMP_UNIT, null)
    }

    fun hasData(context: Context): Boolean {
        return getCityName(context) != null
    }

    fun clear(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}