package com.nghianguyen.gomuni.features.transit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nghianguyen.gomuni.domain.model.TransitStop
import com.nghianguyen.gomuni.ui.components.EditorialDivider
import com.nghianguyen.gomuni.ui.theme.EditorialTheme

@Composable
fun TransitStopList(
    stops: List<TransitStop>,
    selectedStopId: String?,
    onStopClick: (TransitStop) -> Unit,
    modifier: Modifier = Modifier,
    listState: androidx.compose.foundation.lazy.LazyListState,
) {
    val editorialColors = EditorialTheme.colors
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
    ) {
        item {
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(
                            horizontal = EditorialTheme.spacing.gutterMobile,
                            vertical = EditorialTheme.spacing.spaceSm,
                        ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Stops",
                    style =
                        EditorialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = editorialColors.editorialPaper,
                )
            }
            EditorialDivider()
        }
        items(stops, key = { it.id }) { stop ->
            val isSelected = stop.id == selectedStopId
            TransitStopRow(stop, isSelected, onStopClick)
            EditorialDivider()
        }
    }
}

@Composable
private fun TransitStopRow(
    stop: TransitStop,
    isSelected: Boolean,
    onStopClick: (TransitStop) -> Unit,
) {
    val editorialColors = EditorialTheme.colors
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .background(
                    if (isSelected) editorialColors.surfaceCard
                    else MaterialTheme.colorScheme.surface,
                )
                .clickable { onStopClick(stop) }
                .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier.fillMaxHeight()
                    .width(4.dp)
                    .background(
                        if (isSelected) editorialColors.electricAlert else Color.Transparent,
                    ),
        )
        Row(
            modifier =
                Modifier.weight(1f)
                    .padding(
                        horizontal = EditorialTheme.spacing.gutterMobile,
                        vertical = 14.dp,
                    ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StopLabel(stop, Modifier.weight(1f))

            if (stop.stopType != null) {
                Box(
                    modifier =
                        Modifier.background(editorialColors.surfaceContainerHigh)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = stop.stopType.toTitleCase(),
                        style = EditorialTheme.typography.labelSmall,
                        color = editorialColors.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun StopLabel(stop: TransitStop, modifier: Modifier) {
    val editorialColors = EditorialTheme.colors
    Column(modifier = modifier) {
        Text(
            text = stop.name.toTitleCase(),
            style =
                EditorialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
            color = editorialColors.editorialPaper,
        )
        Text(
            text = "Stop ID: ${stop.id}",
            style = EditorialTheme.typography.labelSmall,
            color = editorialColors.onSurfaceVariant,
        )
    }
}
