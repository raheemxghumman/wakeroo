package com.wakeroo.app.feature.sleep

import com.wakeroo.app.ui.i18n.tr
import com.wakeroo.app.ui.theme.DawnLavenderFixed
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Forest
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Water
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wakeroo.app.container
import com.wakeroo.app.data.Settings
import com.wakeroo.app.feature.editor.PillButton
import com.wakeroo.app.feature.main.TabContentBottomPadding
import com.wakeroo.app.reminders.BedtimeCalculator
import com.wakeroo.app.sleep.SleepPlayer
import com.wakeroo.app.sleep.SleepSound
import com.wakeroo.app.ui.components.Chip
import com.wakeroo.app.ui.components.ChipRow
import com.wakeroo.app.ui.components.EntranceStyle
import com.wakeroo.app.ui.components.SectionCard
import com.wakeroo.app.ui.components.SkyBackground
import com.wakeroo.app.ui.components.Stepper
import com.wakeroo.app.ui.components.TabHeader
import com.wakeroo.app.ui.components.ToggleRow
import com.wakeroo.app.ui.components.entrance
import com.wakeroo.app.ui.components.press3d
import com.wakeroo.app.ui.components.rememberDeviceTilt
import com.wakeroo.app.ui.theme.AlwaysDark
import com.wakeroo.app.ui.theme.DawnLavender
import com.wakeroo.app.ui.theme.PaperFixed
import com.wakeroo.app.ui.theme.Hairline
import com.wakeroo.app.ui.theme.MorningGold
import com.wakeroo.app.ui.theme.Night
import com.wakeroo.app.ui.theme.NightSurface
import com.wakeroo.app.ui.theme.NightSurfaceHigh
import com.wakeroo.app.ui.theme.Paper
import com.wakeroo.app.ui.theme.PaperMuted
import com.wakeroo.app.ui.theme.Sky
import com.wakeroo.app.ui.theme.Space
import com.wakeroo.app.ui.theme.SunriseCoral
import com.wakeroo.app.ui.util.formatClock
import com.wakeroo.app.ui.util.formatMeridiem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import kotlin.math.cos
import kotlin.math.sin

val SleepSound.icon: ImageVector
    get() = when (this) {
        SleepSound.Rain -> Icons.Rounded.WaterDrop
        SleepSound.Ocean -> Icons.Rounded.Waves
        SleepSound.White -> Icons.Rounded.GraphicEq
        SleepSound.Brown -> Icons.Rounded.Grain
        SleepSound.Pink -> Icons.Rounded.Water
        SleepSound.Fan -> Icons.Rounded.Air
        SleepSound.Crickets -> Icons.Rounded.Forest
    }

private val TIMERS = listOf(15, 30, 45, 60, 90, -1)

