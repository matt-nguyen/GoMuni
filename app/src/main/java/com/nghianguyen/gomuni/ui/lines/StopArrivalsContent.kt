package com.nghianguyen.gomuni.ui.lines

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nghianguyen.gomuni.domain.model.BusArrival
import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.domain.model.TransitStop
import com.nghianguyen.gomuni.ui.components.EditorialDivider
import com.nghianguyen.gomuni.ui.theme.EditorialColors
import com.nghianguyen.gomuni.ui.theme.EditorialTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun StopArrivalsContent(
    stop: TransitStop,
    selectedRouteArrivals: List<BusArrival>,
    otherRouteArrivals: List<BusArrival>,
    isLoading: Boolean,
    lines: List<TransitLine>,
    arrivalsLastUpdatedMillis: Long?,
    onRefresh: () -> Unit,
    onTrackArrival: (BusArrival) -> Unit
) {
    val editorialColors = EditorialTheme.colors
    var lastUpdatedText by remember { mutableStateOf("") }

    LaunchedEffect(arrivalsLastUpdatedMillis) {
        while (isActive) {
            if (arrivalsLastUpdatedMillis == null) {
                lastUpdatedText = ""
            } else {
                val elapsedMinutes = (System.currentTimeMillis() - arrivalsLastUpdatedMillis) / 60000
                lastUpdatedText = if (elapsedMinutes < 1) {
                    "Updated just now"
                } else {
                    "Updated $elapsedMinutes min ago"
                }
            }
            delay(60000L)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(EditorialTheme.spacing.spaceMd)
            .padding(bottom = EditorialTheme.spacing.spaceXl)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stop.name.toTitleCase(),
                    style = EditorialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = editorialColors.editorialPaper
                )
                Text(
                    text = "Stop ID: ${stop.id}",
                    style = EditorialTheme.typography.labelSmall,
                    color = editorialColors.onSurfaceVariant
                )
                if (lastUpdatedText.isNotEmpty()) {
                    Text(
                        text = lastUpdatedText,
                        style = EditorialTheme.typography.labelSmall,
                        color = editorialColors.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onRefresh, enabled = !isLoading) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = editorialColors.signalChartreuse,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = editorialColors.editorialPaper
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(EditorialTheme.spacing.spaceMd))
        EditorialDivider()
        
        if (selectedRouteArrivals.isEmpty() && otherRouteArrivals.isEmpty() && !isLoading) {
            Text(
                text = "NO UPCOMING ARRIVALS FOUND.",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 32.dp),
                style = EditorialTheme.typography.bodyLarge,
                color = editorialColors.onSurfaceVariant
            )
        } else {
            LazyColumn {
                item { EditorialDivider() }
                items(selectedRouteArrivals) { arrival ->
                    val line = lines.find { it.id == arrival.lineRef }
                    val routeColor = line?.let { getRouteColor(it, editorialColors) } ?: editorialColors.routeBus
                    ArrivalItem(
                        arrival = arrival,
                        routeColor = routeColor,
                        onTrackClick = { onTrackArrival(arrival) }
                    )
                }
                items(otherRouteArrivals) { arrival ->
                    val line = lines.find { it.id == arrival.lineRef }
                    val routeColor = line?.let { getRouteColor(it, editorialColors) } ?: editorialColors.routeBus
                    ArrivalItem(
                        arrival = arrival,
                        routeColor = routeColor,
                        onTrackClick = { onTrackArrival(arrival) },
                        modifier = Modifier.alpha(0.6f)
                    )
                }
            }
        }
    }
}
