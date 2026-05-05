package ru.vladigeras.weatherapp.util

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
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

    fun cleanupWithRetry(tempDir: File, maxRetries: Int = 3) {
        var lastError: Exception? = null
        repeat(maxRetries) { attempt ->
            try {
                tempDir.deleteRecursively()
                return
            } catch (e: Exception) {
                lastError = e
                if (attempt < maxRetries - 1) {
                    runBlocking { delay(100) }
                }
            }
        }
        // Log warning but don't fail the test
        println("Warning: Failed to delete temp dir ${tempDir.absolutePath}: ${lastError?.message}")
    }
}