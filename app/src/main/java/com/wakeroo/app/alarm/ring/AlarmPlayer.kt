package com.wakeroo.app.alarm.ring

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.wakeroo.app.R
import kotlin.math.roundToInt

/** Where the alarm sound goes when headphones or a Bluetooth speaker are connected. */
enum class SoundRoute { SpeakerOnly, SpeakerAndHeadphones, FollowPhone }

/**
 * Plays the alarm on the alarm stream with a gradual ramp, and guards the volume: if the user
 * turns the alarm stream down while ringing, it comes back up. The user's own alarm volume is
 * restored when ringing stops. Can crossfade to another sound (escalation) without a gap.
 */
class AlarmPlayer(private val context: Context) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var fading: MediaPlayer? = null
    private var originalVolume: Int? = null
    private var startedAt = 0L
    private var rampMs = 0L
    private var targetFraction = 0.85f
    private var duckUntil = 0L
    private var route = SoundRoute.SpeakerOnly

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java).defaultVibrator
    } else {
        @Suppress("DEPRECATION") context.getSystemService(Vibrator::class.java)
    }

    private val tick = object : Runnable {
        override fun run() {
            applyVolume()
            handler.postDelayed(this, 500)
        }
    }

    fun start(
        sound: Uri?,
        volumePercent: Int,
        rampSeconds: Int,
        vibrate: Boolean,
        gentle: Boolean = false,
        route: SoundRoute = SoundRoute.SpeakerOnly,
    ) {
        stop()
        this.route = route
        originalVolume = audio.getStreamVolume(AudioManager.STREAM_ALARM)
        targetFraction = (volumePercent.coerceIn(10, 100) / 100f) * if (gentle) 0.6f else 1f
        rampMs = rampSeconds * 1000L
        startedAt = System.currentTimeMillis()
        player = createPlayer(sound)
        applyVolume()
        handler.post(tick)
        if (vibrate) {
            val pattern = longArrayOf(0, 600, 400, 600, 400, 1200, 800)
            val amps = intArrayOf(0, 180, 0, 220, 0, 255, 0)
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, amps, 1))
        }
    }

    /**
     * Escalation: blend into [sound] over [fadeMs] and go to full volume, no ramp. The old
     * sound keeps playing under the new one until the blend finishes, so there's never silence.
     */
    fun crossfadeTo(sound: Uri, fadeMs: Long = 3000) {
        val next = createPlayer(sound, fallbackOnly = false) ?: return
        fading?.runCatching { stop(); release() }
        fading = player
        player = next
        targetFraction = 1f
        rampMs = 0
        duckUntil = 0L
        val start = System.currentTimeMillis()
        val step = object : Runnable {
            override fun run() {
                val p = ((System.currentTimeMillis() - start) / fadeMs.toFloat()).coerceIn(0f, 1f)
                runCatching { next.setVolume(p, p) }
                fading?.runCatching { setVolume(1 - p, 1 - p) }
                if (p < 1f) handler.postDelayed(this, 50) else {
                    fading?.runCatching { stop(); release() }
                    fading = null
                }
            }
        }
        runCatching { next.setVolume(0f, 0f) }
        handler.post(step)
        applyVolume()
    }

    /** Drop to 20% while the user works on a mission; full volume returns after [holdMs] idle. */
    fun duckWhileSolving(holdMs: Long = 20_000) {
        duckUntil = System.currentTimeMillis() + holdMs
        applyVolume()
    }

    /** The mission time limit ran out: back to full volume straight away. */
    fun unduck() {
        duckUntil = 0L
        applyVolume()
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
        player?.runCatching { stop(); release() }
        fading?.runCatching { stop(); release() }
        player = null
        fading = null
        vibrator.cancel()
        originalVolume?.let { runCatching { audio.setStreamVolume(AudioManager.STREAM_ALARM, it, 0) } }
        originalVolume = null
        duckUntil = 0L
    }

    private fun applyVolume() {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val elapsed = System.currentTimeMillis() - startedAt
        val ramp = if (rampMs <= 0) 1f else (0.15f + 0.85f * (elapsed / rampMs.toFloat())).coerceAtMost(1f)
        var fraction = targetFraction * ramp
        if (System.currentTimeMillis() < duckUntil) fraction = minOf(fraction, 0.2f)
        val level = (max * fraction).roundToInt().coerceIn(1, max)
        if (audio.getStreamVolume(AudioManager.STREAM_ALARM) != level) {
            runCatching { audio.setStreamVolume(AudioManager.STREAM_ALARM, level, 0) }
        }
    }

    private fun speaker(): AudioDeviceInfo? =
        audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }

    /**
     * The chosen sound, then Wakeroo's bundled tone, then the system alarm tone. Never silent.
     * With [fallbackOnly] false (escalation) a failing sound returns null and the old one keeps going.
     */
    private fun createPlayer(sound: Uri?, fallbackOnly: Boolean = true): MediaPlayer? {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val candidates = if (!fallbackOnly) listOfNotNull(sound) else listOfNotNull(
            sound,
            Uri.parse("android.resource://${context.packageName}/${R.raw.wakeroo_rise}"),
            RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM),
        )
        for (uri in candidates) {
            val mp = MediaPlayer()
            try {
                mp.setAudioAttributes(attrs)
                mp.setDataSource(context, uri)
                mp.isLooping = true
                // A dead Bluetooth speaker or forgotten earbuds must never swallow the alarm.
                if (route == SoundRoute.SpeakerOnly) speaker()?.let { mp.setPreferredDevice(it) }
                mp.prepare()
                mp.start()
                return mp
            } catch (e: Exception) {
                Log.w("AlarmPlayer", "Could not play $uri, trying the next sound", e)
                mp.release()
            }
        }
        return null
    }

    companion object {
        /** True when headphones or a Bluetooth speaker could take the sound away from the phone. */
        fun externalOutputConnected(context: Context): Boolean {
            val audio = context.getSystemService(AudioManager::class.java)
            val external = setOf(
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO, AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_BLE_HEADSET, AudioDeviceInfo.TYPE_BLE_SPEAKER,
            )
            return audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { it.type in external }
        }
    }
}
