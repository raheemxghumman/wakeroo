package com.wakeroo.app.feature.onboarding

import com.wakeroo.app.ui.i18n.tr
import com.wakeroo.app.ui.i18n.n
import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.core.app.NotificationManagerCompat
import com.wakeroo.app.alarm.health.OemGuides
import com.wakeroo.app.ui.theme.Calm
import com.wakeroo.app.ui.theme.Inter
import com.wakeroo.app.ui.theme.MorningGold
import com.wakeroo.app.ui.theme.Night
import com.wakeroo.app.ui.theme.Paper
import com.wakeroo.app.ui.theme.PaperMuted
import com.wakeroo.app.ui.theme.Space
import com.wakeroo.app.ui.theme.SunriseCoral
import kotlin.math.abs
import androidx.compose.material.icons.rounded.Layers

enum class Permission(private val titleEn: String, private val whyEn: String, private val actionEn: String, val icon: ImageVector, val optional: Boolean = false) {
    Notifications(n("Alarm alerts"), n("So the alarm screen can appear"), n("Allow alarm alerts"), Icons.Rounded.NotificationsActive),
    FullScreen(n("Full-screen alarm"), n("Takes over the lock screen instead of a small banner"), n("Turn on full screen"), Icons.Rounded.Fullscreen),
    Battery(n("No battery limits"), n("The #1 reason alarm apps miss alarms"), n("Remove battery limits"), Icons.Rounded.BatteryChargingFull),
    Overlay(n("Open over other apps"), n("Optional: the full alarm screen even while you're using your phone"), n("Turn on"), Icons.Rounded.Layers, optional = true);

    val title: String get() = tr(titleEn)
    val why: String get() = tr(whyEn)
    val action: String get() = tr(actionEn)
}

/** Which permissions this phone needs at all; full-screen only exists as a switch on Android 14+. */
fun neededPermissions(context: Context): List<Permission> = buildList {
    add(Permission.Notifications)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && !context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()) add(Permission.FullScreen)
    add(Permission.Battery)
    if (!Settings.canDrawOverlays(context)) add(Permission.Overlay)
}

fun isGranted(context: Context, p: Permission): Boolean = when (p) {
    Permission.Notifications -> NotificationManagerCompat.from(context).areNotificationsEnabled()
    Permission.FullScreen -> Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE || context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
    Permission.Battery -> context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
    Permission.Overlay -> Settings.canDrawOverlays(context)
}

/**
 * "Make sure it rings": every switch an alarm depends on, on one screen. Rows turn green as
 * they're granted, and a little mock shows exactly what to tap for the next one.
 */
