package com.nghianguyen.gomuni.features.tracking

import kotlinx.serialization.Serializable

@Serializable
enum class TrackingFailure {
    PERMISSION_DENIED,
    STARTUP_FAILED,
}
