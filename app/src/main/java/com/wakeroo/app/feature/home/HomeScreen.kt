package com.wakeroo.app.feature.home

import com.wakeroo.app.ui.i18n.tr
import com.wakeroo.app.ui.util.full
import com.wakeroo.app.ui.util.initial
import com.wakeroo.app.ui.theme.MorningGoldFixed
import com.wakeroo.app.ui.theme.DawnLavenderFixed
import android.text.format.DateFormat
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wakeroo.app.BuildConfig
import com.wakeroo.app.alarm.health.Health
import com.wakeroo.app.alarm.health.OemGuides
import com.wakeroo.app.data.model.SoundMode
import com.wakeroo.app.feature.editor.soundTitle
import com.wakeroo.app.feature.main.MainTab
import com.wakeroo.app.feature.stats.WakeStatsCalculator
import com.wakeroo.app.feature.you.Medal
import com.wakeroo.app.feature.you.Recap
import com.wakeroo.app.feature.you.WeekRecap
import com.wakeroo.app.reminders.BedtimeCalculator
import com.wakeroo.app.ui.scenes.Scene
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.rounded.Close
import com.wakeroo.app.ui.brand.AnimatedWakerooMark
import com.wakeroo.app.ui.brand.Wordmark
import com.wakeroo.app.feature.main.BottomBarHeight
import com.wakeroo.app.feature.main.TabContentBottomPadding
import com.wakeroo.app.ui.components.EntranceStyle
import com.wakeroo.app.ui.components.TabHeader
import com.wakeroo.app.missions.HarderMode
import com.wakeroo.app.missions.MissionGateDialog
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.wakeroo.app.ui.components.SkyBackground
import com.wakeroo.app.ui.components.entrance
import com.wakeroo.app.ui.components.icon
import com.wakeroo.app.ui.components.press3d
import com.wakeroo.app.ui.components.rememberDeviceTilt
import com.wakeroo.app.ui.components.sharedContainer
import com.wakeroo.app.ui.theme.Alert
import com.wakeroo.app.ui.theme.Calm
import com.wakeroo.app.ui.theme.ClockType
import com.wakeroo.app.ui.theme.DawnLavender
import com.wakeroo.app.ui.theme.Glass
import com.wakeroo.app.ui.theme.MorningGold
import com.wakeroo.app.ui.theme.Night
import com.wakeroo.app.ui.theme.NightSurface
import com.wakeroo.app.ui.theme.NightSurfaceHigh
import com.wakeroo.app.ui.theme.Paper
import com.wakeroo.app.ui.theme.PaperMuted
import com.wakeroo.app.ui.theme.Sky
import com.wakeroo.app.ui.theme.Space
import com.wakeroo.app.ui.theme.SunriseCoral
import com.wakeroo.app.ui.theme.WakerooMotion
import com.wakeroo.app.ui.util.formatClock
import com.wakeroo.app.ui.util.formatMeridiem
import com.wakeroo.app.ui.util.formatUntil
import com.wakeroo.app.ui.util.repeatSummary
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import com.wakeroo.app.ui.util.formatHours

