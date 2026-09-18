package com.nghianguyen.gomuni.data.datasource

import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.domain.model.TransitStop
import kotlinx.coroutines.flow.Flow

interface TransitLocalDataSource {
    fun getTransitLines(): Flow<List<TransitLine>>
    suspend fun saveTransitLines(lines: List<TransitLine>)
    suspend fun clearTransitLines()
    
    fun getStopsForLine(lineId: String): Flow<List<TransitStop>>
    suspend fun saveStopsForLine(lineId: String, stops: List<TransitStop>)
    suspend fun deleteStopsForLine(lineId: String)

    suspend fun getLastSyncTime(): Long
    suspend fun saveLastSyncTime(timestamp: Long)
}
