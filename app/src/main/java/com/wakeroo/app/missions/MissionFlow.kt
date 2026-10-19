package com.wakeroo.app.missions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.UserManagerCompat
import com.wakeroo.app.alarm.ring.RingState
import com.wakeroo.app.camera.hasCameraPermission
import com.wakeroo.app.data.model.MissionConfig
import com.wakeroo.app.data.model.MissionType
import com.wakeroo.app.ui.components.SkyBackground
import com.wakeroo.app.ui.components.press3d
import com.wakeroo.app.ui.theme.Alert
import com.wakeroo.app.ui.theme.DawnLavender
import com.wakeroo.app.ui.theme.MorningGold
import com.wakeroo.app.ui.theme.Night
import com.wakeroo.app.ui.theme.NightSurface
import com.wakeroo.app.ui.theme.Paper
import com.wakeroo.app.ui.theme.PaperMuted
import com.wakeroo.app.ui.theme.Sky
import com.wakeroo.app.ui.theme.Space
import com.wakeroo.app.ui.theme.SunriseCoral
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val BACKUP_MISSION = MissionConfig(MissionType.Math, difficulty = 3, count = 5)
private const val FALLBACK_AFTER_MS = 60_000L
private const val FALLBACK_AFTER_FAILS = 3

/** Runs an alarm's missions in order, then calls [onDone]. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MissionFlow(state: RingState, onActivity: () -> Unit, onDone: () -> Unit, onEscape: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val missions = remember {
        mutableStateListOf<MissionConfig>().apply {
            addAll(
                state.alarm.missions.ifEmpty { listOf(MissionConfig(MissionType.Math)) }.map { m ->
                    // A mission that can't work right now is swapped for the backup up front.
                    val cameraless = m.type in setOf(MissionType.Scan, MissionType.Photo) && !context.hasCameraPermission()
                    val photoLocked = m.type == MissionType.Photo && (m.photoHash == null || !UserManagerCompat.isUserUnlocked(context))
                    if (cameraless || photoLocked) BACKUP_MISSION else m
                },
            )
        }
    }
    var index by remember { mutableIntStateOf(0) }
    var shown by remember { mutableIntStateOf(0) }
    var missionProgress by remember { mutableFloatStateOf(0f) }
    var failures by remember { mutableIntStateOf(0) }
    var startedAt by remember { mutableStateOf(System.currentTimeMillis()) }
    var showEmergency by remember { mutableStateOf(false) }
    var offerBackup by remember { mutableStateOf(false) }
    val flip = remember { Animatable(0f) }

    val overall by animateFloatAsState((index + missionProgress) / missions.size, label = "overall")

    // A 3D card turn between missions: the old one swings away, the next swings in.
    fun advance() {
        if (index + 1 >= missions.size) return onDone()
        scope.launch {
            flip.animateTo(90f, tween(220))
            index++; shown = index; missionProgress = 0f; failures = 0; offerBackup = false
            startedAt = System.currentTimeMillis()
            flip.snapTo(-90f)
            flip.animateTo(0f, tween(260))
        }
    }

    val current = missions[shown]
    LaunchedEffect(shown) {
        if (current.type in setOf(MissionType.Scan, MissionType.Photo)) {
            delay(FALLBACK_AFTER_MS)
            offerBackup = true
        }
    }

    SkyBackground(Sky.Night) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = Space.xl)) {
            Row(Modifier.fillMaxWidth().padding(top = Space.lg), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (missions.size > 1) "Mission ${index + 1} of ${missions.size}" else "Your mission",
                        style = MaterialTheme.typography.labelMedium, color = PaperMuted,
                    )
                    Text(current.type.title, style = MaterialTheme.typography.headlineMedium, color = Paper)
                }
                Text(
                    "Emergency",
                    style = MaterialTheme.typography.labelMedium, color = PaperMuted,
                    modifier = Modifier.clip(CircleShape).clickable { showEmergency = true }.padding(Space.md),
                )
            }
            LinearWavyProgressIndicator(
                progress = { overall },
                modifier = Modifier.fillMaxWidth().padding(vertical = Space.lg),
                color = MorningGold,
                trackColor = Paper.copy(alpha = 0.12f),
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .graphicsLayer { rotationY = flip.value; cameraDistance = 16f * density },
            ) {
                val onProgress: (Float) -> Unit = { missionProgress = it }
                when (current.type) {
                    MissionType.Math -> MathMission(current, onActivity, onProgress, ::advance)
                    MissionType.Typing -> TypingMission(current, onActivity, onProgress, ::advance)
                    MissionType.Shake -> ShakeMission(current, onActivity, onProgress, ::advance)
                    MissionType.Scan -> ScanMission(current, onActivity, onFailedAttempt = {
                        failures++
                        if (failures >= FALLBACK_AFTER_FAILS) offerBackup = true
                    }, onDone = ::advance)
                    MissionType.Photo -> PhotoMission(current, onActivity, ::advance)
                }
            }
            AnimatedVisibility(offerBackup, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = Space.lg)
                        .height(60.dp)
                        .press3d()
                        .clip(RoundedCornerShape(30.dp))
                        .border(1.5.dp, DawnLavender, RoundedCornerShape(30.dp))
                        .clickable {
                            missions[shown] = BACKUP_MISSION
                            offerBackup = false
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Can't do this one? Switch to the backup mission", style = MaterialTheme.typography.labelLarge, color = DawnLavender)
                }
            }
            Spacer(Modifier.height(Space.lg))
        }
        if (showEmergency) EmergencyExit(onCancel = { showEmergency = false }, onEscape = onEscape)
    }
}

/**
 * The humane way out for real emergencies: hold for 15 seconds while the alarm keeps ringing,
 * then type "i am awake". It works, but it's never quicker than the mission.
 */