@Composable
fun HomeScreen(
    vm: HomeViewModel,
    snackbar: SnackbarHostState,
    onNewAlarm: () -> Unit,
    onEdit: (Long) -> Unit,
    onSettings: () -> Unit,
    onHealth: () -> Unit,
    onTab: (MainTab) -> Unit = {},
    highlightId: Long = 0L,
    onHighlightShown: () -> Unit = {},
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val tilt by rememberDeviceTilt()
    val context = LocalContext.current
    val use24h = state.settings.use24Hour ?: DateFormat.is24HourFormat(context)
    val scope = rememberCoroutineScope()
    LifecycleResumeEffect(Unit) {
        vm.refreshHealth()
        onPauseOrDispose { }
    }
    var gate by remember { mutableStateOf<Triple<com.wakeroo.app.data.model.Alarm, String, () -> Unit>?>(null) }
    gate?.let { (alarm, title, action) ->
        MissionGateDialog(
            mission = alarm.missions.firstOrNull() ?: HarderMode.SNOOZE_MISSION, title = title,
            onPassed = { gate = null; action() }, onCancel = { gate = null },
        )
    }
    LaunchedEffect(highlightId) {
        if (highlightId != 0L) {
            kotlinx.coroutines.delay(4000)
            onHighlightShown()
        }
    }
    val hour = state.next?.next?.hour ?: Instant.ofEpochMilli(state.now).atZone(ZoneId.systemDefault()).hour

    SkyBackground(Sky.forHour(hour), tilt = tilt, sunGlow = if (state.next != null) 0.35f else 0f, sunCenter = Offset(0.85f, 0.12f), intensity = 0.42f) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Space.xl, end = Space.xl, bottom = TabContentBottomPadding + 72.dp),
        ) {
            item(key = "header") {
                TabHeader(
                    title = tr("Alarms"), onSettings = onSettings, modifier = Modifier.entrance(0),
                    lead = { Wordmark(fontSize = MaterialTheme.typography.headlineMedium.fontSize) },
                )
            }
            // Something could stop an alarm: say so first, with the fix one tap away.
            val problem = state.healthItems.firstOrNull { it.status == Health.Bad }
                ?: state.healthItems.firstOrNull { it.status == Health.Warn }?.takeIf { state.now > state.settings.fixBannerHiddenUntil }
            if (problem != null && state.loaded) {
                item(key = "fix") {
                    FixBanner(
                        problem,
                        onFix = {
                            val fix = problem.fix
                            when {
                                fix != null -> OemGuides.open(context, fix())
                                problem.id == "test" -> {
                                    vm.ringTest()
                                    scope.launch { snackbar.showSnackbar(tr("Test alarm in 1 minute. Lock your phone and wait")) }
                                }
                                else -> onHealth()
                            }
                        },
                        onHide = if (problem.status == Health.Warn) { { vm.hideBanner() } } else null,
                        modifier = Modifier.padding(bottom = Space.md).animateItem().entrance(1),
                    )
                }
            }
            item(key = "hero") {
                if (state.loaded) {
                    val zone = ZoneId.systemDefault()
                    val now = Instant.ofEpochMilli(state.now).atZone(zone).toLocalTime()
                    val next = state.next
                    Box(Modifier.entrance(1, EntranceStyle.Flip)) {
                        OrbitHero(
                            nowMinutes = now.hour * 60f + now.minute,
                            alarmMinutes = next?.let { it.alarm.hour * 60f + it.alarm.minute },
                            untilText = next?.next?.let { Duration.between(Instant.ofEpochMilli(state.now), it.toInstant()).formatUntil() },
                            timeText = next?.let { formatClock(it.alarm.hour, it.alarm.minute, use24h) } ?: "",
                            meridiem = next?.let { formatMeridiem(it.alarm.hour, use24h) } ?: "",
                            dayText = next?.next?.let { dayName(it.toLocalDate(), state.now) } ?: "",
                            label = next?.alarm?.label.orEmpty(),
                            health = state.health,
                            tilt = tilt,
                            empty = next == null,
                            onCreate = onNewAlarm,
                            onHealth = onHealth,
                        ) {
                            next?.let { HeroBack(it, state.health, onEdit = { onEdit(it.alarm.id) }, onHealth = onHealth) }
                        }
                    }
                }
            }
            item(key = "quick") {
                QuickAlarms(modifier = Modifier.entrance(2), onQuick = { minutes ->
                    vm.quickAlarm(minutes, state.rows.lastOrNull()?.alarm)
                    scope.launch { snackbar.showSnackbar(tr("Alarm set for {0} from now", Duration.ofMinutes(minutes.toLong()).formatUntil())) }
                })
            }
            bedtimeFor(state)?.let { bed ->
                item(key = "bedtime") {
                    BedtimeCard(
                        bedText = bed.first, sleepText = bed.second, closeness = bed.third,
                        onSleepSounds = { onTab(MainTab.Sleep) },
                        modifier = Modifier.padding(top = Space.lg).entrance(3),
                    )
                }
            }
            if (state.events.any { !it.isTest }) {
                item(key = "streak") {
                    val stats = remember(state.events) { WakeStatsCalculator.compute(state.events) }
                    StreakStrip(
                        week = stats.lastSevenDays.map { it.date.dayOfWeek.initial() to it.success },
                        streak = stats.streak,
                        onClick = { onTab(MainTab.You) },
                        modifier = Modifier.padding(top = Space.md).entrance(3),
                    )
                }
            }
            mondayRecap(state)?.let { (week, recap) ->
                item(key = "recap") {
                    MondayRecap(recap, onOpen = { onTab(MainTab.You) }, onDismiss = { vm.dismissRecap(week) }, modifier = Modifier.padding(top = Space.md).animateItem())
                }
            }
            if (state.rows.isNotEmpty()) {
                item(key = "title") {
                    Text(
                        tr("YOUR ALARMS"), style = MaterialTheme.typography.labelSmall, color = PaperMuted,
                        modifier = Modifier.padding(top = Space.xxl, bottom = Space.md).entrance(3),
                    )
                }
            }
            itemsIndexed(state.rows, key = { _, it -> it.alarm.id }) { index, row ->
                AlarmCard(
                    row = row,
                    use24h = use24h,
                    glow = row.alarm.id == highlightId,
                    modifier = Modifier.animateItem().padding(bottom = Space.md).entrance(4 + index.coerceAtMost(6)),
                    onClick = { onEdit(row.alarm.id) },
                    onToggle = { on ->
                        val toggle = { vm.toggle(row.alarm, on); Unit }
                        if (!on && row.alarm.hardMode) gate = Triple(row.alarm, tr("Turn this alarm off"), toggle) else toggle()
                    },
                    onDelete = {
                        val delete = {
                            vm.delete(row.alarm)
                            scope.launch {
                                val r = snackbar.showSnackbar(tr("Alarm deleted"), actionLabel = tr("Undo"), duration = SnackbarDuration.Short)
                                if (r == SnackbarResult.ActionPerformed) vm.restore(row.alarm)
                            }
                            Unit
                        }
                        if (row.alarm.hardMode) gate = Triple(row.alarm, tr("Delete this alarm"), delete) else delete()
                    },
                    onSkip = {
                        val skip = {
                            vm.skipNext(row)
                            scope.launch {
                                val msg = if (row.alarm.isRepeating) tr("Skipping the next one") else tr("Alarm turned off")
                                val r = snackbar.showSnackbar(msg, actionLabel = tr("Undo"), duration = SnackbarDuration.Short)
                                if (r == SnackbarResult.ActionPerformed) {
                                    if (row.alarm.isRepeating) vm.undoSkip(row.alarm) else vm.toggle(row.alarm, true)
                                }
                            }
                            Unit
                        }
                        if (row.alarm.hardMode) gate = Triple(row.alarm, if (row.alarm.isRepeating) tr("Skip the next one") else tr("Turn this alarm off"), skip) else skip()
                    },
                )
            }
        }
        NewAlarmButton(
            Modifier.align(Alignment.BottomEnd).navigationBarsPadding()
                .padding(end = Space.xl, bottom = BottomBarHeight + 28.dp).entrance(5, EntranceStyle.Pop),
            onNewAlarm,
        )
    }
}

