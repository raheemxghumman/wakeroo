package com.wakeroo.app.feature.you

import com.wakeroo.app.ui.i18n.tr
import com.wakeroo.app.ui.util.initial
import com.wakeroo.app.ui.util.formatSeconds
import com.wakeroo.app.ui.theme.MorningGoldFixed
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.navigationBarsPadding
import com.wakeroo.app.ui.components.RoundIconButton
import com.wakeroo.app.ui.components.SectionCard
import com.wakeroo.app.ui.theme.PaperFixed
import java.time.LocalDate
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wakeroo.app.container
import com.wakeroo.app.data.Settings
import com.wakeroo.app.data.db.RingEventEntity
import com.wakeroo.app.data.db.RingOutcome
import com.wakeroo.app.feature.pro.AdBanner
import com.wakeroo.app.feature.main.TabContentBottomPadding
import com.wakeroo.app.feature.stats.WakeStats
import com.wakeroo.app.feature.stats.WakeStatsCalculator
import com.wakeroo.app.ui.components.TabHeader
import com.wakeroo.app.ui.components.entrance
import com.wakeroo.app.ui.components.SkyBackground
import com.wakeroo.app.ui.components.press3d
import com.wakeroo.app.ui.theme.Alert
import com.wakeroo.app.ui.theme.Calm
import com.wakeroo.app.ui.theme.ClockType
import com.wakeroo.app.ui.theme.Base
import com.wakeroo.app.ui.theme.DawnLavender
import com.wakeroo.app.ui.theme.MorningGold
import com.wakeroo.app.ui.theme.Night
import com.wakeroo.app.ui.theme.NightSurface
import com.wakeroo.app.ui.theme.Paper
import com.wakeroo.app.ui.theme.PaperMuted
import com.wakeroo.app.ui.theme.Sky
import com.wakeroo.app.ui.theme.Space
import com.wakeroo.app.ui.theme.SunriseCoral
import com.wakeroo.app.ui.theme.WakerooMotion
import com.wakeroo.app.ui.util.formatClock
import com.wakeroo.app.ui.util.formatMeridiem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun YouScreen(onSettings: () -> Unit, onPro: () -> Unit) {
    val app = LocalContext.current.container
    val scope = rememberCoroutineScope()
    val events by app.repository.ringEvents.collectAsStateWithLifecycle(emptyList())
    val settings by app.settings.settings.collectAsStateWithLifecycle(Settings())
    val moods by app.morning.moods.collectAsStateWithLifecycle(emptyList())
    val routine by app.morning.routine.collectAsStateWithLifecycle(emptyList())
    val routineLog by app.morning.routineLog.collectAsStateWithLifecycle(emptyList())
    val stats = remember(events) { WakeStatsCalculator.compute(events) }
    val log = events.filter { !it.isTest }.let { if (settings.pro) it else it.take(14) }
    val routineDays = remember(routine, routineLog) {
        if (routine.isEmpty()) 0 else routineLog.groupBy { it.date }.count { (_, ticks) -> ticks.size >= routine.size }
    }
    val medals = remember(events, settings.missionCounts, routineDays, settings.achievements) {
        AchievementEngine.evaluate(AchievementInput(events, settings.missionCounts, routineDays)).map {
            // Once earned, a medal stays earned even if a streak later breaks.
            if (it.def.id in settings.achievements) it.copy(unlocked = true, progress = 1f) else it
        }
    }
    // Remember new unlocks with their date.
    LaunchedEffect(medals) {
        val fresh = AchievementEngine.newlyUnlocked(medals, settings.achievements.keys)
        if (fresh.isNotEmpty()) {
            val now = System.currentTimeMillis()
            app.settings.update { s -> s.copy(achievements = s.achievements + fresh.associate { it.id to now }) }
        }
    }
    val recap = remember(events, settings.achievements) { Recap.thisWeek(events, settings.achievements) }
    var openDay by remember { mutableStateOf<LocalDate?>(null) }
    var openMedal by remember { mutableStateOf<AchievementState?>(null) }
    var sharing by remember { mutableStateOf(false) }

    SkyBackground(Sky.Dawn, intensity = 0.55f) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Space.xl, end = Space.xl, bottom = TabContentBottomPadding),
            verticalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            item(key = "header") {
                TabHeader(tr("Your mornings"), onSettings = onSettings, eyebrow = tr("You"), modifier = Modifier.entrance(0)) {
                    RoundIconButton(Icons.Rounded.IosShare, tr("Share your streak"), { sharing = true })
                }
            }
            item(key = "tiles") {
                Row(Modifier.entrance(1), horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                    StreakTile(stats.streak, stats.bestStreak, Modifier.weight(1.2f).height(200.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.md)) {
                        NumberTile(tr("On time"), stats.successRate?.let { "$it%" } ?: "–", tr("last 30 days"), Modifier.height(94.dp))
                        NumberTile(
                            tr("Up in"), stats.averageSecondsToWake?.let { formatSeconds(it) } ?: "–",
                            tr("on average"), Modifier.height(94.dp),
                        )
                    }
                }
            }
            item(key = "recap") { Box(Modifier.entrance(2)) { RecapCard(recap) } }
            item(key = "calendar") {
                SectionCard(tr("Wake-up calendar"), icon = Icons.Rounded.CalendarMonth, modifier = Modifier.entrance(3)) {
                    WakeCalendar(events, settings.pro, onDay = { openDay = it }, onPro = onPro)
                }
            }
            item(key = "medals") {
                SectionCard(tr("Medals · {0} of {1}", medals.count { it.unlocked }, medals.size), icon = Icons.Rounded.MilitaryTech, modifier = Modifier.entrance(4)) {
                    medals.chunked(3).forEach { row ->
                        Row(Modifier.fillMaxWidth().padding(bottom = Space.md), horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                            row.forEach { m ->
                                Column(
                                    Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).clickable { openMedal = m }.padding(vertical = Space.sm),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Medal(m.def, m.unlocked, 64.dp)
                                    Text(m.def.title, style = MaterialTheme.typography.labelSmall, color = if (m.unlocked) Paper else PaperMuted, maxLines = 2, textAlign = TextAlign.Center, modifier = Modifier.padding(top = Space.sm))
                                    if (!m.unlocked) {
                                        Box(Modifier.padding(top = 4.dp).fillMaxWidth(0.6f).height(4.dp).clip(CircleShape).background(Paper.copy(alpha = 0.12f))) {
                                            Box(Modifier.fillMaxWidth(m.progress).height(4.dp).clip(CircleShape).background(MorningGold))
                                        }
                                    }
                                }
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            item(key = "week") { Box(Modifier.entrance(5)) { WeekChart(stats) } }
            item(key = "numbers") {
                Row(Modifier.entrance(6), horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                    NumberTile(tr("Usual wake time"), stats.averageWakeTime?.let { "${it.formatClock()} ${it.formatMeridiem()}" } ?: "–", tr("last 2 weeks"), Modifier.weight(1f).height(94.dp))
                    NumberTile(tr("Wake-ups"), "${stats.totalWakeUps}", tr("all time"), Modifier.weight(1f).height(94.dp))
                }
            }
            item(key = "logTitle") {
                Column {
                    Text(tr("RING LOG"), style = MaterialTheme.typography.labelSmall, color = PaperMuted, modifier = Modifier.padding(top = Space.lg))
                    Text(
                        tr("Proof of every ring. If an alarm ever didn't fire, it shows here in red."),
                        style = MaterialTheme.typography.bodySmall, color = PaperMuted,
                    )
                }
            }
            if (log.isEmpty()) {
                item(key = "empty") {
                    Text(
                        tr("Your first morning shows up here."), style = MaterialTheme.typography.bodyLarge, color = Paper,
                        modifier = Modifier.fillMaxWidth().padding(vertical = Space.xl), textAlign = TextAlign.Center,
                    )
                }
            }
            items(log, key = { it.id }) { LogRow(it, Modifier.animateItem()) }
            if (!settings.pro && events.size > 14) {
                item(key = "more") {
                    Text(
                        tr("See your full history with Pro"), style = MaterialTheme.typography.labelLarge, color = MorningGold,
                        modifier = Modifier.fillMaxWidth().clip(CircleShape).clickable(onClick = onPro).padding(Space.lg), textAlign = TextAlign.Center,
                    )
                }
            }
            if (!settings.pro) item(key = "ad") { AdBanner(Modifier.fillMaxWidth().padding(top = Space.lg)) }
        }
    }
    openDay?.let { day -> DaySheet(day, events, moods.firstOrNull { it.date == day.toString() }) { openDay = null } }
    openMedal?.let { m -> MedalSheet(m, settings.achievements[m.def.id]) { openMedal = null } }
    if (sharing) {
        ShareStreakDialog(
            stats.streak, stats.lastSevenDays.map { it.success },
            medal = medals.filter { it.unlocked }.maxByOrNull { it.def.tier.ordinal }?.def,
        ) { sharing = false }
    }
}

/** This week at a glance: mornings won, usual wake time, fastest mission, the week's medal. */
@Composable
private fun RecapCard(recap: WeekRecap) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF3A2B4E), Color(0xFF6B4A78))))
            .border(1.dp, MorningGold.copy(alpha = 0.25f), RoundedCornerShape(26.dp))
            .padding(Space.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(tr("THIS WEEK"), style = MaterialTheme.typography.labelSmall, color = MorningGold)
            Text(tr("{0} of {1} mornings won", recap.won, recap.days), style = MaterialTheme.typography.titleMedium, color = PaperFixed)
            Text(
                listOfNotNull(
                    recap.averageWake?.let { tr("Usually up at {0}", it) },
                    recap.fastest?.let { tr("Fastest: {0}", formatSeconds(it)) },
                ).joinToString(" · ").ifEmpty { tr("Your first mornings will show here") },
                style = MaterialTheme.typography.bodySmall, color = PaperFixed.copy(alpha = 0.75f),
            )
        }
        recap.medal?.let { Medal(it, unlocked = true, size = 52.dp) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MedalSheet(state: AchievementState, unlockedAt: Long?, onDismiss: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = Base, dragHandle = null) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(Space.xl), horizontalAlignment = Alignment.CenterHorizontally) {
            SpinnableMedal(state.def, state.unlocked, 190.dp)
            Text(if (state.unlocked) tr("Flick it to spin") else tr("Locked"), style = MaterialTheme.typography.labelSmall, color = PaperMuted, modifier = Modifier.padding(top = Space.md))
            Text(state.def.title, style = MaterialTheme.typography.headlineMedium, color = Paper, modifier = Modifier.padding(top = Space.md))
            Text(state.def.description, style = MaterialTheme.typography.bodyLarge, color = PaperMuted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = Space.xs))
            Spacer(Modifier.height(Space.lg))
            if (state.unlocked) {
                val date = unlockedAt?.let { DateTimeFormatter.ofPattern("d MMMM yyyy").format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())) }
                Text(if (date != null) tr("Earned on {0}", date) else tr("Earned"), style = MaterialTheme.typography.labelLarge, color = MorningGold)
            } else {
                Box(Modifier.fillMaxWidth(0.7f).height(8.dp).clip(CircleShape).background(Paper.copy(alpha = 0.12f))) {
                    Box(Modifier.fillMaxWidth(state.progress).height(8.dp).clip(CircleShape).background(MorningGold))
                }
                Text(state.progressLabel, style = MaterialTheme.typography.labelMedium, color = PaperMuted, modifier = Modifier.padding(top = Space.sm))
            }
            Spacer(Modifier.height(Space.lg))
        }
    }
}

