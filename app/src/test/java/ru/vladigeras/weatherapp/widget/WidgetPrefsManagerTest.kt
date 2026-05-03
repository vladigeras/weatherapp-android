package ru.vladigeras.weatherapp.widget

import android.content.Context
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
        assertEquals("25.5°C", WidgetPrefsManager.getTemperature(context))
        assertEquals("24.0°C", WidgetPrefsManager.getFeelsLike(context))
        assertEquals(0, WidgetPrefsManager.getWeatherCode(context))
        assertEquals(1, WidgetPrefsManager.getIsDay(context))
        assertTrue(WidgetPrefsManager.hasData(context))
    }

    @Test
    fun save_withNullFeelsLike_doesNotPersistKey() {
        WidgetPrefsManager.save(
            context = context,
            cityName = "Moscow",
            temperature = 25.5,
            feelsLike = null,
            weatherCode = 0,
            isDay = 1,
            tempUnit = "°C"
        )

        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        assertFalse(prefs.contains("feels_like"))
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

        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        assertTrue(prefs.all.isEmpty())
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
}