package com.nghianguyen.gomuni.domain.repository

import com.nghianguyen.gomuni.domain.model.BusArrival
import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.domain.model.TransitStop
import kotlinx.coroutines.flow.Flow

interface TransitRepository {
    fun getTransitLines(): Flow<List<TransitLine>>
    suspend fun refreshTransitLines(operatorId: String, forceRefresh: Boolean = false)

    fun getTransitStops(lineId: String): Flow<List<TransitStop>>
    suspend fun refreshTransitStops(operatorId: String, lineId: String)

    suspend fun getBusArrivals(stopId: String): List<BusArrival>
}
