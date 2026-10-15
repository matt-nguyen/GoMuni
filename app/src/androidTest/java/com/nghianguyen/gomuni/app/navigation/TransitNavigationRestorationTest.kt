package com.nghianguyen.gomuni.app.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation3.runtime.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Verifies navigation policy and keys survive Navigation 3 saveable-state restoration. */
class TransitNavigationRestorationTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun externalRoute_whenStateRestores_preservesHistoryWithoutSheetEntries() {
        val restoration = StateRestorationTester(composeRule)
        lateinit var stack: NavBackStack<NavKey>
        restoration.setContent {
            stack = rememberNavBackStack(TransitLinesKey)
            Text(stack.last().toString())
        }
        composeRule.runOnIdle { navigateToStopRoute(stack, StopArrivalsTarget("38", "123")) }
        restoration.emulateSavedInstanceStateRestore()
        composeRule.runOnIdle {
            assertEquals(listOf(TransitLinesKey, TransitStopsKey("38")), stack)
            navigateToStopRoute(stack, StopArrivalsTarget("38", "123"))
            assertEquals(2, stack.size)
        }
    }
}
