package com.wakeroo.app.feature.settings

import android.app.Activity
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wakeroo.app.BuildConfig
import com.wakeroo.app.alarm.health.Health
import com.wakeroo.app.alarm.health.HealthChecker
import com.wakeroo.app.alarm.health.HealthItem
import com.wakeroo.app.alarm.health.OemGuides
import com.wakeroo.app.billing.Ads
import com.wakeroo.app.feature.pro.AdBanner
import com.wakeroo.app.container
import com.wakeroo.app.data.MotionPreference
import com.wakeroo.app.data.Settings
import com.wakeroo.app.feature.editor.PillButton
import com.wakeroo.app.ui.components.Chip
import com.wakeroo.app.ui.components.ChipRow
import com.wakeroo.app.ui.components.SectionCard
import com.wakeroo.app.ui.components.SkyBackground
import com.wakeroo.app.ui.components.ToggleRow
import com.wakeroo.app.ui.components.press3d
import com.wakeroo.app.ui.theme.Alert
import com.wakeroo.app.ui.theme.Calm
import com.wakeroo.app.ui.theme.DawnLavender
import com.wakeroo.app.ui.theme.MorningGold
import com.wakeroo.app.ui.theme.Night
import com.wakeroo.app.ui.theme.Paper
import com.wakeroo.app.ui.theme.PaperMuted
import com.wakeroo.app.ui.theme.Sky
import com.wakeroo.app.ui.theme.Space
import com.wakeroo.app.ui.theme.WakerooMotion
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(focusHealth: Boolean, onBack: () -> Unit, onPro: () -> Unit) {
    val context = LocalContext.current
    val app = context.container
    val scope = rememberCoroutineScope()
    val settings by app.settings.settings.collectAsStateWithLifecycle(Settings())
    val checker = remember { HealthChecker(context) }
    var items by remember { mutableStateOf(emptyList<HealthItem>()) }
    LifecycleResumeEffect(settings.lastTestAlarmAt) {
        items = checker.check(settings.lastTestAlarmAt, settings.lastTestAlarmPassed)
        onPauseOrDispose { }
    }
    var testScheduled by remember { mutableStateOf(false) }

    SkyBackground(Sky.Night) {
        Column(Modifier.fillMaxSize()) {
            TopBar("Settings", onBack)
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Space.xl),
                verticalArrangement = Arrangement.spacedBy(Space.lg),
            ) {
                HealthCard(items, checker.overall(items), expandedByDefault = focusHealth)
                SectionCard("Test alarm", icon = Icons.Rounded.Alarm) {
                    Text(
                        "Rings in one minute with an easy mission. Lock your phone and wait: if it rings over the lock screen, you're set.",
                        style = MaterialTheme.typography.bodyMedium, color = PaperMuted,
                    )
                    Spacer(Modifier.height(Space.md))
                    PillButton(if (testScheduled) "Ringing in 1 minute. Lock your phone" else "Ring a test in 1 minute", Modifier.fillMaxWidth(), enabled = !testScheduled) {
                        app.scheduler.scheduleTest(System.currentTimeMillis() + 60_000)
                        testScheduled = true
                    }
                }
                ProCard(settings.pro, onPro)
                SectionCard("Preferences", icon = Icons.Rounded.Tune) {
                    ToggleRow(
                        "Good-morning screen", "A quick celebration after you finish your mission.", settings.showGoodMorning,
                    ) { on -> scope.launch { app.settings.update { it.copy(showGoodMorning = on) } } }
                    Spacer(Modifier.height(Space.md))
                    Text("Animations", style = MaterialTheme.typography.bodyLarge, color = Paper)
                    Spacer(Modifier.height(Space.sm))
                    ChipRow {
                        MotionPreference.entries.forEach { m ->
                            Chip(
                                when (m) { MotionPreference.System -> "Like my phone"; MotionPreference.Full -> "Full"; MotionPreference.Reduced -> "Reduced" },
                                settings.motion == m,
                                { scope.launch { app.settings.update { it.copy(motion = m) } } },
                            )
                        }
                    }
                }
                SectionCard("About", icon = Icons.Rounded.FavoriteBorder) {
                    Text("Wakeroo ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyLarge, color = Paper)
                    Text(
                        "Alarms, missions and photos stay on your phone. No account, no tracking on the alarm path.",
                        style = MaterialTheme.typography.bodySmall, color = PaperMuted, modifier = Modifier.padding(top = Space.xs),
                    )
                }
                if (Ads.privacyOptionsRequired(context)) {
                    Text(
                        "Privacy options", style = MaterialTheme.typography.labelLarge, color = DawnLavender,
                        modifier = Modifier.clip(CircleShape).clickable { (context as? Activity)?.let(Ads::showPrivacyOptions) }.padding(Space.md),
                    )
                }
                Spacer(Modifier.height(Space.xxl))
            }
            if (!settings.pro) AdBanner(Modifier.fillMaxWidth().navigationBarsPadding())
        }
    }
}

