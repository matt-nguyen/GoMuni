package com.nghianguyen.gomuni.features.transit.routes

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nghianguyen.gomuni.app.navigation.CollectNavigation
import com.nghianguyen.gomuni.app.navigation.TransitLinesKey
import com.nghianguyen.gomuni.app.navigation.TransitNavigation
import com.nghianguyen.gomuni.features.transit.navigation.SharedTransitMapState
import com.nghianguyen.gomuni.features.transit.navigation.TransitMapPresentation
import com.nghianguyen.gomuni.features.transit.navigation.rememberTransitMapBinding
import com.nghianguyen.gomuni.ui.theme.EditorialTheme

/**
 * Connects the lines entry to its list and the host-owned map.
 *
 * @param sharedMap Transit-scoped map presentation registrations.
 * @param onNavigate Forwards eligible navigation requests to the host.
 */
@Composable
internal fun TransitLinesRoute(
    sharedMap: SharedTransitMapState,
    onNavigate: (TransitNavigation) -> Unit,
) {
    val viewModel: TransitLinesViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var menuVisible by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val filterScroll = rememberScrollState()
    CollectNavigation(viewModel.navigationEvents, onNavigate)
    rememberTransitMapBinding(
        state = sharedMap,
        owner = TransitLinesKey,
        presentation = TransitMapPresentation(routeColor = EditorialTheme.colors.muniRed),
        topBar = {
            TransitLinesTopBar(
                uiState = state,
                onAction = viewModel::onAction,
                menuVisible = menuVisible,
                onMenuVisibilityChanged = { menuVisible = it },
            )
        },
    )
    TransitLinesPanel(
        uiState = state,
        onAction = viewModel::onAction,
        listState = listState,
        filterScrollState = filterScroll,
    )
}
