package com.nghianguyen.gomuni.domain.model

data class TransitStop(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val stopType: String?,
    val lineId: String,
)
