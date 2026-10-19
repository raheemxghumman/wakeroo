package com.wakeroo.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.AlarmManagerCompat
import com.wakeroo.app.MainActivity
import com.wakeroo.app.data.DirectBootStore
import com.wakeroo.app.data.PendingExtra
import com.wakeroo.app.data.RingKind
import com.wakeroo.app.data.model.Alarm

/**
 * Registers alarms with AlarmManager. Every ring, including snoozes, wake-up checks and the
 * post-reboot re-ring, goes through setAlarmClock: it fires on time in Doze, shows in the
 * status bar and lets us start the ringing service from the background.
 */
class AlarmScheduler(private val context: Context, private val store: DirectBootStore) {
    private val am = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()

    /** Idempotent: computes every alarm's next ring, registers it, and cancels stale entries. */
    fun rescheduleAll(alarms: List<Alarm>, now: Long = System.currentTimeMillis()) {
        store.writeAlarms(alarms)
        val expected = store.expectedTriggers.toMutableMap()
        val scheduled = mutableSetOf<Long>()
        for (alarm in alarms) {
            val at = NextTriggerCalculator.nextMillis(alarm, now)
            if (at == null) {
                cancel(alarm.id, Slot.Main)
                expected.remove(alarm.id)
                continue
            }
            set(at, alarm.id, Slot.Main, RingKind.Alarm, scheduledAt = at)
            expected[alarm.id] = at
            scheduled += alarm.id
        }
        for (stale in store.scheduledIds - scheduled) {
            cancel(stale, Slot.Main)
            expected.remove(stale)
        }
        store.scheduledIds = scheduled
        store.expectedTriggers = expected.filterKeys { it in scheduled }
    }

    fun scheduleSnooze(alarmId: Long, at: Long, scheduledAt: Long, snoozes: Int) =
        set(at, alarmId, Slot.Snooze, RingKind.Alarm, scheduledAt, snoozes)

    fun scheduleWakeCheck(alarmId: Long, at: Long, scheduledAt: Long) =
        set(at, alarmId, Slot.WakeCheck, RingKind.WakeCheck, scheduledAt)

    /** After a reboot during a ring, ring again shortly through the normal exact-alarm path. */
    fun scheduleReRing(alarmId: Long, scheduledAt: Long, snoozes: Int, delayMs: Long = 60_000) =
        set(System.currentTimeMillis() + delayMs, alarmId, Slot.ReRing, RingKind.Alarm, scheduledAt, snoozes)

    fun scheduleTest(at: Long) = set(at, TEST_ALARM_ID, Slot.Main, RingKind.Test, at)

    fun cancelExtras(alarmId: Long) {
        cancel(alarmId, Slot.Snooze)
        cancel(alarmId, Slot.WakeCheck)
        cancel(alarmId, Slot.ReRing)
    }

    /** AlarmManager forgets everything on reboot and app update; put the one-off rings back. */
    fun restoreExtras(now: Long = System.currentTimeMillis()) {
        for (e in store.pendingExtras) {
            val slot = runCatching { Slot.valueOf(e.slot) }.getOrNull() ?: continue
            if (e.at > now) set(e.at, e.alarmId, slot, e.kind, e.scheduledAt, e.snoozes)
        }
    }

    /** A ring fired; it no longer needs restoring. */
    fun consumed(alarmId: Long, slot: Slot) {
        store.pendingExtras = store.pendingExtras.filterNot { it.alarmId == alarmId && it.slot == slot.name }
    }

    fun cancelAll(alarmId: Long) {
        Slot.entries.forEach { cancel(alarmId, it) }
    }

    private fun set(at: Long, alarmId: Long, slot: Slot, kind: RingKind, scheduledAt: Long, snoozes: Int = 0) {
        if (slot != Slot.Main) {
            store.pendingExtras = store.pendingExtras.filterNot { it.alarmId == alarmId && it.slot == slot.name } +
                PendingExtra(alarmId, slot.name, at, kind, scheduledAt, snoozes)
        }
        val operation = ringIntent(alarmId, slot, kind, scheduledAt, snoozes)
        if (canScheduleExact()) {
            AlarmManagerCompat.setAlarmClock(am, at, showIntent(), operation)
        } else {
            // Rare: the exact-alarm permission is missing. The health check flags this in red.
            AlarmManagerCompat.setExactAndAllowWhileIdle(am, AlarmManager.RTC_WAKEUP, at, operation)
        }
    }

    private fun cancel(alarmId: Long, slot: Slot) {
        if (slot != Slot.Main) consumed(alarmId, slot)
        val pi = PendingIntent.getBroadcast(
            context, requestCode(alarmId, slot), Intent(context, AlarmReceiver::class.java).setAction(ACTION_RING),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        am.cancel(pi)
        pi.cancel()
    }

    private fun ringIntent(alarmId: Long, slot: Slot, kind: RingKind, scheduledAt: Long, snoozes: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(ACTION_RING)
            .putExtra(EXTRA_ALARM_ID, alarmId)
            .putExtra(EXTRA_KIND, kind.name)
            .putExtra(EXTRA_SCHEDULED_AT, scheduledAt)
            .putExtra(EXTRA_SNOOZES, snoozes)
            .putExtra(EXTRA_SLOT, slot.name)
        return PendingIntent.getBroadcast(
            context, requestCode(alarmId, slot), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun showIntent(): PendingIntent = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
    )

    enum class Slot { Main, Snooze, WakeCheck, ReRing }

    companion object {
        const val ACTION_RING = "com.wakeroo.app.action.RING"
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_KIND = "kind"
        const val EXTRA_SCHEDULED_AT = "scheduled_at"
        const val EXTRA_SNOOZES = "snoozes"
        const val EXTRA_SLOT = "slot"
        const val TEST_ALARM_ID = 999_999L

        fun requestCode(alarmId: Long, slot: Slot): Int = (alarmId * 4 + slot.ordinal).toInt()
    }
}
