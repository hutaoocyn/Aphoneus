package com.aphoneus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = NavPillAccent,
    onPrimary = TextPrimary,
    primaryContainer = NavPillBackground,
    onPrimaryContainer = NavPillGlow,
    secondary = NominalCyan,
    onSecondary = TextPrimary,
    background = SurfaceCanvas,
    onBackground = TextPrimary,
    surface = SurfaceContainer,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceContainerElevated,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceBorder,
    error = CriticalRust
)

@Composable
fun AphoneusTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AphoneusTypography,
        content = content
    )
}
