package com.wakeroo.app.widget

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.wakeroo.app.MainActivity
import com.wakeroo.app.R
import com.wakeroo.app.alarm.NextTriggerCalculator
import com.wakeroo.app.container
import com.wakeroo.app.ui.util.formatClock
import com.wakeroo.app.ui.util.formatMeridiem
import com.wakeroo.app.ui.util.formatUntil
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 * "Next alarm" on the home screen. It reads the direct-boot mirror, so it works before the
 * first unlock, and an active widget also keeps Android from parking Wakeroo as unused.
 */
class NextAlarmWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val alarms = context.container.directBoot.readAlarms()
        val now = System.currentTimeMillis()
        val next = alarms.mapNotNull { a -> NextTriggerCalculator.nextMillis(a, now)?.let { a to it } }.minByOrNull { it.second }
        val use24h = DateFormat.is24HourFormat(context)
        provideContent {
            GlanceTheme {
                Content(
                    time = next?.let { formatClock(it.first.hour, it.first.minute, use24h) },
                    meridiem = next?.let { formatMeridiem(it.first.hour, use24h) } ?: "",
                    until = next?.let { Duration.between(Instant.ofEpochMilli(now), Instant.ofEpochMilli(it.second)).formatUntil() },
                    day = next?.let { Instant.ofEpochMilli(it.second).atZone(ZoneId.systemDefault()).dayOfWeek.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                    label = next?.first?.label.orEmpty(),
                )
            }
        }
    }

    @Composable
    private fun Content(time: String?, meridiem: String, until: String?, day: String?, label: String) {
        val paper = ColorProvider(Color(0xFFF5F3EE))
        val muted = ColorProvider(Color(0xFFB9A7FF))
        Column(
            GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF0E1120))
                .cornerRadius(28.dp)
                .padding(16.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(ImageProvider(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = GlanceModifier.size(28.dp))
                Spacer(GlanceModifier.width(6.dp))
                Text(if (time == null) "No alarm set" else "Next alarm", style = TextStyle(color = muted, fontSize = 12.sp))
            }
            if (time != null) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(time, style = TextStyle(color = paper, fontSize = 34.sp, fontWeight = FontWeight.Bold))
                    Text(" $meridiem", style = TextStyle(color = paper, fontSize = 14.sp))
                }
                Text(
                    listOfNotNull(day, until?.let { "in $it" }, label.takeIf { it.isNotBlank() }).joinToString(" · "),
                    style = TextStyle(color = muted, fontSize = 12.sp), maxLines = 1,
                )
            } else {
                Text("Tap to set one", style = TextStyle(color = paper, fontSize = 16.sp))
            }
        }
    }

    companion object {
        suspend fun refresh(context: Context) {
            if (GlanceAppWidgetManager(context).getGlanceIds(NextAlarmWidget::class.java).isNotEmpty()) {
                NextAlarmWidget().updateAll(context)
            }
        }
    }
}

class NextAlarmWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextAlarmWidget()
}
