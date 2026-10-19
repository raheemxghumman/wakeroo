package com.wakeroo.app.missions

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.wakeroo.app.BuildConfig
import com.wakeroo.app.data.model.MissionConfig
import com.wakeroo.app.ui.theme.ClockType
import com.wakeroo.app.ui.theme.DawnLavender
import com.wakeroo.app.ui.theme.MorningGold
import com.wakeroo.app.ui.theme.Night
import com.wakeroo.app.ui.theme.Paper
import com.wakeroo.app.ui.theme.PaperMuted
import com.wakeroo.app.ui.theme.Space
import com.wakeroo.app.ui.theme.SunriseCoral
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

private const val SHAKE_THRESHOLD = 13f   // m/s² of linear acceleration; tuned to need a real shake
private const val SHAKE_DEBOUNCE_MS = 250L
private const val DECAY_AFTER_MS = 5_000L

@Composable
fun ShakeMission(config: MissionConfig, onActivity: () -> Unit, onProgress: (Float) -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val target = config.count.coerceAtLeast(5)
    var count by remember { mutableIntStateOf(0) }
    var lastShake by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val pop = remember { Animatable(1f) }
    val wobble = remember { Animatable(0f) }
    val latestDone by rememberUpdatedState(onDone)
    val latestActivity by rememberUpdatedState(onActivity)

    fun registerShake() {
        if (count >= target) return
        count++
        lastShake = System.currentTimeMillis()
        latestActivity()
        onProgress(count / target.toFloat())
        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        scope.launch { pop.snapTo(1.08f); pop.animateTo(1f, spring(0.35f, 600f)) }
        scope.launch { wobble.animateTo(0f, spring(0.3f, 300f), initialVelocity = if (count % 2 == 0) 120f else -120f) }
        if (count >= target) {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            latestDone()
        }
    }

    DisposableEffect(Unit) {
        val sm = context.getSystemService(SensorManager::class.java)
        val linear = sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        val sensor = linear ?: sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var last = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val (x, y, z) = Triple(e.values[0], e.values[1], e.values[2])
                var magnitude = sqrt(x * x + y * y + z * z)
                if (linear == null) magnitude = kotlin.math.abs(magnitude - SensorManager.GRAVITY_EARTH)
                val now = System.currentTimeMillis()
                if (magnitude > SHAKE_THRESHOLD && now - last > SHAKE_DEBOUNCE_MS) {
                    last = now
                    registerShake()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensor?.let { sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
        onDispose { sm.unregisterListener(listener) }
    }

    // Stop and the light drains back out: no resting halfway.
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            if (count in 1 until target && System.currentTimeMillis() - lastShake > DECAY_AFTER_MS) {
                count--
                onProgress(count / target.toFloat())
            }
        }
    }

    val fill by animateFloatAsState(count / target.toFloat(), spring(0.6f, 200f), label = "fill")
    val wave by rememberInfiniteTransition(label = "wave").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart), label = "w",
    )

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Shake your phone", style = MaterialTheme.typography.labelLarge, color = DawnLavender)
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .size(280.dp)
                .graphicsLayer {
                    scaleX = pop.value; scaleY = pop.value
                    rotationZ = wobble.value / 12f
                    rotationY = wobble.value / 6f
                    cameraDistance = 10f * density
                }
                .then(
                    if (BuildConfig.DEBUG) Modifier.clickable(remember { MutableInteractionSource() }, null) { registerShake() } else Modifier,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension / 2
                val c = Offset(size.width / 2, size.height / 2)
                // The orb: a shaded sphere with a soft highlight, so it reads as a 3D object.
                drawCircle(Brush.radialGradient(listOf(Color(0xFF2A2F4D), Night), center = c + Offset(-r * 0.3f, -r * 0.35f), radius = r * 1.3f), r, c)
                val orb = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, r)) }
                clipPath(orb) {
                    val level = c.y + r - fill * 2 * r
                    val liquid = Path().apply {
                        moveTo(0f, size.height)
                        lineTo(0f, level)
                        var x = 0f
                        while (x <= size.width) {
                            val y = level + sin((x / size.width * 2 + wave) * 2 * PI).toFloat() * r * 0.06f * (1 - fill * 0.6f)
                            lineTo(x, y); x += 8f
                        }
                        lineTo(size.width, size.height)
                        close()
                    }
                    drawPath(liquid, Brush.verticalGradient(listOf(MorningGold, SunriseCoral), startY = level, endY = c.y + r))
                }
                drawCircle(
                    Brush.radialGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent), center = c + Offset(-r * 0.38f, -r * 0.42f), radius = r * 0.55f),
                    r, c,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$count", style = ClockType.hero, color = Paper)
                Text("of $target", style = MaterialTheme.typography.titleMedium, color = Paper.copy(alpha = 0.8f))
            }
        }
        Spacer(Modifier.height(Space.xl))
        Text(
            if (count == 0) "Give it a proper shake" else "Keep going. Stop and it drains",
            style = MaterialTheme.typography.bodyLarge, color = PaperMuted,
        )
        Spacer(Modifier.weight(1f))
    }
}
