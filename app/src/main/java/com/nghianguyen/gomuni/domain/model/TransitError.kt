package com.nghianguyen.gomuni.domain.model

sealed class TransitError : Exception() {
    data object Network : TransitError()
    data object NotFound : TransitError()
    data object Unexpected : TransitError()
}
