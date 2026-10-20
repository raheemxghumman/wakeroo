package com.wakeroo.app.missions

import com.wakeroo.app.ui.i18n.tr
import com.wakeroo.app.ui.theme.MorningGoldFixed
import com.wakeroo.app.ui.theme.DawnLavenderFixed
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.wakeroo.app.data.model.MissionType
import com.wakeroo.app.ui.theme.Calm
import com.wakeroo.app.ui.theme.DawnLavender
import com.wakeroo.app.ui.theme.LocalReducedMotion
import com.wakeroo.app.ui.theme.MorningGold
import com.wakeroo.app.ui.theme.Outfit
import com.wakeroo.app.ui.theme.SunriseCoral
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val Ink = Color(0xFFF5F3EE)
private val Dim = Color(0xFFF5F3EE).copy(alpha = 0.16f)
private val Panel = Color(0xFF252A44)

/**
 * A short looping animation of how a mission works, drawn from shapes (no video files), for
 * the mission picker and onboarding. [playing] = false freezes it on a telling frame.
 */
@Composable
fun MissionDemo(type: MissionType, modifier: Modifier = Modifier, playing: Boolean = true) {
    val reduced = LocalReducedMotion.current
    val loop by rememberInfiniteTransition(label = "demo").animateFloat(
        0f, 1f, infiniteRepeatable(tween(3400, easing = LinearEasing), RepeatMode.Restart), label = "t",
    )
    val t = if (playing && !reduced) loop else 0.82f
    val text = rememberTextMeasurer()
    Canvas(modifier) {
        when (type) {
            MissionType.Math -> mathDemo(t, text)
            MissionType.Typing -> typingDemo(t, text)
            MissionType.Shake -> shakeDemo(t)
            MissionType.Scan -> scanDemo(t)
            MissionType.Photo -> photoDemo(t)
            MissionType.Steps -> stepsDemo(t, text)
            MissionType.Memory -> memoryDemo(t)
            MissionType.Nfc -> nfcDemo(t)
            MissionType.Plan -> planDemo(t, text)
            MissionType.Hunt -> huntDemo(t)
        }
    }
}

/** 0..1 progress of [t] through the window [from]..[to]. */
private fun phase(t: Float, from: Float, to: Float) = ((t - from) / (to - from)).coerceIn(0f, 1f)

private fun DrawScope.u() = size.minDimension / 100f

private fun DrawScope.check(at: Offset, r: Float, p: Float, color: Color = Calm) {
    if (p <= 0f) return
    drawCircle(color, r * (0.6f + 0.4f * p), at)
    val path = Path().apply {
        moveTo(at.x - r * 0.42f, at.y + r * 0.02f)
        lineTo(at.x - r * 0.1f, at.y + r * 0.34f)
        lineTo(at.x + r * 0.46f, at.y - r * 0.3f)
    }
    drawPath(path, Color(0xFF0E1120).copy(alpha = p), style = Stroke(r * 0.22f, cap = StrokeCap.Round))
}

private fun DrawScope.finger(at: Offset, pressed: Float) {
    val r = 6 * u()
    drawCircle(Color.White.copy(alpha = 0.18f + 0.25f * pressed), r * (1.4f + pressed * 0.6f), at)
    drawCircle(Color.White.copy(alpha = 0.85f), r * (1f - pressed * 0.2f), at)
}

private fun DrawScope.label(m: TextMeasurer, s: String, center: Offset, sizeSp: Float, color: Color = Ink) {
    val layout = m.measure(s, TextStyle(fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = (sizeSp * 0.7f * u()).toSp(), color = color))
    drawText(layout, topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f))
}

