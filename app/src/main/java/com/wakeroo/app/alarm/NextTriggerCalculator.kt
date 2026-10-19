package com.wakeroo.app.alarm

import com.wakeroo.app.data.model.Alarm
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Works out the next moment an alarm rings. Times are wall-clock times in the device's
 * current zone, so a 7:00 alarm stays at 7:00 after travel or a daylight-saving change.
 */
object NextTriggerCalculator {

    /** Returns the next ring time strictly after [now], or null if the alarm will never ring again. */
    fun next(alarm: Alarm, now: ZonedDateTime): ZonedDateTime? {
        if (!alarm.enabled) return null
        val time = LocalTime.of(alarm.hour, alarm.minute)
        val zone: ZoneId = now.zone

        alarm.date?.let { date ->
            val at = ZonedDateTime.of(date, time, zone)
            return at.takeIf { it.isAfter(now) }
        }

        val today = now.toLocalDate()
        if (alarm.days.isEmpty()) {
            val todayAt = ZonedDateTime.of(today, time, zone)
            return if (todayAt.isAfter(now)) todayAt else ZonedDateTime.of(today.plusDays(1), time, zone)
        }

        // A repeating alarm: the first matching weekday in the next 15 days (a skipped day
        // can push the answer past one full week).
        for (offset in 0L..14L) {
            val date = today.plusDays(offset)
            if (date.dayOfWeek !in alarm.days || date == alarm.skipDate) continue
            val at = ZonedDateTime.of(date, time, zone)
            if (at.isAfter(now)) return at
        }
        return null
    }

    fun nextMillis(alarm: Alarm, nowMillis: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): Long? =
        next(alarm, ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(nowMillis), zone))?.toInstant()?.toEpochMilli()
}