/** The back of the hero card: everything about the next alarm at a glance. */
@Composable
private fun HeroBack(row: AlarmRow, health: Health, onEdit: () -> Unit, onHealth: () -> Unit) {
    val a = row.alarm
    Column(Modifier.fillMaxSize()) {
        Text(tr("ABOUT THIS ALARM"), style = MaterialTheme.typography.labelSmall, color = DawnLavender)
        Spacer(Modifier.height(Space.md))
        HeroFact(tr("Missions"), a.missions.joinToString(" · ") { if (it.needsSetup) tr("{0} (finish setup)", it.type.title) else it.type.title })
        HeroFact(tr("Sound"), a.soundTitle() + when (a.soundMode) { SoundMode.Rotate -> tr(" · rotating"); SoundMode.Escalate -> tr(" · escalates"); else -> "" })
        HeroFact(tr("Scene"), Scene.byId(a.scene).title)
        HeroFact(tr("Snooze"), if (a.snoozeLimit == 0) tr("Off") else tr("{0} min, {1} times", a.snoozeMinutes, a.snoozeLimit))
        HeroFact(tr("Wake-up check"), if (a.wakeCheckMinutes == 0) tr("Off") else tr("After {0} min", a.wakeCheckMinutes))
        if (a.hardMode) HeroFact(tr("Harder Mode"), tr("On"))
        Spacer(Modifier.weight(1f))
        Row {
            Text(
                tr("Edit alarm"), style = MaterialTheme.typography.labelLarge, color = Night,
                modifier = Modifier.clip(CircleShape).background(SunriseCoral).clickable(onClick = onEdit).padding(horizontal = Space.lg, vertical = Space.sm),
            )
            Spacer(Modifier.width(Space.sm))
            Text(
                when (health) { Health.Good -> tr("Ready to ring"); Health.Warn -> tr("Check health"); Health.Bad -> tr("Needs a fix") },
                style = MaterialTheme.typography.labelLarge, color = Paper,
                modifier = Modifier.clip(CircleShape).background(Glass).clickable(onClick = onHealth).padding(horizontal = Space.lg, vertical = Space.sm),
            )
        }
    }
}

