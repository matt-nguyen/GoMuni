package com.nghianguyen.gomuni.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.nghianguyen.gomuni.ui.theme.EditorialTheme

@Composable
fun EditorialTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor =
        if (isFocused) {
            EditorialTheme.colors.electricAlert
        } else {
            EditorialTheme.colors.surfaceStroke
        }
    val borderWidth = if (isFocused) 2.dp else 1.dp

    Box(modifier = modifier) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier =
                Modifier.fillMaxWidth()
                    .background(EditorialTheme.colors.surfaceCard)
                    .border(borderWidth, borderColor)
                    .onFocusChanged { isFocused = it.isFocused }
                    .padding(
                        horizontal = EditorialTheme.spacing.spaceMd,
                        vertical = EditorialTheme.spacing.spaceSm,
                    ),
            textStyle =
                EditorialTheme.typography.bodyLarge.copy(
                    color = EditorialTheme.colors.editorialPaper,
                ),
            cursorBrush = SolidColor(EditorialTheme.colors.electricAlert),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            decorationBox = { innerTextField ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (label != null) {
                        Text(
                            text = label,
                            style = EditorialTheme.typography.labelSmall,
                            color = EditorialTheme.colors.onSurfaceVariant,
                            modifier = Modifier.padding(end = EditorialTheme.spacing.spaceXs),
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = EditorialTheme.typography.bodyLarge,
                                color = EditorialTheme.colors.onSurfaceVariant.copy(alpha = 0.5f),
                            )
                        }
                        innerTextField()
                    }
                }
            },
        )
    }
}
