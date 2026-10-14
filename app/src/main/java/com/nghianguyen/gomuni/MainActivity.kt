package com.nghianguyen.gomuni

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nghianguyen.gomuni.tracking.BusTrackingService
import com.nghianguyen.gomuni.app.navigation.StopArrivalsKey
import com.nghianguyen.gomuni.app.navigation.TransitNavigationHost
import com.nghianguyen.gomuni.ui.theme.GoMuniTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var externalTarget by mutableStateOf<StopArrivalsKey?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Saved navigation history already contains a consumed cold-launch target.
        externalTarget = savedInstanceState?.getString("pending_stop")?.let { stopId ->
            savedInstanceState.getString("pending_line")?.let { StopArrivalsKey(it, stopId) }
        } ?: if (savedInstanceState == null) trackingTarget(intent) else null
        setContent {
            GoMuniTheme {
                TransitNavigationHost(externalTarget) { externalTarget = null }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        externalTarget = trackingTarget(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        externalTarget?.let {
            outState.putString("pending_stop", it.stopId)
            outState.putString("pending_line", it.lineId)
        }
        super.onSaveInstanceState(outState)
    }

    private fun trackingTarget(intent: Intent?): StopArrivalsKey? {
        val stopId = intent?.getStringExtra(BusTrackingService.EXTRA_STOP_ID)?.takeIf { it.isNotBlank() } ?: return null
        val lineId = intent.getStringExtra(BusTrackingService.EXTRA_LINE_REF)?.takeIf { it.isNotBlank() } ?: return null
        return StopArrivalsKey(lineId, stopId)
    }
}
