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
import ru.vladigeras.weatherapp.data.WeatherProviderId

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
        temperature: Double?,
        feelsLike: Double?,
        weatherCode: Int?,
        isDay: Int?,
        tempUnit: String,
        provider: WeatherProviderId = WeatherProviderId.OPEN_METEO
    ) {
        runBlocking(Dispatchers.IO) {
            getDataStore(context).edit { prefs ->
                if (WeatherProviderId.fromValue(prefs[stringPreferencesKey("provider")]) != provider) return@edit
                prefs[stringPreferencesKey("provider")] = provider.value
                prefs[stringPreferencesKey(KEY_CITY_NAME)] = cityName
                if (temperature == null) prefs.remove(stringPreferencesKey(KEY_TEMP))
                else prefs[stringPreferencesKey(KEY_TEMP)] = "${temperature.toInt()}$tempUnit"
                if (feelsLike != null) {
                    prefs[stringPreferencesKey(KEY_FEELS_LIKE)] = "${feelsLike.toInt()}$tempUnit"
                } else {
                    prefs.remove(stringPreferencesKey(KEY_FEELS_LIKE))
                }
                if (weatherCode == null) prefs.remove(intPreferencesKey(KEY_WEATHER_CODE))
                else prefs[intPreferencesKey(KEY_WEATHER_CODE)] = weatherCode
                if (isDay == null) prefs.remove(intPreferencesKey(KEY_IS_DAY))
                else prefs[intPreferencesKey(KEY_IS_DAY)] = isDay
                prefs[stringPreferencesKey(KEY_TEMP_UNIT)] = tempUnit
            }
        }
    }

    fun activateProvider(context: Context, provider: WeatherProviderId) {
        runBlocking(Dispatchers.IO) {
            getDataStore(context).edit { prefs ->
                if (WeatherProviderId.fromValue(prefs[stringPreferencesKey("provider")]) != provider) prefs.clear()
                prefs[stringPreferencesKey("provider")] = provider.value
            }
        }
    }

    fun getProvider(context: Context): WeatherProviderId = runBlocking(Dispatchers.IO) {
        WeatherProviderId.fromValue(getDataStore(context).data.first()[stringPreferencesKey("provider")])
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