package com.nghianguyen.gomuni.features.transit.stops

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nghianguyen.gomuni.ui.components.*
import com.nghianguyen.gomuni.features.transit.components.*
import com.nghianguyen.gomuni.app.navigation.CollectNavigation
import com.nghianguyen.gomuni.app.navigation.TransitNavigation
import com.nghianguyen.gomuni.app.navigation.TransitStopsKey
import com.nghianguyen.gomuni.ui.theme.EditorialTheme
import com.nghianguyen.gomuni.features.transit.map.TransitMapData
import com.nghianguyen.gomuni.features.transit.map.TransitMapRoute
import com.nghianguyen.gomuni.features.transit.map.rememberTransitMapViewport
import com.nghianguyen.gomuni.features.transit.model.RouteKey
import com.nghianguyen.gomuni.features.transit.navigation.LocalArrivalsContent
import com.nghianguyen.gomuni.features.transit.navigation.LocalArrivalsPresentation
import com.nghianguyen.gomuni.features.transit.navigation.LocalRetryStops
import com.nghianguyen.gomuni.features.transit.navigation.LocalStopsContext
import com.nghianguyen.gomuni.features.transit.navigation.LocalWideLayout

@Composable
internal fun RouteStopsRoute(
    key: TransitStopsKey,
    locationGranted: Boolean,
    onNavigate: (TransitNavigation) -> Unit,
) {
    val viewModel: RouteStopsViewModel = hiltViewModel { factory: RouteStopsViewModel.Factory ->
        factory.create(RouteKey(key.operatorId, key.lineId))
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val presentation = LocalArrivalsPresentation.current
    val arrivals = LocalArrivalsContent.current
    val selectedStopId = presentation?.destination?.stopId ?: state.selectedStopId
    LaunchedEffect(presentation?.destination) {
        presentation?.destination?.stopId?.let(viewModel::rememberSelection)
    }
    val listState = rememberLazyListState()
    val line = state.lines.find { it.id == key.lineId }
    val routeColor = line?.let { getRouteColor(it, EditorialTheme.colors) } ?: EditorialTheme.colors.muniRed
    val mapData = TransitMapData.from(state.stops, selectedStopId, key.lineId, presentation?.mapArrivals)
    val viewport = rememberTransitMapViewport(mapData, key.lineId, selectedStopId, presentation != null)
    CollectNavigation(viewModel.navigationEvents, onNavigate)
    CompositionLocalProvider(LocalStopsContext provides state,
        LocalRetryStops provides { viewModel.onAction(RouteStopsAction.Refresh) }) {
        RouteStopsScreen(state, viewModel::onAction, LocalWideLayout.current, listState, selectedStopId,
            map = {
                TransitMapRoute(mapData, selectedStopId, presentation != null, locationGranted,
                    routeColor, viewport, { viewport.transitionToFollowPuckState() },
                    { viewModel.onAction(RouteStopsAction.SelectStop(it.id)) }, it)
            }, arrivals = arrivals)
    }
}
