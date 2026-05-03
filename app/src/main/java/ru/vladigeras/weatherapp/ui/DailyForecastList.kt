package ru.vladigeras.weatherapp.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.vladigeras.weatherapp.R
import ru.vladigeras.weatherapp.util.WeatherCodeMapper

/**
 * Displays a list of daily weather forecasts using a Column (not LazyColumn)
 * to avoid nesting scrollable containers.
 *
 * @param dailyForecast List of [DailyForecast] objects to display.
 */
@Composable
fun DailyForecastList(dailyForecast: List<DailyForecast>, temperatureUnit: String) {
    val context = LocalContext.current
    
    if (dailyForecast.isEmpty()) {
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        dailyForecast.forEach { forecast ->
            DailyForecastItem(forecast = forecast, temperatureUnit = temperatureUnit)
        }
    }
}

/**
 * Represents a single day's weather forecast item.
 *
 * @param forecast The [DailyForecast] to display.
 */
@Composable
private fun DailyForecastItem(forecast: DailyForecast, temperatureUnit: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(110.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            // First row: Day info, icon, temperature
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Day name and date
                Column(
                    modifier = Modifier.weight(1f, fill = false),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = forecast.dayName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = forecast.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Weather icon
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val weatherCode = forecast.weatherCode ?: 0
                    val weatherIcon = WeatherCodeMapper.getIconVector(weatherCode, isDay = 1)
                    val weatherDesc = getWeatherDescription(weatherCode)
                    Icon(
                        imageVector = weatherIcon,
                        contentDescription = weatherDesc,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Temperature range
                Text(
                    text = "${forecast.temperatureMin.toInt()}$temperatureUnit/${forecast.temperatureMax.toInt()}$temperatureUnit",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Second row: Precipitation, UV, Sunrise, Sunset
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Precipitation info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val precipitationSum = forecast.precipitationSum ?: 0.0
                    val context = LocalContext.current
                        if (precipitationSum > 0) {
                            val weatherCode = forecast.weatherCode ?: 0
                            val precipitationIcon = WeatherCodeMapper.getPrecipitationIconVector(weatherCode)
                            if (precipitationIcon != null) {
                                Icon(
                                    imageVector = precipitationIcon,
                                    contentDescription = "Precipitation",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "${precipitationSum.toInt()} ${stringResource(R.string.precipitation_unit)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // UV Index
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val uvIndex = forecast.uvIndexMax
                    if (uvIndex != null) {
                        val uvColor = when (uvIndex) {
                            in 0.0..2.0 -> Color(0xFF4CAF50) // Green - Low
                            in 2.1..5.0 -> Color(0xFFFFEB3B) // Yellow - Moderate
                            in 5.1..7.0 -> Color(0xFFFF9800) // Orange - High
                            else -> Color(0xFFF44336) // Red - Very High
                        }
                        Text(
                            text = stringResource(R.string.uv_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${uvIndex.toInt()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = uvColor,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                }

                // Sunrise
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    forecast.sunrise?.let { sunriseTime ->
                        Text(
                            text = "↑$sunriseTime",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Sunset
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    forecast.sunset?.let { sunsetTime ->
                        Text(
                            text = "↓$sunsetTime",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Composable function to get weather description from weather code.
 * Uses stringArrayResource to avoid using Context directly.
 */
@Composable
fun getWeatherDescription(weatherCode: Int): String {
    val resId = when (weatherCode) {
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
        else -> 0
    }
    return if (resId != 0) stringResource(resId) else stringResource(R.string.unknown_weather)
}