package com.nghianguyen.gomuni.ui.stops

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.nghianguyen.gomuni.domain.model.TransitStop
import com.nghianguyen.gomuni.ui.components.EditorialCard
import com.nghianguyen.gomuni.ui.components.EditorialDivider
import com.nghianguyen.gomuni.ui.theme.EditorialTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopsScreen(
    lineId: String,
    uiState: StopsUiState,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(lineId) {
        onRefresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "STOPS FOR $lineId",
                        style = EditorialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = EditorialTheme.colors.editorialPaper
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EditorialTheme.colors.editorialBlack,
                    titleContentColor = EditorialTheme.colors.editorialPaper
                )
            )
        },
        containerColor = EditorialTheme.colors.editorialBlack,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (uiState.errorMessage != null && uiState.stops.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val colors = EditorialTheme.colors
                    val spacing = EditorialTheme.spacing
                    Text(
                        text = "ERROR: ${uiState.errorMessage.uppercase()}",
                        style = EditorialTheme.typography.bodyLarge,
                        color = colors.electricAlert,
                        modifier = Modifier.padding(spacing.spaceMd)
                    )
                }
            } else {
                StopList(
                    stops = uiState.stops,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun StopList(
    stops: List<TransitStop>,
    modifier: Modifier = Modifier
) {
    if (stops.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "NO STOPS FOUND FOR THIS LINE.",
                style = EditorialTheme.typography.bodyLarge,
                color = EditorialTheme.colors.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(modifier = modifier.fillMaxSize()) {
            item { EditorialDivider() }
            items(stops, key = { it.id }) { stop ->
                EditorialCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = stop.name.uppercase(),
                            style = EditorialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = EditorialTheme.colors.editorialPaper
                        )
                        Spacer(modifier = Modifier.height(EditorialTheme.spacing.space2xs))
                        Text(
                            text = "STOP ID: ${stop.id}",
                            style = EditorialTheme.typography.labelSmall,
                            color = EditorialTheme.colors.onSurfaceVariant
                        )
                        Text(
                            text = "TYPE: ${stop.stopType?.uppercase() ?: "N/A"}",
                            style = EditorialTheme.typography.labelSmall,
                            color = EditorialTheme.colors.onSurfaceVariant
                        )
                        Text(
                            text = "LAT: ${stop.latitude}, LON: ${stop.longitude}",
                            style = EditorialTheme.typography.labelSmall,
                            color = EditorialTheme.colors.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
