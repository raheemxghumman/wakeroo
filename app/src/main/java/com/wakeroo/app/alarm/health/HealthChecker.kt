package com.wakeroo.app.alarm.health

import android.app.NotificationManager
import android.app.usage.UsageStatsManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.wakeroo.app.alarm.Notifications
import com.wakeroo.app.alarm.ring.AlarmPlayer
import com.wakeroo.app.container

enum class Health { Good, Warn, Bad }

data class HealthItem(
    val id: String,
    val title: String,
    val status: Health,
    val detail: String,
    /** What tapping "Fix" opens; null when there is nothing to fix. */
    val fix: (() -> Intent)? = null,
)

/**
 * Checks everything that can stop an alarm from ringing. Most "my alarm didn't ring" reviews
 * trace back to one of these, so the app shows them openly instead of hoping for the best.
 */
class HealthChecker(private val context: Context) {

    fun check(lastTestAt: Long, lastTestPassed: Boolean): List<HealthItem> = buildList {
        add(exactAlarms())
        add(notifications())
        fullScreen()?.let(::add)
        add(battery())
        standby()?.let(::add)
        add(volume())
        output()?.let(::add)
        dnd()?.let(::add)
        add(test(lastTestAt, lastTestPassed))
    }

    fun overall(items: List<HealthItem>): Health = when {
        items.any { it.status == Health.Bad } -> Health.Bad
        items.any { it.status == Health.Warn } -> Health.Warn
        else -> Health.Good
    }

    private fun exactAlarms(): HealthItem {
        val ok = context.container.scheduler.canScheduleExact()
        return HealthItem(
            "exact", "On-time alarms", if (ok) Health.Good else Health.Bad,
            if (ok) "Allowed. Alarms ring to the minute." else "Blocked. Alarms may ring late.",
            fix = if (ok || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) null else {
                { Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")) }
            },
        )
    }

    private fun notifications(): HealthItem {
        val nm = NotificationManagerCompat.from(context)
        val channel = nm.getNotificationChannel(Notifications.CHANNEL_RINGING)
        val ok = nm.areNotificationsEnabled() && (channel == null || channel.importance >= NotificationManager.IMPORTANCE_HIGH)
        return HealthItem(
            "notifications", "Alarm alerts", if (ok) Health.Good else Health.Bad,
            if (ok) "On." else "Off. The alarm screen can't appear.",
            fix = if (ok) null else {
                { Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName) }
            },
        )
    }

    private fun fullScreen(): HealthItem? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return null
        val ok = context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
        return HealthItem(
            "fullscreen", "Full-screen alarm", if (ok) Health.Good else Health.Bad,
            if (ok) "The alarm takes over the lock screen." else "Off. You'd only get a small banner.",
            fix = if (ok) null else {
                { Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${context.packageName}")) }
            },
        )
    }

    private fun battery(): HealthItem {
        val ok = context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
        val guide = OemGuides.forThisPhone()
        return HealthItem(
            "battery", "Battery saver", if (ok) Health.Good else Health.Warn,
            if (ok) "Unrestricted. ${guide.brand} won't put Wakeroo to sleep." else "${guide.brand} may pause Wakeroo to save battery. Set it to Unrestricted.",
            fix = if (ok) null else { { OemGuides.batteryIntent(context) } },
        )
    }

