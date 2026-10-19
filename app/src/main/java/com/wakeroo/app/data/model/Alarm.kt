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
    Steps("Steps", true),
    Memory("Memory tiles", false),
    Nfc("NFC tag", true),
    Plan("Type your plan", false),
    Hunt("Item hunt", true);

    /** Missions that need something registered before they can run. */
    val needsSetupStep: Boolean get() = this == Scan || this == Photo || this == Nfc

    val usesCamera: Boolean get() = this == Scan || this == Photo || this == Hunt
}

@Serializable
data class MissionConfig(
    val type: MissionType,
    /** 1 = easy, 2 = medium, 3 = hard. */
    val difficulty: Int = 2,
    /** Problems, phrases, shakes, steps, memory rounds or objects, depending on [type]. */
    val count: Int = defaultCount(type),
    /** Registered QR/barcode value for [MissionType.Scan]. */
    val codeValue: String? = null,
    val codeLabel: String? = null,
    /** Reference photo for [MissionType.Photo], in app-private credential storage. */
    val photoPath: String? = null,
    val photoLabels: List<String> = emptyList(),
    val photoHash: Long? = null,
    /** Hex UID of the registered sticker for [MissionType.Nfc]. */
    val nfcTagId: String? = null,
    val nfcLabel: String? = null,
    /** Memory tiles grid edge: 3 (3×3) or 4 (4×4). */
    val gridSize: Int = 3,
    /**
     * Chosen during onboarding with "I'll set it up tonight": the mission isn't registered yet,
     * so math runs in its place until it is.
     */
    val needsSetup: Boolean = false,
) {
    /** True when the mission has everything it needs to run at alarm time. */
    val isReady: Boolean
        get() = !needsSetup && when (type) {
            MissionType.Scan -> true // "any code" is a valid choice
            MissionType.Photo -> photoHash != null
            MissionType.Nfc -> nfcTagId != null
            else -> true
        }

    companion object {
        fun defaultCount(type: MissionType) = when (type) {
            MissionType.Math -> 3
            MissionType.Typing -> 2
            MissionType.Shake -> 40
            MissionType.Steps -> 40
            MissionType.Memory -> 4
            MissionType.Hunt -> 1
            MissionType.Scan, MissionType.Photo, MissionType.Nfc, MissionType.Plan -> 1
        }
    }
}

/** How the alarm picks its sound each morning. */
@Serializable
enum class SoundMode { Single, Rotate, Escalate }

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
    /** Content URI of a phone ringtone or audio file. Takes priority over [tone]. */
    val soundUri: String? = null,
    val soundName: String? = null,
    /** A bundled Wakeroo tone id (see `alarm.sound.Tones`); null is Wakeroo Rise. */
    val tone: String? = null,
    val soundMode: SoundMode = SoundMode.Single,
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
    /** Pro: turning off, deleting, skipping or snoozing needs a mission first. */
    val hardMode: Boolean = false,
    /** Motion scene id for the ring screen; null follows the app-wide choice. */
    val scene: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val isRepeating: Boolean get() = date == null && days.isNotEmpty()
}
