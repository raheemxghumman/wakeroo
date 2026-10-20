package com.wakeroo.app.feature.you

import com.wakeroo.app.ui.i18n.tr
import com.wakeroo.app.ui.util.initial
import com.wakeroo.app.ui.util.formatSeconds
import com.wakeroo.app.ui.theme.MorningGoldFixed
import com.wakeroo.app.ui.theme.DawnLavenderFixed
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.wakeroo.app.data.Mood
import com.wakeroo.app.data.db.MoodEntity
import com.wakeroo.app.data.db.RingEventEntity
import com.wakeroo.app.data.db.RingOutcome
import com.wakeroo.app.feature.editor.PillButton
import com.wakeroo.app.feature.morning.MoodFace
import com.wakeroo.app.ui.brand.WakerooMark
import com.wakeroo.app.ui.brand.Wordmark
import com.wakeroo.app.ui.theme.Alert
import com.wakeroo.app.ui.theme.Calm
import com.wakeroo.app.ui.theme.ClockType
import com.wakeroo.app.ui.theme.Base
import com.wakeroo.app.ui.theme.DawnLavender
import com.wakeroo.app.ui.theme.MorningGold
import com.wakeroo.app.ui.theme.Night
import com.wakeroo.app.ui.theme.NightSurface
import com.wakeroo.app.ui.theme.Paper
import com.wakeroo.app.ui.theme.PaperMuted
import com.wakeroo.app.ui.theme.Space
import com.wakeroo.app.ui.theme.SunriseCoral
import com.wakeroo.app.ui.util.formatClock
import com.wakeroo.app.ui.util.formatMeridiem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin

enum class DayMark { Won, Missed, NoAlarm, None }

/** What each day of the calendar shows, from the ring log. Test alarms and passed checks don't count. */
object CalendarMarks {
    fun forDays(events: List<RingEventEntity>, zone: ZoneId = ZoneId.systemDefault(), today: LocalDate = LocalDate.now()): (LocalDate) -> DayMark {
        val byDay = events.filter { !it.isTest && it.wakeCheckPassed != true }
            .groupBy { Instant.ofEpochMilli(it.scheduledAt).atZone(zone).toLocalDate() }
        val first = byDay.keys.minOrNull()
        return { day ->
            val list = byDay[day]
            when {
                list != null && list.any { it.outcome == RingOutcome.Escaped || it.outcome == RingOutcome.Missed } -> DayMark.Missed
                list != null && list.any { it.outcome == RingOutcome.Dismissed } -> DayMark.Won
                list == null && first != null && day >= first && day < today -> DayMark.NoAlarm
                else -> DayMark.None
            }
        }
    }
}

/**
 * A month of mornings: a sun for each day you won, a cloud for missed or escaped ones, a moon
 * on days with no alarm. Months turn like pages; free users see the current month.
 */
