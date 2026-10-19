package com.wakeroo.app.feature.stats

import com.wakeroo.app.data.db.RingEventEntity
import com.wakeroo.app.data.db.RingOutcome
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class WakeStatsTest {
    private val zone = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 10, 20)

    private fun event(daysAgo: Long, outcome: RingOutcome, seconds: Int? = 40, test: Boolean = false, wakeCheck: Boolean? = null): RingEventEntity {
        val at = today.minusDays(daysAgo).atTime(7, 0).atZone(zone).toInstant().toEpochMilli()
        return RingEventEntity(0, 1, "", at, at, at + 60_000, outcome, 0, seconds, wakeCheck, test)
    }

    @Test fun streakCountsConsecutiveGoodMornings() {
        val events = listOf(event(0, RingOutcome.Dismissed), event(1, RingOutcome.Dismissed), event(2, RingOutcome.Dismissed), event(4, RingOutcome.Dismissed))
        assertEquals(3, WakeStatsCalculator.compute(events, today, zone).streak)
    }

    @Test fun pendingTodayDoesNotBreakStreak() {
        val events = listOf(event(1, RingOutcome.Dismissed), event(2, RingOutcome.Dismissed))
        assertEquals(2, WakeStatsCalculator.compute(events, today, zone).streak)
    }

    @Test fun escapeBreaksStreak() {
        val events = listOf(event(0, RingOutcome.Dismissed), event(1, RingOutcome.Escaped), event(2, RingOutcome.Dismissed))
        val s = WakeStatsCalculator.compute(events, today, zone)
        assertEquals(1, s.streak)
        assertEquals(1, s.bestStreak)
    }

    @Test fun testAlarmsAndPassedChecksAreIgnored() {
        val events = listOf(
            event(0, RingOutcome.Dismissed),
            event(0, RingOutcome.Missed, test = true),
            event(0, RingOutcome.Dismissed, seconds = null, wakeCheck = true),
        )
        val s = WakeStatsCalculator.compute(events, today, zone)
        assertEquals(100, s.successRate)
        assertEquals(1, s.totalWakeUps)
    }

    @Test fun failedWakeCheckCountsAsMiss() {
        val events = listOf(event(0, RingOutcome.Dismissed), event(0, RingOutcome.Missed, seconds = null, wakeCheck = false))
        assertEquals(50, WakeStatsCalculator.compute(events, today, zone).successRate)
    }
}
