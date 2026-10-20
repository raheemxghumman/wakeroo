package com.wakeroo.app.feature.editor

import com.wakeroo.app.ui.i18n.tr
import com.wakeroo.app.ui.util.initial
import com.wakeroo.app.ui.util.twoDigits
import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.OpenableColumns
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Wallpaper
import com.wakeroo.app.ui.scenes.Scene
import com.wakeroo.app.missions.HarderMode
import com.wakeroo.app.missions.MissionGateDialog
import androidx.compose.material.icons.rounded.Lock
import com.wakeroo.app.ui.scenes.ScenePicker
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wakeroo.app.data.model.Alarm
import com.wakeroo.app.data.model.MissionConfig
import com.wakeroo.app.data.model.MissionType
import com.wakeroo.app.data.model.SoundMode
import com.wakeroo.app.alarm.sound.Tones
import com.wakeroo.app.ui.components.Chip
import com.wakeroo.app.ui.components.ChipRow
import com.wakeroo.app.ui.components.DrumWheel
import com.wakeroo.app.ui.components.SectionCard
import com.wakeroo.app.ui.components.SkyBackground
import com.wakeroo.app.ui.components.Stepper
import com.wakeroo.app.ui.components.ToggleRow
import com.wakeroo.app.ui.components.icon
import com.wakeroo.app.ui.components.press3d
import com.wakeroo.app.ui.components.sharedContainer
import com.wakeroo.app.ui.theme.Base
import com.wakeroo.app.ui.theme.ClockType
import com.wakeroo.app.ui.theme.DawnLavender
import com.wakeroo.app.ui.theme.Glass
import com.wakeroo.app.ui.theme.MorningGold
import com.wakeroo.app.ui.theme.Night
import com.wakeroo.app.ui.theme.Paper
import com.wakeroo.app.ui.theme.PaperMuted
import com.wakeroo.app.ui.theme.Sky
import com.wakeroo.app.ui.theme.Space
import com.wakeroo.app.ui.theme.SunriseCoral
import com.wakeroo.app.ui.util.formatMeridiem
import com.wakeroo.app.ui.util.formatUntil
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun EditorScreen(
    vm: EditorViewModel,
    onClose: () -> Unit,
    onSaved: (String) -> Unit,
    onUpgrade: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val use24h = DateFormat.is24HourFormat(context)
    val draft = state.draft
    var missionSheet by remember { mutableStateOf<Int?>(null) }
    // Harder Mode: an action waiting for its mission to be done.
    var gated by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    val original = state.original

    Box(Modifier.fillMaxSize().sharedContainer(if (state.isNew) "alarm-new" else "alarm-${draft.id}")) {
        SkyBackground(Sky.forHour(draft.hour), intensity = 0.5f) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.statusBarsPadding().fillMaxWidth().padding(horizontal = Space.lg, vertical = Space.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconCircle(Icons.Rounded.Close, tr("Close"), onClose)
                    Spacer(Modifier.weight(1f))
                    Text(if (state.isNew) tr("New alarm") else tr("Edit alarm"), style = MaterialTheme.typography.titleMedium, color = Paper)
                    Spacer(Modifier.weight(1f))
                    if (!state.isNew) IconCircle(Icons.Rounded.DeleteOutline, tr("Delete alarm")) {
                        val delete = { scope.launch { vm.delete(); onClose() }; Unit }
                        if (original?.hardMode == true) gated = tr("Delete this alarm") to delete else delete()
                    } else Spacer(Modifier.size(48.dp))
                }
                if (!state.loaded) return@Column
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Space.xl).imePadding(),
                    verticalArrangement = Arrangement.spacedBy(Space.lg),
                ) {
                    TimeCard(draft, use24h, state, onTime = vm::setTime)
                    RepeatSection(draft) { vm.update(it) }
                    LabelSection(draft) { vm.update(it) }
                    MissionsSection(draft, state.maxMissions, onEdit = { missionSheet = it }, onRemove = vm::removeMission, onUpgrade = onUpgrade)
                    SoundSection(draft, state.pro, onUpgrade) { vm.update(it) }
                    SceneSection(draft, state, onUpgrade) { vm.update(it) }
                    SnoozeSection(draft) { vm.update(it) }
                    WakeCheckSection(draft) { vm.update(it) }
                    HardModeSection(draft, state, onUpgrade) { vm.update(it) }
                    Spacer(Modifier.height(110.dp))
                }
            }
            SaveBar(
                Modifier.align(Alignment.BottomCenter),
                enabled = state.loaded,
                onSave = {
                    val save = {
                        scope.launch {
                            vm.save()
                            val until = state.nextRing?.let { Duration.between(ZonedDateTime.now(), it).formatUntil() }
                            onSaved(if (until != null) tr("Alarm set for {0} from now", until) else tr("Alarm saved"))
                        }
                        Unit
                    }
                    if (original != null && HarderMode.weakens(original, draft.copy(enabled = true))) gated = tr("Save these changes") to save else save()
                },
            )
        }
    }

    gated?.let { (title, action) ->
        MissionGateDialog(
            mission = original?.missions?.firstOrNull() ?: HarderMode.SNOOZE_MISSION, title = title,
            onPassed = { gated = null; action() }, onCancel = { gated = null },
        )
    }

    missionSheet?.let { index ->
        MissionPickerSheet(
            existing = draft.missions.getOrNull(index),
            onDismiss = { missionSheet = null },
            onSave = { config ->
                vm.setMission(index, config)
                missionSheet = null
            },
        )
    }
}

