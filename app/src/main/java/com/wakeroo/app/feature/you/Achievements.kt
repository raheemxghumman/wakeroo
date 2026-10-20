package com.wakeroo.app.feature.you

import com.wakeroo.app.data.db.RingEventEntity
import com.wakeroo.app.data.db.RingOutcome
import com.wakeroo.app.feature.stats.WakeStatsCalculator
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class MedalTier { Bronze, Silver, Gold, Special }

data class AchievementDef(val id: String, val title: String, val description: String, val tier: MedalTier, val icon: String)

data class AchievementState(val def: AchievementDef, val progress: Float, val unlocked: Boolean, val progressLabel: String)

/** Everything the engine looks at, gathered once. */
data class AchievementInput(
    val events: List<RingEventEntity>,
    /** Completed missions by type name, e.g. "Scan" -> 4. */
    val missionCounts: Map<String, Int> = emptyMap(),
    /** Days on which the whole morning routine was ticked off. */
    val routineDays: Int = 0,
    val today: LocalDate = LocalDate.now(),
    val zone: ZoneId = ZoneId.systemDefault(),
)

/**
 * Medals for mornings won. A pure function of the ring log and a few counters, so it can be
 * re-run any time (after each ring) and unit-tested. Unlocks are kept in settings with the
 * date they happened, so a medal never disappears once earned.
 */
object AchievementEngine {
    val all = listOf(
        AchievementDef("first_rise", "First Rise", "Finish your first wake-up mission.", MedalTier.Bronze, "sunrise"),
        AchievementDef("streak_3", "Three in a Row", "Win three mornings in a row.", MedalTier.Bronze, "flame"),
        AchievementDef("streak_7", "Full Week", "Win seven mornings in a row.", MedalTier.Silver, "flame"),
        AchievementDef("streak_14", "Fortnight", "Win fourteen mornings in a row.", MedalTier.Silver, "flame"),
        AchievementDef("streak_30", "Habit Formed", "Win thirty mornings in a row.", MedalTier.Gold, "flame"),
        AchievementDef("streak_100", "Century", "Win a hundred mornings in a row.", MedalTier.Special, "crown"),
        AchievementDef("early_bird", "Early Bird", "Get up before 6 AM five times.", MedalTier.Silver, "bird"),
        AchievementDef("no_snooze_week", "No Snooze Week", "Seven straight mornings without a single snooze.", MedalTier.Gold, "bolt"),
        AchievementDef("quick_riser", "Quick Riser", "Finish your mission in under a minute, five times.", MedalTier.Bronze, "timer"),
        AchievementDef("mission_explorer", "Mission Explorer", "Complete five different kinds of mission.", MedalTier.Silver, "compass"),
        AchievementDef("spot_on", "Out of Bed", "Finish ten missions in another room (QR, photo, NFC, item hunt or steps).", MedalTier.Gold, "door"),
        AchievementDef("routine_pro", "Routine Pro", "Tick off your whole morning routine on seven days.", MedalTier.Silver, "check"),
        AchievementDef("weekend_warrior", "Weekend Warrior", "Win both mornings of a weekend.", MedalTier.Bronze, "star"),
        AchievementDef("night_owl", "Night Owl Reformed", "Get up 30 minutes earlier on average than in your first week.", MedalTier.Gold, "moon"),
    )

    private val AWAY = setOf("Scan", "Photo", "Nfc", "Hunt", "Steps")

    fun evaluate(input: AchievementInput): List<AchievementState> {
        val real = input.events.filter { !it.isTest && it.wakeCheckPassed != true }
        val wins = real.filter { it.outcome == RingOutcome.Dismissed }
        val stats = WakeStatsCalculator.compute(input.events, input.today, input.zone)
        val best = stats.bestStreak
        fun localDate(ms: Long) = Instant.ofEpochMilli(ms).atZone(input.zone).toLocalDate()
        fun localTime(ms: Long) = Instant.ofEpochMilli(ms).atZone(input.zone).toLocalTime()

        val early = wins.count { e -> e.endedAt?.let { localTime(it).hour < 6 } == true }
        val quick = wins.count { (it.secondsToWake ?: Int.MAX_VALUE) < 60 }
        val kinds = input.missionCounts.filterValues { it > 0 }.size
        val away = input.missionCounts.filterKeys { it in AWAY }.values.sum()

        // Longest run of consecutive calendar days that each had a win and no snoozes at all.
        val byDay = real.groupBy { localDate(it.scheduledAt) }
        var run = 0; var bestClean = 0; var prev: LocalDate? = null
        for (day in byDay.keys.sorted()) {
            val list = byDay.getValue(day)
            val clean = list.any { it.outcome == RingOutcome.Dismissed } && list.none { it.outcome == RingOutcome.Escaped || it.outcome == RingOutcome.Missed } && list.all { it.snoozes == 0 }
            run = if (clean && prev != null && prev.plusDays(1) == day) run + 1 else if (clean) 1 else 0
            bestClean = maxOf(bestClean, run)
            prev = day
        }

        val winDays = wins.map { localDate(it.scheduledAt) }.toSet()
        val weekend = winDays.any { it.dayOfWeek == DayOfWeek.SATURDAY && it.plusDays(1) in winDays }

        // Average wake time, first week of use against the most recent week.
        val sortedWins = wins.filter { it.endedAt != null }.sortedBy { it.endedAt }
        val owlGain: Int? = if (sortedWins.size >= 10) {
            val firstDay = localDate(sortedWins.first().endedAt!!)
            val first = sortedWins.filter { localDate(it.endedAt!!) < firstDay.plusDays(7) }
            val last = sortedWins.filter { localDate(it.endedAt!!) > input.today.minusDays(7) }
            if (first.size >= 3 && last.size >= 3 && localDate(last.first().endedAt!!) >= firstDay.plusDays(14)) {
                fun avg(l: List<RingEventEntity>) = l.map { localTime(it.endedAt!!).let { t -> t.hour * 60 + t.minute } }.average()
                (avg(first) - avg(last)).toInt()
            } else null
        } else null

        return all.map { def ->
            val (value, target) = when (def.id) {
                "first_rise" -> wins.size to 1
                "streak_3" -> best to 3
                "streak_7" -> best to 7
                "streak_14" -> best to 14
                "streak_30" -> best to 30
                "streak_100" -> best to 100
                "early_bird" -> early to 5
                "no_snooze_week" -> bestClean to 7
                "quick_riser" -> quick to 5
                "mission_explorer" -> kinds to 5
                "spot_on" -> away to 10
                "routine_pro" -> input.routineDays to 7
                "weekend_warrior" -> (if (weekend) 1 else 0) to 1
                "night_owl" -> (owlGain ?: 0).coerceAtLeast(0) to 30
                else -> 0 to 1
            }
            val label = when (def.id) {
                "night_owl" -> if (owlGain == null) "Needs three weeks of mornings" else "$value of $target min earlier"
                "weekend_warrior" -> if (value >= 1) "Done" else "Win a Saturday and a Sunday"
                else -> "${value.coerceAtMost(target)} of $target"
            }
            AchievementState(def, (value / target.toFloat()).coerceIn(0f, 1f), value >= target, label)
        }
    }

    /** Ids unlocked now that weren't in [known]; the briefing celebrates these. */
    fun newlyUnlocked(states: List<AchievementState>, known: Set<String>): List<AchievementDef> =
        states.filter { it.unlocked && it.def.id !in known }.map { it.def }
}
