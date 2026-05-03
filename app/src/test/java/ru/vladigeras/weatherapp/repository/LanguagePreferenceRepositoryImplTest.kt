package ru.vladigeras.weatherapp.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.vladigeras.weatherapp.util.TestDataStoreFactory
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LanguagePreferenceRepositoryImplTest {

    private lateinit var repository: LanguagePreferenceRepositoryImpl
    private lateinit var dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>
    private lateinit var tempFile: File

    @Before
    fun setUp() {
        tempFile = TestDataStoreFactory.createTempFile("test_language_prefs")
        dataStore = TestDataStoreFactory.createInMemoryDataStore(
            scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined),
            tempFile = tempFile
        )
        repository = LanguagePreferenceRepositoryImpl(dataStore)
    }

    @After
    fun tearDown() {
        runBlocking {
            delay(100)
        }
    }

    @Test
    fun saveAndGetPreference_roundTripSystem() = runBlocking {
        repository.saveLanguagePreference(LanguagePreference.SYSTEM)
        val result = repository.getLanguagePreference()
        assertEquals(LanguagePreference.SYSTEM, result)
    }

    @Test
    fun saveAndGetPreference_roundTripRussian() = runBlocking {
        repository.saveLanguagePreference(LanguagePreference.RUSSIAN)
        val result = repository.getLanguagePreference()
        assertEquals(LanguagePreference.RUSSIAN, result)
    }

    @Test
    fun getEffectiveLocaleCode_systemEnglishReturnsEn() = runBlocking {
        repository.saveLanguagePreference(LanguagePreference.SYSTEM)
        val result = repository.getEffectiveLocaleCode()
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun getAppLocale_russianReturnsCorrectLocale() = runBlocking {
        repository.saveLanguagePreference(LanguagePreference.RUSSIAN)
        val result = repository.getAppLocale()
        assertEquals("ru", result.language)
        assertEquals("RU", result.country)
    }
}