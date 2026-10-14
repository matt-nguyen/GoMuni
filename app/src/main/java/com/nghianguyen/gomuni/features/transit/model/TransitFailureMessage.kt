package com.nghianguyen.gomuni.features.transit.model

import com.nghianguyen.gomuni.domain.model.TransitError

internal fun transitFailureMessage(error: Throwable): String = when (error) {
    TransitError.Network -> "Check your internet connection and try again."
    TransitError.NotFound -> "The requested transit information was not found."
    else -> "Unable to load transit information. Please try again."
}
