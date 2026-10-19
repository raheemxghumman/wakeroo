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
import com.wakeroo.app.missions.Viewfinder
import com.wakeroo.app.ui.components.Chip
import com.wakeroo.app.ui.components.ChipRow
import com.wakeroo.app.ui.components.SkyBackground
import com.wakeroo.app.ui.theme.DawnLavender
import com.wakeroo.app.ui.theme.Paper
import com.wakeroo.app.ui.theme.PaperMuted
import com.wakeroo.app.ui.theme.Sky
import com.wakeroo.app.ui.theme.Space
import kotlinx.coroutines.launch

private val CODE_PLACES = listOf("toothbrush", "toothpaste", "coffee jar", "kettle", "fridge", "meds", "desk", "bathroom mirror")

/** Register the QR/barcode or the photo spot a mission will ask for at alarm time. */
@Composable
fun SpotSetupDialog(
    type: MissionType,
    onDismiss: () -> Unit,
    onCode: (value: String, label: String) -> Unit,
    onPhoto: (hash: Long, labels: List<String>, path: String) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        SkyBackground(Sky.Night) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(Space.xl)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (type == MissionType.Scan) "Register your code" else "Save your photo spot",
                        style = MaterialTheme.typography.headlineSmall, color = Paper, modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.Rounded.Close, "Close", tint = Paper, modifier = Modifier.clip(CircleShape).clickable(onClick = onDismiss).padding(Space.sm))
                }
                Spacer(Modifier.height(Space.lg))
                if (type == MissionType.Scan) CodeSetup(onCode) else PhotoSetup(onPhoto)
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
