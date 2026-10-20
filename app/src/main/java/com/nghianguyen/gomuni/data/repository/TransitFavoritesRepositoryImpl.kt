package com.nghianguyen.gomuni.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.nghianguyen.gomuni.domain.model.FavoritesStorageException
import com.nghianguyen.gomuni.domain.repository.TransitFavoritesRepository
import com.nghianguyen.gomuni.domain.util.ErrorLogger
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

@Singleton
class TransitFavoritesRepositoryImpl
@Inject
constructor(
    private val dataStore: DataStore<Preferences>,
    private val errorLogger: ErrorLogger,
) : TransitFavoritesRepository {
    override fun observeFavoriteIds() =
        dataStore.data
            .map { it[favoriteIdsKey].orEmpty().toSet() }
            .catch { error ->
                if (error !is IOException) throw error
                errorLogger.log("Favorites unavailable", error)
                throw FavoritesStorageException()
            }

    override suspend fun setFavorite(lineId: String, favorite: Boolean) {
        require(lineId.isNotBlank())
        try {
            dataStore.edit { preferences ->
                val ids = preferences[favoriteIdsKey].orEmpty()
                preferences[favoriteIdsKey] = if (favorite) ids + lineId else ids - lineId
            }
        } catch (error: IOException) {
            errorLogger.log("Favorite could not be saved", error)
            throw FavoritesStorageException()
        }
    }
}

private val favoriteIdsKey = stringSetPreferencesKey("favorite_line_ids_sf")
