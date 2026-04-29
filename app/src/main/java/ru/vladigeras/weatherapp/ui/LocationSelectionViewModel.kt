package ru.vladigeras.weatherapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.vladigeras.weatherapp.data.Location
import ru.vladigeras.weatherapp.network.GeocodingResult
import ru.vladigeras.weatherapp.network.GeocodingService
import ru.vladigeras.weatherapp.repository.CitySearchCache
import ru.vladigeras.weatherapp.repository.LocationRepository
import ru.vladigeras.weatherapp.repository.SelectedLocationRepository
import javax.inject.Inject

@HiltViewModel
class LocationSelectionViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val geocodingService: GeocodingService,
    private val citySearchCache: CitySearchCache,
    private val selectedLocationRepository: SelectedLocationRepository
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val activeLocation: Location? = null,
        val autoLocation: Location? = null,
        val autoLocationLoading: Boolean = false,
        val isManualMode: Boolean = false,
        val error: String? = null,
        val searchResults: List<GeocodingResult> = emptyList(),
        val locationPermissionGranted: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadInitialState()
    }

    private fun loadInitialState() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val savedLocation = selectedLocationRepository.getSelectedLocation().first()
            loadAutoLocation()
            
            // Check location permission status
            val permissionGranted = locationRepository.hasLocationPermission()

            if (savedLocation != null && !savedLocation.isAutoDetected) {
                _uiState.value = _uiState.value.copy(
                    isManualMode = true,
                    activeLocation = savedLocation,
                    isLoading = false,
                    locationPermissionGranted = permissionGranted
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    locationPermissionGranted = permissionGranted
                )
            }
        }
    }

    private fun loadAutoLocation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(autoLocationLoading = true)

            locationRepository.getLocation()
                .onSuccess { location ->
                    val autoDetectedLocation = location.copy(isAutoDetected = true)
                    _uiState.value = _uiState.value.copy(
                        autoLocation = autoDetectedLocation,
                        autoLocationLoading = false,
                        isLoading = false,
                        locationPermissionGranted = true
                    )
                    if (!_uiState.value.isManualMode) {
                        _uiState.value = _uiState.value.copy(activeLocation = autoDetectedLocation)
                    }
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message ?: "Failed to get location",
                        autoLocationLoading = false,
                        isLoading = false,
                        locationPermissionGranted = false
                    )
                }
        }
    }

    fun refreshAutoLocation() {
        loadAutoLocation()
    }

    fun refreshLocationPermission() {
        viewModelScope.launch {
            val permissionGranted = locationRepository.hasLocationPermission()
            _uiState.value = _uiState.value.copy(locationPermissionGranted = permissionGranted)
        }
    }

    fun useAutoLocation(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val autoLocation = _uiState.value.autoLocation
            if (autoLocation != null) {
                selectedLocationRepository.saveSelectedLocation(autoLocation)
            }
            _uiState.value = _uiState.value.copy(
                isManualMode = false,
                activeLocation = autoLocation
            )
            onComplete()
        }
    }

    fun selectLocation(location: Location, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val manualLocation = location.copy(isAutoDetected = false)
            selectedLocationRepository.saveSelectedLocation(manualLocation)
            _uiState.value = _uiState.value.copy(
                isManualMode = true,
                activeLocation = manualLocation,
                searchResults = emptyList()
            )
            _searchQuery.value = ""
            onComplete()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.length >= 2) {
            searchCity(query)
        } else {
            _uiState.value = _uiState.value.copy(searchResults = emptyList())
        }
    }

    private fun searchCity(query: String) {
        val cached = citySearchCache.get(query)
        if (cached != null) {
            _uiState.value = _uiState.value.copy(searchResults = cached)
            return
        }

        viewModelScope.launch {
            geocodingService.searchCity(query)
                .onSuccess { response ->
                    val results = response.results ?: emptyList()
                    if (results.isNotEmpty()) {
                        citySearchCache.put(query, results)
                    }
                    _uiState.value = _uiState.value.copy(searchResults = results)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(error = "Search failed")
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}