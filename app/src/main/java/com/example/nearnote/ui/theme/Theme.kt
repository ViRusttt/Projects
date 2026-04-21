package com.example.nearnote.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Green = Color(0xFF1D9E75)
val GreenLight = Color(0xFFE1F5EE)
val GreenDark = Color(0xFF0F6E56)
val GrayBg = Color(0xFFF5F5F5)
val GrayText = Color(0xFF888888)
val RedDelete = Color(0xFFE74C3C)

private val LightColors = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    primaryContainer = GreenLight,
    onPrimaryContainer = GreenDark,
    background = Color.White,
    surface = Color.White,
    onBackground = Color(0xFF1A1A1A),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = GrayBg
)

private val DarkColors = darkColorScheme(
    primary = Green,
    onPrimary = Color.White,
    primaryContainer = GreenDark,
    onPrimaryContainer = GreenLight,
    background = Color(0xFF1A1A1A),
    surface = Color(0xFF252525),
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF333333)
)

@Composable
fun NearNoteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
