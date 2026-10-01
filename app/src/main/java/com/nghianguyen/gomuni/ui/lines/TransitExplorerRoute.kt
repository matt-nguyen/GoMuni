package com.nghianguyen.gomuni.ui.lines

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nghianguyen.gomuni.R

@Composable
fun TransitExplorerRoute(viewModel: TransitLinesViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    fun hasLocation() = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    var locationGranted by remember { mutableStateOf(hasLocation()) }
    var locationRequested by rememberSaveable { mutableStateOf(false) }
    var pendingTracking by rememberSaveable { mutableStateOf<Bundle?>(null) }
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        locationGranted = hasLocation()
    }
    val notificationsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val request = pendingTracking
        pendingTracking = null
        if (granted && request != null) launchTracking(context, request)
        else if (!granted) Toast.makeText(context, R.string.tracking_permission_denied, Toast.LENGTH_LONG).show()
    }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) locationGranted = hasLocation()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(viewModel) { viewModel.ensureRoutesLoaded() }
    LaunchedEffect(Unit) {
        if (!locationGranted && !locationRequested) {
            locationRequested = true
            locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }
    TransitExplorerScreen(
        uiState = uiState,
        locationPermissionGranted = locationGranted,
        onAction = viewModel::onAction,
        onTrackArrival = { arrival, stop ->
            if (pendingTracking == null) {
                val request = TrackingRequest(stop.id, stop.name, arrival.lineRef, arrival.lineName, arrival.vehicleRef).toBundle()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    pendingTracking = request
                    notificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else launchTracking(context, request)
            }
        },
        modifier = modifier,
    )
}
