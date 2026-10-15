package com.nghianguyen.gomuni.features.transit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation3.runtime.*
import androidx.navigation3.ui.NavDisplay
import androidx.test.espresso.Espresso
import com.nghianguyen.gomuni.domain.model.TransitStop
import com.nghianguyen.gomuni.app.navigation.*
import com.nghianguyen.gomuni.ui.theme.GoMuniTheme
import com.nghianguyen.gomuni.features.transit.arrivals.ArrivalsSheetScope
import com.nghianguyen.gomuni.features.transit.arrivals.StopArrivalsSheet
import com.nghianguyen.gomuni.features.transit.model.LoadStatus
import com.nghianguyen.gomuni.features.transit.model.RouteKey
import com.nghianguyen.gomuni.features.transit.stops.RouteStopsAction
import com.nghianguyen.gomuni.features.transit.stops.RouteStopsScreen
import com.nghianguyen.gomuni.features.transit.stops.RouteStopsUiState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class PresentationNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun stopsScreenReportsTypedSelectionAndBackActions() {
        val actions = mutableListOf<RouteStopsAction>()
        val stop = TransitStop("123", "Market", 37.78, -122.41, null, "38")
        compose.setContent {
            GoMuniTheme {
                RouteStopsScreen(RouteStopsUiState(RouteKey("SF", "38"), stops = listOf(stop),
                    status = LoadStatus.LOADED), actions::add, false, rememberLazyListState(), null,
                    map = { Box(it) { Text("Map placeholder") } })
            }
        }
        compose.onNodeWithText("Market").performClick()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.runOnIdle {
            assertEquals(listOf(RouteStopsAction.SelectStop("123"), RouteStopsAction.Back), actions)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test fun sheetsHaveIndependentScopesAndDismissWithoutPoppingTheStopsRoute() {
        lateinit var stack: NavBackStack<NavKey>
        val models = mutableMapOf<NavKey, EntryModel>()
        val opened = mutableStateOf<String?>(null)
        lateinit var sheetModel: EntryModel
        lateinit var sheetLifecycle: Lifecycle
        val stops = TransitStopsKey("38")
        compose.setContent {
            GoMuniTheme {
                stack = rememberNavBackStack(TransitLinesKey)
                NavDisplay(
                    backStack = stack,
                    entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator()),
                    entryProvider = entryProvider<NavKey> {
                        entry<TransitLinesKey> {
                            models[it] = viewModel(factory = viewModelFactory { initializer { EntryModel() } })
                            Text("Routes")
                        }
                        entry<TransitStopsKey> {
                            models[it] = viewModel(factory = viewModelFactory { initializer { EntryModel() } })
                            Text("Stops")
                            opened.value?.let { stopId ->
                                ArrivalsSheetScope(stopId) {
                                    sheetModel = viewModel(factory = viewModelFactory { initializer { EntryModel() } })
                                    sheetLifecycle = LocalLifecycleOwner.current.lifecycle
                                    StopArrivalsSheet(rememberModalBottomSheetState(), { opened.value = null }) {
                                        Column(Modifier.height(200.dp)) {
                                            Text("Arrivals $stopId")
                                            TextButton(onClick = { opened.value = null }) { Text("Close test sheet") }
                                        }
                                    }
                                }
                            }
                        }
                    },
                )
            }
        }
        compose.runOnIdle { navigate(stack, TransitNavigation.OpenRoute("SF", "38")) }
        compose.onNodeWithText("Stops").assertExists()
        lateinit var parentModel: EntryModel
        compose.runOnIdle {
            parentModel = models.getValue(stops)
            opened.value = "123"
        }
        compose.onNodeWithText("Arrivals 123").assertExists()
        lateinit var first: EntryModel
        lateinit var firstLifecycle: Lifecycle
        compose.runOnIdle {
            first = sheetModel
            firstLifecycle = sheetLifecycle
            assertNotSame(parentModel, first)
        }
        Espresso.pressBack()
        compose.waitUntil { first.cleared }
        compose.runOnIdle {
            assertEquals(Lifecycle.State.DESTROYED, firstLifecycle.currentState)
            assertEquals(stops, stack.last())
            assertEquals(2, stack.size)
            assertFalse(parentModel.cleared)
            opened.value = "123"
        }
        compose.onNodeWithText("Arrivals 123").assertExists()
        lateinit var reopened: EntryModel
        compose.runOnIdle {
            reopened = sheetModel
            assertNotSame(first, reopened)
            opened.value = "456"
        }
        compose.onNodeWithText("Arrivals 456").assertExists()
        compose.runOnIdle {
            assertTrue(reopened.cleared)
            assertNotSame(reopened, sheetModel)
            assertFalse(parentModel.cleared)
        }
        compose.onNodeWithText("Close test sheet").performClick()
        compose.waitUntil { sheetModel.cleared }
        compose.runOnIdle {
            assertEquals(stops, stack.last())
            navigate(stack, TransitNavigation.Back)
        }
        compose.waitUntil { parentModel.cleared }
    }

    @Test fun externalRouteHistoryRestoresWithoutAddingSheetEntries() {
        val restoration = StateRestorationTester(compose)
        lateinit var stack: NavBackStack<NavKey>
        restoration.setContent {
            stack = rememberNavBackStack(TransitLinesKey)
            Text(stack.last().toString())
        }
        compose.runOnIdle { navigateToStopRoute(stack, StopArrivalsTarget("38", "123")) }
        restoration.emulateSavedInstanceStateRestore()
        compose.runOnIdle {
            assertEquals(listOf(TransitLinesKey, TransitStopsKey("38")), stack)
            navigateToStopRoute(stack, StopArrivalsTarget("38", "123"))
            assertEquals(2, stack.size)
        }
    }

    private class EntryModel : ViewModel() {
        var cleared = false
        override fun onCleared() { cleared = true }
    }
}
