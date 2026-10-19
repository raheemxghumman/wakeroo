package com.wakeroo.app.feature.editor

import android.Manifest
import android.os.Build
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.wakeroo.app.camera.hasCameraPermission
import com.wakeroo.app.data.model.MissionConfig
import com.wakeroo.app.data.model.MissionType
import com.wakeroo.app.missions.HuntMission
import com.wakeroo.app.missions.MathMission
import com.wakeroo.app.missions.MemoryMission
import com.wakeroo.app.missions.MissionDemo
import com.wakeroo.app.missions.NfcMission
import com.wakeroo.app.missions.NfcState
import com.wakeroo.app.missions.PlanMission
import com.wakeroo.app.missions.ShakeMission
import com.wakeroo.app.missions.StepsMission
import com.wakeroo.app.missions.TypingMission
import com.wakeroo.app.missions.canCountSteps
import com.wakeroo.app.missions.hasMotionPermission
import com.wakeroo.app.missions.nfcSimulated
import com.wakeroo.app.missions.nfcState
import com.wakeroo.app.ui.components.Chip
import com.wakeroo.app.ui.components.ChipRow
import com.wakeroo.app.ui.components.SkyBackground
import com.wakeroo.app.ui.components.Stepper
import com.wakeroo.app.ui.components.blurb
import com.wakeroo.app.ui.components.icon
import com.wakeroo.app.ui.components.press3d
import com.wakeroo.app.ui.components.tagline
import com.wakeroo.app.ui.theme.Calm
import com.wakeroo.app.ui.theme.DawnLavender
import com.wakeroo.app.ui.theme.Hairline
import com.wakeroo.app.ui.theme.MorningGold
import com.wakeroo.app.ui.theme.Night
import com.wakeroo.app.ui.theme.NightSurface
import com.wakeroo.app.ui.theme.NightSurfaceHigh
import com.wakeroo.app.ui.theme.Paper
import com.wakeroo.app.ui.theme.PaperMuted
import com.wakeroo.app.ui.theme.Sky
import com.wakeroo.app.ui.theme.Space
import com.wakeroo.app.ui.theme.SunriseCoral

