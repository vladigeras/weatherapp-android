package ru.vladigeras.weatherapp.util

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import ru.vladigeras.weatherapp.R

enum class WeatherType {
    CLEAR, CLOUDY, RAIN, SNOW, THUNDERSTORM
}

data class WeatherCodeConfig(
    val icon: ImageVector,
    val stringRes: Int,
    val precipitationIcon: ImageVector?
)

object WeatherCodeMapper {

    private val CODE_MAP = mapOf(
        0 to WeatherCodeConfig(
            icon = Icons.Filled.WbSunny,
            stringRes = R.string.weather_code_0,
            precipitationIcon = null
        ),
        1 to WeatherCodeConfig(
            icon = Icons.Filled.WbCloudy,
            stringRes = R.string.weather_code_1,
            precipitationIcon = null
        ),
        2 to WeatherCodeConfig(
            icon = Icons.Filled.WbCloudy,
            stringRes = R.string.weather_code_2,
            precipitationIcon = null
        ),
        3 to WeatherCodeConfig(
            icon = Icons.Filled.Cloud,
            stringRes = R.string.weather_code_3,
            precipitationIcon = null
        ),
        45 to WeatherCodeConfig(
            icon = Icons.Filled.Cloud,
            stringRes = R.string.weather_code_45,
            precipitationIcon = null
        ),
        48 to WeatherCodeConfig(
            icon = Icons.Filled.Cloud,
            stringRes = R.string.weather_code_48,
            precipitationIcon = null
        ),
        51 to WeatherCodeConfig(
            icon = Icons.Filled.WaterDrop,
            stringRes = R.string.weather_code_51,
            precipitationIcon = Icons.Filled.WaterDrop
        ),
        53 to WeatherCodeConfig(
            icon = Icons.Filled.WaterDrop,
            stringRes = R.string.weather_code_53,
            precipitationIcon = Icons.Filled.WaterDrop
        ),
        55 to WeatherCodeConfig(
            icon = Icons.Filled.WaterDrop,
            stringRes = R.string.weather_code_55,
            precipitationIcon = Icons.Filled.WaterDrop
        ),
        56 to WeatherCodeConfig(
            icon = Icons.Filled.Hail,
            stringRes = R.string.weather_code_56,
            precipitationIcon = Icons.Filled.Hail
        ),
        57 to WeatherCodeConfig(
            icon = Icons.Filled.Hail,
            stringRes = R.string.weather_code_57,
            precipitationIcon = Icons.Filled.Hail
        ),
        61 to WeatherCodeConfig(
            icon = Icons.Filled.WaterDrop,
            stringRes = R.string.weather_code_61,
            precipitationIcon = Icons.Filled.WaterDrop
        ),
        63 to WeatherCodeConfig(
            icon = Icons.Filled.WaterDrop,
            stringRes = R.string.weather_code_63,
            precipitationIcon = Icons.Filled.WaterDrop
        ),
        65 to WeatherCodeConfig(
            icon = Icons.Filled.WaterDrop,
            stringRes = R.string.weather_code_65,
            precipitationIcon = Icons.Filled.WaterDrop
        ),
        66 to WeatherCodeConfig(
            icon = Icons.Filled.Hail,
            stringRes = R.string.weather_code_66,
            precipitationIcon = Icons.Filled.Hail
        ),
        67 to WeatherCodeConfig(
            icon = Icons.Filled.Hail,
            stringRes = R.string.weather_code_67,
            precipitationIcon = Icons.Filled.Hail
        ),
        71 to WeatherCodeConfig(
            icon = Icons.Filled.AcUnit,
            stringRes = R.string.weather_code_71,
            precipitationIcon = Icons.Filled.Grain
        ),
        73 to WeatherCodeConfig(
            icon = Icons.Filled.AcUnit,
            stringRes = R.string.weather_code_73,
            precipitationIcon = Icons.Filled.Grain
        ),
        75 to WeatherCodeConfig(
            icon = Icons.Filled.AcUnit,
            stringRes = R.string.weather_code_75,
            precipitationIcon = Icons.Filled.Grain
        ),
        77 to WeatherCodeConfig(
            icon = Icons.Filled.Grain,
            stringRes = R.string.weather_code_77,
            precipitationIcon = Icons.Filled.Grain
        ),
        80 to WeatherCodeConfig(
            icon = Icons.Filled.WaterDrop,
            stringRes = R.string.weather_code_80,
            precipitationIcon = Icons.Filled.WaterDrop
        ),
        81 to WeatherCodeConfig(
            icon = Icons.Filled.WaterDrop,
            stringRes = R.string.weather_code_81,
            precipitationIcon = Icons.Filled.WaterDrop
        ),
        82 to WeatherCodeConfig(
            icon = Icons.Filled.WaterDrop,
            stringRes = R.string.weather_code_82,
            precipitationIcon = Icons.Filled.WaterDrop
        ),
        85 to WeatherCodeConfig(
            icon = Icons.Filled.AcUnit,
            stringRes = R.string.weather_code_85,
            precipitationIcon = Icons.Filled.Grain
        ),
        86 to WeatherCodeConfig(
            icon = Icons.Filled.AcUnit,
            stringRes = R.string.weather_code_86,
            precipitationIcon = Icons.Filled.Grain
        ),
        95 to WeatherCodeConfig(
            icon = Icons.Filled.Thunderstorm,
            stringRes = R.string.weather_code_95,
            precipitationIcon = Icons.Filled.WaterDrop
        ),
        96 to WeatherCodeConfig(
            icon = Icons.Filled.Thunderstorm,
            stringRes = R.string.weather_code_96,
            precipitationIcon = Icons.Filled.Hail
        ),
        99 to WeatherCodeConfig(
            icon = Icons.Filled.Thunderstorm,
            stringRes = R.string.weather_code_99,
            precipitationIcon = Icons.Filled.Hail
        )
    )

