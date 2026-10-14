package com.nghianguyen.gomuni.app.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

@Composable
internal fun CollectNavigation(events: Flow<TransitNavigation>, onNavigate: (TransitNavigation) -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentNavigate by rememberUpdatedState(onNavigate)
    LaunchedEffect(events, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            events.collect { currentNavigate(it) }
        }
    }
}
