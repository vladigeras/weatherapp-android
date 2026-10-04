package ru.vladigeras.weatherapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
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
        dailyForecast.forEach { forecast ->
            DailyForecastItem(
                forecast = forecast,
                temperatureUnit = temperatureUnit
            )
        }
    }
}

/**
 * Represents a single day's weather forecast item.
 *
 * @param forecast The [DailyForecast] to display.
 */
@Preview(showBackground = true)
@Composable
fun DailyForecastItem(forecast: DailyForecast, temperatureUnit: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val labelResId = when (forecast.relativeDay) {
                        0L -> R.string.today
                        1L -> R.string.tomorrow
                        2L -> R.string.day_after_tomorrow
                        else -> null
                    }
                    if (labelResId != null) {
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = forecast.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val weatherCode = forecast.weatherCode
                    if (weatherCode != null) {
                    val weatherIcon = WeatherCodeMapper.getIconVector(weatherCode, isDay = 1)
                    Icon(
                        imageVector = weatherIcon,
                        contentDescription = "Weather",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp).padding(horizontal = 8.dp)
                    )
                    }
                    Text(
                        text = "${forecast.temperatureMin?.toInt()?.toString() ?: "—"}$temperatureUnit/${forecast.temperatureMax?.toInt()?.toString() ?: "—"}$temperatureUnit",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically
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
                    Text(
                        text = "${stringResource(R.string.uv_label)} ${uvIndex.toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = uvIndexColor(uvIndex, isSystemInDarkTheme()),
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

internal fun uvIndexColor(uvIndex: Double, darkTheme: Boolean): Color = when (uvIndex) {
    in 0.0..2.0 -> if (darkTheme) Color(0xFF81C784) else Color(0xFF2E7D32)
    in 2.1..5.0 -> if (darkTheme) Color(0xFFFFF176) else Color(0xFF8D6E00)
    in 5.1..7.0 -> if (darkTheme) Color(0xFFFFB74D) else Color(0xFFA65300)
    else -> if (darkTheme) Color(0xFFEF9A9A) else Color(0xFFC62828)
}
