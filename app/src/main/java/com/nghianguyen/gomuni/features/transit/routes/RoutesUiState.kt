package com.nghianguyen.gomuni.features.transit.routes

import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.features.transit.model.LoadStatus

data class RoutesUiState(
    val lines: List<TransitLine> = emptyList(),
    val sortOption: TransitLineSortOption = TransitLineSortOption.ID,
    val status: LoadStatus = LoadStatus.NOT_LOADED,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val filter: String = "All",
)
