package com.wakeroo.app.feature.onboarding

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bathtub
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.wakeroo.app.alarm.health.OemGuides
import com.wakeroo.app.container
import com.wakeroo.app.data.model.MissionType
import com.wakeroo.app.feature.editor.PillButton
import com.wakeroo.app.ui.brand.AnimatedWakerooMark
import com.wakeroo.app.ui.brand.Wordmark
import com.wakeroo.app.ui.components.SkyBackground
import com.wakeroo.app.ui.components.press3d
import com.wakeroo.app.ui.theme.Calm
import com.wakeroo.app.ui.theme.DawnLavender
import com.wakeroo.app.ui.theme.MorningGold
import com.wakeroo.app.ui.theme.Night
import com.wakeroo.app.ui.theme.NightSurface
import com.wakeroo.app.ui.theme.Paper
import com.wakeroo.app.ui.theme.PaperMuted
import com.wakeroo.app.ui.theme.Sky
import com.wakeroo.app.ui.theme.Space
import com.wakeroo.app.ui.theme.SunriseCoral
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private enum class Page { Welcome, Spot, Notifications, FullScreen, Battery, Test }

@Composable
fun OnboardingScreen(onFinished: (createFirstAlarm: Boolean) -> Unit) {
    val context = LocalContext.current
    val app = context.container
    val scope = rememberCoroutineScope()
    val pages = remember {
        Page.entries.filter { page ->
            when (page) {
                Page.Notifications -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                Page.FullScreen -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                    !context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
                else -> true
            }
        }
    }
    val pager = rememberPagerState { pages.size }
    fun next() {
        if (pager.currentPage + 1 >= pages.size) {
            scope.launch {
                app.settings.update { it.copy(onboardingDone = true) }
                onFinished(true)
            }
        } else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
    }
    val sky = when (pages[pager.currentPage]) {
        Page.Welcome -> Sky.Night
        Page.Spot, Page.Notifications -> Sky.Dawn
        Page.FullScreen, Page.Battery, Page.Test -> Sky.Sunrise
    }

    SkyBackground(sky) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Dots(pager, pages.size)
            HorizontalPager(pager, userScrollEnabled = false, modifier = Modifier.weight(1f)) { index ->
                Box(Modifier.fillMaxSize().cube(pager, index).padding(horizontal = Space.xl)) {
                    when (pages[index]) {
                        Page.Welcome -> WelcomePage(::next)
                        Page.Spot -> SpotPage { type ->
                            scope.launch { app.settings.update { it.copy(firstMission = type.name) } }
                            next()
                        }
                        Page.Notifications -> NotificationsPage(::next)
                        Page.FullScreen -> FullScreenPage(::next)
                        Page.Battery -> BatteryPage(::next)
                        Page.Test -> TestPage(onDone = ::next)
                    }
                }
            }
        }
    }
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
private fun Dots(pager: PagerState, count: Int) {
    Row(Modifier.fillMaxWidth().padding(Space.xl), horizontalArrangement = Arrangement.Center) {
        repeat(count) { i ->
            val active = i == pager.currentPage
            Box(
                Modifier
                    .padding(horizontal = 4.dp)
                    .height(6.dp)
                    .width(if (active) 28.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (active) SunriseCoral else Paper.copy(alpha = 0.25f)),
            )
        }
    }
}

@Composable
private fun PageLayout(
    icon: ImageVector?,
    title: String,
    body: String,
    content: @Composable () -> Unit = {},
    primary: String,
    onPrimary: () -> Unit,
    secondary: String? = null,
    onSecondary: () -> Unit = {},
    warm: Boolean = false,
) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(0.6f))
        if (icon != null) {
            Box(Modifier.size(96.dp).clip(CircleShape).background(if (warm) Night.copy(alpha = 0.25f) else Paper.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = if (warm) Paper else MorningGold, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(Space.xl))
        }
        Text(title, style = MaterialTheme.typography.displaySmall, color = Paper, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyLarge, color = Paper.copy(alpha = 0.8f), textAlign = TextAlign.Center, modifier = Modifier.padding(top = Space.md))
        Spacer(Modifier.height(Space.xl))
        content()
        Spacer(Modifier.weight(1f))
        PillButton(primary, Modifier.fillMaxWidth(), dark = warm, onClick = onPrimary)
        if (secondary != null) {
            Text(
                secondary, style = MaterialTheme.typography.labelLarge, color = Paper.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = Space.sm).clip(CircleShape).clickable(onClick = onSecondary).padding(Space.md),
            )
        } else Spacer(Modifier.height(Space.xxl + Space.md))
        Spacer(Modifier.height(Space.lg))
    }
}

@Composable
private fun WelcomePage(onNext: () -> Unit) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(0.7f))
        AnimatedWakerooMark(Modifier.size(220.dp))
        Wordmark(fontSize = MaterialTheme.typography.displayMedium.fontSize)
        Text(
            "Wake up for real.", style = MaterialTheme.typography.headlineMedium, color = DawnLavender,
            modifier = Modifier.padding(top = Space.sm),
        )
        Text(
            "An alarm that keeps ringing until you've done a quick mission, like solving a sum or scanning a code in another room.",
            style = MaterialTheme.typography.bodyLarge, color = PaperMuted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = Space.lg),
        )
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.lg), modifier = Modifier.padding(bottom = Space.xl)) {
            Promise("No ads on\nyour alarm")
            Promise("Every mission\nfree")
            Promise("Pay once,\nif ever")
        }
        PillButton("Get started", Modifier.fillMaxWidth(), onClick = onNext)
        Spacer(Modifier.height(Space.xxl + Space.lg))
    }
}