@Composable
private fun Tile(modifier: Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.04f))))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(26.dp))
            .padding(Space.lg),
    ) { content() }
}

@Composable
private fun NumberTile(title: String, value: String, caption: String, modifier: Modifier) {
    Tile(modifier) {
        Column {
            Text(title, style = MaterialTheme.typography.labelMedium, color = DawnLavender)
            Spacer(Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.headlineMedium, color = Paper, maxLines = 1)
            Text(caption, style = MaterialTheme.typography.labelSmall, color = PaperMuted)
        }
    }
}

/** Tap to flip the card over in 3D and see the best streak on the back. */
@Composable
private fun StreakTile(streak: Int, best: Int, modifier: Modifier) {
    var flipped by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (flipped) 180f else 0f, WakerooMotion.bouncy(), label = "flip")
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val shown by animateIntAsState(if (started) (if (rotation > 90f) best else streak) else 0, tween(900), label = "count")
    Box(
        modifier
            .graphicsLayer { rotationY = rotation; cameraDistance = 14f * density }
            .press3d(maxTilt = 6f)
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(SunriseCoral, MorningGold)))
            .clickable(remember { MutableInteractionSource() }, null) { flipped = !flipped }
            .padding(Space.lg),
    ) {
        Column(Modifier.graphicsLayer { rotationY = if (rotation > 90f) 180f else 0f }) {
            Text(if (rotation > 90f) tr("Best streak") else tr("Streak"), style = MaterialTheme.typography.labelLarge, color = Night)
            Spacer(Modifier.weight(1f))
            Text("$shown", style = ClockType.hero, color = Night)
            Text(if (shown == 1) tr("morning in a row") else tr("mornings in a row"), style = MaterialTheme.typography.bodyMedium, color = Night.copy(alpha = 0.8f))
            Text(if (rotation > 90f) tr("Tap to flip back") else tr("Tap to see your best"), style = MaterialTheme.typography.labelSmall, color = Night.copy(alpha = 0.6f), modifier = Modifier.padding(top = Space.xs))
        }
    }
}

