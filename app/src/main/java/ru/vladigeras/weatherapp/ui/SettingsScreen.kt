package ru.vladigeras.weatherapp.ui

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import ru.vladigeras.weatherapp.R
import ru.vladigeras.weatherapp.data.WeatherDisplayPrefs
import ru.vladigeras.weatherapp.repository.LanguagePreference
import ru.vladigeras.weatherapp.repository.LanguagePreferenceRepository
import ru.vladigeras.weatherapp.repository.WeatherDisplayPrefsRepository
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {
    @Inject
    lateinit var prefsRepository: WeatherDisplayPrefsRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // IMPORTANT: Apply locale BEFORE setContent!
        super.onCreate(savedInstanceState)  // ← THIS WAS MISSING!
        val locale = getSavedLocale(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val config = Configuration(resources.configuration)
            config.setLocale(locale)
            val newContext = createConfigurationContext(config)
            // Apply the new context's configuration to our resources
            resources.updateConfiguration(newContext.resources.configuration, newContext.resources.displayMetrics)
        } else {
            @Suppress("DEPRECATION")
            val legacyConfig = resources.configuration
            legacyConfig.setLocale(locale)
            @Suppress("DEPRECATION")
            resources.updateConfiguration(legacyConfig, resources.displayMetrics)
        }
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            MaterialTheme {
                SettingsScreen(
                    viewModel = hiltViewModel<SettingsViewModel>(),
                    onBack = { finish() },
                    onRecreate = { recreate() }
                )
            }
        }
    }
    
    private fun getSavedLocale(context: Context): java.util.Locale {
        val prefs = context.getSharedPreferences("weatherapp_prefs", Context.MODE_PRIVATE)
        val ordinal = prefs.getInt("language_preference", -1)
        
        return when (ordinal) {
            1 -> java.util.Locale("ru", "RU")
            2 -> java.util.Locale.ENGLISH
            else -> {
                val deviceLang = java.util.Locale.getDefault()
                if (deviceLang.language == "ru") {
                    java.util.Locale("ru", "RU")
                } else {
                    java.util.Locale.ENGLISH
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit = {},
    onRecreate: () -> Unit = {}
) {
    val prefs by viewModel.localPrefs.collectAsState(WeatherDisplayPrefs())
    val hasChanges by viewModel.hasChanges.collectAsState(false)
    val languagePreference by viewModel.languagePreference.collectAsState(LanguagePreference.SYSTEM)
    var showRestartDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                val langChanged = viewModel.savePrefsAndCheckLanguage()
                                if (langChanged) {
                                    showRestartDialog = true
                                } else {
                                    onBack()
                                }
                            }
                        },
                        enabled = hasChanges
                    ) {
                        Text(stringResource(R.string.save))
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
                items(settingsItems(prefs, languagePreference)) { item ->
                    when (item) {
                        is SettingsItem.LanguageSelector -> {
                            SettingsLanguageItem(
                                currentPreference = item.currentPreference,
                                onPreferenceChanged = { viewModel.setLanguagePreference(it) }
                            )
                        }
                        is SettingsItem.Toggle -> {
                            SettingsToggleItem(
                                title = getStringResource(item.title),
                                description = item.description?.let { getStringResource(it) },
                                checked = item.checked,
                                icon = item.icon,
                                enabled = item.enabled,
                                onCheckedChange = { viewModel.toggleItem(item.key, it) }
                            )
                        }
                        is SettingsItem.ForecastDays -> {
                            SettingsForecastDaysItem(
                                title = getStringResource(item.title),
                                days = item.days,
                                onDaysChanged = { viewModel.setForecastDays(it) }
                            )
                        }
                    }
                }
            }
        }
        
         if (showRestartDialog) {
             AlertDialog(
                 onDismissRequest = { },
                 title = { Text(stringResource(R.string.language_change_title)) },
                 text = { Text(stringResource(R.string.language_change_message)) },
                 confirmButton = {
                     TextButton(onClick = {
                         showRestartDialog = false
                         val activity = context as? android.app.Activity
                         activity?.finishAndRemoveTask()
                         System.exit(0)
                     }) {
                         Text(stringResource(R.string.exit_app))
                     }
                 }
             )
         }
    }
}

@Composable
private fun SettingsLanguageItem(
    currentPreference: LanguagePreference,
    onPreferenceChanged: (LanguagePreference) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Language, contentDescription = null)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.language),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = when (currentPreference) {
                        LanguagePreference.SYSTEM -> stringResource(R.string.language_system)
                        LanguagePreference.RUSSIAN -> stringResource(R.string.language_russian)
                        LanguagePreference.ENGLISH -> stringResource(R.string.language_english)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.language_system)) },
                onClick = {
                    onPreferenceChanged(LanguagePreference.SYSTEM)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.language_russian)) },
                onClick = {
                    onPreferenceChanged(LanguagePreference.RUSSIAN)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.language_english)) },
                onClick = {
                    onPreferenceChanged(LanguagePreference.ENGLISH)
                    expanded = false
                }
            )
        }
    }
}