@Composable
fun WakeCalendar(events: List<RingEventEntity>, pro: Boolean, onDay: (LocalDate) -> Unit, onPro: () -> Unit) {
    val zone = ZoneId.systemDefault()
    val today = remember { LocalDate.now() }
    val mark = remember(events) { CalendarMarks.forDays(events, zone, today) }
    val firstMonth = remember(events) {
        events.minOfOrNull { it.scheduledAt }?.let { YearMonth.from(Instant.ofEpochMilli(it).atZone(zone)) } ?: YearMonth.from(today)
    }
    val months = if (pro) (ChronoUnit.MONTHS.between(firstMonth, YearMonth.from(today)).toInt() + 1).coerceAtLeast(1) else 1
    val pager = rememberPagerState(initialPage = months - 1) { months }
    val scope = rememberCoroutineScope()
    Column {
        val shown = YearMonth.from(today).minusMonths((months - 1 - pager.currentPage).toLong())
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(DateTimeFormatter.ofPattern("MMMM yyyy").format(shown), style = MaterialTheme.typography.titleMedium, color = Paper, modifier = Modifier.weight(1f))
            val canBack = pager.currentPage > 0
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack, tr("Previous month"), tint = if (canBack || !pro) Paper else PaperMuted.copy(alpha = 0.3f),
                modifier = Modifier.clip(CircleShape).clickable {
                    if (!pro) onPro() else if (canBack) scope.launch { pager.animateScrollToPage(pager.currentPage - 1) }
                }.padding(Space.sm),
            )
            val canForward = pager.currentPage < months - 1
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForward, tr("Next month"), tint = if (canForward) Paper else PaperMuted.copy(alpha = 0.3f),
                modifier = Modifier.clip(CircleShape).clickable(enabled = canForward) { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }.padding(Space.sm),
            )
        }
        Spacer(Modifier.height(Space.sm))
        Row(Modifier.fillMaxWidth()) {
            DayOfWeek.entries.forEach { d ->
                Text(d.initial(), style = MaterialTheme.typography.labelSmall, color = PaperMuted, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        HorizontalPager(pager, modifier = Modifier.fillMaxWidth()) { page ->
            val month = YearMonth.from(today).minusMonths((months - 1 - page).toLong())
            Box(
                Modifier.graphicsLayer {
                    // Pages turn about their left edge like a desk calendar.
                    val offset = (pager.currentPage - page) + pager.currentPageOffsetFraction
                    rotationY = offset.coerceIn(-1f, 1f) * -60f
                    transformOrigin = TransformOrigin(if (offset > 0) 1f else 0f, 0.5f)
                    cameraDistance = 14f * density
                    alpha = 1f - offset.absoluteValue.coerceIn(0f, 1f) * 0.5f
                },
            ) { MonthGrid(month, today, mark, onDay) }
        }
        if (!pro) {
            Text(
                tr("See every month back to your first alarm with Pro"),
                style = MaterialTheme.typography.labelMedium, color = MorningGold,
                modifier = Modifier.padding(top = Space.sm).clip(CircleShape).clickable(onClick = onPro).padding(Space.sm),
            )
        }
    }
}

@Composable
private fun MonthGrid(month: YearMonth, today: LocalDate, mark: (LocalDate) -> DayMark, onDay: (LocalDate) -> Unit) {
    val first = month.atDay(1)
    val lead = first.dayOfWeek.value - 1
    val cells = lead + month.lengthOfMonth()
    Column(Modifier.padding(top = Space.sm), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (week in 0 until (cells + 6) / 7) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (dow in 0 until 7) {
                    val index = week * 7 + dow - lead
                    if (index < 0 || index >= month.lengthOfMonth()) {
                        Spacer(Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val day = month.atDay(index + 1)
                        DayCell(day, mark(day), day == today, Modifier.weight(1f)) { onDay(day) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: LocalDate, mark: DayMark, isToday: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isToday) SunriseCoral.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.03f))
            .then(if (isToday) Modifier.border(1.dp, SunriseCoral.copy(alpha = 0.6f), RoundedCornerShape(12.dp)) else Modifier)
            .clickable(enabled = mark != DayMark.None, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize().padding(7.dp)) {
            val r = size.minDimension / 2
            when (mark) {
                DayMark.Won -> {
                    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFE08A), MorningGoldFixed), center, r * 0.55f), r * 0.5f)
                    for (k in 0 until 8) {
                        val a = k * 0.785f
                        drawLine(MorningGoldFixed, center + Offset(cos(a), sin(a)) * (r * 0.68f), center + Offset(cos(a), sin(a)) * (r * 0.95f), r * 0.12f)
                    }
                }
                DayMark.Missed -> {
                    drawCircle(Color(0xFF8C8FB0), r * 0.32f, center + Offset(-r * 0.25f, r * 0.05f))
                    drawCircle(Color(0xFF8C8FB0), r * 0.42f, center + Offset(r * 0.15f, -r * 0.08f))
                    drawRoundRect(Color(0xFF8C8FB0), center + Offset(-r * 0.6f, 0f), androidx.compose.ui.geometry.Size(r * 1.2f, r * 0.4f), androidx.compose.ui.geometry.CornerRadius(r * 0.2f))
                }
                DayMark.NoAlarm -> {
                    drawCircle(DawnLavenderFixed.copy(alpha = 0.35f), r * 0.32f)
                    drawCircle(Color(0xFF1A1E33), r * 0.3f, center + Offset(r * 0.16f, -r * 0.1f))
                }
                DayMark.None -> Unit
            }
        }
        Text(
            "${day.dayOfMonth}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = if (mark == DayMark.Won) Night else Paper.copy(alpha = if (mark == DayMark.None) 0.5f else 0.9f),
            modifier = Modifier.align(if (mark == DayMark.None) Alignment.Center else Alignment.BottomEnd).padding(3.dp),
        )
    }
}

