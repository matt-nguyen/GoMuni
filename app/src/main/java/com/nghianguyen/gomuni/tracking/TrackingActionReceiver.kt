package com.nghianguyen.gomuni.tracking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TrackingActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent =
            Intent(context, BusTrackingService::class.java).apply {
                action = intent.action
            }
        context.startService(serviceIntent)
    }
}
