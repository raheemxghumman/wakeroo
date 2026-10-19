package com.wakeroo.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.wakeroo.app.data.model.Alarm
import com.wakeroo.app.data.model.MissionConfig
import com.wakeroo.app.data.model.SoundMode
import com.wakeroo.app.data.model.WakerooJson
import com.wakeroo.app.data.model.toDaySet
import com.wakeroo.app.data.model.toMask
import java.time.LocalDate

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val daysMask: Int,
    val date: String?,
    val label: String,
    val enabled: Boolean,
    val missionsJson: String,
    val soundUri: String?,
    val soundName: String?,
    val volume: Int,
    val rampSeconds: Int,
    val vibrate: Boolean,
    val snoozeMinutes: Int,
    val snoozeLimit: Int,
    val wakeCheckMinutes: Int,
    val skipDate: String?,
    val createdAt: Long,
    // Added in schema v2.
    val tone: String? = null,
    @ColumnInfo(defaultValue = "Single") val soundMode: String = SoundMode.Single.name,
    @ColumnInfo(defaultValue = "0") val hardMode: Boolean = false,
    val scene: String? = null,
)

enum class RingOutcome { Dismissed, Escaped, Missed, Snoozed }

@Entity(tableName = "ring_events", indices = [Index("firedAt")])
data class RingEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val alarmId: Long,
    val label: String,
    val scheduledAt: Long,
    val firedAt: Long,
    val endedAt: Long?,
    val outcome: RingOutcome,
    val snoozes: Int,
    /** Seconds from the first ring to finishing the missions. */
    val secondsToWake: Int?,
    /** null = no check, true = passed, false = failed and re-rang. */
    val wakeCheckPassed: Boolean?,
    /** Test alarms are logged for the health check but excluded from stats. */
    val isTest: Boolean = false,
)

/** One "morning feeling" per day; [date] is ISO yyyy-MM-dd in local time. */
@Entity(tableName = "mood_log")
data class MoodEntity(
    @PrimaryKey val date: String,
    val mood: Int,
    val loggedAt: Long,
)

/** A habit on the morning routine checklist. */
@Entity(tableName = "routine_item")
data class RoutineItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val position: Int,
)

/** A routine item ticked off on a given day. */
@Entity(tableName = "routine_log", primaryKeys = ["date", "itemId"])
data class RoutineLogEntity(
    val date: String,
    val itemId: Long,
    val doneAt: Long,
)

@Entity(tableName = "saved_quote")
data class SavedQuoteEntity(
    @PrimaryKey val quoteId: String,
    val savedAt: Long,
)

fun AlarmEntity.toModel() = Alarm(
    id = id, hour = hour, minute = minute, days = daysMask.toDaySet(), date = date?.let(LocalDate::parse),
    label = label, enabled = enabled, missions = WakerooJson.decodeFromString<List<MissionConfig>>(missionsJson),
    soundUri = soundUri, soundName = soundName, tone = tone,
    soundMode = runCatching { SoundMode.valueOf(soundMode) }.getOrDefault(SoundMode.Single),
    volume = volume, rampSeconds = rampSeconds, vibrate = vibrate,
    snoozeMinutes = snoozeMinutes, snoozeLimit = snoozeLimit, wakeCheckMinutes = wakeCheckMinutes,
    skipDate = skipDate?.let(LocalDate::parse), hardMode = hardMode, scene = scene, createdAt = createdAt,
)

fun Alarm.toEntity() = AlarmEntity(
    id = id, hour = hour, minute = minute, daysMask = days.toMask(), date = date?.toString(), label = label,
    enabled = enabled, missionsJson = WakerooJson.encodeToString(missions), soundUri = soundUri, soundName = soundName,
    volume = volume, rampSeconds = rampSeconds, vibrate = vibrate, snoozeMinutes = snoozeMinutes,
    snoozeLimit = snoozeLimit, wakeCheckMinutes = wakeCheckMinutes, skipDate = skipDate?.toString(), createdAt = createdAt,
    tone = tone, soundMode = soundMode.name, hardMode = hardMode, scene = scene,
)
