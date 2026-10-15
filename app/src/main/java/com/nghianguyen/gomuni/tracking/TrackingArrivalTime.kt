package com.nghianguyen.gomuni.tracking

import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeParseException

/** Formats the notification's arrival estimate, retaining the unavailable/invalid placeholders. */
internal fun formatTrackingArrivalTime(isoTime: String, now: Instant = Instant.now()): String {
    if (isoTime.isEmpty()) return "N/A"
    return try {
        Duration.between(now, Instant.parse(isoTime)).toMinutes().coerceAtLeast(0).toString()
    } catch (_: DateTimeParseException) {
        "?"
    }
}
