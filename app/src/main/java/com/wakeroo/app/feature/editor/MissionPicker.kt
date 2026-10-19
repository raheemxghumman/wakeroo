package com.wakeroo.app.feature.editor

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wakeroo.app.camera.hasCameraPermission
import com.wakeroo.app.data.model.MissionConfig
import com.wakeroo.app.data.model.MissionType
import com.wakeroo.app.missions.MathMission
import com.wakeroo.app.missions.ShakeMission
import com.wakeroo.app.missions.TypingMission
import com.wakeroo.app.ui.components.Chip
import com.wakeroo.app.ui.components.ChipRow
import com.wakeroo.app.ui.components.SkyBackground
import com.wakeroo.app.ui.components.Stepper
import com.wakeroo.app.ui.components.blurb
import com.wakeroo.app.ui.components.icon
import com.wakeroo.app.ui.components.press3d
import com.wakeroo.app.ui.theme.Calm
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionPickerSheet(existing: MissionConfig?, onDismiss: () -> Unit, onSave: (MissionConfig) -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var config by remember { mutableStateOf(existing) }
    var trying by remember { mutableStateOf(false) }
    var settingUpSpot by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) settingUpSpot = true
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = Night, dragHandle = null) {
        AnimatedContent(
            targetState = config?.type,
            transitionSpec = {
                if (targetState != null) (slideInHorizontally { it / 2 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 2 } + fadeOut())
                else (slideInHorizontally { -it / 2 } + fadeIn()) togetherWith (slideOutHorizontally { it / 2 } + fadeOut())
            },
            label = "picker",
        ) { type ->
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(Space.xl)) {
                if (type == null) {
                    Text("Pick a mission", style = MaterialTheme.typography.headlineMedium, color = Paper)
                    Text(
                        "Missions in another room work best: you have to get out of bed.",
                        style = MaterialTheme.typography.bodyMedium, color = PaperMuted, modifier = Modifier.padding(top = Space.xs, bottom = Space.lg),
                    )
                    MissionType.entries.chunked(2).forEach { pair ->
                        Row(Modifier.fillMaxWidth().padding(bottom = Space.md), horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                            pair.forEach { t ->
                                MissionTile(t, Modifier.weight(1f)) { config = MissionConfig(t) }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                } else {
                    val c = config!!
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack, "Back to missions", tint = Paper,
                            modifier = Modifier.clip(CircleShape).clickable { config = null }.padding(Space.sm),
                        )
                        Spacer(Modifier.width(Space.sm))
                        Text(c.type.title, style = MaterialTheme.typography.headlineMedium, color = Paper)
                    }
                    Text(c.type.blurb, style = MaterialTheme.typography.bodyMedium, color = PaperMuted, modifier = Modifier.padding(vertical = Space.md))
                    MissionConfigFields(c, onChange = { config = it }, onSetUpSpot = {
                        if (context.hasCameraPermission()) settingUpSpot = true else cameraPermission.launch(Manifest.permission.CAMERA)
                    })
                    Spacer(Modifier.height(Space.xl))
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                        if (c.type in setOf(MissionType.Math, MissionType.Typing, MissionType.Shake)) {
                            PillButton("Try it now", Modifier.weight(1f), outlined = true, icon = Icons.Rounded.PlayArrow) { trying = true }
                        }
                        val ready = when (c.type) {
                            MissionType.Scan -> c.codeValue != null
                            MissionType.Photo -> c.photoHash != null
                            else -> true
                        }
                        PillButton(if (ready) "Add mission" else "Set up first", Modifier.weight(1f), enabled = ready) { onSave(c) }
                    }
                }
            }
        }
    }

    if (trying) config?.let { TryMissionDialog(it) { trying = false } }
    if (settingUpSpot) config?.let { c ->
        SpotSetupDialog(
            type = c.type,
            onDismiss = { settingUpSpot = false },
            onCode = { value, label -> config = c.copy(codeValue = value, codeLabel = label); settingUpSpot = false },
            onPhoto = { hash, labels, path -> config = c.copy(photoHash = hash, photoLabels = labels, photoPath = path); settingUpSpot = false },
        )
    }
}

@Composable
private fun MissionTile(type: MissionType, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .height(150.dp)
            .press3d(maxTilt = 12f)
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(NightSurfaceHigh, NightSurface)))
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(26.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(Space.lg),
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(MorningGold.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Icon(type.icon, null, tint = MorningGold)
        }
        Spacer(Modifier.weight(1f))
        Text(type.title, style = MaterialTheme.typography.titleMedium, color = Paper)
        if (type.worksInAnotherRoom) {
            Text("Gets you out of bed", style = MaterialTheme.typography.labelSmall, color = DawnLavender)
        } else {
            Text(type.blurb, style = MaterialTheme.typography.labelSmall, color = PaperMuted, maxLines = 2)
        }
    }
}

