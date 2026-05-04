package ru.vladigeras.weatherapp.ui

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import ru.vladigeras.weatherapp.data.WeatherDisplayPrefs
import ru.vladigeras.weatherapp.repository.LanguagePreference
import ru.vladigeras.weatherapp.repository.LanguagePreferenceRepository
import ru.vladigeras.weatherapp.repository.WeatherDisplayPrefsRepository

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var prefsRepository: WeatherDisplayPrefsRepository
    private lateinit var languagePreferenceRepository: LanguagePreferenceRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        prefsRepository = mockk(relaxed = true) {
            every { getPrefs() } returns flowOf(WeatherDisplayPrefs())
        }
        languagePreferenceRepository = mockk(relaxed = true)
        coEvery { languagePreferenceRepository.getLanguagePreference() } returns LanguagePreference.SYSTEM

        viewModel = SettingsViewModel(prefsRepository, languagePreferenceRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `toggle humidity should update local prefs`() = runTest {
        advanceUntilIdle()
        viewModel.toggleItem("humidity", false)
        assert(!viewModel.localPrefs.value.showHumidity)
    }

    @Test
    fun `set forecast days should update local prefs`() = runTest {
        advanceUntilIdle()
        viewModel.setForecastDays(5)
        assert(viewModel.localPrefs.value.forecastDays == 5)
    }

    @Test
    fun `savePrefsAndCheckLanguage should call updatePrefs with local prefs`() = runTest {
        advanceUntilIdle()
        viewModel.toggleItem("humidity", false)
        viewModel.setForecastDays(5)
        viewModel.savePrefsAndCheckLanguage()
        io.mockk.coVerify { prefsRepository.updatePrefs(match { it.showHumidity == false && it.forecastDays == 5 }) }
    }

    @Test
    fun `hasChanges should be true after toggle`() = runTest {
        advanceUntilIdle()
        viewModel.toggleItem("humidity", false)
        assert(viewModel.hasChanges.value)
    }

    @Test
    fun `resetPrefs should revert changes`() = runTest {
        advanceUntilIdle()
        viewModel.toggleItem("humidity", false)
        assert(viewModel.hasChanges.value)
        viewModel.resetPrefs()
        assert(!viewModel.hasChanges.value)
        assert(viewModel.localPrefs.value.showHumidity)
    }

    @Test
    fun `savePrefsAndCheckLanguage should return true when language changed`() = runTest {
        advanceUntilIdle()
        viewModel.setLanguagePreference(LanguagePreference.RUSSIAN)
        val result = viewModel.savePrefsAndCheckLanguage()
        assert(result)
        io.mockk.coVerify { languagePreferenceRepository.saveLanguagePreference(LanguagePreference.RUSSIAN) }
    }

    @Test
    fun `savePrefsAndCheckLanguage should return false when language not changed`() = runTest {
        advanceUntilIdle()
        val result = viewModel.savePrefsAndCheckLanguage()
        assert(!result)
    }

    @Test
    fun `setLanguagePreference should update language preference flow`() = runTest {
        advanceUntilIdle()
        viewModel.setLanguagePreference(LanguagePreference.ENGLISH)
        assert(viewModel.languagePreference.value == LanguagePreference.ENGLISH)
    }
}