/** Missions the user can run for practice right in the picker. */
private val TRYABLE = setOf(
    MissionType.Math, MissionType.Typing, MissionType.Shake, MissionType.Steps, MissionType.Memory, MissionType.Plan, MissionType.Hunt,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionPickerSheet(
    existing: MissionConfig?,
    onDismiss: () -> Unit,
    onSave: (MissionConfig) -> Unit,
    allowSetupLater: Boolean = false,
    saveLabel: String = "Add mission",
) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var config by remember { mutableStateOf(existing) }
    var trying by remember { mutableStateOf(false) }
    var settingUp by remember { mutableStateOf(false) }
    // What to do once the camera permission dialog says yes.
    var afterCamera by remember { mutableStateOf<(() -> Unit)?>(null) }
    val context = LocalContext.current
    // Permissions can change in system settings while the sheet is open.
    var permissionTick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        permissionTick++
        onPauseOrDispose { }
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionTick++
        if (granted) afterCamera?.invoke()
        afterCamera = null
    }
    val motionPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permissionTick++ }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = Night, dragHandle = null) {
        AnimatedContent(
            targetState = config?.type,
            transitionSpec = {
                if (targetState != null) (slideInHorizontally { it / 2 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 2 } + fadeOut())
                else (slideInHorizontally { -it / 2 } + fadeIn()) togetherWith (slideOutHorizontally { it / 2 } + fadeOut())
            },
            label = "picker",
        ) { type ->
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(Space.xl)) {
                if (type == null) {
                    Text("Pick a mission", style = MaterialTheme.typography.headlineMedium, color = Paper)
                    Text(
                        "Missions in another room work best: you have to get out of bed.",
                        style = MaterialTheme.typography.bodyMedium, color = PaperMuted, modifier = Modifier.padding(top = Space.xs, bottom = Space.lg),
                    )
                    val nfc = context.nfcState()
                    MissionType.entries.chunked(2).forEach { pair ->
                        Row(Modifier.fillMaxWidth().padding(bottom = Space.md), horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                            pair.forEach { t ->
                                val unavailable = when (t) {
                                    MissionType.Nfc -> if (nfc == NfcState.Missing && !nfcSimulated) "No NFC on this phone" else null
                                    MissionType.Steps -> if (!context.canCountSteps()) "No motion sensor" else null
                                    else -> null
                                }
                                MissionTile(t, Modifier.weight(1f), unavailable) {
                                    config = if (t == MissionType.Hunt) MissionConfig(t, difficulty = 1) else MissionConfig(t)
                                }
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
                    Box(
                        Modifier.padding(top = Space.md).fillMaxWidth().height(170.dp).clip(RoundedCornerShape(26.dp))
                            .background(Brush.linearGradient(listOf(NightSurfaceHigh, NightSurface))),
                        contentAlignment = Alignment.Center,
                    ) { MissionDemo(c.type, Modifier.fillMaxSize().padding(Space.md)) }
                    Text(c.type.blurb, style = MaterialTheme.typography.bodyMedium, color = PaperMuted, modifier = Modifier.padding(vertical = Space.md))
                    val motionOk = remember(permissionTick) { context.hasMotionPermission() }
                    val cameraOk = remember(permissionTick) { context.hasCameraPermission() }
                    MissionConfigFields(
                        c, onChange = { config = it },
                        motionGranted = motionOk,
                        onAskMotion = { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) motionPermission.launch(Manifest.permission.ACTIVITY_RECOGNITION) },
                        onSetUp = {
                            if (c.type == MissionType.Nfc || cameraOk) settingUp = true else {
                                afterCamera = { settingUp = true }
                                cameraPermission.launch(Manifest.permission.CAMERA)
                            }
                        },
                    )
                    Spacer(Modifier.height(Space.xl))
                    val ready = when (c.type) {
                        MissionType.Scan -> c.codeValue != null
                        MissionType.Photo -> c.photoHash != null
                        MissionType.Nfc -> c.nfcTagId != null
                        MissionType.Steps -> motionOk
                        else -> true
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                        if (c.type in TRYABLE && ready) {
                            PillButton("Try it now", Modifier.weight(1f), outlined = true, icon = Icons.Rounded.PlayArrow) {
                                if (c.type == MissionType.Hunt && !cameraOk) {
                                    afterCamera = { trying = true }
                                    cameraPermission.launch(Manifest.permission.CAMERA)
                                } else trying = true
                            }
                        }
                        if (ready || !allowSetupLater) PillButton(if (ready) saveLabel else "Set up first", Modifier.weight(1f), enabled = ready) {
                            if (c.type == MissionType.Hunt && !cameraOk) {
                                afterCamera = { onSave(c) }
                                cameraPermission.launch(Manifest.permission.CAMERA)
                            } else onSave(c)
                        }
                    }
                    if (!ready && allowSetupLater && c.type.needsSetupStep) {
                        Spacer(Modifier.height(Space.md))
                        PillButton("I'll set it up tonight", Modifier.fillMaxWidth(), outlined = true) { onSave(c.copy(needsSetup = true)) }
                        Text(
                            "Until it's set up, this alarm uses math instead.",
                            style = MaterialTheme.typography.bodySmall, color = PaperMuted, modifier = Modifier.padding(top = Space.sm),
                        )
                    }
                    if (c.type == MissionType.Hunt && !cameraOk) {
                        Text(
                            "Item hunt uses the camera only while the alarm screen is open.",
                            style = MaterialTheme.typography.bodySmall, color = PaperMuted, modifier = Modifier.padding(top = Space.sm),
                        )
                    }
                }
            }
        }
    }

    if (trying) config?.let { TryMissionDialog(it) { trying = false } }
    if (settingUp) config?.let { c ->
        SpotSetupDialog(
            type = c.type,
            onDismiss = { settingUp = false },
            onCode = { value, label -> config = c.copy(codeValue = value, codeLabel = label, needsSetup = false); settingUp = false },
            onPhoto = { hash, labels, path -> config = c.copy(photoHash = hash, photoLabels = labels, photoPath = path, needsSetup = false); settingUp = false },
            onNfc = { id, label -> config = c.copy(nfcTagId = id, nfcLabel = label, needsSetup = false); settingUp = false },
        )
    }
}

