package com.nghianguyen.gomuni.tracking

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.nghianguyen.gomuni.MainActivity
import com.nghianguyen.gomuni.R
import com.nghianguyen.gomuni.domain.repository.TransitRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.time.Instant
import javax.inject.Inject
import kotlin.math.max

@AndroidEntryPoint
class BusTrackingService : Service() {

    @Inject
    lateinit var repository: TransitRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var refreshJob: Job? = null
    private var activeRequestId: String? = null
    private val trackingPreferences by lazy { getSharedPreferences("bus_tracking_requests", Context.MODE_PRIVATE) }

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

        const val EXTRA_REQUEST_ID = "EXTRA_REQUEST_ID"
        private const val HANDLED_REQUESTS = "handled_requests"
        private const val SESSION_ACTIVE = "session_active"
        const val EXTRA_STOP_ID = "EXTRA_STOP_ID"
        const val EXTRA_STOP_NAME = "EXTRA_STOP_NAME"
        const val EXTRA_LINE_REF = "EXTRA_LINE_REF"
        const val EXTRA_LINE_NAME = "EXTRA_LINE_NAME"
        const val EXTRA_VEHICLE_REF = "EXTRA_VEHICLE_REF"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val requestId = intent.getStringExtra(EXTRA_REQUEST_ID)
                val handled = trackingPreferences.getStringSet(HANDLED_REQUESTS, emptySet()).orEmpty()
                if (requestId != null && requestId in handled) {
                    // Replays never replace a newer session or revive a stopped request.
                    if (activeRequestId == null) {
                        restoreSession()
                        startForegroundService()
                        if (trackingPreferences.getBoolean(SESSION_ACTIVE, false)) refreshArrivals()
                        else {
                            // Fulfil Android's foreground-start contract before stopping.
                            stopForeground(STOP_FOREGROUND_REMOVE)
                            stopSelf()
                            return START_NOT_STICKY
                        }
                    }
                    return START_STICKY
                }
                stopId = intent.getStringExtra(EXTRA_STOP_ID) ?: ""
                stopName = intent.getStringExtra(EXTRA_STOP_NAME) ?: ""
                lineRef = intent.getStringExtra(EXTRA_LINE_REF) ?: ""
                lineName = intent.getStringExtra(EXTRA_LINE_NAME) ?: ""
                vehicleRef = intent.getStringExtra(EXTRA_VEHICLE_REF)
                activeRequestId = requestId
                startForegroundService()
                persistSession(requestId)
                refreshArrivals()
            }
            ACTION_REFRESH -> {
                if (stopId.isEmpty()) restoreSession()
                refreshArrivals()
            }
            ACTION_STOP -> {
                trackingPreferences.edit().putBoolean(SESSION_ACTIVE, false).commit()
                activeRequestId = null
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            null -> {
                if (trackingPreferences.getBoolean(SESSION_ACTIVE, false)) {
                    restoreSession()
                    startForegroundService()
                    refreshArrivals()
                } else {
                    stopSelf()
                    return START_NOT_STICKY
                }
            }
        }
        return START_STICKY
    }

    private fun persistSession(requestId: String?) {
        val handled = trackingPreferences.getStringSet(HANDLED_REQUESTS, emptySet()).orEmpty().toMutableSet()
        requestId?.let { handled.add(it) }
        // Save the target and replay history atomically before another start is processed.
        trackingPreferences.edit()
            .putStringSet(HANDLED_REQUESTS, handled)
            .putBoolean(SESSION_ACTIVE, true)
            .putString(EXTRA_REQUEST_ID, requestId)
            .putString(EXTRA_STOP_ID, stopId)
            .putString(EXTRA_STOP_NAME, stopName)
            .putString(EXTRA_LINE_REF, lineRef)
            .putString(EXTRA_LINE_NAME, lineName)
            .putString(EXTRA_VEHICLE_REF, vehicleRef)
            .commit()
    }

    private fun restoreSession() {
        activeRequestId = trackingPreferences.getString(EXTRA_REQUEST_ID, null)
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
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
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
                val targetArrival = arrivals.find { it.lineRef == lineRef && (vehicleRef == null || it.vehicleRef == vehicleRef) }
                
                if (targetArrival != null) {
                    val timeStr = formatArrivalTime(targetArrival.expectedArrival ?: targetArrival.aimedArrival ?: "")
                    updateNotification("$timeStr MIN")
                } else {
                    updateNotification("Arrival data unavailable")
                }
            } catch (e: Exception) {
                updateNotification("Error refreshing data")
            }
        }
    }

    private fun updateNotification(content: String) {
        val notification = createNotification(content)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotification(content: String): Notification {
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            putExtra(EXTRA_STOP_ID, stopId)
            putExtra(EXTRA_LINE_REF, lineRef)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val refreshIntent = Intent(this, TrackingActionReceiver::class.java).apply {
            action = ACTION_REFRESH
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(this, 1, refreshIntent, PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, TrackingActionReceiver::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getBroadcast(this, 2, stopIntent, PendingIntent.FLAG_IMMUTABLE)

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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Bus Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time bus arrival information"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun formatArrivalTime(isoTime: String): String {
        if (isoTime.isEmpty()) return "N/A"
        return try {
            val instant = Instant.parse(isoTime)
            val now = Instant.now()
            val duration = java.time.Duration.between(now, instant)
            val minutes = max(0, duration.toMinutes())
            minutes.toString()
        } catch (e: Exception) {
            "?"
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
