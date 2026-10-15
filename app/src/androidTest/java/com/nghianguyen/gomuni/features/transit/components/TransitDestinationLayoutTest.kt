package com.nghianguyen.gomuni.features.transit.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.nghianguyen.gomuni.ui.theme.GoMuniTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TransitDestinationLayoutTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun destinationLayout_whenSwitchingLayouts_keepsHeaderMapAndPanelInteractive() {
        val wide = mutableStateOf(false)
        var headerClicks = 0
        var panelClicks = 0
        var mapClicks = 0
        composeRule.setContent {
            GoMuniTheme {
                Scaffold(
                    topBar = {
                        Box(
                            Modifier.fillMaxWidth().height(48.dp).testTag("header").clickable {
                                headerClicks++
                            },
                        )
                    },
                ) { padding ->
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
            composeRule.runOnIdle { wide.value = layout == 1 }
            val map = composeRule.onNodeWithTag("map").fetchSemanticsNode().boundsInRoot
            val panel = composeRule.onNodeWithTag("panel").fetchSemanticsNode().boundsInRoot
            val header = composeRule.onNodeWithTag("header").fetchSemanticsNode().boundsInRoot
            assertTrue(map.top >= header.bottom)
            assertFalse(map.overlaps(panel))
            composeRule.onNodeWithTag("header").performTouchInput { click() }
            composeRule.onNodeWithTag("map").performTouchInput { click() }
            composeRule.onNodeWithTag("panel").performTouchInput { click() }
        }
        composeRule.runOnIdle {
            assertEquals(2, headerClicks)
            assertEquals(2, panelClicks)
            assertEquals(2, mapClicks)
        }
    }
}
