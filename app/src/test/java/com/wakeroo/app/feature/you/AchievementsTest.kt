package com.wakeroo.app.feature.you

import com.wakeroo.app.data.db.RingEventEntity
import com.wakeroo.app.data.db.RingOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class AchievementsTest {
    private val zone = ZoneId.systemDefault()
    private val today = LocalDate.of(2026, 10, 20)
    private var nextId = 1L

    private fun win(daysAgo: Long, hour: Int = 7, snoozes: Int = 0, seconds: Int = 90, outcome: RingOutcome = RingOutcome.Dismissed): RingEventEntity {
        val at = today.minusDays(daysAgo).atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()
        return RingEventEntity(nextId++, 1, "", at, at, at + seconds * 1000L, outcome, snoozes, seconds, null)
    }

    private fun states(events: List<RingEventEntity>, counts: Map<String, Int> = emptyMap(), routineDays: Int = 0) =
        AchievementEngine.evaluate(AchievementInput(events, counts, routineDays, today, zone)).associateBy { it.def.id }

    @Test fun nothingYet() {
        assertTrue(states(emptyList()).values.none { it.unlocked })
    }

    @Test fun firstRiseAndStreaks() {
        val s = states((0L..6L).map { win(it) })
        assertTrue(s.getValue("first_rise").unlocked)
        assertTrue(s.getValue("streak_3").unlocked)
        assertTrue(s.getValue("streak_7").unlocked)
        assertFalse(s.getValue("streak_14").unlocked)
        assertEquals(0.5f, s.getValue("streak_14").progress, 0.01f)
    }

    @Test fun aMissedDayBreaksTheCleanWeek() {
        val events = (0L..7L).map { if (it == 3L) win(it, outcome = RingOutcome.Missed) else win(it) }
        assertFalse(states(events).getValue("no_snooze_week").unlocked)
        assertTrue(states((0L..6L).map { win(it) }).getValue("no_snooze_week").unlocked)
        assertFalse(states((0L..6L).map { win(it, snoozes = if (it == 2L) 1 else 0) }).getValue("no_snooze_week").unlocked)
    }

    @Test fun earlyBirdAndQuickRiser() {
        val early = (0L..4L).map { win(it, hour = 5, seconds = 40) }
        val s = states(early)
        assertTrue(s.getValue("early_bird").unlocked)
        assertTrue(s.getValue("quick_riser").unlocked)
    }

    @Test fun missionCountsDriveExplorerAndOutOfBed() {
        val s = states(listOf(win(0)), mapOf("Math" to 3, "Scan" to 6, "Photo" to 2, "Steps" to 2, "Memory" to 1))
        assertTrue(s.getValue("mission_explorer").unlocked)
        assertTrue(s.getValue("spot_on").unlocked)
    }

    @Test fun weekendWarrior() {
        // 17 and 18 Oct 2026 are a Saturday and Sunday.
        assertTrue(states(listOf(win(3), win(2))).getValue("weekend_warrior").unlocked)
        assertFalse(states(listOf(win(3))).getValue("weekend_warrior").unlocked)
    }

    @Test fun testAlarmsNeverCount() {
        val test = win(0).copy(isTest = true)
        assertFalse(states(listOf(test)).getValue("first_rise").unlocked)
    }

    @Test fun calendarMarks() {
        val events = listOf(win(2), win(1, outcome = RingOutcome.Escaped))
        val mark = CalendarMarks.forDays(events, zone, today)
        assertEquals(DayMark.Won, mark(today.minusDays(2)))
        assertEquals(DayMark.Missed, mark(today.minusDays(1)))
        assertEquals(DayMark.None, mark(today.minusDays(5)))
        assertEquals(DayMark.None, mark(today.plusDays(1)))
    }

    @Test fun calendarShowsMoonForQuietDaysAfterFirstUse() {
        val mark = CalendarMarks.forDays(listOf(win(4), win(1)), zone, today)
        assertEquals(DayMark.NoAlarm, mark(today.minusDays(2)))
    }
}
