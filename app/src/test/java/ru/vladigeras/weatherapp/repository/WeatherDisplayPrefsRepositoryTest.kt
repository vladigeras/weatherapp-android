package ru.vladigeras.weatherapp.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import app.cash.turbine.test
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import ru.vladigeras.weatherapp.data.WeatherDisplayPrefs
import ru.vladigeras.weatherapp.util.TestDataStoreFactory
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherDisplayPrefsRepositoryTest {

    private lateinit var repository: WeatherDisplayPrefsRepository
    private lateinit var tempFile: File
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var dataStoreScope: CoroutineScope

    @Before
    fun setUp() {
        tempFile = TestDataStoreFactory.createTempFile("test_weather_display_prefs")
        dataStoreScope = CoroutineScope(UnconfinedTestDispatcher())
        dataStore = TestDataStoreFactory.createTestDataStore(
            scope = dataStoreScope,
            tempFile = tempFile
        )
        repository = WeatherDisplayPrefsRepository(
            context = mockk(relaxed = true),
            testDataStore = dataStore
        )
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
        runCatching { java.nio.file.Files.deleteIfExists(tempFile.toPath()) }
    }

    @Test
    fun getPrefs_firstLaunch_returnsDefaults() = runTest {
        repository.getPrefs().test {
            val prefs = awaitItem()
            assertEquals(true, prefs.showHumidity)
            assertEquals(true, prefs.showWind)
            assertEquals(true, prefs.showPrecipitation)
            assertEquals(true, prefs.showCondition)
            assertEquals(true, prefs.showSunTimes)
            assertEquals(true, prefs.showUvIndex)
            assertEquals(true, prefs.showForecast)
            assertEquals(1, prefs.forecastDays)
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
                    showCondition = true,
                    showSunTimes = true,
                    showUvIndex = true,
                    showForecast = true,
                    forecastDays = 1
                )
            )
            
            val prefs = awaitItem()
            assertEquals(false, prefs.showHumidity)
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
                    showCondition = true,
                    showSunTimes = true,
                    showUvIndex = true,
                    showForecast = true,
                    forecastDays = 3
                )
            )
            
            val prefs = awaitItem()
            assertEquals(3, prefs.forecastDays)
            assertEquals(true, prefs.showHumidity)
        }
    }
}