package com.nghianguyen.gomuni.features.tracking

/** One-time platform commands; not retained or replayed after collection restarts. */
sealed interface TrackingEffect {
    val request: TrackingRequest

    /** Checks or requests notification permission for this startup target. */
    data class RequestNotificationPermission(override val request: TrackingRequest) : TrackingEffect

    /** Asks the route to start the foreground tracking service. */
    data class LaunchTrackingService(override val request: TrackingRequest) : TrackingEffect
}
