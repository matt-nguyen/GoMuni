package com.nghianguyen.gomuni.features.transit.testing

import com.nghianguyen.gomuni.domain.repository.TransitFavoritesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

internal class FakeTransitFavoritesRepository : TransitFavoritesRepository {
    val ids = MutableStateFlow<Set<String>>(emptySet())
    private val readFailures = MutableStateFlow<Exception?>(null)
    var readFailure: Exception?
        get() = readFailures.value
        set(value) {
            readFailures.value = value
        }

    var beforeWrite: suspend () -> Unit = {}
    var writes = 0

    override fun observeFavoriteIds() =
        combine(ids, readFailures) { current, failure ->
            failure?.let { throw it }
            current
        }

    override suspend fun setFavorite(lineId: String, favorite: Boolean) {
        writes++
        beforeWrite()
        ids.value = if (favorite) ids.value + lineId else ids.value - lineId
    }
}
