package ru.vladigeras.weatherapp.repository

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.vladigeras.weatherapp.data.Location
import ru.vladigeras.weatherapp.location.LocationService

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LocationRepositoryImplTest {

    private lateinit var context: Context
    private lateinit var locationService: LocationService
    private lateinit var repository: LocationRepositoryImpl

    @Before
    fun setUp() {
        val activity = Robolectric.buildActivity(android.app.Activity::class.java).setup().get()
        context = spyk(activity)
        locationService = mockk()
        repository = LocationRepositoryImpl(context, locationService)
        
        // МОКАЕМ статический метод ContextCompat.checkSelfPermission
        mockkStatic(ContextCompat::class)
    }
    
    @After
    fun tearDown() {
        io.mockk.clearStaticMockk(ContextCompat::class)
    }

    @Test
    fun getLocation_noPermission_returnsFailure() = runBlocking {
        // МОКАЕМ статический ContextCompat.checkSelfPermission
        every { 
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) 
        } returns PackageManager.PERMISSION_DENIED
        
        val result = repository.getLocation()
        
        assertTrue("Expected failure when no permission", result.isFailure)
    }

    @Test
    fun getLocation_withPermission_succeeds() = runBlocking {
        // МОКАЕМ статический ContextCompat.checkSelfPermission - разрешение ДАНО
        every { 
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) 
        } returns PackageManager.PERMISSION_GRANTED
        
        val location = Location(55.75, 37.62, null, false)
        
        coEvery { locationService.getCurrentLocation() } returns Result.success(location)
        
        val result = repository.getLocation()
        
        assertTrue("Call should succeed with permission", result.isSuccess)
        assertEquals(55.75, result.getOrNull()?.latitude)
        assertEquals(37.62, result.getOrNull()?.longitude)
    }

    @Test
    fun getLocation_cacheExpired_fetchesNew() = runBlocking {
        // МОКАЕМ статический ContextCompat.checkSelfPermission - разрешение ДАНО
        every { 
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) 
        } returns PackageManager.PERMISSION_GRANTED
        
        val location1 = Location(55.75, 37.62, "Moscow", false)
        
        coEvery { locationService.getCurrentLocation() } returns Result.success(location1)
        
        // Первый вызов
        val result1 = repository.getLocation()
        assertTrue("First call should succeed", result1.isSuccess)
    }
}