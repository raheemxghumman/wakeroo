package com.wakeroo.app.sleep

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/** A sleep sound. All are synthesised live, so they never loop audibly and cost no APK space. */
enum class SleepSound(val id: String, val title: String, val pro: Boolean) {
    Rain("rain", "Rain", false),
    Ocean("ocean", "Ocean waves", false),
    White("white", "White noise", false),
    Brown("brown", "Brown noise", false),
    Pink("pink", "Pink noise", true),
    Fan("fan", "Fan", true),
    Crickets("crickets", "Crickets", true);

    companion object {
        fun byId(id: String) = entries.firstOrNull { it.id == id }
    }
}

/** Produces one sound's samples, block by block. Each generator keeps its own filter state. */
private abstract class Generator(protected val rate: Int) {
    protected val rnd = Random(System.nanoTime())
    protected fun white() = rnd.nextFloat() * 2f - 1f
    abstract fun next(): Float
}

/** Paul Kellet's economy pink filter. */
private class PinkFilter {
    private var b0 = 0f; private var b1 = 0f; private var b2 = 0f
    fun apply(w: Float): Float {
        b0 = 0.99765f * b0 + w * 0.0990460f
        b1 = 0.96300f * b1 + w * 0.2965164f
        b2 = 0.57000f * b2 + w * 1.0526913f
        return (b0 + b1 + b2 + w * 0.1848f) * 0.18f
    }
}

private class WhiteGen(rate: Int) : Generator(rate) {
    override fun next() = white() * 0.35f
}

private class PinkGen(rate: Int) : Generator(rate) {
    private val pink = PinkFilter()
    override fun next() = pink.apply(white()) * 0.9f
}

private class BrownGen(rate: Int) : Generator(rate) {
    private var last = 0f
    override fun next(): Float {
        last = (last + white() * 0.02f) * 0.998f
        return (last * 3.2f).coerceIn(-1f, 1f)
    }
}

/** Steady hiss of drops plus heavier droplets that patter randomly. */
private class RainGen(rate: Int) : Generator(rate) {
    private val pink = PinkFilter()
    private var hp = 0f; private var lastIn = 0f
    private var drop = 0f; private var dropTone = 0f; private var phase = 0f
    private var swell = 0f
    private var t = 0L
    override fun next(): Float {
        t++
        val p = pink.apply(white())
        // A gentle high-pass keeps it airy instead of rumbly.
        hp = 0.97f * (hp + p - lastIn); lastIn = p
        if (rnd.nextFloat() < 0.0009f) { drop = 0.25f + rnd.nextFloat() * 0.5f; dropTone = 1800f + rnd.nextFloat() * 2600f }
        drop *= 0.996f
        phase += (2 * PI * dropTone / rate).toFloat()
        val droplet = drop * sin(phase) * 0.08f
        val s = (t / rate.toFloat())
        swell = 0.85f + 0.15f * sin(s * 0.13f).toFloat()
        return (hp * 1.1f * swell + droplet)
    }
}

/** Brown noise shaped by slow, uneven swells: waves rolling in and drawing back. */
private class OceanGen(rate: Int) : Generator(rate) {
    private val brown = BrownGen(rate)
    private val pink = PinkFilter()
    private var t = 0L
    override fun next(): Float {
        t++
        val s = t / rate.toFloat()
        val wave = (0.5f - 0.5f * kotlin.math.cos(2 * PI * s / 9.5).toFloat())
        val wave2 = (0.5f - 0.5f * kotlin.math.cos(2 * PI * s / 13.7 + 1.3).toFloat())
        val env = 0.18f + 0.82f * (wave * 0.65f + wave2 * 0.35f)
        val foam = pink.apply(white()) * env * env * 0.7f
        return brown.next() * env * 0.75f + foam
    }
}

/** A steady motor hum under soft airflow. */
private class FanGen(rate: Int) : Generator(rate) {
    private val brown = BrownGen(rate)
    private var phase = 0.0
    private var t = 0L
    override fun next(): Float {
        t++
        phase += 2 * PI * 96.0 / rate
        val wobble = 1f + 0.03f * sin(t / rate.toFloat() * 0.7f)
        val hum = (sin(phase) * 0.06 + sin(phase * 2) * 0.03 + sin(phase * 3) * 0.015).toFloat() * wobble
        return brown.next() * 0.8f + hum
    }
}

