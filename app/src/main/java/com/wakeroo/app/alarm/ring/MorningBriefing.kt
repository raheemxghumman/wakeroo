package com.wakeroo.app.alarm.ring

import com.wakeroo.app.ui.util.formatSeconds
import com.wakeroo.app.ui.theme.MorningGoldFixed
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.UserManagerCompat
import com.wakeroo.app.container
import com.wakeroo.app.data.Mood
import com.wakeroo.app.data.Quote
import com.wakeroo.app.data.QuoteBook
import com.wakeroo.app.data.RingKind
import com.wakeroo.app.feature.morning.MoodPicker
import com.wakeroo.app.feature.morning.WeatherGlyph
import com.wakeroo.app.feature.morning.greetingFor
import com.wakeroo.app.feature.stats.WakeStatsCalculator
import com.wakeroo.app.feature.you.AchievementDef
import com.wakeroo.app.feature.you.AchievementEngine
import com.wakeroo.app.feature.you.AchievementInput
import com.wakeroo.app.feature.you.SpinnableMedal
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.wakeroo.app.ui.brand.WakerooMark
import com.wakeroo.app.ui.theme.MorningGold
import com.wakeroo.app.ui.theme.Night
import com.wakeroo.app.ui.theme.PaperFixed
import com.wakeroo.app.ui.theme.Space
import com.wakeroo.app.ui.theme.SunriseCoral
import com.wakeroo.app.ui.theme.WakerooMotion
import com.wakeroo.app.ui.util.formatClock
import com.wakeroo.app.ui.util.formatMeridiem
import com.wakeroo.app.weather.Forecast
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/** What the morning briefing shows; anything missing is simply left out. */
private data class Briefing(
    val plan: String? = null,
    val forecast: Forecast? = null,
    val streak: Int = 0,
    val quote: Quote? = null,
    val mood: Mood? = null,
    /** A medal this morning just earned, celebrated at the top. */
    val medal: AchievementDef? = null,
)

private const val AUTO_CLOSE_MS = 12_000

/**
 * After the mission: a coral splash, then a short briefing for the day ahead (your plan,
 * the weather, your streak, a quote) and a one-tap "how do you feel". Closes by itself
 * after 12 seconds, or straight away with a tap. Can be switched off in one tap too.
 */