@Composable
private fun IconCircle(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).press3d().clip(CircleShape).background(Paper.copy(alpha = 0.08f)).clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = Paper) }
}

@Composable
private fun TimeCard(draft: Alarm, use24h: Boolean, state: EditorState, onTime: (Int, Int) -> Unit) {
    val hours = if (use24h) (0..23).map { twoDigits(it) } else (0..11).map { if (it == 0) "12" else "$it" }
    val minutes = (0..59).map { twoDigits(it) }
    val pm = draft.hour >= 12
    val wheelStyle = ClockType.card.copy(fontSize = 44.sp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Glass)
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.03f))))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(32.dp))
            .padding(vertical = Space.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            // The highlight band the centre values sit in.
            Box(Modifier.fillMaxWidth(0.86f).height(64.dp).clip(RoundedCornerShape(20.dp)).background(Glass))
            Row(verticalAlignment = Alignment.CenterVertically) {
                DrumWheel(
                    values = hours, selected = if (use24h) draft.hour else draft.hour % 12, style = wheelStyle, label = "Hour",
                    onSelected = { i -> onTime(if (use24h) i else i + if (pm) 12 else 0, draft.minute) },
                )
                Text(":", style = wheelStyle, color = Paper)
                DrumWheel(values = minutes, selected = draft.minute, style = wheelStyle, label = "Minute", onSelected = { m -> onTime(draft.hour, m) })
                if (!use24h) {
                    DrumWheel(
                        values = listOf(formatMeridiem(0), formatMeridiem(12)), selected = if (pm) 1 else 0, cyclic = false, width = 80.dp,
                        style = MaterialTheme.typography.headlineSmall, label = "AM or PM",
                        onSelected = { i -> onTime(draft.hour % 12 + if (i == 1) 12 else 0, draft.minute) },
                    )
                }
            }
        }
        val until = state.nextRing?.let { Duration.between(ZonedDateTime.now(), it).formatUntil() }
        AnimatedContent(until, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "until") { text ->
            Text(
                if (text == null) tr("This date has passed") else tr("Rings in {0}", text),
                style = MaterialTheme.typography.titleMedium, color = MorningGold,
            )
        }
        Row(
            Modifier.padding(top = Space.md).horizontalScroll(rememberScrollState()).padding(horizontal = Space.lg),
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            listOf(5 to tr("+5 min"), 30 to tr("+30 min"), 450 to tr("7.5 h sleep"), 480 to tr("8 h sleep")).forEach { (m, label) ->
                Chip(label, selected = false, onClick = {
                    val at = ZonedDateTime.now().plusMinutes(m.toLong())
                    onTime(at.hour, at.minute)
                })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepeatSection(draft: Alarm, update: ((Alarm) -> Alarm) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    SectionCard(tr("Repeat"), icon = Icons.Rounded.EventRepeat) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            DayOfWeek.entries.forEach { day ->
                val on = day in draft.days && draft.date == null
                val bg = if (on) SunriseCoral else Paper.copy(alpha = 0.07f)
                Box(
                    Modifier
                        .size(42.dp)
                        .press3d()
                        .clip(CircleShape)
                        .background(bg)
                        .clickable(role = Role.Checkbox) {
                            update { a -> a.copy(days = if (day in a.days) a.days - day else a.days + day, date = null, skipDate = null) }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(day.initial(), style = MaterialTheme.typography.labelLarge, color = if (on) Night else Paper)
                }
            }
        }
        Spacer(Modifier.height(Space.md))
        val weekdays = DayOfWeek.entries.take(5).toSet()
        ChipRow {
            Chip(tr("Once"), draft.days.isEmpty() && draft.date == null, { update { it.copy(days = emptySet(), date = null) } })
            Chip(tr("Weekdays"), draft.days == weekdays && draft.date == null, { update { it.copy(days = weekdays, date = null) } })
            Chip(tr("Every day"), draft.days.size == 7 && draft.date == null, { update { it.copy(days = DayOfWeek.entries.toSet(), date = null) } })
            Chip(
                draft.date?.let { DateTimeFormatter.ofPattern("EEE d MMM").format(it) } ?: "Pick a date",
                draft.date != null, { picking = true },
            )
        }
    }
    if (picking) {
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = (draft.date ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { ms ->
                        val date = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                        update { it.copy(date = date, days = emptySet()) }
                    }
                    picking = false
                }) { Text(tr("Set date")) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(tr("Cancel")) } },
        ) { DatePicker(picker) }
    }
}

