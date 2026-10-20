package com.nghianguyen.gomuni.domain.repository

import com.nghianguyen.gomuni.domain.model.BusArrival
import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.domain.model.TransitStop
import kotlinx.coroutines.flow.Flow

interface TransitRepository {
    fun getTransitLines(): Flow<List<TransitLine>>

    /** Loads once when needed. Successful empty datasets are also persisted as complete. */
    suspend fun ensureTransitLinesLoaded(operatorId: String)

    fun getTransitStops(lineId: String): Flow<List<TransitStop>>

    /** Ensures line metadata first, then loads this route once. Failed loads may be retried. */
    suspend fun ensureTransitStopsLoaded(operatorId: String, lineId: String)

    suspend fun getBusArrivals(stopId: String): List<BusArrival>
}
