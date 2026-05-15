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
        coEvery { locationRepository.getLocation() } returns Result.success(mockAutoLocation)
        coEvery { locationRepository.hasLocationPermission() } returns true
        coEvery { languagePreferenceRepository.getEffectiveLocaleCode() } returns "en"
        every { citySearchCache.get(any()) } returns null

        viewModel = LocationSelectionViewModel(
            context = context,
            savedStateHandle = savedStateHandle,
            locationRepository = locationRepository,
            geocodingService = geocodingService,
            citySearchCache = citySearchCache,
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
            geocodingService = geocodingService,
            citySearchCache = citySearchCache,
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
        coEvery { locationRepository.getLocation() } returns Result.failure(SecurityException("No permission"))

        val freshViewModel = LocationSelectionViewModel(
            context = context,
            savedStateHandle = SavedStateHandle(),
            locationRepository = locationRepository,
            geocodingService = geocodingService,
            citySearchCache = citySearchCache,
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
        every { citySearchCache.get("Moscow") } returns testSearchResults

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

        val state = viewModel.uiState.first { it.error != null }
        assertEquals("Search failed", state.error)
    }

    @Test
    fun `search results are cached after successful geocoding`() = runTest {
        coEvery { geocodingService.searchCity("Moscow", "en") } returns Result.success(
            GeocodingResponse(testSearchResults)
        )

        viewModel.updateSearchQuery("Moscow")
        advanceTimeBy(500)
        advanceUntilIdle()

        coVerify { citySearchCache.put("Moscow", testSearchResults) }
    }

    @Test
    fun `empty search results are not cached`() = runTest {
        coEvery { geocodingService.searchCity("Xyz", "en") } returns Result.success(
            GeocodingResponse(emptyList())
        )

        viewModel.updateSearchQuery("Xyz")
        advanceTimeBy(500)
        advanceUntilIdle()

        coVerify(exactly = 0) { citySearchCache.put(any(), any()) }
    }

    @Test
    fun `clearError removes error from state`() = runTest {
        coEvery { geocodingService.searchCity("Unknown", "en") } returns Result.failure(IOException("Network error"))

        viewModel.updateSearchQuery("Unknown")
        advanceTimeBy(500)
        advanceUntilIdle()

        val errorState = viewModel.uiState.first { it.error != null }
        assertNotNull(errorState.error)

        viewModel.clearError()

        val clearedState = viewModel.uiState.first()
        assertNull(clearedState.error)
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
        coEvery { locationRepository.getLocation() } returns Result.failure(SecurityException("No permission"))

        val freshViewModel = LocationSelectionViewModel(
            context = context,
            savedStateHandle = SavedStateHandle(),
            locationRepository = locationRepository,
            geocodingService = geocodingService,
            citySearchCache = citySearchCache,
            selectedLocationRepository = selectedLocationRepository,
            languagePreferenceRepository = languagePreferenceRepository
        )
        advanceUntilIdle()

        freshViewModel.useAutoLocation()
        advanceUntilIdle()

        coVerify(exactly = 0) { selectedLocationRepository.saveSelectedLocation(any()) }

        val state = freshViewModel.uiState.first()
        assertFalse(state.isManualMode)
        assertNull(state.activeLocation)
    }

    @Test
    fun `refreshAutoLocation reloads from repository`() = runTest {
        advanceUntilIdle()

        val newLocation = Location(59.93, 30.32, "Saint Petersburg", isAutoDetected = true)
        coEvery { locationRepository.getLocation() } returns Result.success(newLocation)

        viewModel.refreshAutoLocation()
        advanceUntilIdle()

        coVerify(atLeast = 2) { locationRepository.getLocation() }
    }

    @Test
    fun `refreshLocationPermission updates permission state`() = runTest {
        every { locationRepository.hasLocationPermission() } returns false

        viewModel.refreshLocationPermission()
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertFalse(state.locationPermissionGranted)
    }
}