@Composable
private fun HeroFact(name: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(name, style = MaterialTheme.typography.bodySmall, color = PaperMuted, modifier = Modifier.width(110.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, color = Paper, maxLines = 1)
    }
}

private fun dayName(date: java.time.LocalDate, now: Long): String {
    val today = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()
    return when (date) {
        today -> tr("Today")
        today.plusDays(1) -> tr("Tomorrow")
        else -> date.dayOfWeek.full()
    }
}

/** From 6 PM until the alarm: "Bed by 10:45", the sleep it buys, and how close bedtime is (0..1). */
private fun bedtimeFor(state: HomeState): Triple<String, String, Float>? {
    val zone = ZoneId.systemDefault()
    val now = Instant.ofEpochMilli(state.now).atZone(zone)
    if (now.hour in 4 until 18) return null
    val bed = BedtimeCalculator.next(state.rows.map { it.alarm }, state.settings.sleepGoalMinutes, 0, state.now) ?: return null
    val bedTime = Instant.ofEpochMilli(bed.bedAt).atZone(zone).toLocalTime()
    val use24 = state.settings.use24Hour ?: false
        val left = bed.bedAt - state.now
    val closeness = (1f - left / (4 * 3_600_000f)).coerceIn(0f, 1f)
    val sleep = if (left > 0) tr("{0} of sleep before your alarm", formatHours(state.settings.sleepGoalMinutes))
        else tr("Past bedtime. Every minute of sleep still counts")
    return Triple("${bedTime.formatClock(use24)} ${bedTime.formatMeridiem(use24)}", sleep, closeness)
}

/** On Mondays, last week's recap until dismissed. */
private fun mondayRecap(state: HomeState): Pair<String, WeekRecap>? {
    val today = Instant.ofEpochMilli(state.now).atZone(ZoneId.systemDefault()).toLocalDate()
    if (today.dayOfWeek != java.time.DayOfWeek.MONDAY) return null
    val week = today.minusWeeks(1).let { "${it.get(java.time.temporal.IsoFields.WEEK_BASED_YEAR)}-W${it.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR)}" }
    if (state.settings.recapDismissedWeek == week) return null
    val recap = Recap.forWeek(state.events, state.settings.achievements, today.minusWeeks(1))
    if (recap.won == 0 && recap.medal == null) return null
    return week to recap
}

@Composable
private fun MondayRecap(recap: WeekRecap, onOpen: () -> Unit, onDismiss: () -> Unit, modifier: Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF3A2B4E), Color(0xFF6B4A78))))
            .clickable(onClick = onOpen)
            .padding(Space.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        recap.medal?.let { Medal(it, unlocked = true, size = 44.dp); Spacer(Modifier.width(Space.md)) }
        Column(Modifier.weight(1f)) {
            Text(tr("LAST WEEK"), style = MaterialTheme.typography.labelSmall, color = MorningGold)
            Text(tr("{0} of 7 mornings won", recap.won), style = MaterialTheme.typography.titleSmall, color = Color(0xFFF5F3EE))
            recap.averageWake?.let { Text(tr("Usually up at {0}", it), style = MaterialTheme.typography.bodySmall, color = Color(0xFFF5F3EE).copy(alpha = 0.75f)) }
        }
        Icon(Icons.Rounded.Close, tr("Dismiss"), tint = Color(0xFFF5F3EE).copy(alpha = 0.7f), modifier = Modifier.clip(CircleShape).clickable(onClick = onDismiss).padding(6.dp))
    }
}

