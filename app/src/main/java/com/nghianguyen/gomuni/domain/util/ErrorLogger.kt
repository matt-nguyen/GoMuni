package com.nghianguyen.gomuni.domain.util

interface ErrorLogger {
    fun log(message: String, error: Throwable? = null, metadata: Map<String, String> = emptyMap())
}
