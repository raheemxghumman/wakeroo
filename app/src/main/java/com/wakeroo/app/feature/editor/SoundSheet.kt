package com.wakeroo.app.feature.editor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.wakeroo.app.alarm.sound.Tone
import com.wakeroo.app.alarm.sound.ToneCategory
import com.wakeroo.app.alarm.sound.Tones
import com.wakeroo.app.data.model.Alarm
import com.wakeroo.app.ui.components.press3d
import com.wakeroo.app.ui.theme.Base
import com.wakeroo.app.ui.theme.DawnLavender
import com.wakeroo.app.ui.theme.Hairline
import com.wakeroo.app.ui.theme.MorningGold
import com.wakeroo.app.ui.theme.Night
import com.wakeroo.app.ui.theme.NightSurface
import com.wakeroo.app.ui.theme.NightSurfaceHigh
import com.wakeroo.app.ui.theme.Paper
import com.wakeroo.app.ui.theme.PaperMuted
import com.wakeroo.app.ui.theme.Space
import com.wakeroo.app.ui.theme.SunriseCoral
import kotlin.math.abs
import kotlin.math.sin

/**
 * Plays a short, faded preview on the alarm stream at the alarm's own volume, so what you hear
 * now is what you'll hear at 6 AM. Stops by itself after a few seconds.
 */
class TonePreview(private val context: Context) {
    private var player: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    var playingKey: String? = null
        private set
    var onChange: () -> Unit = {}

    fun play(key: String, uri: Uri, volumePercent: Int) {
        stop()
        val peak = (volumePercent / 100f).coerceIn(0.1f, 1f) * 0.8f
        val mp = MediaPlayer()
        runCatching {
            mp.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            mp.setDataSource(context, uri)
            mp.isLooping = true
            mp.prepare()
            mp.setVolume(0f, 0f)
            mp.start()
        }.onFailure { mp.release(); return }
        player = mp
        playingKey = key
        onChange()
        val start = System.currentTimeMillis()
        val fade = object : Runnable {
            override fun run() {
                val p = player ?: return
                val elapsed = System.currentTimeMillis() - start
                val v = when {
                    elapsed < 500 -> peak * elapsed / 500f
                    elapsed < 5500 -> peak
                    elapsed < 6500 -> peak * (1 - (elapsed - 5500) / 1000f)
                    else -> { stop(); return }
                }
                runCatching { p.setVolume(v, v) }
                handler.postDelayed(this, 50)
            }
        }
        handler.post(fade)
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
        player?.runCatching { stop(); release() }
        player = null
        if (playingKey != null) {
            playingKey = null
            onChange()
        }
    }
}

private enum class SoundTab(val title: String) { Gentle("Gentle"), Energetic("Energetic"), Extreme("Extreme"), Own("Your own") }

fun Alarm.soundTitle(): String = when {
    soundUri != null -> soundName ?: "Your sound"
    else -> Tones.byId(tone).name
}

fun Alarm.soundCategoryTitle(): String = when {
    soundUri?.startsWith("content://media") == true -> "Phone tone"
    soundUri != null -> "Your music"
    else -> Tones.byId(tone).category.title
}

/** Remembers a [TonePreview] for this screen and stops it when the screen goes away. */
@Composable
fun rememberTonePreview(): Pair<TonePreview, String?> {
    val context = LocalContext.current
    var playing by remember { mutableStateOf<String?>(null) }
    val preview = remember { TonePreview(context) }
    DisposableEffect(Unit) {
        preview.onChange = { playing = preview.playingKey }
        onDispose { preview.stop() }
    }
    return preview to playing
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundSheet(draft: Alarm, pro: Boolean, onDismiss: () -> Unit, onUpgrade: () -> Unit, update: ((Alarm) -> Alarm) -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val (preview, playing) = rememberTonePreview()
    ModalBottomSheet(onDismissRequest = { preview.stop(); onDismiss() }, sheetState = sheet, containerColor = Base, dragHandle = null) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(Space.xl)) {
            Text("Alarm sound", style = MaterialTheme.typography.headlineMedium, color = Paper)
            Text(
                "Tap to hear it at your alarm's volume.",
                style = MaterialTheme.typography.bodyMedium, color = PaperMuted, modifier = Modifier.padding(top = Space.xs, bottom = Space.lg),
            )
            SoundPicker(draft, pro, preview, playing, onUpgrade = { preview.stop(); onUpgrade() }, update = update)
            Spacer(Modifier.height(Space.xl))
            PillButton("Done", Modifier.fillMaxWidth()) { preview.stop(); onDismiss() }
        }
    }
}

