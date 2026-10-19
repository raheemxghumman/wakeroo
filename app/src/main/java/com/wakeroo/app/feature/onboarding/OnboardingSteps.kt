package com.wakeroo.app.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakeroo.app.alarm.sound.Tones
import com.wakeroo.app.data.model.Alarm
import com.wakeroo.app.data.model.MissionConfig
import com.wakeroo.app.data.model.MissionType
import com.wakeroo.app.feature.editor.MissionPickerSheet
import com.wakeroo.app.feature.editor.MissionTile
import com.wakeroo.app.feature.editor.PillButton
import com.wakeroo.app.feature.editor.SoundPicker
import com.wakeroo.app.feature.editor.rememberTonePreview
import com.wakeroo.app.feature.editor.summary
import com.wakeroo.app.missions.NfcState
import com.wakeroo.app.missions.canCountSteps
import com.wakeroo.app.missions.nfcSimulated
import com.wakeroo.app.missions.nfcState
import com.wakeroo.app.ui.brand.WakerooMark
import com.wakeroo.app.ui.components.Chip
import com.wakeroo.app.ui.components.ChipRow
import com.wakeroo.app.ui.components.DrumWheel
import com.wakeroo.app.ui.components.icon
import com.wakeroo.app.ui.components.press3d
import com.wakeroo.app.ui.scenes.Scene
import com.wakeroo.app.ui.scenes.SceneBackground
import com.wakeroo.app.ui.scenes.ScenePreviewDialog
import com.wakeroo.app.ui.theme.Calm
import com.wakeroo.app.ui.theme.ClockType
import com.wakeroo.app.ui.theme.Hairline
import com.wakeroo.app.ui.theme.MorningGold
import com.wakeroo.app.ui.theme.Night
import com.wakeroo.app.ui.theme.NightSurface
import com.wakeroo.app.ui.theme.Outfit
import com.wakeroo.app.ui.theme.Paper
import com.wakeroo.app.ui.theme.PaperMuted
import com.wakeroo.app.ui.theme.Space
import com.wakeroo.app.ui.theme.SunriseCoral
import com.wakeroo.app.ui.theme.WakerooMotion
import com.wakeroo.app.ui.util.formatClock
import com.wakeroo.app.ui.util.formatMeridiem
import com.wakeroo.app.ui.util.formatUntil
import com.wakeroo.app.ui.util.repeatSummary
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.Duration
import java.time.ZonedDateTime

private val WEEKDAYS = DayOfWeek.entries.take(5).toSet()
private val WEEKEND = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

@Composable
fun RestoredStep(alarms: List<Alarm>, use24h: Boolean, onNext: () -> Unit) {
    StepScaffold(
        title = "Welcome back",
        subtitle = if (alarms.isEmpty()) "Your settings came over from your old phone." else
            "We restored your ${alarms.size} alarm${if (alarms.size == 1) "" else "s"}. A new phone resets alarm permissions, so let's make sure they ring here too.",
        primary = "Make sure they ring",
        onPrimary = onNext,
    ) {
        alarms.sortedBy { it.hour * 60 + it.minute }.take(6).forEachIndexed { i, a ->
            val appear = remember { Animatable(0f) }
            LaunchedEffect(Unit) { delay(90L * i); appear.animateTo(1f, WakerooMotion.bouncy()) }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = Space.md)
                    .graphicsLayer { alpha = appear.value.coerceIn(0f, 1f); translationY = (1 - appear.value) * 50f; rotationX = (1 - appear.value) * 20f }
                    .clip(RoundedCornerShape(24.dp))
                    .background(NightSurface.copy(alpha = 0.9f))
                    .border(1.dp, Hairline, RoundedCornerShape(24.dp))
                    .padding(Space.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(formatClock(a.hour, a.minute, use24h), style = ClockType.card.copy(fontSize = 32.sp), color = Paper)
                Text(" ${formatMeridiem(a.hour, use24h)}", style = MaterialTheme.typography.titleSmall, color = PaperMuted)
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(a.repeatSummary(), style = MaterialTheme.typography.labelMedium, color = Paper)
                    Text(a.missions.joinToString(" · ") { it.type.title }, style = MaterialTheme.typography.labelSmall, color = MorningGold)
                }
            }
        }
        if (alarms.size > 6) StepHint("and ${alarms.size - 6} more")
    }
}

