package com.nghianguyen.gomuni.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.nghianguyen.gomuni.ui.theme.EditorialTheme

@Composable
fun EditorialCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = EditorialTheme.colors.surfaceCard,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth().background(backgroundColor),
    ) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = EditorialTheme.spacing.gutterMobile)
                    .padding(vertical = EditorialTheme.spacing.spaceMd),
        ) {
            content()
        }
        EditorialDivider()
    }
}
