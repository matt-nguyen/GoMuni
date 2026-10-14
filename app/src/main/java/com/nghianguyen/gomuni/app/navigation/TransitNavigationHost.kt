package com.nghianguyen.gomuni.app.navigation

import com.nghianguyen.gomuni.features.transit.navigation.TRANSIT_DESTINATION

import androidx.compose.foundation.layout.*
import com.nghianguyen.gomuni.platform.permissions.rememberLocationPermissionGranted
import com.nghianguyen.gomuni.features.tracking.TrackingEffects
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
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
import com.nghianguyen.gomuni.ui.theme.EditorialTheme
import com.nghianguyen.gomuni.features.tracking.TrackingAction
import com.nghianguyen.gomuni.features.tracking.TrackingCoordinatorViewModel
import com.nghianguyen.gomuni.features.tracking.TrackingStatusBanner
import com.nghianguyen.gomuni.features.tracking.isPending
import com.nghianguyen.gomuni.features.transit.arrivals.StopArrivalsRoute
import com.nghianguyen.gomuni.features.transit.navigation.ArrivalsSceneStrategy
import com.nghianguyen.gomuni.features.transit.navigation.LocalWideLayout
import com.nghianguyen.gomuni.features.transit.routes.TransitLinesRoute
import com.nghianguyen.gomuni.features.transit.stops.RouteStopsRoute

@Composable
fun TransitNavigationHost(externalTarget: StopArrivalsKey?, onExternalTargetConsumed: () -> Unit) {
    // Acquired outside entries intentionally: tracking startup spans the entire host.
    val tracking: TrackingCoordinatorViewModel = hiltViewModel()
    val trackingState by tracking.uiState.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack(TransitLinesKey)
    LaunchedEffect(externalTarget) {
        externalTarget?.let {
            navigate(backStack, TransitNavigation.OpenStop(it.operatorId, it.lineId, it.stopId))
            onExternalTargetConsumed()
        }
    }
    val dispatch: (NavKey, TransitNavigation) -> Unit = { origin, event ->
        val top = backStack.lastOrNull()
        val parentEligible = origin is TransitStopsKey && top is StopArrivalsKey &&
            origin == TransitStopsKey(top.lineId, top.operatorId)
        if (top == origin || parentEligible) navigate(backStack, event)
    }
    val locationGranted = rememberLocationPermissionGranted()
    TrackingEffects(tracking)
    Scaffold(containerColor = EditorialTheme.colors.editorialBlack,
        // Destination scaffolds consume system insets; this scaffold only allocates banner space.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            TrackingStatusBanner(trackingState) { tracking.onAction(TrackingAction.DismissFailure(it)) }
        }) { padding ->
        BoxWithConstraints(Modifier.padding(padding).fillMaxSize()) {
            CompositionLocalProvider(LocalWideLayout provides (maxWidth >= 600.dp)) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { navigate(backStack, TransitNavigation.Back) },
                    entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator()),
                    sceneStrategies = listOf(remember { ArrivalsSceneStrategy() }),
                    entryProvider = entryProvider<NavKey> {
                        entry<TransitLinesKey> {
                            TransitLinesRoute(locationGranted) { dispatch(TransitLinesKey, it) }
                        }
                        entry<TransitStopsKey>(metadata = { mapOf(TRANSIT_DESTINATION to it) }) { key ->
                            RouteStopsRoute(key, locationGranted) { dispatch(key, it) }
                        }
                        entry<StopArrivalsKey>(metadata = { mapOf(TRANSIT_DESTINATION to it) }) { key ->
                            StopArrivalsRoute(key, !trackingState.isPending,
                                { tracking.onAction(TrackingAction.Start(it)) }, { dispatch(key, it) })
                        }
                    },
                )
            }
        }
    }
}
