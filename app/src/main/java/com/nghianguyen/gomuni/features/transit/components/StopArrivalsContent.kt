package com.nghianguyen.gomuni.features.transit.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nghianguyen.gomuni.R
import com.nghianguyen.gomuni.domain.model.BusArrival
import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.domain.model.TransitStop
import com.nghianguyen.gomuni.ui.components.EditorialDivider
import com.nghianguyen.gomuni.ui.theme.EditorialTheme

@Composable
fun StopArrivalsContent(
    stop: TransitStop,
    selectedRouteArrivals: List<BusArrival>,
    otherRouteArrivals: List<BusArrival>,
    isLoading: Boolean,
    lines: List<TransitLine>,
    arrivalsLastUpdatedMillis: Long?,
    onRefresh: () -> Unit,
    onTrackArrival: (BusArrival) -> Unit,
    showEmpty: Boolean = true,
    trackingEnabled: Boolean = true,
    elapsedMinutes: Long = 0,
    listState: androidx.compose.foundation.lazy.LazyListState,
) {
    val editorialColors = EditorialTheme.colors
    val noArrivals = selectedRouteArrivals.isEmpty() && otherRouteArrivals.isEmpty()
    val lastUpdatedText =
        when {
            arrivalsLastUpdatedMillis == null -> ""
            elapsedMinutes < 1 -> stringResource(R.string.updated_now)
            else -> stringResource(R.string.updated_minutes, elapsedMinutes)
        }

    Column(
        modifier =
            Modifier.fillMaxWidth()
                .padding(EditorialTheme.spacing.spaceMd)
                .padding(bottom = EditorialTheme.spacing.spaceXl),
    ) {
        StopArrivalsHeader(stop, lastUpdatedText, isLoading, onRefresh)

        Spacer(modifier = Modifier.height(EditorialTheme.spacing.spaceMd))
        EditorialDivider()

        if (noArrivals && !isLoading && showEmpty) {
            Text(
                text = stringResource(R.string.no_arrivals),
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 32.dp),
                style = EditorialTheme.typography.bodyLarge,
                color = editorialColors.onSurfaceVariant,
            )
        } else {
            LazyColumn(state = listState) {
                item { EditorialDivider() }
                items(selectedRouteArrivals) { arrival ->
                    val line = lines.find { it.id == arrival.lineRef }
                    val routeColor =
                        line?.let { getRouteColor(it, editorialColors) } ?: editorialColors.routeBus
                    ArrivalItem(
                        arrival = arrival,
                        routeColor = routeColor,
                        trackingEnabled = trackingEnabled,
                        onTrackClick = { onTrackArrival(arrival) },
                    )
                }
                items(otherRouteArrivals) { arrival ->
                    val line = lines.find { it.id == arrival.lineRef }
                    val routeColor =
                        line?.let { getRouteColor(it, editorialColors) } ?: editorialColors.routeBus
                    ArrivalItem(
                        arrival = arrival,
                        routeColor = routeColor,
                        trackingEnabled = trackingEnabled,
                        onTrackClick = { onTrackArrival(arrival) },
                        modifier = Modifier.alpha(OTHER_ROUTE_ALPHA),
                    )
                }
            }
        }
    }
}

private const val OTHER_ROUTE_ALPHA = 0.6f

@Composable
private fun StopArrivalsHeader(
    stop: TransitStop,
    lastUpdatedText: String,
    isLoading: Boolean,
    onRefresh: () -> Unit,
) {
    val editorialColors = EditorialTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stop.name.toTitleCase(),
                style = EditorialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = editorialColors.editorialPaper,
            )
            Text(
                text = stringResource(R.string.stop_id, stop.id),
                style = EditorialTheme.typography.labelSmall,
                color = editorialColors.onSurfaceVariant,
            )
            if (lastUpdatedText.isNotEmpty()) {
                Text(
                    text = lastUpdatedText,
                    style = EditorialTheme.typography.labelSmall,
                    color = editorialColors.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onRefresh, enabled = !isLoading) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = editorialColors.signalChartreuse,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.refresh_arrivals),
                    tint = editorialColors.editorialPaper,
                )
            }
        }
    }
}
