package ru.vladigeras.weatherapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelAndJoin
import ru.vladigeras.weatherapp.repository.LocationRepository
import ru.vladigeras.weatherapp.repository.SelectedLocationRepository
import ru.vladigeras.weatherapp.repository.WeatherRepository
import javax.inject.Inject

sealed interface WeatherUiState {
    data object Loading : WeatherUiState
    data object Empty : WeatherUiState
    data class Success(
        val temperature: Double,
        val feelsLike: Double,
        val humidity: Int,
        val windSpeed: Double,
        val weatherCode: Int,
        val timezone: String
    ) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository,
    private val locationRepository: LocationRepository,
    private val selectedLocationRepository: SelectedLocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()
    
    private var currentJob: Job? = null

    suspend fun loadSavedLocation() {
        val savedLocation = selectedLocationRepository.getSelectedLocation().first()
        if (savedLocation != null) {
            loadWeather(savedLocation.latitude, savedLocation.longitude)
        } else {
            _uiState.value = WeatherUiState.Empty
        }
    }

    fun loadWeatherForCurrentLocation() {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading

            val locationResult = locationRepository.getLocation()
            if (locationResult.isFailure) {
                _uiState.value = WeatherUiState.Error(
                    locationResult.exceptionOrNull()?.message ?: "Unable to get location"
                )
                return@launch
            }

            val loc = locationResult.getOrThrow()
            loadWeather(loc.latitude, loc.longitude)
        }
    }

    fun loadWeather(latitude: Double, longitude: Double) {
        currentJob?.cancel()
        _uiState.value = WeatherUiState.Loading
        
        currentJob = viewModelScope.launch {
            weatherRepository.getWeather(latitude, longitude)
                .onSuccess { response ->
                    val currentWeather = response.currentWeather
                    val hourly = response.hourly

                    val feelsLike = calculateFeelsLike(
                        currentWeather.temperature,
                        currentWeather.windSpeed
                    )

                    val humidity = hourly?.relativehumidity2m?.firstOrNull() ?: 0

                    _uiState.value = WeatherUiState.Success(
                        temperature = currentWeather.temperature,
                        feelsLike = feelsLike,
                        humidity = humidity,
                        windSpeed = currentWeather.windSpeed,
                        weatherCode = currentWeather.weatherCode,
                        timezone = response.timezone
                    )
                }
                .onFailure { error ->
                    _uiState.value = WeatherUiState.Error(
                        error.message ?: "Unknown error occurred"
                    )
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        currentJob?.cancel()
    }

    private fun calculateFeelsLike(temperature: Double, windSpeed: Double): Double {
        return temperature + (0.0555 * windSpeed)
    }
}