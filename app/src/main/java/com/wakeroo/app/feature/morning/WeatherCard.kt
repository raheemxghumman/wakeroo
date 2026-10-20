package com.wakeroo.app.feature.morning

import com.wakeroo.app.ui.theme.MorningGoldFixed
import com.wakeroo.app.ui.theme.DawnLavenderFixed
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.wakeroo.app.data.City
import com.wakeroo.app.data.TempUnit
import com.wakeroo.app.feature.editor.PillButton
import com.wakeroo.app.ui.components.Chip
import com.wakeroo.app.ui.components.ChipRow
import com.wakeroo.app.ui.theme.Base
import com.wakeroo.app.ui.theme.AlwaysDark
import com.wakeroo.app.ui.theme.DawnLavender
import com.wakeroo.app.ui.theme.Hairline
import com.wakeroo.app.ui.theme.LocalReducedMotion
import com.wakeroo.app.ui.theme.MorningGold
import com.wakeroo.app.ui.theme.Night
import com.wakeroo.app.ui.theme.NightSurface
import com.wakeroo.app.ui.theme.Paper
import com.wakeroo.app.ui.theme.PaperMuted
import com.wakeroo.app.ui.theme.Space
import com.wakeroo.app.ui.theme.SunriseCoral
import com.wakeroo.app.weather.Condition
import com.wakeroo.app.weather.Forecast
import com.wakeroo.app.weather.hourLabel
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** A little animated picture of the weather: turning sun rays, drifting clouds, falling rain. */
@Composable
fun WeatherGlyph(condition: Condition, isDay: Boolean, modifier: Modifier = Modifier, animate: Boolean = true) {
    val reduced = LocalReducedMotion.current
    val loop by rememberInfiniteTransition(label = "glyph").animateFloat(
        0f, 1f, infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart), label = "t",
    )
    val t = if (animate && !reduced) loop else 0.3f
    Canvas(modifier) {
        when (condition) {
            Condition.Clear -> if (isDay) sun(t, Offset(size.width / 2, size.height / 2), size.minDimension * 0.24f) else moon(Offset(size.width / 2, size.height / 2), size.minDimension * 0.26f)
            Condition.PartlyCloudy -> {
                if (isDay) sun(t, Offset(size.width * 0.38f, size.height * 0.38f), size.minDimension * 0.18f) else moon(Offset(size.width * 0.4f, size.height * 0.38f), size.minDimension * 0.18f)
                cloud(Offset(size.width * 0.58f + sin(t * 2 * PI.toFloat()) * 2.dp.toPx(), size.height * 0.6f), size.minDimension * 0.32f, Color.White)
            }
            Condition.Cloudy, Condition.Fog -> {
                cloud(Offset(size.width * 0.42f + sin(t * 2 * PI.toFloat()) * 3.dp.toPx(), size.height * 0.45f), size.minDimension * 0.28f, Color(0xFFB8BCD0))
                cloud(Offset(size.width * 0.58f - sin(t * 2 * PI.toFloat()) * 3.dp.toPx(), size.height * 0.58f), size.minDimension * 0.32f, Color.White)
                if (condition == Condition.Fog) for (k in 0..2) {
                    val y = size.height * (0.78f + k * 0.07f)
                    drawLine(Color.White.copy(alpha = 0.5f), Offset(size.width * 0.2f + k * 6f, y), Offset(size.width * 0.8f - k * 6f, y), 2.dp.toPx(), StrokeCap.Round)
                }
            }
            Condition.Drizzle, Condition.Rain, Condition.Storm -> {
                cloud(Offset(size.width / 2, size.height * 0.42f), size.minDimension * 0.34f, if (condition == Condition.Storm) Color(0xFF9EA3BC) else Color(0xFFDDE0EC))
                val drops = if (condition == Condition.Drizzle) 3 else 5
                for (k in 0 until drops) {
                    val p = (t * 2f + k / drops.toFloat()) % 1f
                    val x = size.width * (0.3f + 0.4f * k / (drops - 1).coerceAtLeast(1))
                    val y = size.height * (0.58f + 0.32f * p)
                    drawLine(Color(0xFF8FB8FF).copy(alpha = 1 - p), Offset(x, y), Offset(x - 2.dp.toPx(), y + 5.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
                }
                if (condition == Condition.Storm && (t * 4).toInt() % 4 == 0) {
                    val bolt = androidx.compose.ui.graphics.Path().apply {
                        moveTo(size.width * 0.52f, size.height * 0.55f); lineTo(size.width * 0.44f, size.height * 0.72f)
                        lineTo(size.width * 0.52f, size.height * 0.72f); lineTo(size.width * 0.46f, size.height * 0.9f)
                    }
                    drawPath(bolt, MorningGoldFixed, style = androidx.compose.ui.graphics.drawscope.Stroke(2.5f.dp.toPx(), cap = StrokeCap.Round))
                }
            }
            Condition.Snow -> {
                cloud(Offset(size.width / 2, size.height * 0.42f), size.minDimension * 0.34f, Color.White)
                for (k in 0 until 4) {
                    val p = (t + k / 4f) % 1f
                    val x = size.width * (0.32f + 0.12f * k) + sin((p + k) * 6.28f) * 3.dp.toPx()
                    drawCircle(Color.White.copy(alpha = 1 - p * 0.6f), 2.5f.dp.toPx(), Offset(x, size.height * (0.6f + 0.3f * p)))
                }
            }
        }
    }
}

private fun DrawScope.sun(t: Float, c: Offset, r: Float) {
    drawCircle(Brush.radialGradient(listOf(MorningGoldFixed.copy(alpha = 0.5f), Color.Transparent), c, r * 2.2f), r * 2.2f, c)
    rotate(t * 360f, c) {
        for (k in 0 until 8) {
            val a = k * PI.toFloat() / 4
            drawLine(MorningGoldFixed, c + Offset(cos(a), sin(a)) * (r * 1.35f), c + Offset(cos(a), sin(a)) * (r * 1.75f), r * 0.18f, StrokeCap.Round)
        }
    }
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFE08A), MorningGoldFixed), c, r), r, c)
}

