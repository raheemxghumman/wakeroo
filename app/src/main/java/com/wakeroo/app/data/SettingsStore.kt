package com.wakeroo.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class MotionPreference { System, Full, Reduced }

data class Settings(
    val onboardingDone: Boolean = false,
    val motion: MotionPreference = MotionPreference.System,
    val pro: Boolean = false,
    val showGoodMorning: Boolean = true,
    val lastTestAlarmAt: Long = 0L,
    val lastTestAlarmPassed: Boolean = false,
    val use24Hour: Boolean? = null,
    /** Mission chosen during onboarding, used for the first alarm. */
    val firstMission: String? = null,
)

private val Context.dataStore by preferencesDataStore("settings")

class SettingsStore(private val context: Context) {
    val settings: Flow<Settings> = context.dataStore.data.map { it.toSettings() }

    suspend fun current(): Settings = settings.first()

    suspend fun update(block: (Settings) -> Settings) {
        context.dataStore.edit { prefs ->
            val next = block(prefs.toSettings())
            prefs[ONBOARDING] = next.onboardingDone
            prefs[MOTION] = next.motion.name
            prefs[PRO] = next.pro
            prefs[GOOD_MORNING] = next.showGoodMorning
            prefs[TEST_AT] = next.lastTestAlarmAt
            prefs[TEST_PASSED] = next.lastTestAlarmPassed
            if (next.use24Hour == null) prefs.remove(USE_24H) else prefs[USE_24H] = next.use24Hour
            if (next.firstMission == null) prefs.remove(FIRST_MISSION) else prefs[FIRST_MISSION] = next.firstMission
        }
    }

    private fun Preferences.toSettings() = Settings(
        onboardingDone = this[ONBOARDING] ?: false,
        motion = this[MOTION]?.let { runCatching { MotionPreference.valueOf(it) }.getOrNull() } ?: MotionPreference.System,
        pro = this[PRO] ?: false,
        showGoodMorning = this[GOOD_MORNING] ?: true,
        lastTestAlarmAt = this[TEST_AT] ?: 0L,
        lastTestAlarmPassed = this[TEST_PASSED] ?: false,
        use24Hour = this[USE_24H],
        firstMission = this[FIRST_MISSION],
    )

    private companion object {
        val ONBOARDING = booleanPreferencesKey("onboarding_done")
        val MOTION = stringPreferencesKey("motion")
        val PRO = booleanPreferencesKey("pro")
        val GOOD_MORNING = booleanPreferencesKey("good_morning")
        val TEST_AT = longPreferencesKey("test_at")
        val TEST_PASSED = booleanPreferencesKey("test_passed")
        val USE_24H = booleanPreferencesKey("use_24h")
        val FIRST_MISSION = stringPreferencesKey("first_mission")
    }
}
