package com.nghianguyen.gomuni.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TransitTransitionsTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun panelNavigation_whenMovingForwardAndBack_slidesInExpectedDirection() {
        lateinit var stack: NavBackStack<NavKey>
        val positions = mutableMapOf<String, Offset>()
        composeRule.setContent {
            stack = rememberNavBackStack(TransitLinesKey)
            Box(Modifier.width(240.dp).height(320.dp).clipToBounds()) {
                NavDisplay(
                    backStack = stack,
                    transitionSpec = { transitSlideTransition(forward = true) },
                    popTransitionSpec = { transitSlideTransition(forward = false) },
                    predictivePopTransitionSpec = { transitSlideTransition(forward = false) },
                    entryProvider =
                        entryProvider<NavKey> {
                            entry<TransitLinesKey> {
                                Box(
                                    Modifier.fillMaxSize().testTag("lines").onGloballyPositioned {
                                        positions["lines"] = it.positionInRoot()
                                    },
                                )
                            }
                            entry<TransitStopsKey> {
                                Box(
                                    Modifier.fillMaxSize().testTag("stops").onGloballyPositioned {
                                        positions["stops"] = it.positionInRoot()
                                    },
                                )
                            }
                        },
                )
            }
        }
        composeRule.waitForIdle()
        val origin = composeRule.runOnIdle { positions.getValue("lines") }
        composeRule.mainClock.autoAdvance = false
        composeRule.runOnIdle { navigate(stack, TransitNavigation.OpenRoute("SF", "38")) }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeBy(100)
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            assertTrue(positions.getValue("lines").x < origin.x)
            assertTrue(positions.getValue("stops").x > origin.x)
            assertEquals(origin.y, positions.getValue("stops").y, 0.5f)
        }
        composeRule.mainClock.advanceTimeBy(400)
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            assertEquals(origin, positions.getValue("stops"))
            navigate(stack, TransitNavigation.Back)
        }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeBy(100)
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            assertTrue(positions.getValue("lines").x < origin.x)
            assertTrue(positions.getValue("stops").x > origin.x)
            assertEquals(origin.y, positions.getValue("lines").y, 0.5f)
        }
        composeRule.mainClock.advanceTimeBy(400)
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertEquals(origin, positions.getValue("lines")) }
        composeRule.mainClock.autoAdvance = true
    }
}
