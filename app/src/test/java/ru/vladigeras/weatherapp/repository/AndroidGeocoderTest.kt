package ru.vladigeras.weatherapp.repository

import android.location.Address
import android.location.Geocoder
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.slot
import io.mockk.unmockkConstructor
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AndroidGeocoderTest {
    private val listener = slot<Geocoder.GeocodeListener>()
    private lateinit var geocoder: AndroidGeocoder

    @Before
    fun setup() {
        mockkConstructor(Geocoder::class)
        every { anyConstructed<Geocoder>().getFromLocation(any(), any(), any(), capture(listener)) } returns Unit
        geocoder = AndroidGeocoder(RuntimeEnvironment.getApplication())
    }

    @After
    fun teardown() {
        unmockkConstructor(Geocoder::class)
    }

    @Test
    fun `success returns received addresses`() = runTest {
        val result = async { geocoder.getFromLocation(55.75, 37.62, 1) }
        runCurrent()
        val addresses = listOf(Address(Locale.ENGLISH).apply { locality = "Moscow" })
        listener.captured.onGeocode(addresses)
        assertEquals(addresses, result.await())
    }

    @Test
    fun `empty result finishes without a name`() = runTest {
        val result = async { geocoder.getFromLocation(55.75, 37.62, 1) }
        runCurrent()
        listener.captured.onGeocode(emptyList())
        assertTrue(result.await().isNullOrEmpty())
    }

    @Test
    fun `error callback finishes without waiting`() = runTest {
        val result = async { geocoder.getFromLocation(55.75, 37.62, 1) }
        runCurrent()
        listener.captured.onError("service unavailable")
        runCurrent()
        val completed = result.isCompleted
        result.cancel()
        assertTrue(completed)
    }

    @Test
    fun `missing response ends after five seconds and ignores late response`() = runTest {
        val result = async { geocoder.getFromLocation(55.75, 37.62, 1) }
        runCurrent()
        advanceTimeBy(4_999)
        assertFalse(result.isCompleted)
        advanceTimeBy(1)
        runCurrent()
        val completed = result.isCompleted
        if (!completed) result.cancel()
        assertTrue(completed)
        assertNull(result.await())
        listener.captured.onGeocode(listOf(Address(Locale.ENGLISH)))
        assertNull(result.await())
    }

    @Test
    fun `parent cancellation remains cancellation and ignores late response`() = runTest {
        val result = async { geocoder.getFromLocation(55.75, 37.62, 1) }
        runCurrent()
        result.cancel()
        runCurrent()
        listener.captured.onGeocode(emptyList())
        listener.captured.onError("late error")
        assertTrue(result.isCancelled)
    }
}
