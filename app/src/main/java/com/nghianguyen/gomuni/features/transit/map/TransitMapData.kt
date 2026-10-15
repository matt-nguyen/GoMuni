package com.nghianguyen.gomuni.features.transit.map

import com.nghianguyen.gomuni.domain.model.BusArrival
import com.nghianguyen.gomuni.domain.model.TransitStop

internal data class TransitMapData(val stops: List<TransitStop>, val vehicles: List<BusArrival>) {
    companion object {
        fun from(
            routeStops: List<TransitStop>,
            selectedStopId: String?,
            lineId: String?,
            arrivals: ArrivalMapProjection?,
        ): TransitMapData {
            val stops = routeStops.filter { validLocation(it.latitude, it.longitude) }
            val vehicles =
                if (arrivals?.stopId == selectedStopId && lineId != null) {
                    arrivals
                        ?.predictions
                        .orEmpty()
                        .filter {
                            it.lineRef == lineId && validLocation(it.latitude, it.longitude)
                        }
                        .distinctBy { vehicleKey(it) }
                } else emptyList()
            return TransitMapData(stops, vehicles)
        }
    }
}

internal data class ArrivalMapProjection(val stopId: String, val predictions: List<BusArrival>)

internal fun vehicleKey(bus: BusArrival): String =
    bus.vehicleRef ?: "${bus.lineRef}:${bus.latitude}:${bus.longitude}"

private fun validLocation(latitude: Double, longitude: Double): Boolean =
    latitude.isFinite() &&
        longitude.isFinite() &&
        latitude in -MAX_LATITUDE..MAX_LATITUDE &&
        longitude in -MAX_LONGITUDE..MAX_LONGITUDE &&
        !(latitude == 0.0 && longitude == 0.0)

private const val MAX_LATITUDE = 90.0
private const val MAX_LONGITUDE = 180.0
