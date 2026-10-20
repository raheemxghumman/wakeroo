package com.wakeroo.app.sleep

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.wakeroo.app.MainActivity
import com.wakeroo.app.R
import com.wakeroo.app.container
import com.wakeroo.app.feature.main.MainTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SleepPlayerState(
    val playing: Boolean = false,
    val paused: Boolean = false,
    val layers: List<Pair<SleepSound, Float>> = emptyList(),
    /** When the sounds will have faded out; 0 when not playing. */
    val endsAt: Long = 0L,
    /** True when the end is set by the next alarm rather than the timer. */
    val endsForAlarm: Boolean = false,
)

object SleepPlayer {
    private val _state = MutableStateFlow(SleepPlayerState())
    val state: StateFlow<SleepPlayerState> = _state.asStateFlow()
    internal fun publish(s: SleepPlayerState) { _state.value = s }

    /** [minutes] of -1 plays until a minute before the next alarm. */
    fun start(context: Context, layers: List<Pair<SleepSound, Float>>, minutes: Int) {
        val intent = Intent(context, SleepSoundService::class.java).setAction(SleepSoundService.ACTION_START)
            .putExtra(SleepSoundService.EXTRA_SOUNDS, layers.joinToString(",") { it.first.id })
            .putExtra(SleepSoundService.EXTRA_VOLUMES, layers.map { it.second }.toFloatArray())
            .putExtra(SleepSoundService.EXTRA_MINUTES, minutes)
        ContextCompat.startForegroundService(context, intent)
    }

    fun update(context: Context, layers: List<Pair<SleepSound, Float>>) {
        if (!state.value.playing) return
        context.startService(
            Intent(context, SleepSoundService::class.java).setAction(SleepSoundService.ACTION_UPDATE)
                .putExtra(SleepSoundService.EXTRA_SOUNDS, layers.joinToString(",") { it.first.id })
                .putExtra(SleepSoundService.EXTRA_VOLUMES, layers.map { it.second }.toFloatArray()),
        )
    }

    fun stop(context: Context) {
        if (state.value.playing) context.startService(Intent(context, SleepSoundService::class.java).setAction(SleepSoundService.ACTION_STOP))
    }

    fun togglePause(context: Context) {
        if (state.value.playing) context.startService(Intent(context, SleepSoundService::class.java).setAction(SleepSoundService.ACTION_PAUSE))
    }
}

/**
 * Plays sleep sounds as a media-playback foreground service. The sounds fade out over the
 * last two minutes of the timer, and always stop a minute before the next alarm so they can
 * never mask it.
 */
class SleepSoundService : Service() {
    private val engine = NoiseEngine()
    private val handler = Handler(Looper.getMainLooper())
    private var endsAt = 0L
    private var endsForAlarm = false
    private var layers: List<Pair<SleepSound, Float>> = emptyList()
    private var paused = false

    private val tick = object : Runnable {
        override fun run() {
            val left = endsAt - System.currentTimeMillis()
            when {
                left <= 0 -> return stopAll()
                left < FADE_MS -> engine.gain = (left / FADE_MS.toFloat()).coerceIn(0f, 1f) * if (paused) 0f else 1f
                else -> engine.gain = if (paused) 0f else 1f
            }
            if ((left / 1000) % 30 == 0L) notifyProgress()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> start(intent)
            ACTION_UPDATE -> {
                layers = parseLayers(intent)
                engine.setLayers(layers)
                publish(); notifyProgress()
            }
            ACTION_PAUSE -> {
                paused = !paused
                engine.gain = if (paused) 0f else 1f
                publish(); notifyProgress()
            }
            ACTION_STOP -> stopAll()
            else -> if (!SleepPlayer.state.value.playing) stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun start(intent: Intent) {
        createChannel(this)
        layers = parseLayers(intent).ifEmpty { listOf(SleepSound.Rain to 0.7f) }
        val minutes = intent.getIntExtra(EXTRA_MINUTES, 30)
        val now = System.currentTimeMillis()
        val nextAlarm = container.directBoot.expectedTriggers.values.filter { it > now }.minOrNull()
        val alarmCutoff = nextAlarm?.minus(STOP_BEFORE_ALARM_MS)
        val timerEnd = if (minutes > 0) now + minutes * 60_000L else null
        endsAt = listOfNotNull(timerEnd, alarmCutoff).minOrNull() ?: (now + 8 * 3_600_000L)
        endsForAlarm = alarmCutoff != null && endsAt == alarmCutoff
        paused = false
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        engine.setLayers(layers)
        engine.gain = 1f
        engine.start()
        publish()
        handler.removeCallbacks(tick)
        handler.post(tick)
    }

    private fun parseLayers(intent: Intent): List<Pair<SleepSound, Float>> {
        val ids = intent.getStringExtra(EXTRA_SOUNDS)?.split(',').orEmpty()
        val vols = intent.getFloatArrayExtra(EXTRA_VOLUMES) ?: FloatArray(ids.size) { 0.7f }
        return ids.mapIndexedNotNull { i, id -> SleepSound.byId(id)?.let { it to (vols.getOrNull(i) ?: 0.7f) } }.take(2)
    }

    private fun publish() = SleepPlayer.publish(SleepPlayerState(true, paused, layers, endsAt, endsForAlarm))

    private fun notifyProgress() {
        if (!SleepPlayer.state.value.playing) return
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification())
    }

    private fun notification(): Notification {
        val open = PendingIntent.getActivity(
            this, 30, Intent(this, MainActivity::class.java).putExtra(MainActivity.EXTRA_TAB, MainTab.Sleep.name),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        fun action(id: Int, action: String) = PendingIntent.getService(
            this, id, Intent(this, SleepSoundService::class.java).setAction(action), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val minutesLeft = ((endsAt - System.currentTimeMillis()) / 60_000).coerceAtLeast(0)
        val names = layers.joinToString(" + ") { it.first.title }
        return NotificationCompat.Builder(this, CHANNEL_SLEEP)
            .setSmallIcon(R.drawable.ic_stat_wakeroo)
            .setContentTitle(if (paused) "Paused · $names" else names)
            .setContentText(if (endsForAlarm) "Fades out before your alarm · $minutesLeft min" else "$minutesLeft min left, then a gentle fade")
            .setOngoing(!paused)
            .setSilent(true)
            .setContentIntent(open)
            .addAction(0, if (paused) "Resume" else "Pause", action(31, ACTION_PAUSE))
            .addAction(0, "Stop", action(32, ACTION_STOP))
            .build()
    }

    private fun stopAll() {
        handler.removeCallbacks(tick)
        engine.stop()
        SleepPlayer.publish(SleepPlayerState())
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        engine.stop()
        SleepPlayer.publish(SleepPlayerState())
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.wakeroo.app.sleep.START"
        const val ACTION_UPDATE = "com.wakeroo.app.sleep.UPDATE"
        const val ACTION_PAUSE = "com.wakeroo.app.sleep.PAUSE"
        const val ACTION_STOP = "com.wakeroo.app.sleep.STOP"
        const val EXTRA_SOUNDS = "sounds"
        const val EXTRA_VOLUMES = "volumes"
        const val EXTRA_MINUTES = "minutes"
        const val CHANNEL_SLEEP = "sleep_sounds"
        private const val NOTIFICATION_ID = 40
        private const val FADE_MS = 120_000L
        private const val STOP_BEFORE_ALARM_MS = 60_000L

        fun createChannel(context: Context) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_SLEEP, "Sleep sounds", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Controls for sleep sounds while they play"
                    setSound(null, null)
                },
            )
        }
    }
}
