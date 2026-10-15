package com.nghianguyen.gomuni.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes =
    Shapes(
        extraSmall = RoundedCornerShape(0.dp),
        small = RoundedCornerShape(0.dp),
        medium = RoundedCornerShape(0.dp),
        large = RoundedCornerShape(0.dp),
        extraLarge = RoundedCornerShape(0.dp),
    )

// The sole exception to the zero-radius rule is the Route Tag
// Updated from 9999.dp to 4.dp to match reference screenshots
val PillShape = RoundedCornerShape(4.dp)
