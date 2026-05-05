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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import ru.vladigeras.weatherapp.data.Location
import ru.vladigeras.weatherapp.util.TestDataStoreFactory
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
class SelectedLocationRepositoryImplTest {

    private lateinit var repository: SelectedLocationRepositoryImpl
    private lateinit var tempDir: File
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var dataStoreScope: CoroutineScope

    @Before
    fun setUp() {
        tempDir = TestDataStoreFactory.createTempDir("test_selected_location")
        dataStoreScope = CoroutineScope(UnconfinedTestDispatcher())
        dataStore = TestDataStoreFactory.createTestDataStore(
            scope = dataStoreScope,
            tempDir = tempDir
        )
        repository = SelectedLocationRepositoryImpl(
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
    fun saveAndGetLocation_persistsData() = runTest {
        val location = Location(
            latitude = 55.75,
            longitude = 37.62,
            name = "Moscow",
            isAutoDetected = false
        )
        
        repository.getSelectedLocation().test {
            assertNull(awaitItem())
            
            repository.saveSelectedLocation(location)
            
            val saved = awaitItem()
            assertEquals(location.latitude, saved?.latitude)
            assertEquals(location.longitude, saved?.longitude)
            assertEquals(location.name, saved?.name)
            assertEquals(location.isAutoDetected, saved?.isAutoDetected)
        }
    }

    @Test
    fun clearSelectedLocation_returnsNull() = runTest {
        val location = Location(
            latitude = 55.75,
            longitude = 37.62,
            name = "Moscow",
            isAutoDetected = false
        )
        
        repository.getSelectedLocation().test {
            awaitItem()
            
            repository.saveSelectedLocation(location)
            awaitItem()
            
            repository.clearSelectedLocation()
            val cleared = awaitItem()
            assertNull(cleared)
        }
    }

    @Test
    fun getSelectedLocation_handlesMissingPrefs() = runTest {
        repository.getSelectedLocation().test {
            val result = awaitItem()
            assertNull(result)
        }
    }
}