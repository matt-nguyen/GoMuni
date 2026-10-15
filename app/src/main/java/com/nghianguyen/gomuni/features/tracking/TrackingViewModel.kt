package com.nghianguyen.gomuni.features.tracking

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory startup only. A new process abandons unfinished startup. */
@HiltViewModel
class TrackingViewModel @Inject constructor() : ViewModel() {
    private val state = MutableStateFlow(TrackingUiState())
    val uiState = state.asStateFlow()
    private val _effects = MutableSharedFlow<TrackingEffect>(replay = 0, extraBufferCapacity = 1)
    val effects = _effects.asSharedFlow()
    private var target: TrackingRequest? = null
    private var launching = false

    @MainThread
    fun onAction(action: TrackingAction) {
        when (action) {
            is TrackingAction.Start -> startTracking(action.request)
            is TrackingAction.PermissionResolved -> handlePermissionResult(action)
            TrackingAction.StartupFailed -> handleStartupFailure()
            is TrackingAction.LaunchResolved -> handleLaunchResult(action.succeeded)
            TrackingAction.DismissFailure -> dismissFailure()
            TrackingAction.StartupInterrupted -> interruptStartup()
        }
    }

    private fun startTracking(request: TrackingRequest) {
        if (target != null) return
        target = request
        launching = false
        state.value = TrackingUiState(isStarting = true)
        sendEffect(TrackingEffect.RequestNotificationPermission(request))
    }

    private fun handlePermissionResult(result: TrackingAction.PermissionResolved) {
        val request = target ?: return
        if (launching) return
        if (result.granted) {
            launching = true
            sendEffect(TrackingEffect.LaunchTrackingService(request))
        } else {
            failStartup(TrackingFailure.PERMISSION_DENIED, result.shouldShowRationale)
        }
    }

    private fun handleStartupFailure() {
        if (target == null || launching) return
        failStartup(TrackingFailure.STARTUP_FAILED)
    }

    private fun handleLaunchResult(succeeded: Boolean) {
        if (target == null || !launching) return
        if (succeeded) interruptStartup() else failStartup(TrackingFailure.STARTUP_FAILED)
    }

    private fun sendEffect(effect: TrackingEffect) {
        // tryEmit succeeds without subscribers but discards the effect. Do not leave startup busy.
        if (_effects.subscriptionCount.value == 0 || !_effects.tryEmit(effect)) {
            failStartup(TrackingFailure.STARTUP_FAILED)
        }
    }

    private fun failStartup(reason: TrackingFailure, shouldShowRationale: Boolean = false) {
        target = null
        launching = false
        state.value = TrackingUiState(error = TrackingErrorUiState(reason, shouldShowRationale))
    }

    private fun interruptStartup() {
        target = null
        launching = false
        state.value = state.value.copy(isStarting = false)
    }

    private fun dismissFailure() {
        state.value = state.value.copy(error = null)
    }

}
