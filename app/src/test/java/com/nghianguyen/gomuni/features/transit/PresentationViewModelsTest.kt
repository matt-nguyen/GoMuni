package com.nghianguyen.gomuni.features.transit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import com.nghianguyen.gomuni.domain.model.*
import com.nghianguyen.gomuni.domain.repository.TransitRepository
import com.nghianguyen.gomuni.app.navigation.TransitNavigation
import com.nghianguyen.gomuni.features.transit.arrivals.ArrivalPredictionKey
import com.nghianguyen.gomuni.features.transit.arrivals.StopArrivalsAction
import com.nghianguyen.gomuni.features.transit.arrivals.StopArrivalsViewModel
import com.nghianguyen.gomuni.features.transit.model.LoadStatus
import com.nghianguyen.gomuni.features.transit.model.RouteKey
import com.nghianguyen.gomuni.features.transit.routes.TransitLineSortOption
import com.nghianguyen.gomuni.features.transit.routes.TransitLinesAction
import com.nghianguyen.gomuni.features.transit.routes.TransitLinesViewModel
import com.nghianguyen.gomuni.features.transit.stops.RouteStopsAction
import com.nghianguyen.gomuni.features.transit.stops.RouteStopsViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PresentationViewModelsTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val repository = FakeTransitRepository()
    private val route = RouteKey("SF", "38")

    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { store.clear(); Dispatchers.resetMain() }
    private fun <T : ViewModel> retain(model: T): T {
        store.put("${model.javaClass.name}:${System.identityHashCode(model)}", model)
        return model
    }

    @Test fun routesRestoreSortAndFilterAndRefreshWithoutHidingContent() = runTest(dispatcher) {
        repository.lines.value = listOf(TransitLine("38", "Geary", "bus"), TransitLine("1", "California", "bus"))
        val handle = SavedStateHandle(mapOf("sort" to "NAME", "filter" to "Rapid"))
        val refresh = CompletableDeferred<Unit>()
        repository.refreshLines = { refresh.await() }
        val vm = retain(TransitLinesViewModel(repository, handle))
        runCurrent()
        assertEquals(listOf("1", "38"), vm.uiState.value.lines.map { it.id })
        assertEquals("Rapid", vm.uiState.value.filter)
        assertTrue(vm.uiState.value.isRefreshing)
        vm.onAction(TransitLinesAction.Refresh)
        assertEquals(1, repository.lineRefreshes)
        refresh.complete(Unit)
        runCurrent()
        vm.onAction(TransitLinesAction.ChangeSort(TransitLineSortOption.ID))
        vm.onAction(TransitLinesAction.ChangeFilter("All"))
        assertEquals("ID", handle.get<String>("sort"))
        assertEquals("All", handle.get<String>("filter"))
        assertFalse(vm.uiState.value.isRefreshing)
    }

    @Test fun routeSelectionEmitsOnlyForKnownRouteAndDoesNotLoadStops() = runTest(dispatcher) {
        repository.lines.value = listOf(TransitLine("38", "Geary", "bus"))
        val vm = retain(TransitLinesViewModel(repository, SavedStateHandle()))
        runCurrent()
        val events = mutableListOf<TransitNavigation>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.navigationEvents.toList(events) }
        vm.onAction(TransitLinesAction.SelectRoute("missing"))
        vm.onAction(TransitLinesAction.SelectRoute("38"))
        runCurrent()
        assertEquals(listOf(TransitNavigation.OpenRoute("SF", "38")), events)
        assertEquals(0, repository.stopRefreshes)
    }

    @Test fun stopsIgnoreIntermediateEmptyRefreshAndClearRemovedSelectionOnlyAtCompletion() = runTest(dispatcher) {
        repository.stops.value = listOf(testStop)
        val refresh = CompletableDeferred<Unit>()
        repository.refreshStops = { refresh.await() }
        val saved = SavedStateHandle(mapOf("selected_stop" to testStop.id))
        val vm = retain(RouteStopsViewModel(repository, saved, route))
        runCurrent()
        repository.stops.value = emptyList()
        runCurrent()
        assertEquals(listOf(testStop), vm.uiState.value.stops)
        assertEquals(testStop.id, vm.uiState.value.selectedStopId)
        refresh.complete(Unit)
        runCurrent()
        assertTrue(vm.uiState.value.stops.isEmpty())
        assertNull(vm.uiState.value.selectedStopId)
        assertNull(saved.get<String>("selected_stop"))
        assertEquals(LoadStatus.LOADED, vm.uiState.value.status)
    }

    @Test fun stopRefreshFailureRetainsCachedContentAndCanRetry() = runTest(dispatcher) {
        repository.stops.value = listOf(testStop)
        repository.refreshStops = { throw TransitError.Network }
        val vm = retain(RouteStopsViewModel(repository, SavedStateHandle(), route))
        runCurrent()
        assertEquals(listOf(testStop), vm.uiState.value.stops)
        assertEquals(LoadStatus.FAILED, vm.uiState.value.status)
        assertNotNull(vm.uiState.value.error)
        repository.refreshStops = {}
        vm.onAction(RouteStopsAction.Refresh)
        runCurrent()
        assertNull(vm.uiState.value.error)
        assertEquals(LoadStatus.LOADED, vm.uiState.value.status)
    }

    @Test fun stopSelectionOpensAndDismissesASheetWithoutNavigation() = runTest(dispatcher) {
        repository.stops.value = listOf(testStop)
        val saved = SavedStateHandle()
        val vm = retain(RouteStopsViewModel(repository, saved, route))
        runCurrent()
        val events = mutableListOf<TransitNavigation>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.navigationEvents.toList(events) }
        vm.onAction(RouteStopsAction.SelectStop("missing"))
        assertNull(vm.uiState.value.arrivalsStopId)
        vm.onAction(RouteStopsAction.SelectStop(testStop.id))
        vm.onAction(RouteStopsAction.SelectStop(testStop.id))
        assertEquals(testStop.id, vm.uiState.value.arrivalsStopId)
        assertEquals(testStop.id, saved.get<String>("arrivals_stop"))
        assertTrue(events.isEmpty())
        vm.onAction(RouteStopsAction.DismissArrivals("obsolete"))
        assertEquals(testStop.id, vm.uiState.value.arrivalsStopId)
        vm.onAction(RouteStopsAction.Back)
        assertNull(vm.uiState.value.arrivalsStopId)
        assertEquals(testStop.id, vm.uiState.value.selectedStopId)
        assertNull(saved.get<String>("arrivals_stop"))
        assertTrue(events.isEmpty())
        vm.onAction(RouteStopsAction.Back)
        assertEquals(listOf(TransitNavigation.Back), events)
    }

    @Test fun externalSheetSelectionRestoresAndRejectsOldDismissals() = runTest(dispatcher) {
        val saved = SavedStateHandle()
        val vm = retain(RouteStopsViewModel(repository, saved, route))
        runCurrent()
        vm.onAction(RouteStopsAction.OpenArrivals("123"))
        vm.onAction(RouteStopsAction.OpenArrivals("456"))
        vm.onAction(RouteStopsAction.DismissArrivals("123"))
        val restored = retain(RouteStopsViewModel(repository, saved, route))
        assertEquals("456", restored.uiState.value.arrivalsStopId)
        assertEquals("456", restored.uiState.value.selectedStopId)
        restored.onAction(RouteStopsAction.DismissArrivals("456"))
        restored.onAction(RouteStopsAction.OpenArrivals(""))
        assertNull(restored.uiState.value.arrivalsStopId)
        assertEquals("456", restored.uiState.value.selectedStopId)
    }

    @Test fun arrivalsDoNotRefreshParentAndRetainPredictionsOnFailure() = runTest(dispatcher) {
        repository.arrivals = { listOf(testArrival) }
        val vm = retain(StopArrivalsViewModel(repository, testStop.id))
        vm.updateContext(testStop, true)
        runCurrent()
        assertEquals(listOf(testArrival), vm.uiState.value.predictions)
        repository.arrivals = { throw TransitError.Network }
        vm.onAction(StopArrivalsAction.Refresh)
        runCurrent()
        assertEquals(listOf(testArrival), vm.uiState.value.predictions)
        assertEquals(LoadStatus.FAILED, vm.uiState.value.status)
        assertEquals(0, repository.stopRefreshes)
    }

    @Test fun removedStopRejectsResultFromNonCooperativeRequest() = runTest(dispatcher) {
        val result = CompletableDeferred<List<BusArrival>>()
        repository.arrivals = { withContext(NonCancellable) { result.await() } }
        val vm = retain(StopArrivalsViewModel(repository, testStop.id))
        vm.updateContext(testStop, true)
        runCurrent()
        vm.updateContext(null, true)
        result.complete(listOf(testArrival))
        runCurrent()
        assertTrue(vm.uiState.value.predictions.isEmpty())
        assertNull(vm.uiState.value.stop)
        assertEquals(LoadStatus.FAILED, vm.uiState.value.status)
        assertFalse(vm.uiState.value.isRefreshing)
    }

    @Test fun trackingValidatesPredictionAndHandsOffOnceWithoutRestoration() = runTest(dispatcher) {
        repository.arrivals = { listOf(testArrival) }
        val vm = retain(StopArrivalsViewModel(repository, testStop.id))
        vm.updateContext(testStop, true)
        runCurrent()
        vm.onAction(StopArrivalsAction.Track(ArrivalPredictionKey.from(testArrival.copy(vehicleRef = "unknown"))))
        assertNull(vm.trackingRequest.value)
        vm.onAction(StopArrivalsAction.Track(ArrivalPredictionKey.from(testArrival)))
        val request = requireNotNull(vm.trackingRequest.value)
        assertEquals(request, vm.takeTrackingRequest())
        assertNull(vm.takeTrackingRequest())
        val restored = retain(StopArrivalsViewModel(repository, testStop.id))
        assertNull(restored.trackingRequest.value)
    }

    @Test fun poppedViewModelCancelsItsRequest() = runTest(dispatcher) {
        var cancelled = false
        repository.arrivals = { try { awaitCancellation() } finally { cancelled = true } }
        val vm = retain(StopArrivalsViewModel(repository, testStop.id))
        vm.updateContext(testStop, true)
        runCurrent()
        store.clear()
        runCurrent()
        assertTrue(cancelled)
    }
}

private val testStop = TransitStop("123", "Market", 37.78, -122.41, null, "38")
private val testArrival = BusArrival("38", "Geary", "2026-10-14T12:00:00Z", null, "bus1", 37.78, -122.41)

private class FakeTransitRepository : TransitRepository {
    val lines = MutableStateFlow<List<TransitLine>>(emptyList())
    val stops = MutableStateFlow<List<TransitStop>>(emptyList())
    var lineRefreshes = 0
    var stopRefreshes = 0
    var refreshLines: suspend () -> Unit = {}
    var refreshStops: suspend () -> Unit = {}
    var arrivals: suspend () -> List<BusArrival> = { emptyList() }
    override fun getTransitLines() = lines
    override suspend fun refreshTransitLines(operatorId: String, forceRefresh: Boolean) {
        lineRefreshes++; refreshLines()
    }
    override fun getTransitStops(lineId: String) = stops
    override suspend fun refreshTransitStops(operatorId: String, lineId: String) {
        stopRefreshes++; refreshStops()
    }
    override suspend fun getBusArrivals(stopId: String) = arrivals()
}
