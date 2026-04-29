package ru.vladigeras.weatherapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.vladigeras.weatherapp.data.WeatherDisplayPrefs
import ru.vladigeras.weatherapp.repository.WeatherDisplayPrefsRepository
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {
    @Inject
    lateinit var prefsRepository: WeatherDisplayPrefsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            MaterialTheme {
                SettingsScreen(
                    viewModel = hiltViewModel<SettingsViewModel>(),
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit = {}
) {
    val prefs by viewModel.localPrefs.collectAsState(WeatherDisplayPrefs())
    val hasChanges by viewModel.hasChanges.collectAsState(false)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.savePrefs()
                            onBack()
                        },
                        enabled = hasChanges
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(settingsItems(prefs)) { item ->
                    when (item) {
                        is SettingsItem.Toggle -> {
                            SettingsToggleItem(
                                title = item.title,
                                description = item.description,
                                checked = item.checked,
                                icon = item.icon,
                                enabled = item.enabled,
                                onCheckedChange = { viewModel.toggleItem(item.key, it) }
                            )
                        }
                        is SettingsItem.ForecastDays -> {
                            SettingsForecastDaysItem(
                                title = item.title,
                                days = item.days,
                                onDaysChanged = { viewModel.setForecastDays(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsToggleItem(
    title: String,
    description: String?,
    checked: Boolean,
    icon: @Composable () -> Unit,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            description?.let { desc ->
                Text(
                    desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = { onCheckedChange(it) }
        )
    }
}

@Composable
fun SettingsForecastDaysItem(
    title: String,
    days: Int,
    onDaysChanged: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val availableDays = listOf(1, 2, 3, 5, 7, 10, 14, 16)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Settings, contentDescription = null)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                "Forecast days: $days",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = days.toString(),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(end = 8.dp)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            availableDays.forEach { day ->
                DropdownMenuItem(
                    text = { Text("$day day${if (day > 1) "s" else ""}") },
                    onClick = {
                        onDaysChanged(day)
                        expanded = false
                    }
                )
            }
        }
    }
}

sealed interface SettingsItem {
    data class Toggle(
        val key: String,
        val title: String,
        val description: String?,
        val checked: Boolean,
        val icon: @Composable () -> Unit,
        val enabled: Boolean = true
    ) : SettingsItem
    data class ForecastDays(
        val title: String,
        val days: Int
    ) : SettingsItem
}

private fun settingsItems(prefs: WeatherDisplayPrefs): List<SettingsItem> {
    return listOf(
        // Condition - first, required, cannot be toggled off
        SettingsItem.Toggle(
            key = "condition",
            title = "Condition",
            description = "Show weather condition",
            checked = true,
            icon = { Icon(Icons.Filled.Thermostat, contentDescription = null) },
            enabled = false
        ),
        SettingsItem.Toggle(
            key = "humidity",
            title = "Humidity",
            description = "Show relative humidity",
            checked = prefs.showHumidity,
            icon = { Icon(Icons.Filled.WaterDrop, contentDescription = null) }
        ),
        SettingsItem.Toggle(
            key = "wind",
            title = "Wind",
            description = "Show wind speed and direction",
            checked = prefs.showWind,
            icon = { Icon(Icons.Filled.Air, contentDescription = null) }
        ),
        SettingsItem.Toggle(
            key = "precipitation",
            title = "Precipitation",
            description = "Show precipitation sum",
            checked = prefs.showPrecipitation,
            icon = { Icon(Icons.Filled.Cloud, contentDescription = null) }
        ),
        SettingsItem.Toggle(
            key = "sun_times",
            title = "Sunrise & Sunset",
            description = "Show sunrise and sunset times",
            checked = prefs.showSunTimes,
            icon = { Icon(Icons.Filled.Settings, contentDescription = null) }
        ),
        SettingsItem.Toggle(
            key = "uv_index",
            title = "UV Index",
            description = "Show maximum UV index",
            checked = prefs.showUvIndex,
            icon = { Icon(Icons.Filled.Settings, contentDescription = null) }
        ),
        SettingsItem.Toggle(
            key = "forecast",
            title = "Forecast",
            description = "Enable forecast block",
            checked = prefs.showForecast,
            icon = { Icon(Icons.Filled.Cloud, contentDescription = null) }
        ),
        SettingsItem.ForecastDays(
            title = "Forecast Days",
            days = prefs.forecastDays
        )
    )
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefsRepository: WeatherDisplayPrefsRepository
) : ViewModel() {

    private val _originalPrefs = MutableStateFlow<WeatherDisplayPrefs>(WeatherDisplayPrefs())
    val localPrefs = MutableStateFlow<WeatherDisplayPrefs>(WeatherDisplayPrefs())

    val hasChanges = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            val prefs = prefsRepository.getPrefs().first()
            _originalPrefs.value = prefs
            localPrefs.value = prefs
            updateHasChanges()
        }
    }

    private fun updateHasChanges() {
        hasChanges.value = _originalPrefs.value != localPrefs.value
    }

    fun toggleItem(key: String, checked: Boolean) {
        val current = localPrefs.value
        localPrefs.value = when (key) {
            "humidity" -> current.copy(showHumidity = checked)
            "wind" -> current.copy(showWind = checked)
            "precipitation" -> current.copy(showPrecipitation = checked)
            "condition" -> current.copy(showCondition = checked)
            "sun_times" -> current.copy(showSunTimes = checked)
            "uv_index" -> current.copy(showUvIndex = checked)
            "forecast" -> current.copy(showForecast = checked)
            else -> current
        }
        updateHasChanges()
    }

    fun setForecastDays(days: Int) {
        localPrefs.value = localPrefs.value.copy(forecastDays = days)
        updateHasChanges()
    }

    fun savePrefs() {
        viewModelScope.launch {
            val prefsToSave = localPrefs.value
            prefsRepository.updatePrefs(prefsToSave)
            _originalPrefs.value = prefsToSave
            updateHasChanges()
        }
    }

    fun resetPrefs() {
        localPrefs.value = _originalPrefs.value
        updateHasChanges()
    }
}