    private fun standby(): HealthItem? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        val bucket = context.getSystemService(UsageStatsManager::class.java).appStandbyBucket
        val restricted = bucket >= UsageStatsManager.STANDBY_BUCKET_RESTRICTED
        if (!restricted) return null
        return HealthItem(
            "standby", "App sleeping", Health.Bad, "Android marked Wakeroo as rarely used. Open it daily or add the widget.",
            fix = { appDetails(context) },
        )
    }

    private fun volume(): HealthItem {
        val audio = context.getSystemService(AudioManager::class.java)
        val level = audio.getStreamVolume(AudioManager.STREAM_ALARM)
        return HealthItem(
            "volume", "Alarm volume", if (level > 0) Health.Good else Health.Warn,
            if (level > 0) "Wakeroo raises it to your alarm's level when ringing." else "At zero. Wakeroo raises it when ringing, but check your phone's settings.",
        )
    }

    /** Only shown while headphones or a Bluetooth speaker are connected. */
    private fun output(): HealthItem? {
        if (!AlarmPlayer.externalOutputConnected(context)) return null
        val speaker = context.container.directBoot.ringPrefs.alwaysSpeaker
        return HealthItem(
            "output", "Headphones connected", if (speaker) Health.Good else Health.Warn,
            if (speaker) "Alarms still ring on the phone speaker." else "Alarms may only play in your headphones. Turn on \"Always ring on the phone speaker\" in Settings.",
        )
    }

    private fun dnd(): HealthItem? {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL) return null
        val alarmsAllowed = nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_NONE &&
            (nm.notificationPolicy.priorityCategories and NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS) != 0
        return HealthItem(
            "dnd", "Do Not Disturb", if (alarmsAllowed) Health.Good else Health.Bad,
            if (alarmsAllowed) "On, and alarms are allowed through." else "On, and it blocks alarms. Allow alarms in Do Not Disturb.",
            fix = if (alarmsAllowed) null else { { Intent(Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS) } },
        )
    }

    private fun test(at: Long, passed: Boolean): HealthItem {
        val status = when {
            at == 0L -> Health.Warn
            !passed -> Health.Bad
            else -> Health.Good
        }
        val text = when (status) {
            Health.Warn -> "Not tried yet. One minute proves your alarms work."
            Health.Bad -> "The last test didn't finish. Try again."
            Health.Good -> "Passed. Your phone rings Wakeroo alarms properly."
        }
        return HealthItem("test", "Test alarm", status, text)
    }

    companion object {
        fun appDetails(context: Context) =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
    }
}

data class OemGuide(val brand: String, val steps: List<String>)

/**
 * Phone makers add their own battery killers on top of Android. No code can switch these off,
 * so Wakeroo shows the exact steps for the user's brand and opens the closest settings page.
 */
object OemGuides {
    fun forThisPhone(manufacturer: String = Build.MANUFACTURER): OemGuide = when (manufacturer.lowercase()) {
        "samsung" -> OemGuide("Samsung", listOf(
            "Settings › Apps › Wakeroo › Battery › Unrestricted",
            "Settings › Battery › Background usage limits › Never sleeping apps › add Wakeroo",
            "Turn off \"Put unused apps to sleep\"",
        ))
        "xiaomi", "redmi", "poco" -> OemGuide("Xiaomi", listOf(
            "Settings › Apps › Manage apps › Wakeroo › Autostart: on",
            "Battery saver › No restrictions",
            "In Recents, pull Wakeroo down to lock it",
        ))
        "oppo", "realme", "oneplus" -> OemGuide(manufacturer.replaceFirstChar { it.uppercase() }, listOf(
            "Settings › Apps › Wakeroo › Battery usage › Allow background activity",
            "Allow auto launch",
            "Turn off \"Optimize battery use\" for Wakeroo",
        ))
        "vivo", "iqoo" -> OemGuide("Vivo", listOf(
            "Settings › Battery › Background power consumption › Wakeroo › Allow",
            "i Manager › App manager › Autostart › Wakeroo: on",
        ))
        "huawei", "honor" -> OemGuide(manufacturer.replaceFirstChar { it.uppercase() }, listOf(
            "Settings › Battery › App launch › Wakeroo › Manage manually",
            "Turn on Auto-launch, Secondary launch and Run in background",
        ))
        "motorola" -> OemGuide("Motorola", listOf("Settings › Apps › Wakeroo › Battery › Unrestricted"))
        else -> OemGuide(
            manufacturer.replaceFirstChar { it.uppercase() }.ifBlank { "Your phone" },
            listOf("Settings › Apps › Wakeroo › Battery › Unrestricted"),
        )
    }

    /** The best page we can open for the brand's battery controls, falling back to app details. */
    fun batteryIntent(context: Context): Intent {
        val candidates = when (Build.MANUFACTURER.lowercase()) {
            "xiaomi", "redmi", "poco" -> listOf(
                Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
            )
            "huawei", "honor" -> listOf(
                Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")),
            )
            "vivo", "iqoo" -> listOf(
                Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")),
            )
            else -> emptyList()
        } + Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        return candidates.firstOrNull { it.resolveActivity(context.packageManager) != null } ?: HealthChecker.appDetails(context)
    }

    fun open(context: Context, intent: Intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            context.startActivity(HealthChecker.appDetails(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: SecurityException) {
            context.startActivity(HealthChecker.appDetails(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
