package com.nghianguyen.gomuni.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object TransitLinesKey : NavKey

@Serializable
data class TransitStopsKey(val lineId: String, val operatorId: String = "SF") : NavKey

@Serializable
data class StopArrivalsKey(val lineId: String, val stopId: String, val operatorId: String = "SF") : NavKey
