package com.nghianguyen.gomuni.features.transit.components

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.ui.theme.GoMuniTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TransitLineListInteractionTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun favoriteControl_whenTapped_doesNotNavigateAndExposesCheckedState() {
        val navigations = mutableListOf<String>()
        val changes = mutableListOf<Pair<String, Boolean>>()
        composeRule.setContent {
            GoMuniTheme {
                TransitLineList(
                    lines = listOf(TransitLine("38", "Geary", "bus")),
                    selectedLineId = null,
                    onLineClick = navigations::add,
                    listState = rememberLazyListState(),
                    filterScrollState = rememberScrollState(),
                    favoritesReady = true,
                    favoriteIds = setOf("38"),
                    onFavoriteChanged = { id, favorite -> changes.add(id to favorite) },
                )
            }
        }
        composeRule
            .onNodeWithContentDescription("Remove line 38 from favorites")
            .assertIsOn()
            .performClick()
        composeRule.runOnIdle {
            assertEquals(listOf("38" to false), changes)
            assertEquals(emptyList<String>(), navigations)
        }
        composeRule.onNodeWithText("Geary").performClick()
        composeRule.runOnIdle { assertEquals(listOf("38"), navigations) }
    }

    @Test
    fun groups_whenMembershipChanges_dividerAppearsOnlyBetweenNonemptyGroups() {
        val favorites = mutableStateOf(setOf("38"))
        composeRule.setContent {
            GoMuniTheme {
                TransitLineList(
                    lines =
                        listOf(
                            TransitLine("1", "California", "bus"),
                            TransitLine("38", "Geary", "bus"),
                        ),
                    selectedLineId = null,
                    onLineClick = {},
                    listState = rememberLazyListState(),
                    filterScrollState = rememberScrollState(),
                    favoriteIds = favorites.value,
                    favoritesReady = true,
                    pendingFavoriteIds = setOf("38"),
                )
            }
        }
        composeRule.onNodeWithTag("favorites-divider").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("Remove line 38 from favorites")
            .assertIsNotEnabled()
        composeRule.runOnIdle { favorites.value = emptySet() }
        composeRule.onNodeWithTag("favorites-divider").assertDoesNotExist()
        composeRule.runOnIdle { favorites.value = setOf("1", "38") }
        composeRule.onNodeWithTag("favorites-divider").assertDoesNotExist()
    }
}
