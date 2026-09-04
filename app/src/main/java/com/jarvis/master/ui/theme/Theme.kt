package com.jarvis.master.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ColorWhite = Color.White
private val ColorGreen = Color(0xFF81C784)
private val ColorRed = Color(0xFFEF9A9A)

private val LightColors = lightColorScheme(
    primary = BluePrimary,
    onPrimary = ColorWhite,
    primaryContainer = BlueLight,
    onPrimaryContainer = ColorWhite,
    secondary = CyanAccent,
    onSecondary = ColorWhite,
    tertiary = Green,
    background = Color(0xFFF6F8FB),
    surface = ColorWhite,
    error = Red
)

private val DarkColors = darkColorScheme(
    primary = BlueLight,
    onPrimary = Color(0xFF002E5B),
    primaryContainer = BlueDark,
    onPrimaryContainer = ColorWhite,
    secondary = CyanAccent,
    onSecondary = Color(0xFF00363D),
    tertiary = ColorGreen,
    background = Color(0xFF121417),
    surface = Color(0xFF1B1E22),
    error = ColorRed
)

/**
 * Тема приложения.
 * @param themeMode 0 = системная, 1 = светлая, 2 = тёмная.
 */
@Composable
fun JarvisTheme(themeMode: Int = 0, content: @Composable () -> Unit) {
    val darkTheme = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
