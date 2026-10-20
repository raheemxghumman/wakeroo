package com.wakeroo.app.feature.morning

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wakeroo.app.alarm.NextTriggerCalculator
import com.wakeroo.app.container
import com.wakeroo.app.data.Mood
import com.wakeroo.app.data.Quote
import com.wakeroo.app.data.QuoteBook
import com.wakeroo.app.data.Settings
import com.wakeroo.app.data.db.RingOutcome
import com.wakeroo.app.feature.main.TabContentBottomPadding
import com.wakeroo.app.feature.stats.WakeStatsCalculator
import com.wakeroo.app.ui.components.EntranceStyle
import com.wakeroo.app.ui.components.SectionCard
import com.wakeroo.app.ui.components.SkyBackground
import com.wakeroo.app.ui.components.TabHeader
import com.wakeroo.app.ui.components.entrance
import com.wakeroo.app.ui.components.rememberDeviceTilt
import com.wakeroo.app.ui.theme.ClockType
import com.wakeroo.app.ui.theme.Hairline
import com.wakeroo.app.ui.theme.Glass
import com.wakeroo.app.ui.theme.MorningGold
import com.wakeroo.app.ui.theme.Night
import com.wakeroo.app.ui.theme.Paper
import com.wakeroo.app.ui.theme.PaperMuted
import com.wakeroo.app.ui.theme.Sky
import com.wakeroo.app.ui.theme.Space
import com.wakeroo.app.ui.theme.SunriseCoral
import com.wakeroo.app.ui.util.formatClock
import com.wakeroo.app.ui.util.formatMeridiem
import com.wakeroo.app.weather.Forecast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun greetingFor(time: LocalTime): String = when (time.hour) {
    in 4..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Good night"
}

@Composable
fun MorningScreen(onSettings: () -> Unit) {
    val context = LocalContext.current
    val app = context.container
    val scope = rememberCoroutineScope()
    val settings by app.settings.settings.collectAsStateWithLifecycle(Settings())
    val alarms by app.repository.alarms.collectAsStateWithLifecycle(emptyList())
    val events by app.repository.ringEvents.collectAsStateWithLifecycle(emptyList())
    val moods by app.morning.moods.collectAsStateWithLifecycle(emptyList())
    val routine by app.morning.routine.collectAsStateWithLifecycle(emptyList())
    val routineLog by app.morning.routineLog.collectAsStateWithLifecycle(emptyList())
    val saved by app.morning.savedQuotes.collectAsStateWithLifecycle(emptyList())
    var quotes by remember { mutableStateOf<List<Quote>>(emptyList()) }
    var forecast by remember { mutableStateOf<Forecast?>(app.weather.cached()) }
    var loadingWeather by remember { mutableStateOf(false) }
    var pickingCity by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        app.morning.ensureDefaultRoutine()
        quotes = QuoteBook.all(context)
        while (true) { delay(30_000); now = System.currentTimeMillis() }
    }
    LaunchedEffect(settings.weatherCity, settings.tempUnit) {
        val city = settings.weatherCity ?: return@LaunchedEffect
        loadingWeather = true
        forecast = app.weather.forecast(city, settings.tempUnit)
        loadingWeather = false
    }
    val zone = ZoneId.systemDefault()
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val time = Instant.ofEpochMilli(now).atZone(zone).toLocalTime()
    val use24h = settings.use24Hour ?: DateFormat.is24HourFormat(context)
    val todayMood = moods.firstOrNull { it.date == today.toString() }?.let { Mood.entries.getOrNull(it.mood) }
    val doneToday = routineLog.filter { it.date == today.toString() }.map { it.itemId }.toSet()
    val stats = remember(events) { WakeStatsCalculator.compute(events) }
    val wokeToday = events.firstOrNull { !it.isTest && it.outcome == RingOutcome.Dismissed && it.wakeCheckPassed != true && it.endedAt != null &&
        Instant.ofEpochMilli(it.endedAt).atZone(zone).toLocalDate() == today }
    val nextAlarm = alarms.mapNotNull { NextTriggerCalculator.nextMillis(it, now) }.minOrNull()
    val tilt by rememberDeviceTilt()

    SkyBackground(Sky.forHour(time.hour), intensity = 0.55f, tilt = tilt) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Space.xl, end = Space.xl, bottom = TabContentBottomPadding),
            verticalArrangement = Arrangement.spacedBy(Space.lg),
        ) {
            item(key = "header") {
                TabHeader(
                    greetingFor(time), onSettings = onSettings,
                    eyebrow = DateTimeFormatter.ofPattern("EEEE d MMMM").format(today),
                    modifier = Modifier.entrance(0),
                )
            }
            item(key = "hero") {
                Column(
                    Modifier
                        .entrance(1, EntranceStyle.Flip)
                        .fillMaxWidth()
                        .graphicsLayer { rotationX = -tilt.y * 5f; rotationY = tilt.x * 5f; cameraDistance = 18f * density }
                        .clip(RoundedCornerShape(30.dp))
                        .background(Glass)
                        .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.03f))))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(30.dp))
                        .padding(Space.xl),
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(time.formatClock(use24h), style = ClockType.hero, color = Paper)
                        Text(" ${time.formatMeridiem(use24h)}", style = MaterialTheme.typography.titleMedium, color = PaperMuted, modifier = Modifier.padding(bottom = 10.dp))
                    }
                    val line = when {
                        wokeToday != null -> {
                            val up = Instant.ofEpochMilli(wokeToday.endedAt!!).atZone(zone).toLocalTime()
                            "Up since ${up.formatClock(use24h)} ${up.formatMeridiem(use24h)}${wokeToday.secondsToWake?.let { s -> " · awake in ${if (s < 60) "${s}s" else "${s / 60} min"}" } ?: ""}"
                        }
                        nextAlarm != null -> {
                            val at = Instant.ofEpochMilli(nextAlarm).atZone(zone)
                            val day = if (at.toLocalDate() == today) "today" else if (at.toLocalDate() == today.plusDays(1)) "tomorrow" else at.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
                            "Next alarm ${at.toLocalTime().formatClock(use24h)} ${at.toLocalTime().formatMeridiem(use24h)} $day"
                        }
                        else -> "No alarm set"
                    }
                    Text(line, style = MaterialTheme.typography.bodyLarge, color = Paper.copy(alpha = 0.85f))
                    val plan = settings.plan?.takeIf { settings.planDate == today.toString() }
                    if (plan != null) {
                        Spacer(Modifier.height(Space.lg))
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MorningGold.copy(alpha = 0.16f)).padding(Space.md),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.EditNote, null, tint = MorningGold)
                            Spacer(Modifier.width(Space.md))
                            Column {
                                Text("TODAY'S PLAN", style = MaterialTheme.typography.labelSmall, color = MorningGold)
                                Text(plan, style = MaterialTheme.typography.titleMedium, color = Paper)
                            }
                        }
                    }
                }
            }
            item(key = "weather") {
                WeatherCard(forecast, settings.weatherCity, loadingWeather, Modifier.entrance(2), onPickCity = { pickingCity = true })
            }
            item(key = "quote") {
                SectionCard("Quote of the day", icon = Icons.Rounded.FormatQuote, modifier = Modifier.entrance(3)) {
                    if (quotes.isNotEmpty()) {
                        QuoteDeck(
                            quotes, settings.quoteThemes, saved.map { it.quoteId }.toSet(),
                            onThemes = { t -> scope.launch { app.settings.update { it.copy(quoteThemes = t) } } },
                            onSave = { q, on -> scope.launch { app.morning.setSaved(q.id, on) } },
                        )
                    }
                }
            }
            item(key = "mood") {
                SectionCard("How do you feel?", icon = Icons.Rounded.Mood, modifier = Modifier.entrance(4)) {
                    MoodPicker(todayMood, onPick = { m -> scope.launch { app.morning.setMood(today, m) } })
                    if (todayMood != null) {
                        Text(moodNote(todayMood), style = MaterialTheme.typography.bodySmall, color = PaperMuted, modifier = Modifier.padding(top = Space.md))
                    }
                }
            }
            item(key = "routine") {
                SectionCard("Morning routine", icon = Icons.Rounded.Checklist, modifier = Modifier.entrance(5)) {
                    RoutineList(
                        routine, doneToday,
                        onToggle = { item, done -> scope.launch { app.morning.setDone(today, item.id, done) } },
                        onAdd = { title -> scope.launch { app.morning.addRoutine(title, routine.size) } },
                        onDelete = { item -> scope.launch { app.morning.deleteRoutine(item) } },
                    )
                }
            }
            item(key = "streak") {
                StreakCard(stats.streak, stats.lastSevenDays.map { it.success }, Modifier.entrance(6))
            }
        }
    }
    if (pickingCity) {
        CitySheet(
            unit = settings.tempUnit,
            search = { app.weather.searchCities(it) },
            onPick = { city -> pickingCity = false; scope.launch { app.settings.update { it.copy(weatherCity = city) } } },
            onUnit = { u -> scope.launch { app.settings.update { it.copy(tempUnit = u) } } },
            onDismiss = { pickingCity = false },
        )
    }
}

