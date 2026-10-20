package com.wakeroo.app.feature.pro

import com.wakeroo.app.ui.theme.MorningGoldFixed
import android.app.Activity
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wakeroo.app.container
import com.wakeroo.app.data.Settings
import com.wakeroo.app.ui.brand.WakerooMark
import com.wakeroo.app.ui.components.SkyBackground
import com.wakeroo.app.ui.components.press3d
import com.wakeroo.app.ui.components.rememberDeviceTilt
import com.wakeroo.app.ui.theme.Calm
import com.wakeroo.app.ui.theme.MorningGold
import com.wakeroo.app.ui.theme.Night
import com.wakeroo.app.ui.theme.Paper
import com.wakeroo.app.ui.theme.Sky
import com.wakeroo.app.ui.theme.Space
import com.wakeroo.app.ui.theme.SunriseCoral
import com.wakeroo.app.ui.theme.WakerooMotion
import com.wakeroo.app.ui.theme.WakerooTheme
import kotlinx.coroutines.delay

private data class Benefit(val icon: ImageVector, val title: String, val body: String)

private val BENEFITS get() = listOf(
    Benefit(Icons.Rounded.Layers, "Up to 5 missions per alarm", "Stack math, a QR code in the kitchen and a photo of the sink."),
    Benefit(Icons.Rounded.Lock, "Harder Mode", "Off, delete, skip and snooze all need a mission first. No sleepy cheating."),
    Benefit(Icons.Rounded.Palette, "Every wake-up scene", "Aurora, Ocean Dawn, WAKE UP and your own photo, plus four more tones."),
    Benefit(Icons.Rounded.Bedtime, "Mix sleep sounds", "Layer rain over a fan, and unlock pink noise and crickets."),
    Benefit(Icons.Rounded.Insights, "Your whole history", "Every month on the calendar and every ring in the log."),
    Benefit(Icons.Rounded.VolunteerActivism, "No banner, and you support an indie alarm", "Pay once. No subscription, no renewal, no surprises."),
)

@Composable
fun ProScreen(onClose: () -> Unit) {
    WakerooTheme(dark = true) { ProContent(onClose) }
}

@Composable
private fun ProContent(onClose: () -> Unit) {
    val context = LocalContext.current
    val app = context.container
    val settings by app.settings.settings.collectAsStateWithLifecycle(Settings())
    val price by app.billing.price.collectAsStateWithLifecycle()
    val message by app.billing.message.collectAsStateWithLifecycle()
    val tilt by rememberDeviceTilt()
    LaunchedEffect(Unit) { app.billing.start() }

    SkyBackground(Sky.Sunrise, tilt = tilt) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = Space.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth().padding(top = Space.sm)) {
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(Night.copy(alpha = 0.3f)).clickable(onClickLabel = "Close", onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Close, "Close", tint = Paper) }
            }
            ProOrb(tilt)
            Text("Wakeroo Pro", style = MaterialTheme.typography.displaySmall, color = Paper)
            Text(
                "Every mission and every reliability feature stays free. Always.",
                style = MaterialTheme.typography.bodyLarge, color = Paper.copy(alpha = 0.85f), textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Space.sm, bottom = Space.xl),
            )
            BENEFITS.forEachIndexed { i, b -> BenefitRow(b, i) }
            Spacer(Modifier.height(Space.xl))
            if (settings.pro) {
                Row(
                    Modifier.fillMaxWidth().height(64.dp).clip(CircleShape).background(Calm),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                ) { Text("Pro is active. Thank you!", style = MaterialTheme.typography.titleMedium, color = Night) }
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .press3d()
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(Night, Color(0xFF252A44))))
                        .border(1.5.dp, MorningGold, CircleShape)
                        .clickable { (context as? Activity)?.let(app.billing::buy) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Unlock Pro · ${price ?: "\$4.99"} once", style = MaterialTheme.typography.titleMedium, color = MorningGold,
                    )
                }
                Text(
                    "Restore purchase", style = MaterialTheme.typography.labelLarge, color = Paper,
                    modifier = Modifier.padding(top = Space.md).clip(CircleShape).clickable { app.billing.restore() }.padding(Space.md),
                )
            }
            message?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = Paper, textAlign = TextAlign.Center, modifier = Modifier.padding(top = Space.sm))
                LaunchedEffect(it) { delay(4000); app.billing.clearMessage() }
            }
            Spacer(Modifier.height(Space.xxl))
        }
    }
}

/** A glossy sun orb that floats and turns with the phone. */
@Composable
private fun ProOrb(tilt: Offset) {
    val float by rememberInfiniteTransition(label = "orb").animateFloat(
        -1f, 1f, infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse), label = "f",
    )
    Box(
        Modifier
            .padding(vertical = Space.xl)
            .size(180.dp)
            .graphicsLayer {
                translationY = float * 10f
                rotationX = -tilt.y * 14f
                rotationY = tilt.x * 14f
                cameraDistance = 10f * density
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2
            drawCircle(Brush.radialGradient(listOf(MorningGoldFixed, SunriseCoral, Color(0xFFB94A3C)), center = center + Offset(-r * 0.3f + tilt.x * 18f, -r * 0.35f + tilt.y * 18f), radius = r * 1.4f), r)
            drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.55f), Color.Transparent), center = center + Offset(-r * 0.35f, -r * 0.45f), radius = r * 0.5f), r)
        }
        WakerooMark(Modifier.size(110.dp), horizonColor = Night, sunColor = Night.copy(alpha = 0.85f), leapColor = Night.copy(alpha = 0.7f), dotColor = Paper)
    }
}

@Composable
private fun BenefitRow(b: Benefit, index: Int) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(120L * index); appear.animateTo(1f, WakerooMotion.bouncy()) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = Space.md)
            .graphicsLayer { alpha = appear.value.coerceIn(0f, 1f); translationY = (1 - appear.value) * 60f; rotationX = (1 - appear.value) * 25f }
            .clip(RoundedCornerShape(22.dp))
            .background(Night.copy(alpha = 0.35f))
            .padding(Space.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(b.icon, null, tint = MorningGold)
        Spacer(Modifier.width(Space.lg))
        Column {
            Text(b.title, style = MaterialTheme.typography.titleSmall, color = Paper)
            Text(b.body, style = MaterialTheme.typography.bodySmall, color = Paper.copy(alpha = 0.75f))
        }
    }
}

