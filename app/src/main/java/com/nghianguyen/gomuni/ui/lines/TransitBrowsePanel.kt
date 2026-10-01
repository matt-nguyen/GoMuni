package com.nghianguyen.gomuni.ui.lines

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.nghianguyen.gomuni.R
import com.nghianguyen.gomuni.domain.model.TransitStop
import com.nghianguyen.gomuni.ui.theme.EditorialTheme

@Composable
fun TransitBrowsePanel(
    routesState: RoutesUiState,
    routeStopsState: RouteStopsUiState?,
    onLineSelected: (String) -> Unit,
    onStopSelected: (TransitStop) -> Unit,
    onRefreshRoutes: () -> Unit,
    onRetryStops: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasData = if (routeStopsState != null) routeStopsState.stops.isNotEmpty() else routesState.lines.isNotEmpty()
    val status = routeStopsState?.status ?: routesState.status
    val refreshing = routeStopsState?.isRefreshing ?: routesState.isRefreshing
    val error = if (routeStopsState != null) routeStopsState.error else routesState.error
    val retry = if (routeStopsState != null) onRetryStops else onRefreshRoutes
    Column(modifier) {
        if (hasData && refreshing) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = EditorialTheme.colors.signalChartreuse)
        if (error != null) {
            Text(error, color = EditorialTheme.colors.electricAlert, modifier = Modifier.padding(EditorialTheme.spacing.spaceMd))
            TextButton(onClick = retry, enabled = !refreshing) { Text(stringResource(R.string.retry)) }
        }
        when {
            hasData && routeStopsState != null -> TransitStopList(routeStopsState.stops, routeStopsState.selectedStopId, onStopSelected, Modifier.weight(1f))
            hasData -> TransitLineList(routesState.lines, null, onLineSelected, Modifier.weight(1f))
            status == LoadStatus.LOADING || status == LoadStatus.NOT_LOADED -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EditorialTheme.colors.signalChartreuse)
            }
            status == LoadStatus.FAILED || error != null -> Unit
            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(if (routeStopsState == null) R.string.no_routes else R.string.no_stops),
                    color = EditorialTheme.colors.onSurfaceVariant, modifier = Modifier.padding(EditorialTheme.spacing.spaceMd))
            }
        }
    }
}
