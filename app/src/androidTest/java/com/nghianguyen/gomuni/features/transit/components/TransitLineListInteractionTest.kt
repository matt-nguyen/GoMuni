package com.nghianguyen.gomuni.features.transit.components

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
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
        val stars = composeRule.onAllNodesWithContentDescription("Remove line 38 from favorites")
        stars.assertCountEquals(2)
        stars[0].assertIsOn().performClick()
        stars[1].assertIsOn().performClick()
        composeRule.runOnIdle {
            assertEquals(listOf("38" to false, "38" to false), changes)
            assertEquals(emptyList<String>(), navigations)
        }
        composeRule.onAllNodesWithText("Geary")[1].performClick()
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
        val stars = composeRule.onAllNodesWithContentDescription("Remove line 38 from favorites")
        stars.assertCountEquals(2)
        stars[0].assertIsNotEnabled()
        stars[1].assertIsNotEnabled()
        composeRule.runOnIdle { favorites.value = emptySet() }
        composeRule.onNodeWithTag("favorites-divider").assertDoesNotExist()
        composeRule.runOnIdle { favorites.value = setOf("1", "38") }
        composeRule.onNodeWithTag("favorites-divider").assertIsDisplayed()
        composeRule.onAllNodesWithText("Geary").assertCountEquals(2)
        composeRule.onAllNodesWithText("California").assertCountEquals(2)
    }

    @Test
    fun favoriteMembership_whenToggled_keepsMainRouteAndUpdatesBothCopies() {
        val favorites = mutableStateOf(emptySet<String>())
        composeRule.setContent {
            GoMuniTheme {
                TransitLineList(
                    lines = listOf(TransitLine("38", "Geary", "bus")),
                    selectedLineId = null,
                    onLineClick = {},
                    listState = rememberLazyListState(),
                    filterScrollState = rememberScrollState(),
                    favoriteIds = favorites.value,
                    favoritesReady = true,
                    onFavoriteChanged = { id, favorite ->
                        favorites.value =
                            if (favorite) favorites.value + id else favorites.value - id
                    },
                )
            }
        }
        composeRule.onAllNodesWithText("Geary").assertCountEquals(1)
        composeRule.onNodeWithContentDescription("Favorite line 38").performClick()
        composeRule.onAllNodesWithText("Geary").assertCountEquals(2)
        val stars = composeRule.onAllNodesWithContentDescription("Remove line 38 from favorites")
        stars[0].assertIsOn()
        stars[1].assertIsOn().performClick()
        composeRule.onAllNodesWithText("Geary").assertCountEquals(1)
        composeRule.onNodeWithContentDescription("Favorite line 38").assertIsOff()
        composeRule.onNodeWithTag("favorites-divider").assertDoesNotExist()
    }
}
