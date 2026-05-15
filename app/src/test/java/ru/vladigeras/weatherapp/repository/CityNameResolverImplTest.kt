package ru.vladigeras.weatherapp.repository

import android.location.Address
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.util.Locale
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CityNameResolverImplTest {

    private lateinit var cityNameResolver: CityNameResolverImpl
    private lateinit var languagePreferenceRepository: LanguagePreferenceRepository
    private lateinit var androidGeocoder: AndroidGeocoder

    @Before
    fun setup() {
        languagePreferenceRepository = mockk()
        androidGeocoder = mockk()
        cityNameResolver = CityNameResolverImpl(languagePreferenceRepository, androidGeocoder)

        coEvery { languagePreferenceRepository.getEffectiveLocaleCode() } returns "en"
        coEvery { languagePreferenceRepository.getAppLocale() } returns Locale.ENGLISH
    }

    @Test
    fun `geocoder returns locality returns formatted name`() = runTest {
        val address = mockk<Address>(relaxed = true)
        every { address.locality } returns "Moscow"
        every { address.countryName } returns "Russia"
        coEvery { androidGeocoder.getFromLocation(55.75, 37.62, 1, Locale.ENGLISH) } returns listOf(address)

        val result = cityNameResolver.resolveCityName(55.75, 37.62, null, "Europe/Moscow")

        assertEquals("Moscow, Russia", result)
    }

    @Test
    fun `geocoder null locality falls back to subAdminArea`() = runTest {
        val address = mockk<Address>(relaxed = true)
        every { address.locality } returns null
        every { address.subAdminArea } returns "Moscow District"
        every { address.countryName } returns "Russia"
        coEvery { androidGeocoder.getFromLocation(55.75, 37.62, 1, Locale.ENGLISH) } returns listOf(address)

        val result = cityNameResolver.resolveCityName(55.75, 37.62, null, "Europe/Moscow")

        assertEquals("Moscow District, Russia", result)
    }

    @Test
    fun `geocoder null locality and subAdminArea falls back to adminArea`() = runTest {
        val address = mockk<Address>(relaxed = true)
        every { address.locality } returns null
        every { address.subAdminArea } returns null
        every { address.adminArea } returns "Moscow Region"
        every { address.countryName } returns "Russia"
        coEvery { androidGeocoder.getFromLocation(55.75, 37.62, 1, Locale.ENGLISH) } returns listOf(address)

        val result = cityNameResolver.resolveCityName(55.75, 37.62, null, "Europe/Moscow")

        assertEquals("Moscow Region, Russia", result)
    }

    @Test
    fun `geocoder empty list falls back to savedName`() = runTest {
        coEvery { androidGeocoder.getFromLocation(55.75, 37.62, 1, Locale.ENGLISH) } returns emptyList()

        val result = cityNameResolver.resolveCityName(55.75, 37.62, "Saved Location", "Europe/Moscow")

        assertEquals("Saved Location", result)
    }

    @Test
    fun `savedName null falls back to timezone parsing`() = runTest {
        coEvery { androidGeocoder.getFromLocation(55.75, 37.62, 1, Locale.ENGLISH) } returns emptyList()

        val result = cityNameResolver.resolveCityName(55.75, 37.62, null, "Europe/Moscow")

        assertEquals("Europe", result)
    }

    @Test
    fun `timezone empty string falls back to Unknown`() = runTest {
        coEvery { androidGeocoder.getFromLocation(55.75, 37.62, 1, Locale.ENGLISH) } returns emptyList()

        val result = cityNameResolver.resolveCityName(55.75, 37.62, null, "")

        assertEquals("Unknown", result)
    }

    @Test
    fun `geocoder throws exception falls back to savedName`() = runTest {
        coEvery { androidGeocoder.getFromLocation(55.75, 37.62, 1, Locale.ENGLISH) } throws Exception("Geocoder failed")

        val result = cityNameResolver.resolveCityName(55.75, 37.62, "Fallback", "Europe/Moscow")

        assertEquals("Fallback", result)
    }

    @Test
    fun `geocoder null address falls back to savedName`() = runTest {
        coEvery { androidGeocoder.getFromLocation(55.75, 37.62, 1, Locale.ENGLISH) } returns null

        val result = cityNameResolver.resolveCityName(55.75, 37.62, "Fallback", "Europe/Moscow")

        assertEquals("Fallback", result)
    }
}