@Composable
fun TopBar(title: String, onBack: () -> Unit) {
    Row(Modifier.statusBarsPadding().fillMaxWidth().padding(horizontal = Space.lg, vertical = Space.sm), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(48.dp).press3d().clip(CircleShape).background(Paper.copy(alpha = 0.08f)).clickable(onClickLabel = "Back", onClick = onBack),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Paper) }
        Spacer(Modifier.width(Space.lg))
        Text(title, style = MaterialTheme.typography.headlineMedium, color = Paper)
    }
}

@Composable
private fun HealthCard(items: List<HealthItem>, overall: Health, expandedByDefault: Boolean) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(expandedByDefault || overall == Health.Bad) }
    val color by animateColorAsState(overall.color, WakerooMotion.calm(), label = "health")
    SectionCard("Alarm health", icon = Icons.Rounded.HealthAndSafety) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(color.copy(alpha = 0.14f)).clickable { expanded = !expanded }.padding(Space.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(Space.md))
            Text(
                when (overall) {
                    Health.Good -> "Everything's set. Your alarms will ring."
                    Health.Warn -> "Good, with one or two things to improve"
                    Health.Bad -> "Something could stop your alarm from ringing"
                },
                style = MaterialTheme.typography.titleSmall, color = Paper, modifier = Modifier.weight(1f),
            )
            Text(if (expanded) "Hide" else "Details", style = MaterialTheme.typography.labelMedium, color = DawnLavender)
        }
        AnimatedVisibility(expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(Modifier.padding(top = Space.md), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                items.forEach { item -> HealthRow(item) { intent -> OemGuides.open(context, intent) } }
                val guide = remember { OemGuides.forThisPhone() }
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Night.copy(alpha = 0.4f)).padding(Space.lg)) {
                    Text("${guide.brand} tips", style = MaterialTheme.typography.titleSmall, color = MorningGold)
                    guide.steps.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = Paper, modifier = Modifier.padding(top = Space.xs)) }
                }
            }
        }
    }
}

@Composable
private fun HealthRow(item: HealthItem, onFix: (Intent) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = Space.xs), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(item.status.color))
        Spacer(Modifier.width(Space.md))
        Column(Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.bodyLarge, color = Paper)
            Text(item.detail, style = MaterialTheme.typography.bodySmall, color = PaperMuted)
        }
        item.fix?.let { fix ->
            Text(
                "Fix", style = MaterialTheme.typography.labelLarge, color = Night,
                modifier = Modifier.clip(CircleShape).background(item.status.color).clickable { onFix(fix()) }.padding(horizontal = Space.lg, vertical = Space.sm),
            )
        }
    }
}

@Composable
private fun ProCard(pro: Boolean, onPro: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .press3d(maxTilt = 4f, sink = 0.98f)
            .clip(RoundedCornerShape(28.dp))
            .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF3A2B4E), Color(0xFF6B4A78))))
            .border(1.dp, MorningGold.copy(alpha = 0.3f), RoundedCornerShape(28.dp))
            .clickable(onClick = onPro)
            .padding(Space.xl),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.AutoAwesome, null, tint = MorningGold)
        Spacer(Modifier.width(Space.lg))
        Column(Modifier.weight(1f)) {
            Text(if (pro) "Wakeroo Pro is yours" else "Wakeroo Pro", style = MaterialTheme.typography.titleMedium, color = Paper)
            Text(
                if (pro) "Thank you for supporting an indie alarm." else "More missions per alarm, harder mode, themes. Pay once.",
                style = MaterialTheme.typography.bodySmall, color = Paper.copy(alpha = 0.8f),
            )
        }
    }
}

val Health.color: Color get() = when (this) { Health.Good -> Calm; Health.Warn -> MorningGold; Health.Bad -> Alert }
