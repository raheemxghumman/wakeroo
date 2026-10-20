package com.wakeroo.app.ui.components

import com.wakeroo.app.ui.i18n.tr
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
        MissionType.Math -> tr("Solve quick sums. Wakes your brain.")
        MissionType.Typing -> tr("Type a short phrase, letter by letter.")
        MissionType.Shake -> tr("Shake the phone until the sun fills.")
        MissionType.Scan -> tr("Scan a code you leave in another room.")
        MissionType.Photo -> tr("Photograph a spot, like your sink.")
        MissionType.Steps -> tr("Walk until the track fills up.")
        MissionType.Memory -> tr("Repeat the pattern of glowing tiles.")
        MissionType.Nfc -> tr("Tap your phone on a sticker by the mirror.")
        MissionType.Plan -> tr("Type the first thing you'll do today.")
        MissionType.Hunt -> tr("Find an everyday object, like a cup.")
    }

/** A few words for the small mission tiles. */
val MissionType.tagline: String
    get() = when (this) {
        MissionType.Math -> tr("Quick sums")
        MissionType.Typing -> tr("Type a phrase")
        MissionType.Shake -> tr("Shake to fill")
        MissionType.Memory -> tr("Remember the tiles")
        MissionType.Plan -> tr("Write today's plan")
        else -> tr("Gets you out of bed")
    }
