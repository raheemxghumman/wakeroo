package com.wakeroo.app.ui.brand

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.wakeroo.app.ui.theme.DawnLavender
import com.wakeroo.app.ui.theme.LocalReducedMotion
import com.wakeroo.app.ui.theme.MorningGold
import com.wakeroo.app.ui.theme.Outfit
import com.wakeroo.app.ui.theme.Paper
import com.wakeroo.app.ui.theme.SunriseCoral
import com.wakeroo.app.ui.theme.WakerooMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.pow
import com.wakeroo.app.ui.util.Ltr

// Geometry on a 100 x 100 grid; must match docs/brand/build_brand.py.
private const val HORIZON_Y = 74f
private const val HORIZON_X0 = 14f
private const val HORIZON_X1 = 86f
private const val HORIZON_STROKE = 5f
private val SUN_CENTER = Offset(54f, 69f)
private const val SUN_R = 20f
private val LEAP_START = Offset(19f, 70f)
private val LEAP_CONTROL = Offset(43f, -4f)
private val LEAP_END = Offset(80f, 35f)
private const val DOT_R = 6.6f
private const val LEAP_SEGMENTS = 80

private fun leapPoint(t: Float): Offset {
    val u = 1 - t
    return LEAP_START * (u * u) + LEAP_CONTROL * (2 * u * t) + LEAP_END * (t * t)
}

/**
 * The Wakeroo mark. Each phase runs from 0 to 1: [sunRise] lifts the sun from behind the
 * horizon, [leap] draws the jump arc, [landing] scales the morning point in.
 */
@Composable
fun WakerooMark(
    modifier: Modifier = Modifier,
    sunRise: Float = 1f,
    leap: Float = 1f,
    landing: Float = 1f,
    horizonColor: Color = Paper,
    sunColor: Color = SunriseCoral,
    leapColor: Color = DawnLavender,
    dotColor: Color = MorningGold,
) {
    Canvas(modifier) {
        val unit = size.minDimension / 100f
        val origin = Offset((size.width - 100 * unit) / 2, (size.height - 100 * unit) / 2)
        fun p(o: Offset) = origin + o * unit

        // Sun: half disc, clipped at the horizon so it rises from behind it.
        clipRect(bottom = p(Offset(0f, HORIZON_Y)).y) {
            val lift = (1 - sunRise) * (SUN_R + 6)
            drawArc(
                color = sunColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = p(Offset(SUN_CENTER.x - SUN_R, SUN_CENTER.y - SUN_R + lift)),
                size = Size(2 * SUN_R * unit, 2 * SUN_R * unit),
            )
        }

        drawLine(
            color = horizonColor,
            start = p(Offset(HORIZON_X0, HORIZON_Y)),
            end = p(Offset(HORIZON_X1, HORIZON_Y)),
            strokeWidth = HORIZON_STROKE * unit,
            cap = StrokeCap.Round,
        )

        drawLeap(leap, leapColor, unit, ::p)

        if (landing > 0f) {
            drawCircle(color = dotColor, radius = DOT_R * unit * landing, center = p(LEAP_END))
        }
    }
}

private fun DrawScope.drawLeap(progress: Float, color: Color, unit: Float, p: (Offset) -> Offset) {
    val last = (LEAP_SEGMENTS * progress).toInt()
    for (i in 0 until last) {
        val t0 = i / LEAP_SEGMENTS.toFloat()
        val t1 = (i + 1) / LEAP_SEGMENTS.toFloat()
        // Tapered: thin at take-off, thick near landing (half-width 0.8 to 3.2 units).
        val halfWidth = 0.8f + 2.4f * t0.pow(1.5f)
        drawLine(
            color = color,
            start = p(leapPoint(t0)),
            end = p(leapPoint(t1)),
            strokeWidth = 2 * halfWidth * unit,
            cap = StrokeCap.Round,
        )
    }
}

/** The mark with its intro animation: sun rises, the leap draws, the point lands. */
@Composable
fun AnimatedWakerooMark(modifier: Modifier = Modifier) {
    val reduced = LocalReducedMotion.current
    val sun = remember { Animatable(if (reduced) 1f else 0f) }
    val leap = remember { Animatable(if (reduced) 1f else 0f) }
    val land = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(reduced) {
        if (reduced) return@LaunchedEffect
        launch { sun.animateTo(1f, WakerooMotion.bouncy()) }
        delay(380)
        leap.animateTo(1f, tween(620, easing = FastOutSlowInEasing))
        land.animateTo(1f, WakerooMotion.bouncy())
    }
    WakerooMark(modifier, sunRise = sun.value, leap = leap.value, landing = land.value)
}

/** "wakeroo" set in Outfit SemiBold; the last "o" is the sun. */
@Composable
fun Wordmark(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 40.sp,
    color: Color = Paper,
    accent: Color = SunriseCoral,
) {
    val style = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = fontSize, letterSpacing = (-0.015).em)
    Ltr {
        Row(modifier, verticalAlignment = Alignment.Bottom) {
            Text("wakero", style = style, color = color)
            Text("o", style = style, color = accent)
        }
    }
}