@Composable
private fun EmergencyExit(onCancel: () -> Unit, onEscape: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val hold = remember { Animatable(0f) }
    var unlocked by remember { mutableStateOf(false) }
    var typed by remember { mutableStateOf("") }
    val phrase = "i am awake"

    Box(
        Modifier
            .fillMaxSize()
            .background(Night.copy(alpha = 0.94f))
            .clickable(remember { MutableInteractionSource() }, null) { },
    ) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(Space.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Only for real emergencies", style = MaterialTheme.typography.headlineMedium, color = Paper)
            Text(
                "The alarm keeps ringing. This breaks your streak and is logged.",
                style = MaterialTheme.typography.bodyMedium, color = PaperMuted, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Space.sm),
            )
            Spacer(Modifier.weight(1f))
            if (!unlocked) {
                Box(
                    Modifier
                        .size(220.dp)
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown()
                                val job = scope.launch {
                                    hold.animateTo(1f, tween(((1 - hold.value) * 15_000).toInt(), easing = LinearEasing))
                                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                    unlocked = true
                                }
                                waitForUpOrCancellation()
                                if (!unlocked) {
                                    job.cancel()
                                    scope.launch { hold.animateTo(0f, tween(500)) }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawArc(Paper.copy(alpha = 0.15f), 0f, 360f, false, style = Stroke(12.dp.toPx()))
                        drawArc(Alert, -90f, 360f * hold.value, false, style = Stroke(12.dp.toPx(), cap = StrokeCap.Round))
                    }
                    Box(Modifier.size(170.dp).clip(CircleShape).background(NightSurface), contentAlignment = Alignment.Center) {
                        Text(
                            if (hold.value > 0f) "${(15 - hold.value * 15).toInt() + 1}s" else "Hold",
                            style = MaterialTheme.typography.displaySmall, color = Paper,
                        )
                    }
                }
            } else {
                Text("Type: $phrase", style = MaterialTheme.typography.titleMedium, color = DawnLavender)
                Text(
                    typed.ifEmpty { " " }, style = MaterialTheme.typography.headlineLarge, color = MorningGold,
                    modifier = Modifier.padding(vertical = Space.lg),
                )
                LetterPad(onKey = { key ->
                    when (key) {
                        is Key.Char -> typed += key.c
                        Key.Space -> typed += " "
                        Key.Delete -> typed = typed.dropLast(1)
                        Key.Enter -> Unit
                    }
                    if (TypingPhrases.matches(phrase, typed)) onEscape()
                })
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Back to my mission",
                    style = MaterialTheme.typography.titleMedium, color = SunriseCoral,
                    modifier = Modifier.clip(CircleShape).clickable(onClick = onCancel).padding(Space.lg),
                )
            }
        }
    }
}
