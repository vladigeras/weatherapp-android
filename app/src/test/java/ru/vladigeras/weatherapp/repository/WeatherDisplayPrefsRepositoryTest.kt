package ru.vladigeras.weatherapp.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import app.cash.turbine.test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import ru.vladigeras.weatherapp.data.WeatherDisplayPrefs
import ru.vladigeras.weatherapp.util.TestDataStoreFactory
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
class WeatherDisplayPrefsRepositoryTest {

    private lateinit var repository: WeatherDisplayPrefsRepository
    private lateinit var tempDir: File
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var dataStoreScope: CoroutineScope

    @Before
    fun setUp() {
        tempDir = TestDataStoreFactory.createTempDir("test_weather_display_prefs")
        dataStoreScope = CoroutineScope(UnconfinedTestDispatcher())
        dataStore = TestDataStoreFactory.createTestDataStore(
            scope = dataStoreScope,
            tempDir = tempDir
        )
        repository = WeatherDisplayPrefsRepository(
            context = RuntimeEnvironment.getApplication(),
            testDataStore = dataStore
        )
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
        TestDataStoreFactory.cleanupWithRetry(tempDir)
    }

    @Test
    fun getPrefs_firstLaunch_returnsDefaults() = runTest {
        repository.getPrefs().test {
            val prefs = awaitItem()
            assertEquals(true, prefs.showHumidity)
            assertEquals(true, prefs.showWind)
            assertEquals(true, prefs.showPrecipitation)
            assertEquals(true, prefs.showSunTimes)
            assertEquals(true, prefs.showUvIndex)
            assertEquals(false, prefs.showForecastDays)
            assertEquals(1, prefs.forecastDays)
            assertEquals(false, prefs.showHourlyForecast)
            assertEquals(24, prefs.hourlyForecastHours)
        }
    }

    @Test
    fun updatePrefs_emitsNewValues() = runTest {
        repository.getPrefs().test {
            awaitItem()

            repository.updatePrefs(
                WeatherDisplayPrefs(
                    showHumidity = false,
                    showWind = true,
                    showPrecipitation = true,
                    showSunTimes = true,
                    showUvIndex = true,
                    showForecastDays = true,
                    forecastDays = 1,
                    showHourlyForecast = true,
                    hourlyForecastHours = 12
                )
            )

            val prefs = awaitItem()
            assertEquals(false, prefs.showHumidity)
            assertEquals(true, prefs.showHourlyForecast)
            assertEquals(12, prefs.hourlyForecastHours)
        }
    }

    @Test
    fun updatePrefs_partialUpdate_preservesUnchanged() = runTest {
        repository.getPrefs().test {
            awaitItem()
            
            repository.updatePrefs(
                WeatherDisplayPrefs(
                    showHumidity = true,
                    showWind = true,
                    showPrecipitation = true,
                    showSunTimes = true,
                    showUvIndex = true,
                    showForecastDays = true,
                    forecastDays = 3
                )
            )
            
            val prefs = awaitItem()
            assertEquals(3, prefs.forecastDays)
            assertEquals(true, prefs.showHumidity)
        }
    }

    @Test
    fun updatePrefs_hourlyForecast_preservesOtherSettings() = runTest {
        repository.getPrefs().test {
            awaitItem()

            repository.updatePrefs(
                WeatherDisplayPrefs(
                    showHumidity = true,
                    showWind = true,
                    showPrecipitation = true,
                    showSunTimes = true,
                    showUvIndex = true,
                    showForecastDays = true,
                    forecastDays = 7,
                    showHourlyForecast = true,
                    hourlyForecastHours = 48
                )
            )

            val prefs = awaitItem()
            assertEquals(true, prefs.showHourlyForecast)
            assertEquals(48, prefs.hourlyForecastHours)
            assertEquals(7, prefs.forecastDays)
            assertEquals(true, prefs.showForecastDays)
        }
    }
}