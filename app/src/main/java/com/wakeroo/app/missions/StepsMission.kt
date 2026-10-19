package com.wakeroo.app.missions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.wakeroo.app.BuildConfig
import com.wakeroo.app.data.model.MissionConfig
import com.wakeroo.app.ui.theme.ClockType
import com.wakeroo.app.ui.theme.DawnLavender
import com.wakeroo.app.ui.theme.MorningGold
import com.wakeroo.app.ui.theme.Paper
import com.wakeroo.app.ui.theme.PaperMuted
import com.wakeroo.app.ui.theme.Space
import com.wakeroo.app.ui.theme.SunriseCoral
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val DRAIN_AFTER_MS = 10_000L
private const val SWITCH_TO_HEURISTIC_MS = 20_000L

fun Context.hasMotionPermission(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

fun Context.canCountSteps(): Boolean {
    val sm = getSystemService(SensorManager::class.java) ?: return false
    return sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null || sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
}

/**
 * Walking steps from raw acceleration, for phones whose step detector is missing, slow or
 * asleep. A step is a bounce of moderate size at a walking cadence; a wild shake is too big
 * and too fast to count, so this can't be gamed by shaking the phone in bed.
 */
class StepHeuristic {
    private var smooth = SensorManager.GRAVITY_EARTH
    private var rising = false
    private var peak = 0f
    private var lastStepAt = 0L

    /** Feed one accelerometer sample; returns true when it completes a step. */
    fun onSample(x: Float, y: Float, z: Float, now: Long): Boolean {
        val magnitude = sqrt(x * x + y * y + z * z)
        smooth += (magnitude - smooth) * 0.25f
        val delta = smooth - SensorManager.GRAVITY_EARTH
        if (delta > peak) peak = delta
        if (!rising && delta > MIN_BOUNCE) rising = true
        if (rising && delta < 0f) {
            rising = false
            val p = peak
            peak = 0f
            val gap = now - lastStepAt
            if (p in MIN_BOUNCE..MAX_BOUNCE && gap >= MIN_GAP_MS) {
                lastStepAt = now
                return true
            }
        }
        return false
    }

    companion object {
        const val MIN_BOUNCE = 1.1f
        const val MAX_BOUNCE = 7.5f
        const val MIN_GAP_MS = 280L
    }
}

@Composable
fun StepsMission(config: MissionConfig, onActivity: () -> Unit, onProgress: (Float) -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val target = config.count.coerceIn(10, 500)
    var steps by remember { mutableIntStateOf(0) }
    var lastStep by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val bump = remember { Animatable(0f) }
    val done by rememberUpdatedState(onDone)
    val activity by rememberUpdatedState(onActivity)
    val progress by rememberUpdatedState(onProgress)

    fun step() {
        if (steps >= target) return
        steps++
        lastStep = System.currentTimeMillis()
        activity()
        progress(steps / target.toFloat())
        if (steps % 5 == 0) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        scope.launch { bump.snapTo(1f); bump.animateTo(0f, spring(0.4f, 500f)) }
        if (steps >= target) {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            done()
        }
    }

    DisposableEffect(Unit) {
        val sm = context.getSystemService(SensorManager::class.java)
        val detector = sm?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        val accel = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val heuristic = StepHeuristic()
        val startedAt = System.currentTimeMillis()
        var detectorSeen = false
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val now = System.currentTimeMillis()
                when (e.sensor.type) {
                    Sensor.TYPE_STEP_DETECTOR -> { detectorSeen = true; step() }
                    Sensor.TYPE_ACCELEROMETER -> {
                        // The hardware detector wins when it works; otherwise fall back to ours.
                        val useHeuristic = detector == null || (!detectorSeen && now - startedAt > SWITCH_TO_HEURISTIC_MS)
                        if (heuristic.onSample(e.values[0], e.values[1], e.values[2], now) && useHeuristic) step()
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        detector?.let { sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_FASTEST) }
        accel?.let { sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
        onDispose { sm?.unregisterListener(listener) }
    }

    // Stand still and the track slowly drains: the alarm wants you moving, not leaning on a wall.
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            if (steps in 1 until target && System.currentTimeMillis() - lastStep > DRAIN_AFTER_MS) {
                steps--
                onProgress(steps / target.toFloat())
            }
        }
    }

    val fill by animateFloatAsState(steps / target.toFloat(), spring(0.7f, 180f), label = "track")
    val stride by rememberInfiniteTransition(label = "stride").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "s",
    )
    val gold = MorningGold
    val coral = SunriseCoral
    val trackColor = Paper.copy(alpha = 0.12f)

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Get up and walk", style = MaterialTheme.typography.labelLarge, color = DawnLavender)
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .then(if (BuildConfig.DEBUG) Modifier.clickable(remember { MutableInteractionSource() }, null) { step() } else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            // A running track seen in perspective: the oval tilts away from you like a stadium.
            Canvas(Modifier.fillMaxSize().graphicsLayer { rotationX = 52f; cameraDistance = 9f * density }) {
                val c = Offset(size.width / 2, size.height / 2)
                val rx = size.width * 0.44f
                val ry = size.height * 0.40f
                val width = 26.dp.toPx()
                drawOval(trackColor, Offset(c.x - rx, c.y - ry), Size(rx * 2, ry * 2), style = Stroke(width))
                drawArc(
                    Brush.sweepGradient(listOf(gold, coral, gold), center = c),
                    -90f, 360f * fill, false, Offset(c.x - rx, c.y - ry), Size(rx * 2, ry * 2),
                    style = Stroke(width, cap = StrokeCap.Round),
                )
                // Lane line and footprints marching along the filled part.
                drawOval(Color.White.copy(alpha = 0.08f), Offset(c.x - rx * 0.8f, c.y - ry * 0.8f), Size(rx * 1.6f, ry * 1.6f), style = Stroke(2.dp.toPx()))
                val prints = 28
                for (i in 0 until prints) {
                    val t = i / prints.toFloat()
                    if (t > fill) break
                    val a = (-PI / 2 + t * 2 * PI).toFloat()
                    val side = if (i % 2 == 0) 0.92f else 1.08f
                    val p = Offset(c.x + cos(a) * rx * side, c.y + sin(a) * ry * side)
                    val heading = atan2(cos(a) * ry, -sin(a) * rx) * 180f / PI.toFloat()
                    footprint(p, heading, 0.9f)
                }
                // A runner dot at the head of the fill, bobbing with the stride.
                val a = (-PI / 2 + fill * 2 * PI).toFloat()
                val bob = sin(stride * 2 * PI).toFloat() * 3.dp.toPx()
                drawCircle(Color.White, 9.dp.toPx(), Offset(c.x + cos(a) * rx, c.y + sin(a) * ry + bob))
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { val s = 1f + bump.value * 0.08f; scaleX = s; scaleY = s },
            ) {
                Text("$steps", style = ClockType.hero, color = Paper)
                Text("of $target steps", style = MaterialTheme.typography.titleMedium, color = PaperMuted)
            }
        }
        Spacer(Modifier.height(Space.lg))
        val idle = System.currentTimeMillis() - lastStep > 4000
        Text(
            when {
                steps == 0 -> "Walk to another room. Your steps count as you go."
                idle -> "Keep walking. Stand still and the track drains"
                else -> "Nice pace. Keep going"
            },
            style = MaterialTheme.typography.bodyLarge, color = PaperMuted, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
    }
}

/** A small rounded footprint: sole plus toe dots, rotated to the walking direction. */
private fun DrawScope.footprint(at: Offset, degrees: Float, alpha: Float) {
    val s = 5.dp.toPx()
    rotate(degrees, at) {
        drawOval(MorningGold.copy(alpha = alpha), Offset(at.x - s * 0.7f, at.y - s * 1.2f), Size(s * 1.4f, s * 2.4f))
        drawCircle(MorningGold.copy(alpha = alpha), s * 0.35f, Offset(at.x, at.y - s * 1.8f))
    }
}
