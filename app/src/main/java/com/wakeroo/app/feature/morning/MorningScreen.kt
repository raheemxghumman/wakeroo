package com.wakeroo.app.feature.morning

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wakeroo.app.ui.components.SkyBackground
import com.wakeroo.app.ui.components.TabHeader
import com.wakeroo.app.ui.theme.Sky
import com.wakeroo.app.ui.theme.Space

@Composable
fun MorningScreen(onSettings: () -> Unit) {
    SkyBackground(Sky.Morning, intensity = 0.5f) {
        Column(Modifier.fillMaxSize().padding(horizontal = Space.xl)) {
            TabHeader("Good morning", onSettings = onSettings, eyebrow = "Today")
        }
    }
}
