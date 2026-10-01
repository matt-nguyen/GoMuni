package com.nghianguyen.gomuni.ui.lines

data class TransitExplorerUiState(
    val routesState: RoutesUiState = RoutesUiState(),
    val routeStopsState: RouteStopsUiState? = null,
    val arrivalsState: StopArrivalsUiState? = null,
    val arrivalSheetVisible: Boolean = false,
)
