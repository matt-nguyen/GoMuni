package com.nghianguyen.gomuni.domain.model

data class BusArrival(
    val lineRef: String,
    val lineName: String,
    val expectedArrival: String?,
    val aimedArrival: String?,
    val vehicleRef: String?,
    val latitude: Double,
    val longitude: Double
)
