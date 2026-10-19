package com.wakeroo.app.alarm.ring

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
import com.wakeroo.app.ui.theme.WakerooTheme

/**
 * The alarm screen. Shows over the lock screen, turns the screen on, swallows Back and the
 * volume keys, and asks the service to re-raise it if the user leaves.
 */
class RingActivity : ComponentActivity() {
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

        val showGoodMorning = runCatching { kotlinx.coroutines.runBlocking { container.settings.current().showGoodMorning } }.getOrDefault(true)

        setContent {
            WakerooTheme(dark = true) {
                val state by RingSession.state.collectAsStateWithLifecycle()
                var phase by remember { mutableStateOf(RingPhase.Ringing) }
                var finished by remember { mutableStateOf<FinishedRing?>(null) }
                BackHandler { /* The way out is the mission. */ }

                val current = state
                LaunchedEffect(current, finished) {
                    if (current == null && finished == null) closeScreen()
                }
                when {
                    finished != null -> GoodMorningScreen(finished!!, onDone = ::closeScreen)
                    current == null -> Unit
                    current.kind == RingKind.WakeCheck -> WakeCheckScreen(current) {
                        RingService.send(this, RingService.ACTION_WAKE_CHECK_PASSED)
                    }
                    phase == RingPhase.Ringing -> RingScreen(
                        state = current,
                        onStartMission = { phase = RingPhase.Mission },
                        onSnooze = { RingService.send(this, RingService.ACTION_SNOOZE) },
                    )
                    else -> MissionFlow(
                        state = current,
                        onActivity = { RingService.send(this, RingService.ACTION_SOLVING) },
                        onDone = {
                            val seconds = ((System.currentTimeMillis() - current.firedAt) / 1000).toInt()
                            RingService.send(this, RingService.ACTION_COMPLETE) { putExtra(RingService.EXTRA_SECONDS, seconds) }
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
