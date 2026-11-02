package com.nghianguyen.gomuni.features.transit.arrivals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nghianguyen.gomuni.domain.model.Outcome
import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.domain.model.TransitStop
import com.nghianguyen.gomuni.domain.repository.TransitRepository
import com.nghianguyen.gomuni.features.tracking.TrackingRequest
import com.nghianguyen.gomuni.features.transit.model.LoadStatus
import com.nghianguyen.gomuni.features.transit.model.UNEXPECTED_TRANSIT_FAILURE_MESSAGE
import com.nghianguyen.gomuni.features.transit.model.transitFailureMessage
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// This UI boundary retains safe error state for unexpected repository/stream failures too.
@Suppress("TooGenericExceptionCaught")
@HiltViewModel(assistedFactory = StopArrivalsViewModel.Factory::class)
class StopArrivalsViewModel
@AssistedInject
constructor(
    private val repository: TransitRepository,
    @Assisted val stopId: String,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(stopId: String): StopArrivalsViewModel
    }

    private val state = MutableStateFlow(StopArrivalsUiState(stopId))
    val uiState = state.asStateFlow()
    private var arrivalsJob: Job? = null
    private var generation = 0L
    // The parent route supplies authoritative context; this VM never refreshes its parent's stops.
    private var stop: TransitStop? = null
    private var loaded = false

    private val request = MutableStateFlow<TrackingRequest?>(null)
    val trackingRequest = request.asStateFlow()

    fun updateContext(
        selectedStop: TransitStop?,
        ready: Boolean,
        lines: List<TransitLine> = emptyList(),
        contextError: String? = null,
    ) {
        stop = selectedStop?.takeIf { it.id == stopId }
        state.update { it.copy(stop = stop, lines = lines) }
        if (ready && stop == null) {
            generation++
            arrivalsJob?.cancel()
            arrivalsJob = null
            state.update {
                it.copy(
                    predictions = emptyList(),
                    status = LoadStatus.FAILED,
                    isRefreshing = false,
                    error = "The requested stop is no longer available.",
                )
            }
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
            is StopArrivalsAction.Track -> track(action.prediction)
        }
    }

    private fun track(predictionKey: ArrivalPredictionKey) {
        if (request.value != null) return
        val selectedStop = stop ?: return
        state.value.predictions
            .find { ArrivalPredictionKey.from(it) == predictionKey }
            ?.let { prediction ->
                request.value =
                    TrackingRequest(
                        selectedStop.id,
                        selectedStop.name,
                        prediction.lineRef,
                        prediction.lineName,
                        prediction.vehicleRef,
                    )
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
        state.update {
            it.copy(
                status = if (it.fetchedAtMillis == null) LoadStatus.LOADING else LoadStatus.LOADED,
                isRefreshing = true,
                error = null,
            )
        }
        arrivalsJob = viewModelScope.launch {
            try {
                val result = repository.getBusArrivals(stopId)
                if (result is Outcome.Failure) {
                    if (version == generation)
                        state.update {
                            it.copy(
                                status = LoadStatus.FAILED,
                                isRefreshing = false,
                                error = transitFailureMessage(result.error),
                            )
                        }
                    return@launch
                }
                val predictions = (result as Outcome.Success).value
                if (version == generation && stop != null) {
                    state.update {
                        it.copy(
                            predictions = predictions,
                            fetchedAtMillis = System.currentTimeMillis(),
                            status = LoadStatus.LOADED,
                            isRefreshing = false,
                            error = null,
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (version == generation)
                    state.update {
                        it.copy(
                            status = LoadStatus.FAILED,
                            isRefreshing = false,
                            error = UNEXPECTED_TRANSIT_FAILURE_MESSAGE,
                        )
                    }
            }
        }
    }
}
