package ru.vladigeras.weatherapp.util

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.File

object TestDataStoreFactory {
    fun createInMemoryDataStore(
        scope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined),
        tempFile: File
    ): DataStore<Preferences> {
        tempFile.deleteOnExit()

        return PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { tempFile }
        )
    }

    fun createTempFile(prefix: String): File {
        return File.createTempFile(prefix, ".preferences_pb").apply {
            deleteOnExit()
        }
    }
}