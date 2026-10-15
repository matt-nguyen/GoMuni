package com.nghianguyen.gomuni.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface TransitNavigation {
    data class OpenRoute(val operatorId: String, val lineId: String) : TransitNavigation
    data object Back : TransitNavigation
}

/** Immediate UI requests are bounded and never replayed after collector restart. */
class NavigationEvents {
    private val events = MutableSharedFlow<TransitNavigation>(
        extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val flow = events.asSharedFlow()
    fun emit(event: TransitNavigation) { events.tryEmit(event) }
}

/** Only the current destination can originate navigation. Sheets do not change eligibility. */
internal fun canNavigate(origin: NavKey, current: NavKey?): Boolean = origin == current

/** Reconciles external stop entry to its stops route; the route opens the sheet separately. */
internal fun navigateToStopRoute(stack: MutableList<NavKey>, target: StopArrivalsTarget): Boolean {
    if (target.operatorId.isBlank() || target.lineId.isBlank() || target.stopId.isBlank()) return false
    val parent = TransitStopsKey(target.lineId, target.operatorId)
    val parentIndex = stack.indexOf(parent)
    if (parentIndex >= 0) {
        while (stack.lastIndex > parentIndex) stack.removeAt(stack.lastIndex)
    } else {
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
        if (stack.isEmpty()) stack.add(TransitLinesKey)
        stack.add(parent)
    }
    return true
}

/** Pure stack policy shared by in-app navigation and external entry. */
fun navigate(stack: MutableList<NavKey>, event: TransitNavigation) {
    when (event) {
        TransitNavigation.Back -> if (stack.size > 1) stack.removeAt(stack.lastIndex)
        is TransitNavigation.OpenRoute -> {
            if (event.operatorId.isBlank() || event.lineId.isBlank()) return
            val target = TransitStopsKey(event.lineId, event.operatorId)
            val existing = stack.indexOf(target)
            if (existing >= 0) while (stack.lastIndex > existing) stack.removeAt(stack.lastIndex)
            else stack.add(target)
        }
    }
}
