package com.nghianguyen.gomuni.features.transit.testing

import com.nghianguyen.gomuni.domain.model.BusArrival
import com.nghianguyen.gomuni.domain.model.Outcome
import com.nghianguyen.gomuni.domain.model.TransitError
import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.domain.model.TransitStop
import com.nghianguyen.gomuni.domain.repository.TransitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal class FakeTransitRepository : TransitRepository {
    val lines = MutableStateFlow<List<TransitLine>>(emptyList())
    val stops = MutableStateFlow<List<TransitStop>>(emptyList())
    var lineLoadFailure: TransitError? = null
    var stopLoadFailure: TransitError? = null
    var arrivalsFailure: TransitError? = null
    var lineLoads = 0
    var stopLoads = 0
    var loadLines: suspend () -> Unit = {}
    var loadStops: suspend () -> Unit = {}
    var arrivals: suspend () -> List<BusArrival> = { emptyList() }

    override fun getTransitLines() = lines.map { Outcome.Success(it) }

    override suspend fun ensureTransitLinesLoaded(operatorId: String): Outcome<Unit, TransitError> {
        lineLoads++
        loadLines()
        return lineLoadFailure?.let { Outcome.Failure(it) } ?: Outcome.Success(Unit)
    }

    override fun getTransitStops(lineId: String) = stops.map { Outcome.Success(it) }

    override suspend fun ensureTransitStopsLoaded(
        operatorId: String,
        lineId: String,
    ): Outcome<Unit, TransitError> {
        stopLoads++
        loadStops()
        return stopLoadFailure?.let { Outcome.Failure(it) } ?: Outcome.Success(Unit)
    }

    override suspend fun getBusArrivals(stopId: String): Outcome<List<BusArrival>, TransitError> =
        arrivalsFailure?.let { Outcome.Failure(it) } ?: Outcome.Success(arrivals())
}
