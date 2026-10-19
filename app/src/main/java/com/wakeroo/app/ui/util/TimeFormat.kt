package com.wakeroo.app.ui.util

import java.time.Duration
import java.time.LocalTime

/** 12-hour clock digits without the meridiem, e.g. "6:30". */
fun LocalTime.formatClock(use24h: Boolean = false): String =
    if (use24h) "%02d:%02d".format(hour, minute) else "%d:%02d".format(if (hour % 12 == 0) 12 else hour % 12, minute)

fun LocalTime.formatMeridiem(use24h: Boolean = false): String = if (use24h) "" else if (hour < 12) "AM" else "PM"

fun formatClock(hour: Int, minute: Int, use24h: Boolean = false) = LocalTime.of(hour, minute).formatClock(use24h)

fun formatMeridiem(hour: Int, use24h: Boolean = false) = LocalTime.of(hour, 0).formatMeridiem(use24h)

/** "7 h 20 min", "45 min", "less than a minute". */
fun Duration.formatUntil(): String {
    val total = toMinutes()
    return when {
        total < 1 -> "less than a minute"
        total < 60 -> "$total min"
        total % 60 == 0L -> "${total / 60} h"
        else -> "${total / 60} h ${total % 60} min"
    }
}
