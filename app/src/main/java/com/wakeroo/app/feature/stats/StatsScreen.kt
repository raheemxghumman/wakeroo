package com.wakeroo.app.feature.stats

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
import com.wakeroo.app.feature.settings.TopBar
import com.wakeroo.app.ui.components.SkyBackground
import com.wakeroo.app.ui.components.press3d
import com.wakeroo.app.ui.theme.Alert
import com.wakeroo.app.ui.theme.Calm
import com.wakeroo.app.ui.theme.ClockType
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
fun StatsScreen(onBack: () -> Unit, onPro: () -> Unit) {
    val app = LocalContext.current.container
    val events by app.repository.ringEvents.collectAsStateWithLifecycle(emptyList())
    val settings by app.settings.settings.collectAsStateWithLifecycle(Settings())
    val stats = remember(events) { WakeStatsCalculator.compute(events) }
    val log = events.filter { !it.isTest }.let { if (settings.pro) it else it.take(14) }

    SkyBackground(Sky.Dawn) {
        Column(Modifier.fillMaxSize()) {
            TopBar("Your mornings", onBack)
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = Space.xl, vertical = Space.md),
                verticalArrangement = Arrangement.spacedBy(Space.md),
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                        StreakTile(stats.streak, stats.bestStreak, Modifier.weight(1.2f).height(200.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.md)) {
                            NumberTile("On time", stats.successRate?.let { "$it%" } ?: "–", "last 30 days", Modifier.height(94.dp))
                            NumberTile(
                                "Up in", stats.averageSecondsToWake?.let { if (it < 60) "${it}s" else "${it / 60}m ${it % 60}s" } ?: "–",
                                "on average", Modifier.height(94.dp),
                            )
                        }
                    }
                }
                item { WeekChart(stats) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                        NumberTile("Usual wake time", stats.averageWakeTime?.let { "${it.formatClock()} ${it.formatMeridiem()}" } ?: "–", "last 2 weeks", Modifier.weight(1f).height(94.dp))
                        NumberTile("Wake-ups", "${stats.totalWakeUps}", "all time", Modifier.weight(1f).height(94.dp))
                    }
                }
                item {
                    Text("RING LOG", style = MaterialTheme.typography.labelSmall, color = PaperMuted, modifier = Modifier.padding(top = Space.lg))
                    Text(
                        "Proof of every ring. If an alarm ever didn't fire, it shows here in red.",
                        style = MaterialTheme.typography.bodySmall, color = PaperMuted,
                    )
                }
                if (log.isEmpty()) {
                    item {
                        Text(
                            "Your first morning shows up here.", style = MaterialTheme.typography.bodyLarge, color = Paper,
                            modifier = Modifier.fillMaxWidth().padding(vertical = Space.xl), textAlign = TextAlign.Center,
                        )
                    }
                }
                items(log, key = { it.id }) { LogRow(it, Modifier.animateItem()) }
                if (!settings.pro && events.size > 14) {
                    item {
                        Text(
                            "See your full history with Pro", style = MaterialTheme.typography.labelLarge, color = MorningGold,
                            modifier = Modifier.fillMaxWidth().clip(CircleShape).clickable(onClick = onPro).padding(Space.lg), textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            if (!settings.pro) AdBanner(Modifier.fillMaxWidth().navigationBarsPadding())
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
            Text(if (rotation > 90f) "Best streak" else "Streak", style = MaterialTheme.typography.labelLarge, color = Night)
            Spacer(Modifier.weight(1f))
            Text("$shown", style = ClockType.hero, color = Night)
            Text(if (shown == 1) "morning in a row" else "mornings in a row", style = MaterialTheme.typography.bodyMedium, color = Night.copy(alpha = 0.8f))
            Text(if (rotation > 90f) "Tap to flip back" else "Tap to see your best", style = MaterialTheme.typography.labelSmall, color = Night.copy(alpha = 0.6f), modifier = Modifier.padding(top = Space.xs))
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
            Text("Time to get up, last 7 days", style = MaterialTheme.typography.labelMedium, color = DawnLavender)
            Spacer(Modifier.height(Space.md))
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                stats.lastSevenDays.forEach { day ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Canvas(Modifier.width(22.dp).weight(1f)) {
                            val fraction = day.secondsToWake?.let { 0.15f + 0.85f * it / maxSeconds } ?: 0.06f
                            val h = size.height * fraction * grow
                            val color = when (day.success) { true -> MorningGold; false -> Alert; null -> Paper.copy(alpha = 0.15f) }
                            drawRoundRect(color, Offset(0f, size.height - h), Size(size.width, h), CornerRadius(size.width / 2))
                        }
                        Text(day.date.dayOfWeek.name.take(1), style = MaterialTheme.typography.labelSmall, color = PaperMuted, modifier = Modifier.padding(top = Space.xs))
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
        RingOutcome.Dismissed -> if (event.wakeCheckPassed == true) Calm to "Wake-up check passed" else Calm to "Up at ${ended?.let { "${it.formatClock()} ${it.formatMeridiem()}" } ?: "–"}"
        RingOutcome.Escaped -> MorningGold to "Emergency exit"
        RingOutcome.Missed -> Alert to if (event.wakeCheckPassed == false) "Fell back asleep, alarm re-rang" else "Didn't ring: phone off or app paused"
        RingOutcome.Snoozed -> DawnLavender to "Snoozed"
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
                listOfNotNull(outcome, event.label.takeIf { it.isNotBlank() }, if (event.snoozes > 0) "${event.snoozes} snooze${if (event.snoozes > 1) "s" else ""}" else null).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall, color = PaperMuted,
            )
        }
    }
}
