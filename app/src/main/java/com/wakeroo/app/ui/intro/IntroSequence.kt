package com.wakeroo.app.ui.intro

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.wakeroo.app.ui.theme.DawnLavender
import com.wakeroo.app.ui.theme.LocalReducedMotion
import com.wakeroo.app.ui.theme.MorningGold
import com.wakeroo.app.ui.theme.Night
import com.wakeroo.app.ui.theme.Outfit
import com.wakeroo.app.ui.theme.Paper
import com.wakeroo.app.ui.theme.SunriseCoral
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private class IntroStar(val x: Float, val y: Float, val z: Float, val r: Float)

private val EaseOutBack = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

/**
 * The launch story in about two seconds: rise over a planet at night, the sun breaks the
 * horizon, the gold point leaps over it and lands, then the camera dives into the sunrise and
 * hands over to the app. Tap to skip. With reduced motion it is a short fade.
 */
@Composable
fun IntroSequence(onReveal: () -> Unit, onFinished: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val reduced = LocalReducedMotion.current
    val scope = rememberCoroutineScope()

    val rise =
        remember { Animatable(0f) }      // camera rising over the planet, sun clearing the horizon
    val leap = remember { Animatable(0f) }      // the gold point's jump
    val squash = remember { Animatable(1f) }    // landing squash and stretch
    val shock = remember { Animatable(0f) }     // landing shockwave
    val letters = remember { Animatable(0f) }   // wordmark letters flipping in
    val dive = remember { Animatable(0f) }      // camera dive into the sun, revealing the app
    val stars = remember {
        val rnd = Random(11)
        List(140) {
            IntroStar(
                rnd.nextFloat() * 2 - 1,
                rnd.nextFloat() * 2 - 1,
                0.15f + rnd.nextFloat(),
                0.5f + rnd.nextFloat() * 1.4f
            )
        }
    }
    val revealed = remember { booleanArrayOf(false) }

    suspend fun diveOut() {
        if (!revealed[0]) {
            revealed[0] = true; onReveal()
        }
        dive.animateTo(1f, tween(620, easing = CubicBezierEasing(0.5f, 0f, 0.75f, 0.4f)))
        onFinished()
    }

    LaunchedEffect(Unit) {
        if (reduced) {
            onReveal(); revealed[0] = true
            dive.snapTo(0.85f)
            dive.animateTo(1f, tween(250))
            onFinished()
            return@LaunchedEffect
        }
        coroutineScope {
            val riseJob = async { rise.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }
            delay(650)
            leap.animateTo(1f, tween(620, easing = LinearEasing))
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            launch { squash.snapTo(0.55f); squash.animateTo(1f, spring(0.3f, 500f)) }
            launch { shock.animateTo(1f, tween(700)) }
            launch { letters.animateTo(1f, tween(700)) }
            riseJob.await()
            delay(520)
            diveOut()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .clickable(
                remember { MutableInteractionSource() },
                null
            ) { scope.launch { if (!revealed[0]) diveOut() } }
            .graphicsLayer {
                // The dive: everything rushes past the camera toward the sun.
                val d = dive.value
                scaleX = 1f + d * d * 7f
                scaleY = 1f + d * d * 7f
                alpha = 1f - ((d - 0.55f) / 0.45f).coerceIn(0f, 1f)
                transformOrigin = TransformOrigin(0.5f, 0.47f)
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = rise.value
            drawRect(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF05060C),
                        Night,
                        Color(0xFF231B3A).copy(alpha = 0.6f + 0.4f * r)
                    )
                )
            )
            drawStarfield(stars, r)
            drawPlanetAndSun(r, leap.value, squash.value, shock.value, dive.value)
        }
        Wordmark3D(letters.value, Modifier
            .align(Alignment.Center)
            .offset(y = 150.dp))
    }
}

/** Stars stream downward with depth as the camera rises: near stars move more. */
private fun DrawScope.drawStarfield(stars: List<IntroStar>, rise: Float) {
    val cx = size.width / 2;
    val cy = size.height / 2
    for (s in stars) {
        val x = cx + s.x * size.width * 0.6f
        val y = cy + s.y * size.height * 0.6f + rise * 260f * s.z
        val twinkle = 0.55f + 0.45f * sin((s.x * 40 + rise * 6) * PI).toFloat().let { (it + 1) / 2 }
        drawCircle(
            Color.White.copy(alpha = (0.35f + 0.5f * s.z) * twinkle),
            s.r * density * (0.6f + s.z * 0.5f),
            Offset(x, y)
        )
    }
}

