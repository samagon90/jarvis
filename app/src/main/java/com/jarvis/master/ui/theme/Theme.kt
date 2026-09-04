package com.jarvis.master.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = BluePrimary,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = BlueLight,
    onPrimaryContainer = androidx.compose.ui.graphics.Color.White,
    secondary = CyanAccent,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    tertiary = Green,
    background = androidx.compose.ui.graphics.Color(0xFFF6F8FB),
    surface = androidx.compose.ui.graphics.Color.White,
    error = Red
)

private val DarkColors = darkColorScheme(
    primary = BlueLight,
    onPrimary = androidx.compose.ui.graphics.Color(0xFF002E5B),
    primaryContainer = BlueDark,
    onPrimaryContainer = androidx.compose.ui.graphics.Color.White,
    secondary = CyanAccent,
    onSecondary = androidx.compose.ui.graphics.Color(0xFF00363D),
    tertiary = ColorGreen,
    background = androidx.compose.ui.graphics.Color(0xFF121417),
    surface = androidx.compose.ui.graphics.Color(0xFF1B1E22),
    error = ColorRed
)

private val ColorGreen = androidx.compose.ui.graphics.Color(0xFF81C784)
private val ColorRed = androidx.compose.ui.graphics.Color(0xFFEF9A9A)

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
