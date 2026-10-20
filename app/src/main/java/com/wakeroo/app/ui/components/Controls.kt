package com.wakeroo.app.ui.components

import com.wakeroo.app.ui.i18n.tr
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.wakeroo.app.ui.theme.DawnLavender
import com.wakeroo.app.ui.theme.Night
import com.wakeroo.app.ui.theme.Hairline
import com.wakeroo.app.ui.theme.NightSurface
import com.wakeroo.app.ui.theme.NightSurfaceHigh
import com.wakeroo.app.ui.theme.Paper
import com.wakeroo.app.ui.theme.PaperMuted
import com.wakeroo.app.ui.theme.Space
import com.wakeroo.app.ui.theme.SunriseCoral
import com.wakeroo.app.ui.theme.WakerooMotion

/** A titled, rounded card that groups related settings. */
@Composable
fun SectionCard(title: String, modifier: Modifier = Modifier, icon: ImageVector? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(NightSurface.copy(alpha = 0.9f))
            .border(1.dp, Hairline, RoundedCornerShape(28.dp))
            .padding(Space.xl),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, tint = DawnLavender, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(Space.sm))
            }
            Text(title, style = MaterialTheme.typography.titleMedium, color = Paper)
        }
        Spacer(Modifier.size(Space.md))
        content()
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    val bg by animateColorAsState(if (selected) SunriseCoral else Paper.copy(alpha = 0.07f), WakerooMotion.snappy(), label = "chip")
    val fg by animateColorAsState(if (selected) Night else Paper, WakerooMotion.snappy(), label = "chipText")
    Box(
        modifier
            .press3d()
            .clip(CircleShape)
            .background(bg)
            .clickable(role = Role.Checkbox) {
                haptics.performHapticFeedback(if (selected) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn)
                onClick()
            }
            .padding(horizontal = Space.lg, vertical = Space.md),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = MaterialTheme.typography.labelMedium, color = fg) }
}

@Composable
fun ToggleRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    val haptics = LocalHapticFeedback.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onChange(!checked) }.padding(vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = Paper)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = PaperMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = {
                haptics.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                onChange(it)
            },
            colors = SwitchDefaults.colors(checkedTrackColor = SunriseCoral, checkedThumbColor = Night, uncheckedTrackColor = NightSurfaceHigh, uncheckedThumbColor = PaperMuted, uncheckedBorderColor = PaperMuted),
        )
    }
}

@Composable
fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit, minusEnabled: Boolean = true, plusEnabled: Boolean = true) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = Paper, modifier = Modifier.weight(1f))
        StepButton(Icons.Rounded.Remove, tr("Less"), onMinus, minusEnabled)
        Text(value, style = MaterialTheme.typography.titleMedium, color = Paper, modifier = Modifier.padding(horizontal = Space.lg))
        StepButton(Icons.Rounded.Add, tr("More"), onPlus, plusEnabled)
    }
}

@Composable
private fun StepButton(icon: ImageVector, label: String, onClick: () -> Unit, enabled: Boolean) {
    val haptics = LocalHapticFeedback.current
    Box(
        Modifier
            .size(44.dp)
            .press3d()
            .clip(CircleShape)
            .background(Paper.copy(alpha = if (enabled) 0.1f else 0.03f))
            .clickable(enabled = enabled, onClickLabel = label) {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = if (enabled) Paper else PaperMuted.copy(alpha = 0.4f)) }
}

@Composable
fun ChipRow(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
        verticalArrangement = Arrangement.spacedBy(Space.sm),
    ) { content() }
}
