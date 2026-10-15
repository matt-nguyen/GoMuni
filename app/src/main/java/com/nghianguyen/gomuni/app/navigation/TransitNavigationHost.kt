package com.nghianguyen.gomuni.app.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.nghianguyen.gomuni.features.tracking.TrackingAction
import com.nghianguyen.gomuni.features.tracking.TrackingEffects
import com.nghianguyen.gomuni.features.tracking.TrackingRequest
import com.nghianguyen.gomuni.features.tracking.TrackingStatusBanner
import com.nghianguyen.gomuni.features.tracking.TrackingViewModel
import com.nghianguyen.gomuni.features.transit.arrivals.StopArrivalsRoute
import com.nghianguyen.gomuni.features.transit.navigation.ArrivalsSceneStrategy
import com.nghianguyen.gomuni.features.transit.navigation.LocalWideLayout
import com.nghianguyen.gomuni.features.transit.navigation.TRANSIT_DESTINATION
import com.nghianguyen.gomuni.features.transit.routes.TransitLinesRoute
import com.nghianguyen.gomuni.features.transit.stops.RouteStopsRoute
import com.nghianguyen.gomuni.platform.permissions.rememberLocationPermissionGranted
import com.nghianguyen.gomuni.ui.theme.EditorialTheme

/**
 * Owns transit history, external navigation, and shared tracking integration.
 *
 * @param pendingStopNavigation External stop destination awaiting navigation, or null.
 * @param onStopNavigationHandled Acknowledges that the external destination was handled.
 */
@Composable
fun TransitNavigationHost(
    pendingStopNavigation: StopArrivalsKey?,
    onStopNavigationHandled: () -> Unit,
) {
    // Outside entries: tracking startup uses the surrounding Activity's ViewModel store.
    val tracking: TrackingViewModel = hiltViewModel()
    val trackingState by tracking.uiState.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack(TransitLinesKey)
    val currentHandled by rememberUpdatedState(onStopNavigationHandled)
    LaunchedEffect(pendingStopNavigation) {
        pendingStopNavigation?.let { target ->
            navigate(backStack, TransitNavigation.OpenStop(target.operatorId, target.lineId, target.stopId))
            currentHandled()
        }
    }
    val dispatch: (NavKey, TransitNavigation) -> Unit = { origin, event ->
        if (canNavigate(origin, backStack.lastOrNull())) navigate(backStack, event)
    }
    val locationGranted = rememberLocationPermissionGranted()
    TrackingEffects(effects = tracking.effects, onAction = tracking::onAction)
    Scaffold(
        containerColor = EditorialTheme.colors.editorialBlack,
        // Destinations own system insets; the host only allocates banner space.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            TrackingStatusBanner(
                state = trackingState,
                onDismiss = { tracking.onAction(TrackingAction.DismissFailure) },
            )
        },
    ) { padding ->
        BoxWithConstraints(Modifier.padding(padding).fillMaxSize()) {
            CompositionLocalProvider(LocalWideLayout provides (maxWidth >= 600.dp)) {
                TransitNavDisplay(
                    backStack = backStack,
                    locationGranted = locationGranted,
                    trackingEnabled = !trackingState.isStarting,
                    onBack = { navigate(backStack, TransitNavigation.Back) },
                    onNavigate = dispatch,
                    onStartTracking = { tracking.onAction(TrackingAction.Start(it)) },
                )
            }
        }
    }
}

/**
 * Registers destinations and their entry scopes and scene presentation.
 *
 * @param backStack Destination history supplied by the navigation owner.
 * @param locationGranted Whether destination routes can use location.
 * @param trackingEnabled Whether another tracking startup may be requested.
 * @param onBack Requests a history pop from the navigation owner.
 * @param onNavigate Forwards navigation with its originating destination.
 * @param onStartTracking Forwards a validated tracking target.
 */
@Composable
private fun TransitNavDisplay(
    backStack: List<NavKey>,
    locationGranted: Boolean,
    trackingEnabled: Boolean,
    onBack: () -> Unit,
    onNavigate: (NavKey, TransitNavigation) -> Unit,
    onStartTracking: (TrackingRequest) -> Unit,
) {
    NavDisplay(
        backStack = backStack,
        onBack = onBack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        sceneStrategies = listOf(remember { ArrivalsSceneStrategy() }),
        entryProvider = entryProvider<NavKey> {
            entry<TransitLinesKey> {
                TransitLinesRoute(
                    locationGranted = locationGranted,
                    onNavigate = { onNavigate(TransitLinesKey, it) },
                )
            }
            entry<TransitStopsKey>(metadata = { mapOf(TRANSIT_DESTINATION to it) }) { key ->
                RouteStopsRoute(
                    key = key,
                    locationGranted = locationGranted,
                    onNavigate = { onNavigate(key, it) },
                )
            }
            entry<StopArrivalsKey>(metadata = { mapOf(TRANSIT_DESTINATION to it) }) { key ->
                StopArrivalsRoute(
                    key = key,
                    trackingEnabled = trackingEnabled,
                    onTrackingRequest = onStartTracking,
                    onNavigate = { onNavigate(key, it) },
                )
            }
        },
    )
}
