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
    RoutesPreviewContent(setOf("38"))
}

@Preview(widthDp = 400, heightDp = 800)
@Composable
private fun NoFavoritesPreview() {
    RoutesPreviewContent(emptySet())
}

@Preview(widthDp = 400, heightDp = 800)
@Composable
private fun AllFavoritesPreview() {
    RoutesPreviewContent(setOf("1", "38"))
}

@Composable
private fun RoutesPreviewContent(favoriteIds: Set<String>) {
    GoMuniTheme {
        TransitLinesScreen(
            RoutesUiState(
                lines =
                    listOf(
                        TransitLine("1", "California", "bus"),
                        TransitLine("38", "Geary", "bus"),
                    ),
                favoriteIds = favoriteIds,
                favoritesReady = true,
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
