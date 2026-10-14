package com.nghianguyen.gomuni.features.transit.arrivals



sealed interface StopArrivalsAction {
    data object Back : StopArrivalsAction
    data object Refresh : StopArrivalsAction
    data class Track(val prediction: ArrivalPredictionKey) : StopArrivalsAction
}
