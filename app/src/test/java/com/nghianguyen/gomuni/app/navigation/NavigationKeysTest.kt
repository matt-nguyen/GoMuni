package com.nghianguyen.gomuni.app.navigation

import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class NavigationKeysTest {
    @Test
    fun `external stop target survives serialization round trip`() {
        val target = StopArrivalsTarget("38", "123")
        val encoded = Json.encodeToString(StopArrivalsTarget.serializer(), target)

        val restored = Json.decodeFromString<StopArrivalsTarget>(encoded)

        assertEquals(target, restored)
    }
}
