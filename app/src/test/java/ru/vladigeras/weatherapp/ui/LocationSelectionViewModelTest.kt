package ru.vladigeras.weatherapp.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import ru.vladigeras.weatherapp.data.Location
import ru.vladigeras.weatherapp.network.GeocodingResponse
import ru.vladigeras.weatherapp.network.GeocodingResult
import ru.vladigeras.weatherapp.network.GeocodingService
import ru.vladigeras.weatherapp.repository.CitySearchCache
import ru.vladigeras.weatherapp.repository.WeatherRepository
import ru.vladigeras.weatherapp.repository.WeatherRepositoryImpl
import ru.vladigeras.weatherapp.repository.WeatherDisplayPrefsRepository
import ru.vladigeras.weatherapp.repository.WeatherParamsBuilder
import ru.vladigeras.weatherapp.network.OpenMeteoWeatherProvider
import ru.vladigeras.weatherapp.network.WeatherProviders
import ru.vladigeras.weatherapp.network.WeatherProvider
import ru.vladigeras.weatherapp.data.ProviderCapabilities
import ru.vladigeras.weatherapp.data.WeatherDisplayPrefs
import ru.vladigeras.weatherapp.data.WeatherProviderId
import ru.vladigeras.weatherapp.util.asSearchLocation
import ru.vladigeras.weatherapp.repository.LanguagePreferenceRepository
import ru.vladigeras.weatherapp.repository.LocationRepository
import ru.vladigeras.weatherapp.repository.SelectedLocationRepository
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
class LocationSelectionViewModelTest {

    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var locationRepository: LocationRepository
    private lateinit var geocodingService: GeocodingService
    private lateinit var citySearchCache: CitySearchCache
    private lateinit var selectedLocationRepository: SelectedLocationRepository
    private lateinit var languagePreferenceRepository: LanguagePreferenceRepository
    private val prefs = kotlinx.coroutines.flow.MutableStateFlow(WeatherDisplayPrefs())
    private lateinit var wttrProvider: WeatherProvider
    private lateinit var weatherRepository: WeatherRepository
    private lateinit var prefsRepository: WeatherDisplayPrefsRepository
    private lateinit var viewModel: LocationSelectionViewModel

    private val mockManualLocation = Location(40.7128, -74.0060, "New York", isAutoDetected = false)
    private val mockAutoLocation = Location(55.7558, 37.6173, "Moscow", isAutoDetected = true)
    private val context: Context get() = RuntimeEnvironment.getApplication()

    private val testSearchResults = listOf(
        GeocodingResult(1, "Moscow", 55.75, 37.62, "Russia", "RU", "Moscow City"),
        GeocodingResult(2, "Moscow", 41.7, -83.5, "United States", "US", "Ohio")
    )

