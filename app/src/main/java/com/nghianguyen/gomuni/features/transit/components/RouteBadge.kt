package com.nghianguyen.gomuni.features.transit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nghianguyen.gomuni.ui.theme.EditorialTheme
import com.nghianguyen.gomuni.ui.theme.PillShape

@Composable
fun RouteBadge(
    routeNumber: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    contentColor: Color = Color.White,
) {
    // Matching size 56dp x 36dp (approx w-14 h-9) from HTML reference
    Box(
        modifier = modifier.width(56.dp).height(36.dp).background(backgroundColor, PillShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = routeNumber,
            style = EditorialTheme.typography.labelLarge,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}
