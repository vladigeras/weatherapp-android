package ru.vladigeras.weatherapp.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.vladigeras.weatherapp.data.WeatherDisplayPrefs
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherDisplayPrefsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
        name = "weather_display_prefs"
    )

    private object Keys {
        val SHOW_HUMIDITY = booleanPreferencesKey("show_humidity")
        val SHOW_WIND = booleanPreferencesKey("show_wind")
        val SHOW_PRECIPITATION = booleanPreferencesKey("show_precipitation")
        val SHOW_CONDITION = booleanPreferencesKey("show_condition")
        val SHOW_SUN_TIMES = booleanPreferencesKey("show_sun_times")
        val SHOW_UV_INDEX = booleanPreferencesKey("show_uv_index")
        val SHOW_FORECAST = booleanPreferencesKey("show_forecast")
        val FORECAST_DAYS = intPreferencesKey("forecast_days")
    }

    fun getPrefs(): Flow<WeatherDisplayPrefs> =
        context.dataStore.data.map { prefs ->
            WeatherDisplayPrefs(
                showHumidity = prefs[Keys.SHOW_HUMIDITY] ?: true,
                showWind = prefs[Keys.SHOW_WIND] ?: true,
                showPrecipitation = prefs[Keys.SHOW_PRECIPITATION] ?: true,
                showCondition = prefs[Keys.SHOW_CONDITION] ?: true,
                showSunTimes = prefs[Keys.SHOW_SUN_TIMES] ?: true,
                showUvIndex = prefs[Keys.SHOW_UV_INDEX] ?: true,
                showForecast = prefs[Keys.SHOW_FORECAST] ?: true,
                forecastDays = prefs[Keys.FORECAST_DAYS] ?: 1
            )
        }

    suspend fun updatePrefs(prefs: WeatherDisplayPrefs) {
        context.dataStore.edit { it ->
            it[Keys.SHOW_HUMIDITY] = prefs.showHumidity
            it[Keys.SHOW_WIND] = prefs.showWind
            it[Keys.SHOW_PRECIPITATION] = prefs.showPrecipitation
            it[Keys.SHOW_CONDITION] = prefs.showCondition
            it[Keys.SHOW_SUN_TIMES] = prefs.showSunTimes
            it[Keys.SHOW_UV_INDEX] = prefs.showUvIndex
            it[Keys.SHOW_FORECAST] = prefs.showForecast
            it[Keys.FORECAST_DAYS] = prefs.forecastDays
        }
    }
}