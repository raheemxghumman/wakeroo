package com.wakeroo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.wakeroo.app.ui.theme.DawnLavender
import com.wakeroo.app.ui.theme.Paper
import com.wakeroo.app.ui.theme.Space

/** A round, glassy icon button that sinks in 3D when pressed. */
@Composable
fun RoundIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Paper,
    background: Color = Paper.copy(alpha = 0.08f),
) {
    Box(
        modifier
            .size(48.dp)
            .press3d()
            .clip(CircleShape)
            .background(background)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = tint) }
}

/**
 * The top of every tab: an eyebrow line, a big title (or a custom [lead], like the wordmark),
 * extra [actions] and the settings gear.
 */
@Composable
fun TabHeader(
    title: String,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    lead: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier.statusBarsPadding().padding(top = Space.lg, bottom = Space.xl),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        Column(Modifier.weight(1f)) {
            if (eyebrow != null) Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelSmall, color = DawnLavender)
            if (lead != null) lead() else Text(title, style = MaterialTheme.typography.headlineLarge, color = Paper)
        }
        actions()
        Spacer(Modifier.size(0.dp))
        RoundIconButton(Icons.Rounded.Settings, "Settings", onSettings)
    }
}
