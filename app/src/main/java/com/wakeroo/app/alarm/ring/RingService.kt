package com.wakeroo.app.alarm.ring

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import com.wakeroo.app.alarm.AlarmScheduler
import com.wakeroo.app.alarm.Notifications
import com.wakeroo.app.container
import com.wakeroo.app.data.ActiveRing
import com.wakeroo.app.data.PendingEvent
import com.wakeroo.app.data.RingKind
import com.wakeroo.app.data.db.RingOutcome
import com.wakeroo.app.data.model.Alarm
import com.wakeroo.app.data.model.MissionConfig
import com.wakeroo.app.data.model.MissionType
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Owns a ring from first sound to dismissal. Started by [com.wakeroo.app.alarm.AlarmReceiver]
 * from an exact alarm, which exempts it from background foreground-service limits.
 */
class RingService : Service() {
    private lateinit var player: AlarmPlayer
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())
    private var notificationId = Notifications.ID_RINGING
    private val escalate = Runnable { escalateWakeCheck() }
    private val giveUp = Runnable { finish(RingOutcome.Missed, secondsToWake = null) }

    override fun onCreate() {
        super.onCreate()
        player = AlarmPlayer(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> start(intent)
            ACTION_SNOOZE -> snooze()
            ACTION_SOLVING -> player.duckWhileSolving()
            ACTION_COMPLETE -> finish(RingOutcome.Dismissed, intent.getIntExtra(EXTRA_SECONDS, -1).takeIf { it >= 0 })
            ACTION_ESCAPE -> finish(RingOutcome.Escaped, secondsToWake = null)
            ACTION_WAKE_CHECK_PASSED -> passWakeCheck()
            ACTION_SCREEN_HIDDEN -> repostAlert()
            ACTION_SCREEN_SHOWN -> RingSession.state.value?.let { goForeground(it, onScreen = true) }
            else -> if (RingSession.state.value == null) stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun start(intent: Intent) {
        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
        val kind = intent.getStringExtra(AlarmScheduler.EXTRA_KIND)?.let { RingKind.valueOf(it) } ?: RingKind.Alarm
        val scheduledAt = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, System.currentTimeMillis())
        val snoozes = intent.getIntExtra(AlarmScheduler.EXTRA_SNOOZES, 0)
        val store = container.directBoot

        val current = RingSession.state.value
        if (current != null && current.kind != RingKind.WakeCheck) {
            // Already ringing: a second alarm at the same minute joins this ring and ends with it.
            store.expectedTriggers = store.expectedTriggers - alarmId
            if (alarmId != current.alarm.id && kind == RingKind.Alarm) RingSession.publish(current.copy(joined = current.joined + alarmId))
            return
        }

        store.expectedTriggers = store.expectedTriggers - alarmId
        intent.getStringExtra(AlarmScheduler.EXTRA_SLOT)?.let { slot ->
            runCatching { AlarmScheduler.Slot.valueOf(slot) }.getOrNull()?.let { container.scheduler.consumed(alarmId, it) }
        }
        val alarm = when (kind) {
            RingKind.Test -> TEST_ALARM
            else -> store.alarm(alarmId) ?: fallbackAlarm(alarmId)
        }
        val now = System.currentTimeMillis()
        val state = RingState(
            alarm = alarm, kind = kind, scheduledAt = scheduledAt, firedAt = now, snoozes = snoozes,
            wakeCheckDeadline = if (kind == RingKind.WakeCheck) now + WAKE_CHECK_WINDOW_MS else 0L,
        )
        RingSession.publish(state)
        store.activeRing = ActiveRing(alarmId, scheduledAt, now, snoozes, kind)
        goForeground(state)
        acquireWakeLock()

        val gentle = kind == RingKind.WakeCheck
        player.start(alarm.soundUri, alarm.volume, if (gentle) 20 else alarm.rampSeconds, alarm.vibrate && !gentle, gentle)
        handler.removeCallbacks(escalate)
        handler.removeCallbacks(giveUp)
        if (gentle) handler.postDelayed(escalate, WAKE_CHECK_WINDOW_MS) else handler.postDelayed(giveUp, MAX_RING_MS)
        launchScreen()
    }

    private fun goForeground(state: RingState, onScreen: Boolean = false) {
        val time = DateTimeFormatter.ofPattern("h:mm", Locale.getDefault())
            .format(Instant.ofEpochMilli(state.scheduledAt).atZone(ZoneId.systemDefault()))
        val title = when (state.kind) {
            RingKind.WakeCheck -> "Still up?"
            RingKind.Test -> "Test alarm"
            RingKind.Alarm -> state.alarm.label.ifBlank { "Wake up, it's $time" }
        }
        val text = if (state.kind == RingKind.WakeCheck) "Tap to confirm you're awake" else "Finish your mission to stop the alarm"
        val action = if (state.kind == RingKind.WakeCheck) "I'm up" else "Open mission"
        ServiceCompat.startForeground(
            this, notificationId, Notifications.ringing(this, title, text, onScreen, action),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )
    }

    /** Starting the screen directly works when allowed; otherwise the full-screen intent shows it. */
    private fun launchScreen() {
        runCatching {
            startActivity(Intent(this, RingActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION))
        }
    }

    /** The user left the ring screen (home button, recents): bring the alert straight back. */
    private fun repostAlert() {
        val state = RingSession.state.value ?: return
        notificationId = if (notificationId == Notifications.ID_RINGING) Notifications.ID_RINGING_ALT else Notifications.ID_RINGING
        goForeground(state)
        handler.postDelayed({ if (RingSession.state.value != null) launchScreen() }, 1500)
    }

    private fun snooze() {
        val state = RingSession.state.value ?: return
        if (state.snoozesLeft <= 0) return
        val at = System.currentTimeMillis() + state.alarm.snoozeMinutes * 60_000L
        container.scheduler.scheduleSnooze(state.alarm.id, at, state.scheduledAt, state.snoozes + 1)
        container.directBoot.activeRing = ActiveRing(state.alarm.id, state.scheduledAt, state.firedAt, state.snoozes + 1, state.kind)
        stopRinging()
    }

    private fun finish(outcome: RingOutcome, secondsToWake: Int?) {
        val state = RingSession.state.value ?: return stopRinging()
        val app = container
        app.directBoot.activeRing = null
        app.scheduler.cancelExtras(state.alarm.id)
        val now = System.currentTimeMillis()
        if (state.kind == RingKind.Alarm && outcome == RingOutcome.Dismissed && state.alarm.wakeCheckMinutes > 0) {
            app.scheduler.scheduleWakeCheck(state.alarm.id, now + state.alarm.wakeCheckMinutes * 60_000L, state.scheduledAt)
        }
        val event = PendingEvent(
            alarmId = state.alarm.id, label = state.alarm.label, scheduledAt = state.scheduledAt, firedAt = state.firedAt,
            endedAt = now, outcome = outcome, snoozes = state.snoozes, secondsToWake = secondsToWake,
            wakeCheckPassed = if (state.kind == RingKind.WakeCheck) outcome == RingOutcome.Dismissed else null,
            isTest = state.kind == RingKind.Test,
        )
        app.appScope.launch {
            app.repository.log(event)
            when (state.kind) {
                RingKind.Test -> app.settings.update { it.copy(lastTestAlarmAt = now, lastTestAlarmPassed = outcome == RingOutcome.Dismissed) }
                RingKind.Alarm -> (listOf(state.alarm.id) + state.joined).forEach { app.repository.onRingFinished(it) }
                RingKind.WakeCheck -> Unit
            }
        }
        stopRinging()
    }

    private fun passWakeCheck() {
        val state = RingSession.state.value ?: return
        if (state.kind != RingKind.WakeCheck) return
        finish(RingOutcome.Dismissed, null)
    }

    /** Nobody answered "Still up?": the full alarm comes back, missions and all. */
    private fun escalateWakeCheck() {
        val state = RingSession.state.value ?: return
        if (state.kind != RingKind.WakeCheck) return
        container.repository.let { repo ->
            container.appScope.launch {
                repo.log(
                    PendingEvent(state.alarm.id, state.alarm.label, state.scheduledAt, state.firedAt, System.currentTimeMillis(),
                        RingOutcome.Missed, 0, null, wakeCheckPassed = false),
                )
            }
        }
        val next = state.copy(kind = RingKind.Alarm, firedAt = System.currentTimeMillis(), wakeCheckDeadline = 0L, snoozes = state.alarm.snoozeLimit)
        RingSession.publish(next)
        container.directBoot.activeRing = ActiveRing(state.alarm.id, state.scheduledAt, next.firedAt, next.snoozes, RingKind.Alarm)
        goForeground(next)
        player.start(state.alarm.soundUri, state.alarm.volume, 0, state.alarm.vibrate)
        handler.postDelayed(giveUp, MAX_RING_MS)
        launchScreen()
    }

    private fun stopRinging() {
        handler.removeCallbacksAndMessages(null)
        player.stop()
        RingSession.publish(null)
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wakeroo:ringing")
            .apply { acquire(MAX_RING_MS) }
    }

    override fun onDestroy() {
        player.stop()
        wakeLock?.takeIf { it.isHeld }?.release()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.wakeroo.app.ring.START"
        const val ACTION_SNOOZE = "com.wakeroo.app.ring.SNOOZE"
        const val ACTION_SOLVING = "com.wakeroo.app.ring.SOLVING"
        const val ACTION_COMPLETE = "com.wakeroo.app.ring.COMPLETE"
        const val ACTION_ESCAPE = "com.wakeroo.app.ring.ESCAPE"
        const val ACTION_WAKE_CHECK_PASSED = "com.wakeroo.app.ring.WAKE_CHECK_PASSED"
        const val ACTION_SCREEN_HIDDEN = "com.wakeroo.app.ring.SCREEN_HIDDEN"
        const val ACTION_SCREEN_SHOWN = "com.wakeroo.app.ring.SCREEN_SHOWN"
        const val EXTRA_SECONDS = "seconds"

        const val WAKE_CHECK_WINDOW_MS = 60_000L
        private const val MAX_RING_MS = 60 * 60_000L

        val TEST_ALARM = Alarm(
            id = AlarmScheduler.TEST_ALARM_ID, hour = 0, minute = 0, label = "Test alarm",
            missions = listOf(MissionConfig(MissionType.Math, difficulty = 1, count = 1)),
            rampSeconds = 5, snoozeLimit = 0, wakeCheckMinutes = 0,
        )

        /** An alarm we can't find (deleted mid-flight, corrupt mirror) still rings. */
        private fun fallbackAlarm(id: Long) = Alarm(id = id, hour = 0, minute = 0, label = "")

        fun send(context: Context, action: String, extras: Intent.() -> Unit = {}) {
            context.startService(Intent(context, RingService::class.java).setAction(action).apply(extras))
        }
    }
}
