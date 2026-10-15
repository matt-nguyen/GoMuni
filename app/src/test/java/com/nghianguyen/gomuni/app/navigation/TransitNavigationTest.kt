package com.nghianguyen.gomuni.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class TransitNavigationTest {
    @Test fun navigationEligibilityIncludesCurrentEntryAndMatchingSceneParentOnly() {
        val parent = TransitStopsKey("38", "SF")
        val child = StopArrivalsKey("38", "123", "SF")
        assertTrue(canNavigate(child, child))
        assertTrue(canNavigate(parent, child))
        assertTrue(canNavigate(TransitLinesKey, TransitLinesKey))
        assertFalse(canNavigate(TransitStopsKey("1", "SF"), child))
        assertFalse(canNavigate(TransitStopsKey("38", "other"), child))
        assertFalse(canNavigate(TransitLinesKey, child))
        assertFalse(canNavigate(parent, null))
        assertFalse(canNavigate(child, parent))
    }

    @Test fun routeStopBackSequenceUsesHistoryAndDoesNotDuplicateEntries() {
        val stack = mutableListOf<NavKey>(TransitLinesKey)
        navigate(stack, TransitNavigation.OpenRoute("SF", "38"))
        navigate(stack, TransitNavigation.OpenRoute("SF", "38"))
        navigate(stack, TransitNavigation.OpenStop("SF", "38", "123"))
        navigate(stack, TransitNavigation.OpenStop("SF", "38", "123"))
        assertEquals(listOf(TransitLinesKey, TransitStopsKey("38"), StopArrivalsKey("38", "123")), stack)
        navigate(stack, TransitNavigation.Back)
        assertEquals(TransitStopsKey("38"), stack.last())
        navigate(stack, TransitNavigation.Back)
        navigate(stack, TransitNavigation.Back)
        assertEquals(listOf(TransitLinesKey), stack)
    }

    @Test fun changingStopReplacesArrivalsAndExternalRouteReconcilesParents() {
        val stack = mutableListOf<NavKey>(TransitLinesKey)
        navigate(stack, TransitNavigation.OpenStop("SF", "38", "123"))
        navigate(stack, TransitNavigation.OpenStop("SF", "38", "456"))
        assertEquals(3, stack.size)
        assertEquals(StopArrivalsKey("38", "456"), stack.last())
        navigate(stack, TransitNavigation.OpenStop("SF", "1", "789"))
        assertEquals(listOf(TransitLinesKey, TransitStopsKey("1"), StopArrivalsKey("1", "789")), stack)
    }

    @Test fun invalidTargetsDoNotChangeHistoryAndKeysRoundTrip() {
        val stack = mutableListOf<NavKey>(TransitLinesKey)
        navigate(stack, TransitNavigation.OpenStop("SF", "", "123"))
        assertEquals(listOf(TransitLinesKey), stack)
        val target = StopArrivalsKey("38", "123")
        assertEquals(target, Json.decodeFromString<StopArrivalsKey>(Json.encodeToString(StopArrivalsKey.serializer(), target)))
    }
}
