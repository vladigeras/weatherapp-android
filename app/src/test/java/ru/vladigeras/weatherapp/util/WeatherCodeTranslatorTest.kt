package ru.vladigeras.weatherapp.util

import android.content.Context
import android.content.res.Resources
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class WeatherCodeTranslatorTest {

    private lateinit var mockContext: Context
    private lateinit var mockResources: Resources

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        mockResources = mockk(relaxed = true)
        every { mockContext.resources } returns mockResources
        
        // Mock string array for weather_codes
        val weatherCodes = arrayOf(
            "0|Clear sky",
            "1|Mainly clear",
            "2|Partly cloudy",
            "3|Overcast",
            "45|Fog",
            "48|Depositing rime fog",
            "51|Light drizzle",
            "53|Moderate drizzle",
            "55|Dense drizzle",
            "56|Light freezing drizzle",
            "57|Dense freezing drizzle",
            "61|Slight rain",
            "63|Moderate rain",
            "65|Heavy rain",
            "66|Light freezing rain",
            "67|Heavy freezing rain",
            "71|Slight snow fall",
            "73|Moderate snow fall",
            "75|Heavy snow fall",
            "77|Snow grains",
            "80|Slight rain shower",
            "81|Moderate rain shower",
            "82|Violent rain shower",
            "85|Slight snow shower",
            "86|Heavy snow shower",
            "95|Thunderstorm",
            "96|Slight hail thunderstorm",
            "99|Heavy hail thunderstorm"
        )
        every { mockResources.getStringArray(ru.vladigeras.weatherapp.R.array.weather_codes) } returns weatherCodes
        every { mockContext.getString(ru.vladigeras.weatherapp.R.string.unknown_weather) } returns "Unknown"
    }

    @Test
    fun translate_knownCode_returnsCorrectDescription() {
        val result = WeatherCodeTranslator.translate(mockContext, 0)
        assertEquals("Clear sky", result)
    }

    @Test
    fun translate_unknownCode_returnsUnknownString() {
        val result = WeatherCodeTranslator.translate(mockContext, 999)
        assertEquals("Unknown", result)
    }

    @Test
    fun getWeatherCodeEntries_returnsAllResourceItems() {
        val entries = WeatherCodeTranslator.getWeatherCodeEntries(mockContext)
        assertEquals(28, entries.size)
    }
}
