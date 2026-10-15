package com.nghianguyen.gomuni.features.tracking

/** Immutable target captured when the user asks to track a prediction. */
data class TrackingRequest(
    val stopId: String,
    val stopName: String,
    val lineRef: String,
    val lineName: String,
    val vehicleRef: String?,
)
