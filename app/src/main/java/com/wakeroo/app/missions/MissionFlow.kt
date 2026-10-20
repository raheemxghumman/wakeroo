package com.wakeroo.app.missions

import com.wakeroo.app.ui.i18n.tr
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
/** Stands in for a mission that isn't set up yet ("I'll set it up tonight"). */
private val SETUP_FALLBACK = MissionConfig(MissionType.Math)
private const val FALLBACK_AFTER_MS = 60_000L
private const val FALLBACK_AFTER_FAILS = 3

/** Missions you walk somewhere for: they can take a while and always offer the backup. */
private val AWAY_MISSIONS = setOf(MissionType.Scan, MissionType.Photo, MissionType.Hunt, MissionType.Nfc, MissionType.Steps)

/** Missions whose progress resets when you stop for longer than the time limit. */
private val RESETTABLE = setOf(MissionType.Math, MissionType.Typing, MissionType.Shake, MissionType.Memory, MissionType.Plan)

/** Swaps any mission that can't work on this phone right now for one that can. */
fun runnableMission(context: Context, m: MissionConfig): MissionConfig = when {
    !m.isReady -> SETUP_FALLBACK
    m.type.usesCamera && !context.hasCameraPermission() -> BACKUP_MISSION
    m.type == MissionType.Photo && !UserManagerCompat.isUserUnlocked(context) -> BACKUP_MISSION
    m.type == MissionType.Steps && (!context.hasMotionPermission() || !context.canCountSteps()) -> BACKUP_MISSION
    m.type == MissionType.Nfc && context.nfcState() == NfcState.Missing && !nfcSimulated -> BACKUP_MISSION
    else -> m
}

/**
 * Runs an alarm's missions in order, then calls [onDone]. If the user stops touching the
 * mission for [timeLimitSeconds], [onIdle] brings the alarm back to full volume and the
 * current mission starts over.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MissionFlow(
    state: RingState,
    onActivity: () -> Unit,
    onDone: () -> Unit,
    onEscape: () -> Unit,
    timeLimitSeconds: Int = 20,
    onIdle: () -> Unit = {},
    onPlan: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val missions = remember {
        mutableStateListOf<MissionConfig>().apply {
            addAll(state.alarm.missions.ifEmpty { listOf(MissionConfig(MissionType.Math)) }.map { runnableMission(context, it) })
        }
    }
    var index by remember { mutableIntStateOf(0) }
    var shown by remember { mutableIntStateOf(0) }
    var missionProgress by remember { mutableFloatStateOf(0f) }
    var failures by remember { mutableIntStateOf(0) }
    var showEmergency by remember { mutableStateOf(false) }
    var offerBackup by remember { mutableStateOf(false) }
    var restarts by remember { mutableIntStateOf(0) }
    var lastActivity by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var idle by remember { mutableStateOf(false) }
    var idleReset by remember { mutableStateOf(false) }
    val flip = remember { Animatable(0f) }
    val nudge = remember { Animatable(0f) }

    val overall by animateFloatAsState((index + missionProgress) / missions.size, label = "overall")

    val activity: () -> Unit = {
        lastActivity = System.currentTimeMillis()
        idle = false
        onActivity()
    }

    // A 3D card turn between missions: the old one swings away, the next swings in.
    fun advance() {
        if (index + 1 >= missions.size) return onDone()
        scope.launch {
            flip.animateTo(90f, tween(220))
            index++; shown = index; missionProgress = 0f; failures = 0; offerBackup = false
            lastActivity = System.currentTimeMillis()
            flip.snapTo(-90f)
            flip.animateTo(0f, tween(260))
        }
    }

    val current = missions[shown]
    LaunchedEffect(shown, current.type) {
        if (current.type in AWAY_MISSIONS) {
            delay(FALLBACK_AFTER_MS)
            offerBackup = true
        }
    }

    // The time limit: lying there with the mission open doesn't keep the alarm quiet.
    LaunchedEffect(timeLimitSeconds) {
        if (timeLimitSeconds <= 0) return@LaunchedEffect
        while (true) {
            delay(500)
            if (!idle && System.currentTimeMillis() - lastActivity > timeLimitSeconds * 1000L) {
                idle = true
                onIdle()
                idleReset = missions[shown].type in RESETTABLE && missionProgress > 0f
                if (idleReset) {
                    missionProgress = 0f
                    restarts++
                }
                launch { nudge.snapTo(0f); nudge.animateTo(0f, spring(0.25f, 700f), initialVelocity = 2200f) }
            }
        }
    }

    SkyBackground(Sky.Night) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = Space.xl)) {
            Row(Modifier.fillMaxWidth().padding(top = Space.lg), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (missions.size > 1) tr("Mission {0} of {1}", index + 1, missions.size) else tr("Your mission"),
                        style = MaterialTheme.typography.labelMedium, color = PaperMuted,
                    )
                    Text(current.type.title, style = MaterialTheme.typography.headlineMedium, color = Paper)
                }
                Text(
                    tr("Emergency"),
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
            AnimatedVisibility(idle, enter = slideInVertically { -it } + fadeIn(), exit = slideOutVertically { -it } + fadeOut()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = Space.md)
                        .graphicsLayer { translationX = nudge.value * 0.2f }
                        .clip(RoundedCornerShape(20.dp))
                        .background(SunriseCoral)
                        .padding(horizontal = Space.lg, vertical = Space.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (idleReset) tr("Too quiet! Full volume's back. Start again") else tr("Too quiet! Full volume's back. Keep going"),
                        style = MaterialTheme.typography.labelLarge, color = Night,
                    )
                }
            }
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .graphicsLayer { rotationY = flip.value; cameraDistance = 16f * density },
            ) {
                val onProgress: (Float) -> Unit = { missionProgress = it }
                val onFailed: () -> Unit = {
                    failures++
                    if (failures >= FALLBACK_AFTER_FAILS) offerBackup = true
                }
                // A new key restarts the mission from scratch after the time limit.
                key(shown, restarts, current) {
                    when (current.type) {
                        MissionType.Math -> MathMission(current, activity, onProgress, ::advance)
                        MissionType.Typing -> TypingMission(current, activity, onProgress, ::advance)
                        MissionType.Shake -> ShakeMission(current, activity, onProgress, ::advance)
                        MissionType.Scan -> ScanMission(current, activity, onFailedAttempt = onFailed, onDone = ::advance)
                        MissionType.Photo -> PhotoMission(current, activity, ::advance)
                        MissionType.Steps -> StepsMission(current, activity, onProgress, ::advance)
                        MissionType.Memory -> MemoryMission(current, activity, onProgress, ::advance)
                        MissionType.Nfc -> NfcMission(current, activity, onFailedAttempt = onFailed, onDone = ::advance)
                        MissionType.Plan -> PlanMission(current, activity, onProgress, ::advance, onPlan)
                        MissionType.Hunt -> HuntMission(current, activity, onProgress, ::advance)
                    }
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
                            missionProgress = 0f
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(tr("Can't do this one? Switch to the backup mission"), style = MaterialTheme.typography.labelLarge, color = DawnLavender)
                }
            }
            Spacer(Modifier.height(Space.lg))
        }
        if (showEmergency) {
            EmergencyExit(
                holdSeconds = if (state.alarm.hardMode) 60 else 15,
                onCancel = { showEmergency = false },
                onEscape = onEscape,
            )
        }
    }
}

/**
 * The humane way out for real emergencies: hold for 15 seconds (60 in Harder Mode) while the alarm keeps ringing,
 * then type "i am awake". It works, but it's never quicker than the mission.
 */
