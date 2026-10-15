package com.nghianguyen.gomuni.features.transit.arrivals

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

/**
 * Gives an open sheet its own ViewModel store and a lifecycle bounded by its parent entry.
 * Closing or changing stops clears the store, cancelling unfinished arrivals work. On Activity
 * recreation the parent restores the open stop ID and the sheet reloads its transient data.
 * @param stopId Identity of the current sheet session.
 * @param content Arrivals integration rendered with the sheet's owners.
 */
@Composable
internal fun ArrivalsSheetScope(stopId: String, content: @Composable () -> Unit) {
    key(stopId) {
        val parentStoreOwner = checkNotNull(LocalViewModelStoreOwner.current)
        val parentFactory = parentStoreOwner as HasDefaultViewModelProviderFactory
        val parentLifecycle = LocalLifecycleOwner.current.lifecycle
        val owner = remember(parentStoreOwner, parentLifecycle) { ArrivalsSheetOwner(parentFactory) }
        DisposableEffect(owner, parentLifecycle) {
            val observer = LifecycleEventObserver { _, _ -> owner.moveTo(parentLifecycle.currentState) }
            parentLifecycle.addObserver(observer)
            owner.moveTo(parentLifecycle.currentState)
            onDispose {
                parentLifecycle.removeObserver(observer)
                owner.close()
            }
        }
        CompositionLocalProvider(
            LocalViewModelStoreOwner provides owner,
            LocalLifecycleOwner provides owner,
        ) { content() }
    }
}

private class ArrivalsSheetOwner(
    factoryOwner: HasDefaultViewModelProviderFactory,
) : ViewModelStoreOwner, LifecycleOwner, HasDefaultViewModelProviderFactory by factoryOwner {
    override val viewModelStore = ViewModelStore()
    private val registry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = registry

    fun moveTo(state: Lifecycle.State) {
        if (registry.currentState != Lifecycle.State.DESTROYED) registry.currentState = state
    }

    fun close() {
        if (registry.currentState == Lifecycle.State.INITIALIZED) registry.currentState = Lifecycle.State.CREATED
        registry.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
    }
}
