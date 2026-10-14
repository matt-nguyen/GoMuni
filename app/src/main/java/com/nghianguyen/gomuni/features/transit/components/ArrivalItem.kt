package com.nghianguyen.gomuni.features.transit.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.nghianguyen.gomuni.domain.model.BusArrival
import com.nghianguyen.gomuni.ui.components.EditorialCard
import com.nghianguyen.gomuni.features.transit.components.RouteBadge
import com.nghianguyen.gomuni.ui.theme.EditorialTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

@Composable
fun ArrivalItem(
    arrival: BusArrival,
    routeColor: Color,
    onTrackClick: () -> Unit,
    trackingEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val editorialColors = EditorialTheme.colors
    EditorialCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RouteBadge(
                    routeNumber = arrival.lineRef,
                    backgroundColor = routeColor,
                    contentColor = editorialColors.editorialPaper
                )
                Spacer(modifier = Modifier.width(EditorialTheme.spacing.spaceMd))
                Column {
                    Text(
                        text = arrival.lineName.toTitleCase(),
                        style = EditorialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = editorialColors.editorialPaper
                    )
                    Text(
                        text = "Vehicle: ${arrival.vehicleRef ?: "N/A"}",
                        style = EditorialTheme.typography.labelSmall,
                        color = editorialColors.onSurfaceVariant
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onTrackClick, enabled = trackingEnabled) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Track Bus",
                        tint = editorialColors.editorialPaper
                    )
                }
                Spacer(modifier = Modifier.width(EditorialTheme.spacing.spaceSm))
                Column(horizontalAlignment = Alignment.End) {
                    val timeStr = formatArrivalTime(arrival.expectedArrival ?: arrival.aimedArrival ?: "")
                    Text(
                        text = timeStr,
                        style = EditorialTheme.typography.titleLarge,
                        color = editorialColors.signalChartreuse,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (timeStr.contains(":")) "TIME" else "MIN",
                        style = EditorialTheme.typography.labelSmall,
                        color = editorialColors.signalChartreuse
                    )
                }
            }
        }
    }
}

private fun formatArrivalTime(isoTime: String): String {
    if (isoTime.isEmpty()) return "N/A"
    return try {
        val instant = Instant.parse(isoTime)
        val now = Instant.now()
        val duration = java.time.Duration.between(now, instant)
        val minutes = max(0, duration.toMinutes())
        
        if (minutes in 0..60) {
            java.lang.String.format(java.util.Locale.US, "%02d", minutes)
        } else {
            val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
                .withZone(ZoneId.systemDefault())
            formatter.format(instant)
        }
    } catch (e: Exception) {
        isoTime.split("T").lastOrNull()?.take(5) ?: isoTime
    }
}
