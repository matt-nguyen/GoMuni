package com.nghianguyen.gomuni.features.transit.stops



sealed interface RouteStopsAction {
    data object Back : RouteStopsAction
    data object Refresh : RouteStopsAction
    data class SelectStop(val stopId: String) : RouteStopsAction
}
