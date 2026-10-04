package ru.vladigeras.weatherapp.location

import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LocationServiceImplTest {
    private val client = mockk<FusedLocationProviderClient>()
    private val task = mockk<Task<android.location.Location>>()
    private val token = slot<CancellationToken?>()
    private val success = slot<OnSuccessListener<android.location.Location>>()
    private val failure = slot<OnFailureListener>()
    private lateinit var service: LocationServiceImpl

    @Before
    fun setup() {
        val context: Context = RuntimeEnvironment.getApplication()
        mockkStatic(LocationServices::class)
        every { LocationServices.getFusedLocationProviderClient(context) } returns client
        every { client.getCurrentLocation(any<Int>(), captureNullable(token)) } returns task
        every { task.addOnSuccessListener(capture(success)) } returns task
        every { task.addOnFailureListener(capture(failure)) } returns task
        service = LocationServiceImpl(context)
    }

    @After
    fun teardown() {
        unmockkStatic(LocationServices::class)
    }

    @Test
    fun `cancellation cancels the external request and ignores late callbacks`() = runTest {
        val result = async { service.getCurrentLocation() }
        runCurrent()
        assertNotNull(token.captured)
        result.cancel()
        runCurrent()
        assertTrue(token.captured!!.isCancellationRequested)
        success.captured.onSuccess(android.location.Location("gps"))
        failure.captured.onFailure(Exception("late failure"))
        assertTrue(result.isCancelled)
    }

    @Test
    fun `success returns received coordinates`() = runTest {
        val result = async { service.getCurrentLocation() }
        runCurrent()
        success.captured.onSuccess(android.location.Location("gps").apply {
            latitude = 55.75
            longitude = 37.62
        })
        assertEquals(55.75, result.await().getOrThrow().latitude, 0.0)
    }
}
