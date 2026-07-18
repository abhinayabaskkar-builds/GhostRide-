package com.example.ghostride.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val GhostRideDarkColorScheme = darkColorScheme(
    primary = GhostRideGreenLight,
    secondary = GhostRideGreenDark,
    tertiary = GhostRideAmber,
    background = GhostRideBackground,
    surface = GhostRideSurface,
    onBackground = GhostRideOnBackground,
    onSurface = GhostRideOnSurface
)

private val GhostRideLightColorScheme = lightColorScheme(
    primary = GhostRideGreenDark,
    secondary = GhostRideGreenLight,
    tertiary = GhostRideAmber
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