/** Category tabs and tone cards; shared by the editor's sound sheet and onboarding. */
@Composable
fun SoundPicker(
    draft: Alarm,
    pro: Boolean,
    preview: TonePreview,
    playing: String?,
    onUpgrade: () -> Unit,
    update: ((Alarm) -> Alarm) -> Unit,
    showOwn: Boolean = true,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val tabs = if (showOwn) SoundTab.entries else SoundTab.entries.filter { it != SoundTab.Own }
    val startTab = when {
        draft.soundUri != null && showOwn -> SoundTab.Own
        else -> SoundTab.valueOf(Tones.byId(draft.tone).category.name)
    }
    var tab by remember(startTab) { mutableStateOf(startTab) }
    var lockedPick by remember { mutableStateOf<Tone?>(null) }

    val ringtonePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        @Suppress("DEPRECATION")
        val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI) ?: return@rememberLauncherForActivityResult
        val name = runCatching { RingtoneManager.getRingtone(context, uri).getTitle(context) }.getOrNull() ?: "Phone tone"
        update { it.copy(soundUri = uri.toString(), soundName = name) }
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        } ?: "Your song"
        update { it.copy(soundUri = uri.toString(), soundName = name.substringBeforeLast('.')) }
    }

    Column {
        SegmentedTabs(tabs.map { it.title }, tabs.indexOf(tab).coerceAtLeast(0)) {
            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            tab = tabs[it]
        }
        Spacer(Modifier.height(Space.lg))
        AnimatedContent(
            tab,
            transitionSpec = {
                val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                (slideInHorizontally { dir * it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -dir * it / 3 } + fadeOut())
            },
            label = "soundTab",
        ) { t ->
            Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
                if (t == SoundTab.Own) {
                    OwnSoundRow(Icons.Rounded.PhoneAndroid, "Phone tone", "One of your phone's alarm sounds", draft.soundUri?.startsWith("content://media") == true, draft.soundName) {
                        preview.stop()
                        ringtonePicker.launch(
                            Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                                .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false),
                        )
                    }
                    val isFile = draft.soundUri != null && !draft.soundUri.startsWith("content://media")
                    OwnSoundRow(Icons.Rounded.Folder, "Your music", "Any song or audio file", isFile, draft.soundName) {
                        preview.stop()
                        filePicker.launch(arrayOf("audio/*"))
                    }
                    Text(
                        "If your file is ever moved or deleted, Wakeroo Rise plays instead. An alarm is never silent.",
                        style = MaterialTheme.typography.bodySmall, color = PaperMuted,
                    )
                } else {
                    val category = ToneCategory.valueOf(t.name)
                    Text(category.blurb, style = MaterialTheme.typography.labelMedium, color = DawnLavender)
                    Tones.inCategory(category).chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                            row.forEach { tone ->
                                val selected = draft.soundUri == null && Tones.byId(draft.tone).id == tone.id
                                ToneCard(
                                    tone, selected = selected, playing = playing == tone.id, locked = tone.pro && !pro,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    if (playing == tone.id) preview.stop() else preview.play(tone.id, Tones.uri(context, tone), draft.volume)
                                    if (tone.pro && !pro) lockedPick = tone
                                    else {
                                        lockedPick = null
                                        update { it.copy(tone = tone.id, soundUri = null, soundName = null) }
                                    }
                                }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        lockedPick?.let { tone ->
            Spacer(Modifier.height(Space.lg))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF3A2B4E), Color(0xFF6B4A78))))
                    .clickable { preview.stop(); onUpgrade() }
                    .padding(Space.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Lock, null, tint = MorningGold)
                Spacer(Modifier.width(Space.md))
                Text("${tone.name} comes with Pro. Tap to see Pro", style = MaterialTheme.typography.labelLarge, color = Paper, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** Pill tabs with a coral highlight that slides between them on a spring. */
@Composable
fun SegmentedTabs(titles: List<String>, selected: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    BoxWithConstraints(
        modifier.fillMaxWidth().height(48.dp).clip(CircleShape).background(NightSurface).border(1.dp, Hairline, CircleShape).padding(4.dp),
    ) {
        val w = maxWidth / titles.size
        val x by animateDpAsState(w * selected, spring(dampingRatio = 0.75f, stiffness = 420f), label = "seg")
        Box(Modifier.offset(x = x).width(w).fillMaxHeight().clip(CircleShape).background(SunriseCoral))
        Row(Modifier.fillMaxSize()) {
            titles.forEachIndexed { i, title ->
                val color by animateColorAsState(if (i == selected) Night else Paper, label = "segText")
                Box(
                    Modifier.weight(1f).fillMaxHeight().clip(CircleShape).clickable(role = Role.Tab) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) { Text(title, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1) }
            }
        }
    }
}

/** A tone with its own little waveform, which dances while the preview plays. */
@Composable
private fun ToneCard(tone: Tone, selected: Boolean, playing: Boolean, locked: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val border by animateColorAsState(if (selected) SunriseCoral else Hairline, label = "toneBorder")
    val wave by rememberInfiniteTransition(label = "toneWave").animateFloat(
        0f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart), label = "w",
    )
    val seed = abs(tone.id.hashCode())
    val bars = remember(tone.id) { List(18) { i -> 0.25f + 0.75f * abs(sin((seed % 97) * 0.13f + i * (0.5f + (seed % 7) * 0.09f))) } }
    val accent = when (tone.category) {
        ToneCategory.Gentle -> DawnLavender
        ToneCategory.Energetic -> MorningGold
        ToneCategory.Extreme -> SunriseCoral
    }
    Column(
        modifier
            .press3d(maxTilt = 8f)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(NightSurfaceHigh, NightSurface)))
            .border(if (selected) 2.dp else 1.dp, border, RoundedCornerShape(22.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(Space.md),
    ) {
        Canvas(Modifier.fillMaxWidth().height(40.dp)) {
            val gap = size.width / bars.size
            bars.forEachIndexed { i, h ->
                val live = if (playing) 0.55f + 0.45f * abs(sin((wave + i / bars.size.toFloat()) * 6.28f * 1.5f)) else 1f
                val bh = size.height * h * live
                drawRoundRect(
                    accent.copy(alpha = if (playing || selected) 0.95f else 0.45f),
                    Offset(i * gap + gap * 0.2f, (size.height - bh) / 2), Size(gap * 0.6f, bh), CornerRadius(gap * 0.3f),
                )
            }
        }
        Spacer(Modifier.height(Space.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tone.name, style = MaterialTheme.typography.titleSmall, color = Paper, modifier = Modifier.weight(1f), maxLines = 1)
            Icon(
                when { locked -> Icons.Rounded.Lock; playing -> Icons.Rounded.Stop; else -> Icons.Rounded.PlayArrow },
                null, tint = if (locked) MorningGold else PaperMuted, modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun OwnSoundRow(icon: ImageVector, title: String, subtitle: String, selected: Boolean, name: String?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .press3d(maxTilt = 4f, sink = 0.98f)
            .clip(RoundedCornerShape(22.dp))
            .background(NightSurface)
            .border(if (selected) 2.dp else 1.dp, if (selected) SunriseCoral else Hairline, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(Space.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(MorningGold.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MorningGold)
        }
        Spacer(Modifier.width(Space.md))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Paper)
            Text(if (selected && name != null) "Chosen: $name" else subtitle, style = MaterialTheme.typography.bodySmall, color = PaperMuted, maxLines = 1)
        }
    }
}
