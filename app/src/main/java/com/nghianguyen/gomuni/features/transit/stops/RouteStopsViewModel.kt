package com.nghianguyen.gomuni.features.transit.stops

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nghianguyen.gomuni.domain.model.TransitStop
import com.nghianguyen.gomuni.domain.repository.TransitRepository
import com.nghianguyen.gomuni.app.navigation.NavigationEvents
import com.nghianguyen.gomuni.app.navigation.TransitNavigation
import com.nghianguyen.gomuni.features.transit.model.LoadStatus
import com.nghianguyen.gomuni.features.transit.model.RouteKey
import com.nghianguyen.gomuni.features.transit.model.transitFailureMessage
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@HiltViewModel(assistedFactory = RouteStopsViewModel.Factory::class)
class RouteStopsViewModel @AssistedInject constructor(
    private val repository: TransitRepository,
    private val savedStateHandle: SavedStateHandle,
    @Assisted val route: RouteKey,
) : ViewModel() {
    @AssistedFactory interface Factory { fun create(route: RouteKey): RouteStopsViewModel }
    private val state = MutableStateFlow(RouteStopsUiState(route,
        selectedStopId = savedStateHandle["selected_stop"]))
    val uiState = state.asStateFlow()
    private val navigation = NavigationEvents()
    val navigationEvents = navigation.flow
    private var refreshJob: Job? = null
    private var generation = 0L

    init {
        if (route.lineId.isBlank() || route.operatorId.isBlank()) {
            state.update { it.copy(status = LoadStatus.FAILED, error = "The requested route is unavailable.") }
        } else {
            viewModelScope.launch {
                try {
                    repository.getTransitLines().collect { lines -> state.update { it.copy(lines = lines) } }
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    // Metadata can be unavailable without hiding usable stops.
                }
            }
            viewModelScope.launch {
                try {
                    repository.getTransitStops(route.lineId).collect { updateStops(it, false) }
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    fail(error)
                }
            }
            refresh()
        }
    }

    fun onAction(action: RouteStopsAction) {
        when (action) {
            RouteStopsAction.Back -> navigation.emit(TransitNavigation.Back)
            RouteStopsAction.Refresh -> refresh()
            is RouteStopsAction.SelectStop -> if (state.value.stops.any { it.id == action.stopId }) {
                rememberSelection(action.stopId)
                navigation.emit(TransitNavigation.OpenStop(route.operatorId, route.lineId, action.stopId))
            }
        }
    }

    /** Navigation identity may arrive from an external request rather than a list click. */
    fun rememberSelection(stopId: String) {
        savedStateHandle["selected_stop"] = stopId
        state.update { it.copy(selectedStopId = stopId) }
    }

    private fun refresh() {
        if (route.lineId.isBlank() || route.operatorId.isBlank() || refreshJob?.isActive == true) return
        val version = ++generation
        state.update { it.copy(status = if (it.stops.isEmpty()) LoadStatus.LOADING else LoadStatus.LOADED,
            isRefreshing = true, error = null) }
        refreshJob = viewModelScope.launch {
            try {
                repository.refreshTransitStops(route.operatorId, route.lineId)
                val stops = repository.getTransitStops(route.lineId).first()
                if (version == generation) updateStops(stops, true)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (version == generation) fail(error)
            }
        }
    }

    private fun updateStops(stops: List<TransitStop>, authoritative: Boolean) {
        val current = state.value
        // Delete/insert refresh emits a temporary empty list; only the final snapshot removes selection.
        if (current.isRefreshing && !authoritative && stops.isEmpty()) return
        val selected = current.selectedStopId?.takeUnless { id ->
            authoritative && stops.none { it.id == id }
        }
        if (selected != current.selectedStopId) savedStateHandle["selected_stop"] = selected
        state.update { it.copy(stops = stops, selectedStopId = selected,
            status = if (authoritative) LoadStatus.LOADED else it.status,
            isRefreshing = if (authoritative) false else it.isRefreshing,
            error = if (authoritative) null else it.error) }
    }

    private fun fail(error: Exception) {
        state.update { it.copy(status = LoadStatus.FAILED, isRefreshing = false,
            error = transitFailureMessage(error)) }
    }
}
