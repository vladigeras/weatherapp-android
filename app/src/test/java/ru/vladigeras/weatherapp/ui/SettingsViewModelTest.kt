package ru.vladigeras.weatherapp.ui

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import ru.vladigeras.weatherapp.data.WeatherDisplayPrefs
import ru.vladigeras.weatherapp.repository.LanguagePreferenceRepository
import ru.vladigeras.weatherapp.repository.WeatherDisplayPrefsRepository

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private lateinit var prefsRepository: WeatherDisplayPrefsRepository
    private lateinit var languagePreferenceRepository: LanguagePreferenceRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        prefsRepository = mockk(relaxed = true) {
            every { getPrefs() } returns flowOf(WeatherDisplayPrefs())
        }
        languagePreferenceRepository = mockk(relaxed = true)
        viewModel = SettingsViewModel(prefsRepository, languagePreferenceRepository)
    }

    @Test
    fun `toggle humidity should update local prefs`() {
        viewModel.toggleItem("humidity", false)
        assert(!viewModel.localPrefs.value.showHumidity)
    }

    @Test
    fun `set forecast days should update local prefs`() {
        viewModel.setForecastDays(5)
        assert(viewModel.localPrefs.value.forecastDays == 5)
    }

    @Test
    fun `savePrefsAndCheckLanguage should call updatePrefs with local prefs`() = runTest {
        viewModel.toggleItem("humidity", false)
        viewModel.setForecastDays(5)
        viewModel.savePrefsAndCheckLanguage()
        coVerify { prefsRepository.updatePrefs(match { it.showHumidity == false && it.forecastDays == 5 }) }
    }

    @Test
    fun `hasChanges should be true after toggle`() {
        viewModel.toggleItem("humidity", false)
        assert(viewModel.hasChanges.value)
    }

    @Test
    fun `resetPrefs should revert changes`() {
        viewModel.toggleItem("humidity", false)
        assert(viewModel.hasChanges.value)
        viewModel.resetPrefs()
        assert(!viewModel.hasChanges.value)
        assert(viewModel.localPrefs.value.showHumidity)
    }
}