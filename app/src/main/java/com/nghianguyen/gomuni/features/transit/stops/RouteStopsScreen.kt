package com.nghianguyen.gomuni.features.transit.stops

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.nghianguyen.gomuni.R
import com.nghianguyen.gomuni.features.transit.components.TransitDestinationLayout
import com.nghianguyen.gomuni.features.transit.components.TransitExplorerTopBar
import com.nghianguyen.gomuni.features.transit.components.TransitStopList
import com.nghianguyen.gomuni.features.transit.components.getRouteColor
import com.nghianguyen.gomuni.features.transit.components.isLoading
import com.nghianguyen.gomuni.features.transit.components.toTitleCase
import com.nghianguyen.gomuni.features.transit.routes.RoutesUiState
import com.nghianguyen.gomuni.ui.components.TransitLoadContent
import com.nghianguyen.gomuni.ui.theme.EditorialTheme

/**
 * Standalone stops screen using the shared flow's header and panel components.
 *
 * @param uiState Current route, stops, and loading state.
 * @param onAction Reports user actions.
 * @param wide Whether to arrange map and details side by side.
 * @param listState Entry-owned list scroll state.
 * @param selectedStopId Currently selected stop identity.
 * @param map Supplied map content.
 * @param modifier Caller placement.
 */
@Composable
fun RouteStopsScreen(
    uiState: RouteStopsUiState,
    onAction: (RouteStopsAction) -> Unit,
    wide: Boolean,
    listState: LazyListState,
    selectedStopId: String?,
    map: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier.fillMaxSize(),
        containerColor = EditorialTheme.colors.editorialBlack,
        topBar = {
            RouteStopsTopBar(uiState = uiState, onAction = onAction)
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            TransitDestinationLayout(wide, Modifier.fillMaxSize(), map) { panelModifier ->
                RouteStopsPanel(
                    uiState = uiState,
                    onAction = onAction,
                    listState = listState,
                    selectedStopId = selectedStopId,
                    modifier = panelModifier,
                )
            }
        }
    }
}

/**
 * Renders the stops header in the standalone or shared transit scaffold.
 *
 * @param uiState Route metadata used for the title and route color.
 * @param onAction Reports the toolbar Back action.
 */
@Composable
internal fun RouteStopsTopBar(uiState: RouteStopsUiState, onAction: (RouteStopsAction) -> Unit) {
    val line = uiState.lines.find { it.id == uiState.route.lineId }
    val routeColor =
        line?.let { getRouteColor(it, EditorialTheme.colors) } ?: EditorialTheme.colors.muniRed
    TransitExplorerTopBar(
        title = line?.let { "${it.id} ${it.name.toTitleCase()}" } ?: uiState.route.lineId,
        routeColor = routeColor,
        canGoBack = true,
        routesState = RoutesUiState(),
        onBack = { onAction(RouteStopsAction.Back) },
        onRefresh = {},
        onSort = {},
    )
}

/**
 * Renders the stops list behind the route-owned arrivals sheet.
 *
 * @param uiState Current stops and loading state.
 * @param onAction Reports stop selection and retry actions.
 * @param listState Entry-owned list scroll state.
 * @param selectedStopId Currently selected stop identity.
 * @param modifier Placement in the destination panel.
 */
@Composable
internal fun RouteStopsPanel(
    uiState: RouteStopsUiState,
    onAction: (RouteStopsAction) -> Unit,
    listState: LazyListState,
    selectedStopId: String?,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        TransitLoadContent(
            uiState.stops.isNotEmpty(),
            uiState.status.isLoading,
            uiState.isRefreshing,
            uiState.error,
            stringResource(R.string.no_stops),
            { onAction(RouteStopsAction.Refresh) },
            Modifier.fillMaxSize(),
        ) {
            TransitStopList(
                uiState.stops,
                selectedStopId,
                { stop -> onAction(RouteStopsAction.SelectStop(stop.id)) },
                it,
                listState,
            )
        }
    }
}
