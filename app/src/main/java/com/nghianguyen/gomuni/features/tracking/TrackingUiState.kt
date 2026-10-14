package com.nghianguyen.gomuni.features.tracking

import kotlinx.serialization.Serializable

@Serializable
sealed interface TrackingUiState {
    @Serializable
    data object Idle : TrackingUiState

    @Serializable
    data class AwaitingPermission(val request: TrackingRequest) : TrackingUiState

    // Persisted before opening the dialog, so recreation does not open it twice.
    @Serializable
    data class PermissionInFlight(val request: TrackingRequest) : TrackingUiState

    @Serializable
    data class ReadyToLaunch(val request: TrackingRequest) : TrackingUiState

    @Serializable
    data class Failed(val requestId: String, val reason: TrackingFailure) : TrackingUiState
}

internal val TrackingUiState.isPending: Boolean
    get() = this is TrackingUiState.AwaitingPermission ||
        this is TrackingUiState.PermissionInFlight || this is TrackingUiState.ReadyToLaunch
