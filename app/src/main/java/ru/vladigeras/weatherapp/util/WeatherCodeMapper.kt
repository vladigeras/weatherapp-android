package ru.vladigeras.weatherapp.util

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
import ru.vladigeras.weatherapp.R

enum class WeatherType {
    CLEAR, CLOUDY, RAIN, SNOW, THUNDERSTORM
}

object WeatherCodeMapper {

    fun getWeatherType(code: Int, isDay: Int = 1): WeatherType {
        return when (code) {
            0 -> WeatherType.CLEAR
            1, 2, 3, 45, 48 -> WeatherType.CLOUDY
            51, 53, 55, 61, 63, 65, 80, 81, 82, 95, 96, 99 -> WeatherType.RAIN
            56, 57, 66, 67, 71, 73, 75, 77, 85, 86 -> WeatherType.SNOW
            else -> WeatherType.CLOUDY
        }
    }

    fun getIconRes(code: Int, isDay: Int = 1): Int {
        return when (getWeatherType(code, isDay)) {
            WeatherType.CLEAR -> R.drawable.ic_weather_clear
            WeatherType.CLOUDY -> R.drawable.ic_weather_cloud
            WeatherType.RAIN -> R.drawable.ic_weather_rain
            WeatherType.SNOW -> R.drawable.ic_weather_snow
            WeatherType.THUNDERSTORM -> R.drawable.ic_weather_rain
        }
    }

    fun getIconVector(code: Int, isDay: Int = 1): ImageVector {
        val type = getWeatherType(code, isDay)
        val isDaytime = isDay == 1
        return when (type) {
            WeatherType.CLEAR -> if (isDaytime) Icons.Filled.WbSunny else Icons.Filled.NightsStay
            WeatherType.CLOUDY -> if (isDaytime && code == 1) Icons.Filled.WbCloudy else Icons.Filled.Cloud
            WeatherType.RAIN -> {
                when (code) {
                    56, 57, 66, 67 -> Icons.Filled.Hail
                    95, 96, 99 -> Icons.Filled.Thunderstorm
                    else -> Icons.Filled.WaterDrop
                }
            }
            WeatherType.SNOW -> {
                when (code) {
                    77 -> Icons.Filled.Grain
                    else -> Icons.Filled.AcUnit
                }
            }
            WeatherType.THUNDERSTORM -> Icons.Filled.Thunderstorm
        }
    }

    fun getPrecipitationIconVector(code: Int): ImageVector? {
        return when (code) {
            51, 53, 55, 61, 63, 65, 80, 81, 82 -> Icons.Filled.WaterDrop
            56, 57, 66, 67 -> Icons.Filled.Hail
            71, 73, 75, 77, 85, 86 -> Icons.Filled.Grain
            95 -> Icons.Filled.WaterDrop
            96, 99 -> Icons.Filled.Hail
            else -> null
        }
    }
}