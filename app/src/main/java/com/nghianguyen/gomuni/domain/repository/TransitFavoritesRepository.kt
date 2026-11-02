package com.nghianguyen.gomuni.domain.repository

import com.nghianguyen.gomuni.domain.model.FavoritesError
import com.nghianguyen.gomuni.domain.model.Outcome
import kotlinx.coroutines.flow.Flow

/** Local SF favorites, independent of the available transit datasets. */
interface TransitFavoritesRepository {
    /** Storage failures emit Failure and complete; a new collection retries observation. */
    fun observeFavoriteIds(): Flow<Outcome<Set<String>, FavoritesError>>

    /** Atomically sets membership. Repeating the same request is idempotent. */
    suspend fun setFavorite(lineId: String, favorite: Boolean): Outcome<Unit, FavoritesError>
}
