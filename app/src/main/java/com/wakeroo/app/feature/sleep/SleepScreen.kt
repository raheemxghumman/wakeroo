package com.wakeroo.app.feature.sleep

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
fun SleepScreen(onSettings: () -> Unit) {
    SkyBackground(Sky.Night, intensity = 0.6f) {
        Column(Modifier.fillMaxSize().padding(horizontal = Space.xl)) {
            TabHeader("Sleep", onSettings = onSettings, eyebrow = "Tonight")
        }
    }
}
