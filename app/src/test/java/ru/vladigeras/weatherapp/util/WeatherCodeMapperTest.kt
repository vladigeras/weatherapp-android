package ru.vladigeras.weatherapp.util

import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Hail
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WeatherCodeMapperTest {

    @Test
    fun `getWeatherType returns CLEAR for code 0`() {
        assertEquals(WeatherType.CLEAR, WeatherCodeMapper.getWeatherType(0))
    }

    @Test
    fun `getWeatherType returns CLOUDY for codes 1-3, 45, 48`() {
        assertEquals(WeatherType.CLOUDY, WeatherCodeMapper.getWeatherType(1))
        assertEquals(WeatherType.CLOUDY, WeatherCodeMapper.getWeatherType(2))
        assertEquals(WeatherType.CLOUDY, WeatherCodeMapper.getWeatherType(3))
        assertEquals(WeatherType.CLOUDY, WeatherCodeMapper.getWeatherType(45))
        assertEquals(WeatherType.CLOUDY, WeatherCodeMapper.getWeatherType(48))
    }

    @Test
    fun `getWeatherType returns RAIN for codes 51, 53, 55, 61, 63, 65, 80, 81, 82, 95, 96, 99`() {
        val rainCodes = listOf(51, 53, 55, 61, 63, 65, 80, 81, 82, 95, 96, 99)
        rainCodes.forEach { code ->
            assertEquals(WeatherType.RAIN, WeatherCodeMapper.getWeatherType(code))
        }
    }

    @Test
    fun `getWeatherType returns SNOW for codes 56, 57, 66, 67, 71, 73, 75, 77, 85, 86`() {
        val snowCodes = listOf(56, 57, 66, 67, 71, 73, 75, 77, 85, 86)
        snowCodes.forEach { code ->
            assertEquals(WeatherType.SNOW, WeatherCodeMapper.getWeatherType(code))
        }
    }

    @Test
    fun `getWeatherType returns CLOUDY for unknown codes`() {
        assertEquals(WeatherType.CLOUDY, WeatherCodeMapper.getWeatherType(999))
        assertEquals(WeatherType.CLOUDY, WeatherCodeMapper.getWeatherType(-1))
    }

    @Test
    fun `getIconVector returns sunny icon for code 0 during day`() {
        assertEquals(Icons.Filled.WbSunny, WeatherCodeMapper.getIconVector(0, 1))
    }

    @Test
    fun `getIconVector returns night icon for code 0 during night`() {
        assertEquals(Icons.Filled.NightsStay, WeatherCodeMapper.getIconVector(0, 0))
    }

    @Test
    fun `getIconVector returns partly cloudy for code 1 during day`() {
        assertEquals(Icons.Filled.WbCloudy, WeatherCodeMapper.getIconVector(1, 1))
    }

    @Test
    fun `getIconVector returns cloud for code 1 during night`() {
        assertEquals(Icons.Filled.Cloud, WeatherCodeMapper.getIconVector(1, 0))
    }

    @Test
    fun `getIconVector returns correct icons for rain codes`() {
        assertEquals(Icons.Filled.WaterDrop, WeatherCodeMapper.getIconVector(51, 1))
        assertEquals(Icons.Filled.WaterDrop, WeatherCodeMapper.getIconVector(53, 1))
        assertEquals(Icons.Filled.WaterDrop, WeatherCodeMapper.getIconVector(55, 1))
    }

    @Test
    fun `getIconVector returns correct icons for hail codes`() {
        assertEquals(Icons.Filled.Hail, WeatherCodeMapper.getIconVector(56, 1))
        assertEquals(Icons.Filled.Hail, WeatherCodeMapper.getIconVector(57, 1))
    }

    @Test
    fun `getIconVector returns correct icons for snow codes`() {
        assertEquals(Icons.Filled.AcUnit, WeatherCodeMapper.getIconVector(71, 1))
        assertEquals(Icons.Filled.AcUnit, WeatherCodeMapper.getIconVector(73, 1))
        assertEquals(Icons.Filled.AcUnit, WeatherCodeMapper.getIconVector(75, 1))
    }

    @Test
    fun `getIconVector returns correct icons for snow grains`() {
        assertEquals(Icons.Filled.Grain, WeatherCodeMapper.getIconVector(77, 1))
    }

    @Test
    fun `getIconVector returns correct icons for thunderstorm codes`() {
        assertEquals(Icons.Filled.Thunderstorm, WeatherCodeMapper.getIconVector(95, 1))
        assertEquals(Icons.Filled.Thunderstorm, WeatherCodeMapper.getIconVector(96, 1))
        assertEquals(Icons.Filled.Thunderstorm, WeatherCodeMapper.getIconVector(99, 1))
    }

    @Test
    fun `getIconVector returns cloud for unknown codes`() {
        assertEquals(Icons.Filled.Cloud, WeatherCodeMapper.getIconVector(999, 1))
        assertEquals(Icons.Filled.Cloud, WeatherCodeMapper.getIconVector(-1, 1))
    }

    @Test
    fun `getPrecipitationIconVector returns water drop for rain codes`() {
        assertEquals(Icons.Filled.WaterDrop, WeatherCodeMapper.getPrecipitationIconVector(51))
        assertEquals(Icons.Filled.WaterDrop, WeatherCodeMapper.getPrecipitationIconVector(53))
        assertEquals(Icons.Filled.WaterDrop, WeatherCodeMapper.getPrecipitationIconVector(55))
        assertEquals(Icons.Filled.WaterDrop, WeatherCodeMapper.getPrecipitationIconVector(80))
    }

    @Test
    fun `getPrecipitationIconVector returns hail for hail codes`() {
        assertEquals(Icons.Filled.Hail, WeatherCodeMapper.getPrecipitationIconVector(56))
        assertEquals(Icons.Filled.Hail, WeatherCodeMapper.getPrecipitationIconVector(57))
        assertEquals(Icons.Filled.Hail, WeatherCodeMapper.getPrecipitationIconVector(66))
        assertEquals(Icons.Filled.Hail, WeatherCodeMapper.getPrecipitationIconVector(67))
    }

    @Test
    fun `getPrecipitationIconVector returns grain for snow codes`() {
        assertEquals(Icons.Filled.Grain, WeatherCodeMapper.getPrecipitationIconVector(71))
        assertEquals(Icons.Filled.Grain, WeatherCodeMapper.getPrecipitationIconVector(73))
        assertEquals(Icons.Filled.Grain, WeatherCodeMapper.getPrecipitationIconVector(75))
        assertEquals(Icons.Filled.Grain, WeatherCodeMapper.getPrecipitationIconVector(85))
    }

    @Test
    fun `getPrecipitationIconVector returns grain for snow grains`() {
        assertEquals(Icons.Filled.Grain, WeatherCodeMapper.getPrecipitationIconVector(77))
        assertEquals(Icons.Filled.Grain, WeatherCodeMapper.getPrecipitationIconVector(86))
    }

    @Test
    fun `getPrecipitationIconVector returns water drop for thunderstorm with rain`() {
        assertEquals(Icons.Filled.WaterDrop, WeatherCodeMapper.getPrecipitationIconVector(95))
    }

    @Test
    fun `getPrecipitationIconVector returns hail for thunderstorm with hail`() {
        assertEquals(Icons.Filled.Hail, WeatherCodeMapper.getPrecipitationIconVector(96))
        assertEquals(Icons.Filled.Hail, WeatherCodeMapper.getPrecipitationIconVector(99))
    }

    @Test
    fun `getPrecipitationIconVector returns null for non-precipitation codes`() {
        assertNull(WeatherCodeMapper.getPrecipitationIconVector(0))
        assertNull(WeatherCodeMapper.getPrecipitationIconVector(1))
        assertNull(WeatherCodeMapper.getPrecipitationIconVector(2))
        assertNull(WeatherCodeMapper.getPrecipitationIconVector(3))
    }

    @Test
    fun `getPrecipitationIconVector returns null for unknown codes`() {
        assertNull(WeatherCodeMapper.getPrecipitationIconVector(999))
        assertNull(WeatherCodeMapper.getPrecipitationIconVector(-1))
    }

    @Test
    fun `getCardColor returns correct colors for clear sky during day light theme`() {
        val color = WeatherCodeMapper.getCardColor(0, 1, false)
        assertEquals(Color(0xFFE3F2FD), color)
    }

    @Test
    fun `getCardColor returns correct colors for clear sky during day dark theme`() {
        val color = WeatherCodeMapper.getCardColor(0, 1, true)
        assertEquals(Color(0xFF1A2744), color)
    }

    @Test
    fun `getCardColor returns correct colors for clear sky during night light theme`() {
        val color = WeatherCodeMapper.getCardColor(0, 0, false)
        assertEquals(Color(0xFFE8EAF6), color)
    }

    @Test
    fun `getCardColor returns correct colors for clear sky during night dark theme`() {
        val color = WeatherCodeMapper.getCardColor(0, 0, true)
        assertEquals(Color(0xFF1A1A2E), color)
    }

    @Test
    fun `getCardColor returns correct colors for cloudy weather light theme`() {
        val color = WeatherCodeMapper.getCardColor(1, 1, false)
        assertEquals(Color(0xFFECEFF1), color)
    }

    @Test
    fun `getCardColor returns correct colors for cloudy weather dark theme`() {
        val color = WeatherCodeMapper.getCardColor(1, 1, true)
        assertEquals(Color(0xFF1E2228), color)
    }

    @Test
    fun `getCardColor returns correct colors for rain light theme`() {
        val color = WeatherCodeMapper.getCardColor(51, 1, false)
        assertEquals(Color(0xFFE1F5FE), color)
    }

    @Test
    fun `getCardColor returns correct colors for rain dark theme`() {
        val color = WeatherCodeMapper.getCardColor(51, 1, true)
        assertEquals(Color(0xFF16273A), color)
    }

    @Test
    fun `getCardColor returns correct colors for snow light theme`() {
        val color = WeatherCodeMapper.getCardColor(71, 1, false)
        assertEquals(Color(0xFFF3F4F8), color)
    }

    @Test
    fun `getCardColor returns correct colors for snow dark theme`() {
        val color = WeatherCodeMapper.getCardColor(71, 1, true)
        assertEquals(Color(0xFF1E2430), color)
    }

    @Test
    fun `getCardColor returns correct colors for thunderstorm rain light theme`() {
        val color = WeatherCodeMapper.getCardColor(95, 1, false)
        assertEquals(Color(0xFFE1F5FE), color)
    }

    @Test
    fun `getCardColor returns correct colors for thunderstorm rain dark theme`() {
        val color = WeatherCodeMapper.getCardColor(95, 1, true)
        assertEquals(Color(0xFF16273A), color)
    }
}