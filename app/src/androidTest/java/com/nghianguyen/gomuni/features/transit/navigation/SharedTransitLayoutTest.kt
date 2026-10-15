package com.nghianguyen.gomuni.features.transit.navigation

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import com.nghianguyen.gomuni.features.transit.arrivals.StopArrivalsSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.*
import androidx.navigation3.ui.NavDisplay
import com.mapbox.maps.MapView
import com.nghianguyen.gomuni.app.navigation.*
import com.nghianguyen.gomuni.features.transit.components.TransitDestinationLayout
import com.nghianguyen.gomuni.features.transit.model.RouteKey
import com.nghianguyen.gomuni.features.transit.routes.RoutesUiState
import com.nghianguyen.gomuni.features.transit.routes.TransitLinesTopBar
import com.nghianguyen.gomuni.features.transit.stops.RouteStopsTopBar
import com.nghianguyen.gomuni.features.transit.stops.RouteStopsUiState
import com.nghianguyen.gomuni.ui.theme.GoMuniTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class SharedTransitLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test fun forwardSlidesLeftAndBackSlidesRightWithinThePanel() {
        lateinit var stack: NavBackStack<NavKey>
        val positions = mutableMapOf<String, Offset>()
        compose.setContent {
            stack = rememberNavBackStack(TransitLinesKey)
            Box(Modifier.width(240.dp).height(320.dp).clipToBounds()) {
                NavDisplay(
                    backStack = stack,
                    transitionSpec = { transitSlideTransition(forward = true) },
                    popTransitionSpec = { transitSlideTransition(forward = false) },
                    predictivePopTransitionSpec = { transitSlideTransition(forward = false) },
                    entryProvider = entryProvider<NavKey> {
                        entry<TransitLinesKey> {
                            Box(Modifier.fillMaxSize().testTag("lines").onGloballyPositioned {
                                positions["lines"] = it.positionInRoot()
                            })
                        }
                        entry<TransitStopsKey> {
                            Box(Modifier.fillMaxSize().testTag("stops").onGloballyPositioned {
                                positions["stops"] = it.positionInRoot()
                            })
                        }
                    },
                )
            }
        }
        compose.waitForIdle()
        val origin = positions.getValue("lines")
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { navigate(stack, TransitNavigation.OpenRoute("SF", "38")) }
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeBy(100)
        compose.waitForIdle()
        compose.runOnIdle {
            assertTrue(positions.getValue("lines").x < origin.x)
            assertTrue(positions.getValue("stops").x > origin.x)
            assertEquals(origin.y, positions.getValue("stops").y, 0.5f)
        }
        compose.mainClock.advanceTimeBy(400)
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(origin, positions.getValue("stops"))
            navigate(stack, TransitNavigation.Back)
        }
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeBy(100)
        compose.waitForIdle()
        compose.runOnIdle {
            assertTrue(positions.getValue("lines").x < origin.x)
            assertTrue(positions.getValue("stops").x > origin.x)
            assertEquals(origin.y, positions.getValue("lines").y, 0.5f)
        }
        compose.mainClock.advanceTimeBy(400)
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(origin, positions.getValue("lines")) }
        compose.mainClock.autoAdvance = true
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test fun mapRemainsVisibleAtTheSameBoundsThroughoutNavigationTransitions() {
        val wide = mutableStateOf(false)
        val sheetOpen = mutableStateOf(false)
        lateinit var root: View
        lateinit var stack: NavBackStack<NavKey>
        compose.setContent {
            root = LocalView.current.rootView
            val sharedMap = remember { SharedTransitMapState() }
            stack = rememberNavBackStack(TransitLinesKey)
            GoMuniTheme {
                SharedTransitLayout(
                    state = sharedMap,
                    destination = stack.last(),
                    locationGranted = false,
                    wide = wide.value,
                ) {
                    NavDisplay(
                        backStack = stack,
                        transitionSpec = { transitSlideTransition(forward = true) },
                        popTransitionSpec = { transitSlideTransition(forward = false) },
                        predictivePopTransitionSpec = { transitSlideTransition(forward = false) },
                        entryProvider = entryProvider<NavKey> {
                            entry<TransitLinesKey> {
                                rememberTransitMapBinding(
                                    sharedMap, it, TransitMapPresentation(routeColor = Color.Red),
                                    topBar = { TransitLinesTopBar(RoutesUiState(), {}, false, {}) },
                                )
                                Text("Lines")
                            }
                            entry<TransitStopsKey> { key ->
                                rememberTransitMapBinding(
                                    sharedMap, key,
                                    TransitMapPresentation(
                                        focusArrivals = sheetOpen.value,
                                        routeColor = Color.Blue,
                                    ),
                                    topBar = { RouteStopsTopBar(RouteStopsUiState(RouteKey(key.operatorId, key.lineId)), {}) },
                                )
                                Column {
                                    Text("Stops")
                                    if (sheetOpen.value) {
                                        StopArrivalsSheet(rememberModalBottomSheetState(), { sheetOpen.value = false }) {
                                            Text("Arrivals")
                                        }
                                    }
                                }
                            }

                        },
                    )
                }
            }
        }
        lateinit var original: MapView
        lateinit var originalBounds: Rect
        compose.runOnIdle {
            original = root.mapViews().single()
            originalBounds = original.visibleBounds()
            assertTrue(originalBounds.width() > 1)
            assertTrue(originalBounds.height() > 1)
        }
        compose.mainClock.autoAdvance = false
        fun changeAndCheck(change: () -> Unit) {
            compose.runOnIdle(change)
            // Check intermediate animation frames, including entry registration/disposal.
            repeat(40) {
                compose.mainClock.advanceTimeByFrame()
                compose.waitForIdle()
                compose.runOnIdle {
                    assertSame(original, root.mapViews().single())
                    assertEquals(originalBounds, original.visibleBounds())
                    assertEquals(1f, original.alpha)
                }
            }
            compose.mainClock.advanceTimeBy(1000)
            compose.waitForIdle()
            compose.runOnIdle { assertEquals(originalBounds, original.visibleBounds()) }
        }
        changeAndCheck { navigate(stack, TransitNavigation.OpenRoute("SF", "38")) }
        changeAndCheck { sheetOpen.value = true }
        compose.runOnIdle { assertEquals(2, stack.size) }
        changeAndCheck { sheetOpen.value = false }
        compose.runOnIdle { wide.value = true }
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        compose.runOnIdle {
            assertSame(original, root.mapViews().single())
            originalBounds = original.visibleBounds()
        }
        changeAndCheck { sheetOpen.value = true }
        changeAndCheck { sheetOpen.value = false }
        compose.runOnIdle { wide.value = false }
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        compose.runOnIdle { originalBounds = original.visibleBounds() }
        changeAndCheck { navigate(stack, TransitNavigation.Back) }
        // Adaptive resize changes the region, while retaining the actual Mapbox view.
        compose.runOnIdle { wide.value = true }
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        compose.runOnIdle {
            assertSame(original, root.mapViews().single())
            assertNotEquals(originalBounds, original.visibleBounds())
        }
        compose.runOnIdle { wide.value = false }
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        compose.runOnIdle {
            assertSame(original, root.mapViews().single())
            assertEquals(originalBounds, original.visibleBounds())
        }
        compose.mainClock.autoAdvance = true
    }

    @Test fun headerMapAndPanelRemainInteractiveInBothLayouts() {
        val wide = mutableStateOf(false)
        var headerClicks = 0
        var panelClicks = 0
        var mapClicks = 0
        compose.setContent {
            GoMuniTheme {
                Scaffold(topBar = {
                    Box(Modifier.fillMaxWidth().height(48.dp).testTag("header").clickable { headerClicks++ })
                }) { padding ->
                    TransitDestinationLayout(
                        wide = wide.value,
                        modifier = Modifier.padding(padding),
                        map = { Box(it.testTag("map").clickable { mapClicks++ }) },
                        panel = { Box(it.testTag("panel").clickable { panelClicks++ }) },
                    )
                }
            }
        }
        repeat(2) { layout ->
            compose.runOnIdle { wide.value = layout == 1 }
            val map = compose.onNodeWithTag("map").fetchSemanticsNode().boundsInRoot
            val panel = compose.onNodeWithTag("panel").fetchSemanticsNode().boundsInRoot
            val header = compose.onNodeWithTag("header").fetchSemanticsNode().boundsInRoot
            assertTrue(map.top >= header.bottom)
            assertFalse(map.overlaps(panel))
            compose.onNodeWithTag("header").performTouchInput { click() }
            compose.onNodeWithTag("map").performTouchInput { click() }
            compose.onNodeWithTag("panel").performTouchInput { click() }
        }
        compose.runOnIdle {
            assertEquals(2, headerClicks)
            assertEquals(2, panelClicks)
            assertEquals(2, mapClicks)
        }
    }

    private fun View.visibleBounds(): Rect = Rect().also { assertTrue(getGlobalVisibleRect(it)) }

    private fun View.mapViews(): List<MapView> = when (this) {
        is MapView -> listOf(this)
        is ViewGroup -> (0 until childCount).flatMap { getChildAt(it).mapViews() }
        else -> emptyList()
    }
}
