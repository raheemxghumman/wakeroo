package com.wakeroo.app.ui.util

import com.wakeroo.app.data.model.Alarm
import com.wakeroo.app.ui.i18n.tr
import java.text.DateFormatSymbols
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// Clock digits stay Western in every language, matching the mission keypads.

/** 12-hour clock digits without the meridiem, e.g. "6:30". */
fun LocalTime.formatClock(use24h: Boolean = false): String =
    if (use24h) String.format(Locale.ROOT, "%02d:%02d", hour, minute)
    else String.format(Locale.ROOT, "%d:%02d", if (hour % 12 == 0) 12 else hour % 12, minute)

/** "AM" / "PM" in the user's language (e.g. "a. m." in Spanish). */
fun LocalTime.formatMeridiem(use24h: Boolean = false): String =
    if (use24h) "" else DateFormatSymbols.getInstance(Locale.getDefault()).amPmStrings[if (hour < 12) 0 else 1]

fun formatClock(hour: Int, minute: Int, use24h: Boolean = false) = LocalTime.of(hour, minute).formatClock(use24h)

fun formatMeridiem(hour: Int, use24h: Boolean = false) = LocalTime.of(hour, 0).formatMeridiem(use24h)

/** Two-digit numbers for wheels and clocks, always with Western digits. */
fun twoDigits(n: Int): String = String.format(Locale.ROOT, "%02d", n)

/** A sleep length like "8 h" or "7 h 30 min". */
fun formatHours(minutes: Int): String =
    if (minutes % 60 == 0) tr("{0} h", minutes / 60) else tr("{0} h {1} min", minutes / 60, minutes % 60)

/** "7 h 20 min", "45 min", "less than a minute". */
fun Duration.formatUntil(): String {
    val total = toMinutes()
    return when {
        total < 1 -> tr("less than a minute")
        total < 60 -> tr("{0} min", total)
        total % 60 == 0L -> tr("{0} h", total / 60)
        else -> tr("{0} h {1} min", total / 60, total % 60)
    }
}

/** "45s", "2 min 41s": how long a mission took. */
fun formatSeconds(seconds: Int): String =
    if (seconds < 60) tr("{0}s", seconds) else tr("{0} min {1}s", seconds / 60, seconds % 60)

/** "M", "T", "W"… in the user's language. */
fun DayOfWeek.initial(): String = getDisplayName(TextStyle.NARROW, Locale.getDefault())

/** "Mon", "Tue"… in the user's language. */
fun DayOfWeek.short(): String = getDisplayName(TextStyle.SHORT, Locale.getDefault())

/** "Monday"… in the user's language. */
fun DayOfWeek.full(): String = getDisplayName(TextStyle.FULL, Locale.getDefault())

private val WEEKDAYS = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
private val WEEKEND = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

/** "Weekdays", "Every day", "Mon, Wed, Fri", "Once", "Fri 24 Dec". */
fun Alarm.repeatSummary(): String = when {
    date != null -> DateTimeFormatter.ofPattern("EEE d MMM").format(date)
    days.isEmpty() -> tr("Once")
    days.size == 7 -> tr("Every day")
    days == WEEKDAYS -> tr("Weekdays")
    days == WEEKEND -> tr("Weekends")
    else -> days.sorted().joinToString(", ") { it.short() }
}
