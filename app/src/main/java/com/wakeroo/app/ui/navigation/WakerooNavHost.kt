package com.wakeroo.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.wakeroo.app.alarm.health.HealthChecker
import com.wakeroo.app.container
import com.wakeroo.app.feature.editor.EditorScreen
import com.wakeroo.app.feature.editor.EditorViewModel
import com.wakeroo.app.feature.home.HomeScreen
import com.wakeroo.app.feature.home.HomeViewModel
import com.wakeroo.app.feature.onboarding.OnboardingScreen
import com.wakeroo.app.feature.pro.ProScreen
import com.wakeroo.app.feature.settings.SettingsScreen
import com.wakeroo.app.feature.main.MainScaffold
import com.wakeroo.app.feature.main.MainTab
import com.wakeroo.app.feature.morning.MorningScreen
import com.wakeroo.app.feature.sleep.SleepScreen
import com.wakeroo.app.feature.you.YouScreen
import com.wakeroo.app.ui.components.LocalEntranceGate
import com.wakeroo.app.ui.components.LocalNavScope
import com.wakeroo.app.ui.intro.IntroSequence
import com.wakeroo.app.ui.components.LocalSharedScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data object OnboardingRoute
@Serializable
data object MainRoute
@Serializable
data class EditorRoute(val id: Long = 0)
@Serializable
data class SettingsRoute(val focusHealth: Boolean = false)
@Serializable
data object ProRoute

private val springy = spring<Float>(dampingRatio = 0.85f, stiffness = 380f)

// Screens move in depth: the new one rises from slightly smaller, the old one recedes.
private val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
    { fadeIn(springy) + scaleIn(springy, initialScale = 0.94f) }
private val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
    { fadeOut(springy) + scaleOut(springy, targetScale = 1.04f) }
private val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
    { fadeIn(springy) + scaleIn(springy, initialScale = 1.04f) }
private val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
    { fadeOut(springy) + scaleOut(springy, targetScale = 0.94f) }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun WakerooNavHost(onboardingDone: Boolean, openHealth: Boolean, initialTab: MainTab = MainTab.Alarms) {
    val nav = rememberNavController()
    val context = LocalContext.current
    val app = context.container
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // Decided once: finishing onboarding navigates on its own, so the graph must not reset.
    val start: Any = remember {
        when {
            !onboardingDone -> OnboardingRoute
            openHealth -> SettingsRoute(focusHealth = true)
            else -> MainRoute
        }
    }

    // The launch intro plays once per cold start; the app assembles itself as it hands over.
    var introDone by rememberSaveable { mutableStateOf(false) }
    var revealed by rememberSaveable { mutableStateOf(false) }

    Box {
        SharedTransitionLayout {
            CompositionLocalProvider(
                LocalSharedScope provides this,
                LocalEntranceGate provides revealed
            ) {
                NavHost(
                    nav,
                    startDestination = start,
                    enterTransition = enter,
                    exitTransition = exit,
                    popEnterTransition = popEnter,
                    popExitTransition = popExit
                ) {
                    composable<OnboardingRoute> {
                        CompositionLocalProvider(LocalNavScope provides this) {
                            OnboardingScreen(onFinished = { createFirst ->
                                nav.navigate(MainRoute) {
                                    popUpTo(OnboardingRoute) {
                                        inclusive = true
                                    }
                                }
                                if (createFirst) nav.navigate(EditorRoute())
                            })
                        }
                    }
                    composable<MainRoute> {
                        CompositionLocalProvider(LocalNavScope provides this) {
                            val vm = viewModel { HomeViewModel(app, HealthChecker(context)) }
                            MainScaffold(initialTab, snackbar) { tab, _ ->
                                val openSettings = { nav.navigate(SettingsRoute()) }
                                when (tab) {
                                    MainTab.Alarms -> HomeScreen(
                                        vm = vm, snackbar = snackbar,
                                        onNewAlarm = { nav.navigate(EditorRoute()) },
                                        onEdit = { nav.navigate(EditorRoute(it)) },
                                        onSettings = openSettings,
                                        onHealth = { nav.navigate(SettingsRoute(focusHealth = true)) },
                                    )
                                    MainTab.Sleep -> SleepScreen(onSettings = openSettings)
                                    MainTab.Morning -> MorningScreen(onSettings = openSettings)
                                    MainTab.You -> YouScreen(onSettings = openSettings, onPro = { nav.navigate(ProRoute) })
                                }
                            }
                        }
                    }
                    composable<EditorRoute> { entry ->
                        CompositionLocalProvider(LocalNavScope provides this) {
                            val id = entry.toRoute<EditorRoute>().id
                            val vm = viewModel { EditorViewModel(app, id) }
                            EditorScreen(
                                vm = vm,
                                onClose = { nav.popBackStack() },
                                onSaved = { message ->
                                    nav.popBackStack()
                                    scope.launch { snackbar.showSnackbar(message) }
                                },
                                onUpgrade = { nav.navigate(ProRoute) },
                            )
                        }
                    }
                    composable<SettingsRoute> { entry ->
                        CompositionLocalProvider(LocalNavScope provides this) {
                            SettingsScreen(
                                focusHealth = entry.toRoute<SettingsRoute>().focusHealth,
                                onBack = { if (!nav.popBackStack()) nav.navigate(MainRoute) },
                                onPro = { nav.navigate(ProRoute) },
                            )
                        }
                    }
                    composable<ProRoute> {
                        CompositionLocalProvider(LocalNavScope provides this) {
                            ProScreen(onClose = { nav.popBackStack() })
                        }
                    }
                }
            }
        }
        if (!introDone) IntroSequence(
            onReveal = { revealed = true },
            onFinished = { introDone = true })
    }
}