/** That morning in detail: when it rang, how it ended, how long it took, and how you felt. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaySheet(day: LocalDate, events: List<RingEventEntity>, mood: MoodEntity?, onDismiss: () -> Unit) {
    val zone = ZoneId.systemDefault()
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dayEvents = events.filter { !it.isTest && Instant.ofEpochMilli(it.scheduledAt).atZone(zone).toLocalDate() == day }.sortedBy { it.scheduledAt }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = Base, dragHandle = null) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(Space.xl), verticalArrangement = Arrangement.spacedBy(Space.md)) {
            Text(DateTimeFormatter.ofPattern("EEEE d MMMM").format(day), style = MaterialTheme.typography.headlineSmall, color = Paper)
            if (dayEvents.isEmpty()) Text(tr("No alarm rang this day."), style = MaterialTheme.typography.bodyMedium, color = PaperMuted)
            dayEvents.forEach { e ->
                val rang = Instant.ofEpochMilli(e.scheduledAt).atZone(zone).toLocalTime()
                val (color, label) = when (e.outcome) {
                    RingOutcome.Dismissed -> if (e.wakeCheckPassed == true) Calm to tr("Wake-up check passed") else Calm to (e.secondsToWake?.let { s -> tr("Mission done in {0}", formatSeconds(s)) } ?: tr("Mission done"))
                    RingOutcome.Escaped -> MorningGold to tr("Emergency exit")
                    RingOutcome.Missed -> Alert to if (e.wakeCheckPassed == false) tr("Fell back asleep, the alarm came back") else tr("Didn't ring (phone off or app paused)")
                    RingOutcome.Snoozed -> DawnLavender to tr("Snoozed")
                }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(NightSurface).padding(Space.lg),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                    Spacer(Modifier.width(Space.md))
                    Column {
                        Text("${rang.formatClock()} ${rang.formatMeridiem()}${e.label.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""}", style = MaterialTheme.typography.titleSmall, color = Paper)
                        Text(
                            listOfNotNull(label, if (e.snoozes == 1) tr("1 snooze") else if (e.snoozes > 1) tr("{0} snoozes", e.snoozes) else null).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall, color = PaperMuted,
                        )
                    }
                }
            }
            mood?.let { m ->
                val mood = Mood.entries.getOrNull(m.mood) ?: return@let
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MoodFace(mood, Modifier.size(32.dp))
                    Spacer(Modifier.width(Space.md))
                    Text(tr("You felt {0}", mood.title.lowercase()), style = MaterialTheme.typography.bodyMedium, color = Paper)
                }
            }
        }
    }
}

data class WeekRecap(val won: Int, val days: Int, val averageWake: String?, val fastest: Int?, val medal: AchievementDef?)

object Recap {
    fun thisWeek(events: List<RingEventEntity>, unlocked: Map<String, Long>, today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): WeekRecap =
        forWeek(events, unlocked, today, today, zone)

    /** The week (Monday to Sunday) containing [anyDay], counted up to [today]. */
    fun forWeek(events: List<RingEventEntity>, unlocked: Map<String, Long>, anyDay: LocalDate, today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): WeekRecap {
        val start = anyDay.with(DayOfWeek.MONDAY)
        val end = start.plusDays(6)
        val mark = CalendarMarks.forDays(events, zone, today.plusDays(1))
        val days = (0..6).map { start.plusDays(it.toLong()) }.filter { it <= today }
        val wins = events.filter {
            val d = Instant.ofEpochMilli(it.scheduledAt).atZone(zone).toLocalDate()
            !it.isTest && it.outcome == RingOutcome.Dismissed && it.wakeCheckPassed != true && d >= start && d <= end
        }
        val wakes = wins.mapNotNull { it.endedAt }.map { Instant.ofEpochMilli(it).atZone(zone).toLocalTime().let { t -> t.hour * 60 + t.minute } }
        val avg = wakes.takeIf { it.isNotEmpty() }?.average()?.toInt()?.let { java.time.LocalTime.of(it / 60, it % 60) }
        val startMs = start.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMs = end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val medal = unlocked.filterValues { it in startMs until endMs }.maxByOrNull { it.value }?.key?.let { id -> AchievementEngine.all.firstOrNull { it.id == id } }
        return WeekRecap(
            won = days.count { mark(it) == DayMark.Won }, days = days.size,
            averageWake = avg?.let { "${it.formatClock()} ${it.formatMeridiem()}" },
            fastest = wins.mapNotNull { it.secondsToWake }.minOrNull(), medal = medal,
        )
    }
}

