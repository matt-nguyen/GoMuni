package com.nghianguyen.gomuni.platform.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

sealed interface PermissionStatus {
    data object Granted : PermissionStatus
    data class Denied(val shouldShowRationale: Boolean) : PermissionStatus
}

@Stable
interface PermissionState {
    val status: PermissionStatus
    val isRequestInProgress: Boolean

    /** Call from a UI event or effect. Launch failures propagate to the caller. */
    fun launchPermissionRequest()
}

@Composable
fun rememberPermissionState(
    permission: String,
    onPermissionResult: (PermissionStatus) -> Unit = {},
): PermissionState {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentResult by rememberUpdatedState(onPermissionResult)
    fun readStatus(): PermissionStatus =
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            PermissionStatus.Granted
        } else {
            PermissionStatus.Denied(ActivityCompat.shouldShowRequestPermissionRationale(activity, permission))
        }

    var currentStatus by remember(context, permission) { mutableStateOf(readStatus()) }
    var inProgress by rememberSaveable(permission) { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        inProgress = false
        currentStatus = readStatus()
        // Completion is distinct from a status change: repeated denials must be delivered.
        currentResult(currentStatus)
    }
    DisposableEffect(lifecycle, context, permission) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) currentStatus = readStatus()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return remember(context, permission, launcher) {
        object : PermissionState {
            override val status get() = currentStatus
            override val isRequestInProgress get() = inProgress
            override fun launchPermissionRequest() {
                if (inProgress) return
                currentStatus = readStatus()
                if (currentStatus == PermissionStatus.Granted) {
                    currentResult(currentStatus)
                    return
                }
                inProgress = true
                try {
                    launcher.launch(permission)
                } catch (error: RuntimeException) {
                    inProgress = false
                    throw error
                }
            }
        }
    }
}

/** Before API 33, the runtime permission requirement is satisfied, regardless of app settings. */
@Composable
fun rememberPostNotificationPermissionState(
    onPermissionResult: (PermissionStatus) -> Unit = {},
): PermissionState {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        return rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS, onPermissionResult)
    }
    val currentResult by rememberUpdatedState(onPermissionResult)
    return remember {
        object : PermissionState {
            override val status = PermissionStatus.Granted
            override val isRequestInProgress = false
            override fun launchPermissionRequest() { currentResult(PermissionStatus.Granted) }
        }
    }
}

private tailrec fun Context.findActivity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> error("PermissionState requires an Activity context")
}
