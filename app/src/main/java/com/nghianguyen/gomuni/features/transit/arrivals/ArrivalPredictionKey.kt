package com.nghianguyen.gomuni.features.transit.arrivals

import com.nghianguyen.gomuni.domain.model.BusArrival

/** Identity of a prediction in the current provider snapshot, not just its vehicle. */
data class ArrivalPredictionKey(
    val lineRef: String,
    val vehicleRef: String?,
    val expectedArrival: String?,
    val aimedArrival: String?,
) {
    companion object {
        fun from(arrival: BusArrival) = ArrivalPredictionKey(
            arrival.lineRef, arrival.vehicleRef, arrival.expectedArrival, arrival.aimedArrival,
        )
    }
}
