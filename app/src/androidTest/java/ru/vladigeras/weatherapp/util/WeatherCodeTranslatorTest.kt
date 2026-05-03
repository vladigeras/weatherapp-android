package ru.vladigeras.weatherapp.util

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WeatherCodeTranslatorTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun translate_knownCode_returnsCorrectDescription() {
        val result = WeatherCodeTranslator.translate(context, 0)
        assertEquals("Clear sky", result)
    }

    @Test
    fun translate_unknownCode_returnsUnknownString() {
        val result = WeatherCodeTranslator.translate(context, 999)
        assertEquals(context.getString(ru.vladigeras.weatherapp.R.string.unknown_weather), result)
    }

    @Test
    fun getWeatherCodeEntries_returnsAllResourceItems() {
        val entries = WeatherCodeTranslator.getWeatherCodeEntries(context)
        assertEquals(28, entries.size)
    }
}
