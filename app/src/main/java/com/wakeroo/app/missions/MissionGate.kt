package com.wakeroo.app.missions

import com.wakeroo.app.ui.i18n.tr
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wakeroo.app.data.model.Alarm
import com.wakeroo.app.data.model.MissionConfig
import com.wakeroo.app.data.model.MissionType
import com.wakeroo.app.ui.components.SkyBackground
import com.wakeroo.app.ui.theme.DawnLavender
import com.wakeroo.app.ui.theme.MorningGold
import com.wakeroo.app.ui.theme.Paper
import com.wakeroo.app.ui.theme.PaperMuted
import com.wakeroo.app.ui.theme.Sky
import com.wakeroo.app.ui.theme.Space
import com.wakeroo.app.ui.theme.WakerooTheme

/** Harder Mode, decided in one place: what a change to a protected alarm costs. */
object HarderMode {
    /** Moving an alarm this much later counts as weakening it. */
    const val MOVE_LATER_LIMIT_MIN = 120

    /** Harder Mode can't be switched on this close to the alarm, so nobody locks themselves in. */
    const val MIN_LEAD_MS = 10 * 60_000L

    /**
     * True when saving [edited] over [original] weakens a Harder Mode alarm: switching it
     * or Harder Mode off, dropping missions, or moving it more than two hours later.
     */
    fun weakens(original: Alarm, edited: Alarm): Boolean {
        if (!original.hardMode) return false
        if (!edited.hardMode || !edited.enabled) return true
        if (edited.missions.size < original.missions.size) return true
        val before = original.hour * 60 + original.minute
        val after = edited.hour * 60 + edited.minute
        // On a 24-hour dial, more than 12 hours "later" really means earlier.
        val later = ((after - before) + 24 * 60) % (24 * 60)
        return later in (MOVE_LATER_LIMIT_MIN + 1)..(12 * 60)
    }

    /** The quick mission a Harder Mode snooze costs. */
    val SNOOZE_MISSION = MissionConfig(MissionType.Math, difficulty = 1, count = 1)
}

/**
 * Full-screen "prove you're awake first": runs [mission] with no alarm sound, then calls
 * [onPassed]. Close it to keep the alarm exactly as it was.
 */
@Composable
fun MissionGateDialog(mission: MissionConfig, title: String, onPassed: () -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val runnable = remember(mission) { runnableMission(context, mission) }
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        WakerooTheme(dark = true) {
            SkyBackground(Sky.Night) {
                Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(Space.xl)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Lock, null, tint = MorningGold)
                        Spacer(Modifier.padding(start = Space.sm))
                        Column(Modifier.weight(1f)) {
                            Text(tr("HARDER MODE"), style = MaterialTheme.typography.labelSmall, color = MorningGold)
                            Text(title, style = MaterialTheme.typography.titleMedium, color = Paper)
                        }
                        Icon(Icons.Rounded.Close, tr("Keep the alarm as it is"), tint = Paper, modifier = Modifier.clip(CircleShape).clickable(onClick = onCancel).padding(Space.sm))
                    }
                    Text(
                        tr("Finish the mission first. It's what stops a sleepy you from cancelling a wide-awake plan."),
                        style = MaterialTheme.typography.bodySmall, color = PaperMuted, modifier = Modifier.padding(top = Space.sm),
                    )
                    Spacer(Modifier.height(Space.lg))
                    Box(Modifier.weight(1f)) {
                        when (runnable.type) {
                            MissionType.Math -> MathMission(runnable, {}, {}, onPassed)
                            MissionType.Typing -> TypingMission(runnable, {}, {}, onPassed)
                            MissionType.Shake -> ShakeMission(runnable, {}, {}, onPassed)
                            MissionType.Scan -> ScanMission(runnable, {}, {}, onPassed)
                            MissionType.Photo -> PhotoMission(runnable, {}, onPassed)
                            MissionType.Steps -> StepsMission(runnable, {}, {}, onPassed)
                            MissionType.Memory -> MemoryMission(runnable, {}, {}, onPassed)
                            MissionType.Nfc -> NfcMission(runnable, {}, {}, onPassed)
                            MissionType.Plan -> PlanMission(runnable, {}, {}, onPassed)
                            MissionType.Hunt -> HuntMission(runnable, {}, {}, onPassed)
                        }
                    }
                    Text(tr("Changed your mind? Close it and nothing changes."), style = MaterialTheme.typography.labelSmall, color = DawnLavender, modifier = Modifier.padding(top = Space.sm))
                }
            }
        }
    }
}
