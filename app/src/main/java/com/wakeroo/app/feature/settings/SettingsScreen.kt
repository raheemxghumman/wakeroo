package com.wakeroo.app.feature.settings

import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import com.wakeroo.app.ui.i18n.AppLocale
import com.wakeroo.app.ui.i18n.I18n
import com.wakeroo.app.ui.theme.Base
import com.wakeroo.app.ui.theme.NightSurface
import com.wakeroo.app.ui.theme.SunriseCoral
import com.wakeroo.app.ui.i18n.tr
import com.wakeroo.app.ui.theme.MorningGoldFixed
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
import androidx.compose.material.icons.rounded.WbSunny
import com.wakeroo.app.data.AppTheme
import androidx.compose.material.icons.rounded.Wallpaper
import com.wakeroo.app.ui.scenes.ScenePicker
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
import com.wakeroo.app.ui.theme.Glass
import com.wakeroo.app.ui.theme.MorningGold
import com.wakeroo.app.ui.theme.Night
import com.wakeroo.app.ui.theme.Paper
import com.wakeroo.app.ui.theme.PaperFixed
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

    SkyBackground(Sky.Night, intensity = 0.6f) {
        Column(Modifier.fillMaxSize()) {
            TopBar(tr("Settings"), onBack)
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Space.xl),
                verticalArrangement = Arrangement.spacedBy(Space.lg),
            ) {
                HealthCard(items, checker.overall(items), expandedByDefault = focusHealth)
                SectionCard(tr("Test alarm"), icon = Icons.Rounded.Alarm) {
                    Text(
                        tr("Rings in one minute with an easy mission. Lock your phone and wait: if it rings over the lock screen, you're set."),
                        style = MaterialTheme.typography.bodyMedium, color = PaperMuted,
                    )
                    Spacer(Modifier.height(Space.md))
                    PillButton(if (testScheduled) tr("Ringing in 1 minute. Lock your phone") else tr("Ring a test in 1 minute"), Modifier.fillMaxWidth(), enabled = !testScheduled) {
                        app.scheduler.scheduleTest(System.currentTimeMillis() + 60_000)
                        testScheduled = true
                    }
                }
                ProCard(settings.pro, onPro)
                SectionCard(tr("Wake-up scene"), icon = Icons.Rounded.Wallpaper) {
                    Text(
                        tr("The alarm screen for every alarm, unless an alarm picks its own."),
                        style = MaterialTheme.typography.bodySmall, color = PaperMuted, modifier = Modifier.padding(bottom = Space.md),
                    )
                    ScenePicker(
                        selected = settings.ringScene, pro = settings.pro,
                        onSelect = { id -> scope.launch { app.settings.update { it.copy(ringScene = id ?: "night") } } },
                        onUpgrade = onPro,
                    )
                }
                SectionCard(tr("Alarm behaviour"), icon = Icons.Rounded.Alarm) {
                    Text(tr("If I stop during a mission"), style = MaterialTheme.typography.bodyLarge, color = Paper)
                    Text(
                        tr("After this long without a tap, full volume comes back and the mission starts over."),
                        style = MaterialTheme.typography.bodySmall, color = PaperMuted,
                    )
                    Spacer(Modifier.height(Space.sm))
                    ChipRow {
                        listOf(10, 20, 30, 60, 0).forEach { sec ->
                            Chip(if (sec == 0) tr("Never") else "$sec s", settings.missionTimeLimit == sec, {
                                scope.launch { app.settings.update { it.copy(missionTimeLimit = sec) } }
                            })
                        }
                    }
                    Spacer(Modifier.height(Space.lg))
                    ToggleRow(
                        tr("Always ring on the phone speaker"),
                        tr("So a dead Bluetooth speaker or forgotten earbuds never silence your alarm."),
                        settings.alwaysSpeaker,
                    ) { on -> scope.launch { app.settings.update { it.copy(alwaysSpeaker = on) } } }
                    AnimatedVisibility(settings.alwaysSpeaker) {
                        ToggleRow(
                            tr("Also play in headphones"),
                            tr("Ring on the speaker and on whatever is connected."),
                            settings.alsoHeadphones,
                        ) { on -> scope.launch { app.settings.update { it.copy(alsoHeadphones = on) } } }
                    }
                }
                SectionCard(tr("Appearance"), icon = Icons.Rounded.Tune) {
                    Text(tr("Theme"), style = MaterialTheme.typography.bodyLarge, color = Paper)
                    Text(tr("Alarm and mission screens always stay dark, easy on 6 AM eyes."), style = MaterialTheme.typography.bodySmall, color = PaperMuted)
                    Spacer(Modifier.height(Space.sm))
                    ChipRow {
                        AppTheme.entries.forEach { t ->
                            Chip(
                                when (t) { AppTheme.Dark -> tr("Dark"); AppTheme.Light -> tr("Light"); AppTheme.System -> tr("Like my phone") },
                                settings.appTheme == t,
                                { scope.launch { app.settings.update { it.copy(appTheme = t) } } },
                            )
                        }
                    }
                    Spacer(Modifier.height(Space.lg))
                    Text(tr("Animations"), style = MaterialTheme.typography.bodyLarge, color = Paper)
                    Spacer(Modifier.height(Space.sm))
                    ChipRow {
                        MotionPreference.entries.forEach { m ->
                            Chip(
                                when (m) { MotionPreference.System -> tr("Like my phone"); MotionPreference.Full -> tr("Full"); MotionPreference.Reduced -> tr("Reduced") },
                                settings.motion == m,
                                { scope.launch { app.settings.update { it.copy(motion = m) } } },
                            )
                        }
                    }
                    Spacer(Modifier.height(Space.lg))
                    LanguageRow()
                    Spacer(Modifier.height(Space.lg))
                    ToggleRow(
                        tr("24-hour clock"), if (settings.use24Hour == null) tr("Following your phone") else null,
                        settings.use24Hour ?: android.text.format.DateFormat.is24HourFormat(context),
                    ) { on -> scope.launch { app.settings.update { it.copy(use24Hour = on) } } }
                }
                SectionCard(tr("Morning & notifications"), icon = Icons.Rounded.WbSunny) {
                    ToggleRow(
                        tr("Morning briefing"), tr("After an alarm: your plan, the weather, your streak and a quote. Closes by itself."),
                        settings.showGoodMorning,
                    ) { on -> scope.launch { app.settings.update { it.copy(showGoodMorning = on) } } }
                    Spacer(Modifier.height(Space.sm))
                    ToggleRow(
                        tr("Weather heads-up"),
                        if (settings.weatherCity == null) tr("Choose your city on the Morning tab first.") else tr("After you wake, a note if rain, frost or heat is on the way in {0}.", settings.weatherCity?.name),
                        settings.weatherNotification && settings.weatherCity != null,
                    ) { on -> if (settings.weatherCity != null) scope.launch { app.settings.update { it.copy(weatherNotification = on) } } }
                    Spacer(Modifier.height(Space.sm))
                    ToggleRow(tr("Wind-down reminder"), tr("Before bedtime, worked out from your next alarm."), settings.bedtimeReminder) { on ->
                        scope.launch { app.settings.update { it.copy(bedtimeReminder = on) } }
                    }
                    Spacer(Modifier.height(Space.sm))
                    ToggleRow(tr("No alarm for tomorrow"), tr("An evening nudge if nothing is set for the morning."), settings.noAlarmReminder) { on ->
                        scope.launch { app.settings.update { it.copy(noAlarmReminder = on) } }
                    }
                    Text(
                        tr("Reminders use their own notification channel. Muting them never touches your alarms."),
                        style = MaterialTheme.typography.bodySmall, color = PaperMuted, modifier = Modifier.padding(top = Space.sm),
                    )
                }
                SectionCard(tr("About"), icon = Icons.Rounded.FavoriteBorder) {
                    Text(tr("Wakeroo {0}", BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodyLarge, color = Paper)
                    if (BuildConfig.DEBUG) {
                        // Debug builds only: try Pro features without a Play purchase.
                        ToggleRow(tr("Pretend Pro (debug)"), null, settings.pro) { on -> scope.launch { app.settings.update { it.copy(pro = on) } } }
                    }
                    Text(
                        tr("Alarms, missions and photos stay on your phone. No account, no tracking on the alarm path."),
                        style = MaterialTheme.typography.bodySmall, color = PaperMuted, modifier = Modifier.padding(top = Space.xs),
                    )
                }
                if (Ads.privacyOptionsRequired(context)) {
                    Text(
                        tr("Privacy options"), style = MaterialTheme.typography.labelLarge, color = DawnLavender,
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
            Modifier.size(48.dp).press3d().clip(CircleShape).background(Paper.copy(alpha = 0.08f)).clickable(onClickLabel = tr("Back"), onClick = onBack),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, tr("Back"), tint = Paper) }
        Spacer(Modifier.width(Space.lg))
        Text(title, style = MaterialTheme.typography.headlineMedium, color = Paper)
    }
}