@Composable
fun GoodMorningScreen(result: FinishedRing, onDone: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val app = context.container
    val scope = rememberCoroutineScope()
    val splash = remember { Animatable(0f) }
    val content = remember { Animatable(0f) }
    val countdown = remember { Animatable(1f) }
    var briefing by remember { mutableStateOf(Briefing()) }
    var held by remember { mutableStateOf(false) }
    val test = result.state.kind == RingKind.Test
    val today = remember { LocalDate.now() }

    LaunchedEffect(Unit) {
        if (test || !UserManagerCompat.isUserUnlocked(context)) return@LaunchedEffect
        // Give the ring log a moment to record this morning before counting the streak.
        delay(500)
        runCatching {
            val settings = app.settings.current()
            val events = app.repository.ringEvents.first()
            val quotes = QuoteBook.all(context)
            val routine = app.morning.routine.first()
            val routineDays = if (routine.isEmpty()) 0 else app.morning.routineLog.first().groupBy { it.date }.count { it.value.size >= routine.size }
            val fresh = AchievementEngine.newlyUnlocked(
                AchievementEngine.evaluate(AchievementInput(events, settings.missionCounts, routineDays)), settings.achievements.keys,
            )
            if (fresh.isNotEmpty()) {
                val at = System.currentTimeMillis()
                app.settings.update { s -> s.copy(achievements = s.achievements + fresh.associate { it.id to at }) }
            }
            briefing = Briefing(
                medal = fresh.maxByOrNull { it.tier.ordinal },
                plan = settings.plan?.takeIf { settings.planDate == today.toString() } ?: result.state.alarm.label.takeIf { it.isNotBlank() },
                forecast = app.weather.cached()?.takeIf { System.currentTimeMillis() - it.fetchedAt < 12 * 3_600_000L },
                streak = WakeStatsCalculator.compute(events).streak,
                quote = QuoteBook.forDay(quotes, today, settings.quoteThemes),
                mood = app.morning.moods.first().firstOrNull { it.date == today.toString() }?.let { Mood.entries.getOrNull(it.mood) },
            )
            // Refresh the forecast quietly for the Morning tab (and the next time this screen shows).
            settings.weatherCity?.let { city -> launch { app.weather.forecast(city, settings.tempUnit, maxAgeMs = 3_600_000L) } }
        }
    }
    LaunchedEffect(Unit) {
        splash.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        content.animateTo(1f, WakerooMotion.bouncy())
    }
    LaunchedEffect(held) {
        if (held) return@LaunchedEffect
        countdown.animateTo(0f, tween((countdown.value * AUTO_CLOSE_MS).toInt(), easing = LinearEasing))
        onDone()
    }
    val time = remember { LocalTime.now() }
    val ink = Night

    Box(Modifier.fillMaxSize().background(Night).clickable(remember { MutableInteractionSource() }, null, onClick = onDone)) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.maxDimension * 1.2f * splash.value
            val c = Offset(size.width / 2, size.height * 0.85f)
            drawCircle(Brush.radialGradient(listOf(MorningGoldFixed, SunriseCoral), center = c, radius = r.coerceAtLeast(1f)), radius = r, center = c)
        }
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = Space.xl)
                .graphicsLayer {
                    alpha = content.value.coerceIn(0f, 1f)
                    translationY = (1 - content.value) * 80f
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // The auto-close countdown, as a thin line that drains.
            Box(Modifier.padding(top = Space.md).fillMaxWidth().height(3.dp).clip(CircleShape).background(ink.copy(alpha = 0.12f))) {
                Box(Modifier.fillMaxWidth(countdown.value).height(3.dp).clip(CircleShape).background(ink.copy(alpha = 0.45f)))
            }
            Spacer(Modifier.weight(0.6f))
            WakerooMark(Modifier.size(if (test) 180.dp else 120.dp), horizonColor = ink, sunColor = PaperFixed, leapColor = ink.copy(alpha = 0.7f), dotColor = PaperFixed)
            Text(
                if (test) "Test passed" else greetingFor(time),
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold), color = ink, textAlign = TextAlign.Center,
            )
            Text(
                if (test) "Your alarms will ring like this, even when the phone is locked."
                else "Up at ${time.formatClock()} ${time.formatMeridiem()}  ·  awake in ${formatSeconds(result.secondsToWake)}",
                style = MaterialTheme.typography.titleMedium, color = ink.copy(alpha = 0.8f),
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = Space.sm),
            )
            if (!test) {
                Spacer(Modifier.height(Space.xl))
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.md)) {
                    BriefingLine(visible = briefing.medal != null, order = 0) {
                        val medal = briefing.medal ?: return@BriefingLine
                        LaunchedEffect(medal) { haptics.performHapticFeedback(HapticFeedbackType.Confirm) }
                        SpinnableMedal(medal, unlocked = true, size = 56.dp)
                        Spacer(Modifier.width(Space.md))
                        Column {
                            Text("NEW MEDAL", style = MaterialTheme.typography.labelSmall, color = ink.copy(alpha = 0.6f))
                            Text(medal.title, style = MaterialTheme.typography.titleMedium, color = ink)
                        }
                    }
                    BriefingLine(visible = briefing.plan != null, order = 0) {
                        Icon(Icons.Rounded.EditNote, null, tint = ink)
                        Spacer(Modifier.width(Space.md))
                        Column {
                            Text("TODAY", style = MaterialTheme.typography.labelSmall, color = ink.copy(alpha = 0.6f))
                            Text(briefing.plan.orEmpty(), style = MaterialTheme.typography.titleMedium, color = ink)
                        }
                    }
                    BriefingLine(visible = briefing.forecast != null, order = 1) {
                        val f = briefing.forecast ?: return@BriefingLine
                        WeatherGlyph(f.condition, f.isDay, Modifier.size(36.dp))
                        Spacer(Modifier.width(Space.md))
                        Column {
                            Text("${Math.round(f.currentTemp)}° · ${f.condition.label}", style = MaterialTheme.typography.titleMedium, color = ink)
                            Text(
                                f.notableToday() ?: "High ${Math.round(f.high)}°, low ${Math.round(f.low)}°",
                                style = MaterialTheme.typography.bodySmall, color = ink.copy(alpha = 0.7f),
                            )
                        }
                    }
                    BriefingLine(visible = briefing.streak > 0, order = 2) {
                        Icon(Icons.Rounded.LocalFireDepartment, null, tint = ink)
                        Spacer(Modifier.width(Space.md))
                        Text(
                            "${briefing.streak} ${if (briefing.streak == 1) "morning" else "mornings"} in a row",
                            style = MaterialTheme.typography.titleMedium, color = ink,
                        )
                    }
                }
                Spacer(Modifier.height(Space.lg))
                Text("How do you feel?", style = MaterialTheme.typography.labelLarge, color = ink.copy(alpha = 0.75f))
                Spacer(Modifier.height(Space.sm))
                MoodPicker(
                    briefing.mood, size = 44, showLabels = false,
                    onPick = { m ->
                        held = true
                        briefing = briefing.copy(mood = m)
                        scope.launch {
                            app.morning.setMood(today, m)
                            delay(900)
                            onDone()
                        }
                    },
                )
                briefing.quote?.let { q ->
                    Text(
                        "“${q.text}”",
                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic), color = ink.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center, modifier = Modifier.padding(top = Space.lg),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(30.dp)).background(ink).clickable(onClick = onDone),
                contentAlignment = Alignment.Center,
            ) { Text("Start my day", style = MaterialTheme.typography.titleMedium, color = PaperFixed) }
            if (!test) {
                Text(
                    "Don't show this after alarms",
                    style = MaterialTheme.typography.labelMedium, color = ink.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = Space.sm).clip(CircleShape).clickable {
                        scope.launch {
                            runCatching { app.settings.update { it.copy(showGoodMorning = false) } }
                            onDone()
                        }
                    }.padding(Space.md),
                )
            } else Spacer(Modifier.height(Space.lg))
        }
    }
}

/** One line of the briefing that slides in on its turn, or not at all if it has nothing to say. */
@Composable
private fun BriefingLine(visible: Boolean, order: Int, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(visible) { if (visible) { delay(150L * order); shown = true } }
    AnimatedVisibility(shown, enter = slideInVertically { it / 2 } + fadeIn()) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Night.copy(alpha = 0.1f)).padding(Space.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) { content() }
    }
}

