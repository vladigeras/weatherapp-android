package ru.vladigeras.weatherapp.ui

import android.content.Context
import android.location.Address
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import ru.vladigeras.weatherapp.BuildConfig
import ru.vladigeras.weatherapp.data.*
import ru.vladigeras.weatherapp.domain.mapper.WeatherMapper
import ru.vladigeras.weatherapp.network.*
import ru.vladigeras.weatherapp.repository.*
import ru.vladigeras.weatherapp.widget.WidgetPrefsManager
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ProviderSwitchingTest {
    @Test
    fun `switching isolates HTTP and caches and survives failure retry and restart`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val clients = mutableListOf<HttpClient>()
        val viewModelJobs = mutableListOf<Job>()
        val originalZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Moscow"))
        try {
            val context: Context = RuntimeEnvironment.getApplication()
            val json = Json { ignoreUnknownKeys = true }
            val requests = mutableListOf<String>()
            var wttrFailed = false
            var sevenTimerFailed = false
            val clock = Clock.fixed(Instant.parse("2026-10-03T17:25:00Z"), ZoneOffset.UTC)
            val openMeteo = WeatherResponse(55.7, 37.6, 0.1, 10800, "Europe/Moscow", elevation = 100.0,
                current = Current(temperature = 18.0, apparentTemperature = 17.0, weatherCode = 2, isDay = 1),
                hourly = HourlyWeather(listOf("2026-10-03T21:00"), listOf(19.0)),
                daily = DailyWeather(listOf("2026-10-03"), temperature2mMin = listOf(10.0), temperature2mMax = listOf(20.0),
                    precipitationSum = listOf(2.0), uvIndexMax = listOf(3.0)))
            val client = HttpClient(MockEngine { request ->
                when (request.url.encodedPath) {
                    Url(BuildConfig.API_URL).encodedPath -> {
                        requests += "open-weather"
                        respond(json.encodeToString(openMeteo), headers = headersOf(HttpHeaders.ContentType, "application/json"))
                    }
                    Url(BuildConfig.SEVEN_TIMER_API_URL).encodedPath -> {
                        assertEquals(Url(BuildConfig.SEVEN_TIMER_API_URL).host, request.url.host)
                        assertEquals("40.7128", request.url.parameters["lat"])
                        assertEquals("-74.006", request.url.parameters["lon"])
                        requests += "7timer"
                        if (sevenTimerFailed) respond("Unavailable", HttpStatusCode.ServiceUnavailable)
                        else respond(SevenTimerWeatherProviderTest.fixture())
                    }
                    else -> {
                        assertEquals(Url(BuildConfig.WTTR_API_URL).host, request.url.host)
                        val format = request.url.parameters["format"]!!
                        assertNull(request.url.parameters["lang"])
                        requests += "wttr-$format-${request.url.segments.last()}"
                        if (wttrFailed) respond("Unavailable", HttpStatusCode.ServiceUnavailable)
                        else respond(if (format == "%Z") {
                            if (request.url.segments.last().contains("-74")) "America/New_York" else "Europe/Moscow"
                        } else WttrWeatherProviderTest.fixture(format == "j1"), headers = headersOf(HttpHeaders.ContentType, "text/plain"))
                    }
                }
            }.apply { config.dispatcher = StandardTestDispatcher(testScheduler) }) { install(ContentNegotiation) { json(json) } }.also(clients::add)
            val prefsRepository = WeatherDisplayPrefsRepository(context)
            val selected = SelectedLocationRepositoryImpl(context)
            var locale = Locale.ENGLISH
            val language = mockk<LanguagePreferenceRepository> {
                coEvery { getAppLocale() } coAnswers { locale }
                coEvery { getEffectiveLocaleCode() } coAnswers { locale.toLanguageTag() }
            }
            val resolver = mockk<CityNameResolver> { coEvery { resolveCityName(any(), any(), any(), any()) } coAnswers { thirdArg<String?>() ?: "Unknown" } }
            fun repository() = WeatherRepositoryImpl(WeatherProviders(listOf(
                OpenMeteoWeatherProvider(WeatherApiServiceImpl(client), WeatherParamsBuilder()),
                WttrWeatherProvider(client, json), SevenTimerWeatherProvider(client, json, clock))), WeatherCache(context))
            fun viewModel(repo: WeatherRepository) = WeatherViewModel(context, repo, mockk(), selected, prefsRepository, resolver,
                WeatherMapper(language, clock))
                .also { viewModelJobs.add(it.viewModelScope.coroutineContext[Job]!!) }
            var geocoderCalls = 0
            val geocoder = mockk<AndroidGeocoder> {
                coEvery { getFromLocationName("Moscow", 5, any()) } coAnswers {
                    geocoderCalls++
                    listOf(Address(thirdArg<Locale>()).apply {
                        locality = if (locale.language == "ru") "Москва" else "Moscow"
                        latitude = 55.7
                        longitude = 37.6
                    })
                }
            }
            val cities = CitySearchRepository(geocoder, CitySearchCache())
            val locations = mockk<LocationRepository> {
                coEvery { hasLocationPermission() } returns false
                coEvery { getLocation(any()) } returns Result.failure(SecurityException())
            }
            val cityVm = LocationSelectionViewModel(context, SavedStateHandle(), locations, cities, selected, language)
                .also { viewModelJobs.add(it.viewModelScope.coroutineContext[Job]!!) }
            val repo = repository()
            val original = WeatherDisplayPrefs()
            prefsRepository.updatePrefs(original)
            selected.saveSelectedLocation(Location(55.7, 37.6, "Moscow"))
            var vm = viewModel(repo)
            suspend fun success(provider: WeatherProviderId) = vm.uiState.first {
                it is WeatherUiState.Success && it.prefs.provider == provider
            } as WeatherUiState.Success
            vm.loadSavedLocation()
            assertEquals(18.0, success(WeatherProviderId.OPEN_METEO).temperature!!, 0.001)
            val weatherCalls = requests.size
            cityVm.updateSearchQuery("Moscow")
            advanceUntilIdle()
            assertEquals(0, geocoderCalls)
            cityVm.submitSearch()
            advanceUntilIdle()
            assertEquals("Moscow", cityVm.uiState.value.searchResults.single().name)
            assertEquals(1, geocoderCalls)
            assertEquals(weatherCalls, requests.size)
            val openCalls = requests.count { it.startsWith("open-") }

            prefsRepository.updatePrefs(original.copy(provider = WeatherProviderId.WTTR))
            val wttr = success(WeatherProviderId.WTTR)
            assertEquals(12.0, wttr.temperature!!, 0.001)
            assertEquals(3, wttr.dailyForecast.size)
            assertEquals(listOf("21:00", "00:00", "03:00", "06:00"), wttr.hourlyForecast.map { it.time })
            assertFalse(wttr.prefs.showUvIndex)
            assertFalse(wttr.prefs.showPrecipitation)
            assertNull(wttr.isDay)
            assertEquals("12°C", WidgetPrefsManager.getTemperature(context))
            assertEquals(WeatherProviderId.WTTR, WidgetPrefsManager.getProvider(context))
            assertEquals("Moscow", cityVm.uiState.value.searchResults.single().name)
            assertEquals(1, geocoderCalls)
            val cachedCalls = requests.size
            cityVm.submitSearch()
            advanceUntilIdle()
            assertEquals(1, geocoderCalls)
            locale = Locale.forLanguageTag("ru")
            cityVm.submitSearch()
            advanceUntilIdle()
            assertEquals("Москва", cityVm.uiState.value.searchResults.single().name)
            assertEquals(2, geocoderCalls)
            cityVm.submitSearch()
            advanceUntilIdle()
            assertEquals(2, geocoderCalls)
            locale = Locale.ENGLISH
            assertEquals(cachedCalls, requests.size)
            assertEquals(openCalls, requests.count { it.startsWith("open-") })

            val beforeReturning = requests.size
            prefsRepository.updatePrefs(original)
            val restored = success(WeatherProviderId.OPEN_METEO)
            assertEquals(18.0, restored.temperature!!, 0.001)
            assertTrue(restored.prefs.showUvIndex)
            assertEquals(7, restored.prefs.forecastDays)
            assertEquals(beforeReturning, requests.size)

            wttrFailed = true
            val nextPrefs = original.copy(provider = WeatherProviderId.WTTR, hourlyForecastHours = 24)
            prefsRepository.updatePrefs(nextPrefs)
            vm.uiState.first { it is WeatherUiState.Error }
            assertFalse(WidgetPrefsManager.hasData(context))
            val beforeRetry = requests.size
            vm.refreshActiveLocation()
            vm.uiState.first { it is WeatherUiState.Error }
            assertEquals(beforeRetry + 2, requests.size)
            assertEquals(openCalls, requests.count { it.startsWith("open-") })
            wttrFailed = false
            vm.refreshActiveLocation()
            assertEquals(8, success(WeatherProviderId.WTTR).hourlyForecast.size)
            assertEquals("12°C", WidgetPrefsManager.getTemperature(context))

            locale = Locale.forLanguageTag("ru")
            selected.saveSelectedLocation(Location(40.7128, -74.006, "New York"))
            vm.loadSavedLocation()
            val changed = vm.uiState.first { it is WeatherUiState.Success && it.cityName == "New York" } as WeatherUiState.Success
            assertEquals("America/New_York", changed.timezone)
            assertEquals("15:00", changed.hourlyForecast.first().time)
            assertTrue(changed.dailyForecast.first().date.contains("окт", ignoreCase = true))
            val beforeRestart = requests.size
            vm.viewModelScope.coroutineContext[Job]!!.cancelAndJoin()
            vm = viewModel(repository())
            vm.loadSavedLocation()
            assertEquals("New York", success(WeatherProviderId.WTTR).cityName)
            assertEquals(nextPrefs, prefsRepository.getPrefs().first())
            assertEquals(beforeRestart, requests.size)
            assertEquals(openCalls, requests.count { it.startsWith("open-") })

            val beforeSevenTimer = requests.size
            val sevenTimerPrefs = original.copy(provider = WeatherProviderId.SEVEN_TIMER, forecastDays = 14, hourlyForecastHours = 48)
            prefsRepository.updatePrefs(sevenTimerPrefs)
            val sevenTimer = success(WeatherProviderId.SEVEN_TIMER)
            assertEquals(listOf("7timer"), requests.drop(beforeSevenTimer))
            assertEquals(18.0, sevenTimer.temperature!!, 0.0)
            assertEquals("New York", sevenTimer.cityName)
            assertEquals("Europe/Moscow", sevenTimer.timezone)
            assertEquals(7, sevenTimer.dailyForecast.size)
            assertEquals(16, sevenTimer.hourlyForecast.size)
            assertEquals("21:00", sevenTimer.hourlyForecast.first().time)
            assertEquals(3, sevenTimer.hourlyStepHours)
            assertNull(sevenTimer.feelsLike)
            assertNull(sevenTimer.windSpeed)
            assertFalse(sevenTimer.prefs.showWind)
            assertFalse(sevenTimer.prefs.showSunTimes)
            assertFalse(sevenTimer.prefs.showPrecipitation)
            assertFalse(sevenTimer.prefs.showUvIndex)
            assertEquals(WeatherProviderId.SEVEN_TIMER, WidgetPrefsManager.getProvider(context))
            assertEquals("18°C", WidgetPrefsManager.getTemperature(context))
            assertEquals(2, geocoderCalls)
            sevenTimerFailed = true
            vm.refreshActiveLocation()
            vm.uiState.first { it is WeatherUiState.Error }
            assertEquals(beforeSevenTimer + 2, requests.size)
            sevenTimerFailed = false
            vm.refreshActiveLocation()
            assertEquals(16, success(WeatherProviderId.SEVEN_TIMER).hourlyForecast.size)
            val beforeSevenTimerRestart = requests.size
            vm.viewModelScope.coroutineContext[Job]!!.cancelAndJoin()
            vm = viewModel(repository())
            vm.loadSavedLocation()
            assertEquals("New York", success(WeatherProviderId.SEVEN_TIMER).cityName)
            assertEquals(sevenTimerPrefs, prefsRepository.getPrefs().first())
            assertEquals(beforeSevenTimerRestart, requests.size)
            assertEquals(listOf("7timer", "7timer", "7timer"), requests.drop(beforeSevenTimer))
            prefsRepository.updatePrefs(original)
            val afterSevenTimer = success(WeatherProviderId.OPEN_METEO)
            assertTrue(afterSevenTimer.prefs.showWind)
            assertTrue(afterSevenTimer.prefs.showSunTimes)
            assertTrue(afterSevenTimer.prefs.showPrecipitation)
            assertTrue(afterSevenTimer.prefs.showUvIndex)
        } finally {
            try {
                withContext(NonCancellable) {
                    viewModelJobs.forEach { it.cancel() }
                    viewModelJobs.joinAll()
                    val clientJobs = clients.map { it.coroutineContext[Job]!! }
                    clients.forEach { it.close() }
                    clientJobs.joinAll()
                }
            } finally {
                TimeZone.setDefault(originalZone)
                Dispatchers.resetMain()
            }
        }
    }
}
