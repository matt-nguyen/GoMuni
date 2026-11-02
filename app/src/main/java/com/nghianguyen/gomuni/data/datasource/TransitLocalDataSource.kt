package com.nghianguyen.gomuni.data.datasource

import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.domain.model.TransitStop
import kotlinx.coroutines.flow.Flow

interface TransitLocalDataSource {
    fun getTransitLines(): Flow<List<TransitLine>>

    fun getStopsForLine(lineId: String): Flow<List<TransitStop>>

    suspend fun storeInitialLines(operatorId: String, lines: List<TransitLine>)

    suspend fun storeInitialStops(operatorId: String, lineId: String, stops: List<TransitStop>)
}
