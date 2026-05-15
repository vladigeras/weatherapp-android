package ru.vladigeras.weatherapp.repository

import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import androidx.core.content.ContextCompat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    private lateinit var androidGeocoder: AndroidGeocoder
    private lateinit var repository: LocationRepositoryImpl

    @Before
    fun setUp() {
        val activity = Robolectric.buildActivity(android.app.Activity::class.java).setup().get()
        context = spyk(activity)
        locationService = mockk()
        androidGeocoder = mockk()
        repository = LocationRepositoryImpl(context, locationService, androidGeocoder)

        mockkStatic(ContextCompat::class)
    }

    @After
    fun tearDown() {
        io.mockk.clearStaticMockk(ContextCompat::class)
    }

    private fun grantPermission() {
        every {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
        } returns PackageManager.PERMISSION_GRANTED
    }

    private fun denyPermission() {
        every {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
        } returns PackageManager.PERMISSION_DENIED
    }

    @Test
    fun getLocation_noPermission_returnsFailure() = runBlocking {
        denyPermission()

        val result = repository.getLocation()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun getLocation_withPermission_succeeds() = runBlocking {
        grantPermission()

        val location = Location(55.75, 37.62, null, false)
        coEvery { locationService.getCurrentLocation() } returns Result.success(location)
        coEvery { androidGeocoder.getFromLocation(any(), any(), any()) } returns null

        val result = repository.getLocation()

        assertTrue(result.isSuccess)
        assertEquals(55.75, result.getOrNull()!!.latitude, 0.01)
        assertEquals(37.62, result.getOrNull()!!.longitude, 0.01)
    }

    @Test
    fun getLocation_locationServiceFailure_returnsFailure() = runBlocking {
        grantPermission()

        coEvery { locationService.getCurrentLocation() } returns Result.failure(Exception("GPS unavailable"))

        val result = repository.getLocation()

        assertTrue(result.isFailure)
        assertEquals("Location unavailable", result.exceptionOrNull()?.message)
    }

    @Test
    fun getLocation_cachedResult_returnsFromCacheWithoutServiceCall() = runBlocking {
        grantPermission()

        val location = Location(55.75, 37.62, null, false)
        coEvery { locationService.getCurrentLocation() } returns Result.success(location)
        coEvery { androidGeocoder.getFromLocation(any(), any(), any()) } returns null

        val result1 = repository.getLocation()
        assertTrue(result1.isSuccess)

        val result2 = repository.getLocation()
        assertTrue(result2.isSuccess)

        coVerify(exactly = 1) { locationService.getCurrentLocation() }
    }

    @Test
    fun getLocation_cacheExpired_fetchesNewLocation() = runBlocking {
        grantPermission()

        val location = Location(55.75, 37.62, null, false)
        coEvery { locationService.getCurrentLocation() } returns Result.success(location)
        coEvery { androidGeocoder.getFromLocation(any(), any(), any()) } returns null

        val result1 = repository.getLocation()
        assertTrue(result1.isSuccess)

        val cacheField = LocationRepositoryImpl::class.java.getDeclaredField("cache")
        cacheField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val cacheFlow = cacheField.get(repository) as kotlinx.coroutines.flow.MutableStateFlow<Any?>
        val cachedValue = cacheFlow.value!!
        val timestampField = cachedValue::class.java.getDeclaredField("timestamp")
        timestampField.isAccessible = true
        timestampField.setLong(cachedValue, System.currentTimeMillis() - 11 * 60 * 1000)

        val result2 = repository.getLocation()
        assertTrue(result2.isSuccess)

        coVerify(exactly = 2) { locationService.getCurrentLocation() }
    }

    @Test
    fun getLocation_withGeocoder_resolvesCityName() = runBlocking {
        grantPermission()

        val location = Location(55.75, 37.62, null, false)
        coEvery { locationService.getCurrentLocation() } returns Result.success(location)

        val address = mockk<Address>(relaxed = true)
        every { address.locality } returns "Moscow"
        every { address.countryName } returns "Russia"
        coEvery { androidGeocoder.getFromLocation(55.75, 37.62, 1) } returns listOf(address)

        val result = repository.getLocation()

        assertTrue(result.isSuccess)
        assertEquals("Moscow, Russia", result.getOrNull()?.name)
    }

    @Test
    fun getLocation_geocoderFallsBackToSubAdminArea() = runBlocking {
        grantPermission()

        val location = Location(55.75, 37.62, null, false)
        coEvery { locationService.getCurrentLocation() } returns Result.success(location)

        val address = mockk<Address>(relaxed = true)
        every { address.locality } returns null
        every { address.subAdminArea } returns "Moscow District"
        every { address.countryName } returns "Russia"
        coEvery { androidGeocoder.getFromLocation(any(), any(), any()) } returns listOf(address)

        val result = repository.getLocation()

        assertTrue(result.isSuccess)
        assertEquals("Moscow District, Russia", result.getOrNull()?.name)
    }

    @Test
    fun getLocation_geocoderFailure_returnsLocationWithNullName() = runBlocking {
        grantPermission()

        val location = Location(55.75, 37.62, null, false)
        coEvery { locationService.getCurrentLocation() } returns Result.success(location)
        coEvery { androidGeocoder.getFromLocation(any(), any(), any()) } throws Exception("Geocoder failed")

        val result = repository.getLocation()

        assertTrue(result.isSuccess)
        assertNull(result.getOrNull()?.name)
    }

    @Test
    fun getLocation_geocoderReturnsEmptyList_returnsLocationWithNullName() = runBlocking {
        grantPermission()

        val location = Location(55.75, 37.62, null, false)
        coEvery { locationService.getCurrentLocation() } returns Result.success(location)
        coEvery { androidGeocoder.getFromLocation(any(), any(), any()) } returns emptyList()

        val result = repository.getLocation()

        assertTrue(result.isSuccess)
        assertNull(result.getOrNull()?.name)
    }
}
