package com.nghianguyen.gomuni.features.transit.routes

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.nghianguyen.gomuni.R
import com.nghianguyen.gomuni.ui.components.*
import com.nghianguyen.gomuni.features.transit.components.*
import com.nghianguyen.gomuni.ui.components.TransitLoadContent
import com.nghianguyen.gomuni.ui.theme.EditorialTheme
import com.nghianguyen.gomuni.features.transit.components.TransitDestinationLayout
import com.nghianguyen.gomuni.features.transit.components.TransitExplorerTopBar
import com.nghianguyen.gomuni.features.transit.components.isLoading

@Composable
fun TransitLinesScreen(
    uiState: RoutesUiState,
    onAction: (TransitLinesAction) -> Unit,
    wide: Boolean,
    listState: LazyListState,
    filterScrollState: ScrollState,
    menuVisible: Boolean,
    onMenuVisibilityChanged: (Boolean) -> Unit,
    map: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier.fillMaxSize(), containerColor = EditorialTheme.colors.editorialBlack,
        topBar = {
            TransitExplorerTopBar(stringResource(R.string.muni), null, false, uiState,
                {}, { onAction(TransitLinesAction.Refresh) }, { onAction(TransitLinesAction.ChangeSort(it)) },
                menuVisible, onMenuVisibilityChanged)
        },
    ) { padding ->
        TransitDestinationLayout(wide, Modifier.padding(padding), map) { panelModifier ->
            TransitLoadContent(uiState.lines.isNotEmpty(), uiState.status.isLoading,
                uiState.isRefreshing, uiState.error, stringResource(R.string.no_routes),
                { onAction(TransitLinesAction.Refresh) }, panelModifier) {
                TransitLineList(uiState.lines, null, { id -> onAction(TransitLinesAction.SelectRoute(id)) },
                    it, uiState.filter, { filter -> onAction(TransitLinesAction.ChangeFilter(filter)) },
                    listState, filterScrollState)
            }
        }
    }
}
