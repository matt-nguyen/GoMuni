package com.nghianguyen.gomuni.app.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
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
import com.nghianguyen.gomuni.features.transit.navigation.SharedTransitLayout
import com.nghianguyen.gomuni.features.transit.navigation.SharedTransitMapState
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
    pendingStopNavigation: StopArrivalsTarget?,
    onStopNavigationHandled: () -> Unit,
) {
    // Outside entries: tracking startup uses the surrounding Activity's ViewModel store.
    val tracking: TrackingViewModel = hiltViewModel()
    val trackingState by tracking.uiState.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack(TransitLinesKey)
    val currentHandled by rememberUpdatedState(onStopNavigationHandled)
    LaunchedEffect(pendingStopNavigation) {
        pendingStopNavigation?.let { target ->
            if (!navigateToStopRoute(backStack, target)) currentHandled()
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
            val sharedMap = remember { SharedTransitMapState() }
            SharedTransitLayout(
                state = sharedMap,
                destination = backStack.lastOrNull() ?: TransitLinesKey,
                locationGranted = locationGranted,
                wide = maxWidth >= 600.dp,
            ) {
                TransitNavDisplay(
                    backStack = backStack,
                    sharedMap = sharedMap,
                    trackingEnabled = !trackingState.isStarting,
                    onBack = { navigate(backStack, TransitNavigation.Back) },
                    onNavigate = dispatch,
                    onStartTracking = { tracking.onAction(TrackingAction.Start(it)) },
                    pendingStopNavigation = pendingStopNavigation,
                    onStopNavigationHandled = onStopNavigationHandled,
                )
            }
        }
    }
}

/**
 * Registers lines and stops entries; arrivals are local sheets inside stops routes.
 *
 * @param backStack Destination history supplied by the navigation owner.
 * @param sharedMap Host-owned presentation scope for the persistent transit map.
 * @param trackingEnabled Whether another tracking startup may be requested.
 * @param onBack Requests a history pop from the navigation owner.
 * @param onNavigate Forwards navigation with its originating destination.
 * @param onStartTracking Forwards a validated tracking target.
 * @param pendingStopNavigation External sheet target awaiting delivery to its route.
 * @param onStopNavigationHandled Acknowledges that the route stored the sheet target.
 */
@Composable
private fun TransitNavDisplay(
    backStack: List<NavKey>,
    sharedMap: SharedTransitMapState,
    trackingEnabled: Boolean,
    onBack: () -> Unit,
    onNavigate: (NavKey, TransitNavigation) -> Unit,
    onStartTracking: (TrackingRequest) -> Unit,
    pendingStopNavigation: StopArrivalsTarget?,
    onStopNavigationHandled: () -> Unit,
) {
    NavDisplay(
        backStack = backStack,
        onBack = onBack,
        transitionSpec = { transitSlideTransition(forward = true) },
        popTransitionSpec = { transitSlideTransition(forward = false) },
        predictivePopTransitionSpec = { transitSlideTransition(forward = false) },
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider =
            entryProvider<NavKey> {
                entry<TransitLinesKey> {
                    TransitLinesRoute(
                        sharedMap = sharedMap,
                        onNavigate = { navigation -> onNavigate(TransitLinesKey, navigation) },
                    )
                }
                entry<TransitStopsKey> { key ->
                    RouteStopsRoute(
                        key = key,
                        sharedMap = sharedMap,
                        onNavigate = { onNavigate(key, it) },
                        isActive = backStack.lastOrNull() == key,
                        pendingStopNavigation =
                            pendingStopNavigation?.takeIf {
                                backStack.lastOrNull() == key &&
                                    it.lineId == key.lineId &&
                                    it.operatorId == key.operatorId
                            },
                        onStopNavigationHandled = onStopNavigationHandled,
                        trackingEnabled = trackingEnabled,
                        onTrackingRequest = onStartTracking,
                    )
                }
            },
    )
}
