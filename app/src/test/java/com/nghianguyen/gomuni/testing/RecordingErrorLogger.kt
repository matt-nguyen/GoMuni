package com.nghianguyen.gomuni.testing

import com.nghianguyen.gomuni.domain.util.ErrorLogger

internal class RecordingErrorLogger : ErrorLogger {
    val errors = mutableListOf<Throwable?>()

    override fun log(message: String, error: Throwable?, metadata: Map<String, String>) {
        errors += error
    }
}
