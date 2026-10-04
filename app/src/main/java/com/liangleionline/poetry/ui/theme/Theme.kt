package com.liangleionline.poetry.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 国风配色：素雅青灰
val Ink = Color(0xFF2B2B2B)
val Paper = Color(0xFFFAF6EF)
val Cinnabar = Color(0xFF9C2F2F)
val InkDim = Color(0xFF6B6B6B)

private val LightColors = lightColorScheme(
    primary = Cinnabar,
    onPrimary = Color.White,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    secondary = InkDim,
    onSecondary = Color.White,
    surfaceVariant = Color(0xFFEDE6D9),
    onSurfaceVariant = InkDim,
    outline = Color(0xFFC9C1B2)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD9A0A0),
    onPrimary = Color(0xFF2B1515),
    background = Color(0xFF1C1C1C),
    onBackground = Color(0xFFE8E2D6),
    surface = Color(0xFF242424),
    onSurface = Color(0xFFE8E2D6),
    secondary = Color(0xFFB8AFA0),
    onSecondary = Color(0xFF2B2B2B),
    surfaceVariant = Color(0xFF2E2E2E),
    onSurfaceVariant = Color(0xFFC9C1B2),
    outline = Color(0xFF4A4A4A)
)

@Composable
fun PoetryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = PoetryTypography,
        content = content
    )
}
