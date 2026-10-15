package com.nghianguyen.gomuni.tracking

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.nghianguyen.gomuni.MainActivity
import com.nghianguyen.gomuni.R
import com.nghianguyen.gomuni.domain.model.TransitError
import com.nghianguyen.gomuni.domain.repository.TransitRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BusTrackingService : Service() {

    @Inject lateinit var repository: TransitRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var refreshJob: Job? = null
    private val trackingPreferences by lazy {
        getSharedPreferences("bus_tracking_requests", Context.MODE_PRIVATE)
    }

    private var stopId: String = ""
    private var stopName: String = ""
    private var lineRef: String = ""
    private var lineName: String = ""
    private var vehicleRef: String? = null

    companion object {
        const val CHANNEL_ID = "bus_tracking_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "ACTION_START"
        const val ACTION_REFRESH = "ACTION_REFRESH"
        const val ACTION_STOP = "ACTION_STOP"

        private const val SESSION_ACTIVE = "session_active"
        const val EXTRA_STOP_ID = "EXTRA_STOP_ID"
        const val EXTRA_STOP_NAME = "EXTRA_STOP_NAME"
        const val EXTRA_LINE_REF = "EXTRA_LINE_REF"
        const val EXTRA_LINE_NAME = "EXTRA_LINE_NAME"
        const val EXTRA_VEHICLE_REF = "EXTRA_VEHICLE_REF"
    }

    override fun onCreate() {
        super.onCreate()
        createTrackingNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        var restartMode = START_STICKY
        when (intent?.action) {
            ACTION_START -> {
                stopId = intent.getStringExtra(EXTRA_STOP_ID) ?: ""
                stopName = intent.getStringExtra(EXTRA_STOP_NAME) ?: ""
                lineRef = intent.getStringExtra(EXTRA_LINE_REF) ?: ""
                lineName = intent.getStringExtra(EXTRA_LINE_NAME) ?: ""
                vehicleRef = intent.getStringExtra(EXTRA_VEHICLE_REF)
                startForegroundService()
                persistSession()
                refreshArrivals()
            }
            ACTION_REFRESH -> {
                if (stopId.isEmpty()) restoreSession()
                refreshArrivals()
            }
            ACTION_STOP -> {
                trackingPreferences.edit().putBoolean(SESSION_ACTIVE, false).commit()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                restartMode = START_NOT_STICKY
            }
            null -> {
                if (trackingPreferences.getBoolean(SESSION_ACTIVE, false)) {
                    restoreSession()
                    startForegroundService()
                    refreshArrivals()
                } else {
                    stopSelf()
                    restartMode = START_NOT_STICKY
                }
            }
        }
        return restartMode
    }

    private fun persistSession() {
        trackingPreferences
            .edit()
            .putBoolean(SESSION_ACTIVE, true)
            .putString(EXTRA_STOP_ID, stopId)
            .putString(EXTRA_STOP_NAME, stopName)
            .putString(EXTRA_LINE_REF, lineRef)
            .putString(EXTRA_LINE_NAME, lineName)
            .putString(EXTRA_VEHICLE_REF, vehicleRef)
            .commit()
    }

    private fun restoreSession() {
        stopId = trackingPreferences.getString(EXTRA_STOP_ID, "").orEmpty()
        stopName = trackingPreferences.getString(EXTRA_STOP_NAME, "").orEmpty()
        lineRef = trackingPreferences.getString(EXTRA_LINE_REF, "").orEmpty()
        lineName = trackingPreferences.getString(EXTRA_LINE_NAME, "").orEmpty()
        vehicleRef = trackingPreferences.getString(EXTRA_VEHICLE_REF, null)
    }

    private fun startForegroundService() {
        val notification = createNotification("Fetching arrival time...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun refreshArrivals() {
        refreshJob?.cancel()
        refreshJob = serviceScope.launch {
            try {
                val arrivals = repository.getBusArrivals(stopId)
                val targetArrival = arrivals.find {
                    it.lineRef == lineRef && (vehicleRef == null || it.vehicleRef == vehicleRef)
                }

                if (targetArrival != null) {
                    val timeStr =
                        formatTrackingArrivalTime(
                            targetArrival.expectedArrival ?: targetArrival.aimedArrival ?: "",
                        )
                    updateNotification("$timeStr MIN")
                } else {
                    updateNotification("Arrival data unavailable")
                }
            } catch (error: TransitError) {
                Log.e("BusTrackingService", "Unable to refresh tracked arrival", error)
                updateNotification("Error refreshing data")
            }
        }
    }

    private fun updateNotification(content: String) {
        val notification = createNotification(content)
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotification(content: String): Notification {
        val mainIntent =
            Intent(this, MainActivity::class.java).apply {
                putExtra(EXTRA_STOP_ID, stopId)
                putExtra(EXTRA_LINE_REF, lineRef)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        val pendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val refreshIntent =
            Intent(this, TrackingActionReceiver::class.java).apply {
                action = ACTION_REFRESH
            }
        val refreshPendingIntent =
            PendingIntent.getBroadcast(this, 1, refreshIntent, PendingIntent.FLAG_IMMUTABLE)

        val stopIntent =
            Intent(this, TrackingActionReceiver::class.java).apply {
                action = ACTION_STOP
            }
        val stopPendingIntent =
            PendingIntent.getBroadcast(this, 2, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("$lineRef $lineName")
            .setContentText("Arriving at $stopName: $content")
            .setSmallIcon(R.drawable.bus_icon)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.refresh_icon, "Refresh", refreshPendingIntent)
            .addAction(0, "Stop Tracking", stopPendingIntent)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
