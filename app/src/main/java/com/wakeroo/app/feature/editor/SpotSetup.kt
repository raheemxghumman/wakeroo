package com.wakeroo.app.feature.editor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wakeroo.app.camera.BarcodeReader
import com.wakeroo.app.camera.SceneReader
import com.wakeroo.app.camera.saveThumbnail
import com.wakeroo.app.data.model.MissionType
import com.wakeroo.app.missions.NfcListener
import com.wakeroo.app.missions.NfcState
import com.wakeroo.app.missions.NfcWaves
import com.wakeroo.app.missions.Viewfinder
import com.wakeroo.app.missions.nfcSimulated
import com.wakeroo.app.missions.nfcState
import com.wakeroo.app.ui.theme.MorningGold
import androidx.lifecycle.compose.LifecycleResumeEffect
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.padding
import com.wakeroo.app.ui.components.Chip
import com.wakeroo.app.ui.components.ChipRow
import com.wakeroo.app.ui.components.SkyBackground
import com.wakeroo.app.ui.theme.DawnLavender
import com.wakeroo.app.ui.theme.Paper
import com.wakeroo.app.ui.theme.PaperMuted
import com.wakeroo.app.ui.theme.Sky
import com.wakeroo.app.ui.theme.Space
import com.wakeroo.app.ui.theme.WakerooTheme
import kotlinx.coroutines.launch

private val CODE_PLACES get() = listOf("toothbrush", "toothpaste", "coffee jar", "kettle", "fridge", "meds", "desk", "bathroom mirror")

/** Register the QR/barcode or the photo spot a mission will ask for at alarm time. */
@Composable
fun SpotSetupDialog(
    type: MissionType,
    onDismiss: () -> Unit,
    onCode: (value: String, label: String) -> Unit,
    onPhoto: (hash: Long, labels: List<String>, path: String) -> Unit,
    onNfc: (tagId: String, label: String) -> Unit = { _, _ -> },
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        WakerooTheme(dark = true) { SkyBackground(Sky.Night) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(Space.xl)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        when (type) { MissionType.Scan -> "Register your code"; MissionType.Nfc -> "Register your sticker"; else -> "Save your photo spot" },
                        style = MaterialTheme.typography.headlineSmall, color = Paper, modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.Rounded.Close, "Close", tint = Paper, modifier = Modifier.clip(CircleShape).clickable(onClick = onDismiss).padding(Space.sm))
                }
                Spacer(Modifier.height(Space.lg))
                when (type) {
                    MissionType.Scan -> CodeSetup(onCode)
                    MissionType.Nfc -> NfcSetup(onNfc)
                    else -> PhotoSetup(onPhoto)
                }
            }
        }
    }
    }
}

@Composable
private fun CodeSetup(onCode: (String, String) -> Unit) {
    val haptics = LocalHapticFeedback.current
    val reader = remember { BarcodeReader() }
    DisposableEffect(Unit) { onDispose { reader.close() } }
    var found by remember { mutableStateOf<String?>(null) }
    var torch by remember { mutableStateOf(false) }

    AnimatedContent(found, transitionSpec = { (slideInVertically { it / 3 } + fadeIn()) togetherWith fadeOut() }, label = "code") { value ->
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (value == null) {
                Text(
                    "Walk to another room and scan any barcode or QR code. You'll need to scan the same one to stop the alarm.",
                    style = MaterialTheme.typography.bodyMedium, color = PaperMuted, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Space.lg))
                Viewfinder(matched = false, torch = torch, onTorch = { torch = !torch }) { frame ->
                    if (found != null) return@Viewfinder frame.close()
                    reader.read(frame) { v ->
                        if (v != null && found == null) {
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            found = v
                        }
                    }
                }
            } else {
                Text("Got it!", style = MaterialTheme.typography.displaySmall, color = Paper)
                Text("Where is this code?", style = MaterialTheme.typography.titleMedium, color = DawnLavender, modifier = Modifier.padding(vertical = Space.lg))
                ChipRow { CODE_PLACES.forEach { place -> Chip(place, false, { onCode(value, place) }) } }
                Spacer(Modifier.height(Space.lg))
                Text("Tip: a code you can't reach from bed works best.", style = MaterialTheme.typography.bodySmall, color = PaperMuted)
            }
        }
    }
}

@Composable
private fun PhotoSetup(onPhoto: (Long, List<String>, String) -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val reader = remember { SceneReader() }
    DisposableEffect(Unit) { onDispose { reader.close() } }
    var capture by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var torch by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Text(
            "Stand where you'll be at alarm time and frame something that doesn't move: the sink, the kettle, a poster.",
            style = MaterialTheme.typography.bodyMedium, color = PaperMuted, textAlign = TextAlign.Center,
        )
        Viewfinder(matched = false, torch = torch, onTorch = { torch = !torch }) { frame ->
            if (!capture || busy) return@Viewfinder frame.close()
            busy = true
            scope.launch {
                val bitmap = runCatching { frame.toBitmap() }.getOrNull()
                val print = reader.read(frame)
                if (print != null && bitmap != null) {
                    val path = saveThumbnail(context, bitmap, "spot_${System.currentTimeMillis()}")
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onPhoto(print.hash, print.labels, path)
                } else {
                    busy = false; capture = false
                }
            }
        }
        PillButton(if (busy) "Saving…" else "Use this view", Modifier.fillMaxWidth(), enabled = !busy) { capture = true }
    }
}

private val STICKER_PLACES get() = listOf("bathroom mirror", "kettle", "coffee machine", "front door", "fridge", "desk", "toothbrush")

@Composable
private fun NfcSetup(onNfc: (String, String) -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var found by remember { mutableStateOf<String?>(null) }
    var state by remember { mutableStateOf(context.nfcState()) }
    LifecycleResumeEffect(Unit) {
        state = context.nfcState()
        onPauseOrDispose { }
    }
    NfcListener { id ->
        if (found == null) {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            found = id
        }
    }
    AnimatedContent(found, transitionSpec = { (slideInVertically { it / 3 } + fadeIn()) togetherWith fadeOut() }, label = "nfc") { value ->
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (value == null) {
                Text(
                    "Hold the top of your phone against the sticker. Put it somewhere you can't reach from bed.",
                    style = MaterialTheme.typography.bodyMedium, color = PaperMuted, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Space.xl))
                NfcWaves(
                    matched = false,
                    onSimulate = if (nfcSimulated && state != NfcState.Ready) { { found = "SIM" + System.currentTimeMillis() % 10000 } } else null,
                )
                Spacer(Modifier.height(Space.xl))
                if (state == NfcState.Off) {
                    Text("NFC is switched off", style = MaterialTheme.typography.titleMedium, color = MorningGold)
                    Spacer(Modifier.height(Space.sm))
                    PillButton("Turn on NFC", Modifier.fillMaxWidth()) {
                        runCatching { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    }
                } else if (nfcSimulated && state != NfcState.Ready) {
                    Text("No NFC here. Tap the sticker to simulate it (debug build).", style = MaterialTheme.typography.bodySmall, color = PaperMuted)
                }
            } else {
                NfcWaves(matched = true)
                Text("Sticker locked in", style = MaterialTheme.typography.headlineSmall, color = Paper, modifier = Modifier.padding(top = Space.lg))
                Text("Where is it?", style = MaterialTheme.typography.titleMedium, color = DawnLavender, modifier = Modifier.padding(vertical = Space.lg))
                ChipRow { STICKER_PLACES.forEach { place -> Chip(place, false, { onNfc(value, place) }) } }
            }
        }
    }
}
