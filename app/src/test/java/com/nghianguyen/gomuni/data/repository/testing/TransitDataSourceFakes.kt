package com.nghianguyen.gomuni.data.repository.testing

import com.nghianguyen.gomuni.data.datasource.TransitLocalDataSource
import com.nghianguyen.gomuni.data.datasource.TransitRemoteDataSource
import com.nghianguyen.gomuni.domain.model.BusArrival
import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.domain.model.TransitStop
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

internal class FakeLocalDataSource : TransitLocalDataSource {
    val lines = MutableStateFlow(listOf(TransitLine("19", "Polk", "bus")))
    var readFailure: Exception? = null
    var writeFailure: Exception? = null
    var cleared = false
    val stops = MutableStateFlow(emptyList<TransitStop>())

    override fun getTransitLines() = flow {
        readFailure?.let { throw it }
        emitAll(lines)
    }

    override fun getStopsForLine(lineId: String) = flow {
        readFailure?.let { throw it }
        emitAll(stops)
    }

    override suspend fun storeInitialLines(operatorId: String, lines: List<TransitLine>) {
        writeFailure?.let { throw it }
        this.lines.value = lines
    }

    override suspend fun storeInitialStops(
        operatorId: String,
        lineId: String,
        stops: List<TransitStop>,
    ) {
        writeFailure?.let { throw it }
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
