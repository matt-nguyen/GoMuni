package com.nghianguyen.gomuni.features.transit.components

import com.nghianguyen.gomuni.features.transit.model.LoadStatus

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nghianguyen.gomuni.ui.theme.EditorialTheme

@Composable
internal fun TransitDestinationLayout(
    wide: Boolean,
    modifier: Modifier,
    map: @Composable (Modifier) -> Unit,
    panel: @Composable (Modifier) -> Unit,
) {
    if (wide) Row(modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(EditorialTheme.spacing.columnGap)) {
        map(Modifier.weight(5f).fillMaxHeight())
        panel(Modifier.weight(3f).fillMaxHeight())
    } else Column(modifier.fillMaxSize()) {
        map(Modifier.weight(0.4f).fillMaxWidth())
        panel(Modifier.weight(0.6f).fillMaxWidth())
    }
}

internal val LoadStatus.isLoading get() = this == LoadStatus.LOADING || this == LoadStatus.NOT_LOADED
