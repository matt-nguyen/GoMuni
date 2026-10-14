package com.nghianguyen.gomuni.features.tracking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Host-scoped startup only. The service owns the running tracking session. */
@HiltViewModel
class TrackingCoordinatorViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val state = MutableStateFlow(
        savedStateHandle.get<String>(STATE_KEY)?.let {
            runCatching { Json.decodeFromString<TrackingUiState>(it) }.getOrNull()
        } ?: TrackingUiState.Idle,
    )
    val uiState = state.asStateFlow()

    fun onAction(action: TrackingAction) {
        val current = state.value
        val next = when (action) {
            is TrackingAction.Start -> if (current.isPending ||
                savedStateHandle.get<String>(LAST_REQUEST_KEY) == action.request.requestId) null
                else TrackingUiState.AwaitingPermission(action.request)
            is TrackingAction.PermissionRequested -> (current as? TrackingUiState.AwaitingPermission)
                ?.takeIf { it.request.requestId == action.requestId }
                ?.let { TrackingUiState.PermissionInFlight(it.request) }
            is TrackingAction.PermissionRequestFailed -> (current as? TrackingUiState.PermissionInFlight)
                ?.takeIf { it.request.requestId == action.requestId }
                ?.let { TrackingUiState.Failed(action.requestId, TrackingFailure.STARTUP_FAILED) }
            is TrackingAction.PermissionResolved -> {
                val request = when (current) {
                    is TrackingUiState.AwaitingPermission -> current.request
                    is TrackingUiState.PermissionInFlight -> current.request
                    else -> null
                }
                request?.takeIf { it.requestId == action.requestId }?.let {
                    if (action.granted) TrackingUiState.ReadyToLaunch(it)
                    else TrackingUiState.Failed(action.requestId, TrackingFailure.PERMISSION_DENIED)
                }
            }
            is TrackingAction.LaunchResolved -> (current as? TrackingUiState.ReadyToLaunch)
                ?.takeIf { it.request.requestId == action.requestId }?.let {
                    if (action.succeeded) TrackingUiState.Idle
                    else TrackingUiState.Failed(action.requestId, TrackingFailure.STARTUP_FAILED)
                }
            is TrackingAction.DismissFailure -> (current as? TrackingUiState.Failed)
                ?.takeIf { it.requestId == action.requestId }?.let { TrackingUiState.Idle }
        }
        if (next != null) {
            if (action is TrackingAction.Start) savedStateHandle[LAST_REQUEST_KEY] = action.request.requestId
            savedStateHandle[STATE_KEY] = Json.encodeToString<TrackingUiState>(next)
            state.value = next
        }
    }

    companion object {
        private const val STATE_KEY = "pending_tracking_state"
        private const val LAST_REQUEST_KEY = "last_tracking_request"
    }
}