@Composable
private fun LabelSection(draft: Alarm, update: ((Alarm) -> Alarm) -> Unit) {
    SectionCard(tr("Why are you getting up?"), icon = Icons.Rounded.EditNote) {
        OutlinedTextField(
            value = draft.label,
            onValueChange = { v -> update { it.copy(label = v.take(60)) } },
            placeholder = { Text(tr("Gym before class, exam at 9…"), color = PaperMuted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DawnLavender, unfocusedBorderColor = Paper.copy(alpha = 0.15f),
                focusedTextColor = Paper, unfocusedTextColor = Paper, cursorColor = SunriseCoral,
            ),
        )
        Text(
            tr("It shows big on the alarm screen. A reason beats a snooze."),
            style = MaterialTheme.typography.bodySmall, color = PaperMuted, modifier = Modifier.padding(top = Space.sm),
        )
    }
}

@Composable
private fun MissionsSection(draft: Alarm, max: Int, onEdit: (Int) -> Unit, onRemove: (Int) -> Unit, onUpgrade: () -> Unit) {
    SectionCard(tr("Missions"), icon = Icons.Rounded.Flag) {
        draft.missions.forEachIndexed { i, m ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = Space.sm)
                    .press3d(maxTilt = 3f, sink = 0.98f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Glass)
                    .clickable { onEdit(i) }
                    .padding(Space.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(MorningGold.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                    Icon(m.type.icon, null, tint = MorningGold)
                }
                Spacer(Modifier.width(Space.md))
                Column(Modifier.weight(1f)) {
                    Text(m.type.title, style = MaterialTheme.typography.titleMedium, color = Paper)
                    Text(m.summary(), style = MaterialTheme.typography.bodySmall, color = PaperMuted)
                }
                if (draft.missions.size > 1) {
                    Icon(
                        Icons.Rounded.Close, tr("Remove mission"), tint = PaperMuted,
                        modifier = Modifier.clip(CircleShape).clickable { onRemove(i) }.padding(Space.sm),
                    )
                }
            }
        }
        val canAdd = draft.missions.size < max
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.5.dp, Paper.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                .clickable { if (canAdd) onEdit(draft.missions.size) else onUpgrade() }
                .padding(Space.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Rounded.Add, null, tint = if (canAdd) DawnLavender else MorningGold)
            Spacer(Modifier.width(Space.sm))
            Text(
                if (canAdd) tr("Add a mission") else tr("Up to 5 missions with Pro"),
                style = MaterialTheme.typography.labelLarge, color = if (canAdd) DawnLavender else MorningGold,
            )
        }
    }
}

