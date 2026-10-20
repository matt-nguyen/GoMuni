package com.nghianguyen.gomuni.data.repository.testing

import com.nghianguyen.gomuni.data.datasource.TransitLocalDataSource
import com.nghianguyen.gomuni.data.datasource.TransitRemoteDataSource
import com.nghianguyen.gomuni.domain.model.BusArrival
import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.domain.model.TransitStop
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeLocalDataSource : TransitLocalDataSource {
    val lines = MutableStateFlow(listOf(TransitLine("19", "Polk", "bus")))
    var cleared = false
    val stops = MutableStateFlow(emptyList<TransitStop>())

    override fun getTransitLines() = lines

    override fun getStopsForLine(lineId: String) = stops

    override suspend fun storeInitialLines(operatorId: String, lines: List<TransitLine>) {
        this.lines.value = lines
    }

    override suspend fun storeInitialStops(
        operatorId: String,
        lineId: String,
        stops: List<TransitStop>,
    ) {
        this.stops.value = stops
    }
}

internal class FakeRemoteDataSource : TransitRemoteDataSource {
    var failure: Exception? = null
    var lineCalls = 0
    var stopCalls = 0
    var lineResult = listOf(TransitLine("19", "Polk", "bus"))
    var beforeLines: suspend () -> Unit = {}

    override suspend fun getTransitLines(operatorId: String): List<TransitLine> {
        lineCalls++
        beforeLines()
        failure?.let { throw it }
        return lineResult
    }

    override suspend fun getTransitStops(operatorId: String, lineId: String): List<TransitStop> {
        stopCalls++
        failure?.let { throw it }
        return emptyList()
    }

    override suspend fun getBusArrivals(stopId: String): List<BusArrival> {
        failure?.let { throw it }
        return emptyList()
    }
}
