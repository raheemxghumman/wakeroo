package com.wakeroo.app.alarm.ring

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakeroo.app.data.RingKind
import com.wakeroo.app.ui.brand.WakerooMark
import com.wakeroo.app.ui.scenes.Scene
import com.wakeroo.app.ui.scenes.SceneBackground
import com.wakeroo.app.ui.components.SkyBackground
import com.wakeroo.app.ui.components.press3d
import com.wakeroo.app.ui.components.rememberDeviceTilt
import com.wakeroo.app.ui.theme.ClockType
import com.wakeroo.app.ui.theme.DawnLavender
import com.wakeroo.app.ui.theme.LocalReducedMotion
import com.wakeroo.app.ui.theme.MorningGold
import com.wakeroo.app.ui.theme.Night
import com.wakeroo.app.ui.theme.Paper
import com.wakeroo.app.ui.theme.PaperMuted
import com.wakeroo.app.ui.theme.Sky
import com.wakeroo.app.ui.theme.Space
import com.wakeroo.app.ui.theme.SunriseCoral
import com.wakeroo.app.ui.theme.WakerooMotion
import com.wakeroo.app.ui.util.formatClock
import com.wakeroo.app.ui.util.formatMeridiem
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/** Ringing: one giant action, a breathing sunrise, and nothing else to tap by mistake. */
@Composable
fun RingScreen(state: RingState, onStartMission: () -> Unit, onSnooze: () -> Unit, scene: Scene = Scene.NightSky, topBar: @Composable () -> Unit = {}, actions: (@Composable () -> Unit)? = null, clock: LocalTime? = null) {
    val tilt by rememberDeviceTilt()
    val reduced = LocalReducedMotion.current
    val pulse by rememberInfiniteTransition(label = "ring").animateFloat(
        initialValue = 1f, targetValue = if (reduced) 1f else 1.045f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse",
    )
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }
    val time = clock ?: Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalTime()

    SceneBackground(scene, tilt = tilt, intensity = if (state.escalated) 1f else 0.4f) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = Space.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            topBar()
            Spacer(Modifier.weight(0.9f))
            // The clock floats on its own 3D layer, leaning gently with the phone.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    rotationY = tilt.x * 6f
                    rotationX = -tilt.y * 6f
                    translationX = tilt.x * 10f
                    cameraDistance = 14f * density
                },
            ) {
                Text(time.formatMeridiem(), style = MaterialTheme.typography.titleMedium, color = DawnLavender)
                Text(time.formatClock(), style = ClockType.ring, color = Paper, maxLines = 1)
                Spacer(Modifier.height(Space.md))
                val label = when (state.kind) {
                    RingKind.Test -> "Test alarm: finish the mission to pass"
                    else -> state.alarm.label.ifBlank { "Time to get up" }
                }
                Text(label, style = MaterialTheme.typography.headlineMedium, color = Paper, textAlign = TextAlign.Center)
                MissionSummary(state)
                EscalationHint(state, now)
            }
            Spacer(Modifier.weight(1f))
            if (actions != null) {
                actions()
                Spacer(Modifier.height(Space.xxl))
                return@Column
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .graphicsLayer { scaleX = pulse; scaleY = pulse }
                    .press3d()
                    .clip(RoundedCornerShape(44.dp))
                    .background(Brush.horizontalGradient(listOf(SunriseCoral, Color(0xFFFF9466))))
                    .clickable(role = Role.Button, onClick = onStartMission)
                    .semantics { contentDescription = "Start mission to stop the alarm" },
                contentAlignment = Alignment.Center,
            ) {
                Text("Start mission", style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp), color = Night)
            }
            Spacer(Modifier.height(Space.lg))
            if (state.snoozesLeft > 0 && state.kind == RingKind.Alarm) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .press3d()
                        .clip(RoundedCornerShape(32.dp))
                        .border(1.5.dp, Paper.copy(alpha = 0.25f), RoundedCornerShape(32.dp))
                        .clickable(role = Role.Button, onClick = onSnooze),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Snooze ${state.alarm.snoozeMinutes} min  ·  ${state.snoozesLeft} left",
                        style = MaterialTheme.typography.titleMedium, color = Paper,
                    )
                }
            } else {
                Text("No snoozing this one. You've got this.", style = MaterialTheme.typography.bodyMedium, color = PaperMuted)
            }
            Spacer(Modifier.height(Space.xxl))
        }
    }
}