/** A post-ready card (4:5) with your streak, the week's suns and your best medal. */
@Composable
fun ShareStreakDialog(streak: Int, week: List<Boolean?>, medal: AchievementDef?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val capture = rememberGraphicsLayer()
    Dialog(onDismissRequest = onDismiss) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.8f)
                    .drawWithContent {
                        capture.record { this@drawWithContent.drawContent() }
                        drawLayer(capture)
                    }
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF2B2550), Color(0xFF6B4A78), SunriseCoral, MorningGold)))
                    .padding(Space.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Wordmark(fontSize = 26.sp)
                Spacer(Modifier.weight(1f))
                if (medal != null) Medal(medal, unlocked = true, size = 96.dp) else WakerooMark(Modifier.size(96.dp))
                Spacer(Modifier.height(Space.md))
                Text("$streak", style = ClockType.hero.copy(fontSize = 84.sp), color = Night)
                Text(if (streak == 1) tr("morning in a row") else tr("mornings in a row"), style = MaterialTheme.typography.titleMedium, color = Night.copy(alpha = 0.8f))
                Spacer(Modifier.height(Space.lg))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    week.forEach { s ->
                        Box(Modifier.size(18.dp).clip(CircleShape).background(when (s) { true -> Color.White; false -> Night.copy(alpha = 0.5f); null -> Night.copy(alpha = 0.15f) }))
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(tr("Up with a mission, every morning."), style = MaterialTheme.typography.labelMedium, color = Night.copy(alpha = 0.7f))
            }
            Spacer(Modifier.height(Space.lg))
            PillButton("Share", Modifier.fillMaxWidth(), icon = Icons.Rounded.IosShare) {
                scope.launch {
                    val bmp = capture.toImageBitmap().asAndroidBitmap()
                    shareImage(context, bmp, "wakeroo-streak.png", tr("{0} mornings in a row with Wakeroo", streak))
                    onDismiss()
                }
            }
        }
    }
}

suspend fun shareImage(context: Context, bitmap: Bitmap, name: String, text: String) {
    val uri = withContext(Dispatchers.IO) {
        // Scale up to a crisp 1080 px wide image for social posts.
        val scaled = if (bitmap.width < 1080) Bitmap.createScaledBitmap(bitmap, 1080, (bitmap.height * 1080f / bitmap.width).toInt(), true) else bitmap
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, name)
        file.outputStream().use { scaled.compress(Bitmap.CompressFormat.PNG, 100, it) }
        FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }
    val send = Intent(Intent.ACTION_SEND).setType("image/png")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .putExtra(Intent.EXTRA_TEXT, text)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(send, tr("Share")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
