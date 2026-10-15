package com.nghianguyen.gomuni.features.tracking

/** Rendering data only; platform requests are exposed separately. */
data class TrackingUiState(
    val isStarting: Boolean = false,
    val error: TrackingErrorUiState? = null,
)

data class TrackingErrorUiState(
    val reason: TrackingFailure,
    val shouldShowRationale: Boolean = false,
)
