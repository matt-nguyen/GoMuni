package com.nghianguyen.gomuni.features.transit.routes

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.features.transit.model.LoadStatus
import com.nghianguyen.gomuni.ui.theme.GoMuniTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TransitLinesScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun favoritesReadFailure_whenRetryTapped_reportsRetryAndKeepsRoutesVisible() {
        val actions = mutableListOf<TransitLinesAction>()
        showPanel(FavoriteError.READ, actions::add)

        composeRule.onNodeWithText("Unable to load favorites. Try again.").assertIsDisplayed()
        composeRule.onNodeWithText("Geary").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()
        composeRule.runOnIdle { assertEquals(listOf(TransitLinesAction.RetryFavorites), actions) }
    }

    @Test
    fun favoritesWriteFailure_whenShown_keepsStarAvailableForRetry() {
        showPanel(FavoriteError.WRITE) {}

        composeRule
            .onNodeWithText("Unable to save your favorite. Tap the star to try again.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Retry").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Favorite line 38").assertIsEnabled()
    }

    private fun showPanel(error: FavoriteError, onAction: (TransitLinesAction) -> Unit) {
        composeRule.setContent {
            GoMuniTheme {
                TransitLinesPanel(
                    uiState =
                        RoutesUiState(
                            lines = listOf(TransitLine("38", "Geary", "bus")),
                            status = LoadStatus.LOADED,
                            favoriteError = error,
                            favoritesReady = error != FavoriteError.READ,
                        ),
                    onAction = onAction,
                    listState = rememberLazyListState(),
                    filterScrollState = rememberScrollState(),
                )
            }
        }
    }
}
