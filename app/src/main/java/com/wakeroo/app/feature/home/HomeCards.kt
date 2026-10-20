package com.wakeroo.app.feature.home

import com.wakeroo.app.ui.util.formatMeridiem
import com.wakeroo.app.ui.i18n.tr
import com.wakeroo.app.ui.theme.MorningGoldFixed
import com.wakeroo.app.ui.theme.DawnLavenderFixed
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakeroo.app.alarm.health.Health
import com.wakeroo.app.alarm.health.HealthItem
import com.wakeroo.app.ui.components.press3d
import com.wakeroo.app.ui.theme.Alert
import com.wakeroo.app.ui.theme.Calm
import com.wakeroo.app.ui.theme.ClockType
import com.wakeroo.app.ui.theme.DawnLavender
import com.wakeroo.app.ui.theme.Glass
import com.wakeroo.app.ui.theme.Hairline
import com.wakeroo.app.ui.theme.Inter
import com.wakeroo.app.ui.theme.LocalReducedMotion
import com.wakeroo.app.ui.theme.MorningGold
import com.wakeroo.app.ui.theme.Night
import com.wakeroo.app.ui.theme.Paper
import com.wakeroo.app.ui.theme.PaperMuted
import com.wakeroo.app.ui.theme.Space
import com.wakeroo.app.ui.theme.SunriseCoral
import com.wakeroo.app.ui.theme.WakerooMotion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Position on the tilted 24-hour orbit for a time of day: midnight at the front, noon at the back. */
private fun orbitPoint(minutes: Float, c: Offset, rx: Float, ry: Float, spin: Float): Offset {
    val a = (minutes / (24 * 60f)) * 2 * PI.toFloat() + spin
    return Offset(c.x - rx * sin(a), c.y + ry * cos(a))
}

/**
 * The 3D orbit clock: a 24-hour ring seen at an angle. The sun sits at the time now, the gold
 * point at your next alarm, and the stretch of night between them glows. It leans with the
 * phone, and the near half of the ring is brighter than the far half.
 */
@Composable
fun OrbitClock(nowMinutes: Float, alarmMinutes: Float?, tilt: Offset, modifier: Modifier = Modifier) {
    val reduced = LocalReducedMotion.current
    val pulse by rememberInfiniteTransition(label = "orbit").animateFloat(
        0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart), label = "p",
    )
    val text = rememberTextMeasurer()
    val ink = Paper
    val muted = PaperMuted
    Canvas(modifier) {
        val c = Offset(size.width / 2, size.height * 0.52f)
        val rx = size.width * 0.36f
        val lean = 0.36f + tilt.y * 0.08f
        val ry = rx * lean
        val spin = if (reduced) 0f else tilt.x * 0.25f
        // A faint disc inside the orbit, lit from the near side, gives the ring a floor.
        drawOval(
            Brush.verticalGradient(listOf(Color.Transparent, DawnLavenderFixed.copy(alpha = 0.10f), SunriseCoral.copy(alpha = 0.10f)), c.y - ry, c.y + ry),
            Offset(c.x - rx, c.y - ry), androidx.compose.ui.geometry.Size(rx * 2, ry * 2),
        )
        // The ring, brighter where it's closer to you.
        val steps = 120
        for (i in 0 until steps) {
            val m0 = i * 24 * 60f / steps
            val p0 = orbitPoint(m0, c, rx, ry, spin)
            val p1 = orbitPoint(m0 + 24 * 60f / steps, c, rx, ry, spin)
            val near = ((p0.y - (c.y - ry)) / (2 * ry)).coerceIn(0f, 1f)
            drawLine(ink.copy(alpha = 0.08f + 0.22f * near), p0, p1, (1.5f + 2.5f * near).dp.toPx(), StrokeCap.Round)
        }
        // Hour ticks and four labels.
        for (h in 0 until 24) {
            val p = orbitPoint(h * 60f, c, rx * 1.06f, ry * 1.06f, spin)
            val near = ((p.y - (c.y - ry)) / (2 * ry)).coerceIn(0f, 1f)
            drawCircle(ink.copy(alpha = 0.15f + 0.35f * near), (if (h % 6 == 0) 2.5f else 1.2f).dp.toPx(), p)
        }
        listOf(0 to "12 ${formatMeridiem(0)}", 6 to "6 ${formatMeridiem(6)}", 12 to tr("NOON"), 18 to "6 ${formatMeridiem(18)}").forEach { (h, label) ->
            val p = orbitPoint(h * 60f, c, rx * 1.16f, ry * 1.45f, spin)
            val layout = text.measure(label, TextStyle(fontFamily = Inter, fontSize = 9.sp, color = muted))
            drawText(layout, topLeft = Offset(p.x - layout.size.width / 2f, p.y - layout.size.height / 2f))
        }
        // The arc of sleep from now to the alarm.
        if (alarmMinutes != null) {
            var span = alarmMinutes - nowMinutes
            if (span <= 0) span += 24 * 60
            val path = Path()
            val segs = 80
            for (i in 0..segs) {
                val p = orbitPoint(nowMinutes + span * i / segs, c, rx, ry, spin)
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            drawPath(path, Brush.sweepGradient(listOf(DawnLavenderFixed, SunriseCoral, MorningGoldFixed, DawnLavenderFixed), c), style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
            drawPath(path, MorningGoldFixed.copy(alpha = 0.18f), style = Stroke(14.dp.toPx(), cap = StrokeCap.Round))
            // A comet travelling along the arc.
            if (!reduced) {
                val q = orbitPoint(nowMinutes + span * pulse, c, rx, ry, spin)
                drawCircle(Color.White.copy(alpha = 0.8f * (1 - pulse)), 3.dp.toPx(), q)
            }
            val a = orbitPoint(alarmMinutes, c, rx, ry, spin)
            drawCircle(MorningGoldFixed.copy(alpha = 0.3f), 13.dp.toPx(), a)
            drawCircle(MorningGoldFixed, 7.dp.toPx(), a)
        }
        // The sun, at the time now.
        val s = orbitPoint(nowMinutes, c, rx, ry, spin)
        drawCircle(Brush.radialGradient(listOf(SunriseCoral.copy(alpha = 0.55f), Color.Transparent), s, 26.dp.toPx()), 26.dp.toPx(), s)
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFD39A), SunriseCoral), s, 9.dp.toPx()), 9.dp.toPx(), s)
    }
}