/** "Getting louder in 0:42": an Escalate alarm warns before it switches to the loud tone. */
@Composable
private fun EscalationHint(state: RingState, now: Long) {
    val text = when {
        state.escalated -> "Full volume. Start your mission to quiet it"
        state.escalateAt > 0 -> {
            val left = ((state.escalateAt - now) / 1000).coerceAtLeast(0)
            "Getting louder in %d:%02d".format(left / 60, left % 60)
        }
        else -> return
    }
    Text(
        text, style = MaterialTheme.typography.labelLarge, color = if (state.escalated) SunriseCoral else DawnLavender,
        modifier = Modifier.padding(top = Space.md).clip(CircleShape).background(Night.copy(alpha = 0.4f)).padding(horizontal = Space.lg, vertical = Space.sm),
    )
}

@Composable
private fun MissionSummary(state: RingState) {
    val names = state.alarm.missions.joinToString("  ·  ") { it.type.title }
    if (names.isNotEmpty()) {
        Text(
            names, style = MaterialTheme.typography.labelLarge, color = MorningGold,
            modifier = Modifier.padding(top = Space.md),
        )
    }
}

/** "Still up?": one huge button and a countdown ring. Ignore it and the full alarm returns. */
@Composable
fun WakeCheckScreen(state: RingState, onConfirm: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(100); now = System.currentTimeMillis() } }
    val remaining = ((state.wakeCheckDeadline - now) / RingService.WAKE_CHECK_WINDOW_MS.toFloat()).coerceIn(0f, 1f)
    SkyBackground(Sky.Sunrise) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(Space.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Still up?", style = MaterialTheme.typography.displayMedium, color = Paper)
            Text(
                "Tap before the ring runs out, or your alarm comes back.",
                style = MaterialTheme.typography.bodyLarge, color = Paper.copy(alpha = 0.85f),
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = Space.sm, bottom = Space.xxl),
            )
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(260.dp)) {
                val track = Paper.copy(alpha = 0.18f)
                Canvas(Modifier.fillMaxSize()) {
                    drawArc(track, 0f, 360f, false, style = Stroke(14.dp.toPx()))
                    drawArc(MorningGold, -90f, 360f * remaining, false, style = Stroke(14.dp.toPx(), cap = StrokeCap.Round))
                }
                Box(
                    Modifier
                        .size(200.dp)
                        .press3d(maxTilt = 12f)
                        .clip(CircleShape)
                        .background(Paper)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button,
                        ) {
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            onConfirm()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("I'm up", style = MaterialTheme.typography.displaySmall, color = Night)
                }
            }
        }
    }
}

/** A coral splash grows from the button, the sun lands, and the streak counts up. */
@Composable
fun GoodMorningScreen(result: FinishedRing, onDone: () -> Unit) {
    val splash = remember { Animatable(0f) }
    val content = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        splash.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        content.animateTo(1f, WakerooMotion.bouncy())
        delay(4500)
        onDone()
    }
    val time = LocalTime.now()
    Box(Modifier.fillMaxSize().background(Night).clickable(onClick = onDone)) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.maxDimension * 1.2f * splash.value
            drawCircle(
                Brush.radialGradient(listOf(MorningGold, SunriseCoral), center = Offset(size.width / 2, size.height * 0.85f), radius = r.coerceAtLeast(1f)),
                radius = r, center = Offset(size.width / 2, size.height * 0.85f),
            )
        }
        Column(
            Modifier.fillMaxSize().statusBarsPadding().padding(Space.xl).graphicsLayer {
                alpha = content.value.coerceIn(0f, 1f)
                translationY = (1 - content.value) * 80f
            },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            WakerooMark(Modifier.size(180.dp), horizonColor = Night, sunColor = Paper, leapColor = Night.copy(alpha = 0.7f), dotColor = Paper)
            Text(
                if (result.state.kind == RingKind.Test) "Test passed" else "Good morning",
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold), color = Night,
            )
            Text(
                if (result.state.kind == RingKind.Test) "Your alarms will ring like this, even when the phone is locked."
                else "Up at ${time.formatClock()} ${time.formatMeridiem()}  ·  awake in ${formatDuration(result.secondsToWake)}",
                style = MaterialTheme.typography.titleMedium, color = Night.copy(alpha = 0.8f),
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = Space.sm),
            )
            if (result.state.alarm.label.isNotBlank() && result.state.kind == RingKind.Alarm) {
                Text(
                    "Now: ${result.state.alarm.label}", style = MaterialTheme.typography.bodyLarge, color = Night,
                    modifier = Modifier.padding(top = Space.xl),
                )
            }
        }
    }
}

private fun formatDuration(seconds: Int): String = if (seconds < 60) "${seconds}s" else "${seconds / 60} min ${seconds % 60}s"
