package com.nghianguyen.gomuni.data.repository

import com.nghianguyen.gomuni.data.datasource.TransitLocalDataSource
import com.nghianguyen.gomuni.data.datasource.TransitRemoteDataSource
import com.nghianguyen.gomuni.domain.model.BusArrival
import com.nghianguyen.gomuni.domain.model.TransitError
import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.domain.model.TransitStop
import com.nghianguyen.gomuni.domain.repository.TransitRepository
import com.nghianguyen.gomuni.domain.util.ErrorLogger
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

// Translate storage, parsing and transport failures at the data boundary; preserve cancellation.
@Suppress("TooGenericExceptionCaught")
@Singleton
class TransitRepositoryImpl
@Inject
constructor(
    private val remoteDataSource: TransitRemoteDataSource,
    private val localDataSource: TransitLocalDataSource,
    private val errorLogger: ErrorLogger,
) : TransitRepository {

    override fun getTransitLines(): Flow<List<TransitLine>> {
        return localDataSource.getTransitLines()
    }

    private val initialLoadMutex = kotlinx.coroutines.sync.Mutex()

    override suspend fun ensureTransitLinesLoaded(operatorId: String) {
        require(operatorId == "SF")
        initialLoadMutex.withLock { loadLinesIfNeeded(operatorId) }
    }

    private suspend fun loadLinesIfNeeded(operatorId: String) {
        try {
            if (localDataSource.isLoaded("lines:$operatorId")) return
            val lines = remoteDataSource.getTransitLines(operatorId)
            localDataSource.storeInitialLines(operatorId, lines)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            handleError(error, "Initial lines load failed", mapOf("operatorId" to operatorId), true)
        }
    }

    override fun getTransitStops(lineId: String): Flow<List<TransitStop>> =
        localDataSource.getStopsForLine(lineId)

    override suspend fun ensureTransitStopsLoaded(operatorId: String, lineId: String) {
        require(operatorId == "SF" && lineId.isNotBlank())
        initialLoadMutex.withLock {
            loadLinesIfNeeded(operatorId)
            try {
                if (!localDataSource.isLoaded("stops:$operatorId:$lineId")) {
                    val stops = remoteDataSource.getTransitStops(operatorId, lineId)
                    localDataSource.storeInitialStops(operatorId, lineId, stops)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                handleError(error, "Initial stops load failed", mapOf("lineId" to lineId), true)
            }
        }
    }

    override suspend fun getBusArrivals(stopId: String): List<BusArrival> {
        return try {
            remoteDataSource.getBusArrivals(stopId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            handleError(
                e,
                "Failed to get bus arrivals for stop: $stopId",
                mapOf("stopId" to stopId),
                true,
            )
            emptyList() // unreachable due to rethrow in handleError for shouldRethrow=true
        }
    }

    private fun handleError(
        e: Exception,
        logMessage: String,
        metadata: Map<String, String>,
        shouldRethrow: Boolean,
    ) {
        // Map technical exception to domain error
        val domainError =
            when (e) {
                is IOException -> TransitError.Network
                is HttpException -> {
                    if (e.code() == java.net.HttpURLConnection.HTTP_NOT_FOUND) TransitError.NotFound
                    else TransitError.Unexpected
                }
                else -> TransitError.Unexpected
            }

        // Log the technical details for analytics
        errorLogger.log(
            message = logMessage,
            error = e,
            metadata = metadata + ("domainError" to domainError.javaClass.simpleName),
        )

        if (shouldRethrow) throw domainError
    }
}