@Composable
fun TimeStep(state: OnboardingState, use24h: Boolean, update: ((Alarm) -> Alarm) -> Unit, onNext: () -> Unit) {
    val draft = state.draft
    val hours = if (use24h) (0..23).map { "%02d".format(it) } else (0..11).map { if (it == 0) "12" else "$it" }
    val minutes = (0..59).map { "%02d".format(it) }
    val pm = draft.hour >= 12
    val wheelStyle = ClockType.card.copy(fontSize = 52.sp)
    fun setTime(h: Int, m: Int) = update { it.copy(hour = h, minute = m) }
    StepScaffold(
        title = "When do you want to wake up?",
        subtitle = null,
        primary = "Continue",
        onPrimary = onNext,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = Space.lg)) {
            WakerooMark(Modifier.size(40.dp))
            Spacer(Modifier.width(Space.sm))
            Text("Let's set up your first alarm. It takes about a minute.", style = MaterialTheme.typography.bodyMedium, color = PaperMuted)
        }
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(Night.copy(alpha = 0.35f))
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.03f))))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(32.dp))
                .padding(vertical = Space.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(Modifier.fillMaxWidth(0.86f).height(72.dp).clip(RoundedCornerShape(22.dp)).background(Night.copy(alpha = 0.35f)))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DrumWheel(
                        values = hours, selected = if (use24h) draft.hour else draft.hour % 12, style = wheelStyle, label = "Hour",
                        itemHeight = 72.dp, width = 104.dp,
                        onSelected = { i -> setTime(if (use24h) i else i + if (pm) 12 else 0, draft.minute) },
                    )
                    Text(":", style = wheelStyle, color = Paper)
                    DrumWheel(values = minutes, selected = draft.minute, style = wheelStyle, label = "Minute", itemHeight = 72.dp, width = 104.dp, onSelected = { m -> setTime(draft.hour, m) })
                    if (!use24h) {
                        DrumWheel(
                            values = listOf("AM", "PM"), selected = if (pm) 1 else 0, cyclic = false, width = 80.dp, itemHeight = 72.dp,
                            style = MaterialTheme.typography.headlineSmall, label = "AM or PM",
                            onSelected = { i -> setTime(draft.hour % 12 + if (i == 1) 12 else 0, draft.minute) },
                        )
                    }
                }
            }
            val until = state.nextRing?.let { Duration.between(ZonedDateTime.now(), it).formatUntil() }
            AnimatedContent(until, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "until") { text ->
                Text(if (text == null) "" else "Rings in $text", style = MaterialTheme.typography.titleMedium, color = MorningGold)
            }
        }
        Spacer(Modifier.height(Space.lg))
        ChipRow {
            Chip("Weekdays", draft.days == WEEKDAYS, { update { it.copy(days = WEEKDAYS, date = null) } })
            Chip("Every day", draft.days.size == 7, { update { it.copy(days = DayOfWeek.entries.toSet(), date = null) } })
            Chip("Weekends", draft.days == WEEKEND, { update { it.copy(days = WEEKEND, date = null) } })
            Chip("Just once", draft.days.isEmpty(), { update { it.copy(days = emptySet(), date = null) } })
        }
    }
}

@Composable
fun SceneStep(draft: Alarm, update: ((Alarm) -> Alarm) -> Unit, onNext: () -> Unit) {
    val context = LocalContext.current
    val (preview, _) = rememberTonePreview()
    var previewing by remember { mutableStateOf<Scene?>(null) }
    val selected = Scene.byId(draft.scene)
    StepScaffold(
        title = "Pick your wake-up scene",
        subtitle = "This is what your alarm screen looks like. Tap one to see it full screen.",
        primary = "Continue with ${selected.title}",
        onPrimary = onNext,
    ) {
        val scenes = Scene.entries.filter { it != Scene.Photo }
        scenes.chunked(3).forEach { row ->
            Row(Modifier.padding(bottom = Space.md), horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                row.forEach { scene ->
                    val isSelected = scene == selected
                    val border by animateColorAsState(if (isSelected) SunriseCoral else Hairline, label = "sceneBorder")
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.58f)
                                .press3d(maxTilt = 10f)
                                .clip(RoundedCornerShape(22.dp))
                                .border(if (isSelected) 3.dp else 1.dp, border, RoundedCornerShape(22.dp))
                                .clickable(role = Role.RadioButton) {
                                    previewing = scene
                                    scene.suggestedTone?.let { preview.play(it, Tones.uri(context, Tones.byId(it)), 40) }
                                },
                        ) {
                            SceneBackground(scene) {
                                Text(
                                    formatClock(draft.hour, draft.minute),
                                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 24.sp),
                                    color = Color(0xFFF5F3EE), modifier = Modifier.align(Alignment.Center),
                                )
                            }
                            if (scene.pro) {
                                Box(
                                    Modifier.align(Alignment.TopEnd).padding(6.dp).size(24.dp).clip(CircleShape).background(Night.copy(alpha = 0.7f)),
                                    contentAlignment = Alignment.Center,
                                ) { Icon(Icons.Rounded.Lock, "Pro", tint = MorningGold, modifier = Modifier.size(13.dp)) }
                            }
                        }
                        Text(scene.title, style = MaterialTheme.typography.labelMedium, color = if (isSelected) SunriseCoral else Paper, modifier = Modifier.padding(top = Space.xs), maxLines = 1)
                    }
                }
            }
        }
        StepHint("Pro scenes can be unlocked any time later. No pressure.")
    }
    previewing?.let { scene ->
        ScenePreviewDialog(
            scene, draft.hour, draft.minute, locked = scene.pro,
            onClose = { preview.stop(); previewing = null },
            onUse = {
                preview.stop()
                previewing = null
                // The scene's matching sound becomes the starting pick on the next step.
                update { it.copy(scene = scene.id, tone = scene.suggestedTone ?: it.tone) }
            },
            onUpgrade = { preview.stop(); previewing = null },
        )
    }
}

