package com.nghianguyen.gomuni.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.nghianguyen.gomuni.ui.theme.EditorialTheme
import com.nghianguyen.gomuni.ui.theme.brutalistShadow

@Composable
fun EditorialPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val containerColor = if (isPressed) {
        EditorialTheme.colors.signalChartreuse
    } else {
        EditorialTheme.colors.editorialPaper
    }

    val contentColor = EditorialTheme.colors.editorialBlack

    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = EditorialTheme.colors.editorialPaper.copy(alpha = 0.5f),
            disabledContentColor = EditorialTheme.colors.editorialBlack.copy(alpha = 0.5f)
        ),
        interactionSource = interactionSource,
        contentPadding = PaddingValues(
            horizontal = EditorialTheme.spacing.spaceLg,
            vertical = EditorialTheme.spacing.spaceSm
        ),
        content = content
    )
}

@Composable
fun EditorialSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RectangleShape,
        border = BorderStroke(EditorialTheme.spacing.hairline, EditorialTheme.colors.editorialPaper),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = EditorialTheme.colors.editorialBlack,
            contentColor = EditorialTheme.colors.editorialPaper
        ),
        contentPadding = PaddingValues(
            horizontal = EditorialTheme.spacing.spaceLg,
            vertical = EditorialTheme.spacing.spaceSm
        ),
        content = {
            CompositionLocalProvider(
                LocalTextStyle provides EditorialTheme.typography.labelMedium
            ) {
                content()
            }
        }
    )
}

/**
 * Editorial Floating Action Button (FAB)
 * Sharp square 48x48px box with a 4px hard black offset shadow.
 */
@Composable
fun EditorialFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: androidx.compose.ui.graphics.Color = EditorialTheme.colors.editorialPaper,
    contentColor: androidx.compose.ui.graphics.Color = EditorialTheme.colors.editorialBlack,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .brutalistShadow()
            .background(containerColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // Design calls for "instantaneous" and "zero transition lag"
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}
