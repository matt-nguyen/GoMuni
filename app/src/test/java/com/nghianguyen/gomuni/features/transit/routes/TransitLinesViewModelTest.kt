package com.nghianguyen.gomuni.features.transit.routes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import com.nghianguyen.gomuni.app.navigation.TransitNavigation
import com.nghianguyen.gomuni.domain.model.FavoritesError
import com.nghianguyen.gomuni.domain.model.Outcome
import com.nghianguyen.gomuni.domain.model.TransitError
import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.domain.repository.TransitFavoritesRepository
import com.nghianguyen.gomuni.features.transit.model.LoadStatus
import com.nghianguyen.gomuni.features.transit.testing.FakeTransitFavoritesRepository
import com.nghianguyen.gomuni.features.transit.testing.FakeTransitRepository
import com.nghianguyen.gomuni.testing.MainDispatcherRule
import com.nghianguyen.gomuni.testing.RecordingErrorLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransitLinesViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private val store = ViewModelStore()
    private val repository = FakeTransitRepository()
    private val favorites = FakeTransitFavoritesRepository()

    @After
    fun clearViewModels() {
        store.clear()
    }

    private fun <T : ViewModel> retain(model: T): T {
        store.put("${model.javaClass.name}:${System.identityHashCode(model)}", model)
        return model
    }

    @Test
    fun `routes restore sort and filter and deduplicate initial loading`() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.lines.value =
                listOf(TransitLine("38", "Geary", "bus"), TransitLine("1", "California", "bus"))
            val handle = SavedStateHandle(mapOf("sort" to "NAME", "filter" to "Rapid"))
            val refresh = CompletableDeferred<Unit>()
            repository.loadLines = { refresh.await() }
            val viewModel =
                retain(
                    TransitLinesViewModel(
                        repository,
                        handle,
                        favorites,
                        RecordingErrorLogger(),
                    ),
                )
            runCurrent()
            assertEquals(listOf("1", "38"), viewModel.uiState.value.lines.map { it.id })
            assertEquals("Rapid", viewModel.uiState.value.filter)
            assertEquals(
                LoadStatus.LOADING,
                viewModel.uiState.value.status,
            )
            viewModel.onAction(TransitLinesAction.RetryLoad)
            assertEquals(1, repository.lineLoads)
            refresh.complete(Unit)
            runCurrent()
            viewModel.onAction(TransitLinesAction.ChangeSort(TransitLineSortOption.ID))
            viewModel.onAction(TransitLinesAction.ChangeFilter("All"))
            assertEquals("ID", handle.get<String>("sort"))
            assertEquals("All", handle.get<String>("filter"))
            assertEquals(
                LoadStatus.LOADED,
                viewModel.uiState.value.status,
            )
        }

    @Test
    fun `route selection emits only for known route and does not load stops`() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.lines.value = listOf(TransitLine("38", "Geary", "bus"))
            val viewModel =
                retain(
                    TransitLinesViewModel(
                        repository,
                        SavedStateHandle(),
                        favorites,
                        RecordingErrorLogger(),
                    ),
                )
            runCurrent()
            val events = mutableListOf<TransitNavigation>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.navigationEvents.toList(events)
            }
            viewModel.onAction(TransitLinesAction.SelectRoute("missing"))
            viewModel.onAction(TransitLinesAction.SelectRoute("38"))
            runCurrent()
            assertEquals(listOf(TransitNavigation.OpenRoute("SF", "38")), events)
            assertEquals(0, repository.stopLoads)
        }

    @Test
    fun `favorite is confirmed only after save and duplicate pending taps are ignored`() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.lines.value = listOf(TransitLine("38", "Geary", "bus"))
            val gate = CompletableDeferred<Unit>()
            favorites.beforeWrite = { gate.await() }
            val viewModel =
                retain(
                    TransitLinesViewModel(
                        repository,
                        SavedStateHandle(),
                        favorites,
                        RecordingErrorLogger(),
                    ),
                )
            runCurrent()
            viewModel.onAction(TransitLinesAction.SetFavorite("38", true))
            viewModel.onAction(TransitLinesAction.SetFavorite("38", true))
            runCurrent()
            assertEquals(1, favorites.writes)
            assertEquals(setOf("38"), viewModel.uiState.value.pendingFavoriteIds)
            assertTrue(viewModel.uiState.value.favoriteIds.isEmpty())
            gate.complete(Unit)
            runCurrent()
            assertEquals(setOf("38"), viewModel.uiState.value.favoriteIds)
            assertTrue(viewModel.uiState.value.pendingFavoriteIds.isEmpty())
            viewModel.onAction(TransitLinesAction.SetFavorite("38", false))
            runCurrent()
            assertTrue(viewModel.uiState.value.favoriteIds.isEmpty())
        }

    @Test
    fun `failed favorite write retains confirmed state and can retry`() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.lines.value = listOf(TransitLine("38", "Geary", "bus"))
            favorites.beforeWrite = {
                favorites.writeFailure = FavoritesError.Storage
            }
            val viewModel =
                retain(
                    TransitLinesViewModel(
                        repository,
                        SavedStateHandle(),
                        favorites,
                        RecordingErrorLogger(),
                    ),
                )
            runCurrent()
            viewModel.onAction(TransitLinesAction.SetFavorite("38", true))
            runCurrent()
            assertTrue(viewModel.uiState.value.favoriteIds.isEmpty())
            assertTrue(viewModel.uiState.value.pendingFavoriteIds.isEmpty())
            assertEquals(FavoriteError.WRITE, viewModel.uiState.value.favoriteError)
            favorites.beforeWrite = {}
            favorites.writeFailure = null
            viewModel.onAction(TransitLinesAction.SetFavorite("38", true))
            runCurrent()
            assertEquals(setOf("38"), viewModel.uiState.value.favoriteIds)
            assertEquals(null, viewModel.uiState.value.favoriteError)
        }

    @Test
    fun `favorites read failure permits browsing and retry without discarding unavailable preferences`() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.lines.value = listOf(TransitLine("38", "Geary", "bus"))
            favorites.ids.value = setOf("missing")
            favorites.readFailure = FavoritesError.Storage
            val viewModel =
                retain(
                    TransitLinesViewModel(
                        repository,
                        SavedStateHandle(),
                        favorites,
                        RecordingErrorLogger(),
                    ),
                )
            runCurrent()
            assertFalse(viewModel.uiState.value.favoritesReady)
            assertEquals(FavoriteError.READ, viewModel.uiState.value.favoriteError)
            assertEquals(1, viewModel.uiState.value.lines.size)
            viewModel.onAction(TransitLinesAction.SetFavorite("38", true))
            assertEquals(0, favorites.writes)
            favorites.readFailure = null
            viewModel.onAction(TransitLinesAction.RetryFavorites)
            runCurrent()
            assertTrue(viewModel.uiState.value.favoritesReady)
            assertEquals(setOf("missing"), viewModel.uiState.value.favoriteIds)
        }

    @Test
    fun `read retry remains available when pending write fails after observation failure`() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.lines.value = listOf(TransitLine("38", "Geary", "bus"))
            val writeGate = CompletableDeferred<Unit>()
            favorites.beforeWrite = {
                writeGate.await()
                favorites.writeFailure = FavoritesError.Storage
            }
            val viewModel =
                retain(
                    TransitLinesViewModel(
                        repository,
                        SavedStateHandle(),
                        favorites,
                        RecordingErrorLogger(),
                    ),
                )
            runCurrent()
            viewModel.onAction(TransitLinesAction.SetFavorite("38", true))
            runCurrent()
            assertEquals(setOf("38"), viewModel.uiState.value.pendingFavoriteIds)

            favorites.readFailure = FavoritesError.Storage
            runCurrent()
            assertFalse(viewModel.uiState.value.favoritesReady)
            assertEquals(FavoriteError.READ, viewModel.uiState.value.favoriteError)
            writeGate.complete(Unit)
            runCurrent()
            assertFalse(viewModel.uiState.value.favoritesReady)
            assertEquals(FavoriteError.READ, viewModel.uiState.value.favoriteError)
            assertTrue(viewModel.uiState.value.pendingFavoriteIds.isEmpty())

            favorites.readFailure = null
            favorites.beforeWrite = {}
            favorites.writeFailure = null
            viewModel.onAction(TransitLinesAction.RetryFavorites)
            runCurrent()
            assertTrue(viewModel.uiState.value.favoritesReady)
            assertEquals(null, viewModel.uiState.value.favoriteError)
            viewModel.onAction(TransitLinesAction.SetFavorite("38", true))
            runCurrent()
            assertEquals(setOf("38"), viewModel.uiState.value.favoriteIds)
        }

    @Test
    fun `initial line load failure retries and successful load ignores further retries`() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.lineLoadFailure = TransitError.Network
            val viewModel =
                retain(
                    TransitLinesViewModel(
                        repository,
                        SavedStateHandle(),
                        favorites,
                        RecordingErrorLogger(),
                    ),
                )
            runCurrent()
            assertEquals(
                LoadStatus.FAILED,
                viewModel.uiState.value.status,
            )
            repository.lineLoadFailure = null
            viewModel.onAction(TransitLinesAction.RetryLoad)
            runCurrent()
            viewModel.onAction(TransitLinesAction.RetryLoad)
            runCurrent()
            assertEquals(2, repository.lineLoads)
        }

    @Test
    fun `failed write confirmation does not disable an active favorites observer`() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.lines.value = listOf(TransitLine("38", "Geary", "bus"))
            val confirmationFailure =
                object : TransitFavoritesRepository by favorites {
                    private var observations = 0

                    override fun observeFavoriteIds(): Flow<Outcome<Set<String>, FavoritesError>> {
                        observations++
                        return if (observations == 1) favorites.observeFavoriteIds()
                        else flowOf(Outcome.Failure(FavoritesError.Storage))
                    }
                }
            val viewModel =
                retain(
                    TransitLinesViewModel(
                        repository,
                        SavedStateHandle(),
                        confirmationFailure,
                        RecordingErrorLogger(),
                    ),
                )
            runCurrent()
            viewModel.onAction(TransitLinesAction.SetFavorite("38", true))
            runCurrent()

            assertEquals(FavoriteError.WRITE, viewModel.uiState.value.favoriteError)
            assertTrue(viewModel.uiState.value.favoritesReady)
            assertTrue(viewModel.uiState.value.pendingFavoriteIds.isEmpty())
        }
}
