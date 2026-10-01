package com.nghianguyen.gomuni.ui.lines

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nghianguyen.gomuni.R
import com.nghianguyen.gomuni.domain.model.BusArrival
import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.domain.model.TransitStop
import com.nghianguyen.gomuni.ui.theme.EditorialTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopArrivalsSheet(
    routeState: RouteStopsUiState,
    arrivalsState: StopArrivalsUiState?,
    lines: List<TransitLine>,
    embedded: Boolean,
    onDismiss: () -> Unit,
    onRefresh: (String) -> Unit,
    onTrack: (BusArrival, TransitStop) -> Unit,
    modifier: Modifier = Modifier,
) {
    val stop = routeState.selectedStop ?: return
    val arrivals = arrivalsState?.takeIf { it.stopId == stop.id } ?: StopArrivalsUiState(stop.id)
    val selected = arrivals.predictions.filter { it.lineRef == routeState.route.lineId }
    val others = arrivals.predictions.filter { it.lineRef != routeState.route.lineId }
    val content: @Composable () -> Unit = {
        Column(modifier) {
            if (embedded) TextButton(onClick = onDismiss) { Text(stringResource(R.string.close_arrivals)) }
            arrivals.error?.let {
                Text(it, color = EditorialTheme.colors.electricAlert, modifier = Modifier.padding(EditorialTheme.spacing.spaceMd))
            }
            StopArrivalsContent(
                stop, selected, others,
                isLoading = arrivals.isRefreshing || arrivals.status == LoadStatus.LOADING || arrivals.status == LoadStatus.NOT_LOADED,
                lines = lines,
                arrivalsLastUpdatedMillis = arrivals.fetchedAtMillis,
                onRefresh = { onRefresh(stop.id) },
                onTrackArrival = { onTrack(it, stop) },
                showEmpty = arrivals.status == LoadStatus.LOADED,
            )
        }
    }
    if (embedded) content() else ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RectangleShape,
        containerColor = EditorialTheme.colors.surfaceCard,
        dragHandle = { BottomSheetDefaults.DragHandle(color = EditorialTheme.colors.editorialPaper) },
    ) {
        Box(Modifier.heightIn(max = 600.dp)) { content() }
    }
}
