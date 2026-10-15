package com.nghianguyen.gomuni.features.transit.arrivals

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
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
import com.nghianguyen.gomuni.app.navigation.*
import com.nghianguyen.gomuni.ui.theme.GoMuniTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Verifies sheet scope and dismissal with Navigation 3 and the arrivals sheet. */
class ArrivalsSheetIntegrationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun arrivalsSheet_whenDismissed_clearsIndependentScopeAndRetainsStopsRoute() {
        lateinit var stack: NavBackStack<NavKey>
        val models = mutableMapOf<NavKey, EntryModel>()
        val opened = mutableStateOf<String?>(null)
        lateinit var sheetModel: EntryModel
        lateinit var sheetLifecycle: Lifecycle
        val stops = TransitStopsKey("38")
        composeRule.setContent {
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
        composeRule.runOnIdle { navigate(stack, TransitNavigation.OpenRoute("SF", "38")) }
        composeRule.onNodeWithText("Stops").assertExists()
        lateinit var parentModel: EntryModel
        composeRule.runOnIdle {
            parentModel = models.getValue(stops)
            opened.value = "123"
        }
        composeRule.onNodeWithText("Arrivals 123").assertExists()
        lateinit var first: EntryModel
        lateinit var firstLifecycle: Lifecycle
        composeRule.runOnIdle {
            first = sheetModel
            firstLifecycle = sheetLifecycle
            assertNotSame(parentModel, first)
        }
        Espresso.pressBack()
        composeRule.waitUntil { first.cleared }
        composeRule.runOnIdle {
            assertEquals(Lifecycle.State.DESTROYED, firstLifecycle.currentState)
            assertEquals(stops, stack.last())
            assertEquals(2, stack.size)
            assertFalse(parentModel.cleared)
            opened.value = "123"
        }
        composeRule.onNodeWithText("Arrivals 123").assertExists()
        lateinit var reopened: EntryModel
        composeRule.runOnIdle {
            reopened = sheetModel
            assertNotSame(first, reopened)
            opened.value = "456"
        }
        composeRule.onNodeWithText("Arrivals 456").assertExists()
        composeRule.runOnIdle {
            assertTrue(reopened.cleared)
            assertNotSame(reopened, sheetModel)
            assertFalse(parentModel.cleared)
        }
        composeRule.onNodeWithText("Close test sheet").performClick()
        composeRule.waitUntil { sheetModel.cleared }
        composeRule.runOnIdle {
            assertEquals(stops, stack.last())
            navigate(stack, TransitNavigation.Back)
        }
        composeRule.waitUntil { parentModel.cleared }
    }

    private class EntryModel : ViewModel() {
        var cleared = false
        override fun onCleared() { cleared = true }
    }
}