@Composable
fun PermissionsStep(onNext: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val needed = remember { neededPermissions(context) }
    var tick by remember { mutableIntStateOf(0) }
    // Battery can't always be read back on custom Android skins, so "I've done it" counts.
    var batteryConfirmed by remember { mutableStateOf(false) }
    var openedBattery by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        tick++
        onPauseOrDispose { }
    }
    val granted = remember(tick, batteryConfirmed) {
        needed.associateWith { isGranted(context, it) || (it == Permission.Battery && batteryConfirmed) }
    }
    var lastGranted by remember { mutableIntStateOf(granted.count { it.value }) }
    LaunchedEffect(granted) {
        val now = granted.count { it.value }
        if (now > lastGranted) haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        lastGranted = now
    }
    // Optional rows never hold up "Continue"; they're one tap away in the list.
    val nextMissing = needed.firstOrNull { !it.optional && granted[it] != true }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }

    fun request(p: Permission) {
        when (p) {
            Permission.Notifications -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else OemGuides.open(context, Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
            Permission.FullScreen -> OemGuides.open(context, Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${context.packageName}")))
            Permission.Battery -> {
                if (openedBattery) batteryConfirmed = true else {
                    openedBattery = true
                    OemGuides.open(context, OemGuides.batteryIntent(context))
                }
            }
            Permission.Overlay -> OemGuides.open(context, Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
        }
    }

    StepScaffold(
        title = tr("Make sure it rings"),
        subtitle = tr("Phones block alarms in sneaky ways. Turn these on once and you're covered."),
        primary = when {
            nextMissing == null -> tr("Continue")
            nextMissing == Permission.Battery && openedBattery -> tr("I've set it to Unrestricted")
            else -> nextMissing.action
        },
        onPrimary = { if (nextMissing == null) onNext() else request(nextMissing) },
        secondary = if (nextMissing != null) tr("Skip for now") else null,
        onSecondary = onNext,
        darkButton = true,
    ) {
        AnimatedContent(nextMissing, transitionSpec = { (scaleIn(initialScale = 0.9f) + fadeIn()) togetherWith fadeOut() }, label = "mock") { p ->
            Box(Modifier.fillMaxWidth().height(170.dp), contentAlignment = Alignment.Center) {
                when (p) {
                    Permission.Notifications -> PermissionMock(MockKind.Dialog)
                    Permission.FullScreen -> PermissionMock(MockKind.Toggle)
                    Permission.Battery -> PermissionMock(MockKind.Radio)
                    Permission.Overlay, null -> AllSet()
                }
            }
        }
        Spacer(Modifier.height(Space.lg))
        Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
            needed.forEach { p -> PermissionRow(p, granted[p] == true, highlighted = p == nextMissing) { request(p) } }
        }
        if (nextMissing == Permission.Battery) {
            val guide = remember { OemGuides.forThisPhone() }
            Column(
                Modifier.padding(top = Space.lg).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Night.copy(alpha = 0.45f)).padding(Space.lg),
                verticalArrangement = Arrangement.spacedBy(Space.xs),
            ) {
                Text(tr("On {0}", guide.brand), style = MaterialTheme.typography.titleSmall, color = MorningGold)
                guide.steps.forEachIndexed { i, step ->
                    Row {
                        Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = MorningGold, modifier = Modifier.width(22.dp))
                        Text(step, style = MaterialTheme.typography.bodySmall, color = Paper)
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(p: Permission, granted: Boolean, highlighted: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (granted) Calm.copy(alpha = 0.22f) else if (highlighted) Night.copy(alpha = 0.55f) else Night.copy(alpha = 0.35f), label = "permBg")
    val border by animateColorAsState(if (granted) Calm else if (highlighted) SunriseCoral else Color.White.copy(alpha = 0.1f), label = "permBorder")
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(bg)
            .border(1.5.dp, border, RoundedCornerShape(22.dp))
            .clickable(enabled = !granted, onClick = onClick)
            .padding(Space.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(if (granted) Calm else MorningGold.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
            Icon(if (granted) Icons.Rounded.CheckCircle else p.icon, null, tint = if (granted) Night else MorningGold)
        }
        Spacer(Modifier.width(Space.md))
        Column(Modifier.weight(1f)) {
            Text(p.title, style = MaterialTheme.typography.titleSmall, color = Paper)
            Text(if (granted) tr("Done") else p.why, style = MaterialTheme.typography.bodySmall, color = if (granted) Calm else PaperMuted)
        }
    }
}

private enum class MockKind { Dialog, Toggle, Radio }

private val MockInk = Color(0xFF1B1E2E)
private val MockPanel = Color(0xFFF7F4EF)

/**
 * A tiny, looping picture of the system screen the user is about to see, with a finger
 * showing what to tap. Drawn, not screenshotted, so it fits every phone and language.
 */
@Composable
private fun PermissionMock(kind: MockKind) {
    val text = rememberTextMeasurer()
    val t by rememberInfiniteTransition(label = "mock").animateFloat(
        0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart), label = "t",
    )
    Canvas(Modifier.fillMaxWidth(0.82f).height(160.dp)) {
        val card = Size(size.width, size.height)
        drawRoundRect(MockPanel, Offset.Zero, card, CornerRadius(20.dp.toPx()))
        val pad = 16.dp.toPx()
        val title = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = (13 * 1f).dp.toSp(), color = MockInk)
        val body = TextStyle(fontFamily = Inter, fontSize = 11.dp.toSp(), color = MockInk.copy(alpha = 0.6f))
        // The finger travels to the target, presses, and lifts.
        val press = when {
            t < 0.35f -> 0f
            t < 0.45f -> (t - 0.35f) / 0.1f
            t < 0.55f -> 1f - (t - 0.45f) / 0.1f
            else -> 0f
        }
        val on = t > 0.45f
        val target: Offset = when (kind) {
            MockKind.Dialog -> {
                // Android's own layout: a bell, the question, then stacked Allow / Don't allow.
                drawCircle(Color(0xFF4A5BD6), 7.dp.toPx(), Offset(size.width / 2, pad + 6.dp.toPx()))
                val q = text.measure(tr("Allow Wakeroo to send you notifications?"), title.copy(fontSize = 12.dp.toSp()))
                drawText(q, topLeft = Offset((size.width - q.size.width) / 2, pad + 18.dp.toPx()))
                val bw = size.width - pad * 2
                val bh = 26.dp.toPx()
                val allowY = pad + 46.dp.toPx()
                drawRoundRect(if (on) Calm else Color(0xFFD6E4FF), Offset(pad, allowY), Size(bw, bh), CornerRadius(bh / 2))
                drawRoundRect(Color(0xFFD6E4FF), Offset(pad, allowY + bh + 6.dp.toPx()), Size(bw, bh), CornerRadius(bh / 2))
                val a = text.measure(tr("Allow"), title.copy(fontSize = 11.dp.toSp()))
                drawText(a, topLeft = Offset((size.width - a.size.width) / 2, allowY + (bh - a.size.height) / 2))
                val d = text.measure(tr("Don't allow"), body.copy(color = MockInk))
                drawText(d, topLeft = Offset((size.width - d.size.width) / 2, allowY + bh + 6.dp.toPx() + (bh - d.size.height) / 2))
                Offset(size.width / 2, allowY + bh / 2)
            }
            MockKind.Toggle -> {
                drawText(text, tr("Wakeroo"), Offset(pad, pad), title)
                drawText(text, tr("Allow full screen notifications"), Offset(pad, pad + 44.dp.toPx()), body.copy(color = MockInk, fontSize = 12.dp.toSp()))
                val tw = 46.dp.toPx(); val th = 26.dp.toPx()
                val tx = size.width - pad - tw; val ty = pad + 40.dp.toPx()
                drawRoundRect(if (on) Calm else MockInk.copy(alpha = 0.25f), Offset(tx, ty), Size(tw, th), CornerRadius(th / 2))
                drawCircle(Color.White, th / 2 - 3.dp.toPx(), Offset(if (on) tx + tw - th / 2 else tx + th / 2, ty + th / 2))
                Offset(tx + tw / 2, ty + th / 2)
            }
            MockKind.Radio -> {
                drawText(text, tr("App battery usage"), Offset(pad, pad), title)
                listOf(tr("Unrestricted"), tr("Optimized"), tr("Restricted")).forEachIndexed { i, label ->
                    val y = pad + 30.dp.toPx() + i * 30.dp.toPx()
                    val selected = if (on) i == 0 else i == 1
                    drawCircle(MockInk.copy(alpha = 0.5f), 8.dp.toPx(), Offset(pad + 8.dp.toPx(), y + 8.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                    if (selected) drawCircle(if (i == 0) Calm else MockInk.copy(alpha = 0.6f), 4.5f.dp.toPx(), Offset(pad + 8.dp.toPx(), y + 8.dp.toPx()))
                    drawText(text, label, Offset(pad + 26.dp.toPx(), y), body.copy(color = MockInk, fontSize = 12.dp.toSp()))
                }
                Offset(pad + 8.dp.toPx(), pad + 38.dp.toPx())
            }
        }
        val start = Offset(size.width * 0.75f, size.height + 30.dp.toPx())
        val travel = (t / 0.35f).coerceIn(0f, 1f)
        val ease = 1 - (1 - travel) * (1 - travel)
        val finger = if (t < 0.6f) start + (target - start) * ease else target + Offset(0f, (t - 0.6f) * 60.dp.toPx())
        val alpha = if (t > 0.85f) (1 - (t - 0.85f) / 0.15f) else 1f
        drawCircle(SunriseCoral.copy(alpha = 0.25f * alpha * (0.5f + press)), 22.dp.toPx() * (1 + press * 0.4f), finger)
        drawCircle(SunriseCoral.copy(alpha = alpha), 11.dp.toPx() * (1 - press * 0.15f), finger)
        if (abs(press) > 0f) drawCircle(Color.White.copy(alpha = 0.6f * press), 5.dp.toPx(), finger)
    }
}

@Composable
private fun AllSet() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Rounded.CheckCircle, null, tint = Calm, modifier = Modifier.size(84.dp))
        Text(tr("All set. Your alarms will ring."), style = MaterialTheme.typography.titleMedium, color = Paper, modifier = Modifier.padding(top = Space.sm))
    }
}
