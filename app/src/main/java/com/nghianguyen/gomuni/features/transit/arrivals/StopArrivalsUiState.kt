package com.nghianguyen.gomuni.features.transit.arrivals

import com.nghianguyen.gomuni.domain.model.BusArrival
import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.domain.model.TransitStop
import com.nghianguyen.gomuni.features.transit.model.LoadStatus

data class StopArrivalsUiState(
    val stopId: String,
    val predictions: List<BusArrival> = emptyList(),
    val fetchedAtMillis: Long? = null,
    val status: LoadStatus = LoadStatus.NOT_LOADED,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val stop: TransitStop? = null,
    val lines: List<TransitLine> = emptyList(),
)
