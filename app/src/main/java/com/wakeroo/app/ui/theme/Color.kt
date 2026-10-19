package com.wakeroo.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Expressive Dawn palette. Source of truth: docs/brand/README.md
val Night = Color(0xFF0E1120)
val NightSurface = Color(0xFF1A1E33)
val NightSurfaceHigh = Color(0xFF252A44)
val SunriseCoral = Color(0xFFFF7A59)
val SunriseCoralDeep = Color(0xFFE85D3F)
val DawnLavender = Color(0xFFB9A7FF)
val MorningGold = Color(0xFFFFC857)
val Paper = Color(0xFFF5F3EE)
val PaperMuted = Color(0xFFA9ABB8)
val Day = Color(0xFFFFF8F2)
val Alert = Color(0xFFFF5470)
val Calm = Color(0xFF5BD6A8)

/** Background skies keyed to the hour an alarm rings. */
enum class Sky(val top: Color, val bottom: Color) {
    Night(com.wakeroo.app.ui.theme.Night, NightSurface),
    Dawn(Color(0xFF2B2550), Color(0xFF6B4A78)),
    Sunrise(Color(0xFF6B4A78), SunriseCoral),
    Morning(SunriseCoral, MorningGold),
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
