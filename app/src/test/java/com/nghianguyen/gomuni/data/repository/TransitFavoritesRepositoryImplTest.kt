package com.nghianguyen.gomuni.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.nghianguyen.gomuni.data.datasource.TransitFavoritesLocalDataSource
import com.nghianguyen.gomuni.data.datasource.TransitFavoritesLocalDataSourceImpl
import com.nghianguyen.gomuni.domain.model.FavoritesError
import com.nghianguyen.gomuni.domain.model.Outcome
import com.nghianguyen.gomuni.testing.RecordingErrorLogger
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
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
            val repository =
                TransitFavoritesRepositoryImpl(
                    TransitFavoritesLocalDataSourceImpl(store),
                    RecordingErrorLogger(),
                )
            val first = async { repository.setFavorite("38", true) }
            val second = async { repository.setFavorite("unavailable", true) }
            first.await()
            second.await()
            repository.setFavorite("38", true)
            assertEquals(
                Outcome.Success(setOf("38", "unavailable")),
                repository.observeFavoriteIds().first(),
            )
        } finally {
            scope.cancel()
            requireNotNull(scope.coroutineContext[Job]).join()
        }
        val reopenedScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val reopened =
                PreferenceDataStoreFactory.create(scope = reopenedScope, produceFile = { file })
            val repository =
                TransitFavoritesRepositoryImpl(
                    TransitFavoritesLocalDataSourceImpl(reopened),
                    RecordingErrorLogger(),
                )
            assertEquals(
                Outcome.Success(setOf("38", "unavailable")),
                repository.observeFavoriteIds().first(),
            )
            repository.setFavorite("38", false)
            assertEquals(
                Outcome.Success(setOf("unavailable")),
                repository.observeFavoriteIds().first(),
            )
        } finally {
            reopenedScope.cancel()
            requireNotNull(reopenedScope.coroutineContext[Job]).join()
        }
    }

    @Test
    fun `storage failures are translated without reporting successful empty favorites`() = runTest {
        val store =
            object : DataStore<Preferences> {
                override val data =
                    flow<Preferences> {
                        throw IOException("Unavailable")
                    }

                override suspend fun updateData(
                    transform:
                        suspend (
                            Preferences,
                        ) -> Preferences,
                ): Preferences {
                    throw IOException("Unavailable")
                }
            }
        val repository =
            TransitFavoritesRepositoryImpl(
                TransitFavoritesLocalDataSourceImpl(store),
                RecordingErrorLogger(),
            )
        assertEquals(
            listOf(Outcome.Failure(FavoritesError.Storage)),
            repository.observeFavoriteIds().toList(),
        )
        assertEquals(Outcome.Failure(FavoritesError.Storage), repository.setFavorite("38", true))
    }

    @Test
    fun `cancellation and unexpected failures propagate without storage failure logging`() =
        runTest {
            listOf(CancellationException("Cancelled"), IllegalStateException("Broken source"))
                .forEach { failure ->
                    val local =
                        object : TransitFavoritesLocalDataSource {
                            override fun observeFavoriteIds() = flow<Set<String>> { throw failure }

                            override suspend fun setFavorite(lineId: String, favorite: Boolean) {
                                throw failure
                            }
                        }
                    val logger = RecordingErrorLogger()
                    val repository = TransitFavoritesRepositoryImpl(local, logger)
                    val operations: List<suspend () -> Unit> =
                        listOf(
                            { repository.observeFavoriteIds().first() },
                            { repository.setFavorite("38", true) },
                        )
                    operations.forEach { operation ->
                        try {
                            operation()
                            fail("Expected original exception")
                        } catch (actual: Exception) {
                            assertSame(failure, actual)
                        }
                    }
                    assertTrue(logger.errors.isEmpty())
                }
        }
}