@Composable
fun SleepScreen(onSettings: () -> Unit, onNewAlarm: () -> Unit, onPro: () -> Unit) {
    val context = LocalContext.current
    val app = context.container
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val settings by app.settings.settings.collectAsStateWithLifecycle(Settings())
    val alarms by app.repository.alarms.collectAsStateWithLifecycle(emptyList())
    val player by SleepPlayer.state.collectAsStateWithLifecycle()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(15_000); now = System.currentTimeMillis() } }
    val use24h = settings.use24Hour ?: DateFormat.is24HourFormat(context)
    val bedtime = remember(alarms, settings.sleepGoalMinutes, now) { BedtimeCalculator.next(alarms, settings.sleepGoalMinutes, settings.windDownMinutes, now) }
    val nextAlarm = remember(alarms, now) { alarms.mapNotNull { com.wakeroo.app.alarm.NextTriggerCalculator.nextMillis(it, now) }.minOrNull() }
    val volumes = remember { mutableStateMapOf<String, Float>() }

    val selected = settings.sleepSounds.mapNotNull { SleepSound.byId(it) }
    fun layers() = selected.map { it to (volumes[it.id] ?: 0.7f) }
    fun saveSelection(list: List<SleepSound>) = scope.launch { app.settings.update { it.copy(sleepSounds = list.map { s -> s.id }) } }

    fun toggle(sound: SleepSound) {
        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        if (sound.pro && !settings.pro) return onPro()
        val next = when {
            sound in selected -> (selected - sound).ifEmpty { selected }
            // Mixing two sounds is a Pro extra; free users swap instead.
            settings.pro -> (selected + sound).takeLast(2)
            else -> listOf(sound)
        }
        saveSelection(next)
        SleepPlayer.update(context, next.map { it to (volumes[it.id] ?: 0.7f) })
    }

    SkyBackground(Sky.Night, intensity = 0.7f) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Space.xl, end = Space.xl, bottom = TabContentBottomPadding),
            verticalArrangement = Arrangement.spacedBy(Space.lg),
        ) {
            item(key = "header") { TabHeader(tr("Sleep"), onSettings = onSettings, eyebrow = tr("Tonight"), modifier = Modifier.entrance(0)) }
            item(key = "moon") {
                Box(Modifier.entrance(1, EntranceStyle.Pop)) {
                    MoonHero(bedtime, nextAlarm, now, use24h, settings.sleepGoalMinutes, playing = player.playing && !player.paused, onNewAlarm = onNewAlarm)
                }
            }
            item(key = "sounds") {
                SectionCard(tr("Sleep sounds"), icon = Icons.Rounded.GraphicEq, modifier = Modifier.entrance(2)) {
                    Text(
                        if (settings.pro) tr("Pick up to two to mix.") else tr("Pick one. Mix two sounds with Pro."),
                        style = MaterialTheme.typography.bodySmall, color = PaperMuted, modifier = Modifier.padding(bottom = Space.md),
                    )
                    SleepSound.entries.chunked(3).forEach { row ->
                        Row(Modifier.padding(bottom = Space.md), horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                            row.forEach { sound ->
                                SoundTile(sound, sound in selected, playing = player.playing && sound in selected && !player.paused, locked = sound.pro && !settings.pro, Modifier.weight(1f)) { toggle(sound) }
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    AnimatedVisibility(selected.size > 1, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                        Column {
                            selected.forEach { sound ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(sound.icon, null, tint = DawnLavender, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(Space.sm))
                                    Slider(
                                        value = volumes[sound.id] ?: 0.7f,
                                        onValueChange = { v ->
                                            volumes[sound.id] = v
                                            SleepPlayer.update(context, layers())
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = SliderDefaults.colors(thumbColor = DawnLavender, activeTrackColor = DawnLavender, inactiveTrackColor = Paper.copy(alpha = 0.12f)),
                                    )
                                }
                            }
                        }
                    }
                    Text(tr("Timer"), style = MaterialTheme.typography.bodyLarge, color = Paper, modifier = Modifier.padding(top = Space.sm, bottom = Space.sm))
                    ChipRow {
                        TIMERS.forEach { m ->
                            Chip(if (m < 0) tr("Until my alarm") else tr("{0} min", m), settings.sleepTimerMinutes == m, {
                                scope.launch { app.settings.update { it.copy(sleepTimerMinutes = m) } }
                            })
                        }
                    }
                    Spacer(Modifier.height(Space.xl))
                    PlayControl(player.playing, player.paused, player.endsAt, player.endsForAlarm,
                        onPlay = { SleepPlayer.start(context, layers(), settings.sleepTimerMinutes) },
                        onPause = { SleepPlayer.togglePause(context) },
                        onStop = { SleepPlayer.stop(context) },
                    )
                    Text(
                        tr("Plays on media volume and always fades out a minute before your alarm, so it never covers it."),
                        style = MaterialTheme.typography.bodySmall, color = PaperMuted, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = Space.md),
                    )
                }
            }
            item(key = "bedtime") {
                SectionCard(tr("Bedtime"), icon = Icons.Rounded.Bedtime, modifier = Modifier.entrance(3)) {
                    val goal = settings.sleepGoalMinutes
                    Stepper(
                        tr("Sleep goal"), "${goal / 60} h${if (goal % 60 != 0) " ${goal % 60}" else ""}",
                        onMinus = { scope.launch { app.settings.update { it.copy(sleepGoalMinutes = (it.sleepGoalMinutes - 30).coerceAtLeast(360)) } } },
                        onPlus = { scope.launch { app.settings.update { it.copy(sleepGoalMinutes = (it.sleepGoalMinutes + 30).coerceAtMost(600)) } } },
                        minusEnabled = goal > 360, plusEnabled = goal < 600,
                    )
                    Spacer(Modifier.height(Space.md))
                    ToggleRow(tr("Wind-down reminder"), tr("A nudge before bedtime, with sleep sounds one tap away."), settings.bedtimeReminder) { on ->
                        scope.launch { app.settings.update { it.copy(bedtimeReminder = on) } }
                    }
                    AnimatedVisibility(settings.bedtimeReminder) {
                        ChipRow {
                            listOf(0 to tr("At bedtime"), 15 to tr("15 min before"), 30 to tr("30 min before"), 60 to tr("1 h before")).forEach { (m, label) ->
                                Chip(label, settings.windDownMinutes == m, { scope.launch { app.settings.update { it.copy(windDownMinutes = m) } } })
                            }
                        }
                    }
                    Spacer(Modifier.height(Space.md))
                    ToggleRow(
                        tr("No alarm for tomorrow"),
                        tr("At {0} {1}, a reminder if nothing is set for the morning.", formatClock(settings.noAlarmReminderAt / 60, settings.noAlarmReminderAt % 60, use24h), formatMeridiem(settings.noAlarmReminderAt / 60, use24h)),
                        settings.noAlarmReminder,
                    ) { on -> scope.launch { app.settings.update { it.copy(noAlarmReminder = on) } } }
                }
            }
        }
    }
}

