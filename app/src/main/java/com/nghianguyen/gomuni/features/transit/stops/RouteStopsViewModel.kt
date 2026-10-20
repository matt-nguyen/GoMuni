package com.nghianguyen.gomuni.features.transit.stops

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nghianguyen.gomuni.app.navigation.NavigationEvents
import com.nghianguyen.gomuni.app.navigation.TransitNavigation
import com.nghianguyen.gomuni.domain.model.TransitStop
import com.nghianguyen.gomuni.domain.repository.TransitRepository
import com.nghianguyen.gomuni.domain.util.ErrorLogger
import com.nghianguyen.gomuni.features.transit.model.LoadStatus
import com.nghianguyen.gomuni.features.transit.model.RouteKey
import com.nghianguyen.gomuni.features.transit.model.transitFailureMessage
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// This UI boundary retains safe error state for unexpected repository/stream failures too.
@Suppress("TooGenericExceptionCaught")
@HiltViewModel(assistedFactory = RouteStopsViewModel.Factory::class)
class RouteStopsViewModel
@AssistedInject
constructor(
    private val repository: TransitRepository,
    private val savedStateHandle: SavedStateHandle,
    @Assisted val route: RouteKey,
    private val errorLogger: ErrorLogger,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(route: RouteKey): RouteStopsViewModel
    }

    private val state =
        MutableStateFlow(
            RouteStopsUiState(
                route,
                selectedStopId = savedStateHandle["selected_stop"],
                arrivalsStopId = savedStateHandle["arrivals_stop"],
            ),
        )
    val uiState = state.asStateFlow()
    private val navigation = NavigationEvents()
    val navigationEvents = navigation.flow
    private var loadJob: Job? = null

    init {
        if (route.lineId.isBlank() || route.operatorId.isBlank()) {
            state.update {
                it.copy(status = LoadStatus.FAILED, error = "The requested route is unavailable.")
            }
        } else {
            viewModelScope.launch {
                try {
                    repository.getTransitLines().collect { lines ->
                        state.update { it.copy(lines = lines) }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    // Metadata can be unavailable without hiding usable stops.
                    errorLogger.log("Route metadata unavailable", error)
                }
            }
            viewModelScope.launch {
                try {
                    repository.getTransitStops(route.lineId).collect { updateStops(it, false) }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    fail(error)
                }
            }
            load()
        }
    }

    fun onAction(action: RouteStopsAction) {
        when (action) {
            RouteStopsAction.Back -> goBack()
            RouteStopsAction.RetryLoad -> load()
            is RouteStopsAction.SelectStop -> selectStop(action.stopId)
            is RouteStopsAction.OpenArrivals -> openArrivals(action.stopId)
            is RouteStopsAction.DismissArrivals -> dismissArrivals(action.stopId)
        }
    }

    private fun goBack() {
        val openStop = state.value.arrivalsStopId
        if (openStop != null) dismissArrivals(openStop) else navigation.emit(TransitNavigation.Back)
    }

    private fun selectStop(stopId: String) {
        if (state.value.stops.any { it.id == stopId }) openArrivals(stopId)
    }

    private fun openArrivals(stopId: String) {
        if (stopId.isBlank()) return
        savedStateHandle["selected_stop"] = stopId
        savedStateHandle["arrivals_stop"] = stopId
        state.update { it.copy(selectedStopId = stopId, arrivalsStopId = stopId) }
    }

    private fun dismissArrivals(stopId: String) {
        if (state.value.arrivalsStopId != stopId) return
        savedStateHandle.remove<String>("arrivals_stop")
        state.update { it.copy(arrivalsStopId = null) }
    }

    private fun load() {
        if (route.lineId.isBlank() || route.operatorId.isBlank()) return
        if (loadJob?.isActive == true || state.value.status == LoadStatus.LOADED) return
        state.update {
            it.copy(
                status = if (it.stops.isEmpty()) LoadStatus.LOADING else LoadStatus.LOADED,
                error = null,
            )
        }
        loadJob = viewModelScope.launch {
            try {
                repository.ensureTransitStopsLoaded(route.operatorId, route.lineId)
                val stops = repository.getTransitStops(route.lineId).first()
                updateStops(stops, true)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                fail(error)
            }
        }
    }

    private fun updateStops(stops: List<TransitStop>, authoritative: Boolean) {
        val current = state.value
        val selected =
            current.selectedStopId?.takeUnless { id ->
                authoritative && stops.none { it.id == id }
            }
        if (selected != current.selectedStopId) savedStateHandle["selected_stop"] = selected
        state.update {
            it.copy(
                stops = stops,
                selectedStopId = selected,
                status = if (authoritative) LoadStatus.LOADED else it.status,
                error = if (authoritative) null else it.error,
            )
        }
    }

    private fun fail(error: Exception) {
        state.update {
            it.copy(
                status = LoadStatus.FAILED,
                error = transitFailureMessage(error),
            )
        }
    }
}
