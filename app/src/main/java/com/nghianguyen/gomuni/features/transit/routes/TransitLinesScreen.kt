package com.nghianguyen.gomuni.features.transit.routes

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.nghianguyen.gomuni.R
import com.nghianguyen.gomuni.features.transit.components.*
import com.nghianguyen.gomuni.features.transit.components.TransitDestinationLayout
import com.nghianguyen.gomuni.features.transit.components.TransitExplorerTopBar
import com.nghianguyen.gomuni.features.transit.components.isLoading
import com.nghianguyen.gomuni.ui.components.*
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
        onRefresh = { onAction(TransitLinesAction.Refresh) },
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
        uiState.isRefreshing,
        uiState.error,
        stringResource(R.string.no_routes),
        { onAction(TransitLinesAction.Refresh) },
        modifier,
    ) {
        TransitLineList(
            uiState.lines,
            null,
            { id -> onAction(TransitLinesAction.SelectRoute(id)) },
            it,
            uiState.filter,
            { filter -> onAction(TransitLinesAction.ChangeFilter(filter)) },
            listState,
            filterScrollState,
        )
    }
}