@Composable
private fun HealthCard(items: List<HealthItem>, overall: Health, expandedByDefault: Boolean) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(expandedByDefault || overall == Health.Bad) }
    val color by animateColorAsState(overall.color, WakerooMotion.calm(), label = "health")
    SectionCard(tr("Alarm health"), icon = Icons.Rounded.HealthAndSafety) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(color.copy(alpha = 0.14f)).clickable { expanded = !expanded }.padding(Space.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(Space.md))
            Text(
                when (overall) {
                    Health.Good -> tr("Everything's set. Your alarms will ring.")
                    Health.Warn -> tr("Good, with one or two things to improve")
                    Health.Bad -> tr("Something could stop your alarm from ringing")
                },
                style = MaterialTheme.typography.titleSmall, color = Paper, modifier = Modifier.weight(1f),
            )
            Text(if (expanded) tr("Hide") else tr("Details"), style = MaterialTheme.typography.labelMedium, color = DawnLavender)
        }
        AnimatedVisibility(expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(Modifier.padding(top = Space.md), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                items.forEach { item -> HealthRow(item) { intent -> OemGuides.open(context, intent) } }
                val guide = remember { OemGuides.forThisPhone() }
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Glass).padding(Space.lg)) {
                    Text(tr("{0} tips", guide.brand), style = MaterialTheme.typography.titleSmall, color = MorningGold)
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
                tr("Fix"), style = MaterialTheme.typography.labelLarge, color = Night,
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
            Text(if (pro) tr("Wakeroo Pro is yours") else tr("Wakeroo Pro"), style = MaterialTheme.typography.titleMedium, color = PaperFixed)
            Text(
                if (pro) tr("Thank you for supporting an indie alarm.") else tr("Up to 5 missions per alarm, full history, no banner. Pay once."),
                style = MaterialTheme.typography.bodySmall, color = PaperFixed.copy(alpha = 0.8f),
            )
        }
    }
}