private fun DrawScope.mathDemo(t: Float, m: TextMeasurer) {
    val c = Offset(size.width / 2, size.height / 2)
    val unit = u()
    label(m, "7 × 6 + 9", Offset(c.x, c.y - 30 * unit), 16f)
    val answer = when { t < 0.3f -> ""; t < 0.48f -> "5"; else -> "51" }
    val solved = phase(t, 0.62f, 0.72f)
    val box = Rect(Offset(c.x - 24 * unit, c.y - 16 * unit), Size(48 * unit, 18 * unit))
    drawRoundRect(if (solved > 0) MorningGoldFixed.copy(alpha = 0.25f + 0.5f * solved) else Panel, box.topLeft, box.size, CornerRadius(6 * unit))
    label(m, answer.ifEmpty { "?" }, box.center, 12f, if (answer.isEmpty()) Dim else MorningGoldFixed)
    // Mini keypad.
    val keys = Offset(c.x - 21 * unit, c.y + 10 * unit)
    for (r in 0 until 2) for (k in 0 until 3) {
        drawRoundRect(Panel, keys + Offset(k * 15 * unit, r * 11 * unit), Size(12 * unit, 9 * unit), CornerRadius(3 * unit))
    }
    val taps = listOf(0.25f to Offset(1f, 1f), 0.43f to Offset(0f, 0f), 0.6f to Offset(2f, 1f))
    val current = taps.lastOrNull { t >= it.first - 0.1f } ?: taps.first()
    val p = 1f - abs(t - current.first) / 0.06f
    finger(keys + Offset(current.second.x * 15 * unit + 6 * unit, current.second.y * 11 * unit + 4.5f * unit), p.coerceIn(0f, 1f))
    check(Offset(box.right + 2 * unit, box.top), 7 * unit, solved)
}

private fun DrawScope.typingDemo(t: Float, m: TextMeasurer) {
    val c = Offset(size.width / 2, size.height / 2)
    val unit = u()
    val phrase = tr("rise and shine")
    val typed = (phrase.length * phase(t, 0.08f, 0.72f)).toInt()
    drawRoundRect(Panel, Offset(c.x - 46 * unit, c.y - 32 * unit), Size(92 * unit, 26 * unit), CornerRadius(8 * unit))
    val layout = m.measure(
        androidx.compose.ui.text.buildAnnotatedString {
            phrase.forEachIndexed { i, ch ->
                pushStyle(androidx.compose.ui.text.SpanStyle(color = if (i < typed) MorningGoldFixed else Dim)); append(ch); pop()
            }
        },
        TextStyle(fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = (12 * 0.7f * unit).toSp()),
    )
    drawText(layout, topLeft = Offset(c.x - layout.size.width / 2f, c.y - 19 * unit - layout.size.height / 2f))
    // Keyboard rows with the pressed key glowing.
    val rows = listOf(10, 9, 7)
    rows.forEachIndexed { r, n ->
        val w = 8 * unit
        val start = c.x - (n * (w + 1.2f * unit)) / 2
        for (k in 0 until n) {
            val lit = typed < phrase.length && (typed * 7 + 3) % (rows.sum()) == r * 10 + k
            drawRoundRect(if (lit) SunriseCoral else Panel, Offset(start + k * (w + 1.2f * unit), c.y + 4 * unit + r * 10 * unit), Size(w, 8 * unit), CornerRadius(2 * unit))
        }
    }
    check(Offset(c.x + 44 * unit, c.y - 32 * unit), 7 * unit, phase(t, 0.76f, 0.84f))
}

private fun DrawScope.phone(center: Offset, w: Float, h: Float, angle: Float, screen: DrawScope.(Rect) -> Unit = {}) {
    rotate(angle, center) {
        val tl = Offset(center.x - w / 2, center.y - h / 2)
        drawRoundRect(Color(0xFF3A3F5C), tl, Size(w, h), CornerRadius(w * 0.18f))
        val inset = w * 0.08f
        val screenRect = Rect(tl + Offset(inset, inset), Size(w - 2 * inset, h - 2 * inset))
        drawRoundRect(Color(0xFF0E1120), screenRect.topLeft, screenRect.size, CornerRadius(w * 0.12f))
        screen(screenRect)
    }
}

