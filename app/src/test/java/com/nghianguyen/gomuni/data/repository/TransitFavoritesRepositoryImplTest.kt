package com.nghianguyen.gomuni.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.nghianguyen.gomuni.testing.RecordingErrorLogger
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TransitFavoritesRepositoryImplTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun `favorites persist across reopening and concurrent independent writes`() = runTest {
        val file = temporaryFolder.root.resolve("favorites.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
            val repository = TransitFavoritesRepositoryImpl(store, RecordingErrorLogger())
            val first = async { repository.setFavorite("38", true) }
            val second = async { repository.setFavorite("unavailable", true) }
            first.await()
            second.await()
            repository.setFavorite("38", true)
            assertEquals(setOf("38", "unavailable"), repository.observeFavoriteIds().first())
        } finally {
            scope.cancel()
            requireNotNull(scope.coroutineContext[Job]).join()
        }
        val reopenedScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val reopened =
                PreferenceDataStoreFactory.create(scope = reopenedScope, produceFile = { file })
            val repository = TransitFavoritesRepositoryImpl(reopened, RecordingErrorLogger())
            assertEquals(setOf("38", "unavailable"), repository.observeFavoriteIds().first())
            repository.setFavorite("38", false)
            assertEquals(setOf("unavailable"), repository.observeFavoriteIds().first())
        } finally {
            reopenedScope.cancel()
            requireNotNull(reopenedScope.coroutineContext[Job]).join()
        }
    }

    @Test
    fun `storage failures are translated without reporting successful empty favorites`() = runTest {
        val store =
            object :
                androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> {
                override val data =
                    kotlinx.coroutines.flow.flow<androidx.datastore.preferences.core.Preferences> {
                        throw IOException("Unavailable")
                    }

                override suspend fun updateData(
                    transform:
                        suspend (
                            androidx.datastore.preferences.core.Preferences,
                        ) -> androidx.datastore.preferences.core.Preferences,
                ): androidx.datastore.preferences.core.Preferences {
                    throw IOException("Unavailable")
                }
            }
        val repository = TransitFavoritesRepositoryImpl(store, RecordingErrorLogger())
        val operations: List<suspend () -> Unit> =
            listOf(
                { repository.observeFavoriteIds().first() },
                { repository.setFavorite("38", true) },
            )
        operations.forEach { operation ->
            try {
                operation()
                org.junit.Assert.fail("Expected storage failure")
            } catch (_: com.nghianguyen.gomuni.domain.model.FavoritesStorageException) {
                // Expected typed boundary; never an empty successful result.
            }
        }
    }
}
