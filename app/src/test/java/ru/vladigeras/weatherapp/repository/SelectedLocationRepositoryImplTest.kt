package ru.vladigeras.weatherapp.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import app.cash.turbine.test
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import ru.vladigeras.weatherapp.data.Location
import java.io.File

class SelectedLocationRepositoryImplTest {

    private lateinit var repository: SelectedLocationRepositoryImpl
    private lateinit var tempFile: File
    private lateinit var dataStore: DataStore<Preferences>

    @Before
    fun setUp() {
        tempFile = File.createTempFile("test_datastore", ".preferences_pb")
        tempFile.deleteOnExit()
        
        dataStore = PreferenceDataStoreFactory.create(
            produceFile = { tempFile }
        )
        
        repository = SelectedLocationRepositoryImpl(
            context = mockk(relaxed = true),
            testDataStore = dataStore
        )
    }

    @After
    fun tearDown() {
        tempFile.delete()
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
            // Initial value should be null
            assertNull(awaitItem())
            
            // Save location
            repository.saveSelectedLocation(location)
            
            // Verify saved location
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
            // Skip initial value
            awaitItem()
            
            // Save location
            repository.saveSelectedLocation(location)
            awaitItem()
            
            // Clear location
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
