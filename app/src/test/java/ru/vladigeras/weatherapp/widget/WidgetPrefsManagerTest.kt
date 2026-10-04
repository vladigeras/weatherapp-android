package ru.vladigeras.weatherapp.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WidgetPrefsManagerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        WidgetPrefsManager.clear(context)
    }

    @Test
    fun save_and_read_returnsCorrectData() {
        WidgetPrefsManager.save(
            context = context,
            cityName = "Moscow",
            temperature = 25.5,
            feelsLike = 24.0,
            weatherCode = 0,
            isDay = 1,
            tempUnit = "°C"
        )

        assertEquals("Moscow", WidgetPrefsManager.getCityName(context))
        assertEquals("25°C", WidgetPrefsManager.getTemperature(context))
        assertEquals("24°C", WidgetPrefsManager.getFeelsLike(context))
        assertEquals(0, WidgetPrefsManager.getWeatherCode(context))
        assertEquals(1, WidgetPrefsManager.getIsDay(context))
        assertTrue(WidgetPrefsManager.hasData(context))
    }

    @Test
    fun save_withNullFeelsLike_doesNotPersistKey() {
        WidgetPrefsManager.save(context, "Moscow", 25.5, 24.0, 0, 1, "°C")
        assertEquals("24°C", WidgetPrefsManager.getFeelsLike(context))
        WidgetPrefsManager.save(
            context = context,
            cityName = "Moscow",
            temperature = 25.5,
            feelsLike = null,
            weatherCode = 0,
            isDay = 1,
            tempUnit = "°C"
        )

        assertFalse(readStoredPrefs().contains(stringPreferencesKey("feels_like")))
        assertEquals(null, WidgetPrefsManager.getFeelsLike(context))
    }

    @Test
    fun clear_removesAllKeys() {
        WidgetPrefsManager.save(
            context = context,
            cityName = "Moscow",
            temperature = 25.5,
            feelsLike = 24.0,
            weatherCode = 0,
            isDay = 1,
            tempUnit = "°C"
        )

        WidgetPrefsManager.clear(context)

        assertTrue(readStoredPrefs().asMap().isEmpty())
        assertFalse(WidgetPrefsManager.hasData(context))
    }

    @Test
    fun hasData_returnsFalseWhenEmpty() {
        assertFalse(WidgetPrefsManager.hasData(context))
    }

    @Test
    fun getWeatherCode_returnsNullWhenNotSet() {
        assertEquals(null, WidgetPrefsManager.getWeatherCode(context))
    }

    @Test
    fun getIsDay_returnsNullWhenNotSet() {
        assertEquals(null, WidgetPrefsManager.getIsDay(context))
    }
    @Test
    fun providerChange_clearsOldWeatherAndRejectsStaleSave() {
        val context = RuntimeEnvironment.getApplication()
        val wttr = ru.vladigeras.weatherapp.data.WeatherProviderId.WTTR
        val openMeteo = ru.vladigeras.weatherapp.data.WeatherProviderId.OPEN_METEO
        WidgetPrefsManager.save(context, "Old", 18.0, 17.0, 0, 1, "°C", openMeteo)
        WidgetPrefsManager.activateProvider(context, wttr)
        assertEquals(false, WidgetPrefsManager.hasData(context))
        WidgetPrefsManager.save(context, "New", 27.0, 26.0, 0, null, "°C", wttr)
        WidgetPrefsManager.save(context, "Late", 18.0, 17.0, 0, 1, "°C", openMeteo)
        assertEquals("New", WidgetPrefsManager.getCityName(context))
        assertEquals(wttr, WidgetPrefsManager.getProvider(context))
        assertEquals(null, WidgetPrefsManager.getIsDay(context))
        WidgetPrefsManager.activateProvider(context, openMeteo)
        assertEquals(false, WidgetPrefsManager.hasData(context))
    }

    private fun readStoredPrefs(): Preferences = runBlocking {
        val method = WidgetPrefsManager::class.java.getDeclaredMethod("getDataStore", Context::class.java)
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val store = method.invoke(WidgetPrefsManager, context) as DataStore<Preferences>
        store.data.first()
    }

}