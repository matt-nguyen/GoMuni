package com.nghianguyen.gomuni.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object TransitLinesKey : NavKey

@Serializable
data class TransitStopsKey(val lineId: String, val operatorId: String = "SF") : NavKey