fun MissionConfig.summary(): String = when (type) {
    MissionType.Math -> tr("{0} problems · {1}", count, difficultyName(difficulty))
    MissionType.Typing -> if (count == 1) tr("1 phrase") else tr("{0} phrases", count)
    MissionType.Shake -> tr("{0} shakes", count)
    MissionType.Scan -> codeLabel?.let { tr("Code on your {0}", it) } ?: if (codeValue == null) tr("Any code") else tr("Registered code")
    MissionType.Photo -> if (photoHash == null) tr("Spot not set") else tr("Your photo spot · {0} match", difficultyName(difficulty))
    MissionType.Steps -> tr("{0} steps", count)
    MissionType.Memory -> tr("{0} rounds · {1}×{2}", count, gridSize, gridSize)
    MissionType.Nfc -> nfcLabel?.let { tr("Sticker by the {0}", it) } ?: tr("Sticker not registered")
    MissionType.Plan -> tr("Three words about your day")
    MissionType.Hunt -> (if (count == 1) tr("1 object") else tr("{0} objects", count)) + " · " + listOf(tr("Everyday"), tr("Varied"), tr("Strict"))[(difficulty - 1).coerceIn(0, 2)]
}.let { if (needsSetup) tr("Set up tonight · math until then") else it }

fun difficultyName(d: Int) = when (d) { 1 -> "Easy"; 2 -> tr("Medium"); else -> tr("Hard") }

@Composable
private fun SoundSection(draft: Alarm, pro: Boolean, onUpgrade: () -> Unit, update: ((Alarm) -> Alarm) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    SectionCard(tr("Sound"), icon = Icons.Rounded.MusicNote) {
        Row(
            Modifier
                .fillMaxWidth()
                .press3d(maxTilt = 3f, sink = 0.98f)
                .clip(RoundedCornerShape(20.dp))
                .background(Glass)
                .clickable { picking = true }
                .padding(Space.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(Brush.linearGradient(listOf(MorningGold, SunriseCoral))),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.GraphicEq, null, tint = Night) }
            Spacer(Modifier.width(Space.md))
            Column(Modifier.weight(1f)) {
                Text(draft.soundTitle(), style = MaterialTheme.typography.titleMedium, color = Paper, maxLines = 1)
                Text(draft.soundCategoryTitle(), style = MaterialTheme.typography.bodySmall, color = PaperMuted)
            }
            Text(tr("Change"), style = MaterialTheme.typography.labelLarge, color = DawnLavender)
        }
        Spacer(Modifier.height(Space.lg))
        Text(tr("Each morning"), style = MaterialTheme.typography.bodyLarge, color = Paper)
        Spacer(Modifier.height(Space.sm))
        ChipRow {
            Chip(tr("Same sound"), draft.soundMode == SoundMode.Single, { update { it.copy(soundMode = SoundMode.Single) } })
            Chip(tr("Rotate"), draft.soundMode == SoundMode.Rotate, {
                // Rotation draws from Wakeroo's tones, so a custom sound gives way to its category.
                update { it.copy(soundMode = SoundMode.Rotate, soundUri = null, soundName = null) }
            })
            Chip(tr("Escalate"), draft.soundMode == SoundMode.Escalate, { update { it.copy(soundMode = SoundMode.Escalate) } })
        }
        AnimatedContent(draft.soundMode, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "soundMode") { mode ->
            Text(
                when (mode) {
                    SoundMode.Single -> tr("The same sound every time.")
                    SoundMode.Rotate -> tr("A different {0} tone each morning, so you never get used to it.", Tones.byId(draft.tone).category.title.lowercase())
                    SoundMode.Escalate -> tr("Starts with this sound. No response in a minute and it switches to an extreme tone at full volume.")
                },
                style = MaterialTheme.typography.bodySmall, color = PaperMuted, modifier = Modifier.padding(top = Space.sm),
            )
        }
        Spacer(Modifier.height(Space.lg))
        LabeledSlider(tr("Volume"), "${draft.volume}%", draft.volume / 100f, 0.1f..1f) { v -> update { it.copy(volume = (v * 100).toInt()) } }
        val rampLabels = listOf(0 to tr("Off"), 15 to "15 s", 30 to "30 s", 60 to tr("1 min"), 120 to tr("2 min"), 300 to tr("5 min"), 600 to tr("10 min"), 900 to tr("15 min"))
        val rampIndex = rampLabels.indexOfFirst { it.first >= draft.rampSeconds }.coerceAtLeast(0)
        LabeledSlider(
            tr("Gentle start"), rampLabels[rampIndex].second, rampIndex / (rampLabels.size - 1f), 0f..1f, steps = rampLabels.size - 2,
        ) { v -> update { it.copy(rampSeconds = rampLabels[(v * (rampLabels.size - 1)).toInt().coerceIn(0, rampLabels.lastIndex)].first) } }
        ToggleRow(tr("Vibrate"), null, draft.vibrate) { on -> update { it.copy(vibrate = on) } }
    }
    if (picking) SoundSheet(draft, pro, onDismiss = { picking = false }, onUpgrade = { picking = false; onUpgrade() }, update = update)
}

