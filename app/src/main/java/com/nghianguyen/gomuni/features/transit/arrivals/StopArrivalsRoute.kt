package com.nghianguyen.gomuni.features.transit.arrivals

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.nghianguyen.gomuni.features.tracking.TrackingRequest
import com.nghianguyen.gomuni.features.transit.map.ArrivalMapProjection
import com.nghianguyen.gomuni.features.transit.model.LoadStatus
import com.nghianguyen.gomuni.features.transit.stops.RouteStopsUiState
import com.nghianguyen.gomuni.ui.theme.EditorialTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Integrates sheet-scoped arrivals state with explicit parent context and callbacks.
 *
 * @param stopId Stop whose arrivals are shown.
 * @param lineId Route used to group arrivals and construct tracking requests.
 * @param context Authoritative stops and route metadata from the parent route.
 * @param trackingEnabled Whether tracking startup can be requested.
 * @param onTrackingRequest Hands a validated tracking target to the host.
 * @param onRetryContext Retries initial loading of missing parent stop context.
 * @param onDismiss Closes this sheet session without navigating.
 * @param onMapArrivalsChanged Publishes or clears vehicle predictions for the shared map.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StopArrivalsRoute(
    stopId: String,
    lineId: String,
    context: RouteStopsUiState,
    trackingEnabled: Boolean,
    onTrackingRequest: (TrackingRequest) -> Unit,
    onRetryContext: () -> Unit,
    onDismiss: () -> Unit,
    onMapArrivalsChanged: (ArrivalMapProjection?) -> Unit,
) {
    val viewModel: StopArrivalsViewModel = hiltViewModel { factory: StopArrivalsViewModel.Factory ->
        factory.create(stopId)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(context, stopId) {
        viewModel.updateContext(
            context.stops.find { it.id == stopId },
            context.status == LoadStatus.LOADED,
            context.lines,
            context.error,
        )
    }
    val currentMapChanged by rememberUpdatedState(onMapArrivalsChanged)
    SideEffect { currentMapChanged(ArrivalMapProjection(state.stopId, state.predictions)) }
    DisposableEffect(stopId) { onDispose { currentMapChanged(null) } }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentTrackingRequest by rememberUpdatedState(onTrackingRequest)
    val listState = rememberLazyListState()
    var elapsedMinutes by remember(state.fetchedAtMillis) { mutableLongStateOf(0) }
    LaunchedEffect(state.fetchedAtMillis, lifecycle) {
        val fetchedAtMillis = state.fetchedAtMillis ?: return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                elapsedMinutes =
                    ((System.currentTimeMillis() - fetchedAtMillis) / ARRIVAL_AGE_TICK_MILLIS)
                        .coerceAtLeast(
                            0,
                        )
                delay(ARRIVAL_AGE_TICK_MILLIS)
            }
        }
    }
    val action: (StopArrivalsAction) -> Unit = {
        if (it == StopArrivalsAction.Dismiss) onDismiss()
        else if (it == StopArrivalsAction.Refresh && state.stop == null) onRetryContext()
        else if (it !is StopArrivalsAction.Track || trackingEnabled) {
            viewModel.onAction(it)
            if (it is StopArrivalsAction.Track) {
                viewModel.takeTrackingRequest()?.let(currentTrackingRequest)
            }
        }
    }
    val sheetState = rememberModalBottomSheetState()
    StopArrivalsSheet(sheetState = sheetState, onDismiss = onDismiss) {
        StopArrivalsScreen(
            uiState = state,
            lineId = lineId,
            onAction = action,
            listState = listState,
            elapsedMinutes = elapsedMinutes,
            trackingEnabled = trackingEnabled,
            modifier =
                Modifier.heightIn(max = androidx.compose.ui.unit.Dp(ARRIVALS_SHEET_MAX_HEIGHT_DP)),
        )
    }
}

/**
 * Presents arrivals in the same modal sheet on compact and wide displays.
 *
 * @param sheetState Sheet animation state owned by the integration route.
 * @param onDismiss Reports Back, swipe, or outside-tap dismissal.
 * @param content Stateless arrivals content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StopArrivalsSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RectangleShape,
        containerColor = EditorialTheme.colors.surfaceCard,
    ) {
        content()
    }
}

private const val ARRIVAL_AGE_TICK_MILLIS = 60_000L
private const val ARRIVALS_SHEET_MAX_HEIGHT_DP = 600f
