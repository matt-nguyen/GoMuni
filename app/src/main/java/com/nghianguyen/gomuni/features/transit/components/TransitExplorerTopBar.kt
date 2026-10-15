package com.nghianguyen.gomuni.features.transit.components

import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.nghianguyen.gomuni.R
import com.nghianguyen.gomuni.features.transit.routes.RoutesUiState
import com.nghianguyen.gomuni.features.transit.routes.TransitLineSortOption
import com.nghianguyen.gomuni.ui.theme.EditorialTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransitExplorerTopBar(
    title: String,
    routeColor: Color?,
    canGoBack: Boolean,
    routesState: RoutesUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSort: (TransitLineSortOption) -> Unit,
    menuVisible: Boolean = false,
    onMenuVisibilityChanged: (Boolean) -> Unit = {},
) {
    val colors = EditorialTheme.colors
    TopAppBar(
        title = {
            Text(
                title,
                style = EditorialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        },
        navigationIcon = {
            if (canGoBack)
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        stringResource(R.string.back),
                        tint = colors.editorialPaper,
                    )
                }
        },
        actions = {
            if (routeColor == null) {
                IconButton(onClick = onRefresh, enabled = !routesState.isRefreshing) {
                    Icon(
                        Icons.Default.Refresh,
                        stringResource(R.string.refresh_routes),
                        tint = colors.editorialPaper,
                    )
                }
                IconButton(onClick = { onMenuVisibilityChanged(true) }) {
                    Icon(
                        Icons.Default.MoreVert,
                        stringResource(R.string.sort_options),
                        tint = colors.editorialPaper,
                    )
                }
                RouteSortMenu(routesState.sortOption, menuVisible, onMenuVisibilityChanged, onSort)
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = routeColor ?: colors.surfaceContainerLowest.copy(alpha = 0.9f),
                titleContentColor = colors.editorialPaper,
            ),
    )
}

@Composable
private fun RouteSortMenu(
    sortOption: TransitLineSortOption,
    menuVisible: Boolean,
    onMenuVisibilityChanged: (Boolean) -> Unit,
    onSort: (TransitLineSortOption) -> Unit,
) {
    val colors = EditorialTheme.colors
    DropdownMenu(
        menuVisible,
        { onMenuVisibilityChanged(false) },
        modifier = Modifier.background(colors.surfaceCard),
    ) {
        TransitLineSortOption.entries.forEach { option ->
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (option == TransitLineSortOption.ID) R.string.sort_id
                            else R.string.sort_name,
                        ),
                    )
                },
                onClick = {
                    onSort(option)
                    onMenuVisibilityChanged(false)
                },
                trailingIcon = {
                    if (sortOption == option)
                        RadioButton(
                            true,
                            null,
                            colors =
                                RadioButtonDefaults.colors(
                                    selectedColor = colors.signalChartreuse,
                                ),
                        )
                },
            )
        }
    }
}
