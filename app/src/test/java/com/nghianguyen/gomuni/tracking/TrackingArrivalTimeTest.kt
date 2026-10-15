package com.nghianguyen.gomuni.tracking

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackingArrivalTimeTest {
    private val now = Instant.parse("2026-10-15T12:00:00Z")

    @Test
    fun `future arrival shows whole remaining minutes`() {
        assertEquals("5", formatTrackingArrivalTime("2026-10-15T12:05:59Z", now))
    }

    @Test
    fun `past arrival is clamped to zero`() {
        assertEquals("0", formatTrackingArrivalTime("2026-10-15T11:59:00Z", now))
    }

    @Test
    fun `missing and malformed arrivals retain distinct placeholders`() {
        assertEquals("N/A", formatTrackingArrivalTime("", now))
        assertEquals("?", formatTrackingArrivalTime("invalid", now))
    }
}
