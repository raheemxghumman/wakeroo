package com.wakeroo.app.missions

import com.wakeroo.app.ui.i18n.tr
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakeroo.app.data.model.MissionConfig
import com.wakeroo.app.ui.theme.Alert
import com.wakeroo.app.ui.theme.ClockType
import com.wakeroo.app.ui.theme.DawnLavender
import com.wakeroo.app.ui.theme.MorningGold
import com.wakeroo.app.ui.theme.NightSurface
import com.wakeroo.app.ui.theme.Paper
import com.wakeroo.app.ui.theme.PaperMuted
import com.wakeroo.app.ui.theme.Space
import kotlinx.coroutines.launch

/** Shakes the content sideways: the universal "nope". */
@Composable
private fun rememberNope(): Pair<Animatable<Float, *>, suspend () -> Unit> {
    val offset = remember { Animatable(0f) }
    return offset to {
        offset.snapTo(0f)
        offset.animateTo(0f, spring(dampingRatio = 0.25f, stiffness = 900f), initialVelocity = 2400f)
    }
}

@Composable
fun MathMission(config: MissionConfig, onActivity: () -> Unit, onProgress: (Float) -> Unit, onDone: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var solved by remember { mutableIntStateOf(0) }
    var problem by remember { mutableStateOf(MathProblems.generate(config.difficulty)) }
    var input by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    val (shake, nope) = rememberNope()

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(tr("Problem {0} of {1}", solved + 1, config.count), style = MaterialTheme.typography.labelLarge, color = DawnLavender)
        Spacer(Modifier.weight(1f))
        AnimatedContent(
            targetState = problem,
            transitionSpec = {
                (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
            },
            label = "problem",
        ) { p ->
            Text(p.text, style = ClockType.hero.copy(fontSize = 52.sp), color = Paper, maxLines = 1)
        }
        Spacer(Modifier.height(Space.lg))
        Box(
            Modifier
                .fillMaxWidth(0.7f)
                .height(72.dp)
                .graphicsLayer { translationX = shake.value * 0.3f }
                .clip(RoundedCornerShape(24.dp))
                .background(NightSurface),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                input.ifEmpty { "?" },
                style = ClockType.card,
                color = if (wrong) Alert else if (input.isEmpty()) PaperMuted else MorningGold,
            )
        }
        Spacer(Modifier.weight(1f))
        NumberPad(onKey = { key ->
            onActivity()
            wrong = false
            when (key) {
                is Key.Char -> if (input.length < 5) input += key.c
                Key.Delete -> input = input.dropLast(1)
                Key.Enter -> {
                    if (input.toIntOrNull() == problem.answer) {
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        solved++
                        onProgress(solved / config.count.toFloat())
                        input = ""
                        if (solved >= config.count) onDone() else problem = MathProblems.generate(config.difficulty)
                    } else if (input.isNotEmpty()) {
                        haptics.performHapticFeedback(HapticFeedbackType.Reject)
                        wrong = true
                        scope.launch { nope() }
                        input = ""
                    }
                }
                Key.Space -> Unit
            }
        })
    }
}

@Composable
fun TypingMission(config: MissionConfig, onActivity: () -> Unit, onProgress: (Float) -> Unit, onDone: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val phrases = remember { TypingPhrases.pick(config.count) }
    var index by remember { mutableIntStateOf(0) }
    var typed by remember { mutableStateOf("") }
    val (shake, nope) = rememberNope()
    val target = phrases[index]

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Top) {
        Text(tr("Phrase {0} of {1}", index + 1, phrases.size), style = MaterialTheme.typography.labelLarge, color = DawnLavender)
        Spacer(Modifier.weight(1f))
        AnimatedContent(
            targetState = target,
            transitionSpec = { (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut()) },
            label = "phrase",
        ) { phrase ->
            // Letters turn gold as they are typed correctly; a wrong letter shows in red.
            val norm = TypingPhrases.normalize(typed)
            val text = buildAnnotatedString {
                phrase.forEachIndexed { i, c ->
                    val color = when {
                        i >= norm.length -> Paper.copy(alpha = 0.55f)
                        norm[i] == c -> MorningGold
                        else -> Alert
                    }
                    withStyle(SpanStyle(color = color)) { append(c) }
                }
            }
            Text(
                text,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { translationX = shake.value * 0.3f }
                    .clip(RoundedCornerShape(24.dp))
                    .background(NightSurface)
                    .padding(Space.xl),
            )
        }
        Spacer(Modifier.weight(1f))
        LetterPad(onKey = { key ->
            onActivity()
            when (key) {
                is Key.Char -> if (typed.length < target.length + 4) typed += key.c
                Key.Space -> typed += " "
                Key.Delete -> typed = typed.dropLast(1)
                Key.Enter -> Unit
            }
            if (TypingPhrases.matches(target, typed)) {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                typed = ""
                onProgress((index + 1) / phrases.size.toFloat())
                if (index + 1 >= phrases.size) onDone() else index++
            } else if (key == Key.Enter) {
                haptics.performHapticFeedback(HapticFeedbackType.Reject)
                scope.launch { nope() }
            }
        })
    }
}
