package com.nghianguyen.gomuni.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Applies a rigid brutalist hard-edge drop shadow as specified in DESIGN.md. "box-shadow: 4px 4px
 * 0px #000000"
 */
fun Modifier.brutalistShadow(
    color: Color = Color.Black,
    offset: Dp = 4.dp,
): Modifier =
    this.drawBehind {
        drawIntoCanvas { canvas ->
            val paint =
                Paint().apply {
                    this.color = color
                }
            val offsetPx = offset.toPx()
            canvas.drawRect(
                left = offsetPx,
                top = offsetPx,
                right = size.width + offsetPx,
                bottom = size.height + offsetPx,
                paint = paint,
            )
        }
    }