@Composable
fun SoundStep(draft: Alarm, update: ((Alarm) -> Alarm) -> Unit, onNext: () -> Unit) {
    val (preview, playing) = rememberTonePreview()
    StepScaffold(
        title = "Pick your sound",
        subtitle = "Tap to hear it at alarm volume.",
        primary = "Continue",
        onPrimary = { preview.stop(); onNext() },
    ) {
        SoundPicker(draft, pro = false, preview = preview, playing = playing, onUpgrade = { preview.stop() }, update = update)
        Spacer(Modifier.height(Space.xl))
        Row(Modifier.fillMaxWidth()) {
            Text("Volume", style = MaterialTheme.typography.bodyLarge, color = Paper, modifier = Modifier.weight(1f))
            Text("${draft.volume}%", style = MaterialTheme.typography.labelLarge, color = MorningGold)
        }
        Slider(
            value = draft.volume / 100f, valueRange = 0.1f..1f,
            onValueChange = { v -> update { it.copy(volume = (v * 100).toInt()) } },
            colors = SliderDefaults.colors(thumbColor = SunriseCoral, activeTrackColor = SunriseCoral, inactiveTrackColor = Paper.copy(alpha = 0.12f)),
        )
        Text("Gentle start", style = MaterialTheme.typography.bodyLarge, color = Paper, modifier = Modifier.padding(top = Space.sm))
        StepHint("Starts quiet and rises to full volume, so you wake calmly.")
        Spacer(Modifier.height(Space.sm))
        ChipRow {
            listOf(0 to "Off", 30 to "30 s", 60 to "1 min", 300 to "5 min").forEach { (sec, label) ->
                Chip(label, draft.rampSeconds == sec, { update { it.copy(rampSeconds = sec) } })
            }
        }
    }
}

@Composable
fun MissionStep(draft: Alarm, update: ((Alarm) -> Alarm) -> Unit, onNext: () -> Unit) {
    val context = LocalContext.current
    var editing by remember { mutableStateOf<MissionConfig?>(null) }
    val current = draft.missions.firstOrNull() ?: MissionConfig(MissionType.Math)
    StepScaffold(
        title = "Choose your wake-up mission",
        subtitle = "The alarm keeps ringing until it's done. Missions in another room work best.",
        primary = "Continue with ${current.type.title}",
        onPrimary = onNext,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = Space.lg).clip(RoundedCornerShape(20.dp)).background(Night.copy(alpha = 0.35f)).padding(Space.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(current.type.icon, null, tint = MorningGold)
            Spacer(Modifier.width(Space.md))
            Column(Modifier.weight(1f)) {
                Text("Chosen: ${current.type.title}", style = MaterialTheme.typography.titleSmall, color = Paper)
                Text(current.summary(), style = MaterialTheme.typography.bodySmall, color = PaperMuted)
            }
        }
        val nfc = context.nfcState()
        MissionType.entries.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth().padding(bottom = Space.md), horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                pair.forEach { t ->
                    val unavailable = when (t) {
                        MissionType.Nfc -> if (nfc == NfcState.Missing && !nfcSimulated) "No NFC on this phone" else null
                        MissionType.Steps -> if (!context.canCountSteps()) "No motion sensor" else null
                        else -> null
                    }
                    MissionTile(t, Modifier.weight(1f), unavailable, selected = t == current.type) {
                        editing = if (t == current.type) current else if (t == MissionType.Hunt) MissionConfig(t, difficulty = 1) else MissionConfig(t)
                    }
                }
            }
        }
    }
    editing?.let { config ->
        MissionPickerSheet(
            existing = config,
            onDismiss = { editing = null },
            onSave = { chosen ->
                update { it.copy(missions = listOf(chosen)) }
                editing = null
            },
            allowSetupLater = true,
            saveLabel = "Use this mission",
        )
    }
}

