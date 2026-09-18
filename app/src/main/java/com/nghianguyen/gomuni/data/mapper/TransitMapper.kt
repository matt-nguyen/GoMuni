package com.nghianguyen.gomuni.data.mapper

import com.nghianguyen.gomuni.data.local.entity.TransitLineEntity
import com.nghianguyen.gomuni.data.local.entity.TransitStopEntity
import com.nghianguyen.gomuni.data.remote.model.MonitoredVehicleJourney
import com.nghianguyen.gomuni.data.remote.model.ScheduledStopPointDto
import com.nghianguyen.gomuni.data.remote.model.TransitLineDto
import com.nghianguyen.gomuni.domain.model.BusArrival
import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.domain.model.TransitStop

fun TransitLineDto.toDomain(): TransitLine {
    return TransitLine(
        id = id,
        name = name,
        transportMode = transportMode
    )
}

fun TransitLineEntity.toDomain(): TransitLine {
    return TransitLine(
        id = id,
        name = name,
        transportMode = transportMode
    )
}

fun TransitLine.toEntity(): TransitLineEntity {
    return TransitLineEntity(
        id = id,
        name = name,
        transportMode = transportMode,
        // Using defaults or empty   strings for fields not in domain model yet
        fromDate = "",
        toDate = "",
        publicCode = id,
        siriLineRef = id,
        monitored = true,
        operatorRef = ""
    )
}

fun ScheduledStopPointDto.toDomain(lineId: String): TransitStop {
    return TransitStop(
        id = id,
        name = name,
        latitude = location.latitude.toDoubleOrNull() ?: 0.0,
        longitude = location.longitude.toDoubleOrNull() ?: 0.0,
        stopType = stopType,
        lineId = lineId
    )
}

fun TransitStopEntity.toDomain(): TransitStop {
    return TransitStop(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        stopType = stopType,
        lineId = lineId
    )
}

fun TransitStop.toEntity(): TransitStopEntity {
    return TransitStopEntity(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        stopType = stopType,
        lineId = lineId
    )
}

fun MonitoredVehicleJourney.toDomain(): BusArrival {
    return BusArrival(
        lineRef = lineRef,
        lineName = publishedLineName,
        expectedArrival = monitoredCall.expectedArrivalTime,
        aimedArrival = monitoredCall.aimedArrivalTime,
        vehicleRef = vehicleRef,
        latitude = vehicleLocation.latitude.toDoubleOrNull() ?: 0.0,
        longitude = vehicleLocation.longitude.toDoubleOrNull() ?: 0.0
    )
}
