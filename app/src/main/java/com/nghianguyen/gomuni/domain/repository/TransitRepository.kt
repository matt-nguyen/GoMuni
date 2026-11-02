package com.nghianguyen.gomuni.domain.repository

import com.nghianguyen.gomuni.domain.model.BusArrival
import com.nghianguyen.gomuni.domain.model.Outcome
import com.nghianguyen.gomuni.domain.model.TransitError
import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.domain.model.TransitStop
import kotlinx.coroutines.flow.Flow

interface TransitRepository {
    /** Storage failures emit Failure and complete; a new collection retries observation. */
    fun getTransitLines(): Flow<Outcome<List<TransitLine>, TransitError>>

    /** Uses cached lines when present; empty datasets are fetched on later load attempts. */
    suspend fun ensureTransitLinesLoaded(operatorId: String): Outcome<Unit, TransitError>

    /** Storage failures emit Failure and complete; a new collection retries observation. */
    fun getTransitStops(lineId: String): Flow<Outcome<List<TransitStop>, TransitError>>

    /** Ensures line metadata first, then fetches stops if none are cached for this line. */
    suspend fun ensureTransitStopsLoaded(
        operatorId: String,
        lineId: String,
    ): Outcome<Unit, TransitError>

    suspend fun getBusArrivals(stopId: String): Outcome<List<BusArrival>, TransitError>
}
