package com.wakeroo.app.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wakeroo.app.data.model.WakerooJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import java.util.Locale

enum class MotionPreference { System, Full, Reduced }

enum class AppTheme { Dark, Light, System }

enum class TempUnit { Celsius, Fahrenheit }

/** A city picked for the weather card. Only its coordinates are ever sent to Open-Meteo. */
@Serializable
data class City(val name: String, val region: String, val latitude: Double, val longitude: Double)

/** Bump when onboarding gains steps existing users should see. 2 = the setup wizard. */
const val ONBOARDING_VERSION = 2

data class Settings(
    /** 0 = never onboarded. 1 = the original pages, 2 = the setup wizard. */
    val onboardingVersion: Int = 0,
    val motion: MotionPreference = MotionPreference.System,
    val appTheme: AppTheme = AppTheme.Dark,
    val pro: Boolean = false,
    /** The morning screen after an alarm (greeting, weather, plan). */
    val showGoodMorning: Boolean = true,
    val lastTestAlarmAt: Long = 0L,
    val lastTestAlarmPassed: Boolean = false,
    val use24Hour: Boolean? = null,
    /** Mission chosen during onboarding, used for the first alarm. */
    val firstMission: String? = null,
    /** App-wide ring scene; an alarm can override it. */
    val ringScene: String = "night",
    val customScenePath: String? = null,
    /** Seconds without touching a mission before full volume returns and it resets. */
    val missionTimeLimit: Int = 20,
    /** Route the alarm to the built-in speaker even when headphones or Bluetooth are connected. */
    val alwaysSpeaker: Boolean = true,
    /** With [alwaysSpeaker], also play on the connected headphones or speaker. */
    val alsoHeadphones: Boolean = false,
    val sleepGoalMinutes: Int = 480,
    val bedtimeReminder: Boolean = true,
    val windDownMinutes: Int = 30,
    val noAlarmReminder: Boolean = true,
    /** Minutes after midnight for the "no alarm for tomorrow" check. */
    val noAlarmReminderAt: Int = 20 * 60,
    val weatherNotification: Boolean = false,
    val weatherCity: City? = null,
    val tempUnit: TempUnit = defaultTempUnit(),
    /** Achievement id to the time it was unlocked. */
    val achievements: Map<String, Long> = emptyMap(),
    /** ISO week ("2026-W43") whose recap was dismissed. */
    val recapDismissedWeek: String? = null,
    /** An amber Fix banner stays hidden until this time. */
    val fixBannerHiddenUntil: Long = 0L,
    /** Today's plan from the "Type your plan" mission, shown on the Morning tab. */
    val plan: String? = null,
    val planDate: String? = null,
    val quoteThemes: Set<String> = emptySet(),
    val sleepSounds: List<String> = listOf("rain"),
    val sleepTimerMinutes: Int = 30,
    /** Completed missions by type, for the medals. */
    val missionCounts: Map<String, Int> = emptyMap(),
) {
    val onboardingDone: Boolean get() = onboardingVersion > 0
}

private fun defaultTempUnit(): TempUnit =
    if (Locale.getDefault().country in setOf("US", "LR", "MM", "BS", "BZ", "KY", "PW")) TempUnit.Fahrenheit else TempUnit.Celsius

private val Context.dataStore by preferencesDataStore("settings")

class SettingsStore(private val context: Context, private val directBoot: DirectBootStore? = null) {
    val settings: Flow<Settings> = context.dataStore.data.map { it.toSettings() }

    suspend fun current(): Settings = settings.first()

