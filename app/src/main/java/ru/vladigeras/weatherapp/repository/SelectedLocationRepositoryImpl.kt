package ru.vladigeras.weatherapp.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.vladigeras.weatherapp.data.Location
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "selected_location")

@Singleton
class SelectedLocationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SelectedLocationRepository {

    private object PreferencesKeys {
        val LATITUDE = doublePreferencesKey("latitude")
        val LONGITUDE = doublePreferencesKey("longitude")
        val NAME = stringPreferencesKey("name")
        val IS_AUTO_DETECTED = booleanPreferencesKey("is_auto_detected")
    }

    override fun getSelectedLocation(): Flow<Location?> {
        return context.dataStore.data.map { preferences ->
            try {
                val latitude = preferences[PreferencesKeys.LATITUDE]
                val longitude = preferences[PreferencesKeys.LONGITUDE]
                val name = preferences[PreferencesKeys.NAME]
                val isAutoDetected = preferences[PreferencesKeys.IS_AUTO_DETECTED] ?: false

                if (latitude != null && longitude != null) {
                    Location(
                        latitude = latitude,
                        longitude = longitude,
                        name = name,
                        isAutoDetected = isAutoDetected
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    override suspend fun saveSelectedLocation(location: Location) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LATITUDE] = location.latitude
            preferences[PreferencesKeys.LONGITUDE] = location.longitude
            location.name?.let { preferences[PreferencesKeys.NAME] = it }
            preferences[PreferencesKeys.IS_AUTO_DETECTED] = location.isAutoDetected
        }
    }

    override suspend fun clearSelectedLocation() {
        context.dataStore.edit { it.clear() }
    }
}