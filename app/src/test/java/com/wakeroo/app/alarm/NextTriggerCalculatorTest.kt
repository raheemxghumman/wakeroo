package com.wakeroo.app.alarm

import com.wakeroo.app.data.model.Alarm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.TUESDAY
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class NextTriggerCalculatorTest {
    private val karachi = ZoneId.of("Asia/Karachi")
    private val newYork = ZoneId.of("America/New_York")

    // Tuesday 20 October 2026
    private fun at(y: Int, mo: Int, d: Int, h: Int, mi: Int, zone: ZoneId = karachi) =
        ZonedDateTime.of(LocalDateTime.of(y, mo, d, h, mi), zone)

    private fun alarm(hour: Int, minute: Int, block: Alarm.() -> Alarm = { this }) = Alarm(hour = hour, minute = minute).block()

    @Test fun onceLaterToday() {
        assertEquals(at(2026, 10, 20, 7, 30), NextTriggerCalculator.next(alarm(7, 30), at(2026, 10, 20, 6, 0)))
    }

    @Test fun onceAlreadyPassedRollsToTomorrow() {
        assertEquals(at(2026, 10, 21, 7, 30), NextTriggerCalculator.next(alarm(7, 30), at(2026, 10, 20, 8, 0)))
    }

    @Test fun exactlyNowIsNotNext() {
        assertEquals(at(2026, 10, 21, 7, 30), NextTriggerCalculator.next(alarm(7, 30), at(2026, 10, 20, 7, 30)))
    }

    @Test fun disabledNeverRings() {
        assertNull(NextTriggerCalculator.next(alarm(7, 0) { copy(enabled = false) }, at(2026, 10, 20, 6, 0)))
    }

    @Test fun weekdaysSkipTheWeekend() {
        val a = alarm(7, 0) { copy(days = setOf(MONDAY, TUESDAY, FRIDAY)) }
        // Friday 23 Oct after 7:00 -> next is Monday 26 Oct
        assertEquals(at(2026, 10, 26, 7, 0), NextTriggerCalculator.next(a, at(2026, 10, 23, 9, 0)))
    }

    @Test fun repeatingTodayBeforeTime() {
        val a = alarm(7, 0) { copy(days = setOf(TUESDAY)) }
        assertEquals(at(2026, 10, 20, 7, 0), NextTriggerCalculator.next(a, at(2026, 10, 20, 5, 0)))
    }

    @Test fun skipNextJumpsAWeek() {
        val a = alarm(7, 0) { copy(days = setOf(TUESDAY), skipDate = LocalDate.of(2026, 10, 20)) }
        assertEquals(at(2026, 10, 27, 7, 0), NextTriggerCalculator.next(a, at(2026, 10, 20, 5, 0)))
    }

    @Test fun oneOffDateInFuture() {
        val a = alarm(6, 15) { copy(date = LocalDate.of(2026, 12, 24)) }
        assertEquals(at(2026, 12, 24, 6, 15), NextTriggerCalculator.next(a, at(2026, 10, 20, 5, 0)))
    }

    @Test fun oneOffDateInPastNeverRings() {
        val a = alarm(6, 15) { copy(date = LocalDate.of(2026, 10, 1)) }
        assertNull(NextTriggerCalculator.next(a, at(2026, 10, 20, 5, 0)))
    }

    @Test fun weekendOnly() {
        val a = alarm(9, 0) { copy(days = setOf(SATURDAY, SUNDAY)) }
        assertEquals(at(2026, 10, 24, 9, 0), NextTriggerCalculator.next(a, at(2026, 10, 20, 10, 0)))
    }

    @Test fun springForwardGapMovesToValidTime() {
        // New York skips 2:00-3:00 on 8 March 2026; a 2:30 alarm rings at 3:30 local.
        val a = alarm(2, 30) { copy(date = LocalDate.of(2026, 3, 8)) }
        val result = NextTriggerCalculator.next(a, at(2026, 3, 7, 22, 0, newYork))!!
        assertEquals(LocalDateTime.of(2026, 3, 8, 3, 30), result.toLocalDateTime())
    }

    @Test fun fallBackKeepsWallClock() {
        // 1 November 2026, clocks go back at 2:00. A 7:00 alarm still rings at 7:00 local.
        val a = alarm(7, 0) { copy(days = setOf(SUNDAY)) }
        val result = NextTriggerCalculator.next(a, at(2026, 10, 31, 23, 0, newYork))!!
        assertEquals(LocalDateTime.of(2026, 11, 1, 7, 0), result.toLocalDateTime())
    }

    @Test fun travelKeepsLocalTime() {
        val a = alarm(7, 0)
        val result = NextTriggerCalculator.next(a, at(2026, 10, 20, 5, 0, newYork))!!
        assertEquals(LocalDateTime.of(2026, 10, 20, 7, 0), result.toLocalDateTime())
        assertEquals(newYork, result.zone)
    }
}
