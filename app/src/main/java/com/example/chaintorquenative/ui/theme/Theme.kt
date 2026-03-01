package com.example.chaintorquenative.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ChainTorque Brand Colors
private val Primary = Color(0xFF6366F1) // Indigo
private val Secondary = Color(0xFF8B5CF6) // Purple
private val Tertiary = Color(0xFF03DAC6) // Teal
private val Background = Color(0xFF0F172A) // Slate 900
private val Surface = Color(0xFF1E293B) // Slate 800
private val OnPrimary = Color.White
private val OnSecondary = Color.White
private val OnBackground = Color.White
private val OnSurface = Color.White
private val Error = Color(0xFFEF4444)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    secondary = Secondary,
    tertiary = Tertiary,
    background = Background,
    surface = Surface,
    onPrimary = OnPrimary,
    onSecondary = OnSecondary,
    onBackground = OnBackground,
    onSurface = OnSurface,
    error = Error
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    secondary = Secondary,
    tertiary = Tertiary,
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    error = Error
)

@Composable
fun ChainTorqueTheme(
    darkTheme: Boolean = true, // Default to dark theme to match web
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}