    private val DEFAULT_CONFIG = WeatherCodeConfig(
        icon = Icons.Filled.Cloud,
        stringRes = R.string.unknown_weather,
        precipitationIcon = null
    )

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
        val config = CODE_MAP[code] ?: DEFAULT_CONFIG
        val isDaytime = isDay == 1

        return when (code) {
            0 -> if (isDaytime) Icons.Filled.WbSunny else Icons.Filled.NightsStay
            1 -> if (isDaytime) Icons.Filled.WbCloudy else Icons.Filled.Cloud
            else -> config.icon
        }
    }

    fun getPrecipitationIconVector(code: Int): ImageVector? {
        return CODE_MAP[code]?.precipitationIcon
    }

    fun getWeatherCodeStringResId(code: Int): Int {
        return CODE_MAP[code]?.stringRes ?: R.string.unknown_weather
    }

    fun getCardColor(code: Int, isDay: Int = 1, isDarkTheme: Boolean = false): Color {
        val weatherType = getWeatherType(code, isDay)
        val isDaytime = isDay == 1
        
        return when (weatherType) {
            WeatherType.CLEAR -> {
                if (isDaytime) {
                    if (isDarkTheme) Color(0xFF1A2744) else Color(0xFFE3F2FD)
                } else {
                    if (isDarkTheme) Color(0xFF1A1A2E) else Color(0xFFE8EAF6)
                }
            }
            WeatherType.CLOUDY -> {
                if (isDarkTheme) Color(0xFF1E2228) else Color(0xFFECEFF1)
            }
            WeatherType.RAIN -> {
                if (isDarkTheme) Color(0xFF16273A) else Color(0xFFE1F5FE)
            }
            WeatherType.SNOW -> {
                if (isDarkTheme) Color(0xFF1E2430) else Color(0xFFF3F4F8)
            }
            WeatherType.THUNDERSTORM -> {
                if (isDarkTheme) Color(0xFF1A1625) else Color(0xFFEDE7F6)
            }
        }
    }
}