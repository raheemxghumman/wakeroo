package com.wakeroo.app.feature.morning

import com.wakeroo.app.ui.i18n.tr
import com.wakeroo.app.ui.theme.MorningGoldFixed
import com.wakeroo.app.ui.theme.DawnLavenderFixed
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.wakeroo.app.data.Mood
import com.wakeroo.app.data.db.RoutineItemEntity
import com.wakeroo.app.ui.components.press3d
import com.wakeroo.app.ui.theme.Calm
import com.wakeroo.app.ui.theme.DawnLavender
import com.wakeroo.app.ui.theme.Hairline
import com.wakeroo.app.ui.theme.MorningGold
import com.wakeroo.app.ui.theme.Night
import com.wakeroo.app.ui.theme.Paper
import com.wakeroo.app.ui.theme.PaperMuted
import com.wakeroo.app.ui.theme.Space
import com.wakeroo.app.ui.theme.SunriseCoral
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

val Mood.color: Color
    get() = when (this) {
        Mood.Exhausted -> Color(0xFF8C8FB0)
        Mood.Tired -> DawnLavenderFixed
        Mood.Okay -> MorningGoldFixed
        Mood.Calm -> Calm
        Mood.Refreshed -> SunriseCoral
    }

/** A drawn face whose mouth and eyes follow the mood, from sleepy to bright. */
@Composable
fun MoodFace(mood: Mood, modifier: Modifier = Modifier, ink: Color = Night) {
    Canvas(modifier.semantics { contentDescription = mood.title }) {
        val r = size.minDimension / 2
        val c = center
        drawCircle(mood.color, r, c)
        drawCircle(Color.White.copy(alpha = 0.25f), r * 0.45f, c + Offset(-r * 0.32f, -r * 0.38f))
        val stroke = Stroke(r * 0.12f, cap = StrokeCap.Round)
        val eyeY = c.y - r * 0.12f
        for (side in listOf(-1, 1)) {
            val ex = c.x + side * r * 0.36f
            when (mood) {
                Mood.Exhausted -> drawLine(ink, Offset(ex - r * 0.14f, eyeY), Offset(ex + r * 0.14f, eyeY), r * 0.1f, StrokeCap.Round)
                Mood.Tired -> drawArc(ink, 0f, 180f, false, Offset(ex - r * 0.14f, eyeY - r * 0.1f), Size(r * 0.28f, r * 0.2f), style = stroke)
                Mood.Calm -> drawArc(ink, 180f, 180f, false, Offset(ex - r * 0.14f, eyeY - r * 0.06f), Size(r * 0.28f, r * 0.22f), style = stroke)
                else -> drawCircle(ink, r * 0.09f, Offset(ex, eyeY))
            }
        }
        // Mouth: a curve from a sag (exhausted) to a wide smile (refreshed).
        val curve = (mood.ordinal - 1.5f) / 2.5f
        val mouth = Path().apply {
            moveTo(c.x - r * 0.34f, c.y + r * 0.32f)
            quadraticTo(c.x, c.y + r * (0.32f + curve * 0.4f), c.x + r * 0.34f, c.y + r * 0.32f)
        }
        drawPath(mouth, ink, style = stroke)
        if (mood == Mood.Exhausted) {
            // A little "z" drifting off.
            drawLine(ink.copy(alpha = 0.6f), Offset(c.x + r * 0.62f, c.y - r * 0.78f), Offset(c.x + r * 0.84f, c.y - r * 0.78f), r * 0.07f)
            drawLine(ink.copy(alpha = 0.6f), Offset(c.x + r * 0.84f, c.y - r * 0.78f), Offset(c.x + r * 0.62f, c.y - r * 0.56f), r * 0.07f)
            drawLine(ink.copy(alpha = 0.6f), Offset(c.x + r * 0.62f, c.y - r * 0.56f), Offset(c.x + r * 0.84f, c.y - r * 0.56f), r * 0.07f)
        }
    }
}

/** Five springy faces; tap the one that fits. One per day, change it any time. */
@Composable
fun MoodPicker(selected: Mood?, onPick: (Mood) -> Unit, modifier: Modifier = Modifier, size: Int = 52, showLabels: Boolean = true) {
    val haptics = LocalHapticFeedback.current
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Mood.entries.forEach { m ->
            val chosen = m == selected
            val scale by animateFloatAsState(if (chosen) 1.18f else if (selected == null) 1f else 0.88f, spring(dampingRatio = 0.45f, stiffness = 400f), label = "mood")
            val hop = remember { Animatable(0f) }
            LaunchedEffect(chosen) { if (chosen) { hop.snapTo(0f); hop.animateTo(0f, spring(0.35f, 500f), initialVelocity = -40f) } }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MoodFace(
                    m,
                    Modifier
                        .size(size.dp)
                        .graphicsLayer {
                            scaleX = scale; scaleY = scale
                            translationY = hop.value * 2.dp.toPx()
                            alpha = if (selected == null || chosen) 1f else 0.5f
                        }
                        .press3d(maxTilt = 16f)
                        .clip(CircleShape)
                        .clickable(role = Role.RadioButton) {
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            onPick(m)
                        },
                )
                if (showLabels) {
                    Text(m.title, style = MaterialTheme.typography.labelSmall, color = if (chosen) m.color else PaperMuted, modifier = Modifier.padding(top = Space.sm))
                }
            }
        }
    }
}