    suspend fun update(block: (Settings) -> Settings) {
        context.dataStore.edit { prefs ->
            val next = block(prefs.toSettings())
            prefs[ONBOARDING_V] = next.onboardingVersion
            prefs[MOTION] = next.motion.name
            prefs[THEME] = next.appTheme.name
            prefs[PRO] = next.pro
            prefs[GOOD_MORNING] = next.showGoodMorning
            prefs[TEST_AT] = next.lastTestAlarmAt
            prefs[TEST_PASSED] = next.lastTestAlarmPassed
            prefs.putOrRemove(USE_24H, next.use24Hour)
            prefs.putOrRemove(FIRST_MISSION, next.firstMission)
            prefs[RING_SCENE] = next.ringScene
            prefs.putOrRemove(CUSTOM_SCENE, next.customScenePath)
            prefs[MISSION_LIMIT] = next.missionTimeLimit
            prefs[ALWAYS_SPEAKER] = next.alwaysSpeaker
            prefs[ALSO_HEADPHONES] = next.alsoHeadphones
            prefs[SLEEP_GOAL] = next.sleepGoalMinutes
            prefs[BEDTIME_REMINDER] = next.bedtimeReminder
            prefs[WIND_DOWN] = next.windDownMinutes
            prefs[NO_ALARM_REMINDER] = next.noAlarmReminder
            prefs[NO_ALARM_AT] = next.noAlarmReminderAt
            prefs[WEATHER_NOTIFICATION] = next.weatherNotification
            prefs.putOrRemove(CITY, next.weatherCity?.let { WakerooJson.encodeToString(it) })
            prefs[TEMP_UNIT] = next.tempUnit.name
            prefs[ACHIEVEMENTS] = WakerooJson.encodeToString(next.achievements)
            prefs.putOrRemove(RECAP_DISMISSED, next.recapDismissedWeek)
            prefs[FIX_HIDDEN_UNTIL] = next.fixBannerHiddenUntil
            prefs.putOrRemove(PLAN, next.plan)
            prefs.putOrRemove(PLAN_DATE, next.planDate)
            prefs[QUOTE_THEMES] = next.quoteThemes
            prefs[SLEEP_SOUNDS] = next.sleepSounds.joinToString(",")
            prefs[SLEEP_TIMER] = next.sleepTimerMinutes
            prefs[MISSION_COUNTS] = WakerooJson.encodeToString(next.missionCounts)
            directBoot?.ringPrefs = RingPrefs(next.alwaysSpeaker, next.alsoHeadphones, next.missionTimeLimit, next.ringScene)
        }
    }

    private fun <T> MutablePreferences.putOrRemove(key: Preferences.Key<T>, value: T?) {
        if (value == null) remove(key) else this[key] = value
    }

    private fun Preferences.toSettings() = Settings(
        // The original onboarding stored a boolean; it counts as version 1.
        onboardingVersion = this[ONBOARDING_V] ?: if (this[ONBOARDING_LEGACY] == true) 1 else 0,
        motion = enumOr(this[MOTION], MotionPreference.System),
        appTheme = enumOr(this[THEME], AppTheme.Dark),
        pro = this[PRO] ?: false,
        showGoodMorning = this[GOOD_MORNING] ?: true,
        lastTestAlarmAt = this[TEST_AT] ?: 0L,
        lastTestAlarmPassed = this[TEST_PASSED] ?: false,
        use24Hour = this[USE_24H],
        firstMission = this[FIRST_MISSION],
        ringScene = this[RING_SCENE] ?: "night",
        customScenePath = this[CUSTOM_SCENE],
        missionTimeLimit = this[MISSION_LIMIT] ?: 20,
        alwaysSpeaker = this[ALWAYS_SPEAKER] ?: true,
        alsoHeadphones = this[ALSO_HEADPHONES] ?: false,
        sleepGoalMinutes = this[SLEEP_GOAL] ?: 480,
        bedtimeReminder = this[BEDTIME_REMINDER] ?: true,
        windDownMinutes = this[WIND_DOWN] ?: 30,
        noAlarmReminder = this[NO_ALARM_REMINDER] ?: true,
        noAlarmReminderAt = this[NO_ALARM_AT] ?: (20 * 60),
        weatherNotification = this[WEATHER_NOTIFICATION] ?: false,
        weatherCity = this[CITY]?.let { runCatching { WakerooJson.decodeFromString<City>(it) }.getOrNull() },
        tempUnit = this[TEMP_UNIT]?.let { runCatching { TempUnit.valueOf(it) }.getOrNull() } ?: defaultTempUnit(),
        achievements = this[ACHIEVEMENTS]?.let { runCatching { WakerooJson.decodeFromString<Map<String, Long>>(it) }.getOrNull() } ?: emptyMap(),
        recapDismissedWeek = this[RECAP_DISMISSED],
        fixBannerHiddenUntil = this[FIX_HIDDEN_UNTIL] ?: 0L,
        plan = this[PLAN],
        planDate = this[PLAN_DATE],
        quoteThemes = this[QUOTE_THEMES] ?: emptySet(),
        sleepSounds = this[SLEEP_SOUNDS]?.split(',')?.filter { it.isNotBlank() } ?: listOf("rain"),
        sleepTimerMinutes = this[SLEEP_TIMER] ?: 30,
        missionCounts = this[MISSION_COUNTS]?.let { runCatching { WakerooJson.decodeFromString<Map<String, Int>>(it) }.getOrNull() } ?: emptyMap(),
    )

