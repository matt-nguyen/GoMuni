package com.nghianguyen.gomuni.features.transit.stops

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nghianguyen.gomuni.domain.model.TransitStop
import com.nghianguyen.gomuni.features.transit.model.LoadStatus
import com.nghianguyen.gomuni.features.transit.model.RouteKey
import com.nghianguyen.gomuni.ui.theme.GoMuniTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RouteStopsScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun stopsScreen_whenSelectingStopAndGoingBack_reportsTypedActions() {
        val actions = mutableListOf<RouteStopsAction>()
        val stop = TransitStop("123", "Market", 37.78, -122.41, null, "38")
        composeRule.setContent {
            GoMuniTheme {
                RouteStopsScreen(
                    RouteStopsUiState(
                        RouteKey("SF", "38"),
                        stops = listOf(stop),
                        status = LoadStatus.LOADED,
                    ),
                    actions::add,
                    false,
                    rememberLazyListState(),
                    null,
                    map = { Box(it) { Text("Map placeholder") } },
                )
            }
        }
        composeRule.onNodeWithText("Market").performClick()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.runOnIdle {
            assertEquals(listOf(RouteStopsAction.SelectStop("123"), RouteStopsAction.Back), actions)
        }
    }
}
