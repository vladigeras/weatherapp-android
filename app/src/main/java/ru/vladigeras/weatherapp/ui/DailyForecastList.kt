package ru.vladigeras.weatherapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.vladigeras.weatherapp.R
import ru.vladigeras.weatherapp.util.WeatherCodeMapper

/**
 * Displays a list of daily weather forecasts using a simple Column.
 * This component is safe to use inside any scrollable container.
 *
 * @param dailyForecast List of [DailyForecast] objects to display.
 */
@Composable
fun DailyForecastList(dailyForecast: List<DailyForecast>, temperatureUnit: String) {
    if (dailyForecast.isEmpty()) {
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        dailyForecast.forEachIndexed { index, forecast ->
            DailyForecastItem(
                forecast = forecast,
                temperatureUnit = temperatureUnit,
                index = index
            )
        }
    }
}

/**
 * Represents a single day's weather forecast item.
 *
 * @param forecast The [DailyForecast] to display.
 * @param index The position of this item in the list (0 = today, 1 = tomorrow)
 */
@Preview(showBackground = true)
@Composable
fun DailyForecastItem(forecast: DailyForecast, temperatureUnit: String, index: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val isToday = index == 0
                        val isTomorrow = index == 1
                        val isDayAfterTomorrow = index == 2
                        if (isToday || isTomorrow || isDayAfterTomorrow) {
                            val labelResId = when {
                                isToday -> R.string.today
                                isTomorrow -> R.string.tomorrow
                                else -> R.string.day_after_tomorrow
                            }
                            Text(
                                text = stringResource(labelResId),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                        Text(
                            text = forecast.dayName,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = forecast.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val weatherCode = forecast.weatherCode ?: 0
                val weatherIcon = WeatherCodeMapper.getIconVector(weatherCode, isDay = 1)
                val weatherDesc = stringResource(WeatherCodeMapper.getWeatherCodeStringResId(weatherCode))
                Icon(
                    imageVector = weatherIcon,
                    contentDescription = weatherDesc,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp).padding(horizontal = 8.dp)
                )

                Text(
                    text = "${forecast.temperatureMin.toInt()}$temperatureUnit/${forecast.temperatureMax.toInt()}$temperatureUnit",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val precipitationSum = forecast.precipitationSum ?: 0.0
                if (precipitationSum > 0) {
                    val precipitationIcon = forecast.weatherCode?.let { WeatherCodeMapper.getPrecipitationIconVector(it) }
                    if (precipitationIcon != null) {
                        Icon(
                            imageVector = precipitationIcon,
                            contentDescription = "Precipitation",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Text(
                        text = "${precipitationSum.toInt()} ${stringResource(R.string.precipitation_unit)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val uvIndex = forecast.uvIndexMax
                if (uvIndex != null) {
                    val uvColor = when (uvIndex) {
                        in 0.0..2.0 -> Color(0xFF4CAF50)
                        in 2.1..5.0 -> Color(0xFFFFEB3B)
                        in 5.1..7.0 -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    }
                    Text(
                        text = "${stringResource(R.string.uv_label)} ${uvIndex.toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = uvColor,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }

                forecast.sunrise?.let { sunriseTime ->
                    Text(
                        text = "↑$sunriseTime",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                forecast.sunset?.let { sunsetTime ->
                    Text(
                        text = "↓$sunsetTime",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                forecast.windSpeedMax?.let { windSpeed ->
                    if (windSpeed > 0) {
                        Icon(
                            imageVector = Icons.Filled.Air,
                            contentDescription = stringResource(R.string.wind),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${windSpeed.toInt()} ${stringResource(R.string.wind_speed_unit)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

