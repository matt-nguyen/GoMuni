package com.nghianguyen.gomuni.features.transit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.ui.components.EditorialDivider
import com.nghianguyen.gomuni.ui.theme.EditorialTheme

@Composable
fun TransitLineList(
    lines: List<TransitLine>,
    selectedLineId: String?,
    onLineClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    activeFilter: String = "All",
    onFilterSelected: (String) -> Unit = {},
    listState: androidx.compose.foundation.lazy.LazyListState,
    filterScrollState: androidx.compose.foundation.ScrollState,
) {

    val filteredLines =
        remember(lines, activeFilter) {
            if (activeFilter == "All") lines
            else lines.filter { matchesRouteFilter(it, activeFilter) }
        }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
    ) {
        item {
            RouteFilters(activeFilter, onFilterSelected, filterScrollState)
            EditorialDivider()
        }

        item {
            RouteListHeading()
            EditorialDivider()
        }

        items(filteredLines, key = { it.id }) { line ->
            val isSelected = line.id == selectedLineId

            TransitLineRow(line, isSelected, onLineClick)
            EditorialDivider()
        }
    }
}

@Composable
private fun RouteFilters(
    activeFilter: String,
    onFilterSelected: (String) -> Unit,
    filterScrollState: androidx.compose.foundation.ScrollState,
) {
    val editorialColors = EditorialTheme.colors
    val filters = listOf("All", "Local", "Rapid", "Express", "Owl")
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .background(editorialColors.surfaceContainerLow)
                .padding(
                    horizontal = EditorialTheme.spacing.gutterMobile,
                    vertical = EditorialTheme.spacing.spaceXs,
                )
                .horizontalScroll(filterScrollState),
        horizontalArrangement = Arrangement.spacedBy(EditorialTheme.spacing.space2xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        filters.forEach { filter ->
            val isActive = filter == activeFilter
            Box(
                modifier =
                    Modifier.background(
                            if (isActive) editorialColors.editorialPaper
                            else editorialColors.surfaceCard,
                        )
                        .clickable { onFilterSelected(filter) }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(
                    text = filter.uppercase(),
                    style = EditorialTheme.typography.labelLarge,
                    color =
                        if (isActive) editorialColors.editorialBlack else editorialColors.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun RouteListHeading() {
    val editorialColors = EditorialTheme.colors
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
            text = "Muni routes",
            style = EditorialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = editorialColors.editorialPaper,
        )
    }
}

@Composable
private fun TransitLineRow(line: TransitLine, isSelected: Boolean, onLineClick: (String) -> Unit) {
    val editorialColors = EditorialTheme.colors
    val routeColor = getRouteColor(line, editorialColors)
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .background(
                    if (isSelected) editorialColors.surfaceCard
                    else MaterialTheme.colorScheme.surface,
                )
                .clickable { onLineClick(line.id) }
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
            RouteLabel(line, routeColor, Modifier.weight(1f, fill = false))

            Box(
                modifier =
                    Modifier.background(editorialColors.surfaceContainerHigh)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = line.transportMode.toTitleCase(),
                    style = EditorialTheme.typography.labelSmall,
                    color = editorialColors.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun RouteLabel(line: TransitLine, routeColor: Color, modifier: Modifier) {
    val editorialColors = EditorialTheme.colors
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(EditorialTheme.spacing.spaceSm),
    ) {
        RouteBadge(
            routeNumber = line.id,
            backgroundColor = routeColor,
            contentColor = editorialColors.editorialPaper,
        )
        Text(
            text = line.name.toTitleCase(),
            style =
                EditorialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
            color = editorialColors.editorialPaper,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal fun matchesRouteFilter(line: TransitLine, filter: String): Boolean {
    val id = line.id.uppercase()
    return when (filter) {
        "Local" -> id.all { it.isDigit() } || (id.all { it.isLetter() } && !id.endsWith("OWL"))
        "Rapid" -> id.endsWith("R")
        "Express" -> id.endsWith("X") || id.endsWith("AX") || id.endsWith("BX")
        "Owl" -> id.endsWith("OWL")
        else -> true
    }
}
