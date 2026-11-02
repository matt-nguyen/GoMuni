package com.nghianguyen.gomuni.features.transit.model

import com.nghianguyen.gomuni.domain.model.TransitError

internal const val UNEXPECTED_TRANSIT_FAILURE_MESSAGE =
    "Unable to load transit information. Please try again."

internal fun transitFailureMessage(error: TransitError): String =
    when (error) {
        TransitError.Network -> "Check your internet connection and try again."
        TransitError.NotFound -> "The requested transit information was not found."
        TransitError.Storage -> "Unable to read or save transit information. Please try again."
        TransitError.ServiceUnavailable -> "The transit service is unavailable. Please try again."
    }
