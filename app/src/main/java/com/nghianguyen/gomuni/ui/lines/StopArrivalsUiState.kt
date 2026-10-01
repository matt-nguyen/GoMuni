package com.nghianguyen.gomuni.ui.lines

import com.nghianguyen.gomuni.domain.model.BusArrival

data class StopArrivalsUiState(
    val stopId: String,
    val predictions: List<BusArrival> = emptyList(),
    val fetchedAtMillis: Long? = null,
    val status: LoadStatus = LoadStatus.NOT_LOADED,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)