    @Before
    fun setup() {
        val testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        savedStateHandle = SavedStateHandle()

        locationRepository = mockk(relaxed = true)
        geocodingService = mockk()
        coEvery { geocodingService.searchCity(any(), any()) } returns Result.success(GeocodingResponse(emptyList()))
        citySearchCache = mockk(relaxed = true)
        selectedLocationRepository = mockk(relaxed = true)
        languagePreferenceRepository = mockk(relaxed = true)

        coEvery { selectedLocationRepository.getSelectedLocation() } returns flowOf(mockManualLocation)
        coEvery { selectedLocationRepository.clearSelectedLocation() } returns Unit
        coEvery { locationRepository.getLocation(any()) } returns Result.success(mockAutoLocation)
        coEvery { locationRepository.hasLocationPermission() } returns true
        coEvery { languagePreferenceRepository.getEffectiveLocaleCode() } returns "en"
        every { citySearchCache.get(any(), any(), any()) } returns null
        prefs.value = WeatherDisplayPrefs()
        prefsRepository = mockk { every { getPrefs() } returns prefs }
        wttrProvider = mockk {
            every { id } returns WeatherProviderId.WTTR
            every { capabilities } returns ProviderCapabilities(3, 3, true, false, false, false, false)
            coEvery { searchLocations(any(), any()) } returns emptyList()
        }
        weatherRepository = WeatherRepositoryImpl(WeatherProviders(listOf(OpenMeteoWeatherProvider(mockk(), geocodingService, WeatherParamsBuilder()), wttrProvider)), mockk(), citySearchCache)

        viewModel = LocationSelectionViewModel(
            context = context,
            savedStateHandle = savedStateHandle,
            locationRepository = locationRepository,
            weatherRepository = weatherRepository,
            prefsRepository = prefsRepository,
            selectedLocationRepository = selectedLocationRepository,
            languagePreferenceRepository = languagePreferenceRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state shows manual mode when manual location is saved`() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.first { it.isManualMode }
        assertEquals(mockManualLocation, state.activeLocation)
        assertFalse(state.isLoading)
    }

    @Test
    fun `initial state shows auto mode when no saved location`() = runTest {
        coEvery { selectedLocationRepository.getSelectedLocation() } returns flowOf(null)

        val freshViewModel = LocationSelectionViewModel(
            context = context,
            savedStateHandle = SavedStateHandle(),
            locationRepository = locationRepository,
            weatherRepository = weatherRepository,
            prefsRepository = prefsRepository,
            selectedLocationRepository = selectedLocationRepository,
            languagePreferenceRepository = languagePreferenceRepository
        )

        advanceUntilIdle()

        val state = freshViewModel.uiState.first()
        assertFalse(state.isManualMode)
    }

    @Test
    fun `initial state loads auto location and sets permission granted`() = runTest {
        advanceUntilIdle()

        val state = viewModel.uiState.first { it.autoLocation != null }
        assertNotNull(state.autoLocation)
        assertTrue(state.autoLocation!!.isAutoDetected)
        assertTrue(state.locationPermissionGranted)
    }

    @Test
    fun `auto location failure sets error state`() = runTest {
        every { locationRepository.hasLocationPermission() } returns false
        coEvery { locationRepository.getLocation(any()) } returns Result.failure(SecurityException("No permission"))

        val freshViewModel = LocationSelectionViewModel(
            context = context,
            savedStateHandle = SavedStateHandle(),
            locationRepository = locationRepository,
            weatherRepository = weatherRepository,
            prefsRepository = prefsRepository,
            selectedLocationRepository = selectedLocationRepository,
            languagePreferenceRepository = languagePreferenceRepository
        )

        advanceUntilIdle()

        val state = freshViewModel.uiState.first { it.error != null }
        assertNotNull(state.error)
        assertFalse(state.autoLocationLoading)
        assertFalse(state.locationPermissionGranted)
    }

    @Test
    fun `updateSearchQuery updates query state`() = runTest {
        viewModel.updateSearchQuery("Moscow")
        advanceUntilIdle()

        val query = viewModel.searchQuery.first()
        assertEquals("Moscow", query)
    }

    @Test
    fun `search query shorter than 2 chars clears results`() = runTest {
        viewModel.updateSearchQuery("Moscow")
        advanceTimeBy(500)
        advanceUntilIdle()

        viewModel.updateSearchQuery("M")
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertTrue(state.searchResults.isEmpty())
    }

    @Test
    fun `search query triggers geocoding after debounce`() = runTest {
        coEvery { geocodingService.searchCity("Moscow", "en") } returns Result.success(
            GeocodingResponse(testSearchResults)
        )

        viewModel.updateSearchQuery("Moscow")
        advanceTimeBy(500)
        advanceUntilIdle()

        val state = viewModel.uiState.first { it.searchResults.isNotEmpty() }
        assertEquals(2, state.searchResults.size)
        assertEquals("Moscow", state.searchResults[0].name)

        coVerify { geocodingService.searchCity("Moscow", "en") }
    }

    @Test
    fun `search query returns cached results without API call`() = runTest {
        every { citySearchCache.get("Moscow", WeatherProviderId.OPEN_METEO, "en") } returns testSearchResults.map { it.asSearchLocation() }

        viewModel.updateSearchQuery("Moscow")
        advanceTimeBy(500)
        advanceUntilIdle()

        val state = viewModel.uiState.first { it.searchResults.isNotEmpty() }
        assertEquals(2, state.searchResults.size)

        coVerify(exactly = 0) { geocodingService.searchCity(any(), any()) }
    }

    @Test
    fun `search query failure shows error`() = runTest {
        coEvery { geocodingService.searchCity("Unknown", "en") } returns Result.failure(IOException("Network error"))

        viewModel.updateSearchQuery("Unknown")
        advanceTimeBy(500)
        advanceUntilIdle()

        val state = viewModel.uiState.first { it.searchError != null }
        assertNotNull(state.searchError)
        assertFalse(state.searchCompleted)
        assertFalse(state.searchLoading)
    }

    @Test
    fun `search results are cached after successful geocoding`() = runTest {
        coEvery { geocodingService.searchCity("Moscow", "en") } returns Result.success(
            GeocodingResponse(testSearchResults)
        )

        viewModel.updateSearchQuery("Moscow")
        advanceTimeBy(500)
        advanceUntilIdle()

        coVerify { citySearchCache.put("Moscow", testSearchResults.map { it.asSearchLocation() }, WeatherProviderId.OPEN_METEO, "en") }
    }

    @Test
    fun `empty search results are not cached`() = runTest {
        coEvery { geocodingService.searchCity("Xyz", "en") } returns Result.success(
            GeocodingResponse(emptyList())
        )

        viewModel.updateSearchQuery("Xyz")
        advanceTimeBy(500)
        advanceUntilIdle()

        coVerify(exactly = 0) { citySearchCache.put(any(), any(), any(), any()) }
        assertTrue(viewModel.uiState.value.searchCompleted)
        assertNull(viewModel.uiState.value.searchError)
    }

    @Test
    fun `search can be retried after a timeout without changing the query`() = runTest {
        coEvery { geocodingService.searchCity("Unknown", "en") } returns Result.failure(io.ktor.client.plugins.HttpRequestTimeoutException("https://example.com/search", 10_000))

        viewModel.updateSearchQuery("Unknown")
        advanceTimeBy(500)
        advanceUntilIdle()

        val errorState = viewModel.uiState.first { it.searchError != null }
        assertEquals(context.getString(ru.vladigeras.weatherapp.R.string.request_timed_out), errorState.searchError)
        assertFalse(errorState.searchCompleted)

        coEvery { geocodingService.searchCity("Unknown", "en") } returns Result.success(GeocodingResponse(testSearchResults))
        viewModel.submitSearch()
        advanceUntilIdle()

        val clearedState = viewModel.uiState.first()
        assertNull(clearedState.searchError)
        assertEquals(testSearchResults.map { it.asSearchLocation() }, clearedState.searchResults)
    }

    @Test
    fun `selectLocation saves manual location and switches to manual mode`() = runTest {
        val newLocation = Location(60.0, 30.0, "Saint Petersburg", isAutoDetected = false)

        coEvery { selectedLocationRepository.saveSelectedLocation(any()) } returns Unit

        viewModel.selectLocation(newLocation)
        advanceUntilIdle()

        viewModel.uiState
            .first { it.isManualMode && it.activeLocation?.name == "Saint Petersburg" }

        coVerify { selectedLocationRepository.saveSelectedLocation(match { it.name == "Saint Petersburg" && !it.isAutoDetected }) }

        val finalState = viewModel.uiState.first()
        assertTrue(finalState.isManualMode)
        assertEquals("Saint Petersburg", finalState.activeLocation?.name)
    }

    @Test
    fun `selectLocation clears search results`() = runTest {
        val newLocation = Location(60.0, 30.0, "Saint Petersburg", isAutoDetected = false)

        viewModel.selectLocation(newLocation)
        advanceUntilIdle()

        viewModel.uiState
            .first { it.searchResults.isEmpty() }

        val finalState = viewModel.uiState.first()
        assertTrue(finalState.searchResults.isEmpty())
    }

    @Test
    fun `selectLocation resets search query`() = runTest {
        viewModel.updateSearchQuery("Moscow")
        advanceUntilIdle()

        viewModel.selectLocation(Location(60.0, 30.0, "SPb", isAutoDetected = false))
        advanceUntilIdle()

        val query = viewModel.searchQuery.first()
        assertEquals("", query)
    }

    @Test
    fun `useAutoLocation switches to auto mode and saves auto location`() = runTest {
        viewModel.useAutoLocation()
        advanceUntilIdle()

        viewModel.uiState
            .first { !it.isManualMode && it.activeLocation == mockAutoLocation }

        coVerify { selectedLocationRepository.saveSelectedLocation(mockAutoLocation) }

        val finalState = viewModel.uiState.first()
        assertFalse(finalState.isManualMode)
        assertEquals(mockAutoLocation, finalState.activeLocation)
    }

    @Test
    fun `useAutoLocation when auto location is null does not save`() = runTest {
        coEvery { locationRepository.getLocation(any()) } returns Result.failure(SecurityException("No permission"))

        val freshViewModel = LocationSelectionViewModel(
            context = context,
            savedStateHandle = SavedStateHandle(),
            locationRepository = locationRepository,
            weatherRepository = weatherRepository,
            prefsRepository = prefsRepository,
            selectedLocationRepository = selectedLocationRepository,
            languagePreferenceRepository = languagePreferenceRepository
        )
        advanceUntilIdle()

        freshViewModel.useAutoLocation()
        advanceUntilIdle()

        coVerify(exactly = 0) { selectedLocationRepository.saveSelectedLocation(any()) }

        val state = freshViewModel.uiState.first()
        assertTrue(state.isManualMode)
        assertEquals(mockManualLocation, state.activeLocation)
    }

    @Test
    fun `granting permission after manual choice obtains and saves auto location`() = runTest {
        coEvery { locationRepository.getLocation(any()) } returns Result.failure(SecurityException("No permission"))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isManualMode)
        coEvery { locationRepository.getLocation(any()) } returns Result.success(mockAutoLocation)
        var completed = false
        viewModel.useAutoLocation { completed = true }
        advanceUntilIdle()
        assertTrue(completed)
        assertEquals(mockAutoLocation, viewModel.uiState.value.activeLocation)
        coVerify { selectedLocationRepository.saveSelectedLocation(mockAutoLocation) }
    }

    @Test
    fun `refreshAutoLocation reloads from repository`() = runTest {
        advanceUntilIdle()

        val newLocation = Location(59.93, 30.32, "Saint Petersburg", isAutoDetected = true)
        coEvery { locationRepository.getLocation(any()) } returns Result.success(newLocation)

        viewModel.refreshAutoLocation()
        advanceUntilIdle()

        coVerify { locationRepository.getLocation(true) }
        coVerify { selectedLocationRepository.saveSelectedLocation(newLocation) }
        assertEquals(newLocation, viewModel.uiState.value.activeLocation)
        assertFalse(viewModel.uiState.value.isManualMode)
    }

    @Test
    fun `refreshLocationPermission updates permission state`() = runTest {
        every { locationRepository.hasLocationPermission() } returns false

        viewModel.refreshLocationPermission()
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertFalse(state.locationPermissionGranted)
    }
    @Test
    fun `restored query waits for saved provider before any search`() = runTest {
        val loadedPrefs = kotlinx.coroutines.CompletableDeferred<WeatherDisplayPrefs>()
        every { prefsRepository.getPrefs() } returns kotlinx.coroutines.flow.flow { emit(loadedPrefs.await()) }
        val vm = LocationSelectionViewModel(context, SavedStateHandle(mapOf("search_query" to "Moscow")),
            locationRepository, weatherRepository, prefsRepository, selectedLocationRepository, languagePreferenceRepository)
        advanceTimeBy(301)
        runCurrent()
        vm.submitSearch()
        coVerify(exactly = 0) { geocodingService.searchCity(any(), any()) }
        coVerify(exactly = 0) { wttrProvider.searchLocations(any(), any()) }
        loadedPrefs.complete(WeatherDisplayPrefs(provider = WeatherProviderId.WTTR))
        advanceUntilIdle()
        assertTrue(vm.uiState.value.explicitSearch)
        coVerify(exactly = 0) { wttrProvider.searchLocations(any(), any()) }
        vm.submitSearch()
        advanceUntilIdle()
        coVerify(exactly = 1) { wttrProvider.searchLocations("Moscow", "en") }
        coVerify(exactly = 0) { geocodingService.searchCity(any(), any()) }
    }

    @Test
    fun `wttr search requires submission and selecting candidate before saving`() = runTest {
        advanceUntilIdle()
        prefs.value = prefs.value.copy(provider = WeatherProviderId.WTTR)
        advanceUntilIdle()
        val candidate = testSearchResults.first().asSearchLocation()
        coEvery { wttrProvider.searchLocations("Moscow", "en") } returns listOf(candidate)
        viewModel.updateSearchQuery("Moscow")
        advanceTimeBy(500)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.explicitSearch)
        coVerify(exactly = 0) { wttrProvider.searchLocations(any(), any()) }
        viewModel.submitSearch()
        advanceUntilIdle()
        assertEquals(listOf(candidate), viewModel.uiState.value.searchResults)
        coVerify(exactly = 0) { selectedLocationRepository.saveSelectedLocation(any()) }
        coVerify(exactly = 0) { geocodingService.searchCity(any(), any()) }
        viewModel.selectLocation(Location(candidate.latitude, candidate.longitude, candidate.name))
        advanceUntilIdle()
        coVerify { selectedLocationRepository.saveSelectedLocation(match { it.latitude == candidate.latitude && it.longitude == candidate.longitude }) }
    }

    @Test
    fun `provider change rejects late old city result`() = runTest {
        advanceUntilIdle()
        val old = kotlinx.coroutines.CompletableDeferred<GeocodingResponse>()
        coEvery { geocodingService.searchCity("Moscow", "en") } coAnswers {
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) { old.await() }.let { Result.success(it) }
        }
        viewModel.updateSearchQuery("Moscow")
        advanceTimeBy(301)
        runCurrent()
        prefs.value = prefs.value.copy(provider = WeatherProviderId.WTTR)
        runCurrent()
        val candidate = testSearchResults.first().asSearchLocation()
        coEvery { wttrProvider.searchLocations("Moscow", "en") } returns listOf(candidate)
        viewModel.submitSearch()
        runCurrent()
        old.complete(GeocodingResponse(testSearchResults))
        advanceUntilIdle()
        assertEquals(WeatherProviderId.WTTR, viewModel.uiState.value.provider)
        assertEquals(listOf(candidate), viewModel.uiState.value.searchResults)
    }

    @Test
    fun `new GPS request rejects the late cancelled result`() = runTest {
        advanceUntilIdle()
        val old = kotlinx.coroutines.CompletableDeferred<Location>()
        coEvery { locationRepository.getLocation(any()) } coAnswers {
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) { old.await() }.let { Result.success(it) }
        }
        viewModel.refreshAutoLocation()
        runCurrent()
        val latest = Location(59.93, 30.32, "Saint Petersburg", true)
        coEvery { locationRepository.getLocation(any()) } returns Result.success(latest)
        viewModel.refreshAutoLocation()
        runCurrent()
        old.complete(mockAutoLocation)
        advanceUntilIdle()
        assertEquals(latest, viewModel.uiState.value.autoLocation)
    }

    @Test
    fun `GPS refresh finishes only after persisting and retains old place on failure`() = runTest {
        advanceUntilIdle()
        val persisted = kotlinx.coroutines.CompletableDeferred<Unit>()
        coEvery { selectedLocationRepository.saveSelectedLocation(any()) } coAnswers { persisted.await() }
        var completed = false
        viewModel.refreshAutoLocation { completed = true }
        runCurrent()
        assertFalse(completed)
        persisted.complete(Unit)
        advanceUntilIdle()
        assertTrue(completed)
        coEvery { locationRepository.getLocation(true) } returns Result.failure(Exception("GPS unavailable"))
        completed = false
        viewModel.refreshAutoLocation { completed = true }
        advanceUntilIdle()
        assertFalse(completed)
        assertEquals(mockAutoLocation, viewModel.uiState.value.activeLocation)
        coVerify(exactly = 1) { selectedLocationRepository.saveSelectedLocation(any()) }
    }

}
