package com.nghianguyen.gomuni.app.navigation

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransitNavigationTest {
    @Test
    fun `only the current route can navigate`() {
        val stops = TransitStopsKey("38", "SF")
        assertTrue(canNavigate(stops, stops))
        assertTrue(canNavigate(TransitLinesKey, TransitLinesKey))
        assertFalse(canNavigate(TransitLinesKey, stops))
        assertFalse(canNavigate(TransitStopsKey("38", "other"), stops))
        assertFalse(canNavigate(stops, null))
    }

    @Test
    fun `route back sequence uses history without duplicates`() {
        val stack = mutableListOf<NavKey>(TransitLinesKey)
        navigate(stack, TransitNavigation.OpenRoute("SF", "38"))
        navigate(stack, TransitNavigation.OpenRoute("SF", "38"))
        assertEquals(listOf(TransitLinesKey, TransitStopsKey("38")), stack)
        navigate(stack, TransitNavigation.Back)
        navigate(stack, TransitNavigation.Back)
        assertEquals(listOf(TransitLinesKey), stack)
    }

    @Test
    fun `external stop entry reconciles routes without adding a sheet destination`() {
        val stack = mutableListOf<NavKey>(TransitLinesKey)
        assertTrue(navigateToStopRoute(stack, StopArrivalsTarget("38", "123")))
        assertTrue(navigateToStopRoute(stack, StopArrivalsTarget("38", "456")))
        assertEquals(listOf(TransitLinesKey, TransitStopsKey("38")), stack)
        navigate(stack, TransitNavigation.OpenRoute("SF", "1"))
        assertTrue(navigateToStopRoute(stack, StopArrivalsTarget("38", "123")))
        assertEquals(listOf(TransitLinesKey, TransitStopsKey("38")), stack)
        assertTrue(navigateToStopRoute(stack, StopArrivalsTarget("1", "789", "other")))
        assertEquals(listOf(TransitLinesKey, TransitStopsKey("1", "other")), stack)
    }

    @Test
    fun `invalid external targets do not change history`() {
        val stack = mutableListOf<NavKey>(TransitLinesKey)
        assertFalse(navigateToStopRoute(stack, StopArrivalsTarget("", "123")))
        assertFalse(navigateToStopRoute(stack, StopArrivalsTarget("38", "")))
        assertFalse(navigateToStopRoute(stack, StopArrivalsTarget("38", "123", "")))
        assertEquals(listOf(TransitLinesKey), stack)
    }
}