    private inline fun <reified E : Enum<E>> enumOr(name: String?, fallback: E): E =
        name?.let { runCatching { enumValueOf<E>(it) }.getOrNull() } ?: fallback

    private companion object {
        val ONBOARDING_LEGACY = booleanPreferencesKey("onboarding_done")
        val ONBOARDING_V = intPreferencesKey("onboarding_version")
        val MOTION = stringPreferencesKey("motion")
        val THEME = stringPreferencesKey("app_theme")
        val PRO = booleanPreferencesKey("pro")
        val GOOD_MORNING = booleanPreferencesKey("good_morning")
        val TEST_AT = longPreferencesKey("test_at")
        val TEST_PASSED = booleanPreferencesKey("test_passed")
        val USE_24H = booleanPreferencesKey("use_24h")
        val FIRST_MISSION = stringPreferencesKey("first_mission")
        val RING_SCENE = stringPreferencesKey("ring_scene")
        val CUSTOM_SCENE = stringPreferencesKey("custom_scene")
        val MISSION_LIMIT = intPreferencesKey("mission_time_limit")
        val ALWAYS_SPEAKER = booleanPreferencesKey("always_speaker")
        val ALSO_HEADPHONES = booleanPreferencesKey("also_headphones")
        val SLEEP_GOAL = intPreferencesKey("sleep_goal")
        val BEDTIME_REMINDER = booleanPreferencesKey("bedtime_reminder")
        val WIND_DOWN = intPreferencesKey("wind_down")
        val NO_ALARM_REMINDER = booleanPreferencesKey("no_alarm_reminder")
        val NO_ALARM_AT = intPreferencesKey("no_alarm_at")
        val WEATHER_NOTIFICATION = booleanPreferencesKey("weather_notification")
        val CITY = stringPreferencesKey("weather_city")
        val TEMP_UNIT = stringPreferencesKey("temp_unit")
        val ACHIEVEMENTS = stringPreferencesKey("achievements")
        val RECAP_DISMISSED = stringPreferencesKey("recap_dismissed")
        val FIX_HIDDEN_UNTIL = longPreferencesKey("fix_hidden_until")
        val PLAN = stringPreferencesKey("plan")
        val PLAN_DATE = stringPreferencesKey("plan_date")
        val QUOTE_THEMES = stringSetPreferencesKey("quote_themes")
        val SLEEP_SOUNDS = stringPreferencesKey("sleep_sounds")
        val SLEEP_TIMER = intPreferencesKey("sleep_timer")
        val MISSION_COUNTS = stringPreferencesKey("mission_counts")
    }
}
