package com.nghianguyen.gomuni.features.tracking

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CancellationException

@Composable
internal fun TrackingEffects(viewModel: TrackingCoordinatorViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    fun hasNotifications() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    // Only the launcher correlation token is local; the request lives in SavedStateHandle.
    var permissionRequestId by rememberSaveable { mutableStateOf<String?>(null) }
    val notificationsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val requestId = permissionRequestId
        permissionRequestId = null
        requestId?.let {
            viewModel.onAction(TrackingAction.PermissionResolved(it, granted))
        }
    }
    LaunchedEffect(viewModel, lifecycleOwner, context) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.uiState.collect { tracking ->
                when (tracking) {
                    is TrackingUiState.AwaitingPermission -> {
                        if (hasNotifications()) {
                            viewModel.onAction(TrackingAction.PermissionResolved(tracking.request.requestId, true))
                        } else {
                            // Record the dialog phase before launching; recreation waits for
                            // the Activity Result registry instead of opening another dialog.
                            viewModel.onAction(TrackingAction.PermissionRequested(tracking.request.requestId))
                            permissionRequestId = tracking.request.requestId
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            } catch (error: RuntimeException) {
                                if (error is CancellationException) throw error
                                permissionRequestId = null
                                viewModel.onAction(TrackingAction.PermissionRequestFailed(tracking.request.requestId))
                            }
                        }
                    }
                    is TrackingUiState.PermissionInFlight -> {
                        // Also recover approval made while the route was inactive.
                        if (hasNotifications()) viewModel.onAction(
                            TrackingAction.PermissionResolved(tracking.request.requestId, true))
                    }
                    is TrackingUiState.ReadyToLaunch -> {
                        val accepted = try {
                            launchTracking(context, tracking.request)
                            true
                        } catch (error: RuntimeException) {
                            if (error is CancellationException) throw error
                            false
                        }
                        viewModel.onAction(TrackingAction.LaunchResolved(tracking.request.requestId, accepted))
                    }
                    TrackingUiState.Idle, is TrackingUiState.Failed -> Unit
                }
            }
        }
    }
}
