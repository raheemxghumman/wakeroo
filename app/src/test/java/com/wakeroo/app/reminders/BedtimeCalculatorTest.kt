package com.wakeroo.app.reminders

import com.wakeroo.app.data.model.Alarm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class BedtimeCalculatorTest {
    private val zone = ZoneId.systemDefault()
    private fun at(day: Int, hour: Int, minute: Int = 0) =
        LocalDateTime.of(2026, 10, day, hour, minute).atZone(zone).toInstant().toEpochMilli()

    @Test fun bedtimeIsAlarmMinusGoal() {
        val alarm = Alarm(hour = 6, minute = 45, days = DayOfWeek.entries.toSet())
        val b = BedtimeCalculator.next(listOf(alarm), goalMinutes = 480, windDownMinutes = 30, now = at(20, 18))!!
        assertEquals(at(21, 6, 45), b.alarmAt)
        assertEquals(at(20, 22, 45), b.bedAt)
        assertEquals(at(20, 22, 15), b.remindAt)
    }

    @Test fun noReminderWhenAlarmIsSoon() {
        val alarm = Alarm(hour = 6, minute = 0)
        assertNull(BedtimeCalculator.next(listOf(alarm), 480, 30, now = at(21, 2)))
    }

    @Test fun disabledAlarmsDontCount() {
        val alarm = Alarm(hour = 7, minute = 0, enabled = false)
        assertNull(BedtimeCalculator.next(listOf(alarm), 480, 30, now = at(20, 18)))
    }

    @Test fun earliestAlarmWins() {
        val a = Alarm(id = 1, hour = 8, minute = 0)
        val b = Alarm(id = 2, hour = 6, minute = 30)
        assertEquals(at(21, 6, 30), BedtimeCalculator.next(listOf(a, b), 450, 0, now = at(20, 20))!!.alarmAt)
    }

    @Test fun noAlarmTomorrowDetection() {
        val tuesdayOnly = Alarm(hour = 7, minute = 0, days = setOf(DayOfWeek.TUESDAY))
        // 20 Oct 2026 is a Tuesday: at 8 PM the next Tuesday alarm is a week away.
        assertEquals(DayOfWeek.TUESDAY, LocalDate.of(2026, 10, 20).dayOfWeek)
        assertFalse(BedtimeCalculator.hasAlarmWithinADay(listOf(tuesdayOnly), at(20, 20)))
        assertTrue(BedtimeCalculator.hasAlarmWithinADay(listOf(tuesdayOnly), at(26, 20)))
    }
}
