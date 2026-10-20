package com.wakeroo.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Expressive Dawn palette. Source of truth: docs/brand/README.md
val Night = Color(0xFF0E1120)
val SunriseCoral = Color(0xFFFF7A59)
val SunriseCoralDeep = Color(0xFFE85D3F)
val DawnLavenderFixed = Color(0xFFB9A7FF)
val MorningGoldFixed = Color(0xFFFFC857)
val Day = Color(0xFFFFF8F2)
/** Always-light text, for use on coral, gold and photo backgrounds in both themes. */
val PaperFixed = Color(0xFFF5F3EE)

/**
 * The colours that flip between the dark and light app themes. Ring, mission and morning
 * screens always run with [DarkPalette].
 */
@Immutable
data class WakerooPalette(
    val isDark: Boolean,
    /** Primary text and icons. */
    val ink: Color,
    val inkMuted: Color,
    /** Cards and sheets. */
    val surface: Color,
    val surfaceHigh: Color,
    /** The screen behind everything. */
    val base: Color,
    /** Translucent panels over a sky. */
    val glass: Color,
    /** Hairline borders on cards. */
    val hairline: Color,
)

val DarkPalette = WakerooPalette(
    isDark = true,
    ink = Color(0xFFF5F3EE),
    inkMuted = Color(0xFFA9ABB8),
    surface = Color(0xFF1A1E33),
    surfaceHigh = Color(0xFF252A44),
    base = Night,
    glass = Night.copy(alpha = 0.35f),
    hairline = Color.White.copy(alpha = 0.07f),
)

val LightPalette = WakerooPalette(
    isDark = false,
    ink = Color(0xFF1B1E2E),
    inkMuted = Color(0xFF5E6175),
    surface = Color(0xFFFFFFFF),
    surfaceHigh = Color(0xFFF4EDE6),
    base = Day,
    glass = Color.White.copy(alpha = 0.62f),
    hairline = Color(0xFF1B1E2E).copy(alpha = 0.08f),
)

val LocalPalette = staticCompositionLocalOf { DarkPalette }

// Theme-aware names kept from the original palette so screens read naturally.
val Paper: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.ink
val PaperMuted: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.inkMuted
val NightSurface: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.surface
val NightSurfaceHigh: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.surfaceHigh
val Base: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.base
val Glass: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.glass
val Hairline: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.hairline

// Accents deepen in the light theme so text in them stays readable on paper.
val DawnLavender: Color @Composable @ReadOnlyComposable get() = if (LocalPalette.current.isDark) DawnLavenderFixed else Color(0xFF5B4BC4)
val MorningGold: Color @Composable @ReadOnlyComposable get() = if (LocalPalette.current.isDark) MorningGoldFixed else Color(0xFFB7791F)
val Alert = Color(0xFFFF5470)
val Calm = Color(0xFF5BD6A8)

/** Background skies keyed to the hour an alarm rings. */
enum class Sky(val top: Color, val bottom: Color) {
    Night(com.wakeroo.app.ui.theme.Night, Color(0xFF1A1E33)),
    Dawn(Color(0xFF2B2550), Color(0xFF6B4A78)),
    Sunrise(Color(0xFF6B4A78), SunriseCoral),
    Morning(SunriseCoral, MorningGoldFixed),
    Day(com.wakeroo.app.ui.theme.Day, Color(0xFFFFE9D6));

    val brush: Brush get() = Brush.verticalGradient(listOf(top, bottom))

    companion object {
        fun forHour(hour: Int): Sky = when (hour) {
            in 0..3 -> Night
            in 4..5 -> Dawn
            6 -> Sunrise
            in 7..9 -> Morning
            in 10..17 -> Day
            in 18..20 -> Dawn
            else -> Night
        }
    }
}
