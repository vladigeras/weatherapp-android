package ru.vladigeras.weatherapp.repository

import android.location.Address
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.vladigeras.weatherapp.data.SearchLocation
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CitySearchRepositoryTest {
    private val geocoder = mockk<AndroidGeocoder>()
    private val cache = CitySearchCache()
    private val repository = CitySearchRepository(geocoder, cache)

    private fun address(name: String? = "Moscow") = Address(Locale.ENGLISH).apply {
        locality = name
        latitude = 55.75
        longitude = 37.62
        adminArea = "Moscow"
        countryName = "Russia"
    }

    @Test
    fun `maps usable addresses and limits results to five`() = runTest {
        val invalid = listOf(Address(Locale.ENGLISH), address().apply { latitude = Double.NaN }, address().apply { longitude = 181.0 })
        val fallback = address(" ").apply { subAdminArea = "District" }
        coEvery { geocoder.getFromLocationName("Moscow", 5, Locale.ENGLISH) } returns invalid + fallback + List(6) { address() }
        val results = repository.searchLocations("Moscow", "en").getOrThrow()
        assertEquals(5, results.size)
        assertEquals(SearchLocation("District", 55.75, 37.62, "Russia", "Moscow"), results.first())
        assertEquals(SearchLocation("Moscow", 55.75, 37.62, "Russia", "Moscow"), results.last())
    }

    @Test
    fun `reuses successful results and searches each language separately`() = runTest {
        coEvery { geocoder.getFromLocationName("Moscow", 5, Locale.ENGLISH) } returns listOf(address())
        coEvery { geocoder.getFromLocationName("Moscow", 5, Locale.forLanguageTag("ru")) } returns listOf(address("Москва"))
        assertEquals("Moscow", repository.searchLocations("Moscow", "en").getOrThrow().single().name)
        assertEquals("Moscow", repository.searchLocations("MOSCOW", "en").getOrThrow().single().name)
        assertEquals("Москва", repository.searchLocations("Moscow", "ru").getOrThrow().single().name)
        coVerify(exactly = 1) { geocoder.getFromLocationName("Moscow", 5, Locale.ENGLISH) }
        coVerify(exactly = 1) { geocoder.getFromLocationName("Moscow", 5, Locale.forLanguageTag("ru")) }
    }

    @Test
    fun `empty results and service failures are not cached`() = runTest {
        coEvery { geocoder.getFromLocationName(any(), any(), any()) } returns emptyList()
        assertTrue(repository.searchLocations("Moscow", "en").getOrThrow().isEmpty())
        assertNull(cache.get("Moscow", "en"))
        coEvery { geocoder.getFromLocationName(any(), any(), any()) } throws CitySearchUnavailableException()
        assertTrue(repository.searchLocations("Moscow", "en").exceptionOrNull() is CitySearchUnavailableException)
        assertNull(cache.get("Moscow", "en"))
        coEvery { geocoder.getFromLocationName(any(), any(), any()) } returns listOf(address())
        assertEquals("Moscow", repository.searchLocations("Moscow", "en").getOrThrow().single().name)
        coVerify(exactly = 3) { geocoder.getFromLocationName("Moscow", 5, Locale.ENGLISH) }
    }

    @Test
    fun `cancelled search never caches a late response`() = runTest {
        val response = CompletableDeferred<List<Address>>()
        coEvery { geocoder.getFromLocationName(any(), any(), any()) } coAnswers {
            withContext(NonCancellable) { response.await() }
        }
        val result = async { repository.searchLocations("Moscow", "en") }
        runCurrent()
        result.cancel()
        response.complete(listOf(address()))
        runCurrent()
        assertTrue(result.isCancelled)
        assertNull(cache.get("Moscow", "en"))
    }
}
