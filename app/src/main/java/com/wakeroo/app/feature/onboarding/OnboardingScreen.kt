package com.wakeroo.app.feature.onboarding

import com.wakeroo.app.ui.i18n.tr
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.createSavedStateHandle
import com.wakeroo.app.container
import com.wakeroo.app.feature.editor.PillButton
import com.wakeroo.app.ui.components.RoundIconButton
import com.wakeroo.app.ui.components.SkyBackground
import com.wakeroo.app.ui.scenes.Scene
import com.wakeroo.app.ui.scenes.SceneBackground
import com.wakeroo.app.ui.theme.LocalReducedMotion
import com.wakeroo.app.ui.theme.MorningGold
import com.wakeroo.app.ui.theme.Night
import com.wakeroo.app.ui.theme.Paper
import com.wakeroo.app.ui.theme.PaperMuted
import com.wakeroo.app.ui.theme.Sky
import com.wakeroo.app.ui.theme.Space
import com.wakeroo.app.ui.theme.WakerooTheme
import com.wakeroo.app.ui.theme.SunriseCoral
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/**
 * The setup wizard: about forty seconds from install to a real, tested alarm. Time, scene,
 * sound, mission, permissions, then it builds the alarm and offers a one-minute test.
 * No paywall, no tracking prompt, no account. [restore] runs the short "welcome back" path.
 */
@Composable
fun OnboardingScreen(restore: Boolean, onFinished: (alarmId: Long) -> Unit) {
    // First run is always the immersive dark story, whatever the app theme.
    WakerooTheme(dark = true) { OnboardingContent(restore, onFinished) }
}

@Composable
private fun OnboardingContent(restore: Boolean, onFinished: (alarmId: Long) -> Unit) {
    val app = LocalContext.current.container
    val vm = viewModel(key = "onboarding-$restore") { OnboardingViewModel(app, createSavedStateHandle(), restore) }
    val state by vm.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val pager = rememberPagerState(initialPage = state.index) { state.steps.size }
    val reduced = LocalReducedMotion.current
    LaunchedEffect(state.index) {
        if (pager.currentPage != state.index) {
            if (reduced) pager.scrollToPage(state.index) else pager.animateScrollToPage(state.index, animationSpec = spring(dampingRatio = 0.86f, stiffness = 260f))
        }
    }
    val canGoBack = state.index > 0 && state.step != Step.Building && state.step != Step.Test
    BackHandler(enabled = canGoBack) { vm.back() }

    fun finish() = scope.launch {
        vm.finish()
        onFinished(state.savedId)
    }

    val context = LocalContext.current
    val use24h = DateFormat.is24HourFormat(context)

    // The background follows the story: night while setting the time, your scene while
    // choosing it, then the sky brightens toward morning as the alarm comes together.
    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = when (state.step) {
                Step.Scene -> Backdrop.Scene(Scene.byId(state.draft.scene))
                Step.Time, Step.Restored -> Backdrop.Sky(Sky.Night)
                Step.Sound, Step.Mission -> Backdrop.Sky(Sky.Dawn)
                Step.Permissions -> Backdrop.Sky(Sky.Sunrise)
                Step.Building, Step.Test -> Backdrop.Sky(Sky.Morning)
            },
            transitionSpec = { fadeIn(spring(stiffness = 120f)) togetherWith fadeOut(spring(stiffness = 120f)) },
            label = "backdrop",
        ) { backdrop ->
            when (backdrop) {
                is Backdrop.Sky -> SkyBackground(backdrop.sky)
                is Backdrop.Scene -> SceneBackground(backdrop.scene) {
                    Box(Modifier.fillMaxSize().background(Night.copy(alpha = 0.55f)))
                }
            }
        }
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            TopRow(state, canGoBack, onBack = vm::back)
            HorizontalPager(pager, userScrollEnabled = false, modifier = Modifier.weight(1f)) { page ->
                Box(Modifier.fillMaxSize().cube(pager, page).padding(horizontal = Space.xl)) {
                    when (state.steps[page]) {
                        Step.Restored -> RestoredStep(state.restoredAlarms, use24h, onNext = vm::next)
                        Step.Time -> TimeStep(state, use24h, vm::update, onNext = vm::next)
                        Step.Scene -> SceneStep(state.draft, vm::update, onNext = vm::next)
                        Step.Sound -> SoundStep(state.draft, vm::update, onNext = vm::next)
                        Step.Mission -> MissionStep(state.draft, vm::update, onNext = vm::next)
                        Step.Permissions -> PermissionsStep(onNext = vm::next)
                        Step.Building -> BuildingStep(state, use24h, onNext = vm::next)
                        Step.Test -> TestStep(
                            restore = restore,
                            onTest = { vm.scheduleTest(); finish() },
                            onLater = { finish() },
                        )
                    }
                }
            }
        }
    }
}

