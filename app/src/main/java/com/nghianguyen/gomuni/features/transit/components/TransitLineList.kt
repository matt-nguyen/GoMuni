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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nghianguyen.gomuni.R
import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.ui.components.EditorialDivider
import com.nghianguyen.gomuni.ui.theme.EditorialTheme

/**
 * Renders sorted lines with filtered favorites first; row and star actions remain separate.
 *
 * @param lines Available lines in the selected sort order.
 * @param selectedLineId Optional selected line identity.
 * @param onLineClick Reports row navigation intent.
 * @param modifier Caller placement.
 * @param activeFilter Route-type filter applied to both groups.
 * @param onFilterSelected Reports filter selection.
 * @param listState Route-owned list position.
 * @param filterScrollState Route-owned filter scroll position.
 * @param favoriteIds Confirmed favorite identities, including unavailable lines.
 * @param pendingFavoriteIds Lines whose favorite controls are disabled during persistence.
 * @param favoritesReady Whether stored preferences are available for editing.
 * @param onFavoriteChanged Reports an explicit desired favorite membership.
 */
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
    favoriteIds: Set<String> = emptySet(),
    pendingFavoriteIds: Set<String> = emptySet(),
    favoritesReady: Boolean = false,
    onFavoriteChanged: (String, Boolean) -> Unit = { _, _ -> },
) {

    val groups =
        remember(lines, activeFilter, favoriteIds) {
            groupTransitLines(lines, activeFilter, favoriteIds)
        }
    val displayedLines = groups.favorites + groups.others

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

        items(displayedLines, key = { it.id }) { line ->
            val isSelected = line.id == selectedLineId

            if (
                groups.favorites.isNotEmpty() &&
                    groups.others.isNotEmpty() &&
                    line.id == groups.others.first().id
            ) {
                EditorialDivider(
                    Modifier.padding(vertical = EditorialTheme.spacing.spaceXs)
                        .testTag("favorites-divider"),
                )
            }
            TransitLineRow(
                line = line,
                isSelected = isSelected,
                onLineClick = onLineClick,
                isFavorite = line.id in favoriteIds,
                favoriteEnabled = favoritesReady && line.id !in pendingFavoriteIds,
                onFavoriteChanged = onFavoriteChanged,
            )
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
private fun TransitLineRow(
    line: TransitLine,
    isSelected: Boolean,
    onLineClick: (String) -> Unit,
    isFavorite: Boolean,
    favoriteEnabled: Boolean,
    onFavoriteChanged: (String, Boolean) -> Unit,
) {
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

            FavoriteLineControl(line.id, isFavorite, favoriteEnabled, onFavoriteChanged)
        }
    }
}

@Composable
private fun FavoriteLineControl(
    lineId: String,
    isFavorite: Boolean,
    enabled: Boolean,
    onFavoriteChanged: (String, Boolean) -> Unit,
) {
    val editorialColors = EditorialTheme.colors
    IconToggleButton(
        checked = isFavorite,
        onCheckedChange = { onFavoriteChanged(lineId, it) },
        enabled = enabled,
    ) {
        Icon(
            painter =
                if (isFavorite) rememberVectorPainter(Icons.Filled.Star)
                else painterResource(R.drawable.ic_star_outline),
            contentDescription =
                stringResource(
                    if (isFavorite) R.string.unfavorite_line else R.string.favorite_line,
                    lineId,
                ),
            tint =
                if (isFavorite) editorialColors.signalChartreuse
                else editorialColors.onSurfaceVariant,
        )
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

internal data class TransitLineGroups(
    val favorites: List<TransitLine>,
    val others: List<TransitLine>,
)

/** Preserves the caller's sort order within each group and renders each identity once. */
internal fun groupTransitLines(
    lines: List<TransitLine>,
    filter: String,
    favoriteIds: Set<String>,
): TransitLineGroups {
    val visible = lines.distinctBy { it.id }.filter { matchesRouteFilter(it, filter) }
    val (favorites, others) = visible.partition { it.id in favoriteIds }
    return TransitLineGroups(favorites, others)
}