/** Tick-off habits for the first minutes of the day, with a little burst when all are done. */
@Composable
fun RoutineList(
    items: List<RoutineItemEntity>,
    done: Set<Long>,
    onToggle: (RoutineItemEntity, Boolean) -> Unit,
    onAdd: (String) -> Unit,
    onDelete: (RoutineItemEntity) -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    var newItem by remember { mutableStateOf("") }
    val allDone = items.isNotEmpty() && items.all { it.id in done }
    val burst = remember { Animatable(0f) }
    LaunchedEffect(allDone) { if (allDone) { burst.snapTo(0f); burst.animateTo(1f, tween(900)) } }

    Box {
        Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            items.forEach { item -> RoutineRow(item, item.id in done, editing, { onToggle(item, it) }, { onDelete(item) }) }
            AnimatedVisibility(editing && items.size < 5, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).border(1.dp, Hairline, RoundedCornerShape(18.dp)).padding(Space.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Add, null, tint = PaperMuted)
                    Spacer(Modifier.width(Space.sm))
                    BasicTextField(
                        value = newItem, onValueChange = { newItem = it.take(40) }, singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Paper), cursorBrush = SolidColor(SunriseCoral),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if (newItem.isNotBlank()) { onAdd(newItem); newItem = "" } }),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner -> if (newItem.isEmpty()) Text(tr("Add a habit, like 'Make the bed'"), style = MaterialTheme.typography.bodyLarge, color = PaperMuted); inner() },
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when {
                        allDone -> tr("Routine complete. Great start!")
                        items.isEmpty() -> tr("Add up to five small habits.")
                        else -> tr("{0} of {1} done", items.count { it.id in done }, items.size)
                    },
                    style = MaterialTheme.typography.labelMedium, color = if (allDone) Calm else PaperMuted,
                )
                Text(
                    if (editing) tr("Done") else tr("Edit"), style = MaterialTheme.typography.labelLarge, color = DawnLavender,
                    modifier = Modifier.clip(CircleShape).clickable { editing = !editing }.padding(Space.sm),
                )
            }
        }
        if (burst.value in 0.01f..0.99f) {
            Canvas(Modifier.matchParentSize()) {
                val p = burst.value
                for (k in 0 until 18) {
                    val a = k * 0.349f
                    val d = size.minDimension * 0.45f * p
                    val o = Offset(size.width / 2 + cos(a) * d * 1.4f, size.height / 2 + sin(a) * d)
                    drawCircle(listOf(MorningGoldFixed, SunriseCoral, Calm, DawnLavenderFixed)[k % 4].copy(alpha = 1 - p), (5 - 3 * p).dp.toPx(), o)
                }
            }
        }
    }
}

@Composable
private fun RoutineRow(item: RoutineItemEntity, checked: Boolean, editing: Boolean, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val pop = remember { Animatable(1f) }
    val fill by animateFloatAsState(if (checked) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 350f), label = "check")
    val bg by animateColorAsState(if (checked) Calm.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.04f), label = "rowBg")
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .clickable(enabled = !editing) {
                haptics.performHapticFeedback(if (checked) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn)
                scope.launch { pop.snapTo(0.8f); pop.animateTo(1f, spring(0.4f, 600f)) }
                onToggle(!checked)
            }
            .padding(Space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(28.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }, contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension / 2
                drawCircle(Calm.copy(alpha = 0.2f + 0.8f * fill), r * (0.7f + 0.3f * fill), center)
                drawCircle(Color.White.copy(alpha = 0.3f * (1 - fill)), r, center, style = Stroke(2.dp.toPx()))
                if (fill > 0.05f) {
                    // The tick draws itself in as the circle fills.
                    val path = Path().apply {
                        moveTo(center.x - r * 0.42f, center.y + r * 0.02f)
                        lineTo(center.x - r * 0.1f, center.y + r * 0.34f)
                        lineTo(center.x + r * 0.46f, center.y - r * 0.3f)
                    }
                    drawPath(path, Night.copy(alpha = fill), style = Stroke(r * 0.22f, cap = StrokeCap.Round))
                }
            }
        }
        Spacer(Modifier.width(Space.md))
        Text(
            item.title, style = MaterialTheme.typography.bodyLarge,
            color = if (checked) PaperMuted else Paper,
            textDecoration = if (checked) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f),
        )
        if (editing) Icon(Icons.Rounded.Close, tr("Remove"), tint = PaperMuted, modifier = Modifier.clip(CircleShape).clickable(onClick = onDelete).padding(4.dp))
    }
}
