package com.wakeroo.app.alarm.sound

import com.wakeroo.app.ui.i18n.tr
import com.wakeroo.app.ui.i18n.n
import android.content.Context
import android.net.Uri
import com.wakeroo.app.R
import com.wakeroo.app.data.model.Alarm
import com.wakeroo.app.data.model.SoundMode
import kotlin.random.Random

enum class ToneCategory(private val titleEn: String, private val blurbEn: String) {
    Gentle(n("Gentle"), n("Soft and warm, for light sleepers")),
    Energetic(n("Energetic"), n("Bright and upbeat")),
    Extreme(n("Extreme"), n("For heavy sleepers. Very loud"));

    val title: String get() = tr(titleEn)
    val blurb: String get() = tr(blurbEn)
}

/** A tone bundled with the app. All are synthesised by tools/make_tones.py: no licences. */
data class Tone(val id: String, private val nameEn: String, val category: ToneCategory, val res: Int, val pro: Boolean = false) {
    val name: String get() = tr(nameEn)
}

object Tones {
    const val DEFAULT = "wakeroo_rise"

    val all = listOf(
        Tone("dawn_chimes", n("Dawn Chimes"), ToneCategory.Gentle, R.raw.tone_dawn_chimes),
        Tone("soft_marimba", n("Soft Marimba"), ToneCategory.Gentle, R.raw.tone_soft_marimba),
        Tone("birdsong_bells", n("Birdsong Bells"), ToneCategory.Gentle, R.raw.tone_birdsong_bells, pro = true),
        Tone("warm_pads", n("Warm Pads"), ToneCategory.Gentle, R.raw.tone_warm_pads, pro = true),
        Tone("wakeroo_rise", n("Wakeroo Rise"), ToneCategory.Energetic, R.raw.tone_wakeroo_rise),
        Tone("bright_pulse", n("Bright Pulse"), ToneCategory.Energetic, R.raw.tone_bright_pulse),
        Tone("steel_drum_run", n("Steel Drum Run"), ToneCategory.Energetic, R.raw.tone_steel_drum_run, pro = true),
        Tone("fanfare", n("Fanfare"), ToneCategory.Energetic, R.raw.tone_fanfare, pro = true),
        Tone("klaxon", n("Klaxon"), ToneCategory.Extreme, R.raw.tone_klaxon),
        Tone("siren_sweep", n("Siren Sweep"), ToneCategory.Extreme, R.raw.tone_siren_sweep),
        Tone("bell_storm", n("Bell Storm"), ToneCategory.Extreme, R.raw.tone_bell_storm, pro = true),
        Tone("buzzer", n("Buzzer"), ToneCategory.Extreme, R.raw.tone_buzzer, pro = true),
    )

    fun byId(id: String?): Tone = all.firstOrNull { it.id == id } ?: all.first { it.id == DEFAULT }

    fun inCategory(category: ToneCategory) = all.filter { it.category == category }

    fun uri(context: Context, tone: Tone): Uri = Uri.parse("android.resource://${context.packageName}/${tone.res}")

    /**
     * The tone for this morning. Rotate picks another tone from the same category, never the
     * one from last time, so the ear doesn't learn to sleep through it.
     */
    fun forRing(alarm: Alarm, lastToneId: String?, random: Random = Random.Default): Tone {
        val chosen = byId(alarm.tone)
        if (alarm.soundMode != SoundMode.Rotate) return chosen
        val pool = inCategory(chosen.category).filter { it.id != lastToneId }
        return pool.random(random)
    }

    /** The loud tone an Escalate alarm switches to after a minute without a response. */
    fun escalationFor(start: Tone): Tone =
        if (start.category == ToneCategory.Extreme) start else byId(if (start.id == "klaxon") "siren_sweep" else "klaxon")
}
