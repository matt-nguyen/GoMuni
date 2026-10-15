package com.nghianguyen.gomuni.features.tracking

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.nghianguyen.gomuni.platform.permissions.PermissionState
import com.nghianguyen.gomuni.platform.permissions.PermissionStatus
import com.nghianguyen.gomuni.platform.permissions.rememberPostNotificationPermissionState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

/**
 * Executes tracking effects while resumed and reports results through [onAction]. Stopping
 * collection interrupts startup unless a permission dialog is in progress.
 *
 * @param effects One-time permission and service-launch commands to execute.
 * @param onAction Receives permission results, launch outcomes, and startup interruptions.
 */
@Composable
internal fun TrackingEffects(
    effects: Flow<TrackingEffect>,
    onAction: (TrackingAction) -> Unit,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentAction by rememberUpdatedState(onAction)
    val permission = rememberPostNotificationPermissionState { status ->
        currentAction(
            TrackingAction.PermissionResolved(
                granted = status == PermissionStatus.Granted,
                shouldShowRationale =
                    (status as? PermissionStatus.Denied)?.shouldShowRationale ?: false,
            ),
        )
    }
    LaunchedEffect(effects, lifecycle, context, permission) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            try {
                effects.collect { tracking ->
                    when (tracking) {
                        is TrackingEffect.RequestNotificationPermission ->
                            requestNotificationPermission(permission, currentAction)
                        is TrackingEffect.LaunchTrackingService ->
                            startTrackingService(context, tracking.request, currentAction)
                    }
                }
            } finally {
                // An actual permission dialog may finish after pause or rotation.
                // Other unhandled transient work is abandoned when collection stops.
                if (!permission.isRequestInProgress)
                    currentAction(TrackingAction.StartupInterrupted)
            }
        }
    }
}

private fun requestNotificationPermission(
    permission: PermissionState,
    onAction: (TrackingAction) -> Unit,
) {
    try {
        permission.launchPermissionRequest()
    } catch (error: RuntimeException) {
        if (error is CancellationException) throw error
        onAction(TrackingAction.StartupFailed)
    }
}

private fun startTrackingService(
    context: Context,
    request: TrackingRequest,
    onAction: (TrackingAction) -> Unit,
) {
    val accepted =
        try {
            launchTracking(context, request)
            true
        } catch (error: RuntimeException) {
            if (error is CancellationException) throw error
            false
        }
    onAction(TrackingAction.LaunchResolved(accepted))
}
