package com.wakeroo.app.alarm.ring

import android.content.Context
import com.wakeroo.app.ui.i18n.AppLocale
import com.wakeroo.app.ui.i18n.tr
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wakeroo.app.container
import com.wakeroo.app.data.RingKind
import com.wakeroo.app.missions.MissionFlow
import com.wakeroo.app.missions.HarderMode
import com.wakeroo.app.missions.MissionGateDialog
import com.wakeroo.app.missions.runnableMission
import com.wakeroo.app.ui.theme.WakerooTheme
import com.wakeroo.app.ui.scenes.Scene
import kotlinx.coroutines.launch

/**
 * The alarm screen. Shows over the lock screen, turns the screen on, swallows Back and the
 * volume keys, and asks the service to re-raise it if the user leaves.
 */
class RingActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(AppLocale.wrap(newBase))

    private var leavingOnPurpose = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge(SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT))

        // Credential storage may still be locked right after a reboot; defaults are safe then.
        val settings = runCatching { kotlinx.coroutines.runBlocking { container.settings.current() } }.getOrNull()
        val showGoodMorning = settings?.showGoodMorning ?: true
        val ringPrefs = container.directBoot.ringPrefs
        val timeLimit = ringPrefs.missionTimeLimit

        setContent {
            WakerooTheme(dark = true) {
                val state by RingSession.state.collectAsStateWithLifecycle()
                var phase by remember { mutableStateOf(RingPhase.Ringing) }
                var finished by remember { mutableStateOf<FinishedRing?>(null) }
                var snoozeGate by remember { mutableStateOf(false) }
                BackHandler { /* The way out is the mission. */ }

                val current = state
                LaunchedEffect(current, finished) {
                    if (current == null && finished == null) closeScreen()
                }
                if (snoozeGate) {
                    MissionGateDialog(
                        HarderMode.SNOOZE_MISSION, tr("Snooze for {0} minutes", current?.alarm?.snoozeMinutes ?: 5),
                        onPassed = { snoozeGate = false; RingService.send(this, RingService.ACTION_SNOOZE) },
                        onCancel = { snoozeGate = false },
                    )
                }
                when {
                    finished != null -> GoodMorningScreen(finished!!, onDone = ::closeScreen)
                    current == null -> Unit
                    current.kind == RingKind.WakeCheck -> WakeCheckScreen(current) {
                        RingService.send(this, RingService.ACTION_WAKE_CHECK_PASSED)
                    }
                    phase == RingPhase.Ringing -> RingScreen(
                        state = current,
                        scene = Scene.byId(current.alarm.scene ?: ringPrefs.ringScene),
                        onStartMission = {
                            RingService.send(this, RingService.ACTION_ENGAGED)
                            phase = RingPhase.Mission
                        },
                        onSnooze = {
                            // Harder Mode: even a snooze takes one quick sum.
                            if (current.alarm.hardMode) snoozeGate = true else RingService.send(this, RingService.ACTION_SNOOZE)
                        },
                    )
                    else -> MissionFlow(
                        state = current,
                        onActivity = {
                            RingService.send(this, RingService.ACTION_SOLVING) { putExtra(RingService.EXTRA_HOLD_MS, (if (timeLimit > 0) timeLimit else 20) * 1000L) }
                        },
                        timeLimitSeconds = timeLimit,
                        onIdle = { RingService.send(this, RingService.ACTION_IDLE) },
                        onPlan = { plan ->
                            val today = java.time.LocalDate.now().toString()
                            container.appScope.launch {
                                runCatching { container.settings.update { it.copy(plan = plan, planDate = today) } }
                            }
                        },
                        onDone = {
                            val seconds = ((System.currentTimeMillis() - current.firedAt) / 1000).toInt()
                            RingService.send(this, RingService.ACTION_COMPLETE) { putExtra(RingService.EXTRA_SECONDS, seconds) }
                            if (current.kind == RingKind.Alarm) recordMissions(current)
                            val result = FinishedRing(current, seconds, escaped = false)
                            val celebrate = (showGoodMorning && current.kind == RingKind.Alarm) || current.kind == RingKind.Test
                            if (celebrate) finished = result else closeScreen()
                        },
                        onEscape = {
                            RingService.send(this, RingService.ACTION_ESCAPE)
                            closeScreen()
                        },
                    )
                }
            }
        }
    }

    /** Counts the missions just completed, for the medals (credential storage permitting). */
    private fun recordMissions(state: RingState) {
        val types = state.alarm.missions.map { runnableMission(this, it).type.name }
        container.appScope.launch {
            runCatching {
                container.settings.update { s ->
                    s.copy(missionCounts = types.fold(s.missionCounts) { acc, t -> acc + (t to ((acc[t] ?: 0) + 1)) })
                }
            }
        }
    }

    private fun closeScreen() {
        leavingOnPurpose = true
        finishAndRemoveTask()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_MUTE -> true
        else -> super.onKeyDown(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        if (RingSession.state.value != null) RingService.send(this, RingService.ACTION_SCREEN_SHOWN)
    }

    override fun onStop() {
        super.onStop()
        if (!leavingOnPurpose && !isChangingConfigurations && RingSession.state.value != null) {
            RingService.send(this, RingService.ACTION_SCREEN_HIDDEN)
        }
    }
}

enum class RingPhase { Ringing, Mission }

data class FinishedRing(val state: RingState, val secondsToWake: Int, val escaped: Boolean)
