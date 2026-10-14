package com.nghianguyen.gomuni.features.transit.components

import java.util.Locale

internal fun String.toTitleCase(): String {
    return this.lowercase().split(" ").joinToString(" ") {
        it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(Locale.US) else char.toString() }
    }
}