@Composable
private fun MissionConfigFields(c: MissionConfig, onChange: (MissionConfig) -> Unit, onSetUpSpot: () -> Unit) {
    when (c.type) {
        MissionType.Math -> {
            DifficultyChips(c.difficulty) { onChange(c.copy(difficulty = it)) }
            Spacer(Modifier.height(Space.md))
            Stepper("Problems", "${c.count}", { onChange(c.copy(count = (c.count - 1).coerceAtLeast(1))) }, { onChange(c.copy(count = (c.count + 1).coerceAtMost(10))) }, c.count > 1, c.count < 10)
            Text(
                when (c.difficulty) { 1 -> "Like 24 + 7"; 2 -> "Like 17 × 6 + 38"; else -> "Like 34 × 11 + 245" },
                style = MaterialTheme.typography.bodySmall, color = PaperMuted, modifier = Modifier.padding(top = Space.sm),
            )
        }
        MissionType.Typing -> Stepper("Phrases", "${c.count}", { onChange(c.copy(count = (c.count - 1).coerceAtLeast(1))) }, { onChange(c.copy(count = (c.count + 1).coerceAtMost(5))) }, c.count > 1, c.count < 5)
        MissionType.Shake -> Stepper("Shakes", "${c.count}", { onChange(c.copy(count = (c.count - 10).coerceAtLeast(10))) }, { onChange(c.copy(count = (c.count + 10).coerceAtMost(150))) }, c.count > 10, c.count < 150)
        MissionType.Scan -> SpotStatus(
            done = c.codeValue != null,
            doneText = "Registered: the code on your ${c.codeLabel ?: "item"}",
            todo = "Pick something in another room with a barcode or QR code (toothpaste, coffee, a book) and scan it now.",
            action = if (c.codeValue != null) "Scan a different code" else "Scan your code",
            onClick = onSetUpSpot,
        )
        MissionType.Photo -> {
            SpotStatus(
                done = c.photoHash != null,
                doneText = "Spot saved. At alarm time, point the camera at the same view.",
                todo = "Take a photo of a spot away from your bed, like the bathroom sink or the kettle.",
                action = if (c.photoHash != null) "Retake photo" else "Take the photo",
                onClick = onSetUpSpot,
            )
            Spacer(Modifier.height(Space.md))
            Text("How close the view must match", style = MaterialTheme.typography.bodyMedium, color = Paper)
            Spacer(Modifier.height(Space.sm))
            DifficultyChips(c.difficulty, names = listOf("Relaxed", "Normal", "Strict")) { onChange(c.copy(difficulty = it)) }
        }
    }
}

@Composable
private fun DifficultyChips(value: Int, names: List<String> = listOf("Easy", "Medium", "Hard"), onChange: (Int) -> Unit) {
    ChipRow { names.forEachIndexed { i, n -> Chip(n, value == i + 1, { onChange(i + 1) }) } }
}

@Composable
private fun SpotStatus(done: Boolean, doneText: String, todo: String, action: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(NightSurface).padding(Space.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (done) {
            Icon(Icons.Rounded.CheckCircle, null, tint = Calm)
            Spacer(Modifier.width(Space.md))
        }
        Text(if (done) doneText else todo, style = MaterialTheme.typography.bodyMedium, color = Paper, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(Space.md))
    PillButton(action, Modifier.fillMaxWidth(), outlined = done, onClick = onClick)
}

@Composable
fun PillButton(
    text: String,
    modifier: Modifier = Modifier,
    outlined: Boolean = false,
    enabled: Boolean = true,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    dark: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .height(56.dp)
            .press3d()
            .clip(CircleShape)
            .then(
                if (outlined) Modifier.border(1.5.dp, Paper.copy(alpha = 0.25f), CircleShape)
                else Modifier.background(if (!enabled) Paper.copy(alpha = 0.1f) else if (dark) Night else SunriseCoral),
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = Space.xl),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (outlined) Paper else Night, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(Space.sm))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = if (outlined || dark) Paper else if (enabled) Night else PaperMuted)
    }
}

/** Run a mission for real, with no alarm attached: confidence before trusting it at 6 AM. */
@Composable
private fun TryMissionDialog(config: MissionConfig, onClose: () -> Unit) {
    var done by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        SkyBackground(Sky.Night) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(Space.xl)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Practice run", style = MaterialTheme.typography.titleMedium, color = DawnLavender, modifier = Modifier.weight(1f))
                    Icon(Icons.Rounded.Close, "Close", tint = Paper, modifier = Modifier.clip(CircleShape).clickable(onClick = onClose).padding(Space.sm))
                }
                Spacer(Modifier.height(Space.lg))
                AnimatedContent(done, transitionSpec = { scaleIn() + fadeIn() togetherWith fadeOut() }, label = "try") { finished ->
                    if (finished) {
                        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(Icons.Rounded.CheckCircle, null, tint = MorningGold, modifier = Modifier.size(96.dp))
                            Text("That's how it works", style = MaterialTheme.typography.headlineMedium, color = Paper, modifier = Modifier.padding(top = Space.lg))
                            Text("At alarm time the sound stops once you finish.", style = MaterialTheme.typography.bodyMedium, color = PaperMuted)
                            Spacer(Modifier.height(Space.xl))
                            PillButton("Done", onClick = onClose)
                        }
                    } else {
                        Box(Modifier.fillMaxSize()) {
                            when (config.type) {
                                MissionType.Math -> MathMission(config, {}, {}) { done = true }
                                MissionType.Typing -> TypingMission(config, {}, {}) { done = true }
                                MissionType.Shake -> ShakeMission(config, {}, {}) { done = true }
                                else -> Unit
                            }
                        }
                    }
                }
            }
        }
    }
}

