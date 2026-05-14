package ru.vladigeras.weatherapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.vladigeras.weatherapp.R
import ru.vladigeras.weatherapp.data.WeatherDisplayPrefs
import ru.vladigeras.weatherapp.util.WeatherCodeMapper

@Composable
fun HourlyForecastList(
    forecast: List<HourlyForecast>,
    temperatureUnit: String,
    prefs: WeatherDisplayPrefs,
    modifier: Modifier = Modifier
) {
    if (forecast.isEmpty()) {
        return
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items = forecast, key = { it.time }) { item ->
            HourlyForecastCard(forecast = item, temperatureUnit = temperatureUnit, prefs = prefs)
        }
    }
}

@Composable
fun HourlyForecastCard(
    forecast: HourlyForecast,
    temperatureUnit: String,
    prefs: WeatherDisplayPrefs,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(80.dp)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = forecast.time,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            val weatherCode = forecast.weatherCode ?: 0
            val weatherIcon = WeatherCodeMapper.getIconVector(weatherCode, isDay = 1)
            val weatherDesc = stringResource(WeatherCodeMapper.getWeatherCodeStringResId(weatherCode))
            Icon(
                imageVector = weatherIcon,
                contentDescription = weatherDesc,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            val temperature = forecast.temperature
            if (temperature != null) {
                Text(
                    text = "${temperature.toInt()}$temperatureUnit",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (prefs.showHumidity) {
                forecast.humidity?.let { humidity ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WaterDrop,
                            contentDescription = stringResource(R.string.humidity),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = "$humidity%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (prefs.showWind) {
                forecast.windSpeed?.let { windSpeed ->
                    if (prefs.showHumidity && forecast.humidity != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Air,
                            contentDescription = stringResource(R.string.wind),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(10.dp)
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