package com.nghianguyen.gomuni.features.transit.stops

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import com.nghianguyen.gomuni.app.navigation.TransitNavigation
import com.nghianguyen.gomuni.domain.model.TransitError
import com.nghianguyen.gomuni.domain.model.TransitStop
import com.nghianguyen.gomuni.features.transit.model.LoadStatus
import com.nghianguyen.gomuni.features.transit.model.RouteKey
import com.nghianguyen.gomuni.features.transit.testing.FakeTransitRepository
import com.nghianguyen.gomuni.testing.MainDispatcherRule
import com.nghianguyen.gomuni.testing.RecordingErrorLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RouteStopsViewModelTest {
    private val route = RouteKey("SF", "38")
    private val testStop = TransitStop("123", "Market", 37.78, -122.41, null, "38")

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
    fun `initial stop loading resolves removed selection at completion`() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.stops.value = listOf(testStop)
            val refresh = CompletableDeferred<Unit>()
            repository.loadStops = { refresh.await() }
            val saved = SavedStateHandle(mapOf("selected_stop" to testStop.id))
            val viewModel =
                retain(RouteStopsViewModel(repository, saved, route, RecordingErrorLogger()))
            runCurrent()
            repository.stops.value = emptyList()
            runCurrent()
            assertTrue(viewModel.uiState.value.stops.isEmpty())
            assertEquals(testStop.id, viewModel.uiState.value.selectedStopId)
            refresh.complete(Unit)
            runCurrent()
            assertTrue(viewModel.uiState.value.stops.isEmpty())
            assertNull(viewModel.uiState.value.selectedStopId)
            assertNull(saved.get<String>("selected_stop"))
            assertEquals(LoadStatus.LOADED, viewModel.uiState.value.status)
        }

    @Test
    fun `initial stop load failure retains available content and can retry`() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.stops.value = listOf(testStop)
            repository.stopLoadFailure = TransitError.Network
            val viewModel =
                retain(
                    RouteStopsViewModel(
                        repository,
                        SavedStateHandle(),
                        route,
                        RecordingErrorLogger(),
                    ),
                )
            runCurrent()
            assertEquals(listOf(testStop), viewModel.uiState.value.stops)
            assertEquals(LoadStatus.FAILED, viewModel.uiState.value.status)
            assertNotNull(viewModel.uiState.value.error)
            repository.stopLoadFailure = null
            viewModel.onAction(RouteStopsAction.RetryLoad)
            runCurrent()
            assertNull(viewModel.uiState.value.error)
            assertEquals(LoadStatus.LOADED, viewModel.uiState.value.status)
        }

    @Test
    fun `stop selection opens and dismisses a sheet without navigation`() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.stops.value = listOf(testStop)
            val saved = SavedStateHandle()
            val viewModel =
                retain(RouteStopsViewModel(repository, saved, route, RecordingErrorLogger()))
            runCurrent()
            val events = mutableListOf<TransitNavigation>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.navigationEvents.toList(events)
            }
            viewModel.onAction(RouteStopsAction.SelectStop("missing"))
            assertNull(viewModel.uiState.value.arrivalsStopId)
            viewModel.onAction(RouteStopsAction.SelectStop(testStop.id))
            viewModel.onAction(RouteStopsAction.SelectStop(testStop.id))
            assertEquals(testStop.id, viewModel.uiState.value.arrivalsStopId)
            assertEquals(testStop.id, saved.get<String>("arrivals_stop"))
            assertTrue(events.isEmpty())
            viewModel.onAction(RouteStopsAction.DismissArrivals("obsolete"))
            assertEquals(testStop.id, viewModel.uiState.value.arrivalsStopId)
            viewModel.onAction(RouteStopsAction.Back)
            assertNull(viewModel.uiState.value.arrivalsStopId)
            assertEquals(testStop.id, viewModel.uiState.value.selectedStopId)
            assertNull(saved.get<String>("arrivals_stop"))
            assertTrue(events.isEmpty())
            viewModel.onAction(RouteStopsAction.Back)
            assertEquals(listOf(TransitNavigation.Back), events)
        }

    @Test
    fun `external sheet selection restores and rejects old dismissals`() =
        runTest(mainDispatcherRule.dispatcher) {
            val saved = SavedStateHandle()
            val viewModel =
                retain(RouteStopsViewModel(repository, saved, route, RecordingErrorLogger()))
            runCurrent()
            viewModel.onAction(RouteStopsAction.OpenArrivals("123"))
            viewModel.onAction(RouteStopsAction.OpenArrivals("456"))
            viewModel.onAction(RouteStopsAction.DismissArrivals("123"))
            val restored =
                retain(RouteStopsViewModel(repository, saved, route, RecordingErrorLogger()))
            assertEquals("456", restored.uiState.value.arrivalsStopId)
            assertEquals("456", restored.uiState.value.selectedStopId)
            restored.onAction(RouteStopsAction.DismissArrivals("456"))
            restored.onAction(RouteStopsAction.OpenArrivals(""))
            assertNull(restored.uiState.value.arrivalsStopId)
            assertEquals("456", restored.uiState.value.selectedStopId)
        }
}
