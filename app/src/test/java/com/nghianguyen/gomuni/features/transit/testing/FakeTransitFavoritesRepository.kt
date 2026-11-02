package com.nghianguyen.gomuni.features.transit.testing

import com.nghianguyen.gomuni.domain.model.FavoritesError
import com.nghianguyen.gomuni.domain.model.Outcome
import com.nghianguyen.gomuni.domain.repository.TransitFavoritesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.transformWhile

@OptIn(ExperimentalCoroutinesApi::class)
internal class FakeTransitFavoritesRepository : TransitFavoritesRepository {
    val ids = MutableStateFlow<Set<String>>(emptySet())
    private val readFailures = MutableStateFlow<FavoritesError?>(null)
    var readFailure: FavoritesError?
        get() = readFailures.value
        set(value) {
            readFailures.value = value
        }

    var beforeWrite: suspend () -> Unit = {}
    var writeFailure: FavoritesError? = null
    var writes = 0

    override fun observeFavoriteIds() =
        combine(ids, readFailures) { current, failure ->
                failure?.let { Outcome.Failure(it) } ?: Outcome.Success(current)
            }
            .transformWhile { result ->
                emit(result)
                result is Outcome.Success
            }

    override suspend fun setFavorite(
        lineId: String,
        favorite: Boolean,
    ): Outcome<Unit, FavoritesError> {
        writes++
        beforeWrite()
        writeFailure?.let {
            return Outcome.Failure(it)
        }
        ids.value = if (favorite) ids.value + lineId else ids.value - lineId
        return Outcome.Success(Unit)
    }
}
