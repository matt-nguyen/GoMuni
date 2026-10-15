package com.nghianguyen.gomuni.features.transit.stops

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nghianguyen.gomuni.app.navigation.CollectNavigation
import com.nghianguyen.gomuni.app.navigation.StopArrivalsTarget
import com.nghianguyen.gomuni.app.navigation.TransitNavigation
import com.nghianguyen.gomuni.app.navigation.TransitStopsKey
import com.nghianguyen.gomuni.features.tracking.TrackingRequest
import com.nghianguyen.gomuni.features.transit.arrivals.ArrivalsSheetScope
import com.nghianguyen.gomuni.features.transit.arrivals.StopArrivalsRoute
import com.nghianguyen.gomuni.features.transit.components.*
import com.nghianguyen.gomuni.features.transit.map.ArrivalMapProjection
import com.nghianguyen.gomuni.features.transit.map.TransitMapData
import com.nghianguyen.gomuni.features.transit.model.RouteKey
import com.nghianguyen.gomuni.features.transit.navigation.SharedTransitMapState
import com.nghianguyen.gomuni.features.transit.navigation.TransitMapPresentation
import com.nghianguyen.gomuni.features.transit.navigation.rememberTransitMapBinding
import com.nghianguyen.gomuni.ui.components.*
import com.nghianguyen.gomuni.ui.theme.EditorialTheme

/**
 * Connects the stops entry and its modal arrivals sheet to the shared transit map.
 *
 * @param key Route and operator identifying this entry.
 * @param sharedMap Transit-scoped map presentation registrations.
 * @param onNavigate Forwards eligible navigation requests to the host.
 * @param isActive Whether this entry may present a modal sheet.
 * @param pendingStopNavigation External stop to open after this route becomes active.
 * @param onStopNavigationHandled Acknowledges that the sheet target is stored in route state.
 * @param trackingEnabled Whether another tracking startup is allowed.
 * @param onTrackingRequest Hands validated tracking requests to the host.
 */
@Composable
internal fun RouteStopsRoute(
    key: TransitStopsKey,
    sharedMap: SharedTransitMapState,
    onNavigate: (TransitNavigation) -> Unit,
    isActive: Boolean,
    pendingStopNavigation: StopArrivalsTarget?,
    onStopNavigationHandled: () -> Unit,
    trackingEnabled: Boolean,
    onTrackingRequest: (TrackingRequest) -> Unit,
) {
    val viewModel: RouteStopsViewModel = hiltViewModel { factory: RouteStopsViewModel.Factory ->
        factory.create(RouteKey(key.operatorId, key.lineId))
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentHandled by rememberUpdatedState(onStopNavigationHandled)
    LaunchedEffect(pendingStopNavigation) {
        pendingStopNavigation?.let { target ->
            viewModel.onAction(RouteStopsAction.OpenArrivals(target.stopId))
            currentHandled()
        }
    }
    var mapArrivals by remember(key) { mutableStateOf<ArrivalMapProjection?>(null) }
    val selectedStopId = state.selectedStopId
    val listState = rememberLazyListState()
    val line = state.lines.find { it.id == key.lineId }
    val routeColor =
        line?.let { getRouteColor(it, EditorialTheme.colors) } ?: EditorialTheme.colors.muniRed
    val mapData = TransitMapData.from(state.stops, selectedStopId, key.lineId, mapArrivals)
    rememberTransitMapBinding(
        state = sharedMap,
        owner = key,
        presentation =
            TransitMapPresentation(
                data = mapData,
                selectedStopId = selectedStopId,
                focusArrivals = state.arrivalsStopId != null,
                routeColor = routeColor,
                onStopSelected = { viewModel.onAction(RouteStopsAction.SelectStop(it.id)) },
            ),
        topBar = { RouteStopsTopBar(uiState = state, onAction = viewModel::onAction) },
    )
    CollectNavigation(viewModel.navigationEvents, onNavigate)
    RouteStopsPanel(
        uiState = state,
        onAction = viewModel::onAction,
        listState = listState,
        selectedStopId = selectedStopId,
    )
    if (isActive)
        state.arrivalsStopId?.let { stopId ->
            ArrivalsSheetScope(stopId = stopId) {
                StopArrivalsRoute(
                    stopId = stopId,
                    lineId = key.lineId,
                    context = state,
                    trackingEnabled = trackingEnabled,
                    onTrackingRequest = onTrackingRequest,
                    onRetryContext = { viewModel.onAction(RouteStopsAction.Refresh) },
                    onDismiss = { viewModel.onAction(RouteStopsAction.DismissArrivals(stopId)) },
                    onMapArrivalsChanged = { projection ->
                        if (
                            state.arrivalsStopId == stopId ||
                                projection == null && mapArrivals?.stopId == stopId
                        ) {
                            mapArrivals = projection
                        }
                    },
                )
            }
        }
}
