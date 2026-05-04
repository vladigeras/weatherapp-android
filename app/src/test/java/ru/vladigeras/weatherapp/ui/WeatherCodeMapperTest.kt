package ru.vladigeras.weatherapp.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.vladigeras.weatherapp.R
import ru.vladigeras.weatherapp.util.WeatherCodeMapper

class WeatherCodeMapperTest {

    @Test
    fun getIconVector_clearSkyDay_returnsSunIcon() {
        val result = WeatherCodeMapper.getIconVector(code = 0, isDay = 1)
        assertEquals(Icons.Filled.WbSunny, result)
    }

    @Test
    fun getIconVector_clearSkyNight_returnsMoonIcon() {
        val result = WeatherCodeMapper.getIconVector(code = 0, isDay = 0)
        assertEquals(Icons.Filled.NightsStay, result)
    }

    @Test
    fun getIconVector_rain_returnsDropIcon() {
        val codes = listOf(51, 61, 80)
        for (code in codes) {
            val result = WeatherCodeMapper.getIconVector(code = code, isDay = 1)
            assertEquals("Code $code should return WaterDrop", Icons.Filled.WaterDrop, result)
        }
    }

    @Test
    fun getIconVector_snow_returnsAcUnitIcon() {
        val codes = listOf(71, 73, 75)
        for (code in codes) {
            val result = WeatherCodeMapper.getIconVector(code = code, isDay = 1)
            assertEquals("Code $code should return AcUnit", Icons.Filled.AcUnit, result)
        }
    }

    @Test
    fun getIconVector_thunderstorm_returnsThunderIcon() {
        val codes = listOf(95, 96, 99)
        for (code in codes) {
            val result = WeatherCodeMapper.getIconVector(code = code, isDay = 1)
            assertEquals("Code $code should return Thunderstorm", Icons.Filled.Thunderstorm, result)
        }
    }

    @Test
    fun getPrecipitationIconVector_clearSky_returnsNull() {
        val result = WeatherCodeMapper.getPrecipitationIconVector(code = 0)
        assertEquals(null, result)
    }

    @Test
    fun getPrecipitationIconVector_drizzle_returnsDropIcon() {
        val result = WeatherCodeMapper.getPrecipitationIconVector(code = 51)
        assertEquals(Icons.Filled.WaterDrop, result)
    }

    @Test
    fun getWeatherCodeStringResId_clearSky_returnsCorrectString() {
        val result = WeatherCodeMapper.getWeatherCodeStringResId(code = 0)
        assertEquals(R.string.weather_code_0, result)
    }

    @Test
    fun getWeatherCodeStringResId_rain_returnsCorrectString() {
        val result = WeatherCodeMapper.getWeatherCodeStringResId(code = 61)
        assertEquals(R.string.weather_code_61, result)
    }

    @Test
    fun getWeatherCodeStringResId_unknown_returnsUnknownString() {
        val result = WeatherCodeMapper.getWeatherCodeStringResId(code = 100)
        assertEquals(R.string.unknown_weather, result)
    }
}