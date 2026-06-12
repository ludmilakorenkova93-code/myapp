package com.winasde.apps.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF245B3A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = Color(0xFF062311),
    secondary = Color(0xFF385A7A),
    tertiary = Color(0xFFB45F06),
    background = Color(0xFFF7F8F4),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE0E6DC),
    onSurface = Color(0xFF1B1D1A),
    onSurfaceVariant = Color(0xFF424940)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9BD5A7),
    onPrimary = Color(0xFF00391B),
    primaryContainer = Color(0xFF15522D),
    onPrimaryContainer = Color(0xFFC8E6C9),
    secondary = Color(0xFFA7C9EA),
    tertiary = Color(0xFFFFC27A),
    background = Color(0xFF111411),
    surface = Color(0xFF191D18),
    surfaceVariant = Color(0xFF3E493F),
    onSurface = Color(0xFFE5E8E1),
    onSurfaceVariant = Color(0xFFC1C9BD)
)

@Composable
fun FootballSchemesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
