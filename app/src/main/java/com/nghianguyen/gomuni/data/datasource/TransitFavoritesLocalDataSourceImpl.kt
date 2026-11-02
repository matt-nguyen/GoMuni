package com.nghianguyen.gomuni.data.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import javax.inject.Inject
import kotlinx.coroutines.flow.map

class TransitFavoritesLocalDataSourceImpl
@Inject
constructor(private val dataStore: DataStore<Preferences>) : TransitFavoritesLocalDataSource {
    override fun observeFavoriteIds() = dataStore.data.map { it[favoriteIdsKey].orEmpty().toSet() }

    override suspend fun setFavorite(lineId: String, favorite: Boolean) {
        dataStore.edit { preferences ->
            val ids = preferences[favoriteIdsKey].orEmpty()
            preferences[favoriteIdsKey] = if (favorite) ids + lineId else ids - lineId
        }
    }
}

private val favoriteIdsKey = stringSetPreferencesKey("favorite_line_ids_sf")