/**
 * The hero: the orbit clock with "in 7 h 20 min" on the front; tap to flip it over in 3D for
 * the alarm's details and health. With no alarm it invites you to set one.
 */
@Composable
fun OrbitHero(
    nowMinutes: Float,
    alarmMinutes: Float?,
    untilText: String?,
    timeText: String,
    meridiem: String,
    dayText: String,
    label: String,
    health: Health,
    tilt: Offset,
    empty: Boolean,
    onCreate: () -> Unit,
    onHealth: () -> Unit,
    back: @Composable () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var flipped by remember { mutableStateOf(false) }
    val turn by animateFloatAsState(if (flipped) 180f else 0f, WakerooMotion.bouncy(), label = "heroFlip")
    Box(
        Modifier
            .fillMaxWidth()
            .height(300.dp)
            .graphicsLayer {
                rotationY = turn + tilt.x * 6f
                rotationX = -tilt.y * 6f
                cameraDistance = 18f * density
            }
            .clip(RoundedCornerShape(32.dp))
            .background(Glass)
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.03f))))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(32.dp))
            .clickable(remember { MutableInteractionSource() }, null, enabled = !empty) {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                flipped = !flipped
            },
    ) {
        if (turn <= 90f) {
            OrbitClock(nowMinutes, alarmMinutes, tilt, Modifier.fillMaxSize().padding(horizontal = Space.md))
            Column(Modifier.fillMaxSize().padding(Space.xl)) {
                Text(if (empty) tr("NO ALARM SET") else tr("NEXT ALARM"), style = MaterialTheme.typography.labelSmall, color = DawnLavender)
                if (empty) {
                    Spacer(Modifier.weight(1f))
                    Text(tr("Your mornings start here"), style = MaterialTheme.typography.headlineSmall, color = Paper)
                    Spacer(Modifier.height(Space.md))
                    Box(
                        Modifier.fillMaxWidth().height(52.dp).press3d().clip(CircleShape).background(SunriseCoral).clickable(onClick = onCreate),
                        contentAlignment = Alignment.Center,
                    ) { Text(tr("Set an alarm"), style = MaterialTheme.typography.labelLarge, color = Night) }
                } else {
                    Text("in ${untilText.orEmpty()}", style = MaterialTheme.typography.headlineMedium, color = Paper)
                    Spacer(Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(timeText, style = ClockType.card, color = Paper)
                        Text(" $meridiem · $dayText", style = MaterialTheme.typography.titleSmall, color = PaperMuted, modifier = Modifier.padding(bottom = 6.dp))
                        Spacer(Modifier.weight(1f))
                        HealthDot(health, onHealth)
                    }
                    if (label.isNotBlank()) Text(label, style = MaterialTheme.typography.bodyMedium, color = Paper.copy(alpha = 0.85f), maxLines = 1)
                }
            }
        } else {
            // The back of the card, mirrored so it reads the right way round.
            Box(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }.padding(Space.xl)) { back() }
        }
    }
}

