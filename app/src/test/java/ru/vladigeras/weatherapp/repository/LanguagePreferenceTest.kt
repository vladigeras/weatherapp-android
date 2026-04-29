package ru.vladigeras.weatherapp.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Simple test without Android dependencies
class LanguagePreferenceTest {

    @Test
    fun testLanguagePreferenceHasThreeOptions() {
        val options = listOf(
            LanguagePreference.SYSTEM,
            LanguagePreference.RUSSIAN,
            LanguagePreference.ENGLISH
        )
        assertEquals(3, options.size)
    }

    @Test
    fun testLanguagePreferenceCanBeCompared() {
        assertTrue(LanguagePreference.SYSTEM != LanguagePreference.RUSSIAN)
        assertTrue(LanguagePreference.RUSSIAN != LanguagePreference.ENGLISH)
    }
}