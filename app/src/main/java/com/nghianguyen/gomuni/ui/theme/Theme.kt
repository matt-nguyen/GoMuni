package com.nghianguyen.gomuni.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColorScheme =
    darkColorScheme(
        primary = Primary,
        onPrimary = OnPrimary,
        primaryContainer = PrimaryContainer,
        onPrimaryContainer = OnPrimaryContainer,
        inversePrimary = InversePrimary,
        secondary = Secondary,
        onSecondary = OnSecondary,
        secondaryContainer = SecondaryContainer,
        onSecondaryContainer = OnSecondaryContainer,
        tertiary = Tertiary,
        onTertiary = OnTertiary,
        tertiaryContainer = TertiaryContainer,
        onTertiaryContainer = OnTertiaryContainer,
        background = Background,
        onBackground = OnBackground,
        surface = Surface,
        onSurface = OnSurface,
        surfaceVariant = SurfaceVariant,
        onSurfaceVariant = OnSurfaceVariant,
        outline = Outline,
        outlineVariant = OutlineVariant,
        error = Error,
        onError = OnError,
        errorContainer = ErrorContainer,
        onErrorContainer = OnErrorContainer,
        inverseSurface = InverseSurface,
        inverseOnSurface = InverseOnSurface,
        surfaceTint = SurfaceTint,
    )

data class EditorialColors(
    val editorialPaper: Color = EditorialPaper,
    val editorialBlack: Color = EditorialBlack,
    val hairlineGray: Color = HairlineGray,
    val ruleLight: Color = RuleLight,
    val surfaceCard: Color = SurfaceCard,
    val muniRed: Color = MuniRed,
    val cableCarGreen: Color = CableCarGreen,
    val electricAlert: Color = ElectricAlert,
    val signalChartreuse: Color = SignalChartreuse,
    val telemetryCyan: Color = TelemetryCyan,
    val routeBus: Color = RouteBus,
    val routeLightRail: Color = RouteLightRail,
    val routeRapid: Color = RouteRapid,
    val routeExpress: Color = RouteExpress,
    val routeLateNight: Color = RouteLateNight,
    val onSurface: Color = OnSurface,
    val onSurfaceVariant: Color = OnSurfaceVariant,
    val surfaceStroke: Color = Color(0xFF333333),
    val mapOverlay: Color = Color(0xD9121212),
    val surfaceContainerLowest: Color = SurfaceContainerLowest,
    val surfaceContainerLow: Color = SurfaceContainerLow,
    val surfaceContainer: Color = SurfaceContainer,
    val surfaceContainerHigh: Color = SurfaceContainerHigh,
    val surfaceContainerHighest: Color = SurfaceContainerHighest,
)

data class EditorialSpacing(
    val hairline: Dp = 1.dp,
    val space2xs: Dp = 4.dp,
    val spaceXs: Dp = 8.dp,
    val spaceSm: Dp = 12.dp,
    val spaceMd: Dp = 16.dp,
    val spaceLg: Dp = 24.dp,
    val spaceXl: Dp = 32.dp,
    val space2xl: Dp = 48.dp,
    val gutterMobile: Dp = 16.dp,
    val gutterTablet: Dp = 24.dp,
    val columnGap: Dp = 12.dp,
)

val LocalEditorialColors = staticCompositionLocalOf { EditorialColors() }
val LocalEditorialSpacing = staticCompositionLocalOf { EditorialSpacing() }

object EditorialTheme {
    val colors: EditorialColors
        @Composable @ReadOnlyComposable get() = LocalEditorialColors.current

    val spacing: EditorialSpacing
        @Composable @ReadOnlyComposable get() = LocalEditorialSpacing.current

    val typography: Typography
        @Composable @ReadOnlyComposable get() = MaterialTheme.typography
}

@Composable
fun GoMuniTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    // Forcing dark theme as per DESIGN.md "uncompromising dark-mode default"
    val colorScheme = DarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    CompositionLocalProvider(
        LocalEditorialColors provides EditorialColors(),
        LocalEditorialSpacing provides EditorialSpacing(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content,
        )
    }
}
