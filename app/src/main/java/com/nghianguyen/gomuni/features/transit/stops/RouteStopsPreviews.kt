package com.nghianguyen.gomuni.features.transit.stops

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.nghianguyen.gomuni.domain.model.TransitStop
import com.nghianguyen.gomuni.ui.theme.GoMuniTheme
import com.nghianguyen.gomuni.features.transit.model.LoadStatus
import com.nghianguyen.gomuni.features.transit.model.RouteKey

@Preview(widthDp = 900, heightDp = 600)
@Composable
private fun WideStopsRefreshFailurePreview() {
    GoMuniTheme {
        RouteStopsScreen(RouteStopsUiState(RouteKey("SF", "38"),
            stops = listOf(TransitStop("123", "Market", 37.78, -122.41, null, "38")),
            status = LoadStatus.FAILED, error = "Check your internet connection and try again."),
            {}, true, rememberLazyListState(), "123", map = { Box(it) { Text("Map") } })
    }
}
