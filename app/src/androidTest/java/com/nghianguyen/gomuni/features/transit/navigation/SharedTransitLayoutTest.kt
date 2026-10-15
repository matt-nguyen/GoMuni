package com.nghianguyen.gomuni.features.transit.navigation

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation3.runtime.*
import androidx.navigation3.ui.NavDisplay
import com.mapbox.maps.MapView
import com.nghianguyen.gomuni.app.navigation.*
import com.nghianguyen.gomuni.features.transit.arrivals.StopArrivalsSheet
import com.nghianguyen.gomuni.features.transit.model.RouteKey
import com.nghianguyen.gomuni.features.transit.routes.RoutesUiState
import com.nghianguyen.gomuni.features.transit.routes.TransitLinesTopBar
import com.nghianguyen.gomuni.features.transit.stops.RouteStopsTopBar
import com.nghianguyen.gomuni.features.transit.stops.RouteStopsUiState
import com.nghianguyen.gomuni.ui.theme.GoMuniTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Verifies SharedTransitLayout retains its real map across navigation, sheets, and resizing. */
class SharedTransitLayoutTest {
    @get:Rule val composeRule = createComposeRule()

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun sharedMap_whenNavigatingAndResizing_retainsViewAndExpectedBounds() {
        val wide = mutableStateOf(false)
        val sheetOpen = mutableStateOf(false)
        lateinit var root: View
        lateinit var stack: NavBackStack<NavKey>
        composeRule.setContent {
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
                        entryProvider =
                            entryProvider<NavKey> {
                                entry<TransitLinesKey> {
                                    rememberTransitMapBinding(
                                        sharedMap,
                                        it,
                                        TransitMapPresentation(routeColor = Color.Red),
                                        topBar = {
                                            TransitLinesTopBar(RoutesUiState(), {}, false, {})
                                        },
                                    )
                                    Text("Lines")
                                }
                                entry<TransitStopsKey> { key ->
                                    rememberTransitMapBinding(
                                        sharedMap,
                                        key,
                                        TransitMapPresentation(
                                            focusArrivals = sheetOpen.value,
                                            routeColor = Color.Blue,
                                        ),
                                        topBar = {
                                            RouteStopsTopBar(
                                                RouteStopsUiState(
                                                    RouteKey(key.operatorId, key.lineId),
                                                ),
                                                {},
                                            )
                                        },
                                    )
                                    Column {
                                        Text("Stops")
                                        if (sheetOpen.value) {
                                            StopArrivalsSheet(
                                                rememberModalBottomSheetState(),
                                                { sheetOpen.value = false },
                                            ) {
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
        composeRule.runOnIdle {
            original = root.mapViews().single()
            originalBounds = original.visibleBounds()
            assertTrue(originalBounds.width() > 1)
            assertTrue(originalBounds.height() > 1)
        }
        composeRule.mainClock.autoAdvance = false
        fun changeAndCheck(change: () -> Unit) {
            composeRule.runOnIdle(change)
            // Check intermediate animation frames, including entry registration/disposal.
            repeat(40) {
                composeRule.mainClock.advanceTimeByFrame()
                composeRule.waitForIdle()
                composeRule.runOnIdle {
                    assertSame(original, root.mapViews().single())
                    assertEquals(originalBounds, original.visibleBounds())
                    assertEquals(1f, original.alpha)
                }
            }
            composeRule.mainClock.advanceTimeBy(1000)
            composeRule.waitForIdle()
            composeRule.runOnIdle { assertEquals(originalBounds, original.visibleBounds()) }
        }
        changeAndCheck { navigate(stack, TransitNavigation.OpenRoute("SF", "38")) }
        changeAndCheck { sheetOpen.value = true }
        composeRule.runOnIdle { assertEquals(2, stack.size) }
        changeAndCheck { sheetOpen.value = false }
        composeRule.runOnIdle { wide.value = true }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            assertSame(original, root.mapViews().single())
            originalBounds = original.visibleBounds()
        }
        changeAndCheck { sheetOpen.value = true }
        changeAndCheck { sheetOpen.value = false }
        composeRule.runOnIdle { wide.value = false }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        composeRule.runOnIdle { originalBounds = original.visibleBounds() }
        changeAndCheck { navigate(stack, TransitNavigation.Back) }
        // Adaptive resize changes the region, while retaining the actual Mapbox view.
        composeRule.runOnIdle { wide.value = true }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            assertSame(original, root.mapViews().single())
            assertNotEquals(originalBounds, original.visibleBounds())
        }
        composeRule.runOnIdle { wide.value = false }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            assertSame(original, root.mapViews().single())
            assertEquals(originalBounds, original.visibleBounds())
        }
        composeRule.mainClock.autoAdvance = true
    }

    private fun View.visibleBounds(): Rect = Rect().also { assertTrue(getGlobalVisibleRect(it)) }

    private fun View.mapViews(): List<MapView> =
        when (this) {
            is MapView -> listOf(this)
            is ViewGroup -> (0 until childCount).flatMap { getChildAt(it).mapViews() }
            else -> emptyList()
        }
}
