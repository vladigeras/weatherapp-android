package ru.vladigeras.weatherapp.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.vladigeras.weatherapp.core.error.ErrorMapper
import ru.vladigeras.weatherapp.data.Location
import ru.vladigeras.weatherapp.data.WeatherDisplayPrefs
import ru.vladigeras.weatherapp.data.WeatherProviderId
import ru.vladigeras.weatherapp.domain.mapper.WeatherMapper
import ru.vladigeras.weatherapp.repository.CityNameResolver
import ru.vladigeras.weatherapp.repository.LocationRepository
import ru.vladigeras.weatherapp.repository.SelectedLocationRepository
import ru.vladigeras.weatherapp.repository.WeatherDisplayPrefsRepository
import ru.vladigeras.weatherapp.repository.WeatherRepository
import ru.vladigeras.weatherapp.widget.WeatherWidgetProvider
import ru.vladigeras.weatherapp.widget.WidgetPrefsManager
import javax.inject.Inject
import java.time.ZoneId
import kotlin.coroutines.cancellation.CancellationException

sealed interface WeatherUiState {
    data object Loading : WeatherUiState
    data object Empty : WeatherUiState
    data class Success(
        val temperature: Double?,
        val feelsLike: Double?,
        val humidity: Int?,
        val windSpeed: Double?,
        val weatherCode: Int?,
        val isDay: Int?,
        val timezone: String?,
        val cityName: String,
        val temperatureUnit: String,
        val dailyForecast: List<DailyForecast> = emptyList(),
        val hourlyForecast: List<HourlyForecast> = emptyList(),
        val prefs: WeatherDisplayPrefs = WeatherDisplayPrefs(),
        val hourlyStepHours: Int = 1
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
    private val cityNameResolver: CityNameResolver,
    private val weatherMapper: WeatherMapper
) : ViewModel() {
    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()
    private val _showUpdateToast = MutableStateFlow(false)
    val showUpdateToast: StateFlow<Boolean> = _showUpdateToast.asStateFlow()
    private var currentJob: Job? = null
    private var coordinates: Pair<Double, Double>? = null
    private var generation = 0L
    private var latestPrefs: WeatherDisplayPrefs? = null

    init {
        viewModelScope.launch {
            weatherDisplayPrefsRepository.getPrefs().distinctUntilChanged().collect { prefs ->
                val previous = latestPrefs
                latestPrefs = prefs
                WidgetPrefsManager.activateProvider(context, prefs.provider)
                WeatherWidgetProvider.updateAllWidgets(context)
                if (previous != null && previous != prefs) {
                    coordinates?.let { (latitude, longitude) -> loadWeather(latitude, longitude) }
                }
            }
        }
    }

    suspend fun loadSavedLocation() {
        val version = generation
        val location = selectedLocationRepository.getSelectedLocation().first()
        if (version != generation) return
        if (location == null) _uiState.value = WeatherUiState.Empty
        else loadWeather(location.latitude, location.longitude)
    }

    fun loadWeather(latitude: Double, longitude: Double, forceRefresh: Boolean = false) {
        coordinates = latitude to longitude
        startLoad { version -> fetchWeather(latitude, longitude, forceRefresh, version) }
    }

    fun refreshActiveLocation() {
        _showUpdateToast.value = true
        coordinates?.let { (latitude, longitude) -> loadWeather(latitude, longitude, true) }
            ?: loadWeatherForCurrentLocation(true)
    }

    fun reloadIfTimeZoneChanged() {
        val state = _uiState.value as? WeatherUiState.Success ?: return
        if (state.prefs.provider in listOf(WeatherProviderId.SEVEN_TIMER, WeatherProviderId.YR) && state.timezone != ZoneId.systemDefault().id) {
            coordinates?.let { (latitude, longitude) -> loadWeather(latitude, longitude) }
        }
    }

    fun loadWeatherForCurrentLocation(forceRefresh: Boolean = false) {
        startLoad { version ->
            val location = locationRepository.getLocation(forceRefresh).getOrThrow()
            currentCoroutineContext().ensureActive()
            if (version != generation) return@startLoad
            selectedLocationRepository.saveSelectedLocation(location.copy(isAutoDetected = true))
            coordinates = location.latitude to location.longitude
            fetchWeather(location.latitude, location.longitude, forceRefresh, version)
        }
    }

    private fun startLoad(block: suspend (Long) -> Unit) {
        val version = ++generation
        currentJob?.cancel()
        _uiState.value = WeatherUiState.Loading
        currentJob = viewModelScope.launch {
            try {
                block(version)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (version == generation) _uiState.value = WeatherUiState.Error(ErrorMapper.mapToUiMessage(e, context))
            }
        }
    }

    private suspend fun fetchWeather(latitude: Double, longitude: Double, forceRefresh: Boolean, version: Long) {
        val prefs = weatherDisplayPrefsRepository.getPrefs().first()
        val savedLocation = selectedLocationRepository.getSelectedLocation().first()
        val response = weatherRepository.getWeather(latitude, longitude, prefs, forceRefresh).getOrThrow()
        val savedName = savedLocation?.takeIf { it.latitude == latitude && it.longitude == longitude }?.name
        val cityName = cityNameResolver.resolveCityName(latitude, longitude, savedName, response.timezone ?: "")
        val daily = weatherMapper.mapToDailyForecast(response.daily)
        val hourly = weatherMapper.mapToHourlyForecast(response.hourly, response.timezone, prefs.hourlyForecastHours)
        currentCoroutineContext().ensureActive()
        if (version != generation || latestPrefs != prefs) return
        val current = response.current
        WidgetPrefsManager.save(context, cityName, current.temperature, current.feelsLike, current.condition?.displayCode,
            current.isDay, response.temperatureUnit, response.provider)
        currentCoroutineContext().ensureActive()
        if (version != generation || latestPrefs != prefs) return
        _uiState.value = WeatherUiState.Success(
            current.temperature, current.feelsLike, current.humidity, current.windSpeed, current.condition?.displayCode,
            current.isDay, response.timezone, cityName, response.temperatureUnit, daily, hourly,
            weatherRepository.capabilities(prefs.provider).effectivePrefs(prefs), weatherRepository.capabilities(prefs.provider).hourlyStepHours
        )
        _showUpdateToast.value = false
        WeatherWidgetProvider.updateAllWidgets(context)
    }

    override fun onCleared() {
        ++generation
        currentJob?.cancel()
        super.onCleared()
    }
}