/** The sun rises behind a progress ring while the alarm is really saved and scheduled. */
@Composable
fun BuildingStep(state: OnboardingState, use24h: Boolean, onNext: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val p = state.building
    val done = p >= 1f && state.savedId != 0L
    LaunchedEffect(done) { if (done) haptics.performHapticFeedback(HapticFeedbackType.Confirm) }
    val float by rememberInfiniteTransition(label = "medal").animateFloat(
        -1f, 1f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "f",
    )
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(0.7f))
        Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 10.dp.toPx()
                drawArc(Color.White.copy(alpha = 0.18f), 0f, 360f, false, style = Stroke(stroke))
                drawArc(
                    Brush.sweepGradient(listOf(MorningGold, Color.White, MorningGold)), -90f, 360f * p, false,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
                drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.5f * p), Color.Transparent), center, size.minDimension * 0.5f), size.minDimension * 0.5f)
            }
            WakerooMark(
                Modifier.size(150.dp).graphicsLayer { translationY = if (done) float * 4.dp.toPx() else 0f },
                sunRise = (p * 1.4f).coerceIn(0f, 1f), leap = ((p - 0.45f) / 0.4f).coerceIn(0f, 1f), landing = ((p - 0.85f) / 0.15f).coerceIn(0f, 1f),
                horizonColor = Night, sunColor = Color.White, leapColor = Night.copy(alpha = 0.75f), dotColor = Color.White,
            )
        }
        Spacer(Modifier.height(Space.xl))
        AnimatedContent(
            when {
                done -> "All set!"
                p < 0.3f -> "Setting your time…"
                p < 0.6f -> "Tuning your sound…"
                else -> "Loading your mission…"
            },
            transitionSpec = { (slideInVertically { it / 2 } + fadeIn()) togetherWith fadeOut() },
            label = "buildText",
        ) { text -> Text(text, style = MaterialTheme.typography.headlineMedium, color = Night) }
        Spacer(Modifier.weight(0.3f))
        AnimatedContent(done, transitionSpec = { (scaleIn(initialScale = 0.8f) + fadeIn()) togetherWith fadeOut() }, label = "medal") { ready ->
            if (ready) {
                val a = state.draft
                Column(
                    Modifier
                        .fillMaxWidth()
                        .graphicsLayer { rotationX = float * 4f; cameraDistance = 12f * density }
                        .clip(RoundedCornerShape(28.dp))
                        .background(Night.copy(alpha = 0.85f))
                        .border(1.5.dp, MorningGold.copy(alpha = 0.6f), RoundedCornerShape(28.dp))
                        .padding(Space.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("YOUR FIRST ALARM IS READY", style = MaterialTheme.typography.labelSmall, color = MorningGold)
                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = Space.sm)) {
                        Text(formatClock(a.hour, a.minute, use24h), style = ClockType.card, color = Color(0xFFF5F3EE))
                        Text(" ${formatMeridiem(a.hour, use24h)}", style = MaterialTheme.typography.titleMedium, color = Color(0xFFA9ABB8), modifier = Modifier.padding(bottom = 6.dp))
                    }
                    Text(
                        "${a.repeatSummary()} · ${a.missions.firstOrNull()?.type?.title ?: "Math"} · ${Scene.byId(a.scene).title}",
                        style = MaterialTheme.typography.bodyMedium, color = Color(0xFFA9ABB8), textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(Space.lg))
                    PillButton("Continue", Modifier.fillMaxWidth(), onClick = onNext)
                }
            } else Spacer(Modifier.height(200.dp))
        }
        Spacer(Modifier.height(Space.lg))
    }
}

@Composable
fun TestStep(restore: Boolean, onTest: () -> Unit, onLater: () -> Unit) {
    StepScaffold(
        title = "Want to test it now?",
        subtitle = "A test alarm rings in one minute with an easy mission. Lock your phone and wait: if it rings over the lock screen, you're set.",
        primary = "Ring in 1 minute",
        onPrimary = onTest,
        warm = true,
        secondary = if (restore) "Not now" else "Later, take me to my alarm",
        onSecondary = onLater,
        scroll = false,
    ) {
        Box(Modifier.fillMaxWidth().weight(1f, fill = false).padding(vertical = Space.xl), contentAlignment = Alignment.Center) {
            val ring by rememberInfiniteTransition(label = "testRing").animateFloat(
                0f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Restart), label = "r",
            )
            Canvas(Modifier.size(220.dp)) {
                for (k in 0 until 3) {
                    val q = (ring + k / 3f) % 1f
                    drawCircle(Night.copy(alpha = (1 - q) * 0.35f), size.minDimension / 2 * (0.4f + 0.6f * q), style = Stroke(3.dp.toPx()))
                }
            }
            Box(Modifier.size(96.dp).clip(CircleShape).background(Night), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Alarm, null, tint = MorningGold, modifier = Modifier.size(48.dp))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(Calm))
            Spacer(Modifier.width(Space.sm))
            Text("Takes one minute. Proves it works on your phone.", style = MaterialTheme.typography.bodySmall, color = Night.copy(alpha = 0.8f))
        }
    }
}
