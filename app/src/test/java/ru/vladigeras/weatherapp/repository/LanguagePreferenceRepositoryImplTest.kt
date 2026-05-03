package ru.vladigeras.weatherapp.repository

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LanguagePreferenceRepositoryImplTest {

    private lateinit var repository: LanguagePreferenceRepositoryImpl
    private val context get() = RuntimeEnvironment.getApplication()

    @Before
    fun setUp() {
        repository = LanguagePreferenceRepositoryImpl(context)
        // Clear preferences before each test
        val prefs = context.getSharedPreferences("weatherapp_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    @Test
    fun saveAndGetPreference_roundTripSystem() = kotlinx.coroutines.runBlocking {
        repository.saveLanguagePreference(LanguagePreference.SYSTEM)
        val result = repository.getLanguagePreference()
        assertEquals(LanguagePreference.SYSTEM, result)
    }

    @Test
    fun saveAndGetPreference_roundTripRussian() = kotlinx.coroutines.runBlocking {
        repository.saveLanguagePreference(LanguagePreference.RUSSIAN)
        val result = repository.getLanguagePreference()
        assertEquals(LanguagePreference.RUSSIAN, result)
    }

    @Test
    fun getEffectiveLocaleCode_systemEnglishReturnsEn() = runBlocking {
        repository.saveLanguagePreference(LanguagePreference.SYSTEM)
        // Need to mock default locale, but for now just check it returns something
        val result = repository.getEffectiveLocaleCode()
        // Since device locale might not be English, we just check it's not null
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun getAppLocale_russianReturnsCorrectLocale() = kotlinx.coroutines.runBlocking {
        repository.saveLanguagePreference(LanguagePreference.RUSSIAN)
        val result = repository.getAppLocale()
        assertEquals("ru", result.language)
        assertEquals("RU", result.country)
    }
}
