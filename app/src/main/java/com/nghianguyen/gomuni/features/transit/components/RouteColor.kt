package com.nghianguyen.gomuni.features.transit.components

import androidx.compose.ui.graphics.Color
import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.ui.theme.EditorialColors

fun getRouteColor(line: TransitLine, colors: EditorialColors): Color {
    val id = line.id.uppercase()
    return when {
        id.endsWith("R") -> colors.routeRapid
        id.endsWith("X") || id.endsWith("AX") || id.endsWith("BX") -> colors.routeExpress
        id.endsWith("OWL") -> colors.routeLateNight
        id.all { it.isLetter() } -> colors.routeLightRail
        id.all { it.isDigit() } -> colors.routeBus
        else -> colors.routeBus
    }
}