@Composable
private fun QuickAlarms(modifier: Modifier = Modifier, onQuick: (Int) -> Unit) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier.fillMaxWidth().padding(top = Space.lg).horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        val presets = listOf(10 to tr("+10 min"), 20 to tr("+20 min"), 30 to tr("+30 min"), 60 to tr("+1 h"), 480 to tr("In 8 h"))
        // Debug builds get a one-minute alarm for testing the full ring flow.
        (if (BuildConfig.DEBUG) listOf(1 to tr("+1 min")) + presets else presets).forEach { (m, label) ->
            Row(
                Modifier
                    .press3d()
                    .clip(CircleShape)
                    .background(Paper.copy(alpha = 0.08f))
                    .border(1.dp, Paper.copy(alpha = 0.1f), CircleShape)
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        onQuick(m)
                    }
                    .padding(horizontal = Space.lg, vertical = Space.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (m == 480) {
                    Icon(Icons.Rounded.NightsStay, null, tint = DawnLavender, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(label, style = MaterialTheme.typography.labelMedium, color = Paper)
            }
        }
    }
}

@Composable
private fun AlarmCard(
    row: AlarmRow,
    use24h: Boolean,
    glow: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onSkip: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val dismiss = rememberSwipeToDismissBoxState()
    LaunchedEffect(dismiss.currentValue) {
        when (dismiss.currentValue) {
            SwipeToDismissBoxValue.EndToStart -> { onDelete(); dismiss.snapTo(SwipeToDismissBoxValue.Settled) }
            SwipeToDismissBoxValue.StartToEnd -> { onSkip(); dismiss.snapTo(SwipeToDismissBoxValue.Settled) }
            SwipeToDismissBoxValue.Settled -> Unit
        }
    }
    val alarm = row.alarm
    // An enabled one-off whose time has passed is effectively off; show it that way.
    val passed = alarm.enabled && row.next == null
    val on = alarm.enabled && !passed
    val sky = Sky.forHour(alarm.hour)
    val contentAlpha by animateFloatAsState(if (on) 1f else 0.45f, WakerooMotion.calm(), label = "alpha")
    val surface by animateColorAsState(if (on) NightSurface.copy(alpha = 0.92f) else NightSurface.copy(alpha = 0.55f), WakerooMotion.calm(), label = "surface")
    // A freshly created alarm pulses a warm outline a few times so the eye finds it.
    val pulse by rememberInfiniteTransition(label = "glow").animateFloat(
        0.2f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "glowPulse",
    )
    val glowAlpha by animateFloatAsState(if (glow) 1f else 0f, tween(600), label = "glowAlpha")
    val outline = if (glowAlpha > 0.01f) SunriseCoral.copy(alpha = pulse * glowAlpha) else Color.White.copy(alpha = 0.06f)

    SwipeToDismissBox(
        state = dismiss,
        modifier = modifier,
        backgroundContent = {
            if (dismiss.dismissDirection == SwipeToDismissBoxValue.Settled) return@SwipeToDismissBox
            val toStart = dismiss.dismissDirection == SwipeToDismissBoxValue.EndToStart
            Row(
                Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp))
                    .background(if (toStart) Alert.copy(alpha = 0.85f) else DawnLavender.copy(alpha = 0.85f))
                    .padding(horizontal = Space.xl),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (toStart) Arrangement.End else Arrangement.Start,
            ) {
                Icon(if (toStart) Icons.Rounded.DeleteOutline else Icons.Rounded.SkipNext, null, tint = Night)
                Spacer(Modifier.width(Space.sm))
                Text(if (toStart) tr("Delete") else if (alarm.isRepeating) tr("Skip next") else tr("Turn off"), style = MaterialTheme.typography.labelLarge, color = Night)
            }
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .sharedContainer("alarm-${alarm.id}")
                .press3d(maxTilt = 4f, sink = 0.98f)
                .clip(RoundedCornerShape(28.dp))
                .background(surface)
                .background(Brush.horizontalGradient(listOf(sky.top.copy(alpha = if (on) 0.28f else 0.1f), Color.Transparent)))
                .border(if (glowAlpha > 0.01f) 2.dp else 1.dp, outline, RoundedCornerShape(28.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = Space.xl, vertical = Space.lg),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).graphicsLayer { alpha = contentAlpha }) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(formatClock(alarm.hour, alarm.minute, use24h), style = ClockType.card, color = Paper)
                        Text(" ${formatMeridiem(alarm.hour, use24h)}", style = MaterialTheme.typography.titleMedium, color = PaperMuted, modifier = Modifier.padding(bottom = 6.dp))
                    }
                    Text(
                        listOfNotNull(alarm.label.takeIf { it.isNotBlank() }, if (passed) tr("Passed") else alarm.repeatSummary()).joinToString("  ·  "),
                        style = MaterialTheme.typography.bodyMedium, color = PaperMuted, maxLines = 1,
                    )
                }
                SkyGlyph(alarm.hour, Modifier.padding(end = Space.md).size(22.dp))
                Switch(
                    checked = on,
                    onCheckedChange = {
                        haptics.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                        onToggle(it)
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = SunriseCoral, checkedThumbColor = Night, uncheckedTrackColor = NightSurfaceHigh, uncheckedThumbColor = PaperMuted, uncheckedBorderColor = PaperMuted),
                )
            }
            Row(Modifier.padding(top = Space.md).graphicsLayer { alpha = contentAlpha }, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                alarm.missions.forEach { m ->
                    // A mission saved with "I'll set it up tonight" shows up coral until it's done.
                    val tint = if (m.needsSetup) SunriseCoral else MorningGold
                    Row(
                        Modifier.clip(CircleShape).background(tint.copy(alpha = 0.14f)).padding(horizontal = Space.md, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(m.type.icon, null, tint = tint, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (m.needsSetup) tr("{0} · finish setup", m.type.title) else m.type.title, style = MaterialTheme.typography.labelSmall, color = tint)
                    }
                }
                if (alarm.hardMode) {
                    Row(
                        Modifier.clip(CircleShape).background(DawnLavender.copy(alpha = 0.16f)).padding(horizontal = Space.sm, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) { Icon(Icons.Rounded.Lock, tr("Harder Mode"), tint = DawnLavender, modifier = Modifier.size(14.dp)) }
                }
                if (alarm.skipDate != null) {
                    Text(tr("Next one skipped"), style = MaterialTheme.typography.labelSmall, color = DawnLavender, modifier = Modifier.align(Alignment.CenterVertically))
                }
            }
        }
    }
}

