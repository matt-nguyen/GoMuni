package com.nghianguyen.gomuni.features.transit.arrivals

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.nghianguyen.gomuni.app.navigation.CollectNavigation
import com.nghianguyen.gomuni.app.navigation.StopArrivalsKey
import com.nghianguyen.gomuni.app.navigation.TransitNavigation
import com.nghianguyen.gomuni.ui.theme.EditorialTheme
import com.nghianguyen.gomuni.features.tracking.TrackingRequest
import com.nghianguyen.gomuni.features.transit.model.LoadStatus
import com.nghianguyen.gomuni.features.transit.map.ArrivalMapProjection
import com.nghianguyen.gomuni.features.transit.navigation.LocalArrivalsPresentation
import com.nghianguyen.gomuni.features.transit.navigation.LocalRetryStops
import com.nghianguyen.gomuni.features.transit.navigation.LocalStopsContext
import com.nghianguyen.gomuni.features.transit.navigation.LocalWideLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StopArrivalsRoute(
    key: StopArrivalsKey,
    trackingEnabled: Boolean,
    onTrackingRequest: (TrackingRequest) -> Unit,
    onNavigate: (TransitNavigation) -> Unit,
) {
    val viewModel: StopArrivalsViewModel = hiltViewModel { factory: StopArrivalsViewModel.Factory ->
        factory.create(key.stopId)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalStopsContext.current
    val presentation = LocalArrivalsPresentation.current
    val onRetryContext = LocalRetryStops.current
    LaunchedEffect(context) {
        viewModel.updateContext(context?.stops?.find { it.id == key.stopId },
            context?.status == LoadStatus.LOADED && !context.isRefreshing,
            context?.lines.orEmpty(), context?.error)
    }
    SideEffect { presentation?.mapArrivals = ArrivalMapProjection(state.stopId, state.predictions) }
    DisposableEffect(presentation) { onDispose { presentation?.mapArrivals = null } }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentTrackingRequest by rememberUpdatedState(onTrackingRequest)
    CollectNavigation(viewModel.navigationEvents, onNavigate)
    val listState = rememberLazyListState()
    var elapsedMinutes by remember(state.fetchedAtMillis) { mutableLongStateOf(0) }
    LaunchedEffect(state.fetchedAtMillis, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive && state.fetchedAtMillis != null) {
                elapsedMinutes = ((System.currentTimeMillis() - state.fetchedAtMillis!!) / 60000).coerceAtLeast(0)
                delay(60000)
            }
        }
    }
    val action: (StopArrivalsAction) -> Unit = {
        if (it == StopArrivalsAction.Refresh && state.stop == null) onRetryContext()
        else if (it !is StopArrivalsAction.Track || trackingEnabled) {
            viewModel.onAction(it)
            if (it is StopArrivalsAction.Track) {
                viewModel.takeTrackingRequest()?.let(currentTrackingRequest)
            }
        }
    }
    if (LocalWideLayout.current) {
        StopArrivalsScreen(state, key.lineId, action, listState, elapsedMinutes, trackingEnabled)
    } else {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(onDismissRequest = { viewModel.onAction(StopArrivalsAction.Back) },
            sheetState = sheetState, shape = RectangleShape, containerColor = EditorialTheme.colors.surfaceCard) {
            StopArrivalsScreen(state, key.lineId, action, listState, elapsedMinutes, trackingEnabled,
                Modifier.heightIn(max = androidx.compose.ui.unit.Dp(600f)))
        }
    }
}
