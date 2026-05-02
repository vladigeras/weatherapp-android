package ru.vladigeras.weatherapp.ui

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import io.mockk.coEvery
import io.mockk.coVerify
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
import ru.vladigeras.weatherapp.data.Location
import ru.vladigeras.weatherapp.network.GeocodingService
import ru.vladigeras.weatherapp.repository.CitySearchCache
import ru.vladigeras.weatherapp.repository.LocationRepository
import ru.vladigeras.weatherapp.repository.SelectedLocationRepository

@OptIn(ExperimentalCoroutinesApi::class)
class LocationSelectionViewModelTest {

    private lateinit var locationRepository: LocationRepository
    private lateinit var geocodingService: GeocodingService
    private lateinit var citySearchCache: CitySearchCache
    private lateinit var selectedLocationRepository: SelectedLocationRepository
    private lateinit var viewModel: LocationSelectionViewModel

    private val mockManualLocation = Location(40.7128, -74.0060, "New York", isAutoDetected = false)
    private val mockAutoLocation = Location(55.7558, 37.6173, "Moscow", isAutoDetected = true)

    @Before
    fun setup() {
        locationRepository = mockk(relaxed = true)
        geocodingService = mockk(relaxed = true)
        citySearchCache = mockk(relaxed = true)
        selectedLocationRepository = mockk(relaxed = true)

        coEvery { selectedLocationRepository.getSelectedLocation() } returns flowOf(mockManualLocation)
        coEvery { selectedLocationRepository.clearSelectedLocation() } returns Unit
        coEvery { locationRepository.getLocation() } returns Result.success(mockAutoLocation)

        viewModel = LocationSelectionViewModel(
            locationRepository = locationRepository,
            geocodingService = geocodingService,
            citySearchCache = citySearchCache,
            selectedLocationRepository = selectedLocationRepository
        )

        Dispatchers.setMain(Dispatchers.Unconfined)
        ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) = runnable.run()
            override fun executeOnMainThread(runnable: Runnable) = runnable.run()
            override fun postToMainThread(runnable: Runnable) = runnable.run()
            override fun isMainThread(): Boolean = true
        })
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        ArchTaskExecutor.getInstance().setDelegate(null)
    }

    @Test
    fun `initial state shows manual mode when manual location is saved`() = runTest {
        val state = viewModel.uiState.first { it.isManualMode }
        assertEquals(mockManualLocation, state.activeLocation)
    }

    @Test
    fun `updateSearchQuery updates query state`() = runTest {
        viewModel.updateSearchQuery("Moscow")

        val query = viewModel.searchQuery.first()
        assertEquals("Moscow", query)
    }

    @Test
    fun `selectLocation saves manual location and switches to manual mode`() = runTest {
        val newLocation = Location(60.0, 30.0, "Saint Petersburg", isAutoDetected = false)
        
        coEvery { selectedLocationRepository.saveSelectedLocation(any()) } returns Unit
        
        viewModel.selectLocation(newLocation)
        
        // Wait for state to update to manual mode with new location
        viewModel.uiState
            .first { it.isManualMode && it.activeLocation?.name == "Saint Petersburg" }
        
        coVerify { selectedLocationRepository.saveSelectedLocation(match { it.name == "Saint Petersburg" && !it.isAutoDetected }) }
        
        // Get final state for assertions
        val finalState = viewModel.uiState.first()
        assertTrue(finalState.isManualMode)
        assertEquals("Saint Petersburg", finalState.activeLocation?.name)
    }

    @Test
    fun `useAutoLocation switches to auto mode and saves auto location`() = runTest {
        viewModel.useAutoLocation()
        
        // Wait for mode switch to auto
        viewModel.uiState
            .first { !it.isManualMode && it.activeLocation == mockAutoLocation }
        
        coVerify { selectedLocationRepository.saveSelectedLocation(mockAutoLocation) }
        
        // Get final state for assertions
        val finalState = viewModel.uiState.first()
        assertTrue(!finalState.isManualMode)
        assertEquals(mockAutoLocation, finalState.activeLocation)
    }

    @Test
    fun `selectLocation clears search results`() = runTest {
        val newLocation = Location(60.0, 30.0, "Saint Petersburg", isAutoDetected = false)

        viewModel.selectLocation(newLocation)

        // Wait for search results to be cleared
        viewModel.uiState
            .first { it.searchResults.isEmpty() }

        // Get final state for assertions
        val finalState = viewModel.uiState.first()
        assertTrue(finalState.searchResults.isEmpty())
    }

}

    