/** A mission card with its demo playing on a little stage at the top. */
@Composable
fun MissionTile(type: MissionType, modifier: Modifier, unavailable: String? = null, selected: Boolean = false, onClick: () -> Unit) {
    Column(
        modifier
            .height(178.dp)
            .press3d(maxTilt = 12f)
            .clip(RoundedCornerShape(26.dp))
            .background(if (selected) Brush.linearGradient(listOf(SunriseCoral.copy(alpha = 0.35f), MorningGold.copy(alpha = 0.15f))) else Brush.linearGradient(listOf(NightSurfaceHigh, NightSurface)))
            .border(if (selected) 2.dp else 1.dp, if (selected) SunriseCoral else Hairline, RoundedCornerShape(26.dp))
            .clickable(enabled = unavailable == null, role = Role.Button, onClick = onClick)
            .padding(Space.md),
    ) {
        Box(
            Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(18.dp)).background(Night.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) {
            MissionDemo(type, Modifier.fillMaxSize().padding(Space.xs), playing = unavailable == null)
            Box(
                Modifier.align(Alignment.TopStart).padding(6.dp).size(28.dp).clip(CircleShape).background(MorningGold.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) { Icon(type.icon, null, tint = MorningGold, modifier = Modifier.size(16.dp)) }
        }
        Spacer(Modifier.height(Space.sm))
        Text(type.title, style = MaterialTheme.typography.titleSmall, color = if (unavailable == null) Paper else PaperMuted, maxLines = 1)
        Text(
            unavailable ?: type.tagline,
            style = MaterialTheme.typography.labelSmall,
            color = if (unavailable != null) PaperMuted else if (type.worksInAnotherRoom) DawnLavender else PaperMuted,
            maxLines = 1,
        )
    }
}

@Composable
private fun MissionConfigFields(
    c: MissionConfig,
    onChange: (MissionConfig) -> Unit,
    motionGranted: Boolean,
    onAskMotion: () -> Unit,
    onSetUp: () -> Unit,
) {
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
        MissionType.Steps -> {
            Stepper("Steps", "${c.count}", { onChange(c.copy(count = (c.count - 10).coerceAtLeast(20))) }, { onChange(c.copy(count = (c.count + 10).coerceAtMost(200))) }, c.count > 20, c.count < 200)
            Text(
                "Stand still for 10 seconds and the count drains, so you have to keep moving.",
                style = MaterialTheme.typography.bodySmall, color = PaperMuted, modifier = Modifier.padding(top = Space.sm),
            )
            if (!motionGranted) {
                Spacer(Modifier.height(Space.md))
                SpotStatus(
                    done = false, doneText = "",
                    todo = "Wakeroo needs motion access to count your steps. It's only used while this mission runs.",
                    action = "Allow motion access", onClick = onAskMotion,
                )
            }
        }
        MissionType.Memory -> {
            DifficultyChips(
                when { c.gridSize >= 4 -> 3; c.count >= 4 -> 2; else -> 1 },
            ) { d ->
                onChange(when (d) { 1 -> c.copy(gridSize = 3, count = 3); 2 -> c.copy(gridSize = 3, count = 4); else -> c.copy(gridSize = 4, count = 5) })
            }
            Spacer(Modifier.height(Space.md))
            Stepper("Rounds", "${c.count}", { onChange(c.copy(count = (c.count - 1).coerceAtLeast(2))) }, { onChange(c.copy(count = (c.count + 1).coerceAtMost(8))) }, c.count > 2, c.count < 8)
            Text(
                "${c.gridSize}×${c.gridSize} grid. Each round lights one more tile.",
                style = MaterialTheme.typography.bodySmall, color = PaperMuted, modifier = Modifier.padding(top = Space.sm),
            )
        }
        MissionType.Scan -> SpotStatus(
            done = c.codeValue != null,
            doneText = "Registered: the code on your ${c.codeLabel ?: "item"}",
            todo = "Pick something in another room with a barcode or QR code (toothpaste, coffee, a book) and scan it now.",
            action = if (c.codeValue != null) "Scan a different code" else "Scan your code",
            onClick = onSetUp,
        )
        MissionType.Photo -> {
            SpotStatus(
                done = c.photoHash != null,
                doneText = "Spot saved. At alarm time, point the camera at the same view.",
                todo = "Take a photo of a spot away from your bed, like the bathroom sink or the kettle.",
                action = if (c.photoHash != null) "Retake photo" else "Take the photo",
                onClick = onSetUp,
            )
            Spacer(Modifier.height(Space.md))
            Text("How close the view must match", style = MaterialTheme.typography.bodyMedium, color = Paper)
            Spacer(Modifier.height(Space.sm))
            DifficultyChips(c.difficulty, names = listOf("Relaxed", "Normal", "Strict")) { onChange(c.copy(difficulty = it)) }
        }
        MissionType.Nfc -> SpotStatus(
            done = c.nfcTagId != null,
            doneText = "Registered: the sticker by the ${c.nfcLabel ?: "door"}",
            todo = "Stick an NFC tag (cheap NTAG stickers work) by the bathroom mirror or the kettle, then register it here.",
            action = if (c.nfcTagId != null) "Register a different sticker" else "Register your sticker",
            onClick = onSetUp,
        )
        MissionType.Plan -> Text(
            "No setup needed. At alarm time you type at least three words about your day. Your plan then shows on the Morning tab.",
            style = MaterialTheme.typography.bodyMedium, color = Paper,
        )
        MissionType.Hunt -> {
            DifficultyChips(c.difficulty, names = listOf("Everyday", "Varied", "Strict")) { onChange(c.copy(difficulty = it)) }
            Spacer(Modifier.height(Space.md))
            Stepper("Objects to find", "${c.count}", { onChange(c.copy(count = (c.count - 1).coerceAtLeast(1))) }, { onChange(c.copy(count = (c.count + 1).coerceAtMost(3))) }, c.count > 1, c.count < 3)
            Text(
                when (c.difficulty) {
                    1 -> "Things in almost every home: a cup, a chair, a plant, the sink."
                    2 -> "Any of 30 objects, from a jacket to a frying pan."
                    else -> "Any object, and the camera must be really sure."
                },
                style = MaterialTheme.typography.bodySmall, color = PaperMuted, modifier = Modifier.padding(top = Space.sm),
            )
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
        Text(
            text, style = MaterialTheme.typography.labelLarge,
            color = if (outlined) Paper else if (dark) Color(0xFFF5F3EE) else if (enabled) Night else PaperMuted,
        )
    }
}

/** Run a mission for real, with no alarm attached: confidence before trusting it at 6 AM. */
@Composable
fun TryMissionDialog(config: MissionConfig, onClose: () -> Unit) {
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
                            val finish = { done = true }
                            when (config.type) {
                                MissionType.Math -> MathMission(config, {}, {}, finish)
                                MissionType.Typing -> TypingMission(config, {}, {}, finish)
                                MissionType.Shake -> ShakeMission(config, {}, {}, finish)
                                MissionType.Steps -> StepsMission(config, {}, {}, finish)
                                MissionType.Memory -> MemoryMission(config, {}, {}, finish)
                                MissionType.Plan -> PlanMission(config, {}, {}, finish)
                                MissionType.Hunt -> HuntMission(config, {}, {}, finish)
                                MissionType.Nfc -> NfcMission(config, {}, {}, finish)
                                else -> Unit
                            }
                        }
                    }
                }
            }
        }
    }
}