@Composable
private fun SceneSection(draft: Alarm, state: EditorState, onUpgrade: () -> Unit, update: ((Alarm) -> Alarm) -> Unit) {
    SectionCard(tr("Wake-up scene"), icon = Icons.Rounded.Wallpaper) {
        Text(
            tr("What the alarm screen looks like. Tap one to see it full screen."),
            style = MaterialTheme.typography.bodySmall, color = PaperMuted, modifier = Modifier.padding(bottom = Space.md),
        )
        ScenePicker(
            selected = draft.scene, pro = state.pro, hour = draft.hour, minute = draft.minute,
            defaultLabel = tr("App default"), defaultScene = state.defaultScene,
            onSelect = { id -> update { it.copy(scene = id) } }, onUpgrade = onUpgrade,
        )
        val scene = Scene.byId(draft.scene ?: state.defaultScene)
        val suggested = scene.suggestedTone?.let { Tones.byId(it) }
        AnimatedVisibility(suggested != null && draft.soundUri == null && Tones.byId(draft.tone).id != suggested.id) {
            if (suggested == null) return@AnimatedVisibility
            Row(
                Modifier.padding(top = Space.md).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Glass)
                    .clickable { if (!suggested.pro || state.pro) update { it.copy(tone = suggested.id, soundUri = null, soundName = null) } else onUpgrade() }
                    .padding(Space.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.GraphicEq, null, tint = MorningGold, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Space.sm))
                Text(tr("{0} pairs well with {1}", scene.title, suggested.name), style = MaterialTheme.typography.bodySmall, color = Paper, modifier = Modifier.weight(1f))
                Text(tr("Use it"), style = MaterialTheme.typography.labelLarge, color = SunriseCoral)
            }
        }
    }
}

@Composable
private fun LabeledSlider(title: String, value: String, fraction: Float, range: ClosedFloatingPointRange<Float>, steps: Int = 0, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = Paper, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelLarge, color = MorningGold)
    }
    Slider(
        value = fraction, onValueChange = onChange, valueRange = range, steps = steps,
        colors = SliderDefaults.colors(thumbColor = SunriseCoral, activeTrackColor = SunriseCoral, inactiveTrackColor = Paper.copy(alpha = 0.12f), activeTickColor = Night, inactiveTickColor = PaperMuted),
    )
}

