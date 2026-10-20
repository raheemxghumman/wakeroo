package com.wakeroo.app.feature.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.wakeroo.app.ui.theme.LocalReducedMotion

/** Space a tab's scrolling content leaves at the bottom so nothing hides behind the bar. */
val TabContentBottomPadding = BottomBarHeight + 56.dp

/**
 * The four-tab home of the app. Each tab keeps its own state (scroll position, open cards)
 * while you switch, and tabs turn past each other like panels on a carousel.
 */
@Composable
fun MainScaffold(
    initialTab: MainTab,
    snackbar: SnackbarHostState,
    content: @Composable (tab: MainTab, selectTab: (MainTab) -> Unit) -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(initialTab) }
    var barVisible by remember { mutableStateOf(true) }
    var direction by remember { mutableIntStateOf(1) }
    val holder = rememberSaveableStateHolder()
    val reduced = LocalReducedMotion.current
    LaunchedEffect(tab) { barVisible = true }
    fun select(next: MainTab) {
        direction = if (next.ordinal >= tab.ordinal) 1 else -1
        tab = next
    }
    val hideOnScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -8f) barVisible = false else if (available.y > 8f) barVisible = true
                return Offset.Zero
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = tab,
            modifier = Modifier.fillMaxSize().nestedScroll(hideOnScroll),
            transitionSpec = {
                val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                if (reduced) fadeIn() togetherWith fadeOut()
                else (slideInHorizontally(spring(dampingRatio = 0.86f, stiffness = 340f)) { dir * it / 4 } + fadeIn()) togetherWith
                    (slideOutHorizontally(spring(dampingRatio = 0.86f, stiffness = 340f)) { -dir * it / 4 } + fadeOut())
            },
            label = "tabs",
        ) { shown ->
            // A slight turn in depth as panels swap, so it reads as one rotating carousel.
            val turn by transition.animateFloat(label = "turn") { state ->
                when (state) {
                    EnterExitState.PreEnter -> 1f
                    EnterExitState.Visible -> 0f
                    EnterExitState.PostExit -> -1f
                }
            }
            Box(
                Modifier.fillMaxSize().graphicsLayer {
                    if (!reduced) {
                        rotationY = turn * 10f * direction
                        cameraDistance = 14f * density
                    }
                },
            ) {
                holder.SaveableStateProvider(shown.name) { content(shown, ::select) }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = BottomBarHeight + 28.dp))
        BottomBar(tab, barVisible, onSelect = ::select, modifier = Modifier.align(Alignment.BottomCenter))
    }
}
