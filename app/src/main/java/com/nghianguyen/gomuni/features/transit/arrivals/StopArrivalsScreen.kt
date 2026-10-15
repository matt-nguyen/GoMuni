package com.nghianguyen.gomuni.features.transit.arrivals

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.nghianguyen.gomuni.R
import com.nghianguyen.gomuni.features.transit.components.StopArrivalsContent
import com.nghianguyen.gomuni.features.transit.components.isLoading
import com.nghianguyen.gomuni.features.transit.model.LoadStatus
import com.nghianguyen.gomuni.ui.theme.EditorialTheme

@Composable
fun StopArrivalsScreen(
    uiState: StopArrivalsUiState,
    lineId: String,
    onAction: (StopArrivalsAction) -> Unit,
    listState: LazyListState,
    elapsedMinutes: Long,
    trackingEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        TextButton(onClick = { onAction(StopArrivalsAction.Dismiss) }) {
            Text(stringResource(R.string.close_arrivals))
        }
        val stop = uiState.stop
        if (stop == null) {
            if (uiState.error != null)
                Text(
                    uiState.error,
                    color = EditorialTheme.colors.electricAlert,
                    modifier = Modifier.padding(EditorialTheme.spacing.spaceMd),
                )
            else CircularProgressIndicator(color = EditorialTheme.colors.signalChartreuse)
            if (uiState.error != null)
                TextButton(onClick = { onAction(StopArrivalsAction.Refresh) }) {
                    Text(stringResource(R.string.retry))
                }
        } else {
            uiState.error?.let {
                Text(
                    it,
                    color = EditorialTheme.colors.electricAlert,
                    modifier = Modifier.padding(EditorialTheme.spacing.spaceMd),
                )
            }
            StopArrivalsContent(
                stop,
                uiState.predictions.filter { it.lineRef == lineId },
                uiState.predictions.filter { it.lineRef != lineId },
                uiState.isRefreshing || uiState.status.isLoading,
                uiState.lines,
                uiState.fetchedAtMillis,
                { onAction(StopArrivalsAction.Refresh) },
                { onAction(StopArrivalsAction.Track(ArrivalPredictionKey.from(it))) },
                uiState.status == LoadStatus.LOADED,
                trackingEnabled,
                elapsedMinutes,
                listState,
            )
        }
    }
}
