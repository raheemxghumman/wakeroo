package com.wakeroo.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.ui.graphics.vector.ImageVector
import com.wakeroo.app.data.model.MissionType

val MissionType.icon: ImageVector
    get() = when (this) {
        MissionType.Math -> Icons.Rounded.Calculate
        MissionType.Typing -> Icons.Rounded.Keyboard
        MissionType.Shake -> Icons.Rounded.Vibration
        MissionType.Scan -> Icons.Rounded.QrCodeScanner
        MissionType.Photo -> Icons.Rounded.CameraAlt
    }

val MissionType.blurb: String
    get() = when (this) {
        MissionType.Math -> "Solve quick sums. Wakes your brain."
        MissionType.Typing -> "Type a short phrase, letter by letter."
        MissionType.Shake -> "Shake the phone until the sun fills."
        MissionType.Scan -> "Scan a code you leave in another room."
        MissionType.Photo -> "Photograph a spot, like your sink."
    }
