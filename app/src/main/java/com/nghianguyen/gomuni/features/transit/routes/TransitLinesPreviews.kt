package com.nghianguyen.gomuni.features.transit.routes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.features.transit.model.LoadStatus
import com.nghianguyen.gomuni.ui.theme.GoMuniTheme

@Preview(widthDp = 400, heightDp = 800)
@Preview(widthDp = 900, heightDp = 600)
@Composable
private fun RoutesPreview() {
    GoMuniTheme {
        TransitLinesScreen(
            RoutesUiState(
                lines = listOf(TransitLine("38", "Geary", "bus")),
                status = LoadStatus.LOADED,
            ),
            {},
            false,
            rememberLazyListState(),
            rememberScrollState(),
            false,
            {},
            map = { Box(it) { Text("Map") } },
        )
    }
}
