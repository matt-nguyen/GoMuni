package com.nghianguyen.gomuni.data.datasource

import com.nghianguyen.gomuni.domain.model.BusArrival
import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.domain.model.TransitStop

interface TransitRemoteDataSource {
    suspend fun getTransitLines(operatorId: String): List<TransitLine>
    suspend fun getTransitStops(operatorId: String, lineId: String): List<TransitStop>
    suspend fun getBusArrivals(stopId: String): List<BusArrival>
}
