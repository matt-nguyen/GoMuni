package com.nghianguyen.gomuni.features.transit

import com.nghianguyen.gomuni.features.transit.navigation.TRANSIT_DESTINATION

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation3.runtime.*
import androidx.navigation3.ui.NavDisplay
import com.nghianguyen.gomuni.domain.model.TransitStop
import com.nghianguyen.gomuni.app.navigation.StopArrivalsKey
import com.nghianguyen.gomuni.app.navigation.TransitLinesKey
import com.nghianguyen.gomuni.app.navigation.TransitNavigation
import com.nghianguyen.gomuni.app.navigation.TransitStopsKey
import com.nghianguyen.gomuni.app.navigation.navigate
import com.nghianguyen.gomuni.ui.theme.GoMuniTheme
import com.nghianguyen.gomuni.features.transit.model.LoadStatus
import com.nghianguyen.gomuni.features.transit.model.RouteKey
import com.nghianguyen.gomuni.features.transit.navigation.ArrivalsSceneStrategy
import com.nghianguyen.gomuni.features.transit.navigation.LocalArrivalsContent
import com.nghianguyen.gomuni.features.transit.navigation.LocalStopsContext
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

    @Test fun entryViewModelsRemainIndependentAcrossSceneAndAreClearedOnPop() {
        lateinit var stack: NavBackStack<NavKey>
        val models = mutableMapOf<NavKey, EntryModel>()
        val stops = TransitStopsKey("38")
        val arrivals = StopArrivalsKey("38", "123")
        compose.setContent {
            stack = rememberNavBackStack(TransitLinesKey)
            NavDisplay(backStack = stack,
                sceneStrategies = listOf(ArrivalsSceneStrategy()),
                entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator()),
                entryProvider = entryProvider<NavKey> {
                    entry<TransitLinesKey> {
                        models[it] = viewModel(factory = viewModelFactory { initializer { EntryModel() } })
                        Text("Routes")
                    }
                    entry<TransitStopsKey>(metadata = { mapOf(TRANSIT_DESTINATION to it) }) {
                        models[it] = viewModel(factory = viewModelFactory { initializer { EntryModel() } })
                        CompositionLocalProvider(LocalStopsContext provides RouteStopsUiState(RouteKey("SF", it.lineId))) {
                            Column {
                                Text("Stops")
                                LocalArrivalsContent.current?.invoke()
                            }
                        }
                    }
                    entry<StopArrivalsKey>(metadata = { mapOf(TRANSIT_DESTINATION to it) }) {
                        models[it] = viewModel(factory = viewModelFactory { initializer { EntryModel() } })
                        Text("Arrivals for ${LocalStopsContext.current?.route?.lineId}")
                    }
                })
        }
        compose.runOnIdle { navigate(stack, TransitNavigation.OpenRoute("SF", "38")) }
        compose.onNodeWithText("Stops").assertExists()
        lateinit var parentModel: EntryModel
        compose.runOnIdle {
            parentModel = models.getValue(stops)
            navigate(stack, TransitNavigation.OpenStop("SF", "38", "123"))
        }
        compose.onNodeWithText("Arrivals for 38").assertExists()
        compose.runOnIdle {
            assertSame(parentModel, models.getValue(stops))
            assertNotSame(parentModel, models.getValue(arrivals))
            navigate(stack, TransitNavigation.Back)
        }
        compose.waitUntil { models.getValue(arrivals).cleared }
        compose.runOnIdle {
            assertFalse(parentModel.cleared)
            navigate(stack, TransitNavigation.Back)
        }
        compose.waitUntil { parentModel.cleared }
    }

    @Test fun navigationHistoryRestoresWithoutAddingDuplicateEntries() {
        val restoration = StateRestorationTester(compose)
        lateinit var stack: NavBackStack<NavKey>
        restoration.setContent {
            stack = rememberNavBackStack(TransitLinesKey)
            Text(stack.last().toString())
        }
        compose.runOnIdle { navigate(stack, TransitNavigation.OpenStop("SF", "38", "123")) }
        restoration.emulateSavedInstanceStateRestore()
        compose.runOnIdle {
            assertEquals(listOf(TransitLinesKey, TransitStopsKey("38"), StopArrivalsKey("38", "123")), stack)
            navigate(stack, TransitNavigation.OpenStop("SF", "38", "123"))
            assertEquals(3, stack.size)
        }
    }

    private class EntryModel : ViewModel() {
        var cleared = false
        override fun onCleared() { cleared = true }
    }
}