@Composable
private fun EmergencyExit(holdSeconds: Int, onCancel: () -> Unit, onEscape: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val hold = remember { Animatable(0f) }
    var unlocked by remember { mutableStateOf(false) }
    var typed by remember { mutableStateOf("") }
    val phrase = TypingPhrases.ESCAPE

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
            Text(tr("Only for real emergencies"), style = MaterialTheme.typography.headlineMedium, color = Paper)
            Text(
                tr("The alarm keeps ringing. This breaks your streak and is logged."),
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
                                    hold.animateTo(1f, tween(((1 - hold.value) * holdSeconds * 1000).toInt(), easing = LinearEasing))
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
                    val track = Paper.copy(alpha = 0.15f)
                    Canvas(Modifier.fillMaxSize()) {
                        drawArc(track, 0f, 360f, false, style = Stroke(12.dp.toPx()))
                        drawArc(Alert, -90f, 360f * hold.value, false, style = Stroke(12.dp.toPx(), cap = StrokeCap.Round))
                    }
                    Box(Modifier.size(170.dp).clip(CircleShape).background(NightSurface), contentAlignment = Alignment.Center) {
                        Text(
                            if (hold.value > 0f) "${(holdSeconds - hold.value * holdSeconds).toInt() + 1}" else tr("Hold"),
                            style = MaterialTheme.typography.displaySmall, color = Paper,
                        )
                    }
                }
            } else {
                Text(tr("Type: {0}", phrase), style = MaterialTheme.typography.titleMedium, color = DawnLavender)
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
                    tr("Back to my mission"),
                    style = MaterialTheme.typography.titleMedium, color = SunriseCoral,
                    modifier = Modifier.clip(CircleShape).clickable(onClick = onCancel).padding(Space.lg),
                )
            }
        }
    }
}
