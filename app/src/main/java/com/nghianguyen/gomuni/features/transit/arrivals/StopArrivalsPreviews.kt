package com.nghianguyen.gomuni.features.transit.arrivals

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.nghianguyen.gomuni.domain.model.TransitStop
import com.nghianguyen.gomuni.features.transit.model.LoadStatus
import com.nghianguyen.gomuni.ui.theme.GoMuniTheme

@Preview(widthDp = 400, heightDp = 600)
@Composable
private fun EmptyArrivalsPreview() {
    GoMuniTheme {
        StopArrivalsScreen(
            StopArrivalsUiState(
                "123",
                status = LoadStatus.LOADED,
                stop = TransitStop("123", "Market", 37.78, -122.41, null, "38"),
            ),
            "38",
            {},
            rememberLazyListState(),
            0,
            true,
        )
    }
}
