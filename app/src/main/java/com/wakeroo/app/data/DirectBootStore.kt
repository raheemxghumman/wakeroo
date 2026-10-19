package com.wakeroo.app.data

import android.content.Context
import androidx.core.content.edit
import com.wakeroo.app.data.db.RingOutcome
import com.wakeroo.app.data.model.Alarm
import com.wakeroo.app.data.model.WakerooJson
import kotlinx.serialization.Serializable

/** A ring that is in progress, persisted so a reboot mid-ring can re-ring the alarm. */
@Serializable
data class ActiveRing(
    val alarmId: Long,
    val scheduledAt: Long,
    val firedAt: Long,
    val snoozes: Int = 0,
    val kind: RingKind = RingKind.Alarm,
)

@Serializable
enum class RingKind { Alarm, WakeCheck, Test }

/** A one-off ring (snooze, wake-up check, re-ring) that must survive reboots and app updates. */
@Serializable
data class PendingExtra(val alarmId: Long, val slot: String, val at: Long, val kind: RingKind, val scheduledAt: Long, val snoozes: Int)

/**
 * The settings the ring path needs, mirrored from DataStore so they also apply before the
 * first unlock after a reboot.
 */
@Serializable
data class RingPrefs(
    val alwaysSpeaker: Boolean = true,
    val alsoHeadphones: Boolean = false,
    val missionTimeLimit: Int = 20,
    val ringScene: String = "night",
)

/** A ring-log entry written while the phone was still locked after boot; moved to Room later. */
@Serializable
data class PendingEvent(
    val alarmId: Long,
    val label: String,
    val scheduledAt: Long,
    val firedAt: Long,
    val endedAt: Long?,
    val outcome: RingOutcome,
    val snoozes: Int,
    val secondsToWake: Int?,
    val wakeCheckPassed: Boolean?,
    val isTest: Boolean = false,
)

/**
 * Device-protected storage: readable before the user unlocks the phone after a reboot.
 * It mirrors every alarm so the ring path never needs the credential-encrypted database.
 */
class DirectBootStore(context: Context) {
    private val prefs = context.createDeviceProtectedStorageContext()
        .getSharedPreferences("wakeroo_direct_boot", Context.MODE_PRIVATE)

    fun writeAlarms(alarms: List<Alarm>) = prefs.edit { putString(KEY_ALARMS, WakerooJson.encodeToString(alarms)) }

    fun readAlarms(): List<Alarm> =
        prefs.getString(KEY_ALARMS, null)?.let { runCatching { WakerooJson.decodeFromString<List<Alarm>>(it) }.getOrNull() }
            ?: emptyList()

    fun alarm(id: Long): Alarm? = readAlarms().firstOrNull { it.id == id }

    var activeRing: ActiveRing?
        get() = prefs.getString(KEY_ACTIVE, null)?.let { runCatching { WakerooJson.decodeFromString<ActiveRing>(it) }.getOrNull() }
        set(value) = prefs.edit {
            if (value == null) remove(KEY_ACTIVE) else putString(KEY_ACTIVE, WakerooJson.encodeToString(value))
        }

    /** Request codes currently registered with AlarmManager, so removed alarms can be cancelled. */
    var scheduledIds: Set<Long>
        get() = prefs.getStringSet(KEY_SCHEDULED, emptySet())!!.mapNotNull { it.toLongOrNull() }.toSet()
        set(value) = prefs.edit { putStringSet(KEY_SCHEDULED, value.map { it.toString() }.toSet()) }

    fun addPendingEvent(event: PendingEvent) {
        val list = pendingEvents() + event
        prefs.edit { putString(KEY_EVENTS, WakerooJson.encodeToString(list)) }
    }

    fun pendingEvents(): List<PendingEvent> =
        prefs.getString(KEY_EVENTS, null)?.let { runCatching { WakerooJson.decodeFromString<List<PendingEvent>>(it) }.getOrNull() }
            ?: emptyList()

    fun clearPendingEvents() = prefs.edit { remove(KEY_EVENTS) }

    /**
     * Alarm id to the trigger time handed to AlarmManager. An entry still here well after its
     * time means the alarm never fired: the phone was off or the system killed us.
     */
    var expectedTriggers: Map<Long, Long>
        get() = prefs.getString(KEY_EXPECTED, null)?.let { runCatching { WakerooJson.decodeFromString<Map<Long, Long>>(it) }.getOrNull() }
            ?: emptyMap()
        set(value) = prefs.edit { putString(KEY_EXPECTED, WakerooJson.encodeToString(value)) }

    /** One-time alarms that finished while the phone was locked; disabled in Room after unlock. */
    var pendingDisables: Set<Long>
        get() = prefs.getStringSet(KEY_DISABLES, emptySet())!!.mapNotNull { it.toLongOrNull() }.toSet()
        set(value) = prefs.edit { putStringSet(KEY_DISABLES, value.map { it.toString() }.toSet()) }

    var pendingExtras: List<PendingExtra>
        get() = prefs.getString(KEY_EXTRAS, null)?.let { runCatching { WakerooJson.decodeFromString<List<PendingExtra>>(it) }.getOrNull() }
            ?: emptyList()
        set(value) = prefs.edit { putString(KEY_EXTRAS, WakerooJson.encodeToString(value)) }

    var ringPrefs: RingPrefs
        get() = prefs.getString(KEY_RING_PREFS, null)?.let { runCatching { WakerooJson.decodeFromString<RingPrefs>(it) }.getOrNull() }
            ?: RingPrefs()
        set(value) = prefs.edit { putString(KEY_RING_PREFS, WakerooJson.encodeToString(value)) }

    /** The tone each alarm played last time, so rotation never repeats it. */
    fun lastTone(alarmId: Long): String? = prefs.getString("$KEY_LAST_TONE$alarmId", null)

    fun setLastTone(alarmId: Long, tone: String) = prefs.edit { putString("$KEY_LAST_TONE$alarmId", tone) }

    private companion object {
        const val KEY_RING_PREFS = "ring_prefs"
        const val KEY_LAST_TONE = "last_tone_"
        const val KEY_EXTRAS = "pending_extras"
        const val KEY_ALARMS = "alarms"
        const val KEY_ACTIVE = "active_ring"
        const val KEY_SCHEDULED = "scheduled_ids"
        const val KEY_EVENTS = "pending_events"
        const val KEY_EXPECTED = "expected_triggers"
        const val KEY_DISABLES = "pending_disables"
    }
}