@Composable
private fun HealthDot(health: Health, onClick: () -> Unit) {
    val color = when (health) { Health.Good -> Calm; Health.Warn -> MorningGold; Health.Bad -> Alert }
    Row(
        Modifier.clip(CircleShape).background(Night.copy(alpha = 0.3f)).clickable(onClick = onClick).padding(horizontal = Space.md, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(if (health == Health.Good) tr("Ready") else tr("Check"), style = MaterialTheme.typography.labelSmall, color = Paper)
    }
}

/**
 * Shown when something could stop an alarm from ringing. Red can't be dismissed; amber can be
 * put away for a week. "Fix" goes straight to the setting that solves it.
 */
@Composable
fun FixBanner(item: HealthItem, onFix: () -> Unit, onHide: (() -> Unit)?, modifier: Modifier = Modifier) {
    val red = item.status == Health.Bad
    val color = if (red) Alert else MorningGold
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(color.copy(alpha = 0.16f))
            .border(1.5.dp, color.copy(alpha = 0.6f), RoundedCornerShape(22.dp))
            .padding(start = Space.lg, end = Space.sm, top = Space.md, bottom = Space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (red) Icons.Rounded.ErrorOutline else Icons.Rounded.WarningAmber, null, tint = color)
        Spacer(Modifier.width(Space.md))
        Column(Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.titleSmall, color = Paper)
            Text(item.detail, style = MaterialTheme.typography.bodySmall, color = PaperMuted, maxLines = 2)
        }
        Text(
            tr("Fix"), style = MaterialTheme.typography.labelLarge, color = Night,
            modifier = Modifier.padding(start = Space.sm).press3d().clip(CircleShape).background(color).clickable(role = Role.Button, onClick = onFix).padding(horizontal = Space.lg, vertical = Space.sm),
        )
        if (onHide != null) {
            Icon(Icons.Rounded.Close, tr("Hide for a week"), tint = PaperMuted, modifier = Modifier.padding(start = 4.dp).clip(CircleShape).clickable(onClick = onHide).padding(6.dp))
        }
    }
}

/** From 6 PM: when to go to bed, with a moon that climbs as bedtime gets closer. */
@Composable
fun BedtimeCard(bedText: String, sleepText: String, closeness: Float, onSleepSounds: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1B1F3D), Color(0xFF2B2550))))
            .border(1.dp, Hairline, RoundedCornerShape(26.dp))
            .padding(Space.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(56.dp)) {
            val y = size.height * (0.85f - 0.55f * closeness)
            val c = Offset(size.width / 2, y)
            val r = size.minDimension * 0.22f
            drawCircle(Brush.radialGradient(listOf(DawnLavenderFixed.copy(alpha = 0.4f), Color.Transparent), c, r * 2.4f), r * 2.4f, c)
            drawCircle(Color(0xFFF2EEE3), r, c)
            drawCircle(Color(0xFF1B1F3D), r * 0.9f, c + Offset(r * 0.45f, -r * 0.25f))
            drawLine(Color.White.copy(alpha = 0.15f), Offset(0f, size.height * 0.9f), Offset(size.width, size.height * 0.9f), 1.dp.toPx())
        }
        Spacer(Modifier.width(Space.md))
        Column(Modifier.weight(1f)) {
            Text(tr("Bed by {0}", bedText), style = MaterialTheme.typography.titleMedium, color = Color(0xFFF5F3EE))
            Text(sleepText, style = MaterialTheme.typography.bodySmall, color = Color(0xFFA9ABB8))
        }
        Row(
            Modifier.press3d().clip(CircleShape).background(DawnLavender.copy(alpha = 0.2f)).clickable(onClick = onSleepSounds).padding(horizontal = Space.md, vertical = Space.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.GraphicEq, null, tint = DawnLavender, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(tr("Sounds"), style = MaterialTheme.typography.labelMedium, color = DawnLavender)
        }
    }
}

/** Seven little suns for the last week: gold for won mornings, coral for missed ones. */
@Composable
fun StreakStrip(week: List<Pair<String, Boolean?>>, streak: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Glass)
            .border(1.dp, Hairline, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = Space.lg, vertical = Space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.width(76.dp)) {
            Text("$streak", style = MaterialTheme.typography.headlineSmall, color = Paper)
            Text(tr("day streak"), style = MaterialTheme.typography.labelSmall, color = PaperMuted)
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
            week.forEach { (day, won) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Canvas(Modifier.size(22.dp)) {
                        val r = size.minDimension / 2
                        when (won) {
                            true -> {
                                drawCircle(MorningGoldFixed, r * 0.55f)
                                for (k in 0 until 8) {
                                    val a = k * 0.785f
                                    drawLine(MorningGoldFixed, center + Offset(cos(a), sin(a)) * (r * 0.7f), center + Offset(cos(a), sin(a)) * r, 1.6f.dp.toPx(), StrokeCap.Round)
                                }
                            }
                            false -> drawCircle(SunriseCoral.copy(alpha = 0.8f), r * 0.5f)
                            null -> drawCircle(Color.White.copy(alpha = 0.12f), r * 0.4f)
                        }
                    }
                    Text(day, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = PaperMuted)
                }
            }
        }
    }
}
