package ru.vladigeras.weatherapp.ui

import io.mockk.coEvery
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import org.junit.Assert.assertTrue
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.justRun
import ru.vladigeras.weatherapp.core.locale.LanguageManager
import ru.vladigeras.weatherapp.data.ProviderCapabilities
import ru.vladigeras.weatherapp.data.WeatherProviderId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import ru.vladigeras.weatherapp.data.WeatherDisplayPrefs
import ru.vladigeras.weatherapp.repository.LanguagePreference
import ru.vladigeras.weatherapp.repository.LanguagePreferenceRepository
import ru.vladigeras.weatherapp.repository.WeatherDisplayPrefsRepository
import ru.vladigeras.weatherapp.repository.WeatherRepository

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val weatherRepository = mockk<WeatherRepository> { every { capabilities(any()) } answers {
        when (firstArg<WeatherProviderId>()) {
            WeatherProviderId.YR -> ProviderCapabilities(9, 1, dailyUv = false)
            WeatherProviderId.OPEN_METEO -> ProviderCapabilities(16, 1)
            WeatherProviderId.WTTR -> ProviderCapabilities(3, 3, false, false, false, false)
            WeatherProviderId.SEVEN_TIMER -> ProviderCapabilities(7, 3, false, false, false, wind = false, sunTimes = false)
        }
    } }
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var prefsRepository: WeatherDisplayPrefsRepository
    private lateinit var languagePreferenceRepository: LanguagePreferenceRepository
    private lateinit var viewModel: SettingsViewModel
    private val viewModelStore = ViewModelStore()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(LanguageManager)
        justRun { LanguageManager.applyLocale(any()) }

        prefsRepository = mockk(relaxed = true) {
            every { getPrefs() } returns flowOf(WeatherDisplayPrefs())
        }
        languagePreferenceRepository = mockk(relaxed = true)
        coEvery { languagePreferenceRepository.getLanguagePreference() } returns LanguagePreference.SYSTEM

        viewModel = SettingsViewModel(prefsRepository, languagePreferenceRepository, weatherRepository)
        viewModelStore.put("settings", viewModel)
    }

    @After
    fun tearDown() = runTest(testDispatcher) {
        try {
            val job = viewModel.viewModelScope.coroutineContext[Job]!!
            viewModelStore.clear()
            job.join()
            assertTrue(job.isCompleted)
        } finally {
            unmockkObject(LanguageManager)
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `toggle humidity should update local prefs`() = runTest {
        advanceUntilIdle()
        viewModel.toggleItem("humidity", false)
        assertFalse(viewModel.localPrefs.value.showHumidity)
        assertTrue(viewModel.hasChanges.value)
    }

    @Test
    fun `set forecast days should update local prefs`() = runTest {
        advanceUntilIdle()
        viewModel.setForecastDays(5)
        assertEquals(5, viewModel.localPrefs.value.forecastDays)
        assertTrue(viewModel.hasChanges.value)
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
        assertTrue(viewModel.hasChanges.value)
    }

    @Test
    fun `resetPrefs should revert changes`() = runTest {
        advanceUntilIdle()
        viewModel.toggleItem("humidity", false)
        assertTrue(viewModel.hasChanges.value)
        viewModel.resetPrefs()
        assertFalse(viewModel.hasChanges.value)
        assertTrue(viewModel.localPrefs.value.showHumidity)
    }

    @Test
    fun `savePrefsAndCheckLanguage should return true when language changed`() = runTest {
        advanceUntilIdle()
        viewModel.setLanguagePreference(LanguagePreference.RUSSIAN)
        val result = viewModel.savePrefsAndCheckLanguage()
        assertTrue(result)
        io.mockk.coVerify { languagePreferenceRepository.saveLanguagePreference(LanguagePreference.RUSSIAN) }
        io.mockk.coVerifyOrder {
            languagePreferenceRepository.saveLanguagePreference(LanguagePreference.RUSSIAN)
            LanguageManager.applyLocale(LanguagePreference.RUSSIAN)
        }
    }

    @Test
    fun `savePrefsAndCheckLanguage should return false when language not changed`() = runTest {
        advanceUntilIdle()
        val result = viewModel.savePrefsAndCheckLanguage()
        assertFalse(result)
    }

    @Test
    fun `setLanguagePreference should update language preference flow`() = runTest {
        advanceUntilIdle()
        viewModel.setLanguagePreference(LanguagePreference.ENGLISH)
        assertEquals(LanguagePreference.ENGLISH, viewModel.languagePreference.value)
        io.mockk.verify(exactly = 0) { LanguageManager.applyLocale(any()) }
        io.mockk.coVerify(exactly = 0) { languagePreferenceRepository.saveLanguagePreference(any()) }
    }

    @Test
    fun `leaving language selection without saving retains the original language`() = runTest {
        advanceUntilIdle()
        viewModel.setLanguagePreference(LanguagePreference.RUSSIAN)
        viewModel.resetPrefs()
        assertEquals(LanguagePreference.SYSTEM, viewModel.languagePreference.value)
        io.mockk.verify(exactly = 0) { LanguageManager.applyLocale(any()) }
        io.mockk.coVerify(exactly = 0) { languagePreferenceRepository.saveLanguagePreference(any()) }
    }

    @Test
    fun `toggle hourly forecast off should update local prefs`() = runTest {
        advanceUntilIdle()
        viewModel.toggleItem("hourly_forecast", false)
        assertFalse(viewModel.localPrefs.value.showHourlyForecast)
        assertTrue(viewModel.hasChanges.value)
    }

    @Test
    fun `set hourly forecast hours should update local prefs`() = runTest {
        advanceUntilIdle()
        viewModel.setHourlyForecastHours(48)
        assertEquals(48, viewModel.localPrefs.value.hourlyForecastHours)
        assertTrue(viewModel.hasChanges.value)
    }

    @Test
    fun `toggle hourly forecast should default to true`() = runTest {
        advanceUntilIdle()
        assertTrue(viewModel.localPrefs.value.showHourlyForecast)
    }

    @Test
    fun `hourly forecast hours should default to 12`() = runTest {
        advanceUntilIdle()
        assertEquals(12, viewModel.localPrefs.value.hourlyForecastHours)
    }

    @Test
    fun `savePrefsAndCheckLanguage should save hourly forecast prefs`() = runTest {
        advanceUntilIdle()
        viewModel.toggleItem("hourly_forecast", true)
        viewModel.setHourlyForecastHours(12)
        viewModel.savePrefsAndCheckLanguage()
        io.mockk.coVerify { prefsRepository.updatePrefs(match { it.showHourlyForecast == true && it.hourlyForecastHours == 12 }) }
    }
    @Test
    fun `provider constraints mask shared prefs without overwriting them`() = runTest {
        advanceUntilIdle()
        viewModel.setForecastDays(14)
        viewModel.setProvider(WeatherProviderId.WTTR)
        val effective = viewModel.capabilities().effectivePrefs(viewModel.localPrefs.value)
        assertEquals(3, effective.forecastDays)
        assertFalse(effective.showUvIndex)
        assertFalse(effective.showPrecipitation)
        viewModel.toggleItem("uv_index", false)
        viewModel.toggleItem("precipitation", false)
        viewModel.setForecastDays(5)
        assertEquals(14, viewModel.localPrefs.value.forecastDays)
        assertTrue(viewModel.localPrefs.value.showUvIndex)
        assertTrue(viewModel.localPrefs.value.showPrecipitation)
        viewModel.toggleItem("humidity", false)
        viewModel.savePrefsAndCheckLanguage()
        io.mockk.coVerify { prefsRepository.updatePrefs(match { it.provider == WeatherProviderId.WTTR && it.forecastDays == 14 && it.showUvIndex && !it.showHumidity }) }
        viewModel.setProvider(WeatherProviderId.OPEN_METEO)
        assertEquals(14, viewModel.capabilities().effectivePrefs(viewModel.localPrefs.value).forecastDays)
        assertTrue(viewModel.localPrefs.value.showUvIndex)
        assertFalse(viewModel.localPrefs.value.showHumidity)
    }

    @Test
    fun `7Timer disables unavailable choices while preserving preferences for other providers`() = runTest {
        advanceUntilIdle()
        viewModel.setForecastDays(14)
        viewModel.setProvider(WeatherProviderId.SEVEN_TIMER)
        val effective = viewModel.capabilities().effectivePrefs(viewModel.localPrefs.value)
        assertEquals(7, effective.forecastDays)
        assertFalse(effective.showWind)
        assertFalse(effective.showSunTimes)
        assertFalse(effective.showPrecipitation)
        assertFalse(effective.showUvIndex)
        listOf("wind", "sun_times", "precipitation", "uv_index").forEach { viewModel.toggleItem(it, false) }
        viewModel.setForecastDays(10)
        assertEquals(14, viewModel.localPrefs.value.forecastDays)
        viewModel.savePrefsAndCheckLanguage()
        io.mockk.coVerify { prefsRepository.updatePrefs(match {
            it.provider == WeatherProviderId.SEVEN_TIMER && it.forecastDays == 14 && it.showWind && it.showSunTimes && it.showUvIndex && it.showPrecipitation
        }) }
        viewModel.setProvider(WeatherProviderId.OPEN_METEO)
        assertEquals(WeatherDisplayPrefs(forecastDays = 14), viewModel.capabilities().effectivePrefs(viewModel.localPrefs.value))
        viewModel.setProvider(WeatherProviderId.SEVEN_TIMER)
        viewModel.setForecastDays(7)
        assertEquals(7, viewModel.localPrefs.value.forecastDays)
    }

    @Test
    fun `yr permits nine days and masks UV without overwriting shared choices`() = runTest {
        advanceUntilIdle()
        viewModel.setForecastDays(16)
        viewModel.setProvider(WeatherProviderId.YR)
        val effective = viewModel.capabilities().effectivePrefs(viewModel.localPrefs.value)
        assertEquals(9, effective.forecastDays)
        assertFalse(effective.showUvIndex)
        assertTrue(effective.showWind && effective.showSunTimes && effective.showPrecipitation)
        viewModel.toggleItem("uv_index", false)
        viewModel.setForecastDays(10)
        assertEquals(16, viewModel.localPrefs.value.forecastDays)
        assertTrue(viewModel.localPrefs.value.showUvIndex)
        viewModel.setForecastDays(9)
        viewModel.savePrefsAndCheckLanguage()
        io.mockk.coVerify { prefsRepository.updatePrefs(match { it.provider == WeatherProviderId.YR && it.forecastDays == 9 && it.showUvIndex }) }
        viewModel.setProvider(WeatherProviderId.OPEN_METEO)
        assertTrue(viewModel.capabilities().effectivePrefs(viewModel.localPrefs.value).showUvIndex)
    }
}
