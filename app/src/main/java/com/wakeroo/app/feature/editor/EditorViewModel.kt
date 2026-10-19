package com.wakeroo.app.feature.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakeroo.app.AppContainer
import com.wakeroo.app.alarm.NextTriggerCalculator
import com.wakeroo.app.data.model.Alarm
import com.wakeroo.app.data.model.MissionConfig
import com.wakeroo.app.data.model.MissionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

data class EditorState(
    val loaded: Boolean = false,
    val isNew: Boolean = true,
    val draft: Alarm = defaultAlarm(),
    val pro: Boolean = false,
    /** The app-wide scene an alarm without its own uses. */
    val defaultScene: String = "night",
) {
    val maxMissions: Int get() = if (pro) 5 else 2
    val nextRing: ZonedDateTime? get() = NextTriggerCalculator.next(draft.copy(enabled = true), ZonedDateTime.now())
}

/** A new alarm starts at 7:00, the most common wake-up time, unless the user has a habit already. */
private fun defaultAlarm(): Alarm = Alarm(hour = 7, minute = 0, missions = listOf(MissionConfig(MissionType.Math)))

class EditorViewModel(private val app: AppContainer, private val alarmId: Long) : ViewModel() {
    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = app.settings.current()
            val existing = if (alarmId > 0) app.repository.get(alarmId) else null
            val first = settings.firstMission?.let { runCatching { MissionType.valueOf(it) }.getOrNull() }
            val latest = app.repository.latest()
            val fresh = when {
                // Same time and missions as the most recent alarm: setting the usual alarm is one tap.
                latest != null -> latest.copy(id = 0, days = emptySet(), date = null, label = "", enabled = true, skipDate = null, createdAt = System.currentTimeMillis())
                first != null -> defaultAlarm().copy(missions = listOf(MissionConfig(first)))
                else -> defaultAlarm()
            }
            _state.value = EditorState(loaded = true, isNew = existing == null, draft = existing ?: fresh, pro = settings.pro, defaultScene = settings.ringScene)
        }
    }

    fun update(block: (Alarm) -> Alarm) = _state.update { it.copy(draft = block(it.draft)) }

    fun setTime(hour: Int, minute: Int) = update { it.copy(hour = hour, minute = minute, skipDate = null) }

    fun setMission(index: Int, config: MissionConfig) = update { a ->
        val list = a.missions.toMutableList()
        if (index in list.indices) list[index] = config else list += config
        a.copy(missions = list)
    }

    fun removeMission(index: Int) = update { a -> a.copy(missions = a.missions.filterIndexed { i, _ -> i != index }) }

    suspend fun save(): Long = app.repository.save(state.value.draft.copy(enabled = true))

    suspend fun delete() {
        if (!state.value.isNew) app.repository.delete(state.value.draft)
    }
}
