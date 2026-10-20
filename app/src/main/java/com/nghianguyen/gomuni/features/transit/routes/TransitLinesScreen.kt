package com.nghianguyen.gomuni.features.transit.routes

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.nghianguyen.gomuni.R
import com.nghianguyen.gomuni.features.transit.components.TransitDestinationLayout
import com.nghianguyen.gomuni.features.transit.components.TransitExplorerTopBar
import com.nghianguyen.gomuni.features.transit.components.TransitLineList
import com.nghianguyen.gomuni.features.transit.components.isLoading
import com.nghianguyen.gomuni.ui.components.TransitLoadContent
import com.nghianguyen.gomuni.ui.theme.EditorialTheme

/**
 * Standalone lines screen using the same header and panel as the shared transit flow.
 *
 * @param uiState Current lines and loading state.
 * @param onAction Reports user actions.
 * @param wide Whether to arrange map and list side by side.
 * @param listState List scroll state supplied by the route or preview.
 * @param filterScrollState Filter scroll state supplied by the route or preview.
 * @param menuVisible Whether the sort menu is open.
 * @param onMenuVisibilityChanged Reports changes to sort menu visibility.
 * @param map Supplied map content.
 * @param modifier Caller placement.
 */
@Composable
fun TransitLinesScreen(
    uiState: RoutesUiState,
    onAction: (TransitLinesAction) -> Unit,
    wide: Boolean,
    listState: LazyListState,
    filterScrollState: ScrollState,
    menuVisible: Boolean,
    onMenuVisibilityChanged: (Boolean) -> Unit,
    map: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier.fillMaxSize(),
        containerColor = EditorialTheme.colors.editorialBlack,
        topBar = {
            TransitLinesTopBar(uiState, onAction, menuVisible, onMenuVisibilityChanged)
        },
    ) { padding ->
        TransitDestinationLayout(wide, Modifier.padding(padding), map) { panelModifier ->
            TransitLinesPanel(
                uiState = uiState,
                onAction = onAction,
                listState = listState,
                filterScrollState = filterScrollState,
                modifier = panelModifier,
            )
        }
    }
}

/**
 * Renders the lines header in either a standalone screen or the shared transit scaffold.
 *
 * @param uiState Current lines and sort state.
 * @param onAction Reports header actions.
 * @param menuVisible Whether the sort menu is open.
 * @param onMenuVisibilityChanged Reports sort menu visibility.
 */
@Composable
internal fun TransitLinesTopBar(
    uiState: RoutesUiState,
    onAction: (TransitLinesAction) -> Unit,
    menuVisible: Boolean,
    onMenuVisibilityChanged: (Boolean) -> Unit,
) {
    TransitExplorerTopBar(
        title = stringResource(R.string.muni),
        routeColor = null,
        canGoBack = false,
        routesState = uiState,
        onBack = {},
        onSort = { onAction(TransitLinesAction.ChangeSort(it)) },
        menuVisible = menuVisible,
        onMenuVisibilityChanged = onMenuVisibilityChanged,
    )
}

/**
 * Renders the lines list without owning a map or scaffold.
 *
 * @param uiState Current loading, filter, and lines state.
 * @param onAction Reports list, filter, and retry actions.
 * @param listState Route-owned list scroll state.
 * @param filterScrollState Route-owned filter scroll state.
 * @param modifier Placement in the shared or standalone destination panel.
 */
@Composable
internal fun TransitLinesPanel(
    uiState: RoutesUiState,
    onAction: (TransitLinesAction) -> Unit,
    listState: LazyListState,
    filterScrollState: ScrollState,
    modifier: Modifier = Modifier,
) {
    TransitLoadContent(
        uiState.lines.isNotEmpty(),
        uiState.status.isLoading,
        false,
        uiState.error,
        stringResource(R.string.no_routes),
        { onAction(TransitLinesAction.RetryLoad) },
        modifier,
    ) {
        Column(it) {
            if (uiState.favoriteError != null) {
                Text(
                    text =
                        stringResource(
                            if (uiState.favoriteError == FavoriteError.READ)
                                R.string.favorites_read_failed
                            else R.string.favorites_write_failed,
                        ),
                    color = EditorialTheme.colors.electricAlert,
                    modifier = Modifier.padding(EditorialTheme.spacing.spaceMd),
                )
                if (uiState.favoriteError == FavoriteError.READ) {
                    TextButton(onClick = { onAction(TransitLinesAction.RetryFavorites) }) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }
            TransitLineList(
                lines = uiState.lines,
                selectedLineId = null,
                onLineClick = { id -> onAction(TransitLinesAction.SelectRoute(id)) },
                modifier = Modifier.weight(1f),
                activeFilter = uiState.filter,
                onFilterSelected = { filter -> onAction(TransitLinesAction.ChangeFilter(filter)) },
                listState = listState,
                filterScrollState = filterScrollState,
                favoriteIds = uiState.favoriteIds,
                pendingFavoriteIds = uiState.pendingFavoriteIds,
                favoritesReady = uiState.favoritesReady,
                onFavoriteChanged = { id, favorite ->
                    onAction(TransitLinesAction.SetFavorite(id, favorite))
                },
            )
        }
    }
}
