package com.nghianguyen.gomuni.features.transit.stops

sealed interface RouteStopsAction {
    data object Back : RouteStopsAction

    data object RetryLoad : RouteStopsAction

    /** External entry can open before stops have loaded; missing stops render a sheet error. */
    data class OpenArrivals(val stopId: String) : RouteStopsAction

    /** Correlated so a late dismissal cannot close a replacement sheet. */
    data class DismissArrivals(val stopId: String) : RouteStopsAction

    data class SelectStop(val stopId: String) : RouteStopsAction
}
