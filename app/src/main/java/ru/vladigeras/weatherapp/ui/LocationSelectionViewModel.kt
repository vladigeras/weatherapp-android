package ru.vladigeras.weatherapp.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.vladigeras.weatherapp.data.Location
import ru.vladigeras.weatherapp.data.SearchLocation
import ru.vladigeras.weatherapp.data.WeatherProviderId
import ru.vladigeras.weatherapp.repository.WeatherRepository
import ru.vladigeras.weatherapp.repository.WeatherDisplayPrefsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.cancellation.CancellationException
import ru.vladigeras.weatherapp.repository.LanguagePreferenceRepository
import ru.vladigeras.weatherapp.repository.LocationRepository
import ru.vladigeras.weatherapp.repository.SelectedLocationRepository
import android.content.Context
import ru.vladigeras.weatherapp.core.error.ErrorMapper
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class LocationSelectionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val savedStateHandle: SavedStateHandle,
    private val locationRepository: LocationRepository,
    private val weatherRepository: WeatherRepository,
    private val prefsRepository: WeatherDisplayPrefsRepository,
    private val selectedLocationRepository: SelectedLocationRepository,
    private val languagePreferenceRepository: LanguagePreferenceRepository
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val activeLocation: Location? = null,
        val autoLocation: Location? = null,
        val autoLocationLoading: Boolean = false,
        val isManualMode: Boolean = false,
        val error: String? = null,
        val searchResults: List<SearchLocation> = emptyList(),
        val locationPermissionGranted: Boolean = false,
        val provider: WeatherProviderId? = null,
        val searchLoading: Boolean = false,
        val explicitSearch: Boolean = false,
        val searchCompleted: Boolean = false,
        val searchError: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _searchQuery = savedStateHandle.getStateFlow("search_query", "")
    val searchQuery: StateFlow<String> = _searchQuery

    init {
        loadInitialState()
        viewModelScope.launch {
            prefsRepository.getPrefs().collect { prefs ->
                if (_uiState.value.provider != prefs.provider) {
                    cancelSearch()
                    _uiState.value = _uiState.value.copy(provider = prefs.provider, searchResults = emptyList(), searchLoading = false, searchCompleted = false, searchError = null,
                        explicitSearch = weatherRepository.capabilities(prefs.provider).explicitSearch)
                    if (!_uiState.value.explicitSearch && _searchQuery.value.length >= 2) search(_searchQuery.value)
                }
            }
        }
        viewModelScope.launch {
            searchQuery.debounce(300).distinctUntilChanged().collect { query ->
                if (query.length >= 2 && !_uiState.value.explicitSearch) search(query)
            }
        }
    }

    private var searchJob: Job? = null
    private var searchGeneration = 0L

    private fun cancelSearch() {
        ++searchGeneration
        searchJob?.cancel()
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
                        error = ErrorMapper.mapToUiMessage(error, context),
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
            savedStateHandle["search_query"] = ""
            onComplete()
        }
    }

    fun updateSearchQuery(query: String) {
        cancelSearch()
        savedStateHandle["search_query"] = query
        _uiState.value = _uiState.value.copy(searchResults = emptyList(), searchLoading = false, searchCompleted = false, searchError = null)
    }

    fun submitSearch() {
        if (_searchQuery.value.length >= 2) search(_searchQuery.value)
    }

    private fun search(query: String) {
        val provider = _uiState.value.provider ?: return
        cancelSearch()
        val version = searchGeneration
        searchJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(searchLoading = true, searchCompleted = false, searchError = null)
            try {
                val language = languagePreferenceRepository.getEffectiveLocaleCode()
                val results = weatherRepository.searchLocations(provider, query, language).getOrThrow()
                currentCoroutineContext().ensureActive()
                if (version == searchGeneration) _uiState.value = _uiState.value.copy(searchResults = results, searchLoading = false, searchCompleted = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (version == searchGeneration) _uiState.value = _uiState.value.copy(searchError = ErrorMapper.mapToUiMessage(e, context), searchLoading = false)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
