package com.wakeroo.app.reminders

import com.wakeroo.app.ui.i18n.tr
import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.wakeroo.app.MainActivity
import com.wakeroo.app.R
import com.wakeroo.app.alarm.NextTriggerCalculator
import com.wakeroo.app.container
import com.wakeroo.app.data.Settings
import com.wakeroo.app.data.model.Alarm
import com.wakeroo.app.feature.main.MainTab
import com.wakeroo.app.sleep.SleepSound
import com.wakeroo.app.sleep.SleepSoundService
import com.wakeroo.app.ui.util.formatClock
import com.wakeroo.app.ui.util.formatMeridiem
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import com.wakeroo.app.ui.util.formatHours

/** When to go to bed for the next alarm, or null if there's no alarm worth planning for. */
data class Bedtime(val bedAt: Long, val alarmAt: Long, val remindAt: Long)

object BedtimeCalculator {
    /** Below this, a reminder would be pointless (a nap, or an alarm in the small hours). */
    const val MIN_GAP_MS = 5 * 3_600_000L

    fun next(alarms: List<Alarm>, goalMinutes: Int, windDownMinutes: Int, now: Long): Bedtime? {
        val nextAlarm = alarms.mapNotNull { NextTriggerCalculator.nextMillis(it, now) }.minOrNull() ?: return null
        if (nextAlarm - now < MIN_GAP_MS) return null
        val bed = nextAlarm - goalMinutes * 60_000L
        return Bedtime(bed, nextAlarm, bed - windDownMinutes * 60_000L)
    }

    /** True if any alarm rings between [from] and 24 hours later. */
    fun hasAlarmWithinADay(alarms: List<Alarm>, from: Long): Boolean =
        alarms.mapNotNull { NextTriggerCalculator.nextMillis(it, from) }.any { it - from <= 24 * 3_600_000L }
}

/**
 * Bedtime and "no alarm for tomorrow" reminders. They use inexact alarms (a reminder doesn't
 * need to-the-minute timing, and exact alarms stay reserved for real ones) and a separate
 * notification channel, so muting reminders never touches alarm alerts.
 */
object Reminders {
    const val CHANNEL = "reminders"
    private const val ID_BEDTIME = 20
    private const val ID_NO_ALARM = 21
    private const val RC_BEDTIME = 50
    private const val RC_NO_ALARM = 51

