package com.nghianguyen.gomuni.features.transit.routes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import com.nghianguyen.gomuni.app.navigation.TransitNavigation
import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.features.transit.testing.FakeTransitRepository
import com.nghianguyen.gomuni.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filter
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

    @After
    fun clearViewModels() {
        store.clear()
    }

    private fun <T : ViewModel> retain(model: T): T {
        store.put("${model.javaClass.name}:${System.identityHashCode(model)}", model)
        return model
    }

    @Test
    fun `routes restore sort and filter and refresh without hiding content`() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.lines.value =
                listOf(TransitLine("38", "Geary", "bus"), TransitLine("1", "California", "bus"))
            val handle = SavedStateHandle(mapOf("sort" to "NAME", "filter" to "Rapid"))
            val refresh = CompletableDeferred<Unit>()
            repository.refreshLines = { refresh.await() }
            val viewModel = retain(TransitLinesViewModel(repository, handle))
            runCurrent()
            assertEquals(listOf("1", "38"), viewModel.uiState.value.lines.map { it.id })
            assertEquals("Rapid", viewModel.uiState.value.filter)
            assertTrue(viewModel.uiState.value.isRefreshing)
            viewModel.onAction(TransitLinesAction.Refresh)
            assertEquals(1, repository.lineRefreshes)
            refresh.complete(Unit)
            runCurrent()
            viewModel.onAction(TransitLinesAction.ChangeSort(TransitLineSortOption.ID))
            viewModel.onAction(TransitLinesAction.ChangeFilter("All"))
            assertEquals("ID", handle.get<String>("sort"))
            assertEquals("All", handle.get<String>("filter"))
            assertFalse(viewModel.uiState.value.isRefreshing)
        }

    @Test
    fun `route selection emits only for known route and does not load stops`() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.lines.value = listOf(TransitLine("38", "Geary", "bus"))
            val viewModel = retain(TransitLinesViewModel(repository, SavedStateHandle()))
            runCurrent()
            val events = mutableListOf<TransitNavigation>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.navigationEvents.toList(events)
            }
            viewModel.onAction(TransitLinesAction.SelectRoute("missing"))
            viewModel.onAction(TransitLinesAction.SelectRoute("38"))
            runCurrent()
            assertEquals(listOf(TransitNavigation.OpenRoute("SF", "38")), events)
            assertEquals(0, repository.stopRefreshes)
        }
}
