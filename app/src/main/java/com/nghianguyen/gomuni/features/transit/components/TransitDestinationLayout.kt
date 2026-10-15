package com.nghianguyen.gomuni.features.transit.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import com.nghianguyen.gomuni.features.transit.model.LoadStatus
import com.nghianguyen.gomuni.ui.theme.EditorialTheme

/**
 * Allocates stable map and panel regions without moving either composition on resize.
 *
 * @param wide Whether to place the map beside the panel instead of above it.
 * @param modifier Placement within the destination scaffold.
 * @param map Persistent map content.
 * @param panel Destination list or detail content.
 */
@Composable
internal fun TransitDestinationLayout(
    wide: Boolean,
    modifier: Modifier,
    map: @Composable (Modifier) -> Unit,
    panel: @Composable (Modifier) -> Unit,
) {
    val columnGap = EditorialTheme.spacing.columnGap
    Layout(
        modifier = modifier.fillMaxSize(),
        content = {
            Box { map(Modifier.fillMaxSize()) }
            Box { panel(Modifier.fillMaxSize()) }
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val gap = if (wide) columnGap.roundToPx().coerceAtMost(width) else 0
        val mapWidth =
            if (wide) ((width - gap) * WIDE_MAP_WIDTH_NUMERATOR / WIDE_MAP_WIDTH_DENOMINATOR)
            else width
        val mapHeight = if (wide) height else (height * COMPACT_MAP_HEIGHT_FRACTION).toInt()
        val panelWidth = if (wide) width - gap - mapWidth else width
        val panelHeight = if (wide) height else height - mapHeight
        val map = measurables[0].measure(Constraints.fixed(mapWidth, mapHeight))
        val panel = measurables[1].measure(Constraints.fixed(panelWidth, panelHeight))
        layout(width, height) {
            map.placeRelative(0, 0)
            panel.placeRelative(if (wide) mapWidth + gap else 0, if (wide) 0 else mapHeight)
        }
    }
}

internal val LoadStatus.isLoading
    get() = this == LoadStatus.LOADING || this == LoadStatus.NOT_LOADED

private const val WIDE_MAP_WIDTH_NUMERATOR = 5
private const val WIDE_MAP_WIDTH_DENOMINATOR = 8
private const val COMPACT_MAP_HEIGHT_FRACTION = 0.4f
