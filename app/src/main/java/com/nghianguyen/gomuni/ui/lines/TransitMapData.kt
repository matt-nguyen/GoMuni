package com.nghianguyen.gomuni.ui.lines

import com.nghianguyen.gomuni.domain.model.BusArrival
import com.nghianguyen.gomuni.domain.model.TransitStop

internal data class TransitMapData(val stops: List<TransitStop>, val vehicles: List<BusArrival>) {
    companion object {
        fun from(route: RouteStopsUiState?, arrivals: StopArrivalsUiState?): TransitMapData {
            val stops = route?.stops.orEmpty().filter { validLocation(it.latitude, it.longitude) }
            val vehicles = if (arrivals?.stopId == route?.selectedStopId && route != null) {
                arrivals?.predictions.orEmpty().filter {
                    it.lineRef == route.route.lineId && validLocation(it.latitude, it.longitude)
                }.distinctBy { vehicleKey(it) }
            } else emptyList()
            return TransitMapData(stops, vehicles)
        }
    }
}

internal fun vehicleKey(bus: BusArrival): String = bus.vehicleRef ?: "${bus.lineRef}:${bus.latitude}:${bus.longitude}"

private fun validLocation(latitude: Double, longitude: Double): Boolean =
    latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 &&
        longitude in -180.0..180.0 && !(latitude == 0.0 && longitude == 0.0)
