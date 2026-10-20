package com.nghianguyen.gomuni.features.transit.testing

import com.nghianguyen.gomuni.domain.model.BusArrival
import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.domain.model.TransitStop
import com.nghianguyen.gomuni.domain.repository.TransitRepository
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeTransitRepository : TransitRepository {
    val lines = MutableStateFlow<List<TransitLine>>(emptyList())
    val stops = MutableStateFlow<List<TransitStop>>(emptyList())
    var lineLoads = 0
    var stopLoads = 0
    var loadLines: suspend () -> Unit = {}
    var loadStops: suspend () -> Unit = {}
    var arrivals: suspend () -> List<BusArrival> = { emptyList() }

    override fun getTransitLines() = lines

    override suspend fun ensureTransitLinesLoaded(operatorId: String) {
        lineLoads++
        loadLines()
    }

    override fun getTransitStops(lineId: String) = stops

    override suspend fun ensureTransitStopsLoaded(operatorId: String, lineId: String) {
        stopLoads++
        loadStops()
    }

    override suspend fun getBusArrivals(stopId: String) = arrivals()
}
