package com.nghianguyen.gomuni.data.datasource

import kotlinx.coroutines.flow.Flow

interface TransitFavoritesLocalDataSource {
    fun observeFavoriteIds(): Flow<Set<String>>

    suspend fun setFavorite(lineId: String, favorite: Boolean)
}
