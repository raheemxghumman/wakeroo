package com.wakeroo.app.missions

import com.wakeroo.app.ui.i18n.tr
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wakeroo.app.camera.BarcodeReader
import com.wakeroo.app.camera.CameraPreview
import com.wakeroo.app.camera.SceneFingerprint
import com.wakeroo.app.camera.SceneReader
import com.wakeroo.app.data.model.MissionConfig
import com.wakeroo.app.ui.theme.Calm
import com.wakeroo.app.ui.theme.DawnLavender
import com.wakeroo.app.ui.theme.MorningGold
import com.wakeroo.app.ui.theme.Night
import com.wakeroo.app.ui.theme.Paper
import com.wakeroo.app.ui.theme.PaperMuted
import com.wakeroo.app.ui.theme.Space
import kotlinx.coroutines.launch

/** Rounded camera window with corner brackets that breathe while searching and snap on a match. */
@Composable
fun Viewfinder(matched: Boolean, torch: Boolean, onTorch: () -> Unit, onFrame: (androidx.camera.core.ImageProxy) -> Unit) {
    val breathe by rememberInfiniteTransition(label = "vf").animateFloat(
        0.88f, 1f, infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b",
    )
    val bracket by animateColorAsState(if (matched) Calm else Paper, label = "bracket")
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
            .clip(RoundedCornerShape(36.dp))
            .background(Night),
    ) {
        CameraPreview(Modifier.fillMaxSize(), torch = torch, onFrame = onFrame)
        Canvas(Modifier.fillMaxSize().padding(28.dp)) {
            val s = if (matched) 1f else breathe
            val w = size.width * s; val h = size.height * s
            val o = Offset((size.width - w) / 2, (size.height - h) / 2)
            val len = 44.dp.toPx(); val stroke = Stroke(6.dp.toPx(), cap = StrokeCap.Round)
            listOf(
                o to (Offset(len, 0f) to Offset(0f, len)),
                Offset(o.x + w, o.y) to (Offset(-len, 0f) to Offset(0f, len)),
                Offset(o.x, o.y + h) to (Offset(len, 0f) to Offset(0f, -len)),
                Offset(o.x + w, o.y + h) to (Offset(-len, 0f) to Offset(0f, -len)),
            ).forEach { (corner, arms) ->
                drawLine(bracket, corner, corner + arms.first, stroke.width, StrokeCap.Round)
                drawLine(bracket, corner, corner + arms.second, stroke.width, StrokeCap.Round)
            }
        }
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(Space.lg)
                .size(56.dp)
                .clip(CircleShape)
                .background(Night.copy(alpha = 0.6f))
                .clickable(onClick = onTorch),
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (torch) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff, tr("Torch"), tint = if (torch) MorningGold else Paper)
        }
    }
}

@Composable
fun ScanMission(config: MissionConfig, onActivity: () -> Unit, onFailedAttempt: () -> Unit, onDone: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val reader = remember { BarcodeReader() }
    DisposableEffect(Unit) { onDispose { reader.close() } }
    var matched by remember { mutableStateOf(false) }
    var torch by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf<String?>(null) }
    var lastWrong by remember { mutableLongStateOf(0L) }
    val done by rememberUpdatedState(onDone)

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            config.codeLabel?.let { tr("Scan the code on your {0}", it) } ?: tr("Scan your registered code"),
            style = MaterialTheme.typography.headlineSmall, color = Paper, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Space.lg))
        Viewfinder(matched, torch, onTorch = { torch = !torch }) { frame ->
            if (matched) return@Viewfinder frame.close()
            reader.read(frame) { value ->
                if (value == null) return@read
                if (config.codeValue == null || value == config.codeValue) {
                    matched = true
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    done()
                } else if (System.currentTimeMillis() - lastWrong > 2500) {
                    lastWrong = System.currentTimeMillis()
                    hint = tr("That's a different code. Find the one you registered.")
                    haptics.performHapticFeedback(HapticFeedbackType.Reject)
                    onFailedAttempt()
                }
                onActivity()
            }
        }
        Spacer(Modifier.height(Space.lg))
        Text(hint ?: tr("Hold it steady inside the frame"), style = MaterialTheme.typography.bodyLarge, color = if (hint != null) MorningGold else PaperMuted, textAlign = TextAlign.Center)
    }
}

@Composable
fun PhotoMission(config: MissionConfig, onActivity: () -> Unit, onDone: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val reader = remember { SceneReader() }
    DisposableEffect(Unit) { onDispose { reader.close() } }
    var matched by remember { mutableStateOf(false) }
    var torch by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var lastCheck by remember { mutableLongStateOf(0L) }
    var closeness by remember { mutableFloatStateOf(0f) }
    val reference = remember(config) { config.photoHash?.let { SceneFingerprint(config.photoLabels, it) } }
    val done by rememberUpdatedState(onDone)

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(tr("Point at your photo spot"), style = MaterialTheme.typography.headlineSmall, color = Paper)
        Text(tr("Same angle as when you set it up"), style = MaterialTheme.typography.bodyMedium, color = DawnLavender)
        Spacer(Modifier.height(Space.lg))
        Viewfinder(matched, torch, onTorch = { torch = !torch }) { frame ->
            val now = System.currentTimeMillis()
            if (matched || busy || now - lastCheck < 450 || reference == null) return@Viewfinder frame.close()
            busy = true; lastCheck = now
            scope.launch {
                val live = reader.read(frame)
                busy = false
                if (live != null) {
                    closeness = (1f - ImageHash.distance(reference.hash, live.hash) / 40f).coerceIn(0f, 1f)
                    onActivity()
                    if (SceneReader.matches(reference, live, config.difficulty)) {
                        matched = true
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        done()
                    }
                }
            }
        }
        Spacer(Modifier.height(Space.lg))
        // A warmth meter: gets fuller the closer the view is to the saved spot.
        Box(Modifier.fillMaxWidth(0.7f).height(10.dp).clip(CircleShape).background(Paper.copy(alpha = 0.15f))) {
            Box(Modifier.fillMaxWidth(closeness).height(10.dp).clip(CircleShape).background(if (matched) Calm else MorningGold))
        }
        Text(
            when { reference == null -> tr("No reference photo saved, use the backup mission"); closeness > 0.7f -> tr("Almost there, hold still"); closeness > 0.45f -> tr("Getting warmer"); else -> tr("Look around for your spot") },
            style = MaterialTheme.typography.bodyMedium, color = PaperMuted, modifier = Modifier.padding(top = Space.sm),
        )
    }
}

