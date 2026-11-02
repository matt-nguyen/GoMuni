package com.nghianguyen.gomuni.features.transit.components

import com.nghianguyen.gomuni.domain.model.TransitLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransitLineListTest {
    @Test
    fun `local filter includes numeric and letter routes but excludes owl and rapid`() {
        assertTrue(matches("19", "Local"))
        assertTrue(matches("N", "Local"))
        assertFalse(matches("NOWL", "Local"))
        assertFalse(matches("14R", "Local"))
    }

    @Test
    fun `rapid express and owl filters retain suffix matching`() {
        assertTrue(matches("14r", "Rapid"))
        assertTrue(matches("1AX", "Express"))
        assertTrue(matches("1BX", "Express"))
        assertTrue(matches("NX", "Express"))
        assertTrue(matches("NOWL", "Owl"))
        assertFalse(matches("19", "Rapid"))
        assertFalse(matches("19", "Express"))
        assertFalse(matches("N", "Owl"))
    }

    @Test
    fun `all and unknown filters retain every route`() {
        assertTrue(matches("19", "All"))
        assertTrue(matches("19", "unknown"))
    }

    @Test
    fun `groups preserve sorting filter both sections and remove duplicate identities`() {
        val lines = listOf("1", "14R", "38", "38R", "38").map { TransitLine(it, it, "bus") }
        val groups = groupTransitLines(lines, "All", setOf("38", "14R", "missing"))
        assertEquals(listOf("14R", "38"), groups.favorites.map { it.id })
        assertEquals(listOf("1", "38R"), groups.others.map { it.id })
        val rapid = groupTransitLines(lines, "Rapid", setOf("38", "14R"))
        assertEquals(listOf("14R"), rapid.favorites.map { it.id })
        assertEquals(listOf("38R"), rapid.others.map { it.id })
    }

    private fun matches(id: String, filter: String) =
        matchesRouteFilter(TransitLine(id, id, "bus"), filter)
}