@Composable
private fun Promise(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.CheckCircle, null, tint = Calm, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = Paper)
    }
}

@Composable
private fun SpotPage(onChoose: (MissionType) -> Unit) {
    val options = listOf(
        Triple(Icons.Rounded.Bathtub, "Bathroom sink", MissionType.Photo),
        Triple(Icons.Rounded.Coffee, "Coffee or kettle", MissionType.Scan),
        Triple(Icons.Rounded.Medication, "Meds shelf", MissionType.Scan),
        Triple(Icons.Rounded.Calculate, "Just math for now", MissionType.Math),
    )
    var chosen by remember { mutableStateOf<Int?>(null) }
    PageLayout(
        icon = null,
        title = "Where should your off-switch live?",
        body = "The best way to stay up is to get out of bed. Pick a spot; you'll set it up with your first alarm.",
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
                options.chunked(2).forEachIndexed { row, pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                        pair.forEachIndexed { col, (icon, label, _) ->
                            val i = row * 2 + col
                            val selected = chosen == i
                            Column(
                                Modifier
                                    .weight(1f)
                                    .height(118.dp)
                                    .press3d(maxTilt = 12f)
                                    .clip(RoundedCornerShape(26.dp))
                                    .background(if (selected) SunriseCoral else NightSurface.copy(alpha = 0.85f))
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(26.dp))
                                    .clickable { chosen = i }
                                    .padding(Space.lg),
                            ) {
                                Icon(icon, null, tint = if (selected) Night else MorningGold)
                                Spacer(Modifier.weight(1f))
                                Text(label, style = MaterialTheme.typography.titleSmall, color = if (selected) Night else Paper)
                            }
                        }
                    }
                }
                Text(
                    "Heads-up: the first two weeks are the hardest. Then it sticks.",
                    style = MaterialTheme.typography.bodySmall, color = Paper.copy(alpha = 0.7f), textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = Space.sm),
                )
            }
        },
        primary = if (chosen == null) "Choose a spot" else "Continue",
        onPrimary = { chosen?.let { onChoose(options[it].third) } },
    )
}

@Composable
private fun NotificationsPage(onNext: () -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onNext() }
    PageLayout(
        Icons.Rounded.NotificationsActive,
        "Let your alarm reach you",
        "Wakeroo needs notifications to show the alarm screen. We never send marketing notifications.",
        primary = "Allow notifications",
        onPrimary = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) },
    )
}

@Composable
private fun FullScreenPage(onNext: () -> Unit) {
    val context = LocalContext.current
    var opened by remember { mutableStateOf(false) }
    LifecycleResumeEffect(opened) {
        if (opened && context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()) onNext()
        onPauseOrDispose { }
    }
    PageLayout(
        Icons.Rounded.Fullscreen,
        "Take over the lock screen",
        "So the alarm fills your screen instead of hiding in a small banner. Turn on \"Allow full screen\" for Wakeroo.",
        primary = "Open setting",
        onPrimary = {
            opened = true
            OemGuides.open(context, Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${context.packageName}")))
        },
        secondary = "Skip for now", onSecondary = onNext, warm = true,
    )
}

@Composable
private fun BatteryPage(onNext: () -> Unit) {
    val context = LocalContext.current
    val guide = remember { OemGuides.forThisPhone() }
    var opened by remember { mutableStateOf(false) }
    PageLayout(
        Icons.Rounded.BatteryChargingFull,
        "Keep ${guide.brand} from pausing Wakeroo",
        "Phones save battery by putting apps to sleep. That's the #1 reason alarm apps miss alarms.",
        content = {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Night.copy(alpha = 0.45f)).padding(Space.lg),
                verticalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                guide.steps.forEachIndexed { i, step ->
                    Row {
                        Text("${i + 1}", style = MaterialTheme.typography.titleSmall, color = MorningGold, modifier = Modifier.width(24.dp))
                        Text(step, style = MaterialTheme.typography.bodyMedium, color = Paper)
                    }
                }
            }
        },
        primary = if (opened) "I've done it" else "Open battery settings",
        onPrimary = {
            if (opened) onNext() else {
                opened = true
                OemGuides.open(context, OemGuides.batteryIntent(context))
            }
        },
        secondary = "Skip for now", onSecondary = onNext, warm = true,
    )
}

@Composable
private fun TestPage(onDone: () -> Unit) {
    val context = LocalContext.current
    var scheduled by remember { mutableStateOf(false) }
    AnimatedContent(scheduled, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "test") { done ->
        if (!done) {
            PageLayout(
                Icons.Rounded.Alarm,
                "Let's prove it works",
                "A test alarm rings in one minute. Lock your phone and wait. If it rings over the lock screen, you're set.",
                primary = "Ring in 1 minute",
                onPrimary = {
                    context.container.scheduler.scheduleTest(System.currentTimeMillis() + 60_000)
                    scheduled = true
                },
                secondary = "Skip, I'll test later", onSecondary = onDone, warm = true,
            )
        } else {
            PageLayout(
                Icons.Rounded.CheckCircle,
                "Lock your phone now",
                "The test rings in a minute. Meanwhile, let's set your first real alarm.",
                primary = "Set my first alarm",
                onPrimary = onDone, warm = true,
            )
        }
    }
}
