package com.wakeroo.app.feature.you

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DoorFront
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FlutterDash
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wakeroo.app.ui.components.rememberDeviceTilt
import com.wakeroo.app.ui.theme.LocalReducedMotion
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val AchievementDef.iconVector: ImageVector
    get() = when (icon) {
        "sunrise" -> Icons.Rounded.WbTwilight
        "flame" -> Icons.Rounded.LocalFireDepartment
        "crown" -> Icons.Rounded.WorkspacePremium
        "bird" -> Icons.Rounded.FlutterDash
        "bolt" -> Icons.Rounded.Bolt
        "timer" -> Icons.Rounded.Timer
        "compass" -> Icons.Rounded.Explore
        "door" -> Icons.Rounded.DoorFront
        "check" -> Icons.Rounded.TaskAlt
        "moon" -> Icons.Rounded.Bedtime
        else -> Icons.Rounded.Star
    }

/** Light and dark ends of each tier's metal. */
fun MedalTier.metal(): Pair<Color, Color> = when (this) {
    MedalTier.Bronze -> Color(0xFFE3A06A) to Color(0xFF8C5523)
    MedalTier.Silver -> Color(0xFFF2F4F8) to Color(0xFF8A90A0)
    MedalTier.Gold -> Color(0xFFFFE08A) to Color(0xFFC98E1E)
    MedalTier.Special -> Color(0xFFFF9466) to Color(0xFF6B5BD6)
}

/**
 * A medal drawn as a real coin: a ridged rim, a domed face with the icon pressed in, and a
 * band of light that slides across as the phone tilts. [spin] turns it on its vertical axis;
 * past 90° you see the back. Locked medals are dark silhouettes.
 */
@Composable
fun Medal(def: AchievementDef, unlocked: Boolean, size: Dp, modifier: Modifier = Modifier, spin: Float = 0f) {
    val tilt by rememberDeviceTilt()
    val reduced = LocalReducedMotion.current
    val shine by rememberInfiniteTransition(label = "shine").animateFloat(
        0f, 1f, infiniteRepeatable(tween(3600, easing = LinearEasing), RepeatMode.Restart), label = "s",
    )
    val (light, dark) = if (unlocked) def.tier.metal() else Color(0xFF3A3F5C) to Color(0xFF1A1E33)
    val angle = ((spin % 360f) + 360f) % 360f
    val back = angle in 90f..270f
    Box(
        modifier
            .size(size)
            .graphicsLayer {
                rotationY = spin + tilt.x * 14f
                rotationX = -tilt.y * 14f
                cameraDistance = 10f * density
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = this.size.minDimension / 2
            val c = center
            // A soft drop shadow under the coin.
            drawCircle(Color.Black.copy(alpha = 0.35f), r * 0.98f, c + Offset(0f, r * 0.06f))
            // Rim with milled ridges.
            drawCircle(Brush.sweepGradient(listOf(light, dark, light, dark, light), c), r, c)
            for (k in 0 until 48) {
                val a = k * (2 * PI / 48).toFloat()
                drawLine(dark.copy(alpha = 0.5f), c + Offset(cos(a), sin(a)) * (r * 0.9f), c + Offset(cos(a), sin(a)) * (r * 0.98f), r * 0.025f)
            }
            // Domed face.
            drawCircle(Brush.radialGradient(listOf(light, dark), c + Offset(-r * 0.3f, -r * 0.35f), r * 1.2f), r * 0.82f, c)
            drawCircle(dark.copy(alpha = 0.6f), r * 0.82f, c, style = Stroke(r * 0.03f))
            if (back) {
                // The back: a rising sun stamped in the metal.
                drawArc(dark.copy(alpha = 0.6f), 180f, 180f, true, c - Offset(r * 0.35f, r * 0.2f), androidx.compose.ui.geometry.Size(r * 0.7f, r * 0.7f))
                drawLine(dark.copy(alpha = 0.6f), c + Offset(-r * 0.45f, r * 0.17f), c + Offset(r * 0.45f, r * 0.17f), r * 0.06f)
            }
            if (unlocked && !reduced) {
                // The light band: drifts across on its own and leans with the phone.
                val x = (shine * 3f - 1f + tilt.x * 0.4f) * r * 2
                drawCircle(
                    Brush.linearGradient(
                        0f to Color.Transparent, 0.45f to Color.Transparent, 0.5f to Color.White.copy(alpha = 0.45f), 0.55f to Color.Transparent, 1f to Color.Transparent,
                        start = Offset(c.x - r + x, c.y - r), end = Offset(c.x + x, c.y + r),
                    ),
                    r * 0.82f, c,
                )
            }
        }
        if (!back) {
            // The icon, "embossed": a dark pressed shape with a light edge just above it.
            val icon = if (unlocked) def.iconVector else Icons.Rounded.Lock
            Icon(icon, null, tint = Color.White.copy(alpha = if (unlocked) 0.45f else 0.15f), modifier = Modifier.size(size * 0.42f).graphicsLayer { translationY = -1.5f * density })
            Icon(icon, def.title, tint = dark.copy(alpha = if (unlocked) 0.9f else 0.8f), modifier = Modifier.size(size * 0.42f))
        }
    }
}

/** A medal you can flick to spin; it slows with real momentum and settles face up. */
@Composable
fun SpinnableMedal(def: AchievementDef, unlocked: Boolean, size: Dp, modifier: Modifier = Modifier, entrance: Boolean = true) {
    val scope = rememberCoroutineScope()
    val spin = remember { Animatable(if (entrance) -540f else 0f) }
    LaunchedEffect(Unit) { if (entrance) spin.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 40f)) }
    val tracker = remember { VelocityTracker() }
    Medal(
        def, unlocked, size,
        modifier.pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragStart = { tracker.resetTracking() },
                onDragEnd = {
                    val v = tracker.calculateVelocity().x
                    scope.launch {
                        spin.animateDecay(v * 0.6f, exponentialDecay(frictionMultiplier = 0.6f))
                        // Settle on the nearest face so the icon ends up readable.
                        val target = (Math.round(spin.value / 360f) * 360f)
                        spin.animateTo(target, spring(dampingRatio = 0.6f, stiffness = 120f))
                    }
                },
            ) { change, dx ->
                tracker.addPosition(change.uptimeMillis, change.position)
                change.consume()
                scope.launch { spin.snapTo(spin.value + dx * 0.6f) }
            }
        },
        spin = spin.value,
    )
}
