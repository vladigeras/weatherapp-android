package ru.vladigeras.weatherapp.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherIconsTest {

    @Test
    fun getWeatherIconForCode_clearSkyDay_returnsSunIcon() {
        val result = getWeatherIconForCode(weatherCode = 0, isDay = 1)
        assertEquals(Icons.Filled.WbSunny, result)
    }

    @Test
    fun getWeatherIconForCode_clearSkyNight_returnsMoonIcon() {
        val result = getWeatherIconForCode(weatherCode = 0, isDay = 0)
        assertEquals(Icons.Filled.NightsStay, result)
    }

    @Test
    fun getWeatherIconForCode_rain_returnsDropIcon() {
        val codes = listOf(51, 61, 80)
        for (code in codes) {
            val result = getWeatherIconForCode(weatherCode = code, isDay = 1)
            assertEquals("Code $code should return WaterDrop", Icons.Filled.WaterDrop, result)
        }
    }

    @Test
    fun getWeatherIconForCode_snow_returnsAcUnitIcon() {
        val codes = listOf(71, 73, 75)
        for (code in codes) {
            val result = getWeatherIconForCode(weatherCode = code, isDay = 1)
            assertEquals("Code $code should return AcUnit", Icons.Filled.AcUnit, result)
        }
    }

    @Test
    fun getWeatherIconForCode_thunderstorm_returnsThunderIcon() {
        val codes = listOf(95, 96, 99)
        for (code in codes) {
            val result = getWeatherIconForCode(weatherCode = code, isDay = 1)
            assertEquals("Code $code should return Thunderstorm", Icons.Filled.Thunderstorm, result)
        }
    }

    @Test
    fun getPrecipitationIconForCode_clearSky_returnsNull() {
        val result = getPrecipitationIconForCode(weatherCode = 0)
        assertEquals(null, result)
    }

    @Test
    fun getPrecipitationIconForCode_drizzle_returnsDropIcon() {
        val result = getPrecipitationIconForCode(weatherCode = 51)
        assertEquals(Icons.Filled.WaterDrop, result)
    }
}
