package com.wakeroo.app.alarm

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.wakeroo.app.MainActivity
import com.wakeroo.app.R
import com.wakeroo.app.alarm.ring.RingActivity

object Notifications {
    const val CHANNEL_RINGING = "ringing"
    const val CHANNEL_ALERTS = "alerts"
    const val ID_RINGING = 1
    const val ID_RINGING_ALT = 2
    const val ID_MISSED = 10

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_RINGING, "Ringing alarms", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Shows the alarm screen when an alarm rings"
                setSound(null, null) // Wakeroo plays its own sound on the alarm stream
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERTS, "Alarm health", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Tells you if an alarm was missed and how to fix it"
            },
        )
    }

    fun ringing(context: Context, title: String, text: String): Notification {
        val fullScreen = PendingIntent.getActivity(
            context, 1, Intent(context, RingActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_RINGING)
            .setSmallIcon(R.drawable.ic_stat_wakeroo)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(false)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .addAction(0, "Open mission", fullScreen)
            .build()
    }

    fun missed(context: Context, label: String, whenText: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val open = PendingIntent.getActivity(
            context, 2, Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_HEALTH, true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_wakeroo)
            .setContentTitle("Your $whenText alarm didn't ring")
            .setContentText(if (label.isBlank()) "Tap to see why and fix it in one minute." else "\"$label\". Tap to see why and fix it.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(ID_MISSED, n)
    }
}