@Composable
private fun WeekChart(stats: WakeStats) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val grow by animateFloatAsState(if (started) 1f else 0f, WakerooMotion.bouncy(), label = "grow")
    val maxSeconds = (stats.lastSevenDays.mapNotNull { it.secondsToWake }.maxOrNull() ?: 60).coerceAtLeast(60)
    Tile(Modifier.height(210.dp)) {
        Column {
            Text(tr("Time to get up, last 7 days"), style = MaterialTheme.typography.labelMedium, color = DawnLavender)
            Spacer(Modifier.height(Space.md))
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                stats.lastSevenDays.forEach { day ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        val empty = Paper.copy(alpha = 0.15f)
                        Canvas(Modifier.width(22.dp).weight(1f)) {
                            val fraction = day.secondsToWake?.let { 0.15f + 0.85f * it / maxSeconds } ?: 0.06f
                            val h = size.height * fraction * grow
                            val color = when (day.success) { true -> MorningGoldFixed; false -> Alert; null -> empty }
                            drawRoundRect(color, Offset(0f, size.height - h), Size(size.width, h), CornerRadius(size.width / 2))
                        }
                        Text(day.date.dayOfWeek.initial(), style = MaterialTheme.typography.labelSmall, color = PaperMuted, modifier = Modifier.padding(top = Space.xs))
                    }
                }
            }
        }
    }
}

