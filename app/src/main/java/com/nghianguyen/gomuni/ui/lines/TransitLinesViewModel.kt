package com.nghianguyen.gomuni.ui.lines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nghianguyen.gomuni.domain.model.*
import com.nghianguyen.gomuni.domain.repository.TransitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class TransitLinesViewModel @Inject constructor(private val repository: TransitRepository) : ViewModel() {
    private val state = MutableStateFlow(TransitExplorerUiState())
    val uiState: StateFlow<TransitExplorerUiState> = state.asStateFlow()
    private var routesJob: Job? = null
    private var stopsJob: Job? = null
    private var arrivalsJob: Job? = null
    private var selectionVersion = 0L
    private var arrivalVersion = 0L

    init {
        viewModelScope.launch {
            try {
                repository.getTransitLines().collect { lines ->
                    state.update { it.copy(routesState = it.routesState.copy(lines = sort(lines, it.routesState.sortOption))) }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                state.update { it.copy(routesState = it.routesState.copy(status = LoadStatus.FAILED, error = message(error))) }
            }
        }
    }

    fun onAction(action: TransitExplorerAction) {
        when (action) {
            TransitExplorerAction.ClearRoute -> clearRouteSelection()
            TransitExplorerAction.DismissArrivals -> dismissArrivals()
            TransitExplorerAction.RefreshRoutes -> refreshTransitLines("SF", force = true)
            is TransitExplorerAction.SelectRoute -> {
                if (action.route.operatorId.isNotBlank() && action.route.lineId.isNotBlank()) selectRoute(action.route)
            }
            is TransitExplorerAction.ChangeSort -> onSortOptionSelected(action.option)
            is TransitExplorerAction.SelectStop -> {
                val stop = state.value.routeStopsState?.stops?.find { it.id == action.stopId }
                if (stop != null) onStopSelected(stop)
            }
            TransitExplorerAction.RetryStops -> refreshSelectedRouteStops()
            TransitExplorerAction.RefreshArrivals -> {
                state.value.routeStopsState?.selectedStop?.let { refreshBusArrivals(it.id) }
            }
        }
    }

    fun ensureRoutesLoaded() {
        if (state.value.routesState.status == LoadStatus.NOT_LOADED) refreshTransitLines("SF")
    }

    private fun dismissArrivals() {
        state.update { it.copy(arrivalSheetVisible = false) }
    }

    private fun refreshSelectedRouteStops() {
        val route = state.value.routeStopsState?.route ?: return
        if (state.value.routeStopsState?.isRefreshing != true) selectRoute(route)
    }

    private fun onSortOptionSelected(option: TransitLineSortOption) {
        state.update { it.copy(routesState = it.routesState.copy(sortOption = option, lines = sort(it.routesState.lines, option))) }
    }

    private fun clearRouteSelection() {
        selectionVersion++
        arrivalVersion++
        stopsJob?.cancel()
        arrivalsJob?.cancel()
        state.update { it.copy(routeStopsState = null, arrivalsState = null, arrivalSheetVisible = false) }
    }

    private fun selectRoute(route: RouteKey, pendingStopId: String? = null) {
        val existing = state.value.routeStopsState?.takeIf { it.route == route }
        if (existing == null || pendingStopId != null) clearRouteSelection()
        else {
            selectionVersion++
            stopsJob?.cancel()
        }
        val version = selectionVersion
        state.update { it.copy(routeStopsState = (existing ?: RouteStopsUiState(route)).copy(
            status = if (existing?.stops.isNullOrEmpty()) LoadStatus.LOADING else LoadStatus.LOADED,
            isRefreshing = true, error = null,
        )) }
        stopsJob = viewModelScope.launch {
            val observation = launch {
                try {
                    repository.getTransitStops(route.lineId).collect { stops ->
                        if (version == selectionVersion) updateStops(stops, authoritative = false)
                    }
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    if (version == selectionVersion) stopFailure(error)
                }
            }
            try {
                repository.refreshTransitStops(route.operatorId, route.lineId)
                val stops = repository.getTransitStops(route.lineId).first()
                if (version == selectionVersion) {
                    updateStops(stops, authoritative = true)
                    if (pendingStopId != null) {
                        val stop = stops.find { it.id == pendingStopId }
                        if (stop != null) onStopSelected(stop)
                        else state.update { it.copy(routeStopsState = it.routeStopsState?.copy(error = "The requested stop is no longer available.")) }
                    }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (version == selectionVersion) stopFailure(error)
            }
            observation.join()
        }
    }

    private fun stopFailure(error: Exception) {
        state.update { it.copy(routeStopsState = it.routeStopsState?.copy(
            status = LoadStatus.FAILED, isRefreshing = false, error = message(error))) }
    }

    private fun updateStops(stops: List<TransitStop>, authoritative: Boolean) {
        state.update { current ->
            val route = current.routeStopsState ?: return@update current
            // The repository still deletes and inserts separately; an intermediate empty
            // emission during refresh is not an authoritative removal.
            if (route.isRefreshing && !authoritative && stops.isEmpty()) return@update current
            val removed = authoritative && route.selectedStopId != null && stops.none { it.id == route.selectedStopId }
            current.copy(
                routeStopsState = route.copy(stops = stops,
                    selectedStopId = if (removed) null else route.selectedStopId,
                    status = if (authoritative) LoadStatus.LOADED else route.status,
                    isRefreshing = if (authoritative) false else route.isRefreshing,
                    error = if (authoritative) null else route.error),
                arrivalsState = if (removed) null else current.arrivalsState,
                arrivalSheetVisible = !removed && current.arrivalSheetVisible,
            )
        }
    }

    private fun onStopSelected(stop: TransitStop?) {
        if (stop == null) { dismissArrivals(); return }
        val route = state.value.routeStopsState ?: return
        if (stop.lineId != route.route.lineId || route.stops.none { it.id == stop.id }) return
        arrivalVersion++
        arrivalsJob?.cancel()
        arrivalsJob = null
        state.update { it.copy(routeStopsState = it.routeStopsState?.copy(selectedStopId = stop.id),
            arrivalsState = StopArrivalsUiState(stop.id), arrivalSheetVisible = true) }
        refreshBusArrivals(stop.id)
    }

    fun onStopSelectedById(lineId: String, stopId: String) {
        if (lineId.isNotBlank() && stopId.isNotBlank()) selectRoute(RouteKey("SF", lineId), stopId)
    }

    private fun refreshBusArrivals(stopId: String) {
        if (state.value.routeStopsState?.selectedStopId != stopId || arrivalsJob?.isActive == true) return
        val version = ++arrivalVersion
        arrivalsJob = viewModelScope.launch {
            state.update { it.copy(arrivalsState = (it.arrivalsState ?: StopArrivalsUiState(stopId)).copy(
                status = if (it.arrivalsState?.fetchedAtMillis == null) LoadStatus.LOADING else LoadStatus.LOADED,
                isRefreshing = true, error = null)) }
            try {
                val predictions = repository.getBusArrivals(stopId)
                if (version == arrivalVersion && state.value.routeStopsState?.selectedStopId == stopId)
                    state.update { it.copy(arrivalsState = StopArrivalsUiState(stopId, predictions, System.currentTimeMillis(), LoadStatus.LOADED)) }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (version == arrivalVersion) state.update { it.copy(arrivalsState = it.arrivalsState?.copy(
                    status = LoadStatus.FAILED, isRefreshing = false, error = message(error))) }
            }
        }
    }

    private fun refreshTransitLines(operatorId: String, force: Boolean = false) {
        if (routesJob?.isActive == true) return
        routesJob = viewModelScope.launch {
            state.update { it.copy(routesState = it.routesState.copy(
                status = if (it.routesState.lines.isEmpty()) LoadStatus.LOADING else LoadStatus.LOADED,
                isRefreshing = true, error = null)) }
            try {
                repository.refreshTransitLines(operatorId, force)
                val lines = repository.getTransitLines().first()
                state.update { it.copy(routesState = it.routesState.copy(lines = sort(lines, it.routesState.sortOption),
                    status = LoadStatus.LOADED, isRefreshing = false)) }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                val text = message(error)
                state.update { it.copy(routesState = it.routesState.copy(status = LoadStatus.FAILED, isRefreshing = false, error = text)) }
            }
        }
    }

    private fun message(error: Throwable): String = when (error) {
        TransitError.Network -> "Check your internet connection and try again."
        TransitError.NotFound -> "The requested transit information was not found."
        else -> "Unable to load transit information. Please try again."
    }

    private fun sort(lines: List<TransitLine>, option: TransitLineSortOption): List<TransitLine> = when (option) {
        TransitLineSortOption.NAME -> lines.sortedBy { it.name }
        TransitLineSortOption.ID -> lines.sortedWith(compareBy<TransitLine> {
            it.id.takeWhile(Char::isDigit).toIntOrNull() ?: Int.MAX_VALUE
        }.thenBy { it.id.dropWhile(Char::isDigit) })
    }
}
