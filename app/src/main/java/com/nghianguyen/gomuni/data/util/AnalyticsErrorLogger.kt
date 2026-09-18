package com.nghianguyen.gomuni.data.util

import android.util.Log
import com.nghianguyen.gomuni.domain.util.ErrorLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsErrorLogger @Inject constructor() : ErrorLogger {
    override fun log(message: String, error: Throwable?, metadata: Map<String, String>) {
        // For now, we log to Logcat. In the future, this would send to Firebase Crashlytics/Analytics.
        val tag = "AnalyticsErrorLogger"
        val fullMessage = StringBuilder(message)
        if (metadata.isNotEmpty()) {
            fullMessage.append(" | Metadata: $metadata")
        }
        
        if (error != null) {
            Log.e(tag, fullMessage.toString(), error)
        } else {
            Log.e(tag, fullMessage.toString())
        }
    }
}
