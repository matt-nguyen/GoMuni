package com.nghianguyen.gomuni.features.tracking



sealed interface TrackingAction {
    data class Start(val request: TrackingRequest) : TrackingAction
    data class PermissionRequested(val requestId: String) : TrackingAction
    data class PermissionRequestFailed(val requestId: String) : TrackingAction
    data class PermissionResolved(val requestId: String, val granted: Boolean) : TrackingAction
    data class LaunchResolved(val requestId: String, val succeeded: Boolean) : TrackingAction
    data class DismissFailure(val requestId: String) : TrackingAction
}
