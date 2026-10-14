package com.nghianguyen.gomuni.features.transit.navigation

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.SceneStrategyScope
import com.nghianguyen.gomuni.app.navigation.StopArrivalsKey
import com.nghianguyen.gomuni.app.navigation.TransitLinesKey
import com.nghianguyen.gomuni.app.navigation.TransitStopsKey
import org.junit.Assert.*
import org.junit.Test

class ArrivalsSceneTest {
    private fun entry(key: NavKey) = NavEntry<NavKey>(key,
        metadata = mapOf(TRANSIT_DESTINATION to key)) {}

    @Test fun arrivalsSceneKeepsBothEntriesAndBackTargetsStops() {
        val routes = entry(TransitLinesKey)
        val stops = entry(TransitStopsKey("38"))
        val arrivals = entry(StopArrivalsKey("38", "123"))
        val scene = with(ArrivalsSceneStrategy()) {
            SceneStrategyScope<NavKey>().calculateScene(listOf(routes, stops, arrivals))
        }
        assertNotNull(scene)
        assertEquals(listOf(stops, arrivals), scene!!.entries)
        assertEquals(listOf(routes, stops), scene.previousEntries)
    }

    @Test fun mismatchedParentCannotRenderAnArrivalsScene() {
        val scene = with(ArrivalsSceneStrategy()) {
            SceneStrategyScope<NavKey>().calculateScene(listOf(
                entry(TransitStopsKey("1")), entry(StopArrivalsKey("38", "123"))))
        }
        assertNull(scene)
    }
}