/** Bursts of three quick chirps at a cricket's pitch over a quiet night bed. */
private class CricketGen(rate: Int) : Generator(rate) {
    private val pink = PinkFilter()
    private var phase = 0.0
    private var t = 0L
    private var nextBurst = rate / 2L
    private var burstStart = -1L
    private var pitch = 4400.0
    override fun next(): Float {
        t++
        if (t >= nextBurst) {
            burstStart = t
            pitch = 4200.0 + rnd.nextDouble() * 500
            nextBurst = t + (rate * (0.7 + rnd.nextDouble() * 0.9)).toLong()
        }
        var chirp = 0f
        if (burstStart >= 0) {
            val local = (t - burstStart) / rate.toFloat()
            val pulse = (local / 0.045f).toInt()
            val inPulse = (local % 0.045f) / 0.028f
            if (pulse < 3 && inPulse <= 1f) {
                phase += 2 * PI * pitch / rate
                chirp = (sin(phase) * sin(PI * inPulse)).toFloat() * 0.12f
            }
        }
        return pink.apply(white()) * 0.12f + chirp
    }
}

/**
 * Mixes up to two sleep sounds into an [AudioTrack] on the media stream, so they follow media
 * volume and never touch the alarm stream. Runs on its own thread.
 */
class NoiseEngine {
    private val rate = 44100
    @Volatile private var running = false
    @Volatile private var layers: List<Pair<SleepSound, Float>> = emptyList()
    /** Master gain 0..1, used for the fade-out at the end of the timer. */
    @Volatile var gain = 1f
    private var thread: Thread? = null
    private var track: AudioTrack? = null

    fun setLayers(sounds: List<Pair<SleepSound, Float>>) {
        layers = sounds
    }

    fun start() {
        if (running) return
        running = true
        val minBuf = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT)
        val t = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_FLOAT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(maxOf(minBuf, 8192 * 4))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track = t
        t.play()
        thread = Thread({ loop(t) }, "wakeroo-sleep-sounds").apply { priority = Thread.MAX_PRIORITY; start() }
    }

    private fun loop(t: AudioTrack) {
        val generators = mutableMapOf<SleepSound, Generator>()
        val smoothGain = mutableMapOf<SleepSound, Float>()
        val block = FloatArray(2048)
        var master = 0f
        while (running) {
            val targets = layers.toMap()
            for (sound in targets.keys) generators.getOrPut(sound) { create(sound) }
            for (i in block.indices) {
                var mix = 0f
                for ((sound, gen) in generators) {
                    // Each layer glides to its volume (or out to silence), so changes never click.
                    val g = smoothGain.getOrDefault(sound, 0f)
                    val ng = g + ((targets[sound] ?: 0f) - g) * 0.0005f
                    smoothGain[sound] = ng
                    mix += gen.next() * ng
                }
                master += (gain - master) * 0.0002f
                block[i] = softClip(mix * master)
            }
            // Drop layers that have fully faded out.
            generators.keys.filter { it !in targets && (smoothGain[it] ?: 0f) < 0.0005f }.forEach {
                generators.remove(it); smoothGain.remove(it)
            }
            t.write(block, 0, block.size, AudioTrack.WRITE_BLOCKING)
        }
    }

    private fun softClip(x: Float): Float = if (x > 1f || x < -1f) (2f / (1f + exp(-2f * x)) - 1f) else x

    private fun create(sound: SleepSound): Generator = when (sound) {
        SleepSound.Rain -> RainGen(rate)
        SleepSound.Ocean -> OceanGen(rate)
        SleepSound.White -> WhiteGen(rate)
        SleepSound.Brown -> BrownGen(rate)
        SleepSound.Pink -> PinkGen(rate)
        SleepSound.Fan -> FanGen(rate)
        SleepSound.Crickets -> CricketGen(rate)
    }

    fun stop() {
        running = false
        thread?.join(500)
        thread = null
        track?.runCatching { stop(); release() }
        track = null
    }
}
