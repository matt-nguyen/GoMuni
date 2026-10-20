package com.nghianguyen.gomuni.domain.repository

import kotlinx.coroutines.flow.Flow

/** Local SF favorites, independent of the available transit datasets. */
interface TransitFavoritesRepository {
    /** Observes confirmed IDs; storage failures propagate rather than becoming empty favorites. */
    fun observeFavoriteIds(): Flow<Set<String>>

    /** Atomically sets membership. Repeating the same request is idempotent. */
    suspend fun setFavorite(lineId: String, favorite: Boolean)
}
