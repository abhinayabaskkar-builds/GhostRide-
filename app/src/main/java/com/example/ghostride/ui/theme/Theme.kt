package com.example.ghostride.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val GhostRideDarkColorScheme = darkColorScheme(
    primary = GhostRideGreenLight,
    onPrimary = GhostRideOnPrimary,
    secondary = GhostRideGreenDark,
    onSecondary = GhostRideOnSecondary,
    tertiary = GhostRideAmber,
    onTertiary = GhostRideOnTertiary,
    background = GhostRideBackground,
    surface = GhostRideSurface,
    onBackground = GhostRideOnBackground,
    onSurface = GhostRideOnSurface
)

private val GhostRideLightColorScheme = lightColorScheme(
    primary = GhostRideGreenDark,
    onPrimary = GhostRideOnSecondary,
    secondary = GhostRideGreenLight,
    onSecondary = GhostRideOnPrimary,
    tertiary = GhostRideAmber,
    onTertiary = GhostRideOnTertiary
)

@Composable
fun GhostRideTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) GhostRideDarkColorScheme else GhostRideLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}