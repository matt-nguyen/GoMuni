package com.nghianguyen.gomuni.features.transit.routes

sealed interface TransitLinesAction {
    data object Refresh : TransitLinesAction

    data class ChangeSort(val option: TransitLineSortOption) : TransitLinesAction

    data class ChangeFilter(val filter: String) : TransitLinesAction

    data class SelectRoute(val lineId: String) : TransitLinesAction
}
