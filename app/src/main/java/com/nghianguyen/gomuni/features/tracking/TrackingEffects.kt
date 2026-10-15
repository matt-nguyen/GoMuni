package com.nghianguyen.gomuni.features.tracking

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
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

// Platform launchers can throw different RuntimeException subclasses across Android versions.
@Suppress("TooGenericExceptionCaught")
private fun requestNotificationPermission(
    permission: PermissionState,
    onAction: (TrackingAction) -> Unit,
) {
    try {
        permission.launchPermissionRequest()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: RuntimeException) {
        Log.e("TrackingEffects", "Unable to request notification permission", error)
        onAction(TrackingAction.StartupFailed)
    }
}

// Report a failed launch without losing the pending UI workflow; cancellation still propagates.
@Suppress("TooGenericExceptionCaught")
private fun startTrackingService(
    context: Context,
    request: TrackingRequest,
    onAction: (TrackingAction) -> Unit,
) {
    val accepted =
        try {
            launchTracking(context, request)
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: RuntimeException) {
            Log.e("TrackingEffects", "Unable to launch tracking service", error)
            false
        }
    onAction(TrackingAction.LaunchResolved(accepted))
}
