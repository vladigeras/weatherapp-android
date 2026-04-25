package ru.vladigeras.weatherapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.vladigeras.weatherapp.repository.WeatherRepository
import javax.inject.Inject

sealed interface WeatherUiState {
    data object Loading : WeatherUiState
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
    private val weatherRepository: WeatherRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()
    
    init {
        loadWeather(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
    }
    
    fun loadWeather(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            
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
    
    private fun calculateFeelsLike(temperature: Double, windSpeed: Double): Double {
        return temperature + (0.0555 * windSpeed)
    }
    
    companion object {
        private const val DEFAULT_LATITUDE = 55.7558
        private const val DEFAULT_LONGITUDE = 37.6173
    }
}