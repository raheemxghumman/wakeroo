package com.wakeroo.app.alarm

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.wakeroo.app.alarm.ring.RingService
import com.wakeroo.app.container
import com.wakeroo.app.data.PendingEvent
import com.wakeroo.app.data.db.RingOutcome
import com.wakeroo.app.widget.NextAlarmWidget
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.wakeroo.app.data.RingKind

/** Receives every exact alarm and hands it to the foreground ringing service. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmScheduler.ACTION_RING) return
        val service = Intent(context, RingService::class.java)
            .setAction(RingService.ACTION_START)
            .putExtras(intent)
        ContextCompat.startForegroundService(context, service)
    }
}

/**
 * Re-registers alarms whenever the system forgets them or the clock changes. It never rings
 * directly: Android 15+ blocks starting a media service from boot, so an interrupted ring is
 * re-armed through setAlarmClock a minute later instead.
 */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.container
        val pending = goAsync()
        app.appScope.launch {
            try {
                when (intent.action) {
                    Intent.ACTION_USER_UNLOCKED -> app.repository.reconcileAfterUnlock()
                    else -> handle(context, intent.action)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun handle(context: Context, action: String?) {
        val app = context.container
        val store = app.directBoot
        val now = System.currentTimeMillis()
        val booted = action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_LOCKED_BOOT_COMPLETED
        // An update kills the process too, so an alarm that was ringing must ring again.
        val interrupted = booted || action == Intent.ACTION_MY_PACKAGE_REPLACED

        // Anything still "expected" more than a minute ago never fired.
        val alarms = store.readAlarms()
        store.expectedTriggers.filter { (_, at) -> at < now - 60_000 }.forEach { (id, at) ->
            val alarm = alarms.firstOrNull { it.id == id } ?: return@forEach
            val whenText = DateTimeFormatter.ofPattern("h:mm a").format(Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()))
            Notifications.missed(context, alarm.label, whenText)
            app.repository.log(
                PendingEvent(id, alarm.label, at, at, null, RingOutcome.Missed, 0, null, null),
            )
        }

        app.scheduler.restoreExtras(now)
        if (interrupted) {
            store.activeRing?.let { ring ->
                app.scheduler.scheduleReRing(
                    ring.alarmId, ring.scheduledAt, ring.snoozes, delayMs = if (booted) 60_000 else 5_000,
                    kind = if (ring.kind == RingKind.Test) RingKind.Test else RingKind.Alarm,
                )
            }
        }
        app.scheduler.rescheduleAll(alarms, now)
        runCatching { NextAlarmWidget.refresh(context) }
    }

    companion object {
        val ACTIONS = setOf(
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            Intent.ACTION_USER_UNLOCKED,
        )
    }
}
