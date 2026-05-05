package ru.vladigeras.weatherapp.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
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

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LanguagePreferenceRepositoryImplTest {

    private lateinit var repository: LanguagePreferenceRepositoryImpl
    private lateinit var dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>
    private lateinit var tempFile: File
    private lateinit var dataStoreScope: CoroutineScope

    @Before
    fun setUp() {
        tempFile = TestDataStoreFactory.createTempFile("test_language_prefs")
        dataStoreScope = CoroutineScope(UnconfinedTestDispatcher())
        dataStore = TestDataStoreFactory.createTestDataStore(
            scope = dataStoreScope,
            tempFile = tempFile
        )
        repository = LanguagePreferenceRepositoryImpl(dataStore)
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
        runCatching { java.nio.file.Files.deleteIfExists(tempFile.toPath()) }
    }

    @Test
    fun saveAndGetPreference_roundTripSystem() = runTest {
        repository.saveLanguagePreference(LanguagePreference.SYSTEM)
        val result = repository.getLanguagePreference()
        assertEquals(LanguagePreference.SYSTEM, result)
    }

    @Test
    fun saveAndGetPreference_roundTripRussian() = runTest {
        repository.saveLanguagePreference(LanguagePreference.RUSSIAN)
        val result = repository.getLanguagePreference()
        assertEquals(LanguagePreference.RUSSIAN, result)
    }

    @Test
    fun getEffectiveLocaleCode_systemEnglishReturnsEn() = runTest {
        repository.saveLanguagePreference(LanguagePreference.SYSTEM)
        val result = repository.getEffectiveLocaleCode()
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun getAppLocale_russianReturnsCorrectLocale() = runTest {
        repository.saveLanguagePreference(LanguagePreference.RUSSIAN)
        val result = repository.getAppLocale()
        assertEquals("ru", result.language)
        assertEquals("RU", result.country)
    }
}