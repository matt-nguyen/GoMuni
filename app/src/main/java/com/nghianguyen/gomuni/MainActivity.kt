package com.nghianguyen.gomuni

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nghianguyen.gomuni.app.navigation.StopArrivalsTarget
import com.nghianguyen.gomuni.app.navigation.TransitNavigationHost
import com.nghianguyen.gomuni.tracking.BusTrackingService
import com.nghianguyen.gomuni.ui.theme.GoMuniTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var pendingStopNavigation by mutableStateOf<StopArrivalsTarget?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Restored stops state already contains a consumed cold-launch sheet target.
        pendingStopNavigation =
            savedInstanceState?.getString("pending_stop")?.let { stopId ->
                savedInstanceState.getString("pending_line")?.let { StopArrivalsTarget(it, stopId) }
            } ?: if (savedInstanceState == null) trackingTarget(intent) else null
        setContent {
            GoMuniTheme {
                TransitNavigationHost(
                    pendingStopNavigation = pendingStopNavigation,
                    onStopNavigationHandled = { pendingStopNavigation = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingStopNavigation = trackingTarget(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        pendingStopNavigation?.let {
            outState.putString("pending_stop", it.stopId)
            outState.putString("pending_line", it.lineId)
        }
        super.onSaveInstanceState(outState)
    }

    private fun trackingTarget(intent: Intent?): StopArrivalsTarget? {
        val stopId =
            intent?.getStringExtra(BusTrackingService.EXTRA_STOP_ID)?.takeIf { it.isNotBlank() }
        val lineId =
            intent?.getStringExtra(BusTrackingService.EXTRA_LINE_REF)?.takeIf { it.isNotBlank() }
        return if (stopId != null && lineId != null) StopArrivalsTarget(lineId, stopId) else null
    }
}