private fun DrawScope.shakeDemo(t: Float) {
    val c = Offset(size.width / 2, size.height / 2)
    val unit = u()
    val shaking = t < 0.75f
    val angle = if (shaking) sin(t * 2 * PI.toFloat() * 7) * 16f else 0f
    val fill = phase(t, 0.05f, 0.75f)
    phone(c, 34 * unit, 60 * unit, angle) { s ->
        val r = s.width * 0.36f
        val oc = s.center
        drawCircle(Panel, r, oc)
        val level = oc.y + r - fill * 2 * r
        drawRect(Brush.verticalGradient(listOf(MorningGoldFixed, SunriseCoral)), Offset(oc.x - r, level), Size(2 * r, oc.y + r - level))
        drawCircle(Color(0xFF0E1120), r * 1.25f, oc, style = Stroke(r * 0.5f))
    }
    if (shaking) {
        val a = 0.5f + 0.5f * abs(sin(t * 2 * PI.toFloat() * 7))
        for (side in listOf(-1, 1)) {
            for (k in 0..1) {
                val x = c.x + side * (24 + k * 6) * unit
                drawArc(DawnLavenderFixed.copy(alpha = a * (0.8f - k * 0.3f)), if (side < 0) 150f else -30f, 60f, false,
                    Offset(x - 6 * unit, c.y - 12 * unit), Size(12 * unit, 24 * unit), style = Stroke(2.2f * unit, cap = StrokeCap.Round))
            }
        }
    }
    check(Offset(c.x + 20 * unit, c.y - 30 * unit), 7 * unit, phase(t, 0.78f, 0.86f))
}

private fun DrawScope.brackets(rect: Rect, color: Color, unit: Float) {
    val len = rect.width * 0.22f
    val stroke = 2.6f * unit
    listOf(
        rect.topLeft to (Offset(len, 0f) to Offset(0f, len)),
        rect.topRight to (Offset(-len, 0f) to Offset(0f, len)),
        rect.bottomLeft to (Offset(len, 0f) to Offset(0f, -len)),
        rect.bottomRight to (Offset(-len, 0f) to Offset(0f, -len)),
    ).forEach { (corner, arms) ->
        drawLine(color, corner, corner + arms.first, stroke, StrokeCap.Round)
        drawLine(color, corner, corner + arms.second, stroke, StrokeCap.Round)
    }
}

private fun DrawScope.scanDemo(t: Float) {
    val c = Offset(size.width / 2, size.height / 2)
    val unit = u()
    val q = 30 * unit
    val tl = Offset(c.x - q / 2, c.y - q / 2)
    drawRoundRect(Color.White, tl - Offset(3 * unit, 3 * unit), Size(q + 6 * unit, q + 6 * unit), CornerRadius(3 * unit))
    val cells = 7
    val cell = q / cells
    for (y in 0 until cells) for (x in 0 until cells) {
        val finder = (x < 2 && y < 2) || (x > 4 && y < 2) || (x < 2 && y > 4)
        if (finder || (x * 3 + y * 5 + x * y) % 3 == 0) drawRect(Color(0xFF0E1120), tl + Offset(x * cell, y * cell), Size(cell, cell))
    }
    val found = phase(t, 0.6f, 0.66f)
    val breathe = 1f + 0.08f * sin(t * 2 * PI.toFloat() * 3) * (1 - found)
    val s = 50 * unit * breathe
    brackets(Rect(Offset(c.x - s / 2, c.y - s / 2), Size(s, s)), if (found > 0) Calm else Ink, unit)
    if (found == 0f) {
        val y = c.y - q / 2 + q * phase(t % 0.3f, 0f, 0.3f)
        drawLine(SunriseCoral, Offset(c.x - q * 0.7f, y), Offset(c.x + q * 0.7f, y), 1.6f * unit)
    }
    check(Offset(c.x + 24 * unit, c.y - 24 * unit), 7 * unit, phase(t, 0.66f, 0.74f))
}