val Health.color: Color get() = when (this) { Health.Good -> Calm; Health.Warn -> MorningGoldFixed; Health.Bad -> Alert }

/** The app's language: native names, and "Like my phone" to follow the system. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageRow() {
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    val current = remember { AppLocale.current(context) }
    val name = current?.let { tag -> I18n.languages.firstOrNull { it.first.equals(tag, true) || tag.startsWith(it.first.substringBefore('-')) }?.second } ?: tr("Like my phone")
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { open = true }.padding(vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Language, null, tint = DawnLavender)
        Spacer(Modifier.width(Space.md))
        Column(Modifier.weight(1f)) {
            Text(tr("Language"), style = MaterialTheme.typography.bodyLarge, color = Paper)
            Text(name, style = MaterialTheme.typography.bodySmall, color = PaperMuted)
        }
        Text(tr("Change"), style = MaterialTheme.typography.labelLarge, color = DawnLavender)
    }
    if (open) {
        val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { open = false }, sheetState = sheet, containerColor = Base, dragHandle = null) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(Space.xl)) {
                Text(tr("Language"), style = MaterialTheme.typography.headlineMedium, color = Paper, modifier = Modifier.padding(bottom = Space.lg))
                (listOf(null to tr("Like my phone")) + I18n.languages).forEach { (tag, label) ->
                    val selected = tag?.let { t -> current?.let { it.equals(t, true) || it.startsWith(t.substringBefore('-')) } } ?: (current == null)
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(16.dp))
                            .background(if (selected) SunriseCoral.copy(alpha = 0.16f) else NightSurface)
                            .clickable {
                                open = false
                                (context as? android.app.Activity)?.let { AppLocale.set(it, tag) }
                            }
                            .padding(Space.lg),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(label, style = MaterialTheme.typography.titleMedium, color = Paper, modifier = Modifier.weight(1f))
                        if (selected) Icon(Icons.Rounded.Check, null, tint = SunriseCoral)
                    }
                }
            }
        }
    }
}
