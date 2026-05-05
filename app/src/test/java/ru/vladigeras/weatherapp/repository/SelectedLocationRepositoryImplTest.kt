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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import ru.vladigeras.weatherapp.data.Location
import ru.vladigeras.weatherapp.util.TestDataStoreFactory
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SelectedLocationRepositoryImplTest {

    private lateinit var repository: SelectedLocationRepositoryImpl
    private lateinit var tempFile: File
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var dataStoreScope: CoroutineScope

    @Before
    fun setUp() {
        tempFile = TestDataStoreFactory.createTempFile("test_selected_location")
        dataStoreScope = CoroutineScope(UnconfinedTestDispatcher())
        dataStore = TestDataStoreFactory.createTestDataStore(
            scope = dataStoreScope,
            tempFile = tempFile
        )
        repository = SelectedLocationRepositoryImpl(
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