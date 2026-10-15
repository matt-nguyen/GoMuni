package com.nghianguyen.gomuni.tracking

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

internal fun Context.createTrackingNotificationChannel() {
    val channel =
        NotificationChannel(
                BusTrackingService.CHANNEL_ID,
                "Bus Tracking",
                NotificationManager.IMPORTANCE_LOW,
            )
            .apply { description = "Shows real-time bus arrival information" }
    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.createNotificationChannel(channel)
}
