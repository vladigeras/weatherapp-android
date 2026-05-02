package ru.vladigeras.weatherapp.ui

import android.Manifest
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import ru.vladigeras.weatherapp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    savedStateHandle: SavedStateHandle,
    onNavigateToLocationSelection: () -> Unit = {},
    viewModel: WeatherViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentState = uiState
    val context = LocalContext.current

    val savedLatitude by savedStateHandle.getStateFlow<Double?>("latitude", null).collectAsState(initial = null)
    val savedLongitude by savedStateHandle.getStateFlow<Double?>("longitude", null).collectAsState(initial = null)
    val hasSavedLocation = savedLatitude != null && savedLongitude != null

    var hasLocationPermission by remember { mutableStateOf(false) }
    var showPermissionError by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (hasLocationPermission) {
            showPermissionError = false
            viewModel.loadWeatherForCurrentLocation()
        } else {
            showPermissionError = true
        }
    }

    fun requestLocationPermission() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    val onSelectLocation = onNavigateToLocationSelection
    val onRequestPermission = ::requestLocationPermission

    LaunchedEffect(savedLatitude, savedLongitude) {
        val lat = savedLatitude
        val lon = savedLongitude
        if (lat != null && lon != null) {
            viewModel.loadWeather(lat, lon)
        } else {
            viewModel.loadSavedLocation()
        }
    }

    val pullToRefreshState = rememberPullToRefreshState()
    val isRefreshing = currentState is WeatherUiState.Loading
    
    val weatherUpdatedText = stringResource(R.string.weather_updated)
    val weatherErrorText = stringResource(R.string.weather_error)
    
    LaunchedEffect(currentState) {
        if (currentState is WeatherUiState.Success) {
            Toast.makeText(context, weatherUpdatedText, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(currentState) {
        if (currentState is WeatherUiState.Error) {
            val errorMessage = currentState.message
            Log.e("WeatherScreen", "Weather error: $errorMessage")
            Toast.makeText(context, weatherErrorText.format(errorMessage), Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Weatherapp", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToLocationSelection) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = "Select location",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    IconButton(onClick = {
                        val intent = android.content.Intent(context, SettingsActivity::class.java)
                        context.startActivity(intent)
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        val isActuallyLoading = currentState is WeatherUiState.Loading

        PullToRefreshBox(
            isRefreshing = isActuallyLoading,
            onRefresh = {
                viewModel.refreshActiveLocation()
            },
            state = pullToRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedVisibility(visible = currentState is WeatherUiState.Loading, enter = fadeIn(), exit = fadeOut()) {
                SkeletonLoader()
            }

            AnimatedVisibility(visible = currentState is WeatherUiState.Success, enter = fadeIn(), exit = fadeOut()) {
                val state = currentState as? WeatherUiState.Success
                state?.let { SuccessContent(it) }
            }

            AnimatedVisibility(visible = currentState is WeatherUiState.Error, enter = fadeIn(), exit = fadeOut()) {
                val state = currentState as? WeatherUiState.Error
                state?.let {
                    ErrorContent(
                        state = it,
                        onRetry = {
                            val lat = savedLatitude
                            val lon = savedLongitude
                            when {
                                lat != null && lon != null -> viewModel.loadWeather(lat, lon, forceRefresh = true)
                                hasLocationPermission -> viewModel.loadWeatherForCurrentLocation(forceRefresh = true)
                                else -> onNavigateToLocationSelection()
                            }
                        },
                        onSelectLocation = onNavigateToLocationSelection
                    )
                }
            }

            AnimatedVisibility(
                visible = !hasSavedLocation && !hasLocationPermission
                        && currentState is WeatherUiState.Empty,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                EmptyStateContent(
                    onSelectLocation = onSelectLocation,
                    onRequestPermission = onRequestPermission
                )
            }
        }
    }
}

@Composable
private fun SkeletonLoader() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        SkeletonCard(modifier = Modifier.fillMaxWidth().height(200.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(2) { SkeletonCard(modifier = Modifier.weight(1f).height(120.dp)) }
        }
        Spacer(modifier = Modifier.height(16.dp))
        SkeletonCard(modifier = Modifier.fillMaxWidth().height(100.dp))
    }
}

@Composable
private fun SkeletonCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp), color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SuccessContent(state: WeatherUiState.Success) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        WeatherMainCard(temperature = state.temperature, weatherCode = state.weatherCode, isDay = state.isDay, cityName = state.cityName, temperatureUnit = state.temperatureUnit)
        Spacer(modifier = Modifier.height(16.dp))
        
        // Show detail cards based on user preferences
        if (state.prefs.showCondition || state.prefs.showHumidity || state.prefs.showWind) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (state.prefs.showCondition && state.feelsLike != null) {
                    DetailCard(
                        icon = Icons.Filled.Thermostat,
                        label = stringResource(R.string.feels_like),
                        value = "${state.feelsLike.toInt()}${state.temperatureUnit}",
                        modifier = Modifier.weight(1f)
                    )
                }
                if (state.prefs.showHumidity) {
                    DetailCard(
                        icon = Icons.Filled.WaterDrop,
                        label = stringResource(R.string.humidity),
                        value = "${state.humidity}%",
                        modifier = Modifier.weight(1f)
                    )
                }
                if (state.prefs.showWind) {
                    DetailCard(
                        icon = Icons.Filled.Air,
                        label = stringResource(R.string.wind_speed),
                        value = "${state.windSpeed.toInt()} ${stringResource(R.string.wind_speed_unit)}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // Show daily forecast if enabled
        if (state.prefs.showForecast) {
            DailyForecastList(dailyForecast = state.dailyForecast, temperatureUnit = state.temperatureUnit)
        }
    }
}

@Composable
private fun WeatherMainCard(temperature: Double, weatherCode: Int, isDay: Int, cityName: String, temperatureUnit: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val weatherIcon = getWeatherIconForCode(weatherCode, isDay)
            val weatherEntries = stringArrayResource(R.array.weather_codes)
            val unknownWeather = stringResource(R.string.unknown_weather)
            val weatherDesc = remember(weatherCode, weatherEntries) {
                weatherEntries.find { it.split("|").firstOrNull()?.toIntOrNull() == weatherCode }
                    ?.let { it.split("|")[1] } ?: unknownWeather
            }
            Icon(
                imageVector = weatherIcon,
                contentDescription = weatherDesc,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "${temperature.toInt()}$temperatureUnit", fontSize = 80.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(text = cityName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun DetailCard(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun ErrorContent(
    state: WeatherUiState.Error,
    onRetry: () -> Unit,
    onSelectLocation: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.Cloud,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.something_went_wrong),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = state.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onSelectLocation) {
                Text(stringResource(R.string.select_city))
            }
            Button(onClick = onRetry) {
                Text(stringResource(R.string.use_gps)) // Или добавить строку R.string.retry
            }
        }
    }
}

@Composable
private fun EmptyStateContent(
    onSelectLocation: () -> Unit,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.select_location),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.choose_city),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onSelectLocation) {
            Text(stringResource(R.string.select_city))
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedButton(onClick = onRequestPermission) {
            Text(stringResource(R.string.use_gps))
        }
    }
}