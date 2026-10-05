package ru.vladigeras.weatherapp.ui

import android.content.Context
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
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
import ru.vladigeras.weatherapp.data.SearchLocation
import ru.vladigeras.weatherapp.repository.CitySearchRepository
import ru.vladigeras.weatherapp.repository.CitySearchUnavailableException
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
    private lateinit var citySearchRepository: CitySearchRepository
    private lateinit var selectedLocationRepository: SelectedLocationRepository
    private lateinit var languagePreferenceRepository: LanguagePreferenceRepository
    private lateinit var viewModel: LocationSelectionViewModel
    private val testDispatcher = StandardTestDispatcher()
    private val viewModelStore = ViewModelStore()
    private val viewModelJobs = mutableListOf<Job>()

    private val mockManualLocation = Location(40.7128, -74.0060, "New York", isAutoDetected = false)
    private val mockAutoLocation = Location(55.7558, 37.6173, "Moscow", isAutoDetected = true)
    private val context: Context get() = RuntimeEnvironment.getApplication()

    private val testSearchResults = listOf(
        SearchLocation("Moscow", 55.75, 37.62, "Russia", "Moscow City"),
        SearchLocation("Moscow", 41.7, -83.5, "United States", "Ohio")
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        savedStateHandle = SavedStateHandle()

        locationRepository = mockk(relaxed = true)
        citySearchRepository = mockk()
        coEvery { citySearchRepository.searchLocations(any(), any()) } returns Result.success(emptyList())
        selectedLocationRepository = mockk(relaxed = true)
        languagePreferenceRepository = mockk(relaxed = true)

        coEvery { selectedLocationRepository.getSelectedLocation() } returns flowOf(mockManualLocation)
        coEvery { selectedLocationRepository.clearSelectedLocation() } returns Unit
        coEvery { locationRepository.getLocation(any()) } returns Result.success(mockAutoLocation)
        coEvery { locationRepository.hasLocationPermission() } returns true
        coEvery { languagePreferenceRepository.getEffectiveLocaleCode() } returns "en"

        viewModel = LocationSelectionViewModel(
            context = context,
            savedStateHandle = savedStateHandle,
            locationRepository = locationRepository,
            citySearchRepository = citySearchRepository,
            selectedLocationRepository = selectedLocationRepository,
            languagePreferenceRepository = languagePreferenceRepository
        ).tracked()
    }

    @After
    fun tearDown() = runTest(testDispatcher) {
        try {
            viewModelStore.clear()
            viewModelJobs.joinAll()
            assertTrue(viewModelJobs.all { it.isCompleted })
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun LocationSelectionViewModel.tracked(): LocationSelectionViewModel = apply {
        viewModelStore.put("location-${viewModelJobs.size}", this)
        viewModelJobs.add(viewModelScope.coroutineContext[Job]!!)
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
            citySearchRepository = citySearchRepository,
            selectedLocationRepository = selectedLocationRepository,
            languagePreferenceRepository = languagePreferenceRepository
        ).tracked()

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
            citySearchRepository = citySearchRepository,
            selectedLocationRepository = selectedLocationRepository,
            languagePreferenceRepository = languagePreferenceRepository
        ).tracked()

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
    fun `search query shorter than 2 chars clears results and cannot submit`() = runTest {
        coEvery { citySearchRepository.searchLocations("Moscow", "en") } returns Result.success(testSearchResults)
        viewModel.updateSearchQuery("Moscow")
        viewModel.submitSearch()
        advanceUntilIdle()

        assertEquals(testSearchResults, viewModel.uiState.value.searchResults)
        viewModel.updateSearchQuery("M")
        viewModel.submitSearch()
        advanceUntilIdle()
        coVerify(exactly = 0) { citySearchRepository.searchLocations("M", any()) }

        val state = viewModel.uiState.first()
        assertTrue(state.searchResults.isEmpty())
    }

    @Test
    fun `submitted search returns candidates without selecting a city`() = runTest {
        coEvery { citySearchRepository.searchLocations("Moscow", "en") } returns Result.success(
            testSearchResults
        )

        viewModel.updateSearchQuery("Moscow")
        viewModel.submitSearch()
        advanceUntilIdle()

        val state = viewModel.uiState.first { it.searchResults.isNotEmpty() }
        assertEquals(2, state.searchResults.size)
        assertEquals("Moscow", state.searchResults[0].name)

        coVerify { citySearchRepository.searchLocations("Moscow", "en") }
    }

    @Test
    fun `search query failure shows error`() = runTest {
        coEvery { citySearchRepository.searchLocations("Unknown", "en") } returns Result.failure(IOException("Network error"))

        viewModel.updateSearchQuery("Unknown")
        viewModel.submitSearch()
        advanceUntilIdle()

        val state = viewModel.uiState.first { it.searchError != null }
        assertNotNull(state.searchError)
        assertFalse(state.searchCompleted)
        assertFalse(state.searchLoading)
    }

    @Test
    fun `empty search completes without an error`() = runTest {
        coEvery { citySearchRepository.searchLocations("Xyz", "en") } returns Result.success(
            emptyList()
        )

        viewModel.updateSearchQuery("Xyz")
        viewModel.submitSearch()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.searchCompleted)
        assertNull(viewModel.uiState.value.searchError)
    }

    @Test
    fun `search can be retried after a timeout without changing the query`() = runTest {
        coEvery { citySearchRepository.searchLocations("Unknown", "en") } returns Result.failure(java.net.SocketTimeoutException())

        viewModel.updateSearchQuery("Unknown")
        viewModel.submitSearch()
        advanceUntilIdle()

        val errorState = viewModel.uiState.first { it.searchError != null }
        assertEquals(context.getString(ru.vladigeras.weatherapp.R.string.request_timed_out), errorState.searchError)
        assertFalse(errorState.searchCompleted)

        coEvery { citySearchRepository.searchLocations("Unknown", "en") } returns Result.success(testSearchResults)
        viewModel.submitSearch()
        advanceUntilIdle()

        val clearedState = viewModel.uiState.first()
        assertNull(clearedState.searchError)
        assertEquals(testSearchResults, clearedState.searchResults)
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
            citySearchRepository = citySearchRepository,
            selectedLocationRepository = selectedLocationRepository,
            languagePreferenceRepository = languagePreferenceRepository
        ).tracked()
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
    fun `restored query waits for explicit action`() = runTest {
        val vm = LocationSelectionViewModel(context, SavedStateHandle(mapOf("search_query" to "Moscow")),
            locationRepository, citySearchRepository, selectedLocationRepository, languagePreferenceRepository).tracked()
        advanceUntilIdle()
        coVerify(exactly = 0) { citySearchRepository.searchLocations(any(), any()) }
        vm.submitSearch()
        advanceUntilIdle()
        coVerify(exactly = 1) { citySearchRepository.searchLocations("Moscow", "en") }
    }

    @Test
    fun `typing never searches and submission uses the app language`() = runTest {
        viewModel.updateSearchQuery("Moscow")
        advanceUntilIdle()
        coVerify(exactly = 0) { citySearchRepository.searchLocations(any(), any()) }
        coEvery { languagePreferenceRepository.getEffectiveLocaleCode() } returns "ru"
        coEvery { citySearchRepository.searchLocations("Moscow", "ru") } returns Result.success(testSearchResults)
        viewModel.submitSearch()
        advanceUntilIdle()
        assertEquals(testSearchResults, viewModel.uiState.value.searchResults)
        coVerify(exactly = 1) { citySearchRepository.searchLocations("Moscow", "ru") }
        coVerify(exactly = 0) { selectedLocationRepository.saveSelectedLocation(any()) }
    }

    @Test
    fun `missing system service shows a search error and preserves the saved city`() = runTest {
        coEvery { citySearchRepository.searchLocations(any(), any()) } returns Result.failure(CitySearchUnavailableException())
        viewModel.updateSearchQuery("Moscow")
        viewModel.submitSearch()
        advanceUntilIdle()
        assertEquals(context.getString(ru.vladigeras.weatherapp.R.string.city_search_unavailable), viewModel.uiState.value.searchError)
        assertEquals(mockManualLocation, viewModel.uiState.value.activeLocation)
        assertFalse(viewModel.uiState.value.searchCompleted)
    }

    @Test
    fun `changing query rejects the cancelled late city result`() = runTest {
        advanceUntilIdle()
        val old = kotlinx.coroutines.CompletableDeferred<List<SearchLocation>>()
        coEvery { citySearchRepository.searchLocations("Moscow", "en") } coAnswers {
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) { old.await() }.let { Result.success(it) }
        }
        viewModel.updateSearchQuery("Moscow")
        viewModel.submitSearch()
        runCurrent()
        viewModel.updateSearchQuery("London")
        coEvery { citySearchRepository.searchLocations("London", "en") } returns Result.success(listOf(SearchLocation("London", 51.5, -0.1)))
        viewModel.submitSearch()
        runCurrent()
        old.complete(testSearchResults)
        advanceUntilIdle()
        assertEquals("London", viewModel.uiState.value.searchResults.single().name)
    }

    @Test
    fun `new GPS request rejects the late cancelled result`() = runTest {
        advanceUntilIdle()
        val old = kotlinx.coroutines.CompletableDeferred<Location>()
        try {
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
        } finally {
            old.complete(mockAutoLocation)
        }
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
