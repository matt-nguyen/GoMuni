package com.nghianguyen.gomuni.data.repository

import com.nghianguyen.gomuni.data.datasource.TransitFavoritesLocalDataSource
import com.nghianguyen.gomuni.domain.model.FavoritesError
import com.nghianguyen.gomuni.domain.model.Outcome
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
    private val localDataSource: TransitFavoritesLocalDataSource,
    private val errorLogger: ErrorLogger,
) : TransitFavoritesRepository {
    override fun observeFavoriteIds() =
        localDataSource
            .observeFavoriteIds()
            .map<Set<String>, Outcome<Set<String>, FavoritesError>> { Outcome.Success(it.toSet()) }
            .catch { error ->
                if (error !is IOException) throw error
                errorLogger.log("Favorites unavailable", error)
                emit(Outcome.Failure(FavoritesError.Storage))
            }

    override suspend fun setFavorite(
        lineId: String,
        favorite: Boolean,
    ): Outcome<Unit, FavoritesError> {
        require(lineId.isNotBlank())
        return try {
            localDataSource.setFavorite(lineId, favorite)
            Outcome.Success(Unit)
        } catch (error: IOException) {
            errorLogger.log("Favorite could not be saved", error)
            Outcome.Failure(FavoritesError.Storage)
        }
    }
}