    fun createChannel(context: Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, tr("Reminders"), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = tr("Bedtime and \"no alarm for tomorrow\" reminders. Muting these never mutes alarms.")
            },
        )
    }

    /** Recomputes both reminders from the current alarms and settings. Safe to call often. */
    fun reschedule(context: Context, alarms: List<Alarm>, settings: Settings, now: Long = System.currentTimeMillis()) {
        val am = context.getSystemService(AlarmManager::class.java)
        val bedtime = pending(context, ACTION_BEDTIME, RC_BEDTIME)
        am.cancel(bedtime)
        if (settings.bedtimeReminder) {
            BedtimeCalculator.next(alarms, settings.sleepGoalMinutes, settings.windDownMinutes, now)
                ?.takeIf { it.remindAt > now }
                ?.let { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, it.remindAt, bedtime) }
        }
        val check = pending(context, ACTION_NO_ALARM_CHECK, RC_NO_ALARM)
        am.cancel(check)
        if (settings.noAlarmReminder) {
            val zone = ZoneId.systemDefault()
            val time = LocalTime.of(settings.noAlarmReminderAt / 60, settings.noAlarmReminderAt % 60)
            var at = ZonedDateTime.of(LocalDate.now(zone), time, zone)
            if (!at.toInstant().isAfter(Instant.ofEpochMilli(now))) at = at.plusDays(1)
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toInstant().toEpochMilli(), check)
        }
    }

    private fun pending(context: Context, action: String, code: Int): PendingIntent = PendingIntent.getBroadcast(
        context, code, Intent(context, ReminderReceiver::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun canNotify(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun showBedtime(context: Context, bed: Bedtime, settings: Settings) {
        if (!canNotify(context)) return
        val zone = ZoneId.systemDefault()
        val bedTime = Instant.ofEpochMilli(bed.bedAt).atZone(zone).toLocalTime()
        val alarmTime = Instant.ofEpochMilli(bed.alarmAt).atZone(zone).toLocalTime()
        val hoursText = formatHours(settings.sleepGoalMinutes)
        val sounds = settings.sleepSounds.mapNotNull { SleepSound.byId(it) }.ifEmpty { listOf(SleepSound.Rain) }
        val startSounds = PendingIntent.getForegroundService(
            context, 52,
            Intent(context, SleepSoundService::class.java).setAction(SleepSoundService.ACTION_START)
                .putExtra(SleepSoundService.EXTRA_SOUNDS, sounds.take(2).joinToString(",") { it.id })
                .putExtra(SleepSoundService.EXTRA_MINUTES, settings.sleepTimerMinutes),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val snooze = PendingIntent.getBroadcast(
            context, 53, Intent(context, ReminderReceiver::class.java).setAction(ACTION_BEDTIME_SNOOZE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_wakeroo)
            .setContentTitle(tr("Time to wind down"))
            .setContentText(tr("Bed by {0} {1} for {2} before your {3} {4} alarm.", bedTime.formatClock(), bedTime.formatMeridiem(), hoursText, alarmTime.formatClock(), alarmTime.formatMeridiem()))
            .setContentIntent(openTab(context, MainTab.Sleep, 54))
            .addAction(0, tr("Start sleep sounds"), startSounds)
            .addAction(0, tr("In 15 min"), snooze)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(ID_BEDTIME, n)
    }

    fun showNoAlarm(context: Context, template: Alarm?) {
        if (!canNotify(context)) return
        val builder = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_wakeroo)
            .setContentTitle(tr("No alarm for tomorrow"))
            .setContentText(if (template != null) tr("Want your usual {0} {1}?", formatClock(template.hour, template.minute), formatMeridiem(template.hour)) else tr("Set one in a few seconds."))
            .setContentIntent(openTab(context, MainTab.Alarms, 55))
            .setAutoCancel(true)
        if (template != null) {
            val set = PendingIntent.getBroadcast(
                context, 56, Intent(context, ReminderReceiver::class.java).setAction(ACTION_SET_TOMORROW),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(0, tr("Set {0} {1}", formatClock(template.hour, template.minute), formatMeridiem(template.hour)), set)
        }
        val dismiss = PendingIntent.getBroadcast(
            context, 57, Intent(context, ReminderReceiver::class.java).setAction(ACTION_DISMISS),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        builder.addAction(0, tr("Not tomorrow"), dismiss)
        NotificationManagerCompat.from(context).notify(ID_NO_ALARM, builder.build())
    }

    fun cancelNoAlarm(context: Context) = NotificationManagerCompat.from(context).cancel(ID_NO_ALARM)

    fun cancelBedtime(context: Context) = NotificationManagerCompat.from(context).cancel(ID_BEDTIME)

    private fun openTab(context: Context, tab: MainTab, code: Int) = PendingIntent.getActivity(
        context, code, Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_TAB, tab.name),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    const val ACTION_BEDTIME = "com.wakeroo.app.reminder.BEDTIME"
    const val ACTION_BEDTIME_SNOOZE = "com.wakeroo.app.reminder.BEDTIME_SNOOZE"
    const val ACTION_NO_ALARM_CHECK = "com.wakeroo.app.reminder.NO_ALARM_CHECK"
    const val ACTION_SET_TOMORROW = "com.wakeroo.app.reminder.SET_TOMORROW"
    const val ACTION_DISMISS = "com.wakeroo.app.reminder.DISMISS"
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.container
        val pending = goAsync()
        app.appScope.launch {
            try {
                val settings = app.settings.current()
                val alarms = app.repository.all()
                val now = System.currentTimeMillis()
                when (intent.action) {
                    Reminders.ACTION_BEDTIME -> BedtimeCalculator.next(alarms, settings.sleepGoalMinutes, settings.windDownMinutes, now)
                        ?.let { Reminders.showBedtime(context, it, settings) }
                    Reminders.ACTION_BEDTIME_SNOOZE -> {
                        Reminders.cancelBedtime(context)
                        val am = context.getSystemService(AlarmManager::class.java)
                        am.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP, now + 15 * 60_000L,
                            PendingIntent.getBroadcast(
                                context, 58, Intent(context, ReminderReceiver::class.java).setAction(Reminders.ACTION_BEDTIME),
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                            ),
                        )
                    }
                    Reminders.ACTION_NO_ALARM_CHECK -> if (!BedtimeCalculator.hasAlarmWithinADay(alarms, now)) {
                        Reminders.showNoAlarm(context, app.repository.latest())
                    }
                    Reminders.ACTION_SET_TOMORROW -> {
                        Reminders.cancelNoAlarm(context)
                        app.repository.latest()?.let { usual ->
                            val tomorrow = LocalDate.now().plusDays(1)
                            app.repository.save(
                                usual.copy(id = 0, days = emptySet(), date = tomorrow, enabled = true, skipDate = null, createdAt = now),
                            )
                        }
                    }
                    Reminders.ACTION_DISMISS -> Reminders.cancelNoAlarm(context)
                }
                // Line up the next ones (tomorrow's check, the next bedtime).
                Reminders.reschedule(context, app.repository.all(), settings)
            } finally {
                pending.finish()
            }
        }
    }
}
