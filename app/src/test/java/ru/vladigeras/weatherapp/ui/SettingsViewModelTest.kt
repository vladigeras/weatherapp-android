package ru.vladigeras.weatherapp.ui

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import ru.vladigeras.weatherapp.data.WeatherDisplayPrefs
import ru.vladigeras.weatherapp.repository.WeatherDisplayPrefsRepository

@ExperimentalCoroutinesApi
class SettingsViewModelTest {
    private lateinit var prefsRepository: WeatherDisplayPrefsRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        prefsRepository = mockk(relaxed = true) {
            every { getPrefs() } returns flowOf(WeatherDisplayPrefs())
        }
        viewModel = SettingsViewModel(prefsRepository)
        Dispatchers.setMain(Dispatchers.Unconfined)
        ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) { runnable.run() }
            override fun executeOnMainThread(runnable: Runnable) { runnable.run() }
            override fun postToMainThread(runnable: Runnable) { runnable.run() }
            override fun isMainThread(): Boolean = true
        })
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        ArchTaskExecutor.getInstance().setDelegate(null)
    }

    @Test
    fun `toggle humidity should update local prefs`() = runTest {
        // When toggle humidity to false
        viewModel.toggleItem("humidity", false)
        // Then local prefs should have showHumidity = false
        assert(!viewModel.localPrefs.value.showHumidity)
    }

    @Test
    fun `set forecast days should update local prefs`() = runTest {
        viewModel.setForecastDays(5)
        assert(viewModel.localPrefs.value.forecastDays == 5)
    }

    @Test
    fun `savePrefs should call updatePrefs with local prefs`() = runTest {
        // Given
        viewModel.toggleItem("humidity", false)
        viewModel.setForecastDays(5)
        // When
        viewModel.savePrefs()
        // Then updatePrefs should be called with showHumidity = false and forecastDays = 5
        coVerify { prefsRepository.updatePrefs(match { it.showHumidity == false && it.forecastDays == 5 }) }
    }

    @Test
    fun `hasChanges should be true after toggle`() = runTest {
        // Given initial state (no changes)
        // When toggle humidity
        viewModel.toggleItem("humidity", false)
        // Then hasChanges should be true
        assert(viewModel.hasChanges.value)
    }

    @Test
    fun `resetPrefs should revert changes`() = runTest {
        // Given toggle humidity
        viewModel.toggleItem("humidity", false)
        assert(viewModel.hasChanges.value)
        // When reset
        viewModel.resetPrefs()
        // Then hasChanges should be false
        assert(!viewModel.hasChanges.value)
        assert(viewModel.localPrefs.value.showHumidity) // back to default true
    }
}
