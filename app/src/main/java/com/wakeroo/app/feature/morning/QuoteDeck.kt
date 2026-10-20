package com.wakeroo.app.feature.morning

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.wakeroo.app.data.Quote
import com.wakeroo.app.data.QuoteBook
import com.wakeroo.app.data.QuoteTheme
import com.wakeroo.app.ui.brand.Wordmark
import com.wakeroo.app.ui.components.Chip
import com.wakeroo.app.ui.theme.Outfit
import com.wakeroo.app.ui.theme.Space
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import kotlin.math.abs

private fun themeColors(theme: String): List<Color> = when (theme) {
    "positivity" -> listOf(Color(0xFFFF9466), Color(0xFFFFC857))
    "productivity" -> listOf(Color(0xFF6B5BD6), Color(0xFFB9A7FF))
    "calm" -> listOf(Color(0xFF2E9C77), Color(0xFF5BD6A8))
    "hard_times" -> listOf(Color(0xFF6B4A78), Color(0xFFFF7A59))
    "health" -> listOf(Color(0xFF2F7FD8), Color(0xFF7FC8FF))
    else -> listOf(Color(0xFF3A2B4E), Color(0xFFB9A7FF))
}

/**
 * Quote of the day on a stack of tilted cards. Swipe it away for another; save the ones you
 * like; share one as an image (good-looking enough to post).
 */
@Composable
fun QuoteDeck(
    quotes: List<Quote>,
    themes: Set<String>,
    saved: Set<String>,
    onThemes: (Set<String>) -> Unit,
    onSave: (Quote, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var skip by remember { mutableIntStateOf(0) }
    val today = remember { LocalDate.now() }
    val drag = remember { Animatable(0f) }
    val capture = rememberGraphicsLayer()
    fun quoteAt(offset: Int) = QuoteBook.forDay(quotes, today, themes, skip + offset)
    val top = quoteAt(0) ?: return

    fun next(direction: Float) {
        scope.launch {
            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            drag.animateTo(direction * 1400f, tween(220))
            skip++
            drag.snapTo(0f)
        }
    }

    Column(modifier) {
        Box(Modifier.fillMaxWidth().height(250.dp)) {
            // Two cards waiting behind, fanned out like a real deck.
            for (depth in 2 downTo 1) {
                val q = quoteAt(depth) ?: continue
                val pull = (abs(drag.value) / 600f).coerceIn(0f, 1f)
                val d = depth - pull
                QuoteCard(
                    q, saved = false,
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        translationY = d * 14.dp.toPx()
                        scaleX = 1f - d * 0.05f; scaleY = 1f - d * 0.05f
                        rotationZ = if (depth == 1) -2.5f * d else 3f * d
                        alpha = 1f - d * 0.25f
                    },
                )
            }
            QuoteCard(
                top, saved = top.id in saved,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = drag.value
                        rotationZ = drag.value / 40f
                        rotationY = drag.value / 30f
                        cameraDistance = 14f * density
                    }
                    .drawWithContent {
                        capture.record { this@drawWithContent.drawContent() }
                        drawLayer(capture)
                    }
                    .pointerDrag(onDrag = { dx -> scope.launch { drag.snapTo(drag.value + dx) } }, onEnd = {
                        if (abs(drag.value) > 220f) next(if (drag.value > 0) 1f else -1f)
                        else scope.launch { drag.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 300f)) }
                    }),
            )
        }
        Spacer(Modifier.height(Space.md))
        Row(verticalAlignment = Alignment.CenterVertically) {
            DeckButton(if (top.id in saved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder, if (top.id in saved) "Saved" else "Save") {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                onSave(top, top.id !in saved)
            }
            Spacer(Modifier.size(Space.sm))
            DeckButton(Icons.Rounded.IosShare, "Share") {
                scope.launch {
                    val bitmap = capture.toImageBitmap().asAndroidBitmap()
                    shareQuote(context, bitmap, top)
                }
            }
            Spacer(Modifier.weight(1f))
            DeckButton(Icons.AutoMirrored.Rounded.ArrowForward, "Another") { next(-1f) }
        }
        Spacer(Modifier.height(Space.md))
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            QuoteTheme.entries.forEach { t ->
                Chip(t.title, t.id in themes, {
                    skip = 0
                    onThemes(if (t.id in themes) themes - t.id else themes + t.id)
                })
            }
        }
    }
}

private fun Modifier.pointerDrag(onDrag: (Float) -> Unit, onEnd: () -> Unit) = this.then(
    Modifier.pointerInput(Unit) {
        detectHorizontalDragGestures(onDragEnd = onEnd, onDragCancel = onEnd) { change, dx ->
            change.consume()
            onDrag(dx)
        }
    },
)

@Composable
private fun DeckButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.08f)).clickable(onClick = onClick).padding(horizontal = Space.lg, vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White)
    }
}

@Composable
fun QuoteCard(quote: Quote, saved: Boolean, modifier: Modifier = Modifier) {
    val colors = themeColors(quote.theme)
    Box(
        modifier
            .clip(RoundedCornerShape(30.dp))
            .background(Brush.linearGradient(colors))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(30.dp))
            .padding(Space.xl),
    ) {
        Icon(Icons.Rounded.FormatQuote, null, tint = Color.White.copy(alpha = 0.35f), modifier = Modifier.size(44.dp).align(Alignment.TopStart))
        Column(Modifier.align(Alignment.CenterStart).padding(top = 28.dp)) {
            Text(
                quote.text,
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = if (quote.text.length > 90) 19.sp else 23.sp, lineHeight = if (quote.text.length > 90) 25.sp else 30.sp),
                color = Color(0xFF0E1120),
            )
            if (quote.author != null) {
                Text("— ${quote.author}", style = MaterialTheme.typography.labelLarge, color = Color(0xFF0E1120).copy(alpha = 0.7f), modifier = Modifier.padding(top = Space.md))
            }
        }
        Wordmark(Modifier.align(Alignment.BottomEnd), fontSize = 16.sp, color = Color(0xFF0E1120).copy(alpha = 0.6f), accent = Color.White)
        if (saved) Icon(Icons.Rounded.Bookmark, "Saved", tint = Color(0xFF0E1120).copy(alpha = 0.6f), modifier = Modifier.align(Alignment.TopEnd))
    }
}

private suspend fun shareQuote(context: Context, bitmap: Bitmap, quote: Quote) {
    val uri = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, "wakeroo-quote.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }
    val text = quote.text + (quote.author?.let { " — $it" } ?: "") + "\n\nWoke up with Wakeroo"
    val send = Intent(Intent.ACTION_SEND).setType("image/png")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .putExtra(Intent.EXTRA_TEXT, text)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(send, "Share quote").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
