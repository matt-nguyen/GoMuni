package com.nghianguyen.gomuni.data.repository

import com.nghianguyen.gomuni.data.datasource.TransitLocalDataSource
import com.nghianguyen.gomuni.data.datasource.TransitRemoteDataSource
import com.nghianguyen.gomuni.domain.model.TransitLine
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransitRepository @Inject constructor(
    private val remoteDataSource: TransitRemoteDataSource,
    private val localDataSource: TransitLocalDataSource
) {
    // 24-hour cache expiration
    private val CACHE_EXPIRATION_MS = 24 * 60 * 60 * 1000L

    /**
     * Exposes the transit lines from the local database as a Flow.
     * The UI will observe this as the Single Source of Truth.
     */
    fun getTransitLinesFlow(): Flow<List<TransitLine>> {
        return localDataSource.getTransitLines()
    }

    /**
     * Refreshes the local cache from the remote API if the cache has expired.
     */
    suspend fun refreshTransitLines(operatorId: String, forceRefresh: Boolean = false) {
        val lastSync = localDataSource.getLastSyncTime()
        val currentTime = System.currentTimeMillis()

        if (forceRefresh || (currentTime - lastSync) > CACHE_EXPIRATION_MS) {
            try {
                val remoteLines = remoteDataSource.getTransitLines(operatorId)
                
                // Atomically update the cache
                localDataSource.clearTransitLines()
                localDataSource.saveTransitLines(remoteLines)
                
                // Save the new sync time in DataStore
                localDataSource.saveLastSyncTime(currentTime)
            } catch (e: Exception) {
                // If there's no cache, rethrow. Otherwise, fail silently so UI shows stale data.
                if (lastSync == 0L) throw e
            }
        }
    }
}
