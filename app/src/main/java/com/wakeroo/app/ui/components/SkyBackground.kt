package com.wakeroo.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.wakeroo.app.ui.theme.LocalReducedMotion
import com.wakeroo.app.ui.theme.Sky
import com.wakeroo.app.ui.theme.WakerooMotion
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private data class Star(val x: Float, val y: Float, val r: Float, val phase: Float, val speed: Float, val depth: Float)

/**
 * The living sky behind Wakeroo screens: a gradient that glides between [Sky] states, stars
 * that twinkle at night and drift with device tilt (parallax depth), and an optional sun glow.
 */
@Composable
fun SkyBackground(
    sky: Sky,
    modifier: Modifier = Modifier,
    tilt: Offset = Offset.Zero,
    sunGlow: Float = 0f,
    sunCenter: Offset = Offset(0.5f, 0.42f),
    content: @Composable BoxScope.() -> Unit = {},
) {
    val top by animateColorAsState(sky.top, WakerooMotion.calm(), label = "skyTop")
    val bottom by animateColorAsState(sky.bottom, WakerooMotion.calm(), label = "skyBottom")
    val starAlpha by androidx.compose.animation.core.animateFloatAsState(
        if (sky == Sky.Night || sky == Sky.Dawn) 1f else 0f, WakerooMotion.calm(), label = "stars",
    )
    val reduced = LocalReducedMotion.current
    val time by if (reduced) remember { androidx.compose.runtime.mutableFloatStateOf(0f) } else
        rememberInfiniteTransition(label = "sky").animateFloat(
            0f, 1f, infiniteRepeatable(tween(12_000, easing = LinearEasing), RepeatMode.Restart), label = "t",
        )
    val stars = remember {
        val rnd = Random(7)
        List(70) { Star(rnd.nextFloat(), rnd.nextFloat() * 0.75f, 0.6f + rnd.nextFloat() * 1.6f, rnd.nextFloat(), 1f + rnd.nextInt(3), 0.3f + rnd.nextFloat()) }
    }

    Box(modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.verticalGradient(listOf(top, bottom)))
            if (starAlpha > 0.01f) drawStars(stars, time, starAlpha, tilt)
            if (sunGlow > 0.01f) {
                val c = Offset(size.width * sunCenter.x - tilt.x * 18f, size.height * sunCenter.y - tilt.y * 18f)
                val r = size.minDimension * (0.55f + 0.06f * sin(time * 2 * PI.toFloat()))
                drawCircle(
                    Brush.radialGradient(
                        listOf(Color(0xFFFF7A59).copy(alpha = 0.55f * sunGlow), Color(0xFFFF7A59).copy(alpha = 0.12f * sunGlow), Color.Transparent),
                        center = c, radius = r,
                    ),
                    radius = r, center = c,
                )
            }
        }
        content()
    }
}

private fun DrawScope.drawStars(stars: List<Star>, time: Float, alpha: Float, tilt: Offset) {
    for (s in stars) {
        val twinkle = 0.45f + 0.55f * ((sin((time * s.speed + s.phase) * 2 * PI) + 1) / 2).toFloat()
        val px = s.x * size.width + tilt.x * 26f * s.depth
        val py = s.y * size.height + tilt.y * 26f * s.depth
        drawCircle(Color.White.copy(alpha = alpha * twinkle * 0.85f), radius = s.r * density * 0.9f, center = Offset(px, py))
    }
}
