package com.nghianguyen.gomuni.features.transit.routes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nghianguyen.gomuni.app.navigation.NavigationEvents
import com.nghianguyen.gomuni.app.navigation.TransitNavigation
import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.domain.repository.TransitRepository
import com.nghianguyen.gomuni.features.transit.model.LoadStatus
import com.nghianguyen.gomuni.features.transit.model.transitFailureMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// This UI boundary retains safe error state for unexpected repository/stream failures too.
@Suppress("TooGenericExceptionCaught")
@HiltViewModel
class TransitLinesViewModel
@Inject
constructor(
    private val repository: TransitRepository,
    private val savedStateHandle: SavedStateHandle,
    private val favorites: com.nghianguyen.gomuni.domain.repository.TransitFavoritesRepository,
    private val errorLogger: com.nghianguyen.gomuni.domain.util.ErrorLogger,
) : ViewModel() {
    private val state =
        MutableStateFlow(
            RoutesUiState(
                sortOption =
                    savedStateHandle.get<String>("sort")?.let {
                        runCatching { TransitLineSortOption.valueOf(it) }.getOrNull()
                    } ?: TransitLineSortOption.ID,
                filter = savedStateHandle.get<String>("filter") ?: "All",
            ),
        )
    val uiState = state.asStateFlow()
    private val navigation = NavigationEvents()
    val navigationEvents = navigation.flow
    private var loadJob: Job? = null
    private var favoritesJob: Job? = null

    init {
        viewModelScope.launch {
            try {
                repository.getTransitLines().collect { lines ->
                    state.update { it.copy(lines = sort(lines, it.sortOption)) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                state.update {
                    it.copy(status = LoadStatus.FAILED, error = transitFailureMessage(error))
                }
            }
        }
        load()
        observeFavorites()
    }

    fun onAction(action: TransitLinesAction) {
        when (action) {
            TransitLinesAction.RetryLoad -> load()
            TransitLinesAction.RetryFavorites -> observeFavorites()
            is TransitLinesAction.SetFavorite -> setFavorite(action.lineId, action.favorite)
            is TransitLinesAction.ChangeSort -> changeSort(action.option)
            is TransitLinesAction.ChangeFilter -> changeFilter(action.filter)
            is TransitLinesAction.SelectRoute -> selectRoute(action.lineId)
        }
    }

    private fun changeSort(option: TransitLineSortOption) {
        savedStateHandle["sort"] = option.name
        state.update { it.copy(sortOption = option, lines = sort(it.lines, option)) }
    }

    private fun changeFilter(filter: String) {
        if (filter !in listOf("All", "Local", "Rapid", "Express", "Owl")) return
        savedStateHandle["filter"] = filter
        state.update { it.copy(filter = filter) }
    }

    private fun selectRoute(lineId: String) {
        if (state.value.lines.any { it.id == lineId })
            navigation.emit(TransitNavigation.OpenRoute("SF", lineId))
    }

    private fun observeFavorites() {
        if (favoritesJob?.isActive == true) return
        state.update { it.copy(favoritesReady = false, favoriteError = null) }
        favoritesJob = viewModelScope.launch {
            try {
                favorites.observeFavoriteIds().collect { ids ->
                    state.update { it.copy(favoriteIds = ids.toSet(), favoritesReady = true) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                favoriteFailure(error, FavoriteError.READ)
            }
        }
    }

    private fun setFavorite(lineId: String, favorite: Boolean) {
        val current = state.value
        if (
            !current.favoritesReady ||
                lineId in current.pendingFavoriteIds ||
                current.lines.none { it.id == lineId }
        )
            return
        if ((lineId in current.favoriteIds) == favorite) return
        state.update {
            it.copy(pendingFavoriteIds = it.pendingFavoriteIds + lineId, favoriteError = null)
        }
        viewModelScope.launch {
            try {
                favorites.setFavorite(lineId, favorite)
                val confirmed = favorites.observeFavoriteIds().first { (lineId in it) == favorite }
                state.update { it.copy(favoriteIds = confirmed.toSet()) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                favoriteFailure(error, FavoriteError.WRITE)
            } finally {
                state.update { it.copy(pendingFavoriteIds = it.pendingFavoriteIds - lineId) }
            }
        }
    }

    private fun favoriteFailure(error: Exception, failure: FavoriteError) {
        // Expected storage errors were logged at the data boundary.
        if (error !is com.nghianguyen.gomuni.domain.model.FavoritesStorageException) {
            errorLogger.log("Favorites failed", error)
        }
        state.update {
            it.copy(
                favoritesReady = if (failure == FavoriteError.READ) false else it.favoritesReady,
                // Keep read recovery available if a pending write fails after observation stops.
                favoriteError =
                    if (it.favoriteError == FavoriteError.READ) FavoriteError.READ else failure,
            )
        }
    }

    private fun load() {
        if (loadJob?.isActive == true || state.value.status == LoadStatus.LOADED) return
        state.update {
            it.copy(
                status = if (it.lines.isEmpty()) LoadStatus.LOADING else LoadStatus.LOADED,
                error = null,
            )
        }
        loadJob = viewModelScope.launch {
            try {
                repository.ensureTransitLinesLoaded("SF")
                val lines = repository.getTransitLines().first()
                state.update {
                    it.copy(
                        lines = sort(lines, it.sortOption),
                        status = LoadStatus.LOADED,
                        error = null,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                state.update {
                    it.copy(
                        status = LoadStatus.FAILED,
                        error = transitFailureMessage(error),
                    )
                }
            }
        }
    }

    private fun sort(lines: List<TransitLine>, option: TransitLineSortOption): List<TransitLine> =
        when (option) {
            TransitLineSortOption.NAME -> lines.sortedBy { it.name }
            TransitLineSortOption.ID ->
                lines.sortedWith(
                    compareBy<TransitLine> {
                            it.id.takeWhile(Char::isDigit).toIntOrNull() ?: Int.MAX_VALUE
                        }
                        .thenBy { it.id.dropWhile(Char::isDigit) },
                )
        }
}
