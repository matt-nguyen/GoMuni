package com.nghianguyen.gomuni.data.repository

import android.database.sqlite.SQLiteException
import com.nghianguyen.gomuni.data.datasource.TransitLocalDataSource
import com.nghianguyen.gomuni.data.datasource.TransitRemoteDataSource
import com.nghianguyen.gomuni.domain.model.BusArrival
import com.nghianguyen.gomuni.domain.model.Outcome
import com.nghianguyen.gomuni.domain.model.TransitError
import com.nghianguyen.gomuni.domain.repository.TransitRepository
import com.nghianguyen.gomuni.domain.util.ErrorLogger
import java.io.IOException
import java.net.HttpURLConnection
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

@Singleton
class TransitRepositoryImpl
@Inject
constructor(
    private val remoteDataSource: TransitRemoteDataSource,
    private val localDataSource: TransitLocalDataSource,
    private val errorLogger: ErrorLogger,
) : TransitRepository {
    private val initialLoadMutex = Mutex()

    override fun getTransitLines() = observeStorage(localDataSource.getTransitLines())

    override fun getTransitStops(lineId: String) =
        observeStorage(localDataSource.getStopsForLine(lineId))

    override suspend fun ensureTransitLinesLoaded(operatorId: String): Outcome<Unit, TransitError> {
        require(operatorId == "SF")
        return initialLoadMutex.withLock { loadLinesIfNeeded(operatorId) }
    }

    private suspend fun loadLinesIfNeeded(operatorId: String): Outcome<Unit, TransitError> {
        return when (val cached = getTransitLines().first()) {
            is Outcome.Failure -> cached
            is Outcome.Success ->
                if (cached.value.isNotEmpty()) {
                    Outcome.Success(Unit)
                } else
                    when (
                        val fetched = remoteResult { remoteDataSource.getTransitLines(operatorId) }
                    ) {
                        is Outcome.Failure -> fetched
                        is Outcome.Success ->
                            storageResult {
                                localDataSource.storeInitialLines(operatorId, fetched.value)
                            }
                    }
        }
    }

    override suspend fun ensureTransitStopsLoaded(
        operatorId: String,
        lineId: String,
    ): Outcome<Unit, TransitError> {
        require(operatorId == "SF" && lineId.isNotBlank())
        return initialLoadMutex.withLock {
            when (val lines = loadLinesIfNeeded(operatorId)) {
                is Outcome.Failure -> return@withLock lines
                is Outcome.Success -> Unit
            }
            when (val cached = getTransitStops(lineId).first()) {
                is Outcome.Failure -> return@withLock cached
                is Outcome.Success ->
                    if (cached.value.isNotEmpty()) return@withLock Outcome.Success(Unit)
            }
            when (
                val fetched = remoteResult { remoteDataSource.getTransitStops(operatorId, lineId) }
            ) {
                is Outcome.Failure -> fetched
                is Outcome.Success ->
                    storageResult {
                        localDataSource.storeInitialStops(operatorId, lineId, fetched.value)
                    }
            }
        }
    }

    override suspend fun getBusArrivals(stopId: String): Outcome<List<BusArrival>, TransitError> =
        remoteResult {
            remoteDataSource.getBusArrivals(stopId)
        }

    private fun <T> observeStorage(source: Flow<T>): Flow<Outcome<T, TransitError>> =
        source
            .map<T, Outcome<T, TransitError>> { Outcome.Success(it) }
            .catch { error ->
                when (error) {
                    is IOException,
                    is SQLiteException -> {
                        errorLogger.log("Transit storage observation failed", error)
                        emit(Outcome.Failure(TransitError.Storage))
                    }
                    else -> throw error
                }
            }

    private suspend fun <T> storageResult(block: suspend () -> T): Outcome<T, TransitError> =
        try {
            Outcome.Success(block())
        } catch (error: IOException) {
            storageFailure(error)
        } catch (error: SQLiteException) {
            storageFailure(error)
        }

    private fun storageFailure(error: Exception): Outcome.Failure<TransitError> {
        errorLogger.log("Transit storage write failed", error)
        return Outcome.Failure(TransitError.Storage)
    }

    private suspend fun <T> remoteResult(block: suspend () -> T): Outcome<T, TransitError> =
        try {
            Outcome.Success(block())
        } catch (error: IOException) {
            errorLogger.log("Transit network request failed", error)
            Outcome.Failure(TransitError.Network)
        } catch (error: HttpException) {
            errorLogger.log("Transit service request failed", error)
            Outcome.Failure(
                if (error.code() == HttpURLConnection.HTTP_NOT_FOUND) TransitError.NotFound
                else TransitError.ServiceUnavailable,
            )
        }
}
