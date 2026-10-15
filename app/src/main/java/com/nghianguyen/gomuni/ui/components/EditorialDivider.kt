package com.nghianguyen.gomuni.ui.components

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nghianguyen.gomuni.ui.theme.EditorialTheme

@Composable
fun EditorialDivider(
    modifier: Modifier = Modifier,
) {
    HorizontalDivider(
        modifier = modifier,
        thickness = EditorialTheme.spacing.hairline,
        color = EditorialTheme.colors.hairlineGray,
    )
}
