package com.nghianguyen.gomuni.features.transit.arrivals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import com.nghianguyen.gomuni.domain.model.*
import com.nghianguyen.gomuni.features.transit.model.LoadStatus
import com.nghianguyen.gomuni.features.transit.testing.FakeTransitRepository
import com.nghianguyen.gomuni.testing.MainDispatcherRule
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StopArrivalsViewModelTest {
    private val testStop = TransitStop("123", "Market", 37.78, -122.41, null, "38")
    private val testArrival =
        BusArrival("38", "Geary", "2026-10-14T12:00:00Z", null, "bus1", 37.78, -122.41)

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
    fun `arrivals do not refresh parent and retain predictions on failure`() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.arrivals = { listOf(testArrival) }
            val viewModel = retain(StopArrivalsViewModel(repository, testStop.id))
            viewModel.updateContext(testStop, true)
            runCurrent()
            assertEquals(listOf(testArrival), viewModel.uiState.value.predictions)
            repository.arrivals = { throw TransitError.Network }
            viewModel.onAction(StopArrivalsAction.Refresh)
            runCurrent()
            assertEquals(listOf(testArrival), viewModel.uiState.value.predictions)
            assertEquals(LoadStatus.FAILED, viewModel.uiState.value.status)
            assertEquals(0, repository.stopRefreshes)
        }

    @Test
    fun `removed stop rejects result from non cooperative request`() =
        runTest(mainDispatcherRule.dispatcher) {
            val result = CompletableDeferred<List<BusArrival>>()
            repository.arrivals = { withContext(NonCancellable) { result.await() } }
            val viewModel = retain(StopArrivalsViewModel(repository, testStop.id))
            viewModel.updateContext(testStop, true)
            runCurrent()
            viewModel.updateContext(null, true)
            result.complete(listOf(testArrival))
            runCurrent()
            assertTrue(viewModel.uiState.value.predictions.isEmpty())
            assertNull(viewModel.uiState.value.stop)
            assertEquals(LoadStatus.FAILED, viewModel.uiState.value.status)
            assertFalse(viewModel.uiState.value.isRefreshing)
        }

    @Test
    fun `tracking validates prediction and hands off once without restoration`() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.arrivals = { listOf(testArrival) }
            val viewModel = retain(StopArrivalsViewModel(repository, testStop.id))
            viewModel.updateContext(testStop, true)
            runCurrent()
            viewModel.onAction(
                StopArrivalsAction.Track(
                    ArrivalPredictionKey.from(testArrival.copy(vehicleRef = "unknown")),
                ),
            )
            assertNull(viewModel.trackingRequest.value)
            viewModel.onAction(StopArrivalsAction.Track(ArrivalPredictionKey.from(testArrival)))
            val request = requireNotNull(viewModel.trackingRequest.value)
            assertEquals(request, viewModel.takeTrackingRequest())
            assertNull(viewModel.takeTrackingRequest())
            val restored = retain(StopArrivalsViewModel(repository, testStop.id))
            assertNull(restored.trackingRequest.value)
        }

    @Test
    fun `popped view model cancels its request`() =
        runTest(mainDispatcherRule.dispatcher) {
            var cancelled = false
            repository.arrivals = {
                try {
                    awaitCancellation()
                } finally {
                    cancelled = true
                }
            }
            val viewModel = retain(StopArrivalsViewModel(repository, testStop.id))
            viewModel.updateContext(testStop, true)
            runCurrent()
            store.clear()
            runCurrent()
            assertTrue(cancelled)
        }
}
