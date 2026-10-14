package com.nghianguyen.gomuni.features.tracking

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.*
import org.junit.Test

class TrackingCoordinatorTest {
    private val request = TrackingRequest("request1", "123", "Market", "38", "Geary", "bus1")

    @Test fun permissionAndLaunchResultsMustMatchPendingRequestAndPhase() {
        val vm = TrackingCoordinatorViewModel(SavedStateHandle())
        vm.onAction(TrackingAction.Start(request))
        vm.onAction(TrackingAction.Start(request.copy(requestId = "duplicate")))
        vm.onAction(TrackingAction.LaunchResolved(request.requestId, true))
        vm.onAction(TrackingAction.PermissionResolved("stale", true))
        assertEquals(TrackingUiState.AwaitingPermission(request), vm.uiState.value)
        vm.onAction(TrackingAction.PermissionRequested(request.requestId))
        assertEquals(TrackingUiState.PermissionInFlight(request), vm.uiState.value)
        vm.onAction(TrackingAction.PermissionResolved(request.requestId, true))
        assertEquals(TrackingUiState.ReadyToLaunch(request), vm.uiState.value)
        vm.onAction(TrackingAction.LaunchResolved(request.requestId, true))
        assertEquals(TrackingUiState.Idle, vm.uiState.value)
        vm.onAction(TrackingAction.Start(request))
        assertEquals(TrackingUiState.Idle, vm.uiState.value)
    }

    @Test fun startupRestoresInFlightPermissionWithoutRestartingItsPhase() {
        val saved = SavedStateHandle()
        val original = TrackingCoordinatorViewModel(saved)
        original.onAction(TrackingAction.Start(request))
        original.onAction(TrackingAction.PermissionRequested(request.requestId))
        val recreated = TrackingCoordinatorViewModel(saved)
        assertEquals(TrackingUiState.PermissionInFlight(request), recreated.uiState.value)
        recreated.onAction(TrackingAction.PermissionResolved(request.requestId, false))
        assertEquals(TrackingUiState.Failed(request.requestId, TrackingFailure.PERMISSION_DENIED), recreated.uiState.value)
        recreated.onAction(TrackingAction.DismissFailure("stale"))
        assertTrue(recreated.uiState.value is TrackingUiState.Failed)
        recreated.onAction(TrackingAction.DismissFailure(request.requestId))
        assertEquals(TrackingUiState.Idle, recreated.uiState.value)
    }

    @Test fun launchFailureIsRecoverableWithANewRequest() {
        val vm = TrackingCoordinatorViewModel(SavedStateHandle())
        vm.onAction(TrackingAction.Start(request))
        vm.onAction(TrackingAction.PermissionResolved(request.requestId, true))
        vm.onAction(TrackingAction.LaunchResolved(request.requestId, false))
        assertEquals(TrackingUiState.Failed(request.requestId, TrackingFailure.STARTUP_FAILED), vm.uiState.value)
        val retry = request.copy(requestId = "retry")
        vm.onAction(TrackingAction.Start(retry))
        assertEquals(TrackingUiState.AwaitingPermission(retry), vm.uiState.value)
    }
}
