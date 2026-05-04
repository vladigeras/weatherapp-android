package ru.vladigeras.weatherapp.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.widgetDataStore: DataStore<Preferences> by preferencesDataStore(name = "widget_prefs")

object WidgetPrefsManager {
    private const val KEY_CITY_NAME = "city_name"
    private const val KEY_TEMP = "temp"
    private const val KEY_FEELS_LIKE = "feels_like"
    private const val KEY_WEATHER_CODE = "weather_code"
    private const val KEY_IS_DAY = "is_day"
    private const val KEY_TEMP_UNIT = "temp_unit"

    private fun getDataStore(context: Context): DataStore<Preferences> = context.widgetDataStore

    fun save(
        context: Context,
        cityName: String,
        temperature: Double,
        feelsLike: Double?,
        weatherCode: Int,
        isDay: Int,
        tempUnit: String
    ) {
        runBlocking(Dispatchers.IO) {
            getDataStore(context).edit { prefs ->
                prefs[stringPreferencesKey(KEY_CITY_NAME)] = cityName
                prefs[stringPreferencesKey(KEY_TEMP)] = "${temperature.toInt()}$tempUnit"
                if (feelsLike != null) {
                    prefs[stringPreferencesKey(KEY_FEELS_LIKE)] = "${feelsLike.toInt()}$tempUnit"
                } else {
                    prefs.remove(stringPreferencesKey(KEY_FEELS_LIKE))
                }
                prefs[intPreferencesKey(KEY_WEATHER_CODE)] = weatherCode
                prefs[intPreferencesKey(KEY_IS_DAY)] = isDay
                prefs[stringPreferencesKey(KEY_TEMP_UNIT)] = tempUnit
            }
        }
    }

    fun getCityName(context: Context): String? {
        return runBlocking(Dispatchers.IO) {
            getDataStore(context).data.first()[stringPreferencesKey(KEY_CITY_NAME)]
        }
    }

    fun getTemperature(context: Context): String? {
        return runBlocking(Dispatchers.IO) {
            getDataStore(context).data.first()[stringPreferencesKey(KEY_TEMP)]
        }
    }

    fun getFeelsLike(context: Context): String? {
        return runBlocking(Dispatchers.IO) {
            getDataStore(context).data.first()[stringPreferencesKey(KEY_FEELS_LIKE)]
        }
    }

    fun getWeatherCode(context: Context): Int? {
        return runBlocking(Dispatchers.IO) {
            getDataStore(context).data.first()[intPreferencesKey(KEY_WEATHER_CODE)]
        }
    }

    fun getIsDay(context: Context): Int? {
        return runBlocking(Dispatchers.IO) {
            getDataStore(context).data.first()[intPreferencesKey(KEY_IS_DAY)]
        }
    }

    fun getTempUnit(context: Context): String? {
        return runBlocking(Dispatchers.IO) {
            getDataStore(context).data.first()[stringPreferencesKey(KEY_TEMP_UNIT)]
        }
    }

    fun hasData(context: Context): Boolean {
        return runBlocking(Dispatchers.IO) {
            getDataStore(context).data.first().contains(stringPreferencesKey(KEY_CITY_NAME))
        }
    }

    fun clear(context: Context) {
        runBlocking(Dispatchers.IO) {
            getDataStore(context).edit { it.clear() }
        }
    }
}