package com.nghianguyen.gomuni.data.repository

import com.nghianguyen.gomuni.data.datasource.TransitLocalDataSource
import com.nghianguyen.gomuni.data.datasource.TransitRemoteDataSource
import com.nghianguyen.gomuni.domain.model.BusArrival
import com.nghianguyen.gomuni.domain.model.TransitError
import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.domain.model.TransitStop
import com.nghianguyen.gomuni.domain.repository.TransitRepository
import com.nghianguyen.gomuni.domain.util.ErrorLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransitRepositoryImpl @Inject constructor(
    private val remoteDataSource: TransitRemoteDataSource,
    private val localDataSource: TransitLocalDataSource,
    private val errorLogger: ErrorLogger
) : TransitRepository {
    
    // 24-hour cache expiration
    private val CACHE_EXPIRATION_MS = 24 * 60 * 60 * 1000L

    override fun getTransitLines(): Flow<List<TransitLine>> {
        return localDataSource.getTransitLines()
    }

    override suspend fun refreshTransitLines(operatorId: String, forceRefresh: Boolean) {
        val lastSync = localDataSource.getLastSyncTime()
        val currentTime = System.currentTimeMillis()
        val isCacheEmpty = localDataSource.getTransitLines().first().isEmpty()

        if (forceRefresh || isCacheEmpty || (currentTime - lastSync) > CACHE_EXPIRATION_MS) {
            try {
                val remoteLines = remoteDataSource.getTransitLines(operatorId)
                
                // Atomically update the cache
                localDataSource.clearTransitLines()
                localDataSource.saveTransitLines(remoteLines)
                
                // Save the new sync time in DataStore
                localDataSource.saveLastSyncTime(currentTime)
            } catch (e: Exception) {
                handleError(e, "Transit lines refresh failed for operator: $operatorId", mapOf("operatorId" to operatorId), lastSync == 0L || isCacheEmpty)
            }
        }
    }

    override fun getTransitStops(lineId: String): Flow<List<TransitStop>> {
        return localDataSource.getStopsForLine(lineId)
    }

    override suspend fun refreshTransitStops(operatorId: String, lineId: String) {
        try {
            val remoteStops = remoteDataSource.getTransitStops(operatorId, lineId)
            
            localDataSource.deleteStopsForLine(lineId)
            localDataSource.saveStopsForLine(lineId, remoteStops)
        } catch (e: Exception) {
            handleError(e, "Transit stops refresh failed for line: $lineId", mapOf("operatorId" to operatorId, "lineId" to lineId), false)
        }
    }

    override suspend fun getBusArrivals(stopId: String): List<BusArrival> {
        return try {
            remoteDataSource.getBusArrivals(stopId)
        } catch (e: Exception) {
            handleError(e, "Failed to get bus arrivals for stop: $stopId", mapOf("stopId" to stopId), true)
            emptyList() // unreachable due to rethrow in handleError for shouldRethrow=true
        }
    }

    private fun handleError(e: Exception, logMessage: String, metadata: Map<String, String>, shouldRethrow: Boolean) {
        // Map technical exception to domain error
        val domainError = when (e) {
            is IOException -> TransitError.Network
            is HttpException -> {
                if (e.code() == 404) TransitError.NotFound else TransitError.Unexpected
            }
            else -> TransitError.Unexpected
        }

        // Log the technical details for analytics
        errorLogger.log(
            message = logMessage,
            error = e,
            metadata = metadata + ("domainError" to domainError.javaClass.simpleName)
        )

        if (shouldRethrow) throw domainError
    }
}
