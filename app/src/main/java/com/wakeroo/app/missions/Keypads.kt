package com.wakeroo.app.missions

import com.wakeroo.app.ui.i18n.tr
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakeroo.app.ui.components.press3d
import com.wakeroo.app.ui.theme.Night
import com.wakeroo.app.ui.theme.NightSurfaceHigh
import com.wakeroo.app.ui.theme.Outfit
import com.wakeroo.app.ui.theme.Paper
import com.wakeroo.app.ui.theme.Space
import com.wakeroo.app.ui.theme.SunriseCoral

sealed interface Key {
    data class Char(val c: kotlin.Char) : Key
    data object Delete : Key
    data object Enter : Key
    data object Space : Key
}

/** A chunky key that sinks under the finger. */
@Composable
fun RowScope.KeyCap(
    key: Key,
    onKey: (Key) -> Unit,
    height: Dp,
    weight: Float = 1f,
    accent: Boolean = false,
) {
    val haptics = LocalHapticFeedback.current
    val label = when (key) {
        is Key.Char -> key.c.toString()
        Key.Space -> tr("space")
        else -> ""
    }
    val description = when (key) {
        is Key.Char -> key.c.toString()
        Key.Delete -> tr("Delete")
        Key.Enter -> tr("Check answer")
        Key.Space -> tr("Space")
    }
    Box(
        Modifier
            .weight(weight)
            .height(height)
            .press3d(maxTilt = 14f, sink = 0.92f)
            .shadow(6.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black, spotColor = Color.Black)
            .clip(RoundedCornerShape(20.dp))
            .background(if (accent) SunriseCoral else NightSurfaceHigh)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button) {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onKey(key)
            }
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        when (key) {
            Key.Delete -> Icon(Icons.AutoMirrored.Rounded.Backspace, null, tint = Paper)
            Key.Enter -> Icon(Icons.Rounded.Check, null, tint = if (accent) Night else Paper)
            else -> Text(
                label,
                style = if (key is Key.Char) MaterialTheme.typography.headlineMedium.copy(fontFamily = Outfit, fontSize = 30.sp) else MaterialTheme.typography.labelLarge,
                color = Paper,
            )
        }
    }
}

@Composable
fun NumberPad(onKey: (Key) -> Unit, modifier: Modifier = Modifier) {
    val rows = listOf("123", "456", "789")
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.md)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                row.forEach { KeyCap(Key.Char(it), onKey, 72.dp) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
            KeyCap(Key.Delete, onKey, 72.dp)
            KeyCap(Key.Char('0'), onKey, 72.dp)
            KeyCap(Key.Enter, onKey, 72.dp, accent = true)
        }
    }
}

@Composable
fun LetterPad(onKey: (Key) -> Unit, modifier: Modifier = Modifier) {
    val rows = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        rows.forEachIndexed { i, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                if (i == 1) Box(Modifier.weight(0.5f))
                if (i == 2) KeyCap(Key.Delete, onKey, 54.dp, weight = 1.5f)
                row.forEach { KeyCap(Key.Char(it), onKey, 54.dp) }
                if (i == 1) Box(Modifier.weight(0.5f))
                if (i == 2) KeyCap(Key.Enter, onKey, 54.dp, weight = 1.5f, accent = true)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(Modifier.weight(1.5f))
            KeyCap(Key.Space, onKey, 54.dp, weight = 7f)
            Box(Modifier.weight(1.5f))
        }
    }
}