@Composable
private fun getStringResource(name: String): String {
    // Map the setting keys to string resources
    return when (name) {
        // Titles
        "condition" -> stringResource(R.string.condition)
        "humidity" -> stringResource(R.string.humidity)
        "wind" -> stringResource(R.string.wind)
        "precipitation" -> stringResource(R.string.precipitation)
        "sun_times" -> stringResource(R.string.sun_times)
        "uv_index" -> stringResource(R.string.uv_index)
        "forecast" -> stringResource(R.string.forecast)
        "forecast_days" -> stringResource(R.string.forecast_days)
        "language" -> stringResource(R.string.language)
        // Descriptions
        "condition_description" -> stringResource(R.string.condition_description)
        "humidity_description" -> stringResource(R.string.humidity_description)
        "wind_description" -> stringResource(R.string.wind_description)
        "precipitation_description" -> stringResource(R.string.precipitation_description)
        "sun_times_description" -> stringResource(R.string.sun_times_description)
        "uv_index_description" -> stringResource(R.string.uv_index_description)
        "forecast_description" -> stringResource(R.string.forecast_description)
        else -> name // fallback
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
    val context = LocalContext.current

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
                val dayText = when (day) {
                    1 -> context.getString(R.string.day_one)
                    2, 3, 4 -> context.getString(R.string.day_few)
                    else -> context.getString(R.string.day_many)
                }
                DropdownMenuItem(
                    text = { Text("$day $dayText") },
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
    data class LanguageSelector(
        val title: String,
        val currentPreference: LanguagePreference
    ) : SettingsItem
}

private fun settingsItems(prefs: WeatherDisplayPrefs, languagePreference: LanguagePreference): List<SettingsItem> {
    return listOf(
        SettingsItem.LanguageSelector(
            title = "language",
            currentPreference = languagePreference
        ),
        // Condition - first, required, cannot be toggled off
        SettingsItem.Toggle(
            key = "condition",
            title = "condition",
            description = "condition_description",
            checked = true,
            icon = { Icon(Icons.Filled.Thermostat, contentDescription = null) },
            enabled = false
        ),
        SettingsItem.Toggle(
            key = "humidity",
            title = "humidity",
            description = "humidity_description",
            checked = prefs.showHumidity,
            icon = { Icon(Icons.Filled.WaterDrop, contentDescription = null) }
        ),
        SettingsItem.Toggle(
            key = "wind",
            title = "wind",
            description = "wind_description",
            checked = prefs.showWind,
            icon = { Icon(Icons.Filled.Air, contentDescription = null) }
        ),
        SettingsItem.Toggle(
            key = "precipitation",
            title = "precipitation",
            description = "precipitation_description",
            checked = prefs.showPrecipitation,
            icon = { Icon(Icons.Filled.Cloud, contentDescription = null) }
        ),
        SettingsItem.Toggle(
            key = "sun_times",
            title = "sun_times",
            description = "sun_times_description",
            checked = prefs.showSunTimes,
            icon = { Icon(Icons.Filled.Settings, contentDescription = null) }
        ),
        SettingsItem.Toggle(
            key = "uv_index",
            title = "uv_index",
            description = "uv_index_description",
            checked = prefs.showUvIndex,
            icon = { Icon(Icons.Filled.Settings, contentDescription = null) }
        ),
        SettingsItem.Toggle(
            key = "forecast",
            title = "forecast",
            description = "forecast_description",
            checked = prefs.showForecast,
            icon = { Icon(Icons.Filled.Cloud, contentDescription = null) }
        ),
        SettingsItem.ForecastDays(
            title = "forecast_days",
            days = prefs.forecastDays
        )
    )
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefsRepository: WeatherDisplayPrefsRepository,
    private val languagePreferenceRepository: LanguagePreferenceRepository
) : ViewModel() {

    private val _originalPrefs = MutableStateFlow<WeatherDisplayPrefs>(WeatherDisplayPrefs())
    val localPrefs = MutableStateFlow<WeatherDisplayPrefs>(WeatherDisplayPrefs())

    val originalLanguagePreference = MutableStateFlow<LanguagePreference>(LanguagePreference.SYSTEM)
    val languagePreference = MutableStateFlow<LanguagePreference>(LanguagePreference.SYSTEM)

    val hasChanges = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            // Initialize display prefs
            val prefs = prefsRepository.getPrefs().first()
            _originalPrefs.value = prefs
            localPrefs.value = prefs
            
            // Initialize language preference
            val languagePref = languagePreferenceRepository.getLanguagePreference()
            originalLanguagePreference.value = languagePref
            languagePreference.value = languagePref
            
            updateHasChanges()
        }
    }

    private fun updateHasChanges() {
        val prefsChanged = _originalPrefs.value != localPrefs.value
        val languagePrefChanged = originalLanguagePreference.value != languagePreference.value
        hasChanges.value = prefsChanged || languagePrefChanged
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

    fun setLanguagePreference(preference: LanguagePreference) {
        languagePreference.value = preference
        updateHasChanges()
    }

    suspend fun savePrefsAndCheckLanguage(): Boolean {
        val languagePrefChanged = languagePreference.value != originalLanguagePreference.value

        // Save display prefs
        val prefsToSave = localPrefs.value
        prefsRepository.updatePrefs(prefsToSave)
        _originalPrefs.value = prefsToSave
        
        // Save language preference
        val languagePrefToSave = languagePreference.value
        languagePreferenceRepository.saveLanguagePreference(languagePrefToSave)
        originalLanguagePreference.value = languagePrefToSave
        
        updateHasChanges()
        
        return languagePrefChanged
    }

    fun resetPrefs() {
        localPrefs.value = _originalPrefs.value
        languagePreference.value = originalLanguagePreference.value
        updateHasChanges()
    }
}
