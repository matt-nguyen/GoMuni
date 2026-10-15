package com.nghianguyen.gomuni.features.tracking

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrackingViewModelTest {
    private val request = TrackingRequest("123", "Market", "38", "Geary", "bus1")

    @Test fun startupEmitsPermissionAndLaunchOnceAndAllowsSameTargetAgain() = runTest {
        val vm = TrackingViewModel()
        val effects = mutableListOf<TrackingEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.effects.collect { effects.add(it) } }
        vm.onAction(TrackingAction.Start(request))
        vm.onAction(TrackingAction.Start(request.copy(stopId = "other")))
        assertEquals(listOf(TrackingEffect.RequestNotificationPermission(request)), effects)
        assertTrue(vm.uiState.value.isStarting)
        vm.onAction(TrackingAction.PermissionResolved(true))
        assertEquals(TrackingEffect.LaunchTrackingService(request), effects.last())
        vm.onAction(TrackingAction.LaunchResolved(true))
        assertEquals(TrackingUiState(), vm.uiState.value)
        vm.onAction(TrackingAction.Start(request))
        assertEquals(3, effects.size)
    }

    @Test fun noCollectorDoesNotLeaveStartupBusyOrReplayEffect() = runTest {
        val vm = TrackingViewModel()
        vm.onAction(TrackingAction.Start(request))
        assertFalse(vm.uiState.value.isStarting)
        assertEquals(TrackingFailure.STARTUP_FAILED, vm.uiState.value.error?.reason)
        val effects = mutableListOf<TrackingEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.effects.collect { effects.add(it) } }
        assertTrue(effects.isEmpty())
    }

    @Test fun deniedPermissionRetainsRationaleAndCanBeDismissed() = runTest {
        val vm = TrackingViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.effects.collect {} }
        vm.onAction(TrackingAction.Start(request))
        vm.onAction(TrackingAction.PermissionResolved(false, true))
        assertEquals(TrackingUiState(error = TrackingErrorUiState(TrackingFailure.PERMISSION_DENIED, true)), vm.uiState.value)
        vm.onAction(TrackingAction.DismissFailure)
        assertEquals(TrackingUiState(), vm.uiState.value)
    }

    @Test fun collectorRestartDoesNotReplayAndInterruptedStartupCanRetry() = runTest {
        val vm = TrackingViewModel()
        val effects = mutableListOf<TrackingEffect>()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.effects.collect { effects.add(it) } }
        vm.onAction(TrackingAction.Start(request))
        collector.cancel()
        vm.onAction(TrackingAction.StartupInterrupted)
        assertFalse(vm.uiState.value.isStarting)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.effects.collect { effects.add(it) } }
        assertEquals(1, effects.size)
        vm.onAction(TrackingAction.Start(request))
        assertEquals(2, effects.size)
        assertEquals(TrackingUiState(), TrackingViewModel().uiState.value)
    }

    @Test fun permissionCompletionWithoutCollectorFailsInsteadOfLeavingBusy() = runTest {
        val vm = TrackingViewModel()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.effects.collect {} }
        vm.onAction(TrackingAction.Start(request))
        collector.cancel()
        vm.onAction(TrackingAction.PermissionResolved(true))
        assertFalse(vm.uiState.value.isStarting)
        assertEquals(TrackingFailure.STARTUP_FAILED, vm.uiState.value.error?.reason)
    }

    @Test fun launchFailureAllowsRetryAndInactivePhaseResultsAreIgnored() = runTest {
        val vm = TrackingViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.effects.collect {} }
        vm.onAction(TrackingAction.Start(request))
        vm.onAction(TrackingAction.LaunchResolved(true))
        assertTrue(vm.uiState.value.isStarting)
        vm.onAction(TrackingAction.StartupFailed)
        assertEquals(TrackingFailure.STARTUP_FAILED, vm.uiState.value.error?.reason)
        vm.onAction(TrackingAction.Start(request))
        vm.onAction(TrackingAction.PermissionResolved(true))
        vm.onAction(TrackingAction.LaunchResolved(false))
        assertEquals(TrackingFailure.STARTUP_FAILED, vm.uiState.value.error?.reason)
        assertFalse(vm.uiState.value.isStarting)
    }
}