private fun DrawScope.drawPlanetAndSun(
    rise: Float,
    leap: Float,
    squash: Float,
    shock: Float,
    dive: Float
) {
    val w = size.width;
    val h = size.height
    val horizonY =
        h * (0.78f - 0.24f * rise)                  // the limb climbs as the camera rises
    val planetR = w * 2.2f
    val planetCenter = Offset(w / 2, horizonY + planetR)
    val sunR = w * 0.17f
    val sunCenter = Offset(w / 2, horizonY + sunR * 1.15f - sunR * 2.1f * rise)

    // Sun glow and rays, behind the planet.
    val glow = (0.25f + 0.75f * rise) * (1f + dive * 2f)
    drawCircle(
        Brush.radialGradient(
            listOf(
                SunriseCoral.copy(alpha = 0.55f * glow.coerceAtMost(1f)),
                DawnLavender.copy(alpha = 0.18f * glow.coerceAtMost(1f)),
                Color.Transparent
            ), sunCenter, w * (0.75f + dive)
        ),
        w * (0.75f + dive), sunCenter,
    )
    rotate(rise * 40f, sunCenter) {
        repeat(12) { i ->
            val a = i * (2 * PI / 12).toFloat()
            val len = w * (0.45f + 0.15f * (i % 2))
            drawLine(
                Brush.linearGradient(
                    listOf(
                        MorningGold.copy(alpha = 0.22f * rise),
                        Color.Transparent
                    ), sunCenter, sunCenter + Offset(cos(a) * len, sin(a) * len)
                ),
                sunCenter,
                sunCenter + Offset(cos(a) * len, sin(a) * len),
                strokeWidth = 10f * density,
                cap = StrokeCap.Round,
            )
        }
    }
    // The sun itself, clipped so it rises from behind the planet's edge.
    val sky = Path().apply { addRect(androidx.compose.ui.geometry.Rect(0f, 0f, w, h)) }
    val planet = Path().apply { addOval(androidx.compose.ui.geometry.Rect(planetCenter, planetR)) }
    val above =
        Path().apply { op(sky, planet, androidx.compose.ui.graphics.PathOperation.Difference) }
    clipPath(above) {
        drawCircle(
            Brush.radialGradient(
                listOf(MorningGold, SunriseCoral),
                sunCenter - Offset(sunR * 0.3f, sunR * 0.4f),
                sunR * 1.5f
            ), sunR, sunCenter
        )
    }

    // The planet: a dark sphere lit along its rim by the dawn.
    drawCircle(
        Brush.verticalGradient(
            listOf(Color(0xFF1C1733), Color(0xFF07080F)),
            startY = horizonY,
            endY = horizonY + h * 0.4f
        ), planetR, planetCenter
    )
    drawCircle(
        Brush.radialGradient(
            listOf(
                Color.Transparent,
                Color.Transparent,
                DawnLavender.copy(alpha = 0.5f * rise),
                Color.Transparent
            ), planetCenter, planetR * 1.012f
        ),
        planetR * 1.012f, planetCenter, style = Stroke(planetR * 0.012f),
    )
    drawArc(
        Brush.horizontalGradient(
            listOf(
                Color.Transparent,
                SunriseCoral.copy(alpha = 0.9f * rise),
                Color.Transparent
            )
        ),
        startAngle = 250f,
        sweepAngle = 40f,
        useCenter = false,
        topLeft = planetCenter - Offset(planetR, planetR),
        size = androidx.compose.ui.geometry.Size(planetR * 2, planetR * 2),
        style = Stroke(3f * density),
    )

    if (leap <= 0f) return
    // The leap: a parabola over the sun. The point grows at the apex as it comes toward the viewer.
    val start = Offset(w * 0.18f, horizonY - 4f)
    val end = Offset(w * 0.82f, sunCenter.y - sunR * 0.9f)
    val control = Offset(w * 0.42f, sunCenter.y - sunR * 4.4f)
    fun at(t: Float): Offset {
        val u = 1 - t
        return start * (u * u) + control * (2 * u * t) + end * (t * t)
    }

    val steps = 48
    val shown = (steps * leap).toInt()
    for (i in 0 until shown) {
        val t0 = i / steps.toFloat();
        val t1 = (i + 1) / steps.toFloat()
        drawLine(
            DawnLavender.copy(alpha = 0.9f),
            at(t0),
            at(t1),
            strokeWidth = (1.5f + 7f * t0) * density,
            cap = StrokeCap.Round
        )
    }
    val p = at(leap)
    val depth = 1f + 0.9f * sin(leap * PI).toFloat()             // nearer the camera mid-jump
    val dotR = sunR * 0.32f * depth
    // Squash on landing: flatter and wider, then springs back round.
    withTransform({ scale(1f + (1f - squash) * 0.8f, squash, pivot = p + Offset(0f, dotR)) }) {
        drawCircle(MorningGold, dotR, p)
        drawCircle(
            Brush.radialGradient(
                listOf(Color.White.copy(alpha = 0.6f), Color.Transparent),
                p - Offset(dotR * 0.35f, dotR * 0.4f),
                dotR * 0.8f
            ), dotR, p
        )
    }
    if (shock > 0f && shock < 1f) {
        drawCircle(
            MorningGold.copy(alpha = (1 - shock) * 0.8f),
            dotR + shock * w * 0.35f,
            end,
            style = Stroke((1 - shock) * 6f * density)
        )
    }
}

/** "wakeroo" letters flip up into place one after another; the last "o" is the sun. */
@Composable
private fun Wordmark3D(progress: Float, modifier: Modifier) {
    val word = "wakeroo"
    Row(modifier) {
        word.forEachIndexed { i, c ->
            val local = ((progress - i * 0.07f) / 0.5f).coerceIn(0f, 1f)
            val eased = EaseOutBack.transform(local)
            Text(
                c.toString(),
                style = TextStyle(
                    fontFamily = Outfit,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 48.sp,
                    letterSpacing = (-0.015).em
                ),
                color = if (i == word.lastIndex) SunriseCoral else Paper,
                modifier = Modifier.graphicsLayer {
                    alpha = local
                    rotationX = (1 - eased) * -90f
                    translationY = (1 - eased) * 40f
                    cameraDistance = 8f * density
                    transformOrigin = TransformOrigin(0.5f, 1f)
                },
            )
        }
    }
}
