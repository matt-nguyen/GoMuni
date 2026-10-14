package com.nghianguyen.gomuni.features.transit.map

import com.nghianguyen.gomuni.domain.model.BusArrival
import com.nghianguyen.gomuni.domain.model.TransitStop
import org.junit.Assert.*
import org.junit.Test

class TransitMapDataTest {
    @Test fun mapFiltersInvalidCoordinatesOtherRoutesAndDuplicateVehicles() {
        val stop = TransitStop("123", "Market", 37.78, -122.41, null, "38")
        val bus = BusArrival("38", "Geary", null, null, "bus1", 37.78, -122.41)
        val data = TransitMapData.from(listOf(stop, stop.copy(id = "invalid", latitude = 91.0)),
            "123", "38", ArrivalMapProjection("123", listOf(bus, bus,
                bus.copy(lineRef = "1", vehicleRef = "other"), bus.copy(vehicleRef = "invalid", longitude = Double.NaN))))
        assertEquals(listOf(stop), data.stops)
        assertEquals(listOf(bus), data.vehicles)
    }

    @Test fun predictionsFromAnotherStopDoNotAppearOnCurrentMap() {
        val bus = BusArrival("38", "Geary", null, null, "bus1", 37.78, -122.41)
        val data = TransitMapData.from(emptyList(), "456", "38", ArrivalMapProjection("123", listOf(bus)))
        assertTrue(data.vehicles.isEmpty())
    }
}
