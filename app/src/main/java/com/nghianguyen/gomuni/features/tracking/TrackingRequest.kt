package com.nghianguyen.gomuni.features.tracking

import kotlinx.serialization.Serializable

/** Immutable target captured when the user asks to track a prediction. */
@Serializable
data class TrackingRequest(
    val requestId: String,
    val stopId: String,
    val stopName: String,
    val lineRef: String,
    val lineName: String,
    val vehicleRef: String?,
)