private sealed interface Backdrop {
    data class Sky(val sky: com.wakeroo.app.ui.theme.Sky) : Backdrop
    data class Scene(val scene: com.wakeroo.app.ui.scenes.Scene) : Backdrop
}

/** Pages turn like the faces of a cube. */
private fun Modifier.cube(pager: PagerState, page: Int) = graphicsLayer {
    val offset = (pager.currentPage - page) + pager.currentPageOffsetFraction
    rotationY = offset.coerceIn(-1f, 1f) * -70f
    transformOrigin = TransformOrigin(if (offset > 0) 1f else 0f, 0.5f)
    cameraDistance = 14f * density
    alpha = 1f - offset.absoluteValue.coerceIn(0f, 1f) * 0.6f
}

@Composable
private fun TopRow(state: OnboardingState, canGoBack: Boolean, onBack: () -> Unit) {
    val numbered = state.numbered
    val position = numbered.indexOf(state.step)
    Row(
        Modifier.fillMaxWidth().height(64.dp).padding(horizontal = Space.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp)) {
            androidx.compose.animation.AnimatedVisibility(canGoBack, enter = fadeIn(), exit = fadeOut()) {
                RoundIconButton(Icons.AutoMirrored.Rounded.ArrowBack, tr("Back"), onBack)
            }
        }
        Spacer(Modifier.size(Space.md))
        if (position >= 0) {
            Column(Modifier.weight(1f)) {
                Text(tr("Step {0} of {1}", position + 1, numbered.size), style = MaterialTheme.typography.labelMedium, color = Paper.copy(alpha = 0.8f))
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    numbered.forEachIndexed { i, _ ->
                        val fill by animateFloatAsState(if (i <= position) 1f else 0f, spring(dampingRatio = 0.7f, stiffness = 200f), label = "seg$i")
                        Box(Modifier.weight(1f).height(5.dp).clip(CircleShape).background(Paper.copy(alpha = 0.2f))) {
                            Box(Modifier.fillMaxWidth(fill).height(5.dp).clip(CircleShape).background(if (i == position) SunriseCoral else MorningGold))
                        }
                    }
                }
            }
            Spacer(Modifier.size(48.dp + Space.md))
        } else Spacer(Modifier.weight(1f))
    }
}

/**
 * The common page layout: a title and line of help, the step's own content (scrollable),
 * and the main button pinned to the bottom.
 */
@Composable
fun StepScaffold(
    title: String,
    subtitle: String?,
    primary: String,
    onPrimary: () -> Unit,
    primaryEnabled: Boolean = true,
    warm: Boolean = false,
    darkButton: Boolean = warm,
    secondary: String? = null,
    onSecondary: () -> Unit = {},
    scroll: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier).padding(top = Space.md),
        ) {
            // Warm, bright skies get dark text so it stays readable.
            val ink = if (warm) Night else Paper
            Text(title, style = MaterialTheme.typography.displaySmall, color = ink)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = ink.copy(alpha = 0.78f), modifier = Modifier.padding(top = Space.sm))
            }
            Spacer(Modifier.height(Space.xl))
            content()
            Spacer(Modifier.height(Space.lg))
        }
        PillButton(primary, Modifier.fillMaxWidth(), enabled = primaryEnabled, dark = darkButton, onClick = onPrimary)
        if (secondary != null) {
            Text(
                secondary, style = MaterialTheme.typography.labelLarge, color = (if (warm) Night else Paper).copy(alpha = 0.8f), textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = Space.xs).clip(CircleShape).clickable(onClick = onSecondary).padding(Space.md),
            )
        } else Spacer(Modifier.height(Space.lg))
    }
}

@Composable
fun StepHint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = PaperMuted, modifier = Modifier.padding(top = Space.sm))
}
