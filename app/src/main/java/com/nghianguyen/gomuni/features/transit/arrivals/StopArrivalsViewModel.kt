package com.nghianguyen.gomuni.features.transit.arrivals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nghianguyen.gomuni.domain.model.TransitStop
import com.nghianguyen.gomuni.domain.repository.TransitRepository
import com.nghianguyen.gomuni.features.tracking.TrackingRequest
import com.nghianguyen.gomuni.features.transit.model.LoadStatus
import com.nghianguyen.gomuni.features.transit.model.transitFailureMessage
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@HiltViewModel(assistedFactory = StopArrivalsViewModel.Factory::class)
class StopArrivalsViewModel @AssistedInject constructor(
    private val repository: TransitRepository,
    @Assisted val stopId: String,
) : ViewModel() {
    @AssistedFactory interface Factory { fun create(stopId: String): StopArrivalsViewModel }
    private val state = MutableStateFlow(StopArrivalsUiState(stopId))
    val uiState = state.asStateFlow()
    private var arrivalsJob: Job? = null
    private var generation = 0L
    // The parent route supplies authoritative context; this VM never refreshes its parent's stops.
    private var stop: TransitStop? = null
    private var loaded = false

    private val request = MutableStateFlow<TrackingRequest?>(null)
    val trackingRequest = request.asStateFlow()

    fun updateContext(selectedStop: TransitStop?, ready: Boolean,
        lines: List<com.nghianguyen.gomuni.domain.model.TransitLine> = emptyList(),
        contextError: String? = null,
    ) {
        stop = selectedStop?.takeIf { it.id == stopId }
        state.update { it.copy(stop = stop, lines = lines) }
        if (ready && stop == null) {
            generation++
            arrivalsJob?.cancel()
            arrivalsJob = null
            state.update { it.copy(predictions = emptyList(), status = LoadStatus.FAILED,
                isRefreshing = false, error = "The requested stop is no longer available.") }
            loaded = false
        } else if (stop == null && contextError != null) {
            state.update { it.copy(status = LoadStatus.FAILED, error = contextError) }
        } else if (stop == null) {
            state.update { it.copy(status = LoadStatus.LOADING, error = null) }
        } else if (stop != null && !loaded) {
            loaded = true
            refresh()
        }
    }

    fun onAction(action: StopArrivalsAction) {
        when (action) {
            StopArrivalsAction.Dismiss -> Unit // The route owns sheet dismissal.
            StopArrivalsAction.Refresh -> refresh()
            is StopArrivalsAction.Track -> {
                if (request.value != null) return
                val selectedStop = stop ?: return
                val prediction = state.value.predictions.find {
                    ArrivalPredictionKey.from(it) == action.prediction
                } ?: return
                val target = TrackingRequest(selectedStop.id,
                    selectedStop.name, prediction.lineRef, prediction.lineName, prediction.vehicleRef)
                request.value = target
            }
        }
    }

    fun takeTrackingRequest(): TrackingRequest? {
        val pending = request.value
        request.value = null
        return pending
    }

    private fun refresh() {
        if (stopId.isBlank() || stop == null || arrivalsJob?.isActive == true) return
        val version = ++generation
        state.update { it.copy(status = if (it.fetchedAtMillis == null) LoadStatus.LOADING else LoadStatus.LOADED,
            isRefreshing = true, error = null) }
        arrivalsJob = viewModelScope.launch {
            try {
                val predictions = repository.getBusArrivals(stopId)
                if (version == generation && stop != null) {
                    state.update { it.copy(predictions = predictions, fetchedAtMillis = System.currentTimeMillis(),
                        status = LoadStatus.LOADED, isRefreshing = false, error = null) }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (version == generation) state.update { it.copy(status = LoadStatus.FAILED,
                    isRefreshing = false, error = transitFailureMessage(error)) }
            }
        }
    }
}
