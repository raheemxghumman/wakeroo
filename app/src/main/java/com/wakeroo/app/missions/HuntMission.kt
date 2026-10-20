package com.wakeroo.app.missions

import com.wakeroo.app.ui.i18n.tr
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import com.wakeroo.app.BuildConfig
import com.wakeroo.app.camera.LabelReader
import com.wakeroo.app.data.model.MissionConfig
import com.wakeroo.app.ui.theme.Calm
import com.wakeroo.app.ui.theme.DawnLavender
import com.wakeroo.app.ui.theme.MorningGold
import com.wakeroo.app.ui.theme.NightSurface
import com.wakeroo.app.ui.theme.Paper
import com.wakeroo.app.ui.theme.PaperMuted
import com.wakeroo.app.ui.theme.Space
import com.wakeroo.app.ui.theme.SunriseCoral
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** "Find a cup": on-device AI checks the camera for a random everyday object. */
@Composable
fun HuntMission(config: MissionConfig, onActivity: () -> Unit, onProgress: (Float) -> Unit, onDone: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val reader = remember { LabelReader() }
    DisposableEffect(Unit) { onDispose { reader.close() } }
    val count = config.count.coerceIn(1, 3)
    var targets by remember { mutableStateOf(HuntObjects.pick(count, config.difficulty)) }
    var index by remember { mutableIntStateOf(0) }
    var skipped by remember { mutableStateOf(false) }
    var matched by remember { mutableStateOf(false) }
    var torch by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var lastCheck by remember { mutableLongStateOf(0L) }
    var warmth by remember { mutableFloatStateOf(0f) }
    val streak = remember { MatchStreak(3) }
    val done by rememberUpdatedState(onDone)
    val target = targets[index]

    val float by rememberInfiniteTransition(label = "badge").animateFloat(
        -1f, 1f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "f",
    )
    val shownWarmth by animateFloatAsState(warmth, label = "warmth")

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(NightSurface).padding(Space.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedContent(target, transitionSpec = { scaleIn() + fadeIn() togetherWith fadeOut() }, label = "target") { t ->
                Box(
                    Modifier
                        .size(64.dp)
                        .graphicsLayer { translationY = float * 4.dp.toPx(); rotationZ = float * 4f }
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(MorningGold, SunriseCoral))),
                    contentAlignment = Alignment.Center,
                ) { Text(t.emoji, fontSize = 32.sp) }
            }
            Spacer(Modifier.width(Space.lg))
            Column(Modifier.weight(1f)) {
                Text(if (count > 1) tr("Object {0} of {1}", index + 1, count) else tr("Find this"), style = MaterialTheme.typography.labelMedium, color = DawnLavender)
                Text(tr("Show me {0}", target.name), style = MaterialTheme.typography.titleLarge, color = Paper)
            }
        }
        Spacer(Modifier.height(Space.lg))
        Viewfinder(matched, torch, onTorch = { torch = !torch }) { frame ->
            val now = System.currentTimeMillis()
            if (matched || busy || now - lastCheck < 300) return@Viewfinder frame.close()
            busy = true; lastCheck = now
            scope.launch {
                val labels = reader.read(frame)
                busy = false
                if (BuildConfig.DEBUG) Log.d("Hunt", "want ${target.id}: " + labels.take(5).joinToString { "${it.first} %.2f".format(it.second) })
                val score = HuntObjects.score(target, labels)
                warmth = (score / target.minConfidence).coerceIn(0f, 1f)
                // Only a partial match counts as "still trying"; a camera aimed at the wall doesn't.
                if (score > 0.3f) onActivity()
                if (streak.offer(HuntObjects.matches(target, labels, config.difficulty))) {
                    matched = true
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onProgress((index + 1) / count.toFloat())
                    delay(700)
                    if (index + 1 >= count) done() else {
                        index++; matched = false; warmth = 0f; streak.reset()
                    }
                }
            }
        }
        Spacer(Modifier.height(Space.md))
        Box(Modifier.fillMaxWidth(0.7f).height(10.dp).clip(CircleShape).background(Paper.copy(alpha = 0.15f))) {
            Box(Modifier.fillMaxWidth(shownWarmth).height(10.dp).clip(CircleShape).background(if (matched) Calm else MorningGold))
        }
        Text(
            when {
                matched -> tr("Got it!")
                shownWarmth > 0.75f -> tr("That's it, hold steady")
                shownWarmth > 0.4f -> tr("Getting warmer")
                else -> tr("Walk around and point the camera at it")
            },
            style = MaterialTheme.typography.bodyMedium, color = PaperMuted, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Space.sm),
        )
        if (!skipped && !matched) {
            Text(
                tr("Don't have one? Try another object"),
                style = MaterialTheme.typography.labelLarge, color = DawnLavender,
                modifier = Modifier.padding(top = Space.sm).clip(CircleShape).clickable {
                    skipped = true
                    val replacement = HuntObjects.pick(1, config.difficulty, exclude = targets.map { it.id }.toSet()).first()
                    targets = targets.toMutableList().also { it[index] = replacement }
                    streak.reset(); warmth = 0f
                }.padding(Space.md),
            )
        }
    }
}
