package com.nghianguyen.gomuni.ui.stops

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nghianguyen.gomuni.domain.model.TransitError
import com.nghianguyen.gomuni.domain.repository.TransitRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = StopsViewModel.Factory::class)
class StopsViewModel @AssistedInject constructor(
    private val repository: TransitRepository,
    @Assisted("lineId") private val lineId: String,
    @Assisted("operatorId") private val operatorId: String
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("lineId") lineId: String,
            @Assisted("operatorId") operatorId: String
        ): StopsViewModel
    }

    private data class ViewModelState(
        val isLoading: Boolean = false,
        val errorMessage: String? = null
    )

    private val _state = MutableStateFlow(ViewModelState())

    private val _errorEvents = Channel<String>(Channel.BUFFERED)
    val errorEvents = _errorEvents.receiveAsFlow()

    val uiState: StateFlow<StopsUiState> = combine(
        repository.getTransitStops(lineId),
        _state
    ) { stops, state ->
        StopsUiState(
            stops = stops,
            isLoading = state.isLoading,
            errorMessage = state.errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StopsUiState(isLoading = true)
    )

    fun refreshStops() {
        if (lineId.isEmpty()) return

        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true, errorMessage = null) }
                repository.refreshTransitStops(operatorId, lineId)
            } catch (e: Exception) {
                val userMessage = when (e) {
                    is TransitError.Network -> "Check your internet connection and try again."
                    is TransitError.NotFound -> "The transit stops were not found."
                    else -> "Something went wrong on our end. Please try later."
                }
                _errorEvents.send(userMessage)
                
                if (repository.getTransitStops(lineId).first().isEmpty()) {
                    _state.update { it.copy(errorMessage = userMessage) }
                }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}