/** A glowing moon with craters and a soft terminator, breathing slowly while you wind down. */
@Composable
private fun MoonHero(
    bedtime: com.wakeroo.app.reminders.Bedtime?,
    nextAlarm: Long?,
    now: Long,
    use24h: Boolean,
    goalMinutes: Int,
    playing: Boolean,
    onNewAlarm: () -> Unit,
) = AlwaysDark {
    val tilt by rememberDeviceTilt()
    val breathe by rememberInfiniteTransition(label = "moon").animateFloat(
        0f, 1f, infiniteRepeatable(tween(if (playing) 4200 else 7000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b",
    )
    val drift by rememberInfiniteTransition(label = "drift").animateFloat(
        0f, 1f, infiniteRepeatable(tween(60_000, easing = LinearEasing), RepeatMode.Restart), label = "d",
    )
    Column(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { rotationX = -tilt.y * 6f; rotationY = tilt.x * 6f; cameraDistance = 16f * density }
            .clip(RoundedCornerShape(32.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF1B1F3D), Color(0xFF0E1120))))
            .border(1.dp, Hairline, RoundedCornerShape(32.dp))
            .padding(Space.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(170.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension * 0.34f
                val c = Offset(size.width / 2 + tilt.x * 6f, size.height / 2 + tilt.y * 6f)
                val glow = r * (1.9f + 0.15f * breathe)
                drawCircle(Brush.radialGradient(listOf(Color(0xFFB9A7FF).copy(alpha = 0.35f), Color.Transparent), c, glow), glow, c)
                drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF7E6), Color(0xFFE8DDC8), Color(0xFFBFB5A3)), c + Offset(-r * 0.35f, -r * 0.35f), r * 1.6f), r, c)
                val moon = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, r)) }
                clipPath(moon) {
                    // Craters drift slowly across: the moon turns.
                    listOf(Triple(0.2f, -0.3f, 0.22f), Triple(-0.35f, 0.15f, 0.16f), Triple(0.35f, 0.35f, 0.12f), Triple(-0.05f, 0.5f, 0.09f), Triple(0.55f, -0.05f, 0.1f)).forEach { (x, y, s) ->
                        val ox = ((x + drift * 2f + 1f) % 2f) - 1f
                        val p = c + Offset(ox * r, y * r)
                        drawCircle(Color(0xFFA79D8C).copy(alpha = 0.45f * (1f - kotlin.math.abs(ox))), r * s, p)
                    }
                    // The shadowed side, with a soft terminator rather than a hard edge.
                    val edge = c.x + r * (0.05f + 0.12f * breathe)
                    drawRect(
                        Brush.horizontalGradient(0f to Color.Transparent, 0.35f to Color(0xFF0E1120).copy(alpha = 0.35f), 1f to Color(0xFF0E1120).copy(alpha = 0.7f), startX = edge - r * 0.5f, endX = c.x + r),
                        Offset(c.x - r, c.y - r), androidx.compose.ui.geometry.Size(r * 2, r * 2),
                    )
                }
                for (k in 0 until 10) {
                    val a = k * 0.63f + drift * 6.28f * 0.2f
                    val d = r * (1.5f + (k % 3) * 0.25f)
                    val tw = 0.4f + 0.6f * (0.5f + 0.5f * sin(breathe * 6.28f + k))
                    drawCircle(Color.White.copy(alpha = tw * 0.8f), (1.2f + k % 2).dp.toPx(), c + Offset(cos(a) * d, sin(a) * d * 0.6f))
                }
            }
        }
        Spacer(Modifier.height(Space.md))
        if (bedtime == null && nextAlarm != null) {
            // The alarm is close: no bedtime to plan, just encouragement.
            val at = Instant.ofEpochMilli(nextAlarm).atZone(ZoneId.systemDefault()).toLocalTime()
            val mins = (nextAlarm - now) / 60_000
            Text(tr("Your alarm is in {0}", if (mins >= 60) tr("{0} h {1} min", mins / 60, mins % 60) else tr("{0} min", mins)), style = MaterialTheme.typography.headlineSmall, color = Paper, textAlign = TextAlign.Center)
            Text(tr("{0} {1}. Close your eyes and get what rest you can.", at.formatClock(use24h), at.formatMeridiem(use24h)), style = MaterialTheme.typography.bodyMedium, color = PaperMuted, textAlign = TextAlign.Center)
        } else if (bedtime == null) {
            Text(tr("No alarm set for the morning"), style = MaterialTheme.typography.headlineSmall, color = Paper)
            Text(tr("Set one and Wakeroo works out your bedtime."), style = MaterialTheme.typography.bodyMedium, color = PaperMuted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(Space.lg))
            PillButton("Set an alarm", onClick = onNewAlarm)
        } else {
            val zone = ZoneId.systemDefault()
            val bed = Instant.ofEpochMilli(bedtime.bedAt).atZone(zone).toLocalTime()
            val alarm = Instant.ofEpochMilli(bedtime.alarmAt).atZone(zone).toLocalTime()
            Text(tr("BED BY"), style = MaterialTheme.typography.labelSmall, color = DawnLavender)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(bed.formatClock(use24h), style = MaterialTheme.typography.displayMedium, color = Paper)
                Text(" ${bed.formatMeridiem(use24h)}", style = MaterialTheme.typography.titleMedium, color = PaperMuted, modifier = Modifier.padding(bottom = 8.dp))
            }
            val hours = goalMinutes / 60f
            Text(
                tr("for {0} of sleep before your {1} {2} alarm", if (hours % 1f == 0f) "${hours.toInt()} h" else "%.1f h".format(hours), alarm.formatClock(use24h), alarm.formatMeridiem(use24h)),
                style = MaterialTheme.typography.bodyMedium, color = PaperMuted, textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SoundTile(sound: SleepSound, selected: Boolean, playing: Boolean, locked: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val border by animateColorAsState(if (selected) DawnLavender else Hairline, label = "tileBorder")
    val lift by animateFloatAsState(if (selected) 1f else 0f, label = "tileLift")
    val wave by rememberInfiniteTransition(label = "tileWave").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart), label = "w",
    )
    Box(modifier.aspectRatio(1f)) {
    Column(
        Modifier
            .fillMaxSize()
            .press3d(maxTilt = 12f)
            .graphicsLayer { translationY = -lift * 4.dp.toPx() }
            .clip(RoundedCornerShape(22.dp))
            .background(if (selected) Brush.linearGradient(listOf(Color(0xFF3A3170), Color(0xFF1E2246))) else Brush.linearGradient(listOf(NightSurfaceHigh, NightSurface)))
            .border(if (selected) 2.dp else 1.dp, border, RoundedCornerShape(22.dp))
            .clickable(role = Role.Checkbox, onClick = onClick)
            .padding(Space.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (playing) {
                Canvas(Modifier.size(52.dp)) {
                    for (k in 0..1) {
                        val p = (wave + k * 0.5f) % 1f
                        drawCircle(DawnLavenderFixed.copy(alpha = (1 - p) * 0.5f), size.minDimension / 2 * (0.5f + 0.5f * p), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                    }
                }
            }
            Icon(sound.icon, null, tint = if (selected) DawnLavenderFixed else Paper.copy(alpha = 0.85f), modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(Space.sm))
        Text(sound.title, style = MaterialTheme.typography.labelMedium, color = if (selected) PaperFixed else Paper, maxLines = 1, textAlign = TextAlign.Center)
    }
    if (locked) {
        Box(
            Modifier.align(Alignment.TopEnd).padding(8.dp).size(22.dp).clip(CircleShape).background(Night.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Lock, tr("Pro"), tint = MorningGold, modifier = Modifier.size(12.dp)) }
    }
    }
}

/** One big round button that morphs between play and stop, with pause beside it while playing. */
@Composable
private fun PlayControl(playing: Boolean, paused: Boolean, endsAt: Long, forAlarm: Boolean, onPlay: () -> Unit, onPause: () -> Unit, onStop: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(playing) { while (playing) { delay(1000); now = System.currentTimeMillis() } }
    val ring by rememberInfiniteTransition(label = "play").animateFloat(
        0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart), label = "r",
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        AnimatedVisibility(playing, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
            Box(
                Modifier.padding(end = Space.xl).size(56.dp).press3d().clip(CircleShape).background(Paper.copy(alpha = 0.1f)).clickable(onClick = onPause),
                contentAlignment = Alignment.Center,
            ) { Icon(if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, if (paused) tr("Resume") else tr("Pause"), tint = Paper) }
        }
        Box(Modifier.size(110.dp), contentAlignment = Alignment.Center) {
            if (playing && !paused) {
                Canvas(Modifier.fillMaxSize()) {
                    for (k in 0..2) {
                        val p = (ring + k / 3f) % 1f
                        drawCircle(DawnLavenderFixed.copy(alpha = (1 - p) * 0.4f), size.minDimension / 2 * (0.7f + 0.3f * p), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                    }
                }
            }
            Box(
                Modifier
                    .size(84.dp)
                    .press3d()
                    .clip(CircleShape)
                    .background(Brush.linearGradient(if (playing) listOf(Color(0xFF6B5BD6), DawnLavender) else listOf(SunriseCoral, MorningGold)))
                    .clickable(role = Role.Button, onClick = if (playing) onStop else onPlay),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(playing, transitionSpec = { scaleIn() + fadeIn() togetherWith scaleOut() + fadeOut() }, label = "playIcon") { p ->
                    Icon(if (p) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, if (p) tr("Stop sleep sounds") else tr("Play sleep sounds"), tint = Night, modifier = Modifier.size(40.dp))
                }
            }
        }
        if (playing) Spacer(Modifier.width(56.dp + Space.xl))
    }
    AnimatedVisibility(playing) {
        val left = ((endsAt - now) / 60_000).coerceAtLeast(0)
        Text(
            if (forAlarm) tr("Fades out before your alarm · {0} h {1} min", left / 60, left % 60) else tr("{0} min left, then a gentle fade", left),
            style = MaterialTheme.typography.labelLarge, color = DawnLavender, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = Space.sm),
        )
    }
}
