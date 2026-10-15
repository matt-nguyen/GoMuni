package com.nghianguyen.gomuni.features.transit.testing

import com.nghianguyen.gomuni.domain.model.BusArrival
import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.domain.model.TransitStop
import com.nghianguyen.gomuni.domain.repository.TransitRepository
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeTransitRepository : TransitRepository {
    val lines = MutableStateFlow<List<TransitLine>>(emptyList())
    val stops = MutableStateFlow<List<TransitStop>>(emptyList())
    var lineRefreshes = 0
    var stopRefreshes = 0
    var refreshLines: suspend () -> Unit = {}
    var refreshStops: suspend () -> Unit = {}
    var arrivals: suspend () -> List<BusArrival> = { emptyList() }

    override fun getTransitLines() = lines

    override suspend fun refreshTransitLines(operatorId: String, forceRefresh: Boolean) {
        lineRefreshes++
        refreshLines()
    }

    override fun getTransitStops(lineId: String) = stops

    override suspend fun refreshTransitStops(operatorId: String, lineId: String) {
        stopRefreshes++
        refreshStops()
    }

    override suspend fun getBusArrivals(stopId: String) = arrivals()
}
