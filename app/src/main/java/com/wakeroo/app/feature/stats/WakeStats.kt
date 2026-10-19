package com.wakeroo.app.feature.stats

import com.wakeroo.app.data.db.RingEventEntity
import com.wakeroo.app.data.db.RingOutcome
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class DayResult(val date: LocalDate, val success: Boolean?, val secondsToWake: Int?)

data class WakeStats(
    val streak: Int,
    val bestStreak: Int,
    val successRate: Int?,
    val averageSecondsToWake: Int?,
    val averageWakeTime: LocalTime?,
    val lastSevenDays: List<DayResult>,
    val totalWakeUps: Int,
)

/** Turns the ring log into the numbers on the stats screen. Test alarms never count. */
object WakeStatsCalculator {
    fun compute(events: List<RingEventEntity>, today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): WakeStats {
        // A passed wake-up check is a confirmation, not a separate morning; a failed one counts.
        val real = events.filter { !it.isTest && it.wakeCheckPassed != true }
        val byDay = real.groupBy { Instant.ofEpochMilli(it.scheduledAt).atZone(zone).toLocalDate() }
        fun success(day: LocalDate): Boolean? = byDay[day]?.let { list ->
            list.none { it.outcome == RingOutcome.Escaped || it.outcome == RingOutcome.Missed }
        }

        // Current streak: successful days running back from today (today may still be pending).
        var streak = 0
        var day = if (success(today) == null) today.minusDays(1) else today
        while (true) {
            val s = success(day) ?: break
            if (!s) break
            streak++; day = day.minusDays(1)
        }

        var best = 0; var run = 0
        byDay.keys.sorted().forEach { d ->
            run = if (success(d) == true) run + 1 else 0
            best = maxOf(best, run)
        }

        val recent = real.filter { Instant.ofEpochMilli(it.scheduledAt).atZone(zone).toLocalDate() >= today.minusDays(29) }
        val rate = recent.takeIf { it.isNotEmpty() }?.let { list ->
            (100 * list.count { it.outcome == RingOutcome.Dismissed } / list.size)
        }
        val dismissed = real.filter { it.outcome == RingOutcome.Dismissed && it.secondsToWake != null }
        val avgSeconds = dismissed.takeIf { it.isNotEmpty() }?.map { it.secondsToWake!! }?.average()?.toInt()
        val wakeMinutes = dismissed.mapNotNull { it.endedAt }.take(14).map {
            val t = Instant.ofEpochMilli(it).atZone(zone).toLocalTime(); t.hour * 60 + t.minute
        }
        val avgWake = wakeMinutes.takeIf { it.isNotEmpty() }?.average()?.toInt()?.let { LocalTime.of(it / 60, it % 60) }

        val seven = (6 downTo 0).map { back ->
            val d = today.minusDays(back.toLong())
            val dayEvents = byDay[d]
            DayResult(d, success(d), dayEvents?.mapNotNull { it.secondsToWake }?.minOrNull())
        }
        return WakeStats(streak, maxOf(best, streak), rate, avgSeconds, avgWake, seven, dismissed.size)
    }
}
