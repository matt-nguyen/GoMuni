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

/**
 * Shows startup progress or a dismissible failure; renders nothing when idle.
 *
 * @param state Startup progress and error information to display.
 * @param onDismiss Called when the user dismisses the failure banner.
 */
@Composable
fun TrackingStatusBanner(state: TrackingUiState, onDismiss: () -> Unit) {
    val error = state.error
    if (!state.isStarting && error == null) return
    Column(Modifier.fillMaxWidth().padding(EditorialTheme.spacing.spaceMd)) {
        if (state.isStarting) {
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = EditorialTheme.colors.signalChartreuse)
            Text(stringResource(R.string.tracking_starting), color = EditorialTheme.colors.editorialPaper)
        } else if (error != null) {
            Text(stringResource(when (error.reason) {
                TrackingFailure.PERMISSION_DENIED -> R.string.tracking_permission_denied
                TrackingFailure.STARTUP_FAILED -> R.string.tracking_start_failed
            }), color = EditorialTheme.colors.electricAlert)
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dismiss)) }
        }
    }
}
