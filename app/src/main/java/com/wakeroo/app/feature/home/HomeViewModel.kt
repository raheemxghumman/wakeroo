package com.wakeroo.app.feature.home

import com.wakeroo.app.ui.i18n.tr
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakeroo.app.AppContainer
import com.wakeroo.app.alarm.NextTriggerCalculator
import com.wakeroo.app.alarm.health.Health
import com.wakeroo.app.alarm.health.HealthChecker
import com.wakeroo.app.alarm.health.HealthItem
import com.wakeroo.app.data.db.RingEventEntity
import com.wakeroo.app.data.Settings
import com.wakeroo.app.data.model.Alarm
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

data class AlarmRow(val alarm: Alarm, val next: ZonedDateTime?)

data class HomeState(
    val loaded: Boolean = false,
    val rows: List<AlarmRow> = emptyList(),
    val next: AlarmRow? = null,
    val now: Long = System.currentTimeMillis(),
    val health: Health = Health.Good,
    /** The checks behind [health]; the Fix banner shows the first one that isn't good. */
    val healthItems: List<HealthItem> = emptyList(),
    val settings: Settings = Settings(),
    val events: List<RingEventEntity> = emptyList(),
)

class HomeViewModel(private val app: AppContainer, private val checker: HealthChecker) : ViewModel() {
    private val health = MutableStateFlow<List<HealthItem>>(emptyList())
    private val clock = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(15_000)
        }
    }

    val state: StateFlow<HomeState> = combine(app.repository.alarms, app.settings.settings, clock, health, app.repository.ringEvents) { alarms, settings, now, items, events ->
        val zoned = ZonedDateTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.systemDefault())
        val rows = alarms.map { AlarmRow(it, NextTriggerCalculator.next(it, zoned)) }
            .sortedWith(compareBy<AlarmRow> { it.alarm.hour * 60 + it.alarm.minute }.thenBy { it.alarm.id })
        HomeState(
            loaded = true,
            rows = rows,
            next = rows.filter { it.next != null }.minByOrNull { it.next!! },
            now = now,
            health = checker.overall(items),
            healthItems = items,
            settings = settings,
            events = events,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    fun refreshHealth() {
        viewModelScope.launch {
            val s = app.settings.current()
            health.value = checker.check(s.lastTestAlarmAt, s.lastTestAlarmPassed)
        }
    }

    fun ringTest() = app.scheduler.scheduleTest(System.currentTimeMillis() + 60_000)

    /** An amber banner can be put away for a week; red ones stay until fixed. */
    fun hideBanner() = viewModelScope.launch {
        app.settings.update { it.copy(fixBannerHiddenUntil = System.currentTimeMillis() + 7 * 24 * 3_600_000L) }
    }

    fun dismissRecap(week: String) = viewModelScope.launch { app.settings.update { it.copy(recapDismissedWeek = week) } }

    fun toggle(alarm: Alarm, on: Boolean) = viewModelScope.launch { app.repository.setEnabled(alarm, on) }

    fun delete(alarm: Alarm) = viewModelScope.launch { app.repository.delete(alarm) }

    fun restore(alarm: Alarm) = viewModelScope.launch { app.repository.save(alarm) }

    fun skipNext(row: AlarmRow) = viewModelScope.launch {
        row.next?.let { app.repository.skipNext(row.alarm, it.toLocalDate()) }
    }

    fun undoSkip(alarm: Alarm) = viewModelScope.launch { app.repository.save(alarm.copy(skipDate = null)) }

    /** "+10 min", "In 8 h": one-off alarms at a relative time, using the user's last mission. */
    fun quickAlarm(minutesFromNow: Int, template: Alarm?) = viewModelScope.launch {
        val at = ZonedDateTime.now().plusMinutes(minutesFromNow.toLong())
        val base = template ?: Alarm(hour = 0, minute = 0)
        app.repository.save(
            base.copy(
                id = 0, hour = at.hour, minute = at.minute, days = emptySet(), date = at.toLocalDate(),
                label = if (minutesFromNow < 120) tr("Power nap") else base.label, enabled = true, skipDate = null,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }
}
