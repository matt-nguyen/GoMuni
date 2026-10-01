package com.nghianguyen.gomuni.ui.lines

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nghianguyen.gomuni.R
import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.ui.components.EditorialDivider
import com.nghianguyen.gomuni.ui.components.RouteBadge
import com.nghianguyen.gomuni.ui.theme.EditorialColors
import com.nghianguyen.gomuni.ui.theme.EditorialTheme
import kotlinx.coroutines.isActive

@Composable
fun TransitLineList(
    lines: List<TransitLine>,
    selectedLineId: String?,
    onLineClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val editorialColors = EditorialTheme.colors
    var activeFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Local", "Rapid", "Express", "Owl")

    val filteredLines = remember(lines, activeFilter) {
        if (activeFilter == "All") lines
        else {
            lines.filter { line ->
                val id = line.id.uppercase()
                when (activeFilter) {
                    "Local" -> id.all { it.isDigit() } || (id.all { it.isLetter() } && !id.endsWith("OWL"))
                    "Rapid" -> id.endsWith("R")
                    "Express" -> id.endsWith("X") || id.endsWith("AX") || id.endsWith("BX")
                    "Owl" -> id.endsWith("OWL")
                    else -> true
                }
            }
        }
    }

    LazyColumn(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(editorialColors.surfaceContainerLow)
                    .padding(horizontal = EditorialTheme.spacing.gutterMobile, vertical = EditorialTheme.spacing.spaceXs)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(EditorialTheme.spacing.space2xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                filters.forEach { filter ->
                    val isActive = filter == activeFilter
                    Box(
                        modifier = Modifier
                            .background(
                                if (isActive) editorialColors.editorialPaper else editorialColors.surfaceCard
                            )
                            .clickable { activeFilter = filter }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = filter.uppercase(),
                            style = EditorialTheme.typography.labelLarge,
                            color = if (isActive) editorialColors.editorialBlack else editorialColors.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            EditorialDivider()
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = EditorialTheme.spacing.gutterMobile, vertical = EditorialTheme.spacing.spaceSm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Muni routes",
                    style = EditorialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = editorialColors.editorialPaper
                )
            }
            EditorialDivider()
        }

        items(filteredLines, key = { it.id }) { line ->
            val isSelected = line.id == selectedLineId
            val routeColor = getRouteColor(line, editorialColors)
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isSelected) editorialColors.surfaceCard else MaterialTheme.colorScheme.surface)
                    .clickable { onLineClick(line.id) }
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .background(if (isSelected) editorialColors.electricAlert else Color.Transparent)
                )

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = EditorialTheme.spacing.gutterMobile, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(EditorialTheme.spacing.spaceSm)
                    ) {
                        RouteBadge(
                            routeNumber = line.id,
                            backgroundColor = routeColor,
                            contentColor = editorialColors.editorialPaper
                        )
                        Text(
                            text = line.name.toTitleCase(),
                            style = EditorialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = editorialColors.editorialPaper,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(editorialColors.surfaceContainerHigh)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = line.transportMode.toTitleCase(),
                            style = EditorialTheme.typography.labelSmall,
                            color = editorialColors.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            EditorialDivider()
        }
    }
}
