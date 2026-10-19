package com.wakeroo.app.feature.home

import android.text.format.DateFormat
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.NightsStay
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
import com.wakeroo.app.alarm.health.Health
import com.wakeroo.app.ui.brand.AnimatedWakerooMark
import com.wakeroo.app.ui.brand.Wordmark
import com.wakeroo.app.ui.components.SkyBackground
import com.wakeroo.app.ui.components.icon
import com.wakeroo.app.ui.components.press3d
import com.wakeroo.app.ui.components.rememberDeviceTilt
import com.wakeroo.app.ui.components.sharedContainer
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
import com.wakeroo.app.ui.util.formatUntil
import com.wakeroo.app.ui.util.repeatSummary
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

@Composable
fun HomeScreen(
    vm: HomeViewModel,
    snackbar: SnackbarHostState,
    onNewAlarm: () -> Unit,
    onEdit: (Long) -> Unit,
    onSettings: () -> Unit,
    onStats: () -> Unit,
    onHealth: () -> Unit,
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
    val hour = state.next?.next?.hour ?: Instant.ofEpochMilli(state.now).atZone(ZoneId.systemDefault()).hour

    SkyBackground(Sky.forHour(hour), tilt = tilt, sunGlow = if (state.next != null) 0.35f else 0f, sunCenter = Offset(0.85f, 0.12f), intensity = 0.42f) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Space.xl, end = Space.xl, bottom = 120.dp),
        ) {
            item(key = "header") {
                Row(Modifier.statusBarsPadding().padding(top = Space.lg, bottom = Space.xl), verticalAlignment = Alignment.CenterVertically) {
                    Wordmark(fontSize = MaterialTheme.typography.headlineMedium.fontSize)
                    Spacer(Modifier.weight(1f))
                    RoundIcon(Icons.Rounded.BarChart, "Stats", onStats)
                    Spacer(Modifier.width(Space.sm))
                    RoundIcon(Icons.Rounded.Settings, "Settings", onSettings)
                }
            }
            item(key = "hero") {
                if (state.loaded) {
                    HeroCard(state, tilt, use24h, onHealth = onHealth, onCreate = onNewAlarm)
                }
            }
            item(key = "quick") {
                QuickAlarms(onQuick = { minutes ->
                    vm.quickAlarm(minutes, state.rows.lastOrNull()?.alarm)
                    scope.launch { snackbar.showSnackbar("Alarm set for ${Duration.ofMinutes(minutes.toLong()).formatUntil()} from now") }
                })
            }
            if (state.rows.isNotEmpty()) {
                item(key = "title") {
                    Text(
                        "YOUR ALARMS", style = MaterialTheme.typography.labelSmall, color = PaperMuted,
                        modifier = Modifier.padding(top = Space.xxl, bottom = Space.md),
                    )
                }
            }
            items(state.rows, key = { it.alarm.id }) { row ->
                AlarmCard(
                    row = row,
                    use24h = use24h,
                    modifier = Modifier.animateItem().padding(bottom = Space.md),
                    onClick = { onEdit(row.alarm.id) },
                    onToggle = { vm.toggle(row.alarm, it) },
                    onDelete = {
                        vm.delete(row.alarm)
                        scope.launch {
                            val r = snackbar.showSnackbar("Alarm deleted", actionLabel = "Undo", duration = SnackbarDuration.Short)
                            if (r == SnackbarResult.ActionPerformed) vm.restore(row.alarm)
                        }
                    },
                    onSkip = {
                        vm.skipNext(row)
                        scope.launch {
                            val msg = if (row.alarm.isRepeating) "Skipping the next one" else "Alarm turned off"
                            val r = snackbar.showSnackbar(msg, actionLabel = "Undo", duration = SnackbarDuration.Short)
                            if (r == SnackbarResult.ActionPerformed) {
                                if (row.alarm.isRepeating) vm.undoSkip(row.alarm) else vm.toggle(row.alarm, true)
                            }
                        }
                    },
                )
            }
        }
        NewAlarmButton(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(Space.xl), onNewAlarm)
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 96.dp))
    }
}

@Composable
private fun RoundIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .press3d()
            .clip(CircleShape)
            .background(Paper.copy(alpha = 0.08f))
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = Paper) }
}