private fun DrawScope.moon(c: Offset, r: Float) {
    drawCircle(Brush.radialGradient(listOf(DawnLavenderFixed.copy(alpha = 0.4f), Color.Transparent), c, r * 2f), r * 2f, c)
    drawCircle(Color(0xFFF2EEE3), r, c)
    drawCircle(Color(0xFF1A1E33), r * 0.92f, c + Offset(r * 0.45f, -r * 0.3f))
}

private fun DrawScope.cloud(c: Offset, w: Float, color: Color) {
    val h = w * 0.55f
    drawRoundRect(color, Offset(c.x - w / 2, c.y - h * 0.1f), Size(w, h * 0.55f), androidx.compose.ui.geometry.CornerRadius(h * 0.3f))
    drawCircle(color, h * 0.38f, Offset(c.x - w * 0.18f, c.y))
    drawCircle(color, h * 0.5f, Offset(c.x + w * 0.08f, c.y - h * 0.12f))
}

private fun Double.deg() = "${Math.round(this)}°"

@Composable
fun WeatherCard(
    forecast: Forecast?,
    city: City?,
    loading: Boolean,
    modifier: Modifier = Modifier,
    onPickCity: () -> Unit,
) = AlwaysDark {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF2A3A6B).copy(alpha = 0.95f), Color(0xFF1A1E33))))
            .border(1.dp, Hairline, RoundedCornerShape(28.dp))
            .padding(Space.xl),
    ) {
        when {
            city == null -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WeatherGlyph(Condition.PartlyCloudy, true, Modifier.size(64.dp))
                    Spacer(Modifier.width(Space.md))
                    Column(Modifier.weight(1f)) {
                        Text("Add your weather", style = MaterialTheme.typography.titleMedium, color = Paper)
                        Text("Pick your city once. No location permission needed.", style = MaterialTheme.typography.bodySmall, color = PaperMuted)
                    }
                }
                Spacer(Modifier.height(Space.md))
                PillButton("Choose a city", Modifier.fillMaxWidth(), icon = Icons.Rounded.LocationCity, onClick = onPickCity)
            }
            forecast == null -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WeatherGlyph(Condition.Cloudy, true, Modifier.size(56.dp), animate = loading)
                    Spacer(Modifier.width(Space.md))
                    Text(
                        if (loading) "Checking the sky over ${city.name}…" else "Weather for ${city.name} isn't available offline.",
                        style = MaterialTheme.typography.bodyMedium, color = PaperMuted, modifier = Modifier.weight(1f),
                    )
                }
            }
            else -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            forecast.city, style = MaterialTheme.typography.labelMedium, color = DawnLavender,
                            modifier = Modifier.clip(CircleShape).clickable(onClick = onPickCity).padding(vertical = 2.dp),
                        )
                        Text(forecast.currentTemp.deg(), style = MaterialTheme.typography.displayMedium, color = Paper)
                        Text(
                            "${forecast.condition.label} · H ${forecast.high.deg()} L ${forecast.low.deg()}",
                            style = MaterialTheme.typography.bodyMedium, color = PaperMuted,
                        )
                        if (forecast.rainChance > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = Space.xs)) {
                                Icon(Icons.Rounded.WaterDrop, null, tint = Color(0xFF8FB8FF), modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("${forecast.rainChance}% chance of rain", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8FB8FF))
                            }
                        }
                    }
                    WeatherGlyph(forecast.condition, forecast.isDay, Modifier.size(96.dp))
                }
                Spacer(Modifier.height(Space.lg))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Space.sm), contentPadding = PaddingValues(end = Space.sm)) {
                    items(forecast.hours, key = { it.time }) { h ->
                        Column(
                            Modifier.width(56.dp).clip(RoundedCornerShape(18.dp)).background(Night.copy(alpha = 0.3f)).padding(vertical = Space.sm),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(if (h == forecast.hours.first()) "Now" else hourLabel(h.hour).replace(" ", ""), style = MaterialTheme.typography.labelSmall, color = PaperMuted)
                            val isDay = h.hour in 6..19
                            WeatherGlyph(h.condition, isDay, Modifier.size(32.dp), animate = false)
                            Text(h.temp.deg(), style = MaterialTheme.typography.labelLarge, color = Paper)
                            if (h.rainChance >= 20) Text("${h.rainChance}%", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8FB8FF))
                            else Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

/** Search for a city by name (Open-Meteo geocoding). Only the chosen coordinates are stored. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitySheet(
    unit: TempUnit,
    search: suspend (String) -> List<City>,
    onPick: (City) -> Unit,
    onUnit: (TempUnit) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<City>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    LaunchedEffect(query) {
        if (query.trim().length < 2) { results = emptyList(); return@LaunchedEffect }
        delay(350) // wait until typing pauses
        searching = true
        results = search(query)
        searching = false
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = Base, dragHandle = null) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(Space.xl)) {
            Text("Your city", style = MaterialTheme.typography.headlineMedium, color = Paper)
            Text(
                "Only the city's coordinates are sent to Open-Meteo for the forecast. Nothing about you.",
                style = MaterialTheme.typography.bodySmall, color = PaperMuted, modifier = Modifier.padding(top = Space.xs, bottom = Space.lg),
            )
            OutlinedTextField(
                value = query, onValueChange = { query = it.take(40) },
                placeholder = { Text("Search, e.g. Lahore or Lisbon", color = PaperMuted) },
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = PaperMuted) },
                singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DawnLavender, unfocusedBorderColor = Paper.copy(alpha = 0.15f),
                    focusedTextColor = Paper, unfocusedTextColor = Paper, cursorColor = SunriseCoral,
                ),
            )
            Spacer(Modifier.height(Space.md))
            if (searching) Text("Searching…", style = MaterialTheme.typography.bodySmall, color = PaperMuted)
            results.forEach { c ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(16.dp)).background(NightSurface)
                        .clickable { onPick(c) }.padding(Space.lg),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.LocationCity, null, tint = MorningGold)
                    Spacer(Modifier.width(Space.md))
                    Column {
                        Text(c.name, style = MaterialTheme.typography.titleSmall, color = Paper)
                        Text(c.region, style = MaterialTheme.typography.bodySmall, color = PaperMuted)
                    }
                }
            }
            if (!searching && query.trim().length >= 2 && results.isEmpty()) {
                Text("No matches yet. Check the spelling or your connection.", style = MaterialTheme.typography.bodySmall, color = PaperMuted)
            }
            Spacer(Modifier.height(Space.lg))
            Text("Temperature", style = MaterialTheme.typography.bodyLarge, color = Paper)
            Spacer(Modifier.height(Space.sm))
            ChipRow {
                Chip("°C", unit == TempUnit.Celsius, { onUnit(TempUnit.Celsius) })
                Chip("°F", unit == TempUnit.Fahrenheit, { onUnit(TempUnit.Fahrenheit) })
            }
        }
    }
}
