package com.wakeroo.app.alarm.ring

import com.wakeroo.app.data.RingKind
import com.wakeroo.app.data.model.Alarm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What the ring screen should show. Owned by [RingService], observed by [RingActivity]. */
data class RingState(
    val alarm: Alarm,
    val kind: RingKind,
    val scheduledAt: Long,
    val firedAt: Long,
    val snoozes: Int,
    /** For a wake-up check: when it escalates back to a full alarm. */
    val wakeCheckDeadline: Long = 0L,
) {
    val snoozesLeft: Int get() = (alarm.snoozeLimit - snoozes).coerceAtLeast(0)
}

object RingSession {
    private val _state = MutableStateFlow<RingState?>(null)
    val state: StateFlow<RingState?> = _state.asStateFlow()

    internal fun publish(state: RingState?) {
        _state.value = state
    }
}
