package com.nghianguyen.gomuni.features.tracking

import android.content.Context
import android.content.Intent
import com.nghianguyen.gomuni.tracking.BusTrackingService

/** Acceptance by Android is not confirmation that arrival data has loaded. */
internal fun launchTracking(context: Context, request: TrackingRequest) {
    context.startForegroundService(
        Intent(context, BusTrackingService::class.java).apply {
            action = BusTrackingService.ACTION_START
            putExtra(BusTrackingService.EXTRA_STOP_ID, request.stopId)
            putExtra(BusTrackingService.EXTRA_STOP_NAME, request.stopName)
            putExtra(BusTrackingService.EXTRA_LINE_REF, request.lineRef)
            putExtra(BusTrackingService.EXTRA_LINE_NAME, request.lineName)
            putExtra(BusTrackingService.EXTRA_VEHICLE_REF, request.vehicleRef)
        },
    )
}
