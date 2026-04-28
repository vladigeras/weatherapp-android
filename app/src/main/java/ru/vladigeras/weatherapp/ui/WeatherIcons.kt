package ru.vladigeras.weatherapp.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Hail
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector

fun getWeatherIconForCode(weatherCode: Int, isDay: Int = 1): ImageVector {
    val isDaytime = isDay == 1
    return when (weatherCode) {
        // Clear sky
        0 -> if (isDaytime) Icons.Filled.WbSunny else Icons.Filled.NightsStay
        // Partly cloudy (WbCloudy = sun behind cloud)
        1 -> if (isDaytime) Icons.Filled.WbCloudy else Icons.Filled.NightsStay
        // Cloudy / Overcast
        2, 3 -> Icons.Filled.Cloud
        // Fog (no Foggy icon, use Cloud as closest alternative)
        45, 48 -> Icons.Filled.Cloud
        // Drizzle (light rain)
        51, 53, 55 -> Icons.Filled.WaterDrop
        // Freezing drizzle
        56, 57 -> Icons.Filled.AcUnit
        // Rain
        61, 63, 65 -> Icons.Filled.WaterDrop
        // Freezing rain (hail icon available!)
        66, 67 -> Icons.Filled.Hail
        // Snow
        71, 73, 75 -> Icons.Filled.AcUnit
        // Snow grains (Grain icon available!)
        77 -> Icons.Filled.Grain
        // Rain showers
        80, 81, 82 -> Icons.Filled.WaterDrop
        // Snow showers
        85, 86 -> Icons.Filled.Grain
        // Thunderstorm
        95 -> Icons.Filled.Thunderstorm
        // Thunderstorm with hail
        96, 99 -> Icons.Filled.Hail
        // Unknown
        else -> Icons.Filled.Warning
    }
}

fun getPrecipitationIconForCode(weatherCode: Int): ImageVector? {
    return when (weatherCode) {
        // Drizzle and rain (liquid precipitation)
        51, 53, 55, 61, 63, 65, 80, 81, 82 -> Icons.Filled.WaterDrop
        // Freezing drizzle and freezing rain (ice)
        56, 57, 66, 67 -> Icons.Filled.Hail
        // Snow (solid precipitation)
        71, 73, 75, 85, 86 -> Icons.Filled.Grain
        // Snow grains
        77 -> Icons.Filled.Grain
        // Thunderstorm (usually rain, with hail if code 96/99)
        95 -> Icons.Filled.WaterDrop
        96, 99 -> Icons.Filled.Hail
        // Unknown precipitation code
        else -> null
    }
}
