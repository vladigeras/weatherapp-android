package ru.vladigeras.weatherapp.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.vladigeras.weatherapp.data.DailyWeather
import ru.vladigeras.weatherapp.data.Location
import ru.vladigeras.weatherapp.data.WeatherDisplayPrefs
import ru.vladigeras.weatherapp.repository.LocationRepository
import ru.vladigeras.weatherapp.repository.SelectedLocationRepository
import ru.vladigeras.weatherapp.repository.WeatherCache
import ru.vladigeras.weatherapp.repository.WeatherDisplayPrefsRepository
import ru.vladigeras.weatherapp.repository.WeatherRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
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
        val temperatureUnit: String,
        val dailyForecast: List<DailyForecast> = emptyList(),
        val prefs: WeatherDisplayPrefs = WeatherDisplayPrefs()
    ) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository,
    private val locationRepository: LocationRepository,
    private val selectedLocationRepository: SelectedLocationRepository,
    private val weatherDisplayPrefsRepository: WeatherDisplayPrefsRepository,
    private val weatherCache: WeatherCache
) : ViewModel() {

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var currentJob: Job? = null
    private var currentLatitude: Double = 0.0
    private var currentLongitude: Double = 0.0

    init {
        viewModelScope.launch {
            weatherDisplayPrefsRepository.getPrefs().collectLatest { prefs ->
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
            loadWeatherInternal(loc.latitude, loc.longitude, prefs)
        }
    }

    fun loadWeather(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            val prefs = weatherDisplayPrefsRepository.getPrefs().first()
            loadWeatherInternal(latitude, longitude, prefs)
        }
    }

    private fun loadWeatherInternal(latitude: Double, longitude: Double, prefs: WeatherDisplayPrefs) {
        currentJob?.cancel()
        _uiState.value = WeatherUiState.Loading
        currentJob = viewModelScope.launch {
            weatherRepository.getWeather(latitude, longitude, prefs)
                .onSuccess { response ->
                    val current = response.current
                    val hourly = response.hourly
                    val daily = response.daily
                    val feelsLike = current?.apparentTemperature
                    val humidity = hourly?.relativehumidity2m?.firstOrNull { it != null } ?: 0
                    val dailyForecast = processDailyForecast(daily, response.utcOffsetSeconds)
                    _uiState.value = WeatherUiState.Success(
                        temperature = current?.temperature ?: 0.0,
                        feelsLike = feelsLike,
                        humidity = humidity,
                        windSpeed = current?.windSpeed ?: 0.0,
                        weatherCode = current?.weatherCode ?: 0,
                        isDay = current?.isDay ?: 1,
                        timezone = response.timezone,
                        temperatureUnit = response.currentUnits?.temperatureUnit ?: "°C",
                        dailyForecast = dailyForecast,
                        prefs = prefs
                    )
                }
                .onFailure { error ->
                    Log.e("WeatherViewModel", "Failed to load weather", error)
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

    private fun processDailyForecast(daily: DailyWeather?, utcOffsetSeconds: Int): List<DailyForecast> {
        daily ?: return emptyList()
        val size = daily.time.size
        val forecasts = mutableListOf<DailyForecast>()
        for (i in 0 until size) {
            val dateStr = daily.time[i]
            val dayName = getDayName(dateStr)
            val formattedDate = getFormattedDate(dateStr)
            forecasts.add(
                DailyForecast(
                    date = formattedDate,
                    dayName = dayName,
                    weatherCode = daily.weatherCode?.getOrNull(i),
                    temperatureMin = daily.temperature2mMin?.getOrNull(i) ?: 0.0,
                    temperatureMax = daily.temperature2mMax?.getOrNull(i) ?: 0.0,
                    precipitationSum = daily.precipitationSum?.getOrNull(i),
                    sunrise = daily.sunrise?.getOrNull(i)?.let { formatTimeFromUTC(it, utcOffsetSeconds) },
                    sunset = daily.sunset?.getOrNull(i)?.let { formatTimeFromUTC(it, utcOffsetSeconds) },
                    windSpeedMax = daily.windspeed10mMax?.getOrNull(i),
                    windDirectionDominant = daily.winddirection10mDominant?.getOrNull(i),
                    uvIndexMax = daily.uvIndexMax?.getOrNull(i)
                )
            )
        }
        return forecasts
    }

    private fun getDayName(isoDate: String): String {
        return try {
            val date = LocalDate.parse(isoDate)
            when (date.dayOfWeek) {
                DayOfWeek.SUNDAY -> "Sun"
                DayOfWeek.MONDAY -> "Mon"
                DayOfWeek.TUESDAY -> "Tue"
                DayOfWeek.WEDNESDAY -> "Wed"
                DayOfWeek.THURSDAY -> "Thu"
                DayOfWeek.FRIDAY -> "Fri"
                DayOfWeek.SATURDAY -> "Sat"
                else -> "?"
            }
        } catch (e: Exception) {
            "?"
        }
    }

    private fun getFormattedDate(isoDate: String): String {
        return try {
            val date = LocalDate.parse(isoDate)
            val day = date.dayOfMonth
            val monthName = date.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
            "$day $monthName"
        } catch (e: Exception) {
            isoDate
        }
    }

    private fun formatTimeFromUTC(utcTimeString: String, utcOffsetSeconds: Int): String {
        if (utcTimeString.isBlank()) return "--:--"
        val timePart = utcTimeString.substringAfterLast('T')
        val timeComponents = timePart.split(':')
        if (timeComponents.size < 2) return timePart
        val hours = timeComponents[0].toIntOrNull() ?: return timePart
        val minutes = timeComponents[1].toIntOrNull() ?: return timePart
        val totalMinutes = (hours * 60 + minutes) + (utcOffsetSeconds / 60)
        val adjustedMinutes = ((totalMinutes % 1440) + 1440) % 1440
        val localHours = adjustedMinutes / 60
        val localMinutes = adjustedMinutes % 60
        return String.format("%02d:%02d", localHours, localMinutes)
    }
}