private fun moodNote(m: Mood) = when (m) {
    Mood.Exhausted -> "Rough start. Water, light, and go easy on yourself today."
    Mood.Tired -> "A bit slow. Some daylight in the next hour helps a lot."
    Mood.Okay -> "Okay is a fine place to start."
    Mood.Calm -> "Nice. Hold on to that calm."
    Mood.Refreshed -> "Love that. Your sleep is paying off."
}

/** The streak, and seven little suns for the last week: gold for won mornings. */
@Composable
private fun StreakCard(streak: Int, week: List<Boolean?>, modifier: Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(SunriseCoral.copy(alpha = 0.9f), MorningGold.copy(alpha = 0.9f))))
            .border(1.dp, Hairline, RoundedCornerShape(28.dp))
            .padding(Space.xl),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.LocalFireDepartment, null, tint = Night, modifier = Modifier.size(36.dp))
        Spacer(Modifier.width(Space.md))
        Column(Modifier.weight(1f)) {
            Text("$streak", style = ClockType.card, color = Night)
            Text(if (streak == 1) "morning in a row" else "mornings in a row", style = MaterialTheme.typography.labelMedium, color = Night.copy(alpha = 0.75f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            week.forEach { s ->
                Canvas(Modifier.size(16.dp)) {
                    val r = size.minDimension / 2
                    when (s) {
                        true -> {
                            drawCircle(Color.White, r * 0.62f)
                            for (k in 0 until 8) {
                                val a = k * 0.785f
                                drawLine(Color.White, center + Offset(kotlin.math.cos(a), kotlin.math.sin(a)) * (r * 0.78f), center + Offset(kotlin.math.cos(a), kotlin.math.sin(a)) * r, 1.4f.dp.toPx())
                            }
                        }
                        false -> drawCircle(Night.copy(alpha = 0.55f), r * 0.55f)
                        null -> drawCircle(Night.copy(alpha = 0.2f), r * 0.4f)
                    }
                }
            }
        }
    }
}
