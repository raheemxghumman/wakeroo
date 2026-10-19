package com.wakeroo.app.alarm.ring

import android.content.Context
import android.media.AudioAttributes
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

/**
 * Plays the alarm on the alarm stream with a gradual ramp, and guards the volume: if the user
 * turns the alarm stream down while ringing, it comes back up. The user's own alarm volume is
 * restored when ringing stops.
 */
class AlarmPlayer(private val context: Context) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var originalVolume: Int? = null
    private var startedAt = 0L
    private var rampMs = 0L
    private var targetFraction = 0.85f
    private var duckUntil = 0L

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

    fun start(soundUri: String?, volumePercent: Int, rampSeconds: Int, vibrate: Boolean, gentle: Boolean = false) {
        stop()
        originalVolume = audio.getStreamVolume(AudioManager.STREAM_ALARM)
        targetFraction = (volumePercent.coerceIn(10, 100) / 100f) * if (gentle) 0.6f else 1f
        rampMs = rampSeconds * 1000L
        startedAt = System.currentTimeMillis()
        player = createPlayer(soundUri)
        applyVolume()
        handler.post(tick)
        if (vibrate) {
            val pattern = longArrayOf(0, 600, 400, 600, 400, 1200, 800)
            val amps = intArrayOf(0, 180, 0, 220, 0, 255, 0)
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, amps, 1))
        }
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
        handler.removeCallbacks(tick)
        player?.runCatching { stop(); release() }
        player = null
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

    /** Custom sound, then the system alarm tone, then Wakeroo's bundled tone. Never silent. */
    private fun createPlayer(soundUri: String?): MediaPlayer? {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val candidates = listOfNotNull(
            soundUri?.let(Uri::parse),
            Uri.parse("android.resource://${context.packageName}/${R.raw.wakeroo_rise}"),
            RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM),
        )
        for (uri in candidates) {
            val mp = MediaPlayer()
            try {
                mp.setAudioAttributes(attrs)
                mp.setDataSource(context, uri)
                mp.isLooping = true
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
}
