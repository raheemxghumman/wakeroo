package com.wakeroo.app.missions

import com.wakeroo.app.ui.theme.MorningGoldFixed
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.nfc.NfcAdapter
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.wakeroo.app.BuildConfig
import com.wakeroo.app.data.model.MissionConfig
import com.wakeroo.app.ui.theme.Calm
import com.wakeroo.app.ui.theme.DawnLavender
import com.wakeroo.app.ui.theme.MorningGold
import com.wakeroo.app.ui.theme.Night
import com.wakeroo.app.ui.theme.Paper
import com.wakeroo.app.ui.theme.PaperMuted
import com.wakeroo.app.ui.theme.Space
import com.wakeroo.app.ui.theme.SunriseCoral
import kotlinx.coroutines.launch

/** NFC hardware state for the mission picker and the ring screen. */
enum class NfcState { Missing, Off, Ready }

fun Context.nfcState(): NfcState {
    val adapter = NfcAdapter.getDefaultAdapter(this) ?: return NfcState.Missing
    return if (adapter.isEnabled) NfcState.Ready else NfcState.Off
}

/** Debug builds show NFC on the emulator (which has none) with a tap-to-simulate sticker. */
val nfcSimulated: Boolean get() = BuildConfig.DEBUG

fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

fun ByteArray.toHex(): String = joinToString("") { "%02X".format(it) }

/**
 * Listens for NFC tags while the screen is resumed, using reader mode so Android doesn't open
 * another app for the sticker. [onTag] gets the tag's UID in hex, on the main thread.
 */
@Composable
fun NfcListener(onTag: (String) -> Unit) {
    val context = LocalContext.current
    val latest by rememberUpdatedState(onTag)
    LifecycleResumeEffect(Unit) {
        val activity = context.findActivity()
        val adapter = NfcAdapter.getDefaultAdapter(context)
        if (activity != null && adapter != null) {
            val flags = NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
            runCatching {
                adapter.enableReaderMode(activity, { tag ->
                    val id = tag.id?.toHex() ?: return@enableReaderMode
                    activity.runOnUiThread { latest(id) }
                }, flags, null)
            }
        }
        onPauseOrDispose { if (activity != null) runCatching { adapter?.disableReaderMode(activity) } }
    }
}

/** Expanding rings from the phone toward the sticker; they snap gold when the tag is read. */
@Composable
fun NfcWaves(matched: Boolean, modifier: Modifier = Modifier, onSimulate: (() -> Unit)? = null) {
    val wave by rememberInfiniteTransition(label = "nfc").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart), label = "w",
    )
    val ring = if (matched) Calm else DawnLavender
    val core = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    Box(
        modifier
            .size(260.dp)
            .then(if (onSimulate != null) Modifier.clickable(remember { MutableInteractionSource() }, null) {
                scope.launch { core.snapTo(0.9f); core.animateTo(1f, spring(0.4f, 500f)) }
                onSimulate()
            } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2
            for (i in 0 until 3) {
                val p = ((wave + i / 3f) % 1f)
                drawCircle(ring.copy(alpha = (1 - p) * 0.5f), r * (0.35f + 0.65f * p), style = Stroke(3.dp.toPx()))
            }
            // The sticker: a rounded square with an antenna coil.
            val s = r * 0.62f
            val tl = Offset(center.x - s / 2, center.y - s / 2)
            drawRoundRect(
                Brush.linearGradient(if (matched) listOf(Calm, Color(0xFF2E9C77)) else listOf(MorningGoldFixed, SunriseCoral), tl, tl + Offset(s, s)),
                tl, Size(s, s), CornerRadius(s * 0.22f),
            )
            for (k in 1..3) {
                val inset = s * 0.1f * k
                drawRoundRect(
                    Night.copy(alpha = 0.35f), tl + Offset(inset, inset), Size(s - 2 * inset, s - 2 * inset), CornerRadius(s * 0.12f),
                    style = Stroke(2.dp.toPx()),
                )
            }
        }
        Icon(Icons.Rounded.Nfc, null, tint = Night, modifier = Modifier.size(40.dp).graphicsLayer { scaleX = core.value; scaleY = core.value })
    }
}

@Composable
fun NfcMission(config: MissionConfig, onActivity: () -> Unit, onFailedAttempt: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var matched by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf<String?>(null) }
    var lastWrong by remember { mutableLongStateOf(0L) }
    var state by remember { mutableStateOf(context.nfcState()) }
    LifecycleResumeEffect(Unit) {
        state = context.nfcState()
        onPauseOrDispose { }
    }
    val done by rememberUpdatedState(onDone)

    fun read(id: String) {
        if (matched) return
        onActivity()
        if (config.nfcTagId == null || id.equals(config.nfcTagId, ignoreCase = true)) {
            matched = true
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            done()
        } else if (System.currentTimeMillis() - lastWrong > 2000) {
            lastWrong = System.currentTimeMillis()
            hint = "That's a different sticker. Find the one you registered."
            haptics.performHapticFeedback(HapticFeedbackType.Reject)
            onFailedAttempt()
        }
    }
    NfcListener(::read)

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            config.nfcLabel?.let { "Tap your phone on the sticker by the $it" } ?: "Tap your phone on your sticker",
            style = MaterialTheme.typography.headlineSmall, color = Paper, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        NfcWaves(matched, onSimulate = if (nfcSimulated && state != NfcState.Ready) { { read(config.nfcTagId ?: "SIM") } } else null)
        Spacer(Modifier.weight(1f))
        if (state == NfcState.Off) {
            Text("NFC is switched off", style = MaterialTheme.typography.titleMedium, color = MorningGold)
            Spacer(Modifier.height(Space.sm))
            com.wakeroo.app.feature.editor.PillButton("Turn on NFC", Modifier.fillMaxWidth()) {
                runCatching { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
        } else {
            Text(
                hint ?: "Hold the top of your phone flat against it for a second",
                style = MaterialTheme.typography.bodyLarge, color = if (hint != null) MorningGold else PaperMuted, textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(Space.lg))
    }
}
