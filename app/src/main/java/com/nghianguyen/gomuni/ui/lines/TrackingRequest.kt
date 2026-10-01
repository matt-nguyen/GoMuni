package com.nghianguyen.gomuni.ui.lines

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.nghianguyen.gomuni.tracking.BusTrackingService

/** A snapshot of the tracking action, retained while permission is requested. */
data class TrackingRequest(
    val stopId: String,
    val stopName: String,
    val lineRef: String,
    val lineName: String,
    val vehicleRef: String?,
) {
    fun toBundle() = Bundle().apply {
        putString(BusTrackingService.EXTRA_STOP_ID, stopId)
        putString(BusTrackingService.EXTRA_STOP_NAME, stopName)
        putString(BusTrackingService.EXTRA_LINE_REF, lineRef)
        putString(BusTrackingService.EXTRA_LINE_NAME, lineName)
        putString(BusTrackingService.EXTRA_VEHICLE_REF, vehicleRef)
    }
}

internal fun launchTracking(context: Context, request: Bundle) {
    context.startForegroundService(Intent(context, BusTrackingService::class.java).apply {
        action = BusTrackingService.ACTION_START
        putExtras(request)
    })
}
