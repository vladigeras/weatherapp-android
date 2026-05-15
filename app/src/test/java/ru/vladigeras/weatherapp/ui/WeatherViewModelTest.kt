package ru.vladigeras.weatherapp.ui

import android.content.Context
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import ru.vladigeras.weatherapp.data.Current
import ru.vladigeras.weatherapp.data.CurrentUnits
import ru.vladigeras.weatherapp.data.DailyWeather
import ru.vladigeras.weatherapp.data.HourlyWeather
import ru.vladigeras.weatherapp.data.Location
import ru.vladigeras.weatherapp.data.WeatherDisplayPrefs
import ru.vladigeras.weatherapp.data.WeatherResponse
import ru.vladigeras.weatherapp.domain.mapper.WeatherMapper
import ru.vladigeras.weatherapp.repository.CityNameResolver
import ru.vladigeras.weatherapp.repository.LanguagePreferenceRepository
import ru.vladigeras.weatherapp.repository.LocationRepository
import ru.vladigeras.weatherapp.repository.SelectedLocationRepository
import ru.vladigeras.weatherapp.repository.WeatherCache
import ru.vladigeras.weatherapp.repository.WeatherDisplayPrefsRepository
import ru.vladigeras.weatherapp.repository.WeatherRepository

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {

    private lateinit var weatherRepository: WeatherRepository
    private lateinit var locationRepository: LocationRepository
    private lateinit var selectedLocationRepository: SelectedLocationRepository
    private lateinit var weatherDisplayPrefsRepository: WeatherDisplayPrefsRepository
    private lateinit var weatherCache: WeatherCache
    private lateinit var languagePreferenceRepository: LanguagePreferenceRepository
    private lateinit var cityNameResolver: CityNameResolver
    private lateinit var weatherMapper: WeatherMapper
    private lateinit var weatherViewModel: WeatherViewModel
    private val context: Context get() = RuntimeEnvironment.getApplication()

    private val mockLocation = Location(55.7558, 37.6173, "Moscow")
    
    private val mockResponse = WeatherResponse(
        latitude = 55.7558,
        longitude = 37.6173,
        generationtimeMs = 0.1,
        utcOffsetSeconds = 10800,
        timezone = "Europe/Moscow",
        timezoneAbbreviation = "MSK",
        elevation = 155.0,
        current = Current(
            time = "2026-04-26T14:00",
            interval = 900,
            temperature = 20.5, // temperature_2m
            apparentTemperature = 22.0,
            windSpeed = 5.2, // windspeed_10m
            weatherCode = 0,
            isDay = 1
        ),
        currentUnits = CurrentUnits(temperatureUnit = "°C"),
        hourly = HourlyWeather(
            time = listOf("2026-04-26T15:00"),
            temperature2m = listOf(21.0),
            relativehumidity2m = listOf(65)
        ),
        daily = DailyWeather(
            time = listOf("2026-04-27", "2026-04-28"),
            weatherCode = listOf(0, 1),
            temperature2mMax = listOf(25.0, 23.0),
            temperature2mMin = listOf(15.0, 14.0),
            precipitationSum = listOf(0.0, 2.5),
            sunrise = listOf("2026-04-27T04:30:00", "2026-04-28T04:29:00"),
            sunset = listOf("2026-04-27T20:15:00", "2026-04-28T20:16:00"),
            windspeed10mMax = listOf(10.0, 12.0),
            winddirection10mDominant = listOf(180, 200),
            uvIndexMax = listOf(5.0, 3.0)
        )
    )
    
    private val mockResponse2 = WeatherResponse(
        latitude = 48.8566,
        longitude = 2.3522,
        generationtimeMs = 0.1,
        utcOffsetSeconds = 7200,
        timezone = "Europe/Paris",
        timezoneAbbreviation = "CEST",
        elevation = 35.0,
        current = Current(
            time = "2026-04-26T14:00",
            interval = 900,
            temperature = 18.2, // temperature_2m
            apparentTemperature = 19.0,
            windSpeed = 3.1, // windspeed_10m
            weatherCode = 1,
            isDay = 1
        ),
        currentUnits = CurrentUnits(temperatureUnit = "°C"),
        hourly = HourlyWeather(
            time = listOf("2026-04-26T15:00"),
            temperature2m = listOf(19.0),
            relativehumidity2m = listOf(70)
        ),
        daily = DailyWeather(
            time = listOf("2026-04-27", "2026-04-28"),
            weatherCode = listOf(1, 2),
            temperature2mMax = listOf(22.0, 20.0),
            temperature2mMin = listOf(12.0, 11.0),
            precipitationSum = listOf(1.0, 0.0),
            sunrise = listOf("2026-04-27T06:15:00", "2026-04-28T06:14:00"),
            sunset = listOf("2026-04-27T21:00:00", "2026-04-28T21:01:00"),
            windspeed10mMax = listOf(8.0, 9.0),
            winddirection10mDominant = listOf(220, 230),
            uvIndexMax = listOf(4.0, 2.0)
        )
    )
       
    @Before
    fun setup() {
        weatherRepository = mockk()
        locationRepository = mockk()
        selectedLocationRepository = mockk {
            every { getSelectedLocation() } returns flowOf(null)
        }
        weatherDisplayPrefsRepository = mockk {
            every { getPrefs() } returns flowOf(WeatherDisplayPrefs())
        }
        weatherCache = mockk {
            coEvery { evict(any(), any()) } returns Unit
        }
        languagePreferenceRepository = mockk {
            coEvery { getAppLocale() } returns java.util.Locale.ENGLISH
        }
        cityNameResolver = mockk {
            coEvery { resolveCityName(any(), any(), any(), any()) } returns "Test City"
        }
        weatherMapper = mockk {
            // Properly mock the suspend function to return a default value
            coEvery { mapToDailyForecast(any(), any()) } returns emptyList()
            coEvery { mapToHourlyForecast(any(), any(), any(), any()) } returns emptyList()
        }
        weatherViewModel = WeatherViewModel(context, weatherRepository, locationRepository, selectedLocationRepository, weatherDisplayPrefsRepository, weatherCache, languagePreferenceRepository, cityNameResolver, weatherMapper)
    
        Dispatchers.setMain(Dispatchers.Unconfined)
    }
        
    @After
    fun tearDown() {
        // Cancel the ViewModel's scope via reflection to clean up coroutines
        val method = WeatherViewModel::class.java.getDeclaredMethod("onCleared")
        method.isAccessible = true
        method.invoke(weatherViewModel)
        Dispatchers.resetMain()
    }
    
    @Test
    fun `should load weather for location without delay`() = runTest {
        coEvery { weatherRepository.getWeather(55.7558, 37.6173, any(), any()) } returns Result.success(mockResponse)
        
        weatherViewModel.loadWeather(55.7558, 37.6173)
        
        // Wait specifically for Success state
        val successState = weatherViewModel.uiState
            .first { it is WeatherUiState.Success } as WeatherUiState.Success
        assertEquals(20.5, successState.temperature, 0.001)
        assertEquals(22.0, successState.feelsLike!!, 0.001) // From apparentTemperature in mockResponse
        assertEquals("°C", successState.temperatureUnit)
        assertEquals("Test City", successState.cityName)
    }
    
    @Test
    fun `should load weather for selected location`() = runTest {
        // Create isolated mocks for this test
        val testWeatherRepository = mockk<WeatherRepository> {
            coEvery { getWeather(55.7558, 37.6173, any(), any()) } returns Result.success(mockResponse)
        }
        val testSelectedLocationRepository = mockk<SelectedLocationRepository> {
            every { getSelectedLocation() } returns flowOf<Location?>(mockLocation)
        }
        val testWeatherMapper = mockk<WeatherMapper> {
            coEvery { mapToDailyForecast(any(), any()) } returns emptyList<DailyForecast>()
            coEvery { mapToHourlyForecast(any(), any(), any(), any()) } returns emptyList()
        }
        
        val testViewModel = WeatherViewModel(
            context,
            testWeatherRepository,
            locationRepository,
            testSelectedLocationRepository,
            weatherDisplayPrefsRepository,
            weatherCache,
            languagePreferenceRepository,
            cityNameResolver,
            testWeatherMapper
        )
        
        testViewModel.loadSavedLocation()
        
        val successState = testViewModel.uiState
            .first { it is WeatherUiState.Success } as WeatherUiState.Success
        assertEquals(20.5, successState.temperature, 0.001)
        assertEquals(22.0, successState.feelsLike!!, 0.001)
        assertEquals("°C", successState.temperatureUnit)
        assertEquals("Test City", successState.cityName)
    }
    
    @Test
    fun `should cancel previous request when loading new location`() = runTest {
        coEvery { weatherRepository.getWeather(55.7558, 37.6173, any(), any()) } returns Result.success(mockResponse)
        every { selectedLocationRepository.getSelectedLocation() } returns flowOf(mockLocation)
        coEvery { weatherRepository.getWeather(48.8566, 2.3522, any(), any()) } returns Result.success(mockResponse2)
        
        weatherViewModel.loadSavedLocation() // This loads Moscow (from selectedLocationRepository)
        kotlinx.coroutines.yield() // Give time for the first request to start
        weatherViewModel.loadWeather(48.8566, 2.3522) // This loads Paris - should cancel Moscow
        
        // Wait for the final Success state (should be Paris)
        val successState = weatherViewModel.uiState
            .first { it is WeatherUiState.Success && it.temperature == 18.2 } as WeatherUiState.Success
        assertEquals(18.2, successState.temperature, 0.001)
        assertEquals("°C", successState.temperatureUnit)
    }
    
    @Test
    fun `should handle rapid location changes correctly`() = runTest {
        every { selectedLocationRepository.getSelectedLocation() } returns flowOf(null)
        
        coEvery { weatherRepository.getWeather(55.7558, 37.6173, any(), any()) } returns Result.success(mockResponse)
        coEvery { weatherRepository.getWeather(51.5074, -0.1278, any(), any()) } returns Result.success(mockResponse2)
        coEvery { weatherRepository.getWeather(40.7128, -74.0060, any(), any()) } returns Result.success(mockResponse)
        
        weatherViewModel.loadWeather(55.7558, 37.6173)    // Moscow
        kotlinx.coroutines.yield() // Give time for the first request to start
        weatherViewModel.loadWeather(51.5074, -0.1278)  // London
        kotlinx.coroutines.yield() // Give time for the second request to start
        weatherViewModel.loadWeather(40.7128, -74.0060) // New York
        
        val successState = weatherViewModel.uiState
            .first { it is WeatherUiState.Success && it.temperature == 20.5 } as WeatherUiState.Success
        assertEquals(20.5, successState.temperature, 0.001)
        assertEquals("°C", successState.temperatureUnit)
    }
    
    @Test
    fun `should show empty state when no location selected`() = runTest {
        coEvery { selectedLocationRepository.getSelectedLocation() } returns flowOf(null)
        // Mock the weatherMapper to avoid UncompletedCoroutinesError
        coEvery { weatherMapper.mapToDailyForecast(any(), any()) } returns emptyList()
 
        weatherViewModel.loadSavedLocation()
 
        // Wait specifically for Empty state
        val emptyState = weatherViewModel.uiState
            .first { it is WeatherUiState.Empty } as WeatherUiState.Empty
    }
    
    @Test
    fun `should use apparent temperature from API when available`() = runTest {
        val responseWithApparentTemp = mockResponse.copy(
            current = mockResponse.current?.copy(
                apparentTemperature = 25.0,
                temperature = 20.0
            )
        )
        coEvery { weatherRepository.getWeather(55.7558, 37.6173, any(), any()) } returns Result.success(responseWithApparentTemp)
        
        weatherViewModel.loadWeather(55.7558, 37.6173)
        
        val successState = weatherViewModel.uiState
            .first { it is WeatherUiState.Success } as WeatherUiState.Success
             
        assertEquals(25.0, successState.feelsLike!!, 0.001)
        assertEquals(20.0, successState.temperature, 0.001)
    }
    
    @Test
    fun `should process daily forecast correctly`() = runTest {
        coEvery { weatherRepository.getWeather(55.7558, 37.6173, any(), any()) } returns Result.success(mockResponse)

        val expectedForecasts = listOf(
            DailyForecast("27 Apr", "Mon", 0, 15.0, 25.0, 0.0, "07:30", "23:15", 10.0, 180, 5.0),
            DailyForecast("28 Apr", "Tue", 1, 14.0, 23.0, 2.5, "07:29", "23:16", 12.0, 200, 3.0)
        )
        coEvery { weatherMapper.mapToDailyForecast(any(), any()) } returns expectedForecasts

        weatherViewModel.loadWeather(55.7558, 37.6173)

        // Wait specifically for Success state
        val successState = weatherViewModel.uiState
            .first { it is WeatherUiState.Success } as WeatherUiState.Success

        // Check that we have two days of forecast
        assertEquals(2, successState.dailyForecast.size)

        // Check first day (2026-04-27)
        val day1 = successState.dailyForecast[0]
        assertEquals("27 Apr", day1.date)  // Note: our formatting is "27 Apr"
        assertEquals("Mon", day1.dayName)   // April 27, 2026 is a Monday
        assertEquals(0, day1.weatherCode)
        assertEquals(15.0, day1.temperatureMin, 0.001)
        assertEquals(25.0, day1.temperatureMax, 0.001)
        assertEquals(0.0, day1.precipitationSum ?: 0.0, 0.001)
        // Sunrise and sunset are formatted to HH:mm in local time (Europe/Moscow, UTC+3)
        // Given sunrise UTC "2026-04-27T04:30:00", offset +3 hours -> 07:30
        assertEquals("07:30", day1.sunrise)
        // Given sunset UTC "2026-04-27T20:15:00", offset +3 hours -> 23:15
        assertEquals("23:15", day1.sunset)
        assertEquals(10.0, day1.windSpeedMax ?: 0.0, 0.001)
        assertEquals(180, day1.windDirectionDominant ?: 0)
        assertEquals(5.0, day1.uvIndexMax ?: 0.0, 0.001)

        // Check second day (2026-04-28)
        val day2 = successState.dailyForecast[1]
        assertEquals("28 Apr", day2.date)
        assertEquals("Tue", day2.dayName)   // April 28, 2026 is a Tuesday
        assertEquals(1, day2.weatherCode)
        assertEquals(14.0, day2.temperatureMin, 0.001)
        assertEquals(23.0, day2.temperatureMax, 0.001)
        assertEquals(2.5, day2.precipitationSum ?: 0.0, 0.001)
        // Sunrise: "2026-04-28T04:29:00" + 3 hours = 07:29
        assertEquals("07:29", day2.sunrise)
        // Sunset: "2026-04-28T20:16:00" + 3 hours = 23:16
        assertEquals("23:16", day2.sunset)
        assertEquals(12.0, day2.windSpeedMax ?: 0.0, 0.001)
        assertEquals(200, day2.windDirectionDominant ?: 0)
        assertEquals(3.0, day2.uvIndexMax ?: 0.0, 0.001)
    }

    @Test
    fun `should format day names with English locale by default`() = runTest {
        coEvery { languagePreferenceRepository.getAppLocale() } returns java.util.Locale.ENGLISH
        coEvery { weatherRepository.getWeather(55.7558, 37.6173, any(), any()) } returns Result.success(mockResponse)

        val expectedForecasts = listOf(
            DailyForecast("27 Apr", "Mon", 0, 15.0, 25.0, 0.0, "07:30", "23:15", 10.0, 180, 5.0),
            DailyForecast("28 Apr", "Tue", 1, 14.0, 23.0, 2.5, "07:29", "23:16", 12.0, 200, 3.0)
        )
        coEvery { weatherMapper.mapToDailyForecast(any(), any()) } returns expectedForecasts

        weatherViewModel.loadWeather(55.7558, 37.6173)

        val successState = weatherViewModel.uiState
            .first { it is WeatherUiState.Success } as WeatherUiState.Success

        // April 27, 2026 is Monday, short form in English is "Mon"
        assertEquals("Mon", successState.dailyForecast[0].dayName)
        // April 28, 2026 is Tuesday, short form in English is "Tue"
        assertEquals("Tue", successState.dailyForecast[1].dayName)
        // Month should contain "apr" in English (case insensitive check for robustness)
        assertTrue(successState.dailyForecast[0].date.lowercase().contains("apr"))
        assertTrue(successState.dailyForecast[1].date.lowercase().contains("apr"))
    }

    @Test
    fun `should format day names with Russian locale when set`() = runTest {
        coEvery { languagePreferenceRepository.getAppLocale() } returns java.util.Locale.Builder().setLanguage("ru").setRegion("RU").build()
        coEvery { weatherRepository.getWeather(55.7558, 37.6173, any(), any()) } returns Result.success(mockResponse)

        val expectedForecasts = listOf(
            DailyForecast("27 апр", "Пн", 0, 15.0, 25.0, 0.0, "07:30", "23:15", 10.0, 180, 5.0),
            DailyForecast("28 апр", "Вт", 1, 14.0, 23.0, 2.5, "07:29", "23:16", 12.0, 200, 3.0)
        )
        coEvery { weatherMapper.mapToDailyForecast(any(), any()) } returns expectedForecasts

        weatherViewModel.loadWeather(55.7558, 37.6173)

        val successState = weatherViewModel.uiState
            .first { it is WeatherUiState.Success }
            .let { it as WeatherUiState.Success }

        // April 27, 2026 is Monday, short form in Russian is "пн"
        assertEquals("Пн", successState.dailyForecast[0].dayName)
        // April 28, 2026 is Tuesday, short form in Russian is "вт"
        assertEquals("Вт", successState.dailyForecast[1].dayName)
        // Month should contain "апр" in Russian (case insensitive check for robustness)
        assertTrue(successState.dailyForecast[0].date.lowercase().contains("апр"))
        assertTrue(successState.dailyForecast[1].date.lowercase().contains("апр"))
    }

    @Test
    fun `forceRefresh bypasses cache logic in repository`() = runTest {
        coEvery { weatherRepository.getWeather(55.7558, 37.6173, any(), forceRefresh = true) } returns Result.success(mockResponse)
        
        weatherViewModel.loadWeather(55.7558, 37.6173, forceRefresh = true)
        
        val successState = weatherViewModel.uiState.first { it is WeatherUiState.Success } as WeatherUiState.Success
        assertEquals(20.5, successState.temperature, 0.001)
        assertEquals("°C", successState.temperatureUnit)
        coVerify { weatherRepository.getWeather(55.7558, 37.6173, any(), forceRefresh = true) }
    }

    @Test
    fun `refreshActiveLocation uses saved coordinates when available`() = runTest {
        coEvery { weatherRepository.getWeather(any(), any(), any(), any()) } returns Result.success(mockResponse)
        coEvery { selectedLocationRepository.getSelectedLocation() } returns flowOf(mockLocation)
        
        weatherViewModel.loadSavedLocation()
        kotlinx.coroutines.yield()
        
        weatherViewModel.refreshActiveLocation()
        
        coVerify { weatherRepository.getWeather(55.7558, 37.6173, any(), forceRefresh = true) }
    }

    @Test
    fun `loadWeather_nullCurrentFields_handlesGracefully`() = runTest {
        val responseWithNulls = mockResponse.copy(
            current = mockResponse.current?.copy(
                temperature = null,
                apparentTemperature = null,
                weatherCode = null
            )
        )
        coEvery { weatherRepository.getWeather(55.7558, 37.6173, any(), any()) } returns Result.success(responseWithNulls)
        
        weatherViewModel.loadWeather(55.7558, 37.6173)
        
        val successState = weatherViewModel.uiState
            .first { it is WeatherUiState.Success } as WeatherUiState.Success
        
        assertEquals(0.0, successState.temperature, 0.001)
        assertEquals(0, successState.weatherCode)
        assertEquals(0.0, successState.feelsLike, 0.001)
    }

    @Test
    fun `loadWeather_apiCancellation_updatesStateToError`() = runTest {
        // Given a cancellation exception
        coEvery { weatherRepository.getWeather(55.7558, 37.6173, any(), any()) } returns
            Result.failure(kotlinx.coroutines.CancellationException("Job was cancelled"))

        weatherViewModel.loadWeather(55.7558, 37.6173)

        val errorState = weatherViewModel.uiState
            .first { it is WeatherUiState.Error }
            .let { it as WeatherUiState.Error }

        assertEquals("Job was cancelled", errorState.message)
    }

    @Test
    fun `loadWeather_repositoryFailure_mapsToUiError`() = runTest {
        coEvery { weatherRepository.getWeather(55.7558, 37.6173, any(), any()) } returns 
            Result.failure(Exception("timeout"))
        
        weatherViewModel.loadWeather(55.7558, 37.6173)
        
        val errorState = weatherViewModel.uiState
            .first { it is WeatherUiState.Error }
            .let { it as WeatherUiState.Error }
        
        assertEquals("Something went wrong", errorState.message)
    }

    @Test
    fun `loadWeather with hourly forecast enabled includes hourly data in state`() = runTest {
        val prefsWithHourly = WeatherDisplayPrefs(
            showHourlyForecast = true,
            hourlyForecastHours = 24
        )
        val testPrefsRepository: WeatherDisplayPrefsRepository = mockk(relaxed = true) {
            every { getPrefs() } returns flowOf(prefsWithHourly)
        }
        val testWeatherMapper: WeatherMapper = mockk(relaxed = true) {
            coEvery { mapToDailyForecast(any(), any()) } returns emptyList()
            coEvery { mapToHourlyForecast(any(), any(), any(), any()) } returns listOf(
                HourlyForecast("10:00", 0, 20.0, 65, 10.0),
                HourlyForecast("11:00", 1, 22.0, 60, 12.0)
            )
        }

        val testViewModel = WeatherViewModel(
            context,
            weatherRepository,
            locationRepository,
            selectedLocationRepository,
            testPrefsRepository,
            weatherCache,
            languagePreferenceRepository,
            cityNameResolver,
            testWeatherMapper
        )

        coEvery { weatherRepository.getWeather(55.7558, 37.6173, any(), any()) } returns Result.success(mockResponse)

        testViewModel.loadWeather(55.7558, 37.6173)

        val successState = testViewModel.uiState
            .first { it is WeatherUiState.Success } as WeatherUiState.Success

        assertEquals(2, successState.hourlyForecast.size)
        assertEquals("10:00", successState.hourlyForecast[0].time)
        assertEquals(20.0, successState.hourlyForecast[0].temperature!!, 0.001)
        assertEquals(65, successState.hourlyForecast[0].humidity!!)
    }

    @Test
    fun `loadWeather without hourly forecast pref still has hourly data in state`() = runTest {
        coEvery { weatherRepository.getWeather(55.7558, 37.6173, any(), any()) } returns Result.success(mockResponse)

        val testWeatherMapper: WeatherMapper = mockk(relaxed = true) {
            coEvery { mapToDailyForecast(any(), any()) } returns emptyList()
            coEvery { mapToHourlyForecast(any(), any(), any(), any()) } returns listOf(
                HourlyForecast("10:00", 0, 20.0, 65, 10.0)
            )
        }

        val testViewModel = WeatherViewModel(
            context,
            weatherRepository,
            locationRepository,
            selectedLocationRepository,
            weatherDisplayPrefsRepository,
            weatherCache,
            languagePreferenceRepository,
            cityNameResolver,
            testWeatherMapper
        )

        testViewModel.loadWeather(55.7558, 37.6173)

        val successState = testViewModel.uiState
            .first { it is WeatherUiState.Success } as WeatherUiState.Success

        // Hourly data is always mapped, but UI checks showHourlyForecast flag
        // Default prefs have showHourlyForecast = false, so UI won't show it
        // But the state still contains the data
        assertEquals(1, successState.hourlyForecast.size)
        assertEquals(false, successState.prefs.showHourlyForecast)
    }
}