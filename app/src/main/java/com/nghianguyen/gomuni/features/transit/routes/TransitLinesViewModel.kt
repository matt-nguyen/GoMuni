package com.nghianguyen.gomuni.features.transit.routes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.domain.repository.TransitRepository
import com.nghianguyen.gomuni.app.navigation.NavigationEvents
import com.nghianguyen.gomuni.app.navigation.TransitNavigation
import com.nghianguyen.gomuni.features.transit.model.LoadStatus
import com.nghianguyen.gomuni.features.transit.model.transitFailureMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class TransitLinesViewModel @Inject constructor(
    private val repository: TransitRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val state = MutableStateFlow(RoutesUiState(
        sortOption = savedStateHandle.get<String>("sort")?.let {
            runCatching { TransitLineSortOption.valueOf(it) }.getOrNull()
        } ?: TransitLineSortOption.ID,
        filter = savedStateHandle.get<String>("filter") ?: "All",
    ))
    val uiState = state.asStateFlow()
    private val navigation = NavigationEvents()
    val navigationEvents = navigation.flow
    private var refreshJob: Job? = null

    init {
        viewModelScope.launch {
            try {
                repository.getTransitLines().collect { lines ->
                    state.update { it.copy(lines = sort(lines, it.sortOption)) }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                state.update { it.copy(status = LoadStatus.FAILED, error = transitFailureMessage(error)) }
            }
        }
        refresh(false)
    }

    fun onAction(action: TransitLinesAction) {
        when (action) {
            TransitLinesAction.Refresh -> refresh(true)
            is TransitLinesAction.ChangeSort -> {
                savedStateHandle["sort"] = action.option.name
                state.update { it.copy(sortOption = action.option, lines = sort(it.lines, action.option)) }
            }
            is TransitLinesAction.ChangeFilter -> {
                if (action.filter !in listOf("All", "Local", "Rapid", "Express", "Owl")) return
                savedStateHandle["filter"] = action.filter
                state.update { it.copy(filter = action.filter) }
            }
            is TransitLinesAction.SelectRoute -> if (state.value.lines.any { it.id == action.lineId }) {
                navigation.emit(TransitNavigation.OpenRoute("SF", action.lineId))
            }
        }
    }

    private fun refresh(force: Boolean) {
        if (refreshJob?.isActive == true) return
        state.update { it.copy(status = if (it.lines.isEmpty()) LoadStatus.LOADING else LoadStatus.LOADED,
            isRefreshing = true, error = null) }
        refreshJob = viewModelScope.launch {
            try {
                repository.refreshTransitLines("SF", force)
                val lines = repository.getTransitLines().first()
                state.update { it.copy(lines = sort(lines, it.sortOption), status = LoadStatus.LOADED,
                    isRefreshing = false, error = null) }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                state.update { it.copy(status = LoadStatus.FAILED, isRefreshing = false,
                    error = transitFailureMessage(error)) }
            }
        }
    }

    private fun sort(lines: List<TransitLine>, option: TransitLineSortOption): List<TransitLine> = when (option) {
        TransitLineSortOption.NAME -> lines.sortedBy { it.name }
        TransitLineSortOption.ID -> lines.sortedWith(compareBy<TransitLine> {
            it.id.takeWhile(Char::isDigit).toIntOrNull() ?: Int.MAX_VALUE
        }.thenBy { it.id.dropWhile(Char::isDigit) })
    }
}
