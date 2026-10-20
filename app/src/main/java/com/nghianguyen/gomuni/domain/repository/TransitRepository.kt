package com.nghianguyen.gomuni.domain.repository

import com.nghianguyen.gomuni.domain.model.BusArrival
import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.domain.model.TransitStop
import kotlinx.coroutines.flow.Flow

interface TransitRepository {
    fun getTransitLines(): Flow<List<TransitLine>>

    /** Uses cached lines when present; empty datasets are fetched on later load attempts. */
    suspend fun ensureTransitLinesLoaded(operatorId: String)

    fun getTransitStops(lineId: String): Flow<List<TransitStop>>

    /** Ensures line metadata first, then fetches stops if none are cached for this line. */
    suspend fun ensureTransitStopsLoaded(operatorId: String, lineId: String)

    suspend fun getBusArrivals(stopId: String): List<BusArrival>
}
