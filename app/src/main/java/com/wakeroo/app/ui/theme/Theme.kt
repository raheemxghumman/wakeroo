package com.wakeroo.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.wakeroo.app.data.MotionPreference

private val DawnDark = darkColorScheme(
    primary = SunriseCoral,
    onPrimary = Night,
    primaryContainer = Color(0xFF3A2530),
    onPrimaryContainer = Paper,
    secondary = DawnLavender,
    onSecondary = Night,
    tertiary = MorningGold,
    onTertiary = Night,
    background = Night,
    onBackground = Paper,
    surface = Night,
    onSurface = Paper,
    surfaceVariant = NightSurface,
    onSurfaceVariant = PaperMuted,
    surfaceContainer = NightSurface,
    surfaceContainerHigh = NightSurfaceHigh,
    surfaceContainerHighest = NightSurfaceHigh,
    surfaceContainerLow = NightSurface,
    inverseSurface = Paper,
    inverseOnSurface = Night,
    outline = Color(0xFF3A3F5C),
    error = Alert,
    onError = Night,
)

private val DawnLight = lightColorScheme(
    primary = SunriseCoralDeep,
    onPrimary = Paper,
    secondary = Color(0xFF6B5BD6),
    tertiary = Color(0xFFB7791F),
    background = Day,
    onBackground = Night,
    surface = Day,
    onSurface = Night,
    surfaceVariant = Color(0xFFF3E9DF),
    onSurfaceVariant = Color(0xFF4A4D5E),
    error = Color(0xFFD62F4F),
)

/**
 * Wakeroo theme. Dark is the default; the ring and mission screens always use [dark] so they
 * stay high-contrast at 6 AM regardless of the user's choice.
 */
@Composable
fun WakerooTheme(dark: Boolean = true, motion: MotionPreference = MotionPreference.System, content: @Composable () -> Unit) {
    val reduced = when (motion) {
        MotionPreference.Full -> false
        MotionPreference.Reduced -> true
        MotionPreference.System -> systemAnimationsOff(LocalContext.current)
    }
    CompositionLocalProvider(LocalReducedMotion provides reduced) {
        MaterialTheme(
            colorScheme = if (dark) DawnDark else DawnLight,
            typography = WakerooTypography,
            shapes = WakerooShapes,
            content = content,
        )
    }
}
