package com.wakeroo.app.feature.main

import com.wakeroo.app.ui.i18n.tr
import com.wakeroo.app.ui.i18n.n
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.WbSunny
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.wakeroo.app.ui.theme.Hairline
import com.wakeroo.app.ui.theme.LocalReducedMotion
import com.wakeroo.app.ui.theme.MorningGold
import com.wakeroo.app.ui.theme.NightSurface
import com.wakeroo.app.ui.theme.NightSurfaceHigh
import com.wakeroo.app.ui.theme.PaperMuted
import com.wakeroo.app.ui.theme.SunriseCoral

enum class MainTab(private val titleEn: String, val filled: ImageVector, val outlined: ImageVector) {
    Alarms(n("Alarms"), Icons.Rounded.Alarm, Icons.Outlined.Alarm),
    Sleep(n("Sleep"), Icons.Rounded.Bedtime, Icons.Outlined.Bedtime),
    Morning(n("Morning"), Icons.Rounded.WbSunny, Icons.Outlined.WbSunny),
    You(n("You"), Icons.Rounded.EmojiEvents, Icons.Outlined.EmojiEvents);

    val title: String get() = tr(titleEn)
}

val BottomBarHeight = 68.dp

/**
 * A floating glass bar. A glowing pill slides under the selected tab on a spring, and the
 * chosen icon hops and fills in. It drops out of the way while a list scrolls down.
 */
@Composable
fun BottomBar(selected: MainTab, visible: Boolean, onSelect: (MainTab) -> Unit, modifier: Modifier = Modifier) {
    val hide by animateFloatAsState(if (visible) 0f else 1f, spring(dampingRatio = 0.8f, stiffness = 420f), label = "barHide")
    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .graphicsLayer {
                translationY = hide * (size.height + 48.dp.toPx())
                alpha = 1f - hide * 0.6f
            }
            .shadow(22.dp, RoundedCornerShape(30.dp), ambientColor = Color.Black, spotColor = Color.Black)
            .clip(RoundedCornerShape(30.dp))
            .background(Brush.verticalGradient(listOf(NightSurfaceHigh, NightSurface)))
            .border(1.dp, Hairline, RoundedCornerShape(30.dp))
            .height(BottomBarHeight),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val itemWidth = maxWidth / MainTab.entries.size
            val x by animateDpAsState(itemWidth * selected.ordinal, spring(dampingRatio = 0.72f, stiffness = 380f), label = "pill")
            // The glowing pill behind the selected tab.
            Box(
                Modifier
                    .offset(x = x)
                    .width(itemWidth)
                    .fillMaxHeight()
                    .padding(6.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.verticalGradient(listOf(SunriseCoral.copy(alpha = 0.26f), MorningGold.copy(alpha = 0.10f))))
                    .border(1.dp, SunriseCoral.copy(alpha = 0.28f), RoundedCornerShape(24.dp)),
            )
            Row(Modifier.fillMaxSize()) {
                MainTab.entries.forEach { tab ->
                    TabItem(tab, tab == selected, Modifier.weight(1f).fillMaxHeight()) { onSelect(tab) }
                }
            }
        }
    }
}

@Composable
private fun TabItem(tab: MainTab, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val reduced = LocalReducedMotion.current
    val hop = remember { Animatable(0f) }
    LaunchedEffect(selected) {
        if (selected && !reduced) {
            hop.snapTo(0f)
            hop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 500f), initialVelocity = 14f)
            hop.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 300f))
        }
    }
    val tint by animateColorAsState(if (selected) SunriseCoral else PaperMuted, label = "tabTint")
    Column(
        modifier
            .clickable(remember { MutableInteractionSource() }, null, role = Role.Tab) {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onClick()
            }
            .semantics { this.selected = selected },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(26.dp)
                .graphicsLayer {
                    translationY = -hop.value * 5.dp.toPx()
                    val s = 1f + hop.value * 0.16f
                    scaleX = s; scaleY = s
                    rotationY = hop.value * 25f
                    cameraDistance = 8f * density
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (selected) tab.filled else tab.outlined, null, tint = tint, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(2.dp))
        Text(tab.title, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}
