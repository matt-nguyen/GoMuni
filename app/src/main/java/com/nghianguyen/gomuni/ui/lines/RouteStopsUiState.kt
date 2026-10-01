package com.nghianguyen.gomuni.ui.lines

import com.nghianguyen.gomuni.domain.model.TransitStop

data class RouteStopsUiState(
    val route: RouteKey,
    val stops: List<TransitStop> = emptyList(),
    val selectedStopId: String? = null,
    val status: LoadStatus = LoadStatus.NOT_LOADED,
    val isRefreshing: Boolean = false,
    val error: String? = null,
) {
    val selectedStop: TransitStop? get() = stops.find { it.id == selectedStopId }
}
