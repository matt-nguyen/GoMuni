@file:OptIn(ExperimentalTextApi::class)

package com.nghianguyen.gomuni.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nghianguyen.gomuni.R

//val SpaceGrotesk = FontFamily(
//    Font(R.font.spacegrotesk_variablefont, FontWeight.W400),
//    Font(R.font.spacegrotesk_variablefont, FontWeight.W600),
//    Font(R.font.spacegrotesk_variablefont, FontWeight.W700)
//)
val SpaceGrotesk = FontFamily(
    Font(
        R.font.spacegrotesk_variablefont,
        FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))
    ),
    Font(
        R.font.spacegrotesk_variablefont,
        FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))
    ),
    Font(
        R.font.spacegrotesk_variablefont,
        FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))
    )
//    Font(R.font.spacegrotesk_bold, FontWeight.Bold),
//    Font(R.font.spacegrotesk_light, FontWeight.Light),
//    Font(R.font.spacegrotesk_medium, FontWeight.Medium),
//    Font(R.font.spacegrotesk_regular, FontWeight.Normal),
//    Font(R.font.spacegrotesk_semibold, FontWeight.SemiBold)
)

val Inter = FontFamily(
    Font(
        R.font.inter_variablefont,
        FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))
    )
//        FontWeight.W400)
)

val JetBrainsMono = FontFamily(
    Font(
        R.font.jetbrainsmono_variablefont,
        FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))
    ),
    Font(
        R.font.jetbrainsmono_variablefont,
        FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))
    ),
    Font(
        R.font.jetbrainsmono_variablefont,
        FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))
    )
//    Font(R.font.jetbrainsmono_variablefont, FontWeight.W400),
//    Font(R.font.jetbrainsmono_variablefont, FontWeight.W500),
//    Font(R.font.jetbrainsmono_variablefont, FontWeight.W700)
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.04).em
    ),
    displayMedium = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.03).em
    ),
    headlineLarge = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.02).em
    ),
    headlineMedium = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.01).em
    ),
    titleLarge = TextStyle( // Massive Telemetry
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle( // Badge
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.05.em
    ),
    labelMedium = TextStyle( // Telemetry
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.02.em
    ),
    labelSmall = TextStyle( // Caption
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.04.em
    )
)
