package com.nghianguyen.gomuni.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.nghianguyen.gomuni.R
import com.nghianguyen.gomuni.ui.theme.EditorialTheme

/** Operation chrome shared by destination screens, without choosing a destination. */
@Composable
fun TransitLoadContent(
    hasData: Boolean,
    loading: Boolean,
    refreshing: Boolean,
    error: String?,
    emptyText: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    Column(modifier) {
        if (hasData && refreshing)
            LinearProgressIndicator(
                Modifier.fillMaxWidth(),
                color = EditorialTheme.colors.signalChartreuse,
            )
        if (error != null) {
            Text(
                error,
                color = EditorialTheme.colors.electricAlert,
                modifier = Modifier.padding(EditorialTheme.spacing.spaceMd),
            )
            TextButton(onClick = onRetry, enabled = !refreshing) {
                Text(stringResource(R.string.retry))
            }
        }
        when {
            hasData -> content(Modifier.weight(1f))
            loading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = EditorialTheme.colors.signalChartreuse)
                }
            error == null ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(emptyText, color = EditorialTheme.colors.onSurfaceVariant)
                }
        }
    }
}
