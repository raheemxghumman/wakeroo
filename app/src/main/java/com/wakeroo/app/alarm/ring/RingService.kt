package com.wakeroo.app.alarm.ring

import com.wakeroo.app.ui.i18n.tr
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
import com.wakeroo.app.data.model.SoundMode
import com.wakeroo.app.alarm.sound.Tone
import com.wakeroo.app.sleep.SleepPlayer
import com.wakeroo.app.tile.PowerNapTileService
import com.wakeroo.app.weather.WeatherNotifier
import androidx.core.os.UserManagerCompat
import com.wakeroo.app.alarm.sound.Tones
import android.net.Uri
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
    private val escalateSound = Runnable { escalateSound() }
    private var currentTone: Tone? = null

    override fun onCreate() {
        super.onCreate()
        player = AlarmPlayer(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> start(intent)
            ACTION_SNOOZE -> snooze()
            ACTION_SOLVING -> {
                cancelEscalation()
                player.duckWhileSolving(intent.getLongExtra(EXTRA_HOLD_MS, 20_000L))
            }
            ACTION_ENGAGED -> cancelEscalation()
            ACTION_IDLE -> player.unduck()
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
        // Sleep sounds stop a minute early anyway; this is the belt to that braces.
        SleepPlayer.stop(this)
        store.activeRing = ActiveRing(alarmId, scheduledAt, now, snoozes, kind)
        goForeground(state)
        acquireWakeLock()

        val gentle = kind == RingKind.WakeCheck
        player.start(soundFor(alarm), alarm.volume, if (gentle) 20 else alarm.rampSeconds, alarm.vibrate && !gentle, gentle, route())
        handler.removeCallbacks(escalate)
        handler.removeCallbacks(giveUp)
        handler.removeCallbacks(escalateSound)
        if (gentle) handler.postDelayed(escalate, WAKE_CHECK_WINDOW_MS) else handler.postDelayed(giveUp, MAX_RING_MS)
        if (kind == RingKind.Alarm && alarm.soundMode == SoundMode.Escalate) {
            handler.postDelayed(escalateSound, ESCALATE_AFTER_MS)
            RingSession.publish(state.copy(escalateAt = now + ESCALATE_AFTER_MS))
        }
        launchScreen()
    }

    /** The user's own sound if set; otherwise today's bundled tone (rotation picks a fresh one). */
    private fun soundFor(alarm: Alarm): Uri {
        alarm.soundUri?.let { currentTone = null; return Uri.parse(it) }
        val store = container.directBoot
        val tone = Tones.forRing(alarm, store.lastTone(alarm.id))
        store.setLastTone(alarm.id, tone.id)
        currentTone = tone
        return Tones.uri(this, tone)
    }

    private fun route(): SoundRoute {
        val prefs = container.directBoot.ringPrefs
        return when {
            !prefs.alwaysSpeaker -> SoundRoute.FollowPhone
            prefs.alsoHeadphones -> SoundRoute.SpeakerAndHeadphones
            else -> SoundRoute.SpeakerOnly
        }
    }

    /** A minute with no response: crossfade to an Extreme tone at full volume. */
    private fun escalateSound() {
        val state = RingSession.state.value ?: return
        val loud = currentTone?.let(Tones::escalationFor) ?: Tones.byId("klaxon")
        player.crossfadeTo(Tones.uri(this, loud))
        RingSession.publish(state.copy(escalateAt = 0L, escalated = true))
    }

    private fun cancelEscalation() {
        handler.removeCallbacks(escalateSound)
        RingSession.state.value?.takeIf { it.escalateAt > 0 }?.let { RingSession.publish(it.copy(escalateAt = 0L)) }
    }

    private fun goForeground(state: RingState, onScreen: Boolean = false) {
        val time = DateTimeFormatter.ofPattern("h:mm", Locale.getDefault())
            .format(Instant.ofEpochMilli(state.scheduledAt).atZone(ZoneId.systemDefault()))
        val title = when (state.kind) {
            RingKind.WakeCheck -> tr("Still up?")
            RingKind.Test -> tr("Test alarm")
            RingKind.Alarm -> state.alarm.label.ifBlank { tr("Wake up, it's {0}", time) }
        }
        val text = if (state.kind == RingKind.WakeCheck) tr("Tap to confirm you're awake") else tr("Finish your mission to stop the alarm")
        val action = if (state.kind == RingKind.WakeCheck) tr("I'm up") else tr("Open mission")
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
                RingKind.Alarm -> {
                    (listOf(state.alarm.id) + state.joined).forEach { app.repository.onRingFinished(it) }
                    if (outcome == RingOutcome.Dismissed && UserManagerCompat.isUserUnlocked(this@RingService)) {
                        runCatching { WeatherNotifier.afterWakeUp(this@RingService) }
                    }
                }
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
        player.start(soundFor(state.alarm), state.alarm.volume, 0, state.alarm.vibrate, route = route())
        handler.postDelayed(giveUp, MAX_RING_MS)
        launchScreen()
    }

    private fun stopRinging() {
        PowerNapTileService.requestRefresh(this)
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
        const val ACTION_IDLE = "com.wakeroo.app.ring.IDLE"
        const val ACTION_ENGAGED = "com.wakeroo.app.ring.ENGAGED"
        const val EXTRA_SECONDS = "seconds"
        const val EXTRA_HOLD_MS = "hold_ms"

        const val WAKE_CHECK_WINDOW_MS = 60_000L
        private const val MAX_RING_MS = 60 * 60_000L
        const val ESCALATE_AFTER_MS = 60_000L

        val TEST_ALARM = Alarm(
            id = AlarmScheduler.TEST_ALARM_ID, hour = 0, minute = 0, label = tr("Test alarm"),
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
