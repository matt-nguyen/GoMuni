package com.nghianguyen.gomuni.ui.lines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nghianguyen.gomuni.domain.model.BusArrival
import com.nghianguyen.gomuni.domain.model.TransitError
import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.domain.model.TransitStop
import com.nghianguyen.gomuni.domain.repository.TransitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TransitLineSortOption {
    ID, NAME
}

data class TransitLinesUiState(
    val lines: List<TransitLine> = emptyList(),
    val stops: List<TransitStop> = emptyList(),
    val selectedLineId: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val sortOption: TransitLineSortOption = TransitLineSortOption.ID,
    val selectedStop: TransitStop? = null,
    val busArrivals: List<BusArrival> = emptyList(),
    val isArrivalsLoading: Boolean = false,
    val arrivalsLastUpdatedMillis: Long? = null
)

@HiltViewModel
class TransitLinesViewModel @Inject constructor(
    private val repository: TransitRepository
) : ViewModel() {

    private data class ViewModelState(
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val sortOption: TransitLineSortOption = TransitLineSortOption.ID,
        val selectedLineId: String? = null,
        val selectedStop: TransitStop? = null,
        val busArrivals: List<BusArrival> = emptyList(),
        val isArrivalsLoading: Boolean = false,
        val arrivalsLastUpdatedMillis: Long? = null
    )

    private val _state = MutableStateFlow(ViewModelState())

    private val _errorEvents = Channel<String>(Channel.BUFFERED)
    val errorEvents = _errorEvents.receiveAsFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TransitLinesUiState> = combine(
        repository.getTransitLines(),
        _state,
        _state.map { it.selectedLineId }.distinctUntilChanged().flatMapLatest { lineId ->
            if (lineId != null && lineId.isNotEmpty()) {
                repository.getTransitStops(lineId)
            } else {
                flowOf(emptyList())
            }
        }
    ) { lines, state, stops ->
        val sortedLines = when (state.sortOption) {
            TransitLineSortOption.ID -> lines.sortedWith(
                compareBy<TransitLine> {
                    it.id.takeWhile { char -> char.isDigit() }.toIntOrNull() ?: Int.MAX_VALUE
                }.thenBy {
                    it.id.dropWhile { char -> char.isDigit() }
                }
            )
            TransitLineSortOption.NAME -> lines.sortedBy { it.name }
        }
        TransitLinesUiState(
            lines = sortedLines,
            stops = stops,
            selectedLineId = state.selectedLineId,
            isLoading = state.isLoading,
            errorMessage = state.errorMessage,
            sortOption = state.sortOption,
            selectedStop = state.selectedStop,
            busArrivals = state.busArrivals,
            isArrivalsLoading = state.isArrivalsLoading,
            arrivalsLastUpdatedMillis = state.arrivalsLastUpdatedMillis
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransitLinesUiState(isLoading = true)
    )

    fun onSortOptionSelected(option: TransitLineSortOption) {
        _state.update { it.copy(sortOption = option) }
    }

    fun onLineSelected(lineId: String, operatorId: String = "SF") {
        // Clear bus arrivals when navigating back or switching routes
        _state.update { it.copy(selectedLineId = lineId, selectedStop = null, busArrivals = emptyList(), arrivalsLastUpdatedMillis = null) }
        if (lineId.isNotEmpty()) {
            viewModelScope.launch {
                try {
                    repository.refreshTransitStops(operatorId, lineId)
                } catch (e: Exception) {
                    // Silently fail
                }
            }
        }
    }

    fun onStopSelected(stop: TransitStop?) {
        if (stop != null) {
            // New stop selection: clear previous arrivals and fetch fresh ones
            _state.update { it.copy(selectedStop = stop, busArrivals = emptyList(), arrivalsLastUpdatedMillis = null) }
            refreshBusArrivals(stop.id)
        } else {
            // Dismiss bottom sheet: clear selection but persist arrivals for map view
            _state.update { it.copy(selectedStop = null) }
        }
    }

    fun onStopSelectedById(lineId: String, stopId: String) {
        onLineSelected(lineId)
        viewModelScope.launch {
            // Wait for stops to be loaded
            uiState.map { it.stops }.filter { it.isNotEmpty() }.first().let { stops ->
                val stop = stops.find { it.id == stopId }
                if (stop != null) {
                    onStopSelected(stop)
                }
            }
        }
    }

    fun refreshBusArrivals(stopId: String) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isArrivalsLoading = true) }
                val arrivals = repository.getBusArrivals(stopId)
                _state.update { it.copy(busArrivals = arrivals, arrivalsLastUpdatedMillis = System.currentTimeMillis()) }
            } catch (e: Exception) {
                _errorEvents.send("Failed to load bus arrivals. Please try again.")
            } finally {
                _state.update { it.copy(isArrivalsLoading = false) }
            }
        }
    }

    fun refreshTransitLines(operatorId: String, force: Boolean = false) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true, errorMessage = null) }
                repository.refreshTransitLines(operatorId, force)
            } catch (e: Exception) {
                val userMessage = when (e) {
                    is TransitError.Network -> "Check your internet connection and try again."
                    is TransitError.NotFound -> "The transit operator was not found."
                    else -> "Something went wrong on our end. Please try later."
                }
                _errorEvents.send(userMessage)

                if (repository.getTransitLines().first().isEmpty()) {
                    _state.update { it.copy(errorMessage = userMessage) }
                }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}
