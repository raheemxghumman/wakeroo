package com.wakeroo.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Nfc
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
        MissionType.Steps -> Icons.AutoMirrored.Rounded.DirectionsWalk
        MissionType.Memory -> Icons.Rounded.GridView
        MissionType.Nfc -> Icons.Rounded.Nfc
        MissionType.Plan -> Icons.Rounded.EditNote
        MissionType.Hunt -> Icons.Rounded.ImageSearch
    }

val MissionType.blurb: String
    get() = when (this) {
        MissionType.Math -> "Solve quick sums. Wakes your brain."
        MissionType.Typing -> "Type a short phrase, letter by letter."
        MissionType.Shake -> "Shake the phone until the sun fills."
        MissionType.Scan -> "Scan a code you leave in another room."
        MissionType.Photo -> "Photograph a spot, like your sink."
        MissionType.Steps -> "Walk until the track fills up."
        MissionType.Memory -> "Repeat the pattern of glowing tiles."
        MissionType.Nfc -> "Tap your phone on a sticker by the mirror."
        MissionType.Plan -> "Type the first thing you'll do today."
        MissionType.Hunt -> "Find an everyday object, like a cup."
    }
