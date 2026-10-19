package com.wakeroo.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import com.wakeroo.app.ui.theme.LocalReducedMotion
import kotlinx.coroutines.delay

/**
 * True once the launch intro has handed over to the app. Screens wait for it so their
 * elements assemble in sequence instead of popping in behind the intro.
 */
val LocalEntranceGate = compositionLocalOf { true }

enum class EntranceStyle { Rise, Flip, Pop }

/**
 * Plays a staggered entrance when the element first appears: [order] sets the delay, so a
 * screen builds itself top to bottom. Each element runs once; recompositions don't replay it.
 */
fun Modifier.entrance(order: Int, style: EntranceStyle = EntranceStyle.Rise): Modifier = composed {
    val gate = LocalEntranceGate.current
    val reduced = LocalReducedMotion.current
    val progress = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(gate) {
        if (!gate || progress.value >= 1f) return@LaunchedEffect
        delay(70L * order)
        progress.animateTo(1f, spring(dampingRatio = if (style == EntranceStyle.Pop) 0.45f else 0.72f, stiffness = 260f))
    }
    graphicsLayer {
        val p = progress.value
        alpha = p.coerceIn(0f, 1f)
        when (style) {
            EntranceStyle.Rise -> {
                translationY = (1 - p) * 90f
                rotationX = (1 - p) * 18f
            }
            EntranceStyle.Flip -> {
                rotationX = (1 - p) * -70f
                translationY = (1 - p) * 40f
                transformOrigin = TransformOrigin(0.5f, 0f)
            }
            EntranceStyle.Pop -> {
                scaleX = 0.4f + 0.6f * p
                scaleY = 0.4f + 0.6f * p
            }
        }
        cameraDistance = 14f * density
    }
}
