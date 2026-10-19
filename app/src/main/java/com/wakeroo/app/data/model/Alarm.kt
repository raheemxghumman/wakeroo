package com.wakeroo.app.data.model

import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDate

/** A wake-up mission the user must finish before the alarm goes quiet. */
@Serializable
enum class MissionType(val title: String, val worksInAnotherRoom: Boolean) {
    Math("Math", false),
    Typing("Typing", false),
    Shake("Shake", false),
    Scan("QR / Barcode", true),
    Photo("Photo spot", true),
}

@Serializable
data class MissionConfig(
    val type: MissionType,
    /** 1 = easy, 2 = medium, 3 = hard. */
    val difficulty: Int = 2,
    /** Problems to solve, phrases to type or shakes to count. */
    val count: Int = defaultCount(type),
    /** Registered QR/barcode value for [MissionType.Scan]. */
    val codeValue: String? = null,
    val codeLabel: String? = null,
    /** Reference photo for [MissionType.Photo], in app-private credential storage. */
    val photoPath: String? = null,
    val photoLabels: List<String> = emptyList(),
    val photoHash: Long? = null,
) {
    companion object {
        fun defaultCount(type: MissionType) = when (type) {
            MissionType.Math -> 3
            MissionType.Typing -> 2
            MissionType.Shake -> 40
            MissionType.Scan, MissionType.Photo -> 1
        }
    }
}

@Serializable
data class Alarm(
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    /** Empty means "once": the next time this clock time comes round. */
    @Serializable(with = DaySetSerializer::class)
    val days: Set<DayOfWeek> = emptySet(),
    /** A specific one-off date. Takes priority over [days]. */
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate? = null,
    val label: String = "",
    val enabled: Boolean = true,
    val missions: List<MissionConfig> = listOf(MissionConfig(MissionType.Math)),
    /** Content URI of a ringtone or audio file; null uses the bundled Wakeroo tone. */
    val soundUri: String? = null,
    val soundName: String? = null,
    /** Maximum alarm-stream volume, 0..100. */
    val volume: Int = 85,
    /** Seconds to ramp from quiet to [volume]. 0 starts at full volume. */
    val rampSeconds: Int = 30,
    val vibrate: Boolean = true,
    val snoozeMinutes: Int = 5,
    /** Snoozes allowed per ring; 0 disables snooze. */
    val snoozeLimit: Int = 2,
    /** Minutes after dismissal for the "Still up?" check; 0 turns it off. */
    val wakeCheckMinutes: Int = 3,
    /** A repeating alarm skips this one date. */
    @Serializable(with = LocalDateSerializer::class)
    val skipDate: LocalDate? = null,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val isRepeating: Boolean get() = date == null && days.isNotEmpty()
}
