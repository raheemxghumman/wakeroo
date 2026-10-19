package com.wakeroo.app.ui.scenes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wakeroo.app.alarm.ring.RingScreen
import com.wakeroo.app.alarm.ring.RingState
import com.wakeroo.app.data.RingKind
import com.wakeroo.app.data.model.Alarm
import com.wakeroo.app.feature.editor.PillButton
import com.wakeroo.app.ui.components.press3d
import com.wakeroo.app.ui.theme.DawnLavender
import com.wakeroo.app.ui.theme.Hairline
import com.wakeroo.app.ui.theme.MorningGold
import com.wakeroo.app.ui.theme.Night
import com.wakeroo.app.ui.theme.Outfit
import com.wakeroo.app.ui.theme.Paper
import com.wakeroo.app.ui.theme.PaperMuted
import com.wakeroo.app.ui.theme.Space
import com.wakeroo.app.ui.theme.SunriseCoral
import com.wakeroo.app.ui.theme.WakerooTheme
import com.wakeroo.app.ui.util.formatClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A row of live miniature ring screens. Tap one to see it full screen with your alarm time,
 * then use it. [selected] null means "follow the app default" when [defaultLabel] is set.
 */
@Composable
fun ScenePicker(
    selected: String?,
    pro: Boolean,
    onSelect: (String?) -> Unit,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
    hour: Int = 7,
    minute: Int = 0,
    defaultLabel: String? = null,
    defaultScene: String = Scene.NightSky.id,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onPreviewScene: (Scene?) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var previewing by remember { mutableStateOf<Scene?>(null) }
    var photoVersion by remember { mutableIntStateOf(0) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) { copySceneImage(context, uri) }
            if (ok) {
                photoVersion++
                previewing = Scene.Photo
            }
        }
    }

    LazyRow(modifier, contentPadding = contentPadding, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
        if (defaultLabel != null) {
            item(key = "default") {
                SceneCard(Scene.byId(defaultScene), defaultLabel, selected == null, locked = false, hour, minute) { onSelect(null) }
            }
        }
        items(Scene.entries, key = { it.id + if (it == Scene.Photo) photoVersion else 0 }) { scene ->
            val hasPhoto = scene != Scene.Photo || customScenePhoto(context).exists()
            SceneCard(
                scene, scene.title, selected == scene.id, locked = scene.pro && !pro, hour, minute, empty = !hasPhoto,
            ) {
                if (scene == Scene.Photo && !hasPhoto && pro) {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                } else {
                    previewing = scene
                    onPreviewScene(scene)
                }
            }
        }
    }

    previewing?.let { scene ->
        ScenePreviewDialog(
            scene, hour, minute, locked = scene.pro && !pro,
            onClose = { previewing = null; onPreviewScene(null) },
            onUse = {
                previewing = null
                onPreviewScene(null)
                onSelect(scene.id)
            },
            onUpgrade = { previewing = null; onPreviewScene(null); onUpgrade() },
            onChangePhoto = if (scene == Scene.Photo && pro) {
                { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            } else null,
        )
    }
}

@Composable
private fun SceneCard(
    scene: Scene,
    title: String,
    selected: Boolean,
    locked: Boolean,
    hour: Int,
    minute: Int,
    empty: Boolean = false,
    onClick: () -> Unit,
) {
    val border by animateColorAsState(if (selected) SunriseCoral else Hairline, label = "sceneBorder")
    Column(Modifier.width(118.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(118.dp, 200.dp)
                .press3d(maxTilt = 10f)
                .clip(RoundedCornerShape(24.dp))
                .border(if (selected) 3.dp else 1.dp, border, RoundedCornerShape(24.dp))
                .clickable(role = Role.RadioButton, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (empty) {
                Box(Modifier.fillMaxSize().background(Brush2.photoEmpty), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.AddPhotoAlternate, null, tint = Paper)
                        Text("Pick a photo", style = MaterialTheme.typography.labelSmall, color = Paper, modifier = Modifier.padding(top = Space.xs))
                    }
                }
            } else {
                SceneBackground(scene) {
                    Text(
                        formatClock(hour, minute),
                        style = MaterialTheme.typography.headlineMedium.copy(fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 30.sp),
                        color = Color(0xFFF5F3EE),
                        modifier = Modifier.align(Alignment.Center).padding(bottom = 30.dp),
                    )
                    Box(
                        Modifier.align(Alignment.BottomCenter).padding(12.dp).fillMaxWidth().height(18.dp)
                            .clip(CircleShape).background(SunriseCoral.copy(alpha = 0.9f)),
                    )
                }
            }
            if (locked) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(8.dp).size(26.dp).clip(CircleShape).background(Night.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Lock, "Pro", tint = MorningGold, modifier = Modifier.size(14.dp)) }
            }
        }
        Text(
            title, style = MaterialTheme.typography.labelMedium, color = if (selected) SunriseCoral else Paper,
            modifier = Modifier.padding(top = Space.sm), maxLines = 1,
        )
    }
}

private object Brush2 {
    val photoEmpty = androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF3A2B4E), Color(0xFF6B4A78)))
}

/** The real ring screen with your time on it, so you see exactly what 6 AM will look like. */
@Composable
fun ScenePreviewDialog(
    scene: Scene,
    hour: Int,
    minute: Int,
    locked: Boolean,
    onClose: () -> Unit,
    onUse: () -> Unit,
    onUpgrade: () -> Unit,
    onChangePhoto: (() -> Unit)? = null,
) {
    val state = remember(hour, minute) {
        val now = System.currentTimeMillis()
        RingState(Alarm(hour = hour, minute = minute, label = scene.blurb), RingKind.Alarm, now, now, 0)
    }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        WakerooTheme(dark = true) {
            RingScreen(
                state = state, scene = scene, onStartMission = {}, onSnooze = {}, clock = java.time.LocalTime.of(hour, minute),
                topBar = {
                    Row(Modifier.fillMaxWidth().padding(top = Space.md), verticalAlignment = Alignment.CenterVertically) {
                        Text("Preview · ${scene.title}", style = MaterialTheme.typography.titleMedium, color = DawnLavender, modifier = Modifier.weight(1f))
                        Box(
                            Modifier.size(44.dp).clip(CircleShape).background(Night.copy(alpha = 0.4f)).clickable(onClick = onClose),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Rounded.Close, "Close preview", tint = Paper) }
                    }
                },
                actions = {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.md)) {
                        if (locked) {
                            PillButton("Unlock with Pro", Modifier.fillMaxWidth(), icon = Icons.Rounded.Lock, onClick = onUpgrade)
                            Text("${scene.title} is one of the Pro scenes.", style = MaterialTheme.typography.bodySmall, color = PaperMuted, modifier = Modifier.align(Alignment.CenterHorizontally))
                        } else {
                            PillButton("Use this scene", Modifier.fillMaxWidth(), onClick = onUse)
                            if (onChangePhoto != null) PillButton("Choose another photo", Modifier.fillMaxWidth(), outlined = true, onClick = onChangePhoto)
                        }
                    }
                },
            )
        }
    }
}

/** Copies a picked photo into device-protected storage, scaled to at most 1600 px on its long side. */
private fun copySceneImage(context: Context, uri: Uri): Boolean = runCatching {
    val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
            val long = maxOf(info.size.width, info.size.height)
            if (long > 1600) decoder.setTargetSampleSize((long / 1600).coerceAtLeast(1))
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } else {
        @Suppress("DEPRECATION") MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    }
    customScenePhoto(context).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
    true
}.getOrDefault(false)