/** The next alarm, on a glass card that leans with the phone like a real object. */
@Composable
private fun HeroCard(state: HomeState, tilt: Offset, use24h: Boolean, onHealth: () -> Unit, onCreate: () -> Unit) {
    val next = state.next
    Column(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                rotationX = -tilt.y * 7f
                rotationY = tilt.x * 7f
                cameraDistance = 18f * density
            }
            .clip(RoundedCornerShape(32.dp))
            .background(Night.copy(alpha = 0.35f))
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.04f))))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(32.dp))
            .padding(Space.xl),
    ) {
        if (next == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedWakerooMark(Modifier.size(84.dp))
                Spacer(Modifier.width(Space.lg))
                Column {
                    Text(if (state.rows.isEmpty()) "No alarms yet" else "All alarms are off", style = MaterialTheme.typography.headlineSmall, color = Paper)
                    Text("Tap + to set one in seconds.", style = MaterialTheme.typography.bodyMedium, color = PaperMuted)
                }
            }
            if (state.rows.isEmpty()) {
                Spacer(Modifier.height(Space.lg))
                Box(
                    Modifier.fillMaxWidth().height(56.dp).press3d().clip(CircleShape).background(SunriseCoral).clickable(onClick = onCreate),
                    contentAlignment = Alignment.Center,
                ) { Text("Set your first alarm", style = MaterialTheme.typography.labelLarge, color = Night) }
            }
            return@Column
        }
        val until = Duration.between(Instant.ofEpochMilli(state.now), next.next!!.toInstant())
        Text("NEXT ALARM", style = MaterialTheme.typography.labelSmall, color = DawnLavender)
        Text("in ${until.formatUntil()}", style = MaterialTheme.typography.displaySmall, color = Paper, modifier = Modifier.padding(top = Space.xs))
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = Space.sm)) {
            Text(formatClock(next.alarm.hour, next.alarm.minute, use24h), style = ClockType.card, color = Paper)
            Text(
                " ${formatMeridiem(next.alarm.hour, use24h)}  ·  ${next.next.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.titleMedium, color = PaperMuted, modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        if (next.alarm.label.isNotBlank()) {
            Text(next.alarm.label, style = MaterialTheme.typography.bodyLarge, color = Paper, modifier = Modifier.padding(top = Space.xs))
        }
        Spacer(Modifier.height(Space.lg))
        val (dot, text) = when (state.health) {
            Health.Good -> Calm to "Ready to ring"
            Health.Warn -> MorningGold to "Ready · one setting could be better"
            Health.Bad -> Alert to "Needs attention before it can ring"
        }
        Row(
            Modifier.clip(CircleShape).background(Night.copy(alpha = 0.35f)).clickable(onClick = onHealth).padding(horizontal = Space.md, vertical = Space.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(dot))
            Spacer(Modifier.width(Space.sm))
            Text(text, style = MaterialTheme.typography.labelMedium, color = Paper)
        }
    }
}

@Composable
private fun QuickAlarms(onQuick: (Int) -> Unit) {
    val haptics = LocalHapticFeedback.current
    Row(
        Modifier.fillMaxWidth().padding(top = Space.lg).horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        listOf(10 to "+10 min", 20 to "+20 min", 30 to "+30 min", 60 to "+1 h", 480 to "In 8 h").forEach { (m, label) ->
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
    val on = alarm.enabled
    val contentAlpha by animateFloatAsState(if (on) 1f else 0.45f, WakerooMotion.calm(), label = "alpha")
    val surface by animateColorAsState(if (on) NightSurface.copy(alpha = 0.92f) else NightSurface.copy(alpha = 0.55f), WakerooMotion.calm(), label = "surface")

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
                Text(if (toStart) "Delete" else if (alarm.isRepeating) "Skip next" else "Turn off", style = MaterialTheme.typography.labelLarge, color = Night)
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
                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(28.dp))
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
                        listOfNotNull(alarm.label.takeIf { it.isNotBlank() }, alarm.repeatSummary()).joinToString("  ·  "),
                        style = MaterialTheme.typography.bodyMedium, color = PaperMuted, maxLines = 1,
                    )
                }
                Switch(
                    checked = on,
                    onCheckedChange = {
                        haptics.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                        onToggle(it)
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = SunriseCoral, checkedThumbColor = Night, uncheckedTrackColor = Night, uncheckedBorderColor = PaperMuted),
                )
            }
            Row(Modifier.padding(top = Space.md).graphicsLayer { alpha = contentAlpha }, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                alarm.missions.forEach { m ->
                    Row(
                        Modifier.clip(CircleShape).background(MorningGold.copy(alpha = 0.14f)).padding(horizontal = Space.md, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(m.type.icon, null, tint = MorningGold, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(m.type.title, style = MaterialTheme.typography.labelSmall, color = MorningGold)
                    }
                }
                if (alarm.skipDate != null) {
                    Text("Next one skipped", style = MaterialTheme.typography.labelSmall, color = DawnLavender, modifier = Modifier.align(Alignment.CenterVertically))
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
        Text("Alarm", style = MaterialTheme.typography.titleMedium, color = Night)
    }
}