@Composable
private fun NewAlarmButton(modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .sharedContainer("alarm-new", corner = 32)
            .press3d()
            .clip(RoundedCornerShape(32.dp))
            .background(Brush.horizontalGradient(listOf(SunriseCoral, Color(0xFFFF9466))))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Space.xl, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Add, null, tint = Night)
        Spacer(Modifier.width(Space.sm))
        Text(tr("Alarm"), style = MaterialTheme.typography.titleMedium, color = Night)
    }
}


/** A tiny sun for daytime alarms and a crescent for night ones, so the list reads at a glance. */
@Composable
private fun SkyGlyph(hour: Int, modifier: Modifier) {
    Canvas(modifier) {
        val r = size.minDimension / 2
        if (hour in 6..18) {
            drawCircle(MorningGoldFixed, r * 0.5f)
            for (k in 0 until 8) {
                val a = k * 0.785f
                drawLine(MorningGoldFixed, center + Offset(kotlin.math.cos(a), kotlin.math.sin(a)) * (r * 0.68f), center + Offset(kotlin.math.cos(a), kotlin.math.sin(a)) * r, 1.4f.dp.toPx())
            }
        } else {
            drawCircle(DawnLavenderFixed, r * 0.62f)
            drawCircle(Color(0xFF1A1E33), r * 0.55f, center + Offset(r * 0.32f, -r * 0.22f))
        }
    }
}
