package com.nghianguyen.gomuni.features.transit.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import com.nghianguyen.gomuni.R
import com.nghianguyen.gomuni.app.navigation.TransitStopsKey
import com.nghianguyen.gomuni.domain.model.TransitStop
import com.nghianguyen.gomuni.features.transit.components.TransitDestinationLayout
import com.nghianguyen.gomuni.features.transit.components.TransitExplorerTopBar
import com.nghianguyen.gomuni.features.transit.map.TransitMapData
import com.nghianguyen.gomuni.features.transit.map.TransitMapRoute
import com.nghianguyen.gomuni.features.transit.map.rememberTransitMapViewport
import com.nghianguyen.gomuni.features.transit.routes.RoutesUiState
import com.nghianguyen.gomuni.ui.theme.EditorialTheme

/** Map inputs supplied by a visible route; never contains a ViewModel or MapView. */
internal data class TransitMapPresentation(
    val data: TransitMapData = TransitMapData(emptyList(), emptyList()),
    val selectedStopId: String? = null,
    val focusArrivals: Boolean = false,
    val routeColor: Color,
    val onStopSelected: (TransitStop) -> Unit = {},
)

/** Transit-host presentation scope. Entries register independently during navigation transitions. */
@Stable
internal class SharedTransitMapState {
    private val bindings = mutableStateMapOf<NavKey, TransitMapBinding>()

    fun binding(owner: NavKey): TransitMapBinding? = bindings[owner]

    fun register(owner: NavKey, binding: TransitMapBinding) {
        bindings[owner] = binding
    }

    fun unregister(owner: NavKey, binding: TransitMapBinding) {
        // An outgoing composition must not remove a replacement for the same entry.
        if (bindings[owner] === binding) bindings.remove(owner)
    }
}

@Stable
internal class TransitMapBinding(presentation: TransitMapPresentation) {
    var presentation by mutableStateOf(presentation)
    var topBar by mutableStateOf<(@Composable () -> Unit)?>(null)
}

/**
 * Publishes a route's map inputs for the lifetime of that route composition.
 * @param state Presentation scope owned by the transit host.
 * @param owner Entry supplying these inputs.
 * @param presentation Current data, selection, styling, and interactions.
 * @param topBar Destination header rendered in the shared scaffold.
 */
@Composable
internal fun rememberTransitMapBinding(
    state: SharedTransitMapState,
    owner: NavKey,
    presentation: TransitMapPresentation,
    topBar: (@Composable () -> Unit)? = null,
): TransitMapBinding {
    val binding = remember(state, owner) { TransitMapBinding(presentation) }
    SideEffect {
        binding.presentation = presentation
        binding.topBar = topBar
    }
    DisposableEffect(state, owner, binding) {
        state.register(owner, binding)
        onDispose { state.unregister(owner, binding) }
    }
    return binding
}

/**
 * Owns a stable scaffold and map region independently of destination transitions.
 * @param state Transit-scoped presentation registrations from entry-owned routes.
 * @param destination Current destination, which exclusively selects presentation inputs.
 * @param locationGranted Whether the map can display the user's location.
 * @param wide Whether the map and destination panel are arranged side by side.
 * @param modifier Placement within the host, including tracking-banner padding.
 * @param content Navigation display rendering only destination panels and overlays.
 */
@Composable
internal fun SharedTransitLayout(
    state: SharedTransitMapState,
    destination: NavKey,
    locationGranted: Boolean,
    wide: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val owner = destination
    val binding = state.binding(owner)
    // Retain the last rendered snapshot while the incoming entry registers. This does
    // not retain entry scopes and never gives an outgoing entry control of map clicks.
    var previous by remember { mutableStateOf<RenderedTransitPresentation?>(null) }
    val rendered = binding?.let { RenderedTransitPresentation(owner, it.presentation, it.topBar) }
        ?: previous
    SideEffect { if (binding != null) previous = rendered }
    val presentation = rendered?.map
        ?: TransitMapPresentation(routeColor = EditorialTheme.colors.muniRed)
    val currentBinding by rememberUpdatedState(binding)
    val viewport = rememberTransitMapViewport(
        data = presentation.data,
        routeId = (rendered?.owner as? TransitStopsKey)?.lineId,
        selectedStopId = presentation.selectedStopId,
        focusArrivals = presentation.focusArrivals,
        cameraKey = rendered?.owner,
    )
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = EditorialTheme.colors.editorialBlack,
        topBar = {
            val topBar = rendered?.topBar
            if (topBar != null) topBar()
            else TransitExplorerTopBar(
                title = stringResource(R.string.muni),
                routeColor = null,
                canGoBack = false,
                routesState = RoutesUiState(),
                onBack = {},
                onRefresh = {},
                onSort = {},
            )
        },
    ) { padding ->
        TransitDestinationLayout(
            wide = wide,
            modifier = Modifier.padding(padding),
            map = { mapModifier ->
                TransitMapRoute(
                    data = presentation.data,
                    selectedStopId = presentation.selectedStopId,
                    focusArrivals = presentation.focusArrivals,
                    locationPermissionGranted = locationGranted,
                    routeColor = presentation.routeColor,
                    viewport = viewport,
                    onFollowLocation = { viewport.transitionToFollowPuckState() },
                    onStopSelected = { currentBinding?.presentation?.onStopSelected?.invoke(it) },
                    modifier = mapModifier,
                )
            },
            panel = { panelModifier -> Box(panelModifier.clipToBounds()) { content() } },
        )
    }
}

private data class RenderedTransitPresentation(
    val owner: NavKey,
    val map: TransitMapPresentation,
    val topBar: (@Composable () -> Unit)?,
)
