package ru.vladigeras.weatherapp.ui

import androidx.compose.ui.graphics.vector.ImageVector
import ru.vladigeras.weatherapp.R
import ru.vladigeras.weatherapp.util.WeatherCodeMapper

fun getWeatherIconForCode(weatherCode: Int, isDay: Int = 1): ImageVector {
    return WeatherCodeMapper.getIconVector(weatherCode, isDay)
}

fun getPrecipitationIconForCode(weatherCode: Int): ImageVector? {
    return WeatherCodeMapper.getPrecipitationIconVector(weatherCode)
}

fun getWeatherCodeStringResId(weatherCode: Int): Int {
    return when (weatherCode) {
        0 -> R.string.weather_code_0
        1 -> R.string.weather_code_1
        2 -> R.string.weather_code_2
        3 -> R.string.weather_code_3
        45 -> R.string.weather_code_45
        48 -> R.string.weather_code_48
        51 -> R.string.weather_code_51
        53 -> R.string.weather_code_53
        55 -> R.string.weather_code_55
        56 -> R.string.weather_code_56
        57 -> R.string.weather_code_57
        61 -> R.string.weather_code_61
        63 -> R.string.weather_code_63
        65 -> R.string.weather_code_65
        66 -> R.string.weather_code_66
        67 -> R.string.weather_code_67
        71 -> R.string.weather_code_71
        73 -> R.string.weather_code_73
        75 -> R.string.weather_code_75
        77 -> R.string.weather_code_77
        80 -> R.string.weather_code_80
        81 -> R.string.weather_code_81
        82 -> R.string.weather_code_82
        85 -> R.string.weather_code_85
        86 -> R.string.weather_code_86
        95 -> R.string.weather_code_95
        96 -> R.string.weather_code_96
        99 -> R.string.weather_code_99
        else -> R.string.unknown_weather
    }
}
