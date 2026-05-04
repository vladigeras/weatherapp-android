package ru.vladigeras.weatherapp.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.vladigeras.weatherapp.data.DailyWeather
import ru.vladigeras.weatherapp.data.Location
import ru.vladigeras.weatherapp.data.WeatherDisplayPrefs
import ru.vladigeras.weatherapp.core.error.ErrorMapper
import ru.vladigeras.weatherapp.domain.mapper.WeatherMapper
import ru.vladigeras.weatherapp.repository.CityNameResolver
import ru.vladigeras.weatherapp.repository.LanguagePreferenceRepository
import ru.vladigeras.weatherapp.repository.LocationRepository
import ru.vladigeras.weatherapp.repository.SelectedLocationRepository
import ru.vladigeras.weatherapp.repository.WeatherCache
import ru.vladigeras.weatherapp.repository.WeatherDisplayPrefsRepository
import ru.vladigeras.weatherapp.repository.WeatherRepository
import ru.vladigeras.weatherapp.widget.WeatherWidgetProvider
import ru.vladigeras.weatherapp.widget.WidgetPrefsManager
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import javax.inject.Inject

sealed interface WeatherUiState {
    data object Loading : WeatherUiState
    data object Empty : WeatherUiState
    data class Success(
        val temperature: Double,
        val feelsLike: Double?,
        val humidity: Int,
        val windSpeed: Double,
        val weatherCode: Int,
        val isDay: Int,
        val timezone: String,
        val cityName: String,
        val temperatureUnit: String,
        val dailyForecast: List<DailyForecast> = emptyList(),
        val prefs: WeatherDisplayPrefs = WeatherDisplayPrefs()
    ) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}

@HiltViewModel
class WeatherViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val weatherRepository: WeatherRepository,
    private val locationRepository: LocationRepository,
    private val selectedLocationRepository: SelectedLocationRepository,
    private val weatherDisplayPrefsRepository: WeatherDisplayPrefsRepository,
    private val weatherCache: WeatherCache,
    private val languagePreferenceRepository: LanguagePreferenceRepository,
    private val cityNameResolver: CityNameResolver,
    private val weatherMapper: WeatherMapper
) : ViewModel() {

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var currentJob: Job? = null
    private var currentLatitude: Double = 0.0
    private var currentLongitude: Double = 0.0

    init {
        viewModelScope.launch {
            weatherDisplayPrefsRepository.getPrefs()
                .distinctUntilChanged()
                .collectLatest { prefs ->
                    if (currentJob?.isActive == true) return@collectLatest
                    if (currentLatitude != 0.0 || currentLongitude != 0.0) {
                        weatherCache.evict(currentLatitude, currentLongitude)
                        loadWeatherInternal(currentLatitude, currentLongitude, prefs)
                    }
                }
        }
    }

    suspend fun loadSavedLocation() {
        val savedLocation = selectedLocationRepository.getSelectedLocation().first()
        if (savedLocation != null) {
            currentLatitude = savedLocation.latitude
            currentLongitude = savedLocation.longitude
            val prefs = weatherDisplayPrefsRepository.getPrefs().first()
            loadWeatherInternal(savedLocation.latitude, savedLocation.longitude, prefs)
        } else {
            _uiState.value = WeatherUiState.Empty
        }
    }

    fun loadWeather(latitude: Double, longitude: Double, forceRefresh: Boolean = false) {
        currentLatitude = latitude
        currentLongitude = longitude

        viewModelScope.launch {
            val prefs = weatherDisplayPrefsRepository.getPrefs().first()
            loadWeatherInternal(latitude, longitude, prefs, forceRefresh)
        }
    }

    fun refreshActiveLocation() {
        viewModelScope.launch {
            if (currentLatitude != 0.0 && currentLongitude != 0.0) {
                loadWeather(currentLatitude, currentLongitude, forceRefresh = true)
            } else {
                loadWeatherForCurrentLocation(forceRefresh = true)
            }
        }
    }

    fun loadWeatherForCurrentLocation(forceRefresh: Boolean = false) {
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
            selectedLocationRepository.saveSelectedLocation(
                Location(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    name = loc.name,
                    isAutoDetected = true
                )
            )
            currentLatitude = loc.latitude
            currentLongitude = loc.longitude
            val prefs = weatherDisplayPrefsRepository.getPrefs().first()
            loadWeatherInternal(loc.latitude, loc.longitude, prefs, forceRefresh)
        }
    }

    private fun loadWeatherInternal(latitude: Double, longitude: Double, prefs: WeatherDisplayPrefs, forceRefresh: Boolean = false) {
        if (latitude == 0.0 && longitude == 0.0) return
        currentJob?.cancel()
        _uiState.value = WeatherUiState.Loading
        currentJob = viewModelScope.launch {
            val savedLocation = selectedLocationRepository.getSelectedLocation().first()
            weatherRepository.getWeather(latitude, longitude, prefs, forceRefresh)
                .onSuccess { response ->
                    val current = response.current
                    val hourly = response.hourly
                    val daily = response.daily
                    val feelsLike = current?.apparentTemperature
                    val humidity = hourly?.relativehumidity2m?.firstOrNull { it != null } ?: 0
                    val dailyForecast = weatherMapper.mapToDailyForecast(daily, response.utcOffsetSeconds)
                    val cityName = cityNameResolver.resolveCityName(latitude, longitude, savedLocation?.name, response.timezone)
                    _uiState.value = WeatherUiState.Success(
                        temperature = current?.temperature ?: 0.0,
                        feelsLike = feelsLike,
                        humidity = humidity,
                        windSpeed = current?.windSpeed ?: 0.0,
                        weatherCode = current?.weatherCode ?: 0,
                        isDay = current?.isDay ?: 1,
                        timezone = response.timezone,
                        cityName = cityName,
                        temperatureUnit = response.currentUnits?.temperatureUnit ?: "°C",
                        dailyForecast = dailyForecast,
                        prefs = prefs
                    )
                    WidgetPrefsManager.save(
                        context,
                        cityName,
                        current?.temperature ?: 0.0,
                        feelsLike,
                        current?.weatherCode ?: 0,
                        current?.isDay ?: 1,
                        response.currentUnits?.temperatureUnit ?: "°C"
                    )
                    WeatherWidgetProvider.updateAllWidgets(context)
                }
                .onFailure { error ->
                    _uiState.value = WeatherUiState.Error(
                        ErrorMapper.mapToUiMessage(error, context)
                    )
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        currentJob?.cancel()
    }
}