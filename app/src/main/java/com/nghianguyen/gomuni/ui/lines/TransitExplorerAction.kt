package com.nghianguyen.gomuni.ui.lines

sealed interface TransitExplorerAction {
    data object ClearRoute : TransitExplorerAction
    data object DismissArrivals : TransitExplorerAction
    data object RefreshRoutes : TransitExplorerAction
    data class SelectRoute(val route: RouteKey) : TransitExplorerAction
    data class ChangeSort(val option: TransitLineSortOption) : TransitExplorerAction
    data class SelectStop(val stopId: String) : TransitExplorerAction
    data object RetryStops : TransitExplorerAction
    data object RefreshArrivals : TransitExplorerAction
}
