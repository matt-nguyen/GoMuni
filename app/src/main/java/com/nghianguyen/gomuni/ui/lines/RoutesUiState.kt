package com.nghianguyen.gomuni.ui.lines

import com.nghianguyen.gomuni.domain.model.TransitLine

data class RoutesUiState(
    val lines: List<TransitLine> = emptyList(),
    val sortOption: TransitLineSortOption = TransitLineSortOption.ID,
    val status: LoadStatus = LoadStatus.NOT_LOADED,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)