private fun DrawScope.photoDemo(t: Float) {
    val c = Offset(size.width / 2, size.height / 2)
    val unit = u()
    // A basin and tap.
    val basin = Path().apply {
        moveTo(c.x - 22 * unit, c.y + 2 * unit)
        lineTo(c.x + 22 * unit, c.y + 2 * unit)
        quadraticTo(c.x + 20 * unit, c.y + 22 * unit, c.x, c.y + 22 * unit)
        quadraticTo(c.x - 20 * unit, c.y + 22 * unit, c.x - 22 * unit, c.y + 2 * unit)
        close()
    }
    drawPath(basin, Ink.copy(alpha = 0.85f))
    drawLine(Ink.copy(alpha = 0.85f), Offset(c.x, c.y + 2 * unit), Offset(c.x, c.y - 14 * unit), 4 * unit, StrokeCap.Round)
    drawLine(Ink.copy(alpha = 0.85f), Offset(c.x, c.y - 14 * unit), Offset(c.x + 9 * unit, c.y - 14 * unit), 4 * unit, StrokeCap.Round)
    // The viewfinder glides in until it lines up with the saved view.
    val align = phase(t, 0.05f, 0.6f)
    val ease = 1 - (1 - align) * (1 - align)
    val off = Offset((1 - ease) * 26 * unit, (1 - ease) * -18 * unit)
    val s = 60 * unit
    val matched = align >= 1f
    brackets(Rect(c + off - Offset(s / 2, s / 2), Size(s, s * 0.85f)), if (matched) Calm else Ink, unit)
    val bar = Rect(Offset(c.x - 24 * unit, c.y + 32 * unit), Size(48 * unit, 4 * unit))
    drawRoundRect(Dim, bar.topLeft, bar.size, CornerRadius(2 * unit))
    drawRoundRect(if (matched) Calm else MorningGoldFixed, bar.topLeft, Size(bar.width * ease, bar.height), CornerRadius(2 * unit))
    check(Offset(c.x + 28 * unit, c.y - 24 * unit), 7 * unit, phase(t, 0.64f, 0.72f))
}

private fun DrawScope.stepsDemo(t: Float, m: TextMeasurer) {
    val unit = u()
    val prints = 8
    val shown = (prints * phase(t, 0.02f, 0.8f)).toInt()
    for (i in 0 until shown) {
        val x = size.width * (0.12f + 0.76f * i / (prints - 1))
        val y = size.height * 0.6f + (if (i % 2 == 0) -6 else 6) * unit
        rotate(90f, Offset(x, y)) {
            drawOval(MorningGoldFixed, Offset(x - 3 * unit, y - 5 * unit), Size(6 * unit, 10 * unit))
            drawCircle(MorningGoldFixed, 1.6f * unit, Offset(x, y - 7.5f * unit))
        }
    }
    label(m, "${(40 * phase(t, 0.02f, 0.8f)).toInt()}", Offset(size.width / 2, size.height * 0.3f), 20f)
    check(Offset(size.width * 0.82f, size.height * 0.28f), 7 * unit, phase(t, 0.82f, 0.9f))
}

private fun DrawScope.memoryDemo(t: Float) {
    val unit = u()
    val g = 3
    val tile = 17 * unit
    val gap = 4 * unit
    val total = g * tile + (g - 1) * gap
    val origin = Offset((size.width - total) / 2, (size.height - total) / 2)
    val pattern = listOf(1, 3, 8)
    val tapOrder = listOf(3, 1, 8)
    for (i in 0 until g * g) {
        val showP = if (i in pattern) {
            val k = pattern.indexOf(i)
            phase(t, 0.05f + k * 0.06f, 0.12f + k * 0.06f) - phase(t, 0.36f, 0.42f)
        } else 0f
        val tapP = if (i in tapOrder) phase(t, 0.5f + tapOrder.indexOf(i) * 0.1f, 0.56f + tapOrder.indexOf(i) * 0.1f) else 0f
        val p = maxOf(showP, tapP)
        val x = origin.x + (i % g) * (tile + gap)
        val y = origin.y + (i / g) * (tile + gap)
        // Fake the 3D flip by squashing the tile horizontally through the turn.
        val squash = abs(cos(p * PI.toFloat()))
        val face = p > 0.5f
        scale(squash.coerceAtLeast(0.05f), 1f, Offset(x + tile / 2, y + tile / 2)) {
            drawRoundRect(
                if (face) Brush.linearGradient(listOf(MorningGoldFixed, SunriseCoral), Offset(x, y), Offset(x + tile, y + tile)) else Brush.linearGradient(listOf(Panel, Panel)),
                Offset(x, y), Size(tile, tile), CornerRadius(4 * unit),
            )
        }
    }
    check(Offset(origin.x + total + 2 * unit, origin.y), 7 * unit, phase(t, 0.8f, 0.88f))
}

