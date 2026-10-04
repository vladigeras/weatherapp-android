package ru.vladigeras.weatherapp.util

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import java.io.File
import java.util.UUID

object TestDataStoreFactory {
    
    fun createTestDataStore(
        scope: CoroutineScope,
        tempDir: File
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File(tempDir, "datastore.preferences_pb") },
            corruptionHandler = null,
            migrations = emptyList()
        )
    }

    fun createTempDir(prefix: String): File {
        val dir = File(System.getProperty("java.io.tmpdir"), "$prefix-${UUID.randomUUID()}")
        dir.mkdirs()
        dir.deleteOnExit()
        return dir
    }

    fun cleanup(tempDir: File) {
        check(tempDir.deleteRecursively()) { "Failed to delete test directory ${tempDir.absolutePath}" }
    }
}
