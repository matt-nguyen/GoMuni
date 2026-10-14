package com.nghianguyen.gomuni.features.transit.routes

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nghianguyen.gomuni.app.navigation.CollectNavigation
import com.nghianguyen.gomuni.app.navigation.TransitNavigation
import com.nghianguyen.gomuni.ui.theme.EditorialTheme
import com.nghianguyen.gomuni.features.transit.map.TransitMapData
import com.nghianguyen.gomuni.features.transit.map.TransitMapRoute
import com.nghianguyen.gomuni.features.transit.map.rememberTransitMapViewport
import com.nghianguyen.gomuni.features.transit.navigation.LocalWideLayout

@Composable
internal fun TransitLinesRoute(locationGranted: Boolean, onNavigate: (TransitNavigation) -> Unit) {
    val viewModel: TransitLinesViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var menuVisible by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val filterScroll = rememberScrollState()
    CollectNavigation(viewModel.navigationEvents, onNavigate)
    val mapData = remember { TransitMapData(emptyList(), emptyList()) }
    val viewport = rememberTransitMapViewport(mapData, null, null, false)
    TransitLinesScreen(state, viewModel::onAction, LocalWideLayout.current,
        listState, filterScroll, menuVisible, { menuVisible = it }, map = {
            TransitMapRoute(mapData, null, false, locationGranted, EditorialTheme.colors.muniRed,
                viewport, { viewport.transitionToFollowPuckState() }, {}, it)
        })
}