@Composable
private fun LogRow(event: RingEventEntity, modifier: Modifier) {
    val zone = ZoneId.systemDefault()
    val scheduled = Instant.ofEpochMilli(event.scheduledAt).atZone(zone)
    val ended = event.endedAt?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalTime() }
    val (color, outcome) = when (event.outcome) {
        RingOutcome.Dismissed -> if (event.wakeCheckPassed == true) Calm to tr("Wake-up check passed") else Calm to tr("Up at {0}", ended?.let { "${it.formatClock()} ${it.formatMeridiem()}" } ?: "–")
        RingOutcome.Escaped -> MorningGold to tr("Emergency exit")
        RingOutcome.Missed -> Alert to if (event.wakeCheckPassed == false) tr("Fell back asleep, alarm re-rang") else tr("Didn't ring: phone off or app paused")
        RingOutcome.Snoozed -> DawnLavender to tr("Snoozed")
    }
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(NightSurface.copy(alpha = 0.7f)).padding(Space.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(Space.md))
        Column(Modifier.weight(1f)) {
            Text(
                "${DateTimeFormatter.ofPattern("EEE d MMM").format(scheduled)} · ${scheduled.toLocalTime().formatClock()} ${scheduled.toLocalTime().formatMeridiem()}",
                style = MaterialTheme.typography.bodyMedium, color = Paper,
            )
            Text(
                listOfNotNull(outcome, event.label.takeIf { it.isNotBlank() }, if (event.snoozes == 1) tr("1 snooze") else if (event.snoozes > 1) tr("{0} snoozes", event.snoozes) else null).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall, color = PaperMuted,
            )
        }
    }
}
