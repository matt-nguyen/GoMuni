package com.nghianguyen.gomuni.features.transit.navigation

import androidx.compose.runtime.*
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import com.nghianguyen.gomuni.app.navigation.StopArrivalsKey
import com.nghianguyen.gomuni.app.navigation.isStopsParentOf
import com.nghianguyen.gomuni.features.transit.map.ArrivalMapProjection
import com.nghianguyen.gomuni.features.transit.stops.RouteStopsUiState

/** Read-only parent projection and child map projection, shared only by visible route wrappers. */
internal class ArrivalsPresentation(val destination: StopArrivalsKey) {
    var mapArrivals by mutableStateOf<ArrivalMapProjection?>(null)
}

internal val LocalArrivalsPresentation = staticCompositionLocalOf<ArrivalsPresentation?> { null }
internal val LocalArrivalsContent = staticCompositionLocalOf<(@Composable () -> Unit)?> { null }
internal val LocalStopsContext = staticCompositionLocalOf<RouteStopsUiState?> { null }
internal val LocalWideLayout = staticCompositionLocalOf { false }
internal val LocalRetryStops = staticCompositionLocalOf<() -> Unit> { {} }
internal const val TRANSIT_DESTINATION = "transit_destination"

/** Both entries remain independently scoped while the parent supplies the surrounding map/scaffold. */
internal class ArrivalsSceneStrategy : SceneStrategy<NavKey> {
    override fun SceneStrategyScope<NavKey>.calculateScene(entries: List<NavEntry<NavKey>>): Scene<NavKey>? {
        val child = entries.lastOrNull() ?: return null
        val destination = child.metadata[TRANSIT_DESTINATION] as? StopArrivalsKey ?: return null
        val parent = entries.getOrNull(entries.lastIndex - 1) ?: return null
        if (!isStopsParentOf(parent.metadata[TRANSIT_DESTINATION] as? NavKey, destination)) return null
        return ArrivalsScene(parent, child, destination, entries.dropLast(1))
    }
}

private data class ArrivalsScene(
    val parent: NavEntry<NavKey>,
    val child: NavEntry<NavKey>,
    val destination: StopArrivalsKey,
    override val previousEntries: List<NavEntry<NavKey>>,
) : Scene<NavKey> {
    override val key: Any = parent.contentKey
    override val entries = listOf(parent, child)
    override val content: @Composable () -> Unit = {
        val presentation = remember(destination) { ArrivalsPresentation(destination) }
        CompositionLocalProvider(
            LocalArrivalsPresentation provides presentation,
            LocalArrivalsContent provides { child.Content() },
        ) { parent.Content() }
    }
}
