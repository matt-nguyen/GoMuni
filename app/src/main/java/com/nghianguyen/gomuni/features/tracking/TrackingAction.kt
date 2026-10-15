package com.nghianguyen.gomuni.features.tracking

/** User intents and platform results handled by [TrackingViewModel.onAction]. */
sealed interface TrackingAction {
    /** Starts a new attempt unless startup is already in progress. */
    data class Start(val request: TrackingRequest) : TrackingAction

    /** Abandons startup when effect collection stops without an active permission dialog. */
    data object StartupInterrupted : TrackingAction

    /** Reports failure to launch the permission request. */
    data object StartupFailed : TrackingAction

    /** Reports permission status and whether Android recommends a rationale after denial. */
    data class PermissionResolved(
        val granted: Boolean,
        val shouldShowRationale: Boolean = false,
    ) : TrackingAction

    /** Reports service-launch acceptance, not arrival-data availability. */
    data class LaunchResolved(val succeeded: Boolean) : TrackingAction

    /** Clears the startup error banner. */
    data object DismissFailure : TrackingAction
}
