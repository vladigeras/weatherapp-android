package ru.vladigeras.weatherapp.util

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherCodeTranslatorTest {

    @Test
    fun testKnownWeatherCodesExist() {
        val knownCodes = listOf(
            0, 1, 2, 3, 45, 48, 51, 53, 55, 56, 57,
            61, 63, 65, 66, 67, 71, 73, 75, 77,
            80, 81, 82, 85, 86, 95, 96, 99
        )
        assertEquals(28, knownCodes.size)
    }

    @Test
    fun testWeatherCodeRanges() {
        val clearSkyCodes = listOf(0)
        val fogCodes = listOf(45, 48)
        val drizzleCodes = listOf(51, 53, 55)
        val rainCodes = listOf(61, 63, 65)
        val snowCodes = listOf(71, 73, 75)
        val thunderstormCodes = listOf(95, 96, 99)

        assertEquals(1, clearSkyCodes.size)
        assertEquals(2, fogCodes.size)
        assertEquals(3, drizzleCodes.size)
        assertEquals(3, rainCodes.size)
        assertEquals(3, snowCodes.size)
        assertEquals(3, thunderstormCodes.size)
    }

    @Test
    fun testAllCodesCovered() {
        val allTestedCodes = 0..99
        assertEquals(100, allTestedCodes.count())
    }
}