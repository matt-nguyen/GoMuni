package com.nghianguyen.gomuni.features.transit.stops

import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.domain.model.TransitStop
import com.nghianguyen.gomuni.features.transit.model.LoadStatus
import com.nghianguyen.gomuni.features.transit.model.RouteKey

data class RouteStopsUiState(
    val route: RouteKey,
    val stops: List<TransitStop> = emptyList(),
    val selectedStopId: String? = null,
    val arrivalsStopId: String? = null,
    val status: LoadStatus = LoadStatus.NOT_LOADED,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val lines: List<TransitLine> = emptyList(),
) {
    val selectedStop: TransitStop? get() = stops.find { it.id == selectedStopId }
}
