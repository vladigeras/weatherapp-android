package ru.vladigeras.weatherapp.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.every
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import ru.vladigeras.weatherapp.data.Location
import ru.vladigeras.weatherapp.network.GeocodingService
import ru.vladigeras.weatherapp.repository.CitySearchCache
import ru.vladigeras.weatherapp.repository.LanguagePreferenceRepository
import ru.vladigeras.weatherapp.repository.LocationRepository
import ru.vladigeras.weatherapp.repository.SelectedLocationRepository
import org.junit.runner.RunWith

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

    @Before
    fun setup() {
        val testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        savedStateHandle = SavedStateHandle()

        locationRepository = mockk(relaxed = true)
        geocodingService = mockk(relaxed = true)
        citySearchCache = mockk(relaxed = true)
        selectedLocationRepository = mockk(relaxed = true)
        languagePreferenceRepository = mockk(relaxed = true)

        coEvery { selectedLocationRepository.getSelectedLocation() } returns flowOf(mockManualLocation)
        coEvery { selectedLocationRepository.clearSelectedLocation() } returns Unit
        coEvery { locationRepository.getLocation() } returns Result.success(mockAutoLocation)
        coEvery { languagePreferenceRepository.getEffectiveLocaleCode() } returns "en"

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
    }

    @Test
    fun `updateSearchQuery updates query state`() = runTest {
        viewModel.updateSearchQuery("Moscow")
        advanceUntilIdle()

        val query = viewModel.searchQuery.first()
        assertEquals("Moscow", query)
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
    fun `useAutoLocation switches to auto mode and saves auto location`() = runTest {
        viewModel.useAutoLocation()
        advanceUntilIdle()

        viewModel.uiState
            .first { !it.isManualMode && it.activeLocation == mockAutoLocation }

        coVerify { selectedLocationRepository.saveSelectedLocation(mockAutoLocation) }

        val finalState = viewModel.uiState.first()
        assertTrue(!finalState.isManualMode)
        assertEquals(mockAutoLocation, finalState.activeLocation)
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

}

    