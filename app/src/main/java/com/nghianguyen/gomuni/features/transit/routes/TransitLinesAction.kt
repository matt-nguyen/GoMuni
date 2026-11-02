package com.nghianguyen.gomuni.features.transit.routes

sealed interface TransitLinesAction {
    data object RetryFavorites : TransitLinesAction

    data class SetFavorite(val lineId: String, val favorite: Boolean) : TransitLinesAction

    data object RetryLoad : TransitLinesAction

    data class ChangeSort(val option: TransitLineSortOption) : TransitLinesAction

    data class ChangeFilter(val filter: String) : TransitLinesAction

    data class SelectRoute(val lineId: String) : TransitLinesAction
}
