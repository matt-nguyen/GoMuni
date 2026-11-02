package com.nghianguyen.gomuni.domain.model

/** An operation's value or expected domain failure. Unexpected failures remain exceptions. */
sealed interface Outcome<out T, out E> {
    data class Success<T>(val value: T) : Outcome<T, Nothing>

    data class Failure<E>(val error: E) : Outcome<Nothing, E>
}
