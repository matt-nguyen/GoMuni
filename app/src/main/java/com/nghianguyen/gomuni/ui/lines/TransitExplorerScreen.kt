package com.nghianguyen.gomuni.ui.lines

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nghianguyen.gomuni.R
import com.nghianguyen.gomuni.domain.model.BusArrival
import com.nghianguyen.gomuni.domain.model.TransitStop
import com.nghianguyen.gomuni.ui.theme.EditorialTheme

@Composable
fun TransitExplorerScreen(
    uiState: TransitExplorerUiState,
    locationPermissionGranted: Boolean,
    onAction: (TransitExplorerAction) -> Unit,
    onTrackArrival: (BusArrival, TransitStop) -> Unit,
    modifier: Modifier = Modifier,
) {
    val route = uiState.routeStopsState
    val line = uiState.routesState.lines.find { it.id == route?.route?.lineId }
    val colors = EditorialTheme.colors
    val routeColor = if (route != null) line?.let { getRouteColor(it, colors) } ?: colors.muniRed else null
    val showArrivals = uiState.arrivalSheetVisible && route?.selectedStop != null
    val onDismissArrivals = { onAction(TransitExplorerAction.DismissArrivals) }
    val onRefreshRoutes = { onAction(TransitExplorerAction.RefreshRoutes) }
    val onRetryStops = { onAction(TransitExplorerAction.RetryStops) }
    val onRefreshArrivals: (String) -> Unit = { onAction(TransitExplorerAction.RefreshArrivals) }
    val onLineSelected: (String) -> Unit = {
        onAction(TransitExplorerAction.SelectRoute(RouteKey(route?.route?.operatorId ?: "SF", it)))
    }
    val onStopSelected: (TransitStop) -> Unit = { onAction(TransitExplorerAction.SelectStop(it.id)) }
    val onBack = {
        onAction(if (showArrivals) TransitExplorerAction.DismissArrivals else TransitExplorerAction.ClearRoute)
    }
    BackHandler(enabled = route != null || showArrivals, onBack = onBack)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.editorialBlack,
        topBar = {
            TransitExplorerTopBar(
                title = line?.let { "${it.id} ${it.name.toTitleCase()}" } ?: route?.route?.lineId ?: stringResource(R.string.muni),
                routeColor = routeColor,
                canGoBack = route != null || showArrivals,
                routesState = uiState.routesState,
                onBack = onBack,
                onRefresh = onRefreshRoutes,
                onSort = { onAction(TransitExplorerAction.ChangeSort(it)) },
            )
        },
    ) { padding ->
        BoxWithConstraints(Modifier.padding(padding).fillMaxSize()) {
            val wide = maxWidth >= 600.dp
            val map: @Composable (Modifier) -> Unit = { mapModifier ->
                TransitMap(route, uiState.arrivalsState, showArrivals, locationPermissionGranted,
                    routeColor ?: colors.muniRed, onStopSelected, mapModifier)
            }
            val panel: @Composable (Modifier) -> Unit = { panelModifier ->
                if (wide && showArrivals) {
                    StopArrivalsSheet(route, uiState.arrivalsState, uiState.routesState.lines, true,
                        onDismissArrivals, onRefreshArrivals, onTrackArrival, panelModifier)
                } else TransitBrowsePanel(uiState.routesState, route, onLineSelected, onStopSelected,
                    onRefreshRoutes, onRetryStops, panelModifier)
            }
            if (wide) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(EditorialTheme.spacing.columnGap)) {
                    map(Modifier.weight(5f).fillMaxHeight())
                    panel(Modifier.weight(3f).fillMaxHeight())
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    map(Modifier.weight(0.4f).fillMaxWidth())
                    panel(Modifier.weight(0.6f).fillMaxWidth())
                }
                if (showArrivals) StopArrivalsSheet(route, uiState.arrivalsState,
                    uiState.routesState.lines, false, onDismissArrivals, onRefreshArrivals, onTrackArrival)
            }
        }
    }
}
