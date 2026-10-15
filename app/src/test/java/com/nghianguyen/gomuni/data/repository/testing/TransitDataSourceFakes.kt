package com.nghianguyen.gomuni.data.repository.testing

import com.nghianguyen.gomuni.data.datasource.TransitLocalDataSource
import com.nghianguyen.gomuni.data.datasource.TransitRemoteDataSource
import com.nghianguyen.gomuni.domain.model.BusArrival
import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.domain.model.TransitStop
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

internal class FakeLocalDataSource : TransitLocalDataSource {
    val lines = MutableStateFlow(listOf(TransitLine("19", "Polk", "bus")))
    var lastSync = 1L
    var cleared = false

    override fun getTransitLines() = lines

    override suspend fun saveTransitLines(lines: List<TransitLine>) {
        this.lines.value = lines
    }

    override suspend fun clearTransitLines() {
        cleared = true
        lines.value = emptyList()
    }

    override fun getStopsForLine(lineId: String) = flowOf(emptyList<TransitStop>())

    override suspend fun saveStopsForLine(lineId: String, stops: List<TransitStop>) = Unit

    override suspend fun deleteStopsForLine(lineId: String) = Unit

    override suspend fun getLastSyncTime() = lastSync

    override suspend fun saveLastSyncTime(timestamp: Long) {
        lastSync = timestamp
    }
}

internal class FakeRemoteDataSource : TransitRemoteDataSource {
    var failure: Exception? = null

    override suspend fun getTransitLines(operatorId: String): List<TransitLine> {
        failure?.let { throw it }
        return emptyList()
    }

    override suspend fun getTransitStops(operatorId: String, lineId: String): List<TransitStop> {
        failure?.let { throw it }
        return emptyList()
    }

    override suspend fun getBusArrivals(stopId: String): List<BusArrival> {
        failure?.let { throw it }
        return emptyList()
    }
}
