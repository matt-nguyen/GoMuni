package com.nghianguyen.gomuni.features.transit.navigation

import androidx.compose.ui.graphics.Color
import com.nghianguyen.gomuni.app.navigation.TransitLinesKey
import com.nghianguyen.gomuni.app.navigation.TransitStopsKey
import org.junit.Assert.*
import org.junit.Test

class SharedTransitMapStateTest {
    @Test
    fun `route and operator identity select independent maps`() {
        val state = SharedTransitMapState()
        val sf = TransitMapBinding(TransitMapPresentation(routeColor = Color.Red))
        val other = TransitMapBinding(TransitMapPresentation(routeColor = Color.Blue))
        state.register(TransitStopsKey("38", "SF"), sf)
        state.register(TransitStopsKey("38", "other"), other)
        assertSame(sf, state.binding(TransitStopsKey("38", "SF")))
        assertSame(other, state.binding(TransitStopsKey("38", "other")))
    }

    @Test
    fun `outgoing routes cannot overwrite the active routes map`() {
        val state = SharedTransitMapState()
        val lines = TransitMapBinding(TransitMapPresentation(routeColor = Color.Red))
        val stops = TransitMapBinding(TransitMapPresentation(routeColor = Color.Blue))
        val route = TransitStopsKey("38")
        state.register(route, stops)
        state.register(TransitLinesKey, lines)

        val arrivalsOwner = route
        assertSame(stops, state.binding(arrivalsOwner))
        lines.presentation = TransitMapPresentation(routeColor = Color.Green)
        assertEquals(Color.Blue, state.binding(arrivalsOwner)?.presentation?.routeColor)
        state.unregister(TransitLinesKey, lines)
        assertSame(stops, state.binding(arrivalsOwner))
        assertNull(state.binding(TransitStopsKey("1")))
    }

    @Test
    fun `disposing an old composition does not remove its replacement`() {
        val state = SharedTransitMapState()
        val old = TransitMapBinding(TransitMapPresentation(routeColor = Color.Red))
        val replacement = TransitMapBinding(TransitMapPresentation(routeColor = Color.Blue))
        state.register(TransitLinesKey, old)
        state.register(TransitLinesKey, replacement)
        state.unregister(TransitLinesKey, old)
        assertSame(replacement, state.binding(TransitLinesKey))
        state.unregister(TransitLinesKey, replacement)
        assertNull(state.binding(TransitLinesKey))
    }
}