@Composable
private fun SnoozeSection(draft: Alarm, update: ((Alarm) -> Alarm) -> Unit) {
    SectionCard(tr("Snooze"), icon = Icons.Rounded.Snooze) {
        ChipRow {
            Chip(tr("Off"), draft.snoozeLimit == 0, { update { it.copy(snoozeLimit = 0) } })
            listOf(3, 5, 9, 10, 15).forEach { m ->
                Chip(tr("{0} min", m), draft.snoozeLimit > 0 && draft.snoozeMinutes == m, {
                    update { it.copy(snoozeMinutes = m, snoozeLimit = if (it.snoozeLimit == 0) 2 else it.snoozeLimit) }
                })
            }
        }
        AnimatedVisibility(draft.snoozeLimit > 0, enter = fadeIn() + slideInVertically(), exit = fadeOut() + slideOutVertically()) {
            Column(Modifier.padding(top = Space.md)) {
                Stepper(
                    tr("Snoozes allowed"), "${draft.snoozeLimit}",
                    onMinus = { update { it.copy(snoozeLimit = (it.snoozeLimit - 1).coerceAtLeast(1)) } },
                    onPlus = { update { it.copy(snoozeLimit = (it.snoozeLimit + 1).coerceAtMost(5)) } },
                    minusEnabled = draft.snoozeLimit > 1, plusEnabled = draft.snoozeLimit < 5,
                )
            }
        }
    }
}

@Composable
private fun WakeCheckSection(draft: Alarm, update: ((Alarm) -> Alarm) -> Unit) {
    SectionCard(tr("Wake-up check"), icon = Icons.Rounded.Verified) {
        ToggleRow(
            tr("Make sure I stay up"), tr("A few minutes later Wakeroo asks \"Still up?\". No answer, and the alarm comes back."),
            draft.wakeCheckMinutes > 0,
        ) { on -> update { it.copy(wakeCheckMinutes = if (on) 3 else 0) } }
        AnimatedVisibility(draft.wakeCheckMinutes > 0) {
            ChipRow {
                listOf(1, 3, 5, 10).forEach { m ->
                    Chip(tr("After {0} min", m), draft.wakeCheckMinutes == m, { update { it.copy(wakeCheckMinutes = m) } })
                }
            }
        }
    }
}

@Composable
private fun HardModeSection(draft: Alarm, state: EditorState, onUpgrade: () -> Unit, update: ((Alarm) -> Alarm) -> Unit) {
    val soon = state.nextRing?.let { Duration.between(ZonedDateTime.now(), it).toMillis() < HarderMode.MIN_LEAD_MS } == true
    SectionCard(tr("Harder Mode"), icon = Icons.Rounded.Lock) {
        if (!state.pro) {
            Text(
                tr("Turning this alarm off, deleting it, skipping it or snoozing takes a mission first. Part of Pro."),
                style = MaterialTheme.typography.bodyMedium, color = PaperMuted,
            )
            Spacer(Modifier.height(Space.md))
            PillButton("See Pro", Modifier.fillMaxWidth(), outlined = true, icon = Icons.Rounded.Lock, onClick = onUpgrade)
            return@SectionCard
        }
        ToggleRow(
            tr("Protect this alarm"),
            tr("Off, delete, skip, snooze or moving it 2+ hours later all need a mission first. The emergency exit still works."),
            draft.hardMode,
        ) { on ->
            // Switching it off is a weakening change, handled at save time like any other.
            if (!on || !soon) update { it.copy(hardMode = on) }
        }
        if (soon && !draft.hardMode) {
            Text(
                tr("Can't be switched on for an alarm less than 10 minutes away, so nobody locks themselves in by accident."),
                style = MaterialTheme.typography.bodySmall, color = MorningGold, modifier = Modifier.padding(top = Space.sm),
            )
        }
    }
}

@Composable
private fun SaveBar(modifier: Modifier, enabled: Boolean, onSave: () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Base.copy(alpha = 0.92f))))
            .navigationBarsPadding()
            .padding(horizontal = Space.xl, vertical = Space.lg),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .press3d()
                .clip(RoundedCornerShape(32.dp))
                .background(Brush.horizontalGradient(listOf(SunriseCoral, Color(0xFFFF9466))))
                .clickable(enabled = enabled, role = Role.Button, onClick = onSave),
            contentAlignment = Alignment.Center,
        ) { Text(tr("Save alarm"), style = MaterialTheme.typography.titleMedium, color = Night) }
    }
}
