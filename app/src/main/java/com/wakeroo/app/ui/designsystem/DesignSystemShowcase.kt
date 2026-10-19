package com.wakeroo.app.ui.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wakeroo.app.ui.brand.AnimatedWakerooMark
import com.wakeroo.app.ui.brand.Wordmark
import com.wakeroo.app.ui.theme.Alert
import com.wakeroo.app.ui.theme.Calm
import com.wakeroo.app.ui.theme.ClockType
import com.wakeroo.app.ui.theme.DawnLavender
import com.wakeroo.app.ui.theme.MorningGold
import com.wakeroo.app.ui.theme.Night
import com.wakeroo.app.ui.theme.NightSurface
import com.wakeroo.app.ui.theme.NightSurfaceHigh
import com.wakeroo.app.ui.theme.Paper
import com.wakeroo.app.ui.theme.PaperMuted
import com.wakeroo.app.ui.theme.Sky
import com.wakeroo.app.ui.theme.Space
import com.wakeroo.app.ui.theme.SunriseCoral
import com.wakeroo.app.ui.theme.WakerooMotion
import com.wakeroo.app.ui.theme.WakerooTheme

/**
 * A living style guide: the brand intro followed by every design token in use.
 * Temporary first screen until the alarm list exists; keep it as a debug-only screen later.
 */
@Composable
fun DesignSystemShowcase(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .background(Sky.Night.brush)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = Space.xl),
    ) {
        BrandIntro()
        Section("Colour") { Palette() }
        Section("Skies") { Skies() }
        Section("Type") { TypeScale() }
        Section("Controls") { Controls() }
        Section("Alarm card") { SampleAlarmCard() }
        Spacer(Modifier.height(Space.hero))
    }
}

@Composable
private fun BrandIntro() {
    // Tapping replays the intro; a new key restarts the animation.
    var replay by remember { mutableIntStateOf(0) }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = Space.hero, bottom = Space.xl)
            .clickable { replay++ },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        key(replay) { AnimatedWakerooMark(Modifier.size(200.dp)) }
        Spacer(Modifier.height(Space.lg))
        Wordmark()
        Text(
            "Bounce out of bed.",
            style = MaterialTheme.typography.bodyLarge,
            color = DawnLavender,
            modifier = Modifier.padding(top = Space.xs),
        )
        Text(
            "Tap to replay",
            style = MaterialTheme.typography.labelSmall,
            color = PaperMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Space.lg),
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = PaperMuted,
        modifier = Modifier.padding(top = Space.xxl, bottom = Space.md),
    )
    content()
}

@Composable
private fun Palette() {
    val swatches = listOf(
        "Night" to Night, "Surface" to NightSurface, "Raised" to NightSurfaceHigh,
        "Coral" to SunriseCoral, "Lavender" to DawnLavender, "Gold" to MorningGold,
        "Paper" to Paper, "Alert" to Alert, "Calm" to Calm,
    )
    swatches.chunked(3).forEach { row ->
        Row(Modifier.fillMaxWidth().padding(bottom = Space.md), horizontalArrangement = Arrangement.spacedBy(Space.md)) {
            row.forEach { (name, color) ->
                Column(Modifier.weight(1f)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.6f)
                            .clip(MaterialTheme.shapes.medium)
                            .background(color)
                            .border(1.dp, Color.White.copy(alpha = 0.08f), MaterialTheme.shapes.medium),
                    )
                    Text(name, style = MaterialTheme.typography.labelMedium, color = Paper, modifier = Modifier.padding(top = Space.xs))
                }
            }
        }
    }
}

@Composable
private fun Skies() {
    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
        Sky.entries.forEach { sky ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(sky.brush),
                )
                Text(sky.name, style = MaterialTheme.typography.labelSmall, color = PaperMuted, modifier = Modifier.padding(top = Space.xs))
            }
        }
    }
}

@Composable
private fun TypeScale() {
    Text("6:30", style = ClockType.hero, color = Paper)
    Text("Display · Outfit", style = MaterialTheme.typography.displaySmall, color = Paper)
    Text("Headline · Outfit", style = MaterialTheme.typography.headlineMedium, color = Paper)
    Text("Title · Inter SemiBold", style = MaterialTheme.typography.titleMedium, color = Paper, modifier = Modifier.padding(top = Space.sm))
    Text(
        "Body · Inter. Your alarm rings in 7 hours 20 minutes. Put the QR code in another room tonight.",
        style = MaterialTheme.typography.bodyLarge,
        color = PaperMuted,
        modifier = Modifier.padding(top = Space.xs),
    )
}

@Composable
private fun Controls() {
    val haptics = LocalHapticFeedback.current
    var on by remember { mutableStateOf(true) }
    Button(
        onClick = { haptics.performHapticFeedback(HapticFeedbackType.Confirm) },
        modifier = Modifier.fillMaxWidth().height(Space.ringTouch),
        colors = ButtonDefaults.buttonColors(containerColor = SunriseCoral, contentColor = Night),
    ) { Text("Start mission", style = MaterialTheme.typography.labelLarge) }
    Spacer(Modifier.height(Space.md))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
        OutlinedButton(onClick = {}) { Text("Snooze 9 min") }
        TextButton(onClick = {}) { Text("Emergency", color = PaperMuted) }
        Spacer(Modifier.weight(1f))
        Switch(
            checked = on,
            onCheckedChange = {
                on = it
                haptics.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
            },
            colors = SwitchDefaults.colors(checkedTrackColor = SunriseCoral, checkedThumbColor = Night),
        )
    }
}

@Composable
private fun SampleAlarmCard() {
    var enabled by remember { mutableStateOf(true) }
    val surface by animateColorAsState(if (enabled) NightSurface else Night, WakerooMotion.calm(), label = "card")
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(surface)
            .border(1.dp, Color.White.copy(alpha = 0.06f), MaterialTheme.shapes.large)
            .padding(Space.xl),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("6:30", style = ClockType.card, color = if (enabled) Paper else PaperMuted)
                Text("Gym before class", style = MaterialTheme.typography.bodyMedium, color = PaperMuted)
            }
            Switch(
                checked = enabled,
                onCheckedChange = { enabled = it },
                colors = SwitchDefaults.colors(checkedTrackColor = SunriseCoral, checkedThumbColor = Night),
            )
        }
        Spacer(Modifier.height(Space.lg))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { i, d ->
                val active = i < 5
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (active) DawnLavender.copy(alpha = 0.18f) else Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) { Text(d, style = MaterialTheme.typography.labelSmall, color = if (active) DawnLavender else PaperMuted) }
            }
            Spacer(Modifier.width(Space.sm))
            Text("QR · Math", style = MaterialTheme.typography.labelMedium, color = MorningGold, modifier = Modifier.align(Alignment.CenterVertically))
        }
    }
}

@Preview(heightDp = 1800)
@Composable
private fun ShowcasePreview() {
    WakerooTheme { DesignSystemShowcase() }
}
