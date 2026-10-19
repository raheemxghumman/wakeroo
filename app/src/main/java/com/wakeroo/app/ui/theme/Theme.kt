package com.wakeroo.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// "Expressive Dawn" palette. See the build plan for the full design system.
val Night = Color(0xFF0E1120)
val NightSurface = Color(0xFF1A1E33)
val SunriseCoral = Color(0xFFFF7A59)
val DawnLavender = Color(0xFFB9A7FF)
val MorningGold = Color(0xFFFFC857)
val Paper = Color(0xFFF5F3EE)
val Alert = Color(0xFFFF5470)

private val DawnDarkColors = darkColorScheme(
    primary = SunriseCoral,
    onPrimary = Night,
    secondary = DawnLavender,
    onSecondary = Night,
    tertiary = MorningGold,
    onTertiary = Night,
    background = Night,
    onBackground = Paper,
    surface = NightSurface,
    onSurface = Paper,
    error = Alert,
)

@Composable
fun WakerooTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DawnDarkColors, content = content)
}
