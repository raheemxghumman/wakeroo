package com.wakeroo.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.launch

/**
 * A physical press: the element sinks and tilts toward the finger in 3D, then springs back.
 * Purely visual; pair it with a clickable for semantics.
 */
fun Modifier.press3d(maxTilt: Float = 9f, sink: Float = 0.96f): Modifier = composed {
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    val rx = remember { Animatable(0f) }
    val ry = remember { Animatable(0f) }
    val size = remember { arrayOf(IntSize.Zero) }
    val springy = spring<Float>(dampingRatio = 0.5f, stiffness = 500f)
    this
        .onSizeChanged { size[0] = it }
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val s = size[0]
                val rel = if (s.width > 0) Offset(down.position.x / s.width - 0.5f, down.position.y / s.height - 0.5f) else Offset.Zero
                scope.launch { scale.animateTo(sink, springy) }
                scope.launch { rx.animateTo(-rel.y * 2 * maxTilt, springy) }
                scope.launch { ry.animateTo(rel.x * 2 * maxTilt, springy) }
                waitForUpOrCancellation()
                scope.launch { scale.animateTo(1f, springy) }
                scope.launch { rx.animateTo(0f, springy) }
                scope.launch { ry.animateTo(0f, springy) }
            }
        }
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
            rotationX = rx.value
            rotationY = ry.value
            cameraDistance = 12f * density
        }
}
