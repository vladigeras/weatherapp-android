package ru.vladigeras.weatherapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.vladigeras.weatherapp.data.Location
import ru.vladigeras.weatherapp.network.GeocodingResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSelectionScreen(
    onLocationChosen: (latitude: Double, longitude: Double) -> Unit,
    onNavigateBack: () -> Unit,
    navController: androidx.navigation.NavHostController,
    viewModel: LocationSelectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Request permission refresh when needed
    LaunchedEffect(uiState.locationPermissionGranted) {
        if (!uiState.locationPermissionGranted && uiState.isManualMode) {
            // Try to refresh permission status periodically
            viewModel.refreshLocationPermission()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Location") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            ActiveLocationCard(
                isManualMode = uiState.isManualMode,
                activeLocation = uiState.activeLocation,
                autoLocationLoading = uiState.autoLocationLoading,
                locationPermissionGranted = uiState.locationPermissionGranted,
                onUseLocation = { location ->
                    viewModel.selectLocation(location) {
                        onNavigateBack()
                    }
                },
                onSwitchToAuto = {
                    viewModel.useAutoLocation {
                        // This runs after save is complete
                        val autoLocation = uiState.autoLocation
                        if (autoLocation != null) {
                            navController.previousBackStackEntry?.
                                savedStateHandle?.
                                set("latitude", autoLocation.latitude)
                            navController.previousBackStackEntry?.
                                savedStateHandle?.
                                set("longitude", autoLocation.longitude)
                        }
                        onNavigateBack()
                    }
                },
                onRefreshAuto = { viewModel.refreshAutoLocation() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            SearchSection(
                query = query,
                onQueryChange = { viewModel.updateSearchQuery(it) },
                searchResults = uiState.searchResults,
                onResultSelected = { result ->
                    val fullName = buildString {
                        append(result.name)
                        result.admin1?.let { append(", $it") }
                        result.country?.let { append(", $it") }
                    }
                    val location = Location(result.latitude, result.longitude, fullName)
                    // Save location and wait for completion before navigating back
                    viewModel.selectLocation(location) {
                        val entry = navController.previousBackStackEntry
                        entry?.savedStateHandle?.set("latitude", result.latitude)
                        entry?.savedStateHandle?.set("longitude", result.longitude)
                        onNavigateBack()
                    }
                }
            )
        }
    }
}

@Composable
private fun ActiveLocationCard(
    isManualMode: Boolean,
    activeLocation: Location?,
    autoLocationLoading: Boolean,
    locationPermissionGranted: Boolean,
    onUseLocation: (Location) -> Unit,
    onSwitchToAuto: () -> Unit,
    onRefreshAuto: () -> Unit
) {
    when {
        activeLocation == null && autoLocationLoading -> {
            LoadingCard()
        }
        activeLocation == null -> {
            ErrorCard(onRetry = onRefreshAuto)
        }
        else -> {
            LocationCard(
                location = activeLocation,
                isManualMode = isManualMode,
                locationPermissionGranted = locationPermissionGranted,
                onUse = { onUseLocation(activeLocation) },
                onSwitchToAuto = onSwitchToAuto,
                onRefresh = onRefreshAuto
            )
        }
    }
}

@Composable
private fun LocationCard(
    location: Location,
    isManualMode: Boolean,
    locationPermissionGranted: Boolean,
    onUse: () -> Unit,
    onSwitchToAuto: () -> Unit,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (location.isAutoDetected) Icons.Default.GpsFixed else Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (location.isAutoDetected) "Auto-detected location" else "Selected location",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = location.name ?: "Unknown location",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${"%.4f".format(location.latitude)}, ${"%.4f".format(location.longitude)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                if (!isManualMode) {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Show permission status when in manual mode and permission is not granted
            if (isManualMode && !locationPermissionGranted) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Location permission required",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Enable location access to use auto-detection",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Always show button to switch to auto-detected when manual is active
            // Disable it when location permission is not granted
            if (isManualMode) {
                OutlinedButton(
                    onClick = {
                        if (locationPermissionGranted) {
                            onSwitchToAuto()
                        } else {
                            // Show snackbar or dialog to guide user to settings
                            // This will be handled by the ViewModel or a separate dialog
                        }
                    },
                    enabled = locationPermissionGranted,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Use auto-detected")
                }
            }
        }
    }
}

@Composable
private fun LoadingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text("Detecting your location...")
        }
    }
}

@Composable
private fun ErrorCard(onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Could not get your location",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun SearchSection(
    query: String,
    onQueryChange: (String) -> Unit,
    searchResults: List<GeocodingResult>,
    onResultSelected: (GeocodingResult) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text("Search city") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear")
                }
            }
        }
    )

    if (searchResults.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp)
                .verticalScroll(rememberScrollState())
        ) {
            searchResults.forEach { result ->
                SearchResultItem(
                    result = result,
                    onClick = { onResultSelected(result) }
                )
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    result: GeocodingResult,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = result.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                val subtitle = listOfNotNull(result.admin1, result.country).joinToString(", ")
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
