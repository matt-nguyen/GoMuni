package com.nghianguyen.gomuni.ui.stops

import com.nghianguyen.gomuni.domain.model.TransitStop

data class StopsUiState(
    val stops: List<TransitStop> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
