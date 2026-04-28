package ru.vladigeras.weatherapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.dp

/**
 * Displays a list of daily weather forecasts using a Column (not LazyColumn)
 * to avoid nesting scrollable containers.
 *
 * @param dailyForecast List of [DailyForecast] objects to display.
 */
@Composable
fun DailyForecastList(dailyForecast: List<DailyForecast>, temperatureUnit: String) {
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
            .height(72.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
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
                val weatherIcon = getWeatherIconForCode(forecast.weatherCode, isDay = 1)
                Icon(
                    imageVector = weatherIcon,
                    contentDescription = "Weather condition",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Temperature range
            Column(
                modifier = Modifier.weight(1f, fill = false),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${forecast.temperatureMin.toInt()}$temperatureUnit/${forecast.temperatureMax.toInt()}$temperatureUnit",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Precipitation info
            Column(
                modifier = Modifier.weight(1f, fill = false),
                horizontalAlignment = Alignment.End
            ) {
                if (forecast.precipitationSum > 0) {
                    val precipitationIcon = getPrecipitationIconForCode(forecast.weatherCode)
                    if (precipitationIcon != null) {
                        Icon(
                            imageVector = precipitationIcon,
                            contentDescription = "Precipitation",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "${forecast.precipitationSum.toInt()} mm",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}