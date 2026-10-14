package com.nghianguyen.gomuni.features.tracking

import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.nghianguyen.gomuni.R
import com.nghianguyen.gomuni.ui.theme.EditorialTheme

@Composable
fun TrackingStatusBanner(state: TrackingUiState, onDismiss: (String) -> Unit) {
    if (state == TrackingUiState.Idle) return
    Column(Modifier.fillMaxWidth().padding(EditorialTheme.spacing.spaceMd)) {
        if (state.isPending) {
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = EditorialTheme.colors.signalChartreuse)
            Text(stringResource(R.string.tracking_starting), color = EditorialTheme.colors.editorialPaper)
        } else if (state is TrackingUiState.Failed) {
            Text(stringResource(when (state.reason) {
                TrackingFailure.PERMISSION_DENIED -> R.string.tracking_permission_denied
                TrackingFailure.STARTUP_FAILED -> R.string.tracking_start_failed
            }), color = EditorialTheme.colors.electricAlert)
            TextButton(onClick = { onDismiss(state.requestId) }) { Text(stringResource(R.string.dismiss)) }
        }
    }
}
