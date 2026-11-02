package com.nghianguyen.gomuni.domain.model

sealed interface TransitError {
    data object Network : TransitError

    data object NotFound : TransitError

    data object Storage : TransitError

    data object ServiceUnavailable : TransitError
}