private fun DrawScope.nfcDemo(t: Float) {
    val c = Offset(size.width / 2, size.height / 2)
    val unit = u()
    val sticker = Offset(c.x + 18 * unit, c.y)
    val st = 18 * unit
    val touched = phase(t, 0.4f, 0.45f) > 0f
    drawRoundRect(
        if (touched) Brush.linearGradient(listOf(Calm, Calm)) else Brush.linearGradient(listOf(MorningGoldFixed, SunriseCoral)),
        sticker - Offset(st / 2, st / 2), Size(st, st), CornerRadius(4 * unit),
    )
    val approach = phase(t, 0.0f, 0.4f) - phase(t, 0.75f, 0.95f)
    val px = c.x - 30 * unit + approach * 34 * unit
    if (touched && t < 0.75f) {
        for (k in 0..2) {
            val p = ((t * 3 + k / 3f) % 1f)
            drawCircle(DawnLavenderFixed.copy(alpha = (1 - p) * 0.7f), st * (0.6f + p * 1.2f), sticker, style = Stroke(1.6f * unit))
        }
    }
    phone(Offset(px, c.y), 22 * unit, 40 * unit, -12f)
    check(Offset(c.x + 30 * unit, c.y - 22 * unit), 7 * unit, phase(t, 0.48f, 0.56f))
}

private fun DrawScope.planDemo(t: Float, m: TextMeasurer) {
    val c = Offset(size.width / 2, size.height / 2)
    val unit = u()
    val card = Rect(Offset(c.x - 40 * unit, c.y - 28 * unit), Size(80 * unit, 56 * unit))
    drawRoundRect(Panel, card.topLeft, card.size, CornerRadius(8 * unit))
    for (k in 0..2) drawLine(Dim, Offset(card.left + 8 * unit, card.top + (20 + k * 12) * unit), Offset(card.right - 8 * unit, card.top + (20 + k * 12) * unit), 1.2f * unit)
    val plan = tr("Go for a run")
    val n = (plan.length * phase(t, 0.08f, 0.62f)).toInt()
    val layout = m.measure(plan.take(n), TextStyle(fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = (12 * 0.7f * unit).toSp(), color = Ink))
    drawText(layout, topLeft = Offset(card.left + 9 * unit, card.top + 18 * unit - layout.size.height))
    if (n < plan.length && (t * 8).toInt() % 2 == 0) {
        drawLine(SunriseCoral, Offset(card.left + 10 * unit + layout.size.width, card.top + 9 * unit), Offset(card.left + 10 * unit + layout.size.width, card.top + 18 * unit), 1.6f * unit)
    }
    check(Offset(card.right, card.top), 7 * unit, phase(t, 0.68f, 0.76f))
}

private fun DrawScope.huntDemo(t: Float) {
    val c = Offset(size.width / 2, size.height / 2)
    val unit = u()
    // A mug with steam.
    val body = Rect(Offset(c.x - 12 * unit, c.y - 8 * unit), Size(22 * unit, 24 * unit))
    drawRoundRect(Ink.copy(alpha = 0.9f), body.topLeft, body.size, CornerRadius(5 * unit))
    drawArc(Ink.copy(alpha = 0.9f), -90f, 180f, false, Offset(body.right - 4 * unit, body.top + 5 * unit), Size(10 * unit, 12 * unit), style = Stroke(3 * unit))
    for (k in 0..1) {
        val x = c.x - 4 * unit + k * 7 * unit
        val rise = (t * 2 + k * 0.5f) % 1f
        translate(0f, -rise * 8 * unit) {
            drawArc(DawnLavenderFixed.copy(alpha = 1 - rise), 180f, 180f, false, Offset(x - 2 * unit, body.top - 10 * unit), Size(4 * unit, 6 * unit), style = Stroke(1.6f * unit, cap = StrokeCap.Round))
        }
    }
    val found = phase(t, 0.55f, 0.6f)
    val pulse = 1f + 0.1f * sin(t * 2 * PI.toFloat() * 4) * (1 - found)
    val s = 58 * unit * pulse
    brackets(Rect(Offset(c.x - s / 2, c.y - s / 2), Size(s, s)), if (found > 0) MorningGoldFixed else Ink, unit)
    if (found == 0f) drawCircle(SunriseCoral.copy(alpha = 0.6f), s * 0.42f, c, style = Stroke(1.4f * unit))
    check(Offset(c.x + 26 * unit, c.y - 26 * unit), 7 * unit, phase(t, 0.6f, 0.68f))
}
