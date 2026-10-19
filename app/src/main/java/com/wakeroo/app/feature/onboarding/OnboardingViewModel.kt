package com.wakeroo.app.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakeroo.app.AppContainer
import com.wakeroo.app.alarm.NextTriggerCalculator
import com.wakeroo.app.data.ONBOARDING_VERSION
import com.wakeroo.app.data.model.Alarm
import com.wakeroo.app.data.model.MissionConfig
import com.wakeroo.app.data.model.MissionType
import com.wakeroo.app.data.model.WakerooJson
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.ZonedDateTime

enum class Step { Restored, Time, Scene, Sound, Mission, Permissions, Building, Test }

data class OnboardingState(
    val steps: List<Step>,
    val index: Int = 0,
    val draft: Alarm,
    /** Set once "Building your alarm" has saved it. */
    val savedId: Long = 0,
    val restoredAlarms: List<Alarm> = emptyList(),
    val building: Float = 0f,
) {
    val step: Step get() = steps[index]

    /** "2 of 5" counts only the steps the user makes choices on. */
    val numbered: List<Step> get() = steps.filter { it != Step.Building && it != Step.Test && it != Step.Restored }
    val nextRing: ZonedDateTime? get() = NextTriggerCalculator.next(draft.copy(enabled = true), ZonedDateTime.now())
}

/**
 * Builds the first alarm across the wizard's steps. The draft and step survive process death,
 * so switching to system settings for a permission never loses the user's choices.
 */
class OnboardingViewModel(private val app: AppContainer, private val saved: SavedStateHandle, restore: Boolean) : ViewModel() {
    private val _state = MutableStateFlow(
        OnboardingState(
            steps = if (restore) listOf(Step.Restored, Step.Permissions, Step.Test)
            else listOf(Step.Time, Step.Scene, Step.Sound, Step.Mission, Step.Permissions, Step.Building, Step.Test),
            index = saved[KEY_INDEX] ?: 0,
            draft = saved.get<String>(KEY_DRAFT)?.let { runCatching { WakerooJson.decodeFromString<Alarm>(it) }.getOrNull() } ?: firstAlarm(),
            savedId = saved[KEY_SAVED] ?: 0L,
        ),
    )
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    init {
        // Process death mid-build: pick the save back up.
        if (_state.value.step == Step.Building && _state.value.savedId == 0L) build()
        if (restore) viewModelScope.launch {
            val alarms = app.repository.all()
            _state.update { it.copy(restoredAlarms = alarms) }
        }
    }

    fun update(block: (Alarm) -> Alarm) {
        _state.update { it.copy(draft = block(it.draft)) }
        saved[KEY_DRAFT] = WakerooJson.encodeToString(_state.value.draft)
    }

    fun next() = go(_state.value.index + 1)

    fun back() = go(_state.value.index - 1)

    private fun go(index: Int) {
        val target = index.coerceIn(0, _state.value.steps.lastIndex)
        _state.update { it.copy(index = target) }
        saved[KEY_INDEX] = target
        if (_state.value.step == Step.Building && _state.value.savedId == 0L) build()
    }

    /**
     * Saves and schedules the alarm while the "building" sequence plays. The sequence takes at
     * least 2.5 s but never claims to be done before the real work is.
     */
    private fun build() = viewModelScope.launch {
        val save = async {
            val draft = _state.value.draft
            val id = app.repository.save(draft.copy(enabled = true, createdAt = System.currentTimeMillis()))
            app.settings.update { it.copy(firstMission = draft.missions.firstOrNull()?.type?.name) }
            id
        }
        val start = System.currentTimeMillis()
        while (true) {
            val elapsed = System.currentTimeMillis() - start
            val visual = (elapsed / 2500f).coerceAtMost(if (save.isCompleted) 1f else 0.92f)
            _state.update { it.copy(building = visual) }
            if (visual >= 1f) break
            delay(16)
        }
        val id = save.await()
        saved[KEY_SAVED] = id
        _state.update { it.copy(savedId = id, building = 1f) }
    }

    suspend fun finish() {
        app.settings.update { it.copy(onboardingVersion = ONBOARDING_VERSION) }
    }

    fun scheduleTest() = app.scheduler.scheduleTest(System.currentTimeMillis() + 60_000)

    private companion object {
        const val KEY_INDEX = "index"
        const val KEY_DRAFT = "draft"
        const val KEY_SAVED = "saved_id"

        /** 7:00 on weekdays with a math mission: the most common first alarm. */
        fun firstAlarm() = Alarm(
            hour = 7, minute = 0, days = DayOfWeek.entries.take(5).toSet(),
            missions = listOf(MissionConfig(MissionType.Math)),
        )
    }
}
