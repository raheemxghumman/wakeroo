package com.wakeroo.app.missions

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import com.wakeroo.app.data.model.MissionConfig
import com.wakeroo.app.ui.theme.Alert
import com.wakeroo.app.ui.theme.DawnLavender
import com.wakeroo.app.ui.theme.MorningGold
import com.wakeroo.app.ui.theme.NightSurface
import com.wakeroo.app.ui.theme.Paper
import com.wakeroo.app.ui.theme.PaperMuted
import com.wakeroo.app.ui.theme.Space
import kotlinx.coroutines.launch

private val PROMPTS get() = listOf(
    "What's the first thing you'll do today?",
    "What would make today a good day?",
    "What are you getting up for?",
)

/**
 * "Type your plan": a few real words about the day ahead. Writing an intention wakes the
 * thinking brain, and the plan then shows on the Morning tab.
 */
@Composable
fun PlanMission(config: MissionConfig, onActivity: () -> Unit, onProgress: (Float) -> Unit, onDone: () -> Unit, onPlan: (String) -> Unit = {}) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val prompt = remember { PROMPTS.random() }
    var typed by remember { mutableStateOf("") }
    var problem by remember { mutableStateOf<PlanValidator.Problem?>(null) }
    val nudge = remember { Animatable(0f) }

    val words = TypingPhrases.normalize(typed).split(' ').count { it.isNotEmpty() }
    val letters = typed.count { it.isLetter() }
    val progress = ((words.coerceAtMost(PlanValidator.MIN_WORDS) / PlanValidator.MIN_WORDS.toFloat()) * 0.5f +
        (letters.coerceAtMost(PlanValidator.MIN_LETTERS) / PlanValidator.MIN_LETTERS.toFloat()) * 0.5f)

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(prompt, style = MaterialTheme.typography.headlineSmall, color = Paper)
        Text("At least 3 real words", style = MaterialTheme.typography.bodyMedium, color = DawnLavender, modifier = Modifier.padding(top = Space.xs))
        Spacer(Modifier.weight(1f))
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .graphicsLayer { translationX = nudge.value * 0.3f }
                .clip(RoundedCornerShape(24.dp))
                .background(NightSurface)
                .border(1.5.dp, (if (problem != null) Alert else MorningGold).copy(alpha = 0.25f + 0.5f * progress), RoundedCornerShape(24.dp))
                .padding(Space.xl),
        ) {
            Text(
                typed.ifEmpty { "Make coffee and go for a walk" },
                style = MaterialTheme.typography.headlineSmall,
                color = if (typed.isEmpty()) PaperMuted.copy(alpha = 0.5f) else Paper,
            )
        }
        Spacer(Modifier.height(Space.sm))
        AnimatedContent(problem, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "planHint") { p ->
            Text(
                when (p) {
                    PlanValidator.Problem.TooShort -> "A little more: three words or so"
                    PlanValidator.Problem.NotWords -> "That doesn't look like words. Try a real plan"
                    null -> if (progress >= 1f) "Looks good. Tap ✓ to start your day" else "$words of 3 words"
                },
                style = MaterialTheme.typography.bodyMedium, color = if (p != null) Alert else PaperMuted,
            )
        }
        Spacer(Modifier.weight(1f))
        LetterPad(onKey = onKey@{ key ->
            onActivity()
            problem = null
            when (key) {
                is Key.Char -> if (typed.length < 80) typed += key.c
                Key.Space -> if (typed.isNotEmpty() && !typed.endsWith(" ")) typed += " "
                Key.Delete -> typed = typed.dropLast(1)
                Key.Enter -> {
                    val issue = PlanValidator.check(typed)
                    if (issue == null) {
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        onProgress(1f)
                        onPlan(TypingPhrases.normalize(typed).replaceFirstChar { it.uppercase() })
                        onDone()
                        return@onKey
                    } else {
                        problem = issue
                        haptics.performHapticFeedback(HapticFeedbackType.Reject)
                        scope.launch { nudge.snapTo(0f); nudge.animateTo(0f, spring(0.25f, 900f), initialVelocity = 2400f) }
                    }
                }
            }
            onProgress(progress.coerceAtMost(0.95f))
        })
    }
}
