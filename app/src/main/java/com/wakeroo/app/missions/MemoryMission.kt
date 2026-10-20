package com.wakeroo.app.missions

import com.wakeroo.app.ui.i18n.tr
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.wakeroo.app.data.model.MissionConfig
import com.wakeroo.app.ui.theme.Alert
import com.wakeroo.app.ui.theme.DawnLavender
import com.wakeroo.app.ui.theme.MorningGold
import com.wakeroo.app.ui.theme.NightSurface
import com.wakeroo.app.ui.theme.NightSurfaceHigh
import com.wakeroo.app.ui.theme.Paper
import com.wakeroo.app.ui.theme.PaperMuted
import com.wakeroo.app.ui.theme.Space
import com.wakeroo.app.ui.theme.SunriseCoral
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Phase { Watch, Repeat, Won, Oops }

private enum class Face { Blank, Lit, Wrong }

/** Tiles flip over in 3D to show a pattern; flip the same ones back from memory. */
@Composable
fun MemoryMission(config: MissionConfig, onActivity: () -> Unit, onProgress: (Float) -> Unit, onDone: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val grid = if (config.gridSize >= 4) 4 else 3
    val rounds = config.count.coerceIn(2, 8)
    var round by remember { mutableIntStateOf(0) }
    var attempt by remember { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf(Phase.Watch) }
    var pattern by remember { mutableStateOf(MemoryPatterns.generate(grid, MemoryPatterns.lengthFor(0, grid))) }
    val found = remember { mutableStateListOf<Int>() }
    val faces = remember { mutableStateListOf<Face>().apply { repeat(grid * grid) { add(Face.Blank) } } }
    val flips = remember { List(grid * grid) { Animatable(0f) } }
    val shake = remember { Animatable(0f) }

    suspend fun flipAll(to: Float, indices: Collection<Int>, stagger: Long = 0) = coroutineScope {
        indices.forEachIndexed { i, idx ->
            launch {
                delay(stagger * i)
                flips[idx].animateTo(to, spring(dampingRatio = 0.62f, stiffness = 260f))
            }
        }
    }

    // Each attempt: show the pattern, hide it, then hand over to the player.
    LaunchedEffect(round, attempt) {
        phase = Phase.Watch
        found.clear()
        for (i in faces.indices) faces[i] = Face.Blank
        flipAll(0f, flips.indices.toList())
        delay(450)
        pattern.forEach { faces[it] = Face.Lit }
        flipAll(180f, pattern, stagger = 140)
        delay(850L + 160L * pattern.size)
        flipAll(0f, pattern)
        pattern.forEach { faces[it] = Face.Blank }
        phase = Phase.Repeat
    }

    fun tap(index: Int) {
        if (phase != Phase.Repeat || index in found) return
        onActivity()
        if (index in pattern) {
            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            found += index
            faces[index] = Face.Lit
            scope.launch { flips[index].animateTo(180f, spring(dampingRatio = 0.6f, stiffness = 320f)) }
            if (found.size == pattern.size) {
                phase = Phase.Won
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                onProgress((round + 1) / rounds.toFloat())
                scope.launch {
                    delay(700)
                    if (round + 1 >= rounds) onDone() else {
                        pattern = MemoryPatterns.generate(grid, MemoryPatterns.lengthFor(round + 1, grid))
                        round++
                    }
                }
            }
        } else {
            phase = Phase.Oops
            haptics.performHapticFeedback(HapticFeedbackType.Reject)
            faces[index] = Face.Wrong
            scope.launch { flips[index].animateTo(180f, tween(180)) }
            scope.launch {
                shake.snapTo(0f)
                shake.animateTo(0f, spring(dampingRatio = 0.25f, stiffness = 900f), initialVelocity = 2600f)
                delay(500)
                attempt++ // the same pattern plays again
            }
        }
    }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(tr("Round {0} of {1}", round + 1, rounds), style = MaterialTheme.typography.labelLarge, color = DawnLavender)
        Spacer(Modifier.height(Space.sm))
        RoundDots(rounds, round, phase == Phase.Won)
        Spacer(Modifier.weight(1f))
        Column(
            Modifier.fillMaxWidth().aspectRatio(1f).graphicsLayer { translationX = shake.value * 0.25f },
            verticalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            for (row in 0 until grid) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                    for (col in 0 until grid) {
                        val index = row * grid + col
                        Tile(
                            rotation = flips[index].value,
                            face = faces[index],
                            modifier = Modifier.weight(1f).fillMaxSize(),
                            onTap = { tap(index) },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(Space.xl))
        Text(
            when (phase) {
                Phase.Watch -> tr("Watch the tiles that light up")
                Phase.Repeat -> tr("Now tap the same {0} tiles · {1} found", pattern.size, found.size)
                Phase.Won -> if (round + 1 >= rounds) tr("Sharp! You're awake") else tr("Nice. One more tile next round")
                Phase.Oops -> tr("Not that one. Watch again")
            },
            style = MaterialTheme.typography.bodyLarge, color = if (phase == Phase.Oops) Alert else PaperMuted,
        )
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun RoundDots(total: Int, current: Int, won: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            val done = i < current || (i == current && won)
            Box(
                Modifier.size(if (i == current) 22.dp else 8.dp, 8.dp).clip(CircleShape)
                    .background(if (done) MorningGold else if (i == current) SunriseCoral else Paper.copy(alpha = 0.18f)),
            )
        }
    }
}

/** One tile: a dark back and a gold (or red) face, turned on the Y axis like a card. */
@Composable
private fun Tile(rotation: Float, face: Face, modifier: Modifier, onTap: () -> Unit) {
    val showingFront = rotation > 90f
    val back = Brush.linearGradient(listOf(NightSurfaceHigh, NightSurface))
    val front = when (face) {
        Face.Wrong -> Brush.linearGradient(listOf(Alert, Color(0xFFB8314A)))
        else -> Brush.linearGradient(listOf(MorningGold, SunriseCoral))
    }
    val edge = Paper.copy(alpha = 0.08f)
    Box(
        modifier
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 10f * density
            }
            .shadow(if (showingFront) 14.dp else 4.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black, spotColor = if (showingFront) SunriseCoral else Color.Black)
            .clip(RoundedCornerShape(20.dp))
            .background(if (showingFront) front else back)
            .border(1.dp, edge, RoundedCornerShape(20.dp))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onTap)
            .semantics { contentDescription = tr("Tile") },
        contentAlignment = Alignment.Center,
    ) {
        if (showingFront && face == Face.Lit) {
            // A small sun on the face, drawn mirrored back so it reads the right way round.
            Box(Modifier.graphicsLayer { rotationY = 180f }.size(14.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.85f)))
        }
    }
}
