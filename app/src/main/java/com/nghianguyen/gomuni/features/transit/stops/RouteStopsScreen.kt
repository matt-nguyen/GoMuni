package com.nghianguyen.gomuni.features.transit.stops

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.nghianguyen.gomuni.R
import com.nghianguyen.gomuni.ui.components.*
import com.nghianguyen.gomuni.features.transit.components.*
import com.nghianguyen.gomuni.ui.components.TransitLoadContent
import com.nghianguyen.gomuni.ui.theme.EditorialTheme
import com.nghianguyen.gomuni.features.transit.components.TransitDestinationLayout
import com.nghianguyen.gomuni.features.transit.components.TransitExplorerTopBar
import com.nghianguyen.gomuni.features.transit.components.isLoading
import com.nghianguyen.gomuni.features.transit.routes.RoutesUiState

@Composable
fun RouteStopsScreen(
    uiState: RouteStopsUiState,
    onAction: (RouteStopsAction) -> Unit,
    wide: Boolean,
    listState: LazyListState,
    selectedStopId: String?,
    map: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    arrivals: (@Composable () -> Unit)? = null,
) {
    val line = uiState.lines.find { it.id == uiState.route.lineId }
    val routeColor = line?.let { getRouteColor(it, EditorialTheme.colors) } ?: EditorialTheme.colors.muniRed
    Scaffold(modifier.fillMaxSize(), containerColor = EditorialTheme.colors.editorialBlack,
        topBar = {
            TransitExplorerTopBar(line?.let { "${it.id} ${it.name.toTitleCase()}" } ?: uiState.route.lineId,
                routeColor, true, RoutesUiState(), { onAction(RouteStopsAction.Back) }, {}, {})
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            TransitDestinationLayout(wide, Modifier.fillMaxSize(), map) { panelModifier ->
                if (wide && arrivals != null) Box(panelModifier) { arrivals() }
                else TransitLoadContent(uiState.stops.isNotEmpty(), uiState.status.isLoading,
                    uiState.isRefreshing, uiState.error, stringResource(R.string.no_stops),
                    { onAction(RouteStopsAction.Refresh) }, panelModifier) {
                    TransitStopList(uiState.stops, selectedStopId,
                        { stop -> onAction(RouteStopsAction.SelectStop(stop.id)) }, it, listState)
                }
            }
            if (!wide) arrivals?.invoke()
        }